package android.zero.studio.compose.preview.editor.analyzers

import android.zero.studio.compose.preview.editor.CodeEditorView
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EventReceiver
import io.github.rosemoe.sora.event.Unsubscribe
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Handles content change events to perform asynchronous diagnostic analysis on Kotlin files.
 * It ensures that only one analysis runs at a time and updates the editor with current diagnostics.
 * 
 * @author android_zero
 */
class DiagnosticAnalyzer(
    val editor: CodeEditorView,
    val file: File
) : EventReceiver<ContentChangeEvent> {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var analyzeJob: Job? = null
    private val compilationMutex: Mutex = Mutex()

    private suspend fun analyze() {
        val analyzer = KotlinAnalyzer(this.editor, this.file)
        analyzer.analyze()

        val container = analyzer.getDiagnosticsContainer()
        if (scope.isActive && container != null) {
            editor.post {
                // FIX: This call now matches the restored method in KotlinLanguage
                analyzer.editorLanguage.setDiagnosticsContainer(container)
                editor.setDiagnostics(container)
            }
        }
    }

    override fun onReceive(event: ContentChangeEvent, unsubscribe: Unsubscribe) {
        analyzeJob?.cancel()
        analyzeJob = scope.launch {
            compilationMutex.withLock {
                analyze()
            }
        }
    }
}