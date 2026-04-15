package android.zero.studio.compose.preview.kotlin.parser

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * An enumeration representing specific Kotlin Compose annotations required for preview detection.
 * 
 * 工作流程:
 * 1. 提供 "Composable" 和 "Preview" 两个核心注解定义的映射。
 * 2. 伴生对象中的 [isPresent] 方法用于静态扫描特定的 [KtNamedFunction]。
 * 3. 通过遍历函数的注解列表并匹配短名称来确定该函数是否为可预览的 Composable 函数。
 *
 * @property annotation The string representation of the annotation name.
 * @author android_zero
 */
enum class PreviewComposableAnnotation(val annotation: String) {
    /** Represents the @Composable annotation. */
    COMPOSABLE("Composable"),
    
    /** Represents the @Preview annotation. */
    PREVIEW("Preview");

    companion object {
        /**
         * Checks if the given Kotlin named function is annotated with both @Preview and @Composable.
         * 
         * 逻辑流程:
         * - 初始化预览 (hasPreview) 和可组合 (hasComposable) 标志位为 false。
         * - 遍历 function.getAnnotationEntries()。
         * - 获取每个注解的短名称。
         * - 如果匹配 "Preview"，则 hasPreview 置为 true。
         * - 如果匹配 "Composable"，则 hasComposable 置为 true。
         * - 循环结束后，若两个标志位均为 true，则调用 [onFound]。
         *
         * @param function The PSI element representing the function declaration to scan.
         * @param onFound A callback function invoked only if both required annotations are found.
         */
        fun isPresent(function: KtNamedFunction, onFound: (KtNamedFunction) -> Unit) {
            var hasPreview = false
            var hasComposable = false

            for (entry: KtAnnotationEntry in function.annotationEntries) {
                val shortName: Name? = entry.shortName
                if (shortName != null) {
                    val nameString: String? = shortName.asString()
                    if (nameString != null) {
                        // Check against the enum defined values
                        if (nameString == PREVIEW.annotation) {
                            hasPreview = true
                        } else if (nameString == COMPOSABLE.annotation) {
                            hasComposable = true
                        }
                    }
                }
            }

            // Only trigger the callback if the function is a Composable Preview
            if (hasPreview && hasComposable) {
                onFound.invoke(function)
            }
        }
    }
}