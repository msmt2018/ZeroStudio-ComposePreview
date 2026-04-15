package android.zero.studio.compose.preview.kotlin.parser

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Compose 预览注解识别枚举。
 * @author android_zero
 */
enum class PreviewComposableAnnotation(val annotation: String) {
    COMPOSABLE("Composable"),
    PREVIEW("Preview");

    companion object {
        /**
         * 静态判定给定的函数节点是否是一个合法的 Compose 预览入口。
         * 
         * @param function PSI 函数节点。
         * @param onFound 匹配成功时的回调。
         */
        fun isPresent(function: KtNamedFunction, onFound: (KtNamedFunction) -> Unit) {
            var hasPreview = false
            var hasComposable = false

            // 遍历 PSI 树中的注解条目
            for (entry: KtAnnotationEntry in function.annotationEntries) {
                val shortName: Name? = entry.shortName
                if (shortName != null) {
                    val nameString = shortName.asString()
                    // 匹配短名称，避免复杂的全限定名解析带来的性能损耗
                    if (nameString == PREVIEW.annotation) {
                        hasPreview = true
                    } else if (nameString == COMPOSABLE.annotation) {
                        hasComposable = true
                    }
                }
                
                // 如果两个条件都已满足，提前中断循环
                if (hasPreview && hasComposable) break
            }

            // 只有同时具备这两个注解的函数，才能作为 Preview 入口
            if (hasPreview && hasComposable) {
                onFound.invoke(function)
            }
        }
    }
}