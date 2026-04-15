package android.zero.studio.compose.preview.kotlin.parser

import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

/**
 * A PSI visitor that collects all functions annotated with both @Composable and @Preview.
 *
 * @author android_zero
 */
class PreviewComposableVisitor : KtTreeVisitorVoid() {
    val functions = mutableListOf<KtNamedFunction>()

    override fun visitNamedFunction(function: KtNamedFunction) {
        // Use our helper to check for the required annotations
        PreviewComposableAnnotation.isPresent(function) {
            functions.add(it)
        }
        super.visitNamedFunction(function)
    }
}