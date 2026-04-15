package android.zero.studio.compose.preview.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import java.lang.reflect.Method
import java.lang.reflect.Modifier

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
        val methodResult = remember(loader, className, methodName) {
            runCatching {
                val clazz = loader.loadClass(className)
                resolveComposableMethod(clazz)
            }
        }

        val method = methodResult.getOrNull()
        if (method != null) {
            invokeComposable(method)
        }
    }

    /**
     * 在目标类中选择最匹配的 Compose 编译产物方法。
     *
     * 关键点：同名方法可能存在多个重载（含 `$default` / 合成桥接）。
     * 我们优先选择「包含 Composer 参数」且参数数量最多的那个，避免误命中普通方法。
     */
    private fun resolveComposableMethod(clazz: Class<*>): Method? {
        return clazz.declaredMethods
            .asSequence()
            .filter { it.name == methodName }
            .filter { method ->
                method.parameterTypes.any { it.name == "androidx.compose.runtime.Composer" }
            }
            .sortedByDescending { it.parameterCount }
            .firstOrNull()
    }

    /**
     * 执行反射调用。
     * Compose 编译器生成的方法参数通常包含：
     * - 业务参数（如果有）
     * - Composer
     * - 若干 Int（changed/default mask）
     */
    @Composable
    private fun invokeComposable(method: Method) {
        val composer = currentComposer

        remember(method) {
            method.isAccessible = true
        }

        try {
            val parameterTypes = method.parameterTypes
            val args = Array<Any?>(parameterTypes.size) { index ->
                defaultArgForType(parameterTypes[index])
            }

            parameterTypes.forEachIndexed { index, type ->
                when {
                    type.name == "androidx.compose.runtime.Composer" -> args[index] = composer
                    type == Int::class.javaPrimitiveType -> args[index] = 0
                }
            }

            val receiver = if (Modifier.isStatic(method.modifiers)) {
                null
            } else {
                resolveInstance(method.declaringClass)
            }

            method.invoke(receiver, *args)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun resolveInstance(clazz: Class<*>): Any? {
        return runCatching {
            val instanceField = clazz.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            instanceField.get(null)
        }.getOrNull()
    }

    private fun defaultArgForType(type: Class<*>): Any? {
        return when (type) {
            java.lang.Boolean.TYPE -> false
            java.lang.Byte.TYPE -> 0.toByte()
            java.lang.Short.TYPE -> 0.toShort()
            java.lang.Integer.TYPE -> 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0f
            java.lang.Double.TYPE -> 0.0
            java.lang.Character.TYPE -> '\u0000'
            else -> null
        }
    }
}
