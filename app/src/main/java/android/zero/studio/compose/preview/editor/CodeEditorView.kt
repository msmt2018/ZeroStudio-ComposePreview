package android.zero.studio.compose.preview.editor

import android.content.Context
import android.util.AttributeSet
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow

/**
 * A customized code editor view that extends the base [CodeEditor].
 * This implementation pre-configures scrolling behavior and built-in components.
 * 
 * 工作流程:
 * 1. 调用父类构造函数进行 View 初始化。
 * 2. 在 init 块中开启拦截父布局横向滑动的属性。
 * 3. 检索并激活文本操作窗口组件（复制、粘贴等浮窗）。
 * 
 * @author android_zero
 */
class CodeEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : CodeEditor(context, attrs, defStyleAttr, defStyleRes) {

    init {
        // 1:1 Restoration of the initialization logic from decompiled source
        
        // Ensures that the editor can handle its own horizontal scrolling 
        // without being intercepted by parent views like ViewPager or Drawers.
        this.setInterceptParentHorizontalScrollIfNeeded(true)

        // Retrieves the built-in EditorTextActionWindow component and enables it.
        // This provides the context-sensitive UI for text manipulation (Copy, Paste, etc.)
        val textActionWindow = this.getComponent(EditorTextActionWindow::class.java) as EditorTextActionWindow
        textActionWindow.isEnabled = true
    }
}