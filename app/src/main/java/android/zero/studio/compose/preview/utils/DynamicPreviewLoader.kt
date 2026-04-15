package android.zero.studio.compose.preview.utils

import androidx.compose.runtime.*
import java.lang.reflect.Method

/**
 * 动态预览加载器：通过反射调用用户编译的 @Composable 函数。
 * 
 * @author android_zero
 */
class DynamicPreviewLoader(
    private val loader: ClassLoader,
    private val className: String,
    private val methodName: String
) {

    @Composable
    fun Render() {
        // 使用 remember 缓存 Method 引用，避免重组时重复反射，提升性能
        val methodResult = remember(loader, className, methodName) {
            runCatching {
                val clazz = loader.loadClass(className)
                // 查找目标方法
                // 注意：Compose 编译器会为函数添加参数，所以不能直接用 getMethod
                clazz.declaredMethods.find { it.name == methodName }
            }
        }

        val method = methodResult.getOrNull()
        
        if (method != null) {
            InvokeComposable(method)
        } else {
            // 如果没找到方法，可能正在编译中或者类名变更
            return
        }
    }

    /**
     * 执行反射调用。
     * Compose 编译器生成的函数通常具有以下签名：
     * static void YourFunction(Composer, int changed)
     */
    @Composable
    private fun InvokeComposable(method: Method) {
        val composer = currentComposer
        
        remember(method) {
            // 确保方法是可访问的
            method.isAccessible = true
        }

        try {
            // 计算参数数量
            val parameterCount = method.parameterTypes.size
            
            when (parameterCount) {
                0 -> {
                    // 异常情况：正常的 Composable 至少有两个参数
                    method.invoke(null)
                }
                2 -> {
                    // 标准情况：YourFunction(composer, changed)
                    // changed 传 0 代表需要重新计算，或者传入对应状态位
                    method.invoke(null, composer, 0)
                }
                else -> {
                    // 带有默认参数或其他参数的情况，根据 Compose 协议，
                    // 参数列表末尾会有额外的 mask 整数。
                    val args = arrayOfNulls<Any>(parameterCount)
                    args[parameterCount - 2] = composer
                    args[parameterCount - 1] = 0 // $changed
                    // 其余参数填充默认值（这取决于函数的默认参数实现）
                    method.invoke(null, *args)
                }
            }
        } catch (e: Exception) {
            // 渲染时可能发生用户代码异常，打印并显示在 Logcat
            e.printStackTrace()
        }
    }
}