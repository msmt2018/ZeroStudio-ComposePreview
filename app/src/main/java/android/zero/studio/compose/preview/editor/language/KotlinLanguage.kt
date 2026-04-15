package android.zero.studio.compose.preview.editor.language

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.zero.studio.compose.preview.editor.CodeEditorView
import android.zero.studio.compose.preview.kotlin.AnalysisReport
import android.zero.studio.compose.preview.kotlin.KotlinEnvironment
import android.zero.studio.compose.preview.language.IdeLanguage
import android.zero.studio.compose.preview.ui.fragments.EditorFragment
import android.zero.studio.compose.preview.utils.CompilerUtils
import android.zero.studio.compose.preview.utils.FileUtil
import android.zero.studio.compose.preview.utils.classPathFiles // FIX: Explicit import for clarity if needed
import com.android.tools.r8.CompilationFailedException
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
import org.eclipse.tm4e.core.grammar.IGrammar
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File
import kotlin.coroutines.resume

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
    private var _diagnosticsContainer: DiagnosticsContainer = DiagnosticsContainer()
    
    fun getDiagnosticsContainer(): DiagnosticsContainer {
        return _diagnosticsContainer
    }

    fun setDiagnosticsContainer(container: DiagnosticsContainer) {
        this._diagnosticsContainer = container
    }

    lateinit var kotlinEnvironment: KotlinEnvironment
        internal set

    private var tempContainer: DiagnosticsContainer? = null
    var onAnalyzed: ((Boolean) -> Unit)? = null
        internal set

    private val analysisMutex: Mutex = Mutex()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val builder = KotlinEnvironment.home(FileUtil.kotlinHomeDir)
                    // FIX: Correctly call extension function on the File object
                    .withJvmClasspathRoots(FileUtil.classpathLibsDir.classPathFiles())
                    .withJvmSdkRoots(listOf(FileUtil.androidJar, FileUtil.coreLambdaStubs, FileUtil.javaBaseJar))
                    // FIX: Correctly call extension function on the File object
                    .withPlugins(FileUtil.classpathPluginsDir.classPathFiles())
                    .withConfiguration { config ->
                        config.put(CommonConfigurationKeys.MODULE_NAME, "compose-preview")
                    }
                
                kotlinEnvironment = builder.withAnalysisReportListener { report ->
                    handleAnalysisReport(report)
                }.create()

                val initialResult = analyze(this@KotlinLanguage.file, null)
                if (initialResult != null) {
                    setDiagnosticsContainer(initialResult)
                    editor.post { editor.setDiagnostics(initialResult) }
                }
                onAnalyzed?.invoke(true)
            } catch (e: Exception) {
                System.out.println("KotlinLanguage: Error during kotlin analysis: ${e.message}")
            }
        }
    }

    private fun handleAnalysisReport(report: AnalysisReport) {
        val targetContainer = tempContainer ?: getDiagnosticsContainer()
        val severity: Short = when (report.severity) {
            CompilerMessageSeverity.ERROR -> 3
            CompilerMessageSeverity.WARNING, CompilerMessageSeverity.STRONG_WARNING -> 2
            else -> return
        }
        targetContainer.addDiagnostic(
            DiagnosticRegion(
                report.startOffset,
                report.endOffset,
                severity,
                0L,
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
                
                kotlinEnvironment.analyze(f, content)
                
                val dexSuccess = suspendCancellableCoroutine<Boolean> { continuation ->
                    CompilerUtils.compileDex(
                        inputDir = FileUtil.classesOutDir,
                        outputDir = FileUtil.dexOutDir,
                        // FIX: Correctly call extension function on the File object
                        classpath = FileUtil.classpathLibsDir.classPathFiles(),
                        library = listOf(FileUtil.androidJar),
                        compilationMode = CompilationMode.DEBUG,
                        outputMode = OutputMode.DexIndexed,
                        minApiLevel = 26
                    ) { _, exception ->
                        if (continuation.isActive) {
                            continuation.resume(exception == null)
                        }
                    }
                }
                
                tempContainer = null
                editorFragment.layoutLoading(!dexSuccess)
                
                newContainer
            }
        }
    }

    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        super.requireAutoComplete(content, position, publisher, extraArguments)
    }
}