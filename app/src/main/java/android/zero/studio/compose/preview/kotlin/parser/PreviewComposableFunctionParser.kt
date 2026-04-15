package android.zero.studio.compose.preview.kotlin.parser

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Parser for identifying @Preview Composable functions within a Kotlin PSI file.
 *
 * @author android_zero
 */
class PreviewComposableFunctionParser private constructor(private val visitor: PreviewComposableVisitor) {

    /**
     * Returns the list of identified previewable functions.
     */
    fun parse(): List<KtNamedFunction> = visitor.functions

    companion object {
        /**
         * Initializes a parser by running a visitor over the provided [KtFile].
         */
        fun initialize(ktFile: KtFile): PreviewComposableFunctionParser {
            val visitor = PreviewComposableVisitor()
            ktFile.accept(visitor)
            return PreviewComposableFunctionParser(visitor)
        }
    }
}