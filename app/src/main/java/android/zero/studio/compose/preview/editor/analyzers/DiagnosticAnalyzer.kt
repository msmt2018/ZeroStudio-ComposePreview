package android.zero.studio.compose.preview.editor.analyzers

import android.zero.studio.compose.preview.editor.CodeEditorView
import android.zero.studio.compose.preview.editor.language.KotlinLanguage
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EventReceiver
import io.github.rosemoe.sora.event.Unsubscribe
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * 诊断分析触发器。
 * 
 * @author android_zero
 */
class DiagnosticAnalyzer(
    val editor: CodeEditorView,
    val file: File
) : EventReceiver<ContentChangeEvent> {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var analyzeJob: Job? = null
    private val compilationMutex = Mutex()

    private suspend fun doAnalyze() {
        val analyzer = KotlinAnalyzer(this.editor, this.file)
        analyzer.analyze()

        val container = analyzer.getDiagnosticsContainer()
        if (scope.isActive && container != null) {
            editor.post {
                // ★ 修复点：确保正确调用 KotlinLanguage 的方法
                val lang = analyzer.editorLanguage
                lang.setDiagnosticsContainer(container)
                editor.setDiagnostics(container)
            }
        }
    }

    override fun onReceive(event: ContentChangeEvent, unsubscribe: Unsubscribe) {
        analyzeJob?.cancel()
        analyzeJob = scope.launch {
            delay(500)
            compilationMutex.withLock {
                try {
                    doAnalyze()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}