package android.zero.studio.compose.preview.editor.analyzers

import android.zero.studio.compose.preview.editor.CodeEditorView
import android.zero.studio.compose.preview.ui.fragments.EditorFragment
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EventReceiver
import io.github.rosemoe.sora.event.Unsubscribe
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * 诊断分析触发器。监听编辑器文本变更，通过防抖机制调度编译行为。
 * 
 * 工作流程线路图:
 * 1. Sora Editor 输入变动 -> 触发 onReceive。
 * 2. 协程防抖 (Delay) -> 取消过期的短时输入分析，保留最后一次稳定期触发。
 * 3. UI 切换 -> 发送热编译状态指示。
 * 4. 委派执行 -> 将编译动作推至 KotlinAnalyzer 执行。
 * 5. 反馈绑定 -> 诊断信息回填给编辑器底层。
 * 
 * @author android_zero
 */
class DiagnosticAnalyzer(
    val editor: CodeEditorView,
    val file: File,
    private val editorFragment: EditorFragment // 引入 Fragment 以回调 UI 状态
) : EventReceiver<ContentChangeEvent> {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var analyzeJob: Job? = null
    private val compilationMutex = Mutex()

    private suspend fun doAnalyze() {
        val analyzer = KotlinAnalyzer(this.editor, this.file)
        analyzer.analyze()

        val container = analyzer.getDiagnosticsContainer()
        if (scope.isActive && container != null) {
            withContext(Dispatchers.Main) {
                val lang = analyzer.editorLanguage
                lang.setDiagnosticsContainer(container)
                editor.setDiagnostics(container)
            }
        }
    }

    override fun onReceive(event: ContentChangeEvent, unsubscribe: Unsubscribe) {
        analyzeJob?.cancel()
        analyzeJob = scope.launch {
            delay(1200) // 用户停止输入 1.2秒 后才触发完整编译，避免大量消耗 IO
            compilationMutex.withLock {
                try {
                    // 通知 UI 展现轻量级 "Loading..." 提示
                    withContext(Dispatchers.Main) {
                        editorFragment.layoutLoading(show = true, isHotReload = true)
                    }
                    
                    doAnalyze()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}