package android.zero.studio.compose.preview.editor.language

import com.google.android.material.bottomnavigation.BottomNavigationView
import android.zero.studio.compose.preview.ComposeApplication
import android.zero.studio.compose.preview.editor.CodeEditorView
import android.zero.studio.compose.preview.kotlin.AnalysisReport
import android.zero.studio.compose.preview.kotlin.KotlinEnvironment
import android.zero.studio.compose.preview.language.IdeLanguage
import android.zero.studio.compose.preview.ui.fragments.EditorFragment
import android.zero.studio.compose.preview.utils.*
import com.android.tools.r8.CompilationMode
import com.android.tools.r8.OutputMode
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import java.io.File
import java.util.ArrayList
import kotlin.coroutines.resume

/**
 * 负责管理 Kotlin 编译与预览流水线的语言层。
 * 
 * 工作流程线路图:
 * 1. 继承 IdeLanguage 处理代码高亮。
 * 2. 协程初始化 KotlinEnvironment，装载所有需要使用的 Jvm/Plugin/Android 库。
 * 3. 拦截每次 analyze 请求：
 *    -> 调用 kotlinEnvironment 生成 `.class`。
 *    -> 校验结果无错误 (severity != 3)。
 *    -> 调用 FileUtilKt 中的 zipToJar 把类打进 jar。
 *    -> 调用 D8 Compiler 将 jar 转换为可被 Dalvik 识别的 `.dex.zip`。
 *    -> 如果全部成功，切换回主线程调度 `editorFragment.loadPreview()`。
 * 
 * @author android_zero
 */
class KotlinLanguage(
    private val editor: CodeEditorView,
    private val file: File,
    private val topRightNavView: BottomNavigationView,
    val editorFragment: EditorFragment
) : IdeLanguage(
    GrammarRegistry.getInstance().findGrammar("source.kotlin"),
    GrammarRegistry.getInstance().findLanguageConfiguration("source.kotlin"),
    GrammarRegistry.getInstance(),
    ThemeRegistry.getInstance(),
    false
) {
    private var _diagnosticsContainer = DiagnosticsContainer()
    lateinit var kotlinEnvironment: KotlinEnvironment
    private var tempContainer: DiagnosticsContainer? = null
    var onAnalyzed: ((Boolean) -> Unit)? = null
    private val analysisMutex = Mutex()

    fun setDiagnosticsContainer(container: DiagnosticsContainer) {
        this._diagnosticsContainer = container
    }

    fun getDiagnosticsContainer(): DiagnosticsContainer = _diagnosticsContainer

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 首次安装启动时，等待 classpath 资产就绪，避免 import 解析偶发失败。
                ComposeApplication.awaitCompilerAssets()

                val libJars = FileUtil.classpathLibsDir.classPathFiles()
                val jvmJars = FileUtil.classpathJvmDir.classPathFiles()
                val allJars = libJars + jvmJars
                
                val builder = KotlinEnvironment.home(FileUtil.kotlinHomeDir)
                    .withJvmClasspathRoots(allJars)
                    .withJvmSdkRoots(listOf(FileUtil.androidJar, FileUtil.coreLambdaStubs, FileUtil.javaBaseJar))
                    .withPlugins(FileUtil.classpathPluginsDir.classPathFiles())
                    .withConfiguration { config ->
                        config.put(CommonConfigurationKeys.MODULE_NAME, "compose-preview")
                        config.put(JVMConfigurationKeys.OUTPUT_DIRECTORY, FileUtil.classesOutDir)
                    }
                
                kotlinEnvironment = builder.withAnalysisReportListener { handleAnalysisReport(it) }.create()
                
                // 初次冷启动全量编译
                analyze(this@KotlinLanguage.file, null)
                
                withContext(Dispatchers.Main) {
                    onAnalyzed?.invoke(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleAnalysisReport(report: AnalysisReport) {
        val targetContainer = tempContainer ?: _diagnosticsContainer
        val severity: Short = when (report.severity) {
            CompilerMessageSeverity.ERROR, CompilerMessageSeverity.EXCEPTION -> 3
            CompilerMessageSeverity.WARNING, CompilerMessageSeverity.STRONG_WARNING -> 2
            else -> return
        }
        targetContainer.addDiagnostic(
            DiagnosticRegion(
                report.startOffset, report.endOffset, severity, 0L, 
                DiagnosticDetail(report.message, null, null, null)
            )
        )
    }

    suspend fun analyze(f: File, content: String? = null): DiagnosticsContainer? {
        if (!::kotlinEnvironment.isInitialized) return null
        return analysisMutex.withLock {
            withContext(Dispatchers.IO) {
                val newContainer = DiagnosticsContainer()
                tempContainer = newContainer
                
                // 清理之前的 .class 脏数据以保持绝对纯净
                if (FileUtil.classesOutDir.exists()) FileUtil.classesOutDir.deleteRecursively()
                FileUtil.classesOutDir.mkdirs()

                // 执行基于 JVM 字节码的生成操作
                kotlinEnvironment.analyzeAndGenerate(f, content)
                
                val extracted = ArrayList<DiagnosticRegion>()
                newContainer.queryInRegion(extracted, 0, Int.MAX_VALUE)
                
                // 核心修复：只在毫无 ERROR (等级 3) 的情况下，才执行 DEX 打包动作
                if (extracted.none { it.severity == 3.toShort() }) {
                    FileUtil.classesOutDir.zipToJar(FileUtil.classesJarOut)
                    
                    // 将生成 Dex 步骤挂起并转为异步等待
                    val dexSuccess = suspendCancellableCoroutine<Boolean> { continuation ->
                        CompilerUtils.compileDex(
                            inputDir = FileUtil.classesJarOut,
                            outputDir = FileUtil.dexOutDir,
                            classpath = FileUtil.classpathLibsDir.classPathFiles() + FileUtil.classpathJvmDir.classPathFiles(),
                            library = listOf(FileUtil.androidJar),
                            minApiLevel = 26
                        ) { _, ex -> 
                            if (continuation.isActive) continuation.resume(ex == null) 
                        }
                    }
                    
                    // 一旦 DEX 转换成功，通知主 UI 线程拉起 Render
                    if (dexSuccess) {
                        runCatching {
                            DexHotReloadEnhancer.enhance(
                                compiledJar = FileUtil.classesJarOut,
                                hotDexZip = FileUtil.classesJarDex,
                                baselineDexZip = FileUtil.previousClassesJarDex.takeIf { it.exists() },
                                changedClassPrefixes = setOf(f.nameWithoutExtension),
                                minApiLevel = 26
                            )
                        }
                        runCatching {
                            FileUtil.classesJarDex.copyTo(FileUtil.previousClassesJarDex, overwrite = true)
                        }
                        withContext(Dispatchers.Main) {
                            editorFragment.loadPreview()
                        }
                    } else {
                        // Dex 过程发生异变，清理 Loader 指示器
                        withContext(Dispatchers.Main) {
                            editorFragment.layoutLoading(show = false)
                        }
                    }
                } else {
                    // 如果代码有报错，隐藏 Loading 但保留当前的旧视图，并在代码行上标记错误红线
                    withContext(Dispatchers.Main) {
                        editorFragment.layoutLoading(show = false)
                    }
                }
                
                tempContainer = null
                newContainer
            }
        }
    }
}
