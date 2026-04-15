package android.zero.studio.compose.preview.editor.analyzers

import android.zero.studio.compose.preview.editor.CodeEditorView
import android.zero.studio.compose.preview.editor.language.KotlinLanguage
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import java.io.File

/**
 * Encapsulates the logic for analyzing Kotlin source code within the editor context.
 * This class coordinates between the editor's text content and the Kotlin language support.
 * 
 * @property editor The specific code editor view instance.
 * @property file The physical file associated with the editor content.
 * @author android_zero
 */
class KotlinAnalyzer(val editor: CodeEditorView, val file: File) {

    /**
     * Stores the result of the latest analysis.
     */
    private var newContainer: DiagnosticsContainer? = null

    /**
     * Lazy-initialized reference to the Kotlin language support.
     * 1:1 Restoration of the lazy delegate and casting logic from the decompiled source.
     */
    val editorLanguage: KotlinLanguage by lazy {
        // Re-sugar KotlinAnalyzer$$ExternalSyntheticLambda0
        val lang = editor.editorLanguage
        lang as KotlinLanguage
    }

    /**
     * Triggers the asynchronous analysis process.
     * It captures the current editor text and invokes the Kotlin language analyzer.
     */
    suspend fun analyze() {
        // Retrieve the language support instance
        val lang = editorLanguage
        
        // Capture the current text from the editor
        val content = editor.text.toString()
        
        // Execute the analysis and capture the diagnostics container
        // Re-sugar logic from KotlinAnalyzer$analyze$1 state machine
        val result = lang.analyze(file, content)
        
        // Store the new diagnostics result
        newContainer = result
    }

    /**
     * Returns the diagnostics container generated during the last successful analysis.
     */
    fun getDiagnosticsContainer(): DiagnosticsContainer? {
        return newContainer
    }

    /**
     * Clears current diagnostic results.
     */
    fun reset() {
        newContainer = null
    }
}