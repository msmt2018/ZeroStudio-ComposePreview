package android.zero.studio.compose.preview.kotlin.parser

import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

/**
 * Compose 预览函数访问者。
 * 用于在 Kotlin PSI 树中递归查找所有可预览的 Composable 函数。
 * 
 * @author android_zero
 */
class PreviewComposableVisitor : KtTreeVisitorVoid() {
    /** 存储扫描到的所有预览函数 */
    val functions = mutableListOf<KtNamedFunction>()

    override fun visitNamedFunction(function: KtNamedFunction) {
        // 执行注解判定
        PreviewComposableAnnotation.isPresent(function) {
            // 将符合条件的函数加入集合
            functions.add(it)
        }
        
        // 继续遍历子节点
        super.visitNamedFunction(function)
    }
}