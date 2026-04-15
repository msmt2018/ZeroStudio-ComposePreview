package android.zero.studio.compose.preview.kotlin

import org.jetbrains.kotlin.com.intellij.openapi.editor.Document
import org.jetbrains.kotlin.psi.KtFile

/**
 * A wrapper for a Kotlin PSI file that includes a content hash for synchronization.
 * 
 * @property ktFile The IntelliJ PSI representation of the Kotlin file.
 * @property textHash The hash code of the text used to generate the PSI tree.
 * @author android_zero
 */
data class KtPsiFile(
    val ktFile: KtFile,
    internal var textHash: Int
) {
    /**
     * Calculates the absolute character offset for a given line and character position.
     * 
     * 工作流程:
     * 1. 从 KtFile 的 ViewProvider 中检索 Document 对象。
     * 2. 如果 Document 存在，获取指定行的起始偏移量。
     * 3. 返回起始偏移量与行内字符位置的和。
     * 
     * @param line The zero-based line index.
     * @param character The zero-based character index within the line.
     * @return The absolute character offset from the start of the file.
     */
    fun offsetFor(line: Int, character: Int): Int {
        val document: Document? = this.ktFile.viewProvider.document
        val lineStartOffset = if (document != null) {
            document.getLineStartOffset(line)
        } else {
            0
        }
        return lineStartOffset + character
    }
}