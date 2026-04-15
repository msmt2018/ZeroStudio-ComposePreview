package android.zero.studio.compose.preview.utils

import androidx.compose.runtime.*
import java.lang.reflect.Method

/**
 * A utility class that dynamically loads a class and invokes a specified @Composable method via reflection.
 * This is primarily used for rendering previews from externally compiled DEX files.
 * 
 * 工作流程线路图:
 * 1. 初始化时存储 ClassLoader、类名和方法名。
 * 2. [Render] 函数作为 Composable 入口，启动重组范围。
 * 3. 利用 [remember] 缓存反射查找结果，避免在每次重组时重新加载类和查找方法。
 * 4. 查找到方法后，通过 [InvokeComposable] 执行反射调用。
 * 5. 反射调用遵循 Compose 的二进制规范，传入当前的 [Composer] 实例。
 * 
 * @property loader The custom ClassLoader containing the target DEX content.
 * @property className The fully qualified name of the class containing the Composable function.
 * @property methodName The name of the Composable function to render.
 * @author android_zero
 */
class DynamicPreviewLoader(
    private val loader: ClassLoader,
    private val className: String,
    private val methodName: String
) {

    /**
     * The main entry point for rendering the dynamic content.
     * 
     * 逻辑还原自 Render 字节码:
     * - 开启 RestartGroup。
     * - 检查实例是否变更。
     * - 执行 remember 块：尝试加载类 -> 遍历方法列表 -> 匹配 methodName -> 包装为 Result。
     * - 如果成功获取 Method 实例，则进入 InvokeComposable 逻辑。
     */
    @Composable
    fun Render() {
        // Restoration of the remember block with 3 keys as seen in bytecode (loader, className, methodName)
        val methodResult = remember(loader, className, methodName) {
            runCatching {
                val clazz = loader.loadClass(className)
                // Restoration of the 'find' logic over declaredMethods array
                val methods = clazz.declaredMethods
                methods.find { it.name == methodName }
            }
        }

        // Extract the method from the Result wrapper
        val method = methodResult.getOrNull()
        
        if (method != null) {
            // Restoration of the ReplaceGroup around the invocation
            InvokeComposable(method)
        }
    }

    /**
     * Performs the actual reflection call to the Composable function.
     * 
     * @param method The [Method] object representing the @Composable function.
     */
    @Composable
    private fun InvokeComposable(method: Method) {
        // Restoration of the InvokeComposable logic:
        // Static Composable functions are invoked with (null, composer, $changed_bits)
        val composer = currentComposer
        try {
            // 1:1 Restoration of method.invoke(null, var2, 0)
            method.invoke(null, composer, 0)
        } catch (e: Exception) {
            // Keep original behavior of catching and printing exceptions during dynamic invocation
            e.printStackTrace()
        }
    }
}