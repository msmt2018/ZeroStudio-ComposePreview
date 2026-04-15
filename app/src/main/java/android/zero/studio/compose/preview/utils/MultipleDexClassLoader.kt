package android.zero.studio.compose.preview.utils

import dalvik.system.BaseDexClassLoader
import dalvik.system.DexClassLoader
import java.io.File
import java.lang.reflect.Method

/**
 * 生产级动态类加载器。支持 Dex 热重载与类名隔离。
 *
 * 工作流程线路图:
 * 1. 实例化: 接收 Dex 文件路径和父加载器。
 * 2. 注入路径: 优先反射调用 addDexPath，将 classes.dex.zip 注入当前 BaseDexClassLoader。
 * 3. 兜底方案: 同时创建 DexClassLoader 作为兼容层，避免 addDexPath 在某些设备被限制。
 * 4. 优先加载 (loadClass): 先查动态 Dex，再委派父加载器。
 *
 * @author android_zero
 */
class MultipleDexClassLoader(
    private val librarySearchPath: String? = null,
    parent: ClassLoader
) : BaseDexClassLoader("", null, librarySearchPath, parent) {

    private var addDexPathMethod: Method? = null
    @Volatile
    private var fallbackDexLoader: DexClassLoader? = null

    init {
        addDexPathMethod = resolveAddDexPathMethod()
    }

    private fun resolveAddDexPathMethod(): Method? {
        val methodCandidates = listOf(
            arrayOf(String::class.java),
            arrayOf(String::class.java, Boolean::class.javaPrimitiveType)
        )

        for (params in methodCandidates) {
            val method = runCatching {
                BaseDexClassLoader::class.java.getDeclaredMethod("addDexPath", *params)
            }.getOrNull()
            if (method != null) {
                method.isAccessible = true
                return method
            }
        }
        return null
    }

    /**
     * 将生成的 Dex/Jar 路径动态添加到当前加载器的搜索路径中。
     */
    fun loadDex(dexFile: File) {
        if (!dexFile.exists()) return

        synchronized(this) {
            val optimizedDir = dexFile.parentFile ?: throw IllegalStateException("dex parent dir is null")
            if (!optimizedDir.exists()) optimizedDir.mkdirs()

            // 兼容层：始终创建 DexClassLoader，避免 addDexPath 在部分 ROM 上失效。
            fallbackDexLoader = DexClassLoader(
                dexFile.absolutePath,
                optimizedDir.absolutePath,
                librarySearchPath,
                parent
            )

            try {
                when (addDexPathMethod?.parameterTypes?.size) {
                    1 -> addDexPathMethod?.invoke(this, dexFile.absolutePath)
                    2 -> addDexPathMethod?.invoke(this, dexFile.absolutePath, false)
                }
            } catch (_: Throwable) {
                // 反射注入失败时由 fallbackDexLoader 负责加载。
            }
        }
    }

    /**
     * 核心加载逻辑重写。
     * 优先从动态 Dex 查找，再回退到父类加载器。
     */
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        var c = findLoadedClass(name)

        if (c == null) {
            c = tryLoadFromFallback(name)
        }

        if (c == null) {
            c = runCatching { findClass(name) }.getOrNull()
        }

        if (c == null) {
            c = super.loadClass(name, resolve)
        }

        if (resolve) {
            resolveClass(c)
        }
        return c
    }

    private fun tryLoadFromFallback(name: String): Class<*>? {
        val loader = fallbackDexLoader ?: return null
        return runCatching { loader.loadClass(name) }.getOrNull()
    }
}
