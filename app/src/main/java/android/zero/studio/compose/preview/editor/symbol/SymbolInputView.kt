package android.zero.studio.compose.preview.editor.symbol

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.zero.studio.compose.preview.databinding.SymbolButtonBinding
import android.zero.studio.compose.preview.editor.CodeEditorView
import androidx.viewbinding.ViewBinding
import kotlin.jvm.internal.Intrinsics

/**
 * A horizontal input panel that provides quick access to common programming symbols.
 * This view binds to a [CodeEditorView] and handles text insertion and snippet navigation.
 *
 * @author android_zero
 */
class SymbolInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private lateinit var codeEditor: CodeEditorView

    init {
        orientation = HORIZONTAL
    }

    fun bindEditor(editor: CodeEditorView) {
        this.codeEditor = editor
    }

    fun addEditorSymbols(display: Array<String>, insertText: Array<String>) {
        val count = maxOf(display.size, insertText.size)
        val inflater = LayoutInflater.from(this.context)

        for (i in 0 until count) {
            val binding = SymbolButtonBinding.inflate(inflater, this, false)
            val button = binding.root
            
            button.text = display[i]
            
            button.setOnClickListener {
                if (!::codeEditor.isInitialized) {
                    Intrinsics.throwUninitializedPropertyAccessException("codeEditor")
                }

                if (codeEditor.isEditable) {
                    val textToInsert = insertText[i]
                    
                    if ("\t" == textToInsert) {
                        if (codeEditor.snippetController.isInSnippet()) {
                            codeEditor.snippetController.shiftToNextTabStop()
                        } else {
                            codeEditor.indentOrCommitTab()
                        }
                    } else {
                        codeEditor.insertText(textToInsert, 1)
                    }
                }
            }
            
            this.addView(button)
        }
    }
}