package android.zero.studio.compose.preview.editor.language

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.zero.studio.compose.preview.editor.CodeEditorView
import android.zero.studio.compose.preview.kotlin.AnalysisReport
import android.zero.studio.compose.preview.kotlin.KotlinEnvironment
import android.zero.studio.compose.preview.language.IdeLanguage
import android.zero.studio.compose.preview.ui.fragments.EditorFragment
import android.zero.studio.compose.preview.utils.*
import com.android.tools.r8.CompilationMode
import com.android.tools.r8.OutputMode
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
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
 * 负责管理 Kotlin 编译与预览流水线的语言类。
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

    // ★ 修复点：公开方法以供 DiagnosticAnalyzer 调用
    fun setDiagnosticsContainer(container: DiagnosticsContainer) {
        this._diagnosticsContainer = container
    }

    fun getDiagnosticsContainer(): DiagnosticsContainer = _diagnosticsContainer

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 扫描所有 Jar 包路径
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
                analyze(this@KotlinLanguage.file, null)
                onAnalyzed?.invoke(true)
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
                editorFragment.layoutLoading(true)
                val newContainer = DiagnosticsContainer()
                tempContainer = newContainer
                
                if (FileUtil.classesOutDir.exists()) FileUtil.classesOutDir.deleteRecursively()
                FileUtil.classesOutDir.mkdirs()

                // ★ 编译 *.kt 到 *.class
                kotlinEnvironment.analyzeAndGenerate(f, content)
                
                val extracted = ArrayList<DiagnosticRegion>()
                newContainer.queryInRegion(extracted, 0, Int.MAX_VALUE)
                
                if (extracted.none { it.severity == 3.toShort() }) {
                    FileUtil.classesOutDir.zipToJar(FileUtil.classesJarOut)
                    val dexSuccess = suspendCancellableCoroutine<Boolean> { continuation ->
                        CompilerUtils.compileDex(
                            inputDir = FileUtil.classesJarOut,
                            outputDir = FileUtil.dexOutDir,
                            classpath = FileUtil.classpathLibsDir.classPathFiles() + FileUtil.classpathJvmDir.classPathFiles(),
                            library = listOf(FileUtil.androidJar),
                            minApiLevel = 26
                        ) { _, ex -> continuation.resume(ex == null) }
                    }
                    if (dexSuccess) editorFragment.loadPreview()
                }
                
                editorFragment.layoutLoading(false)
                tempContainer = null
                newContainer
            }
        }
    }
}