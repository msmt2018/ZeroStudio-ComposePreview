package android.zero.studio.compose.preview.utils

import dalvik.system.BaseDexClassLoader
import java.io.File
import java.lang.reflect.Method

/**
 * 生产级动态类加载器。支持 Dex 热重载与类名隔离。
 * 
 * 工作流程线路图:
 * 1. 实例化: 接收 Dex 文件路径和父加载器。
 * 2. 注入路径: 利用反射调用 addDexPath 将 classes.dex.zip 注入。
 * 3. 优先加载 (findClass): 拦截加载请求，优先从当前 Dex 路径中查找类。
 * 4. 解决缓存: 每次编译完成后通过新建此实例来绕过虚拟机 Class 缓存。
 * 
 * 上下文关系:
 * - 由 EditorFragment 在 loadComposePreview 方法中调用。
 * 
 * @author android_zero
 */
class MultipleDexClassLoader(
    private val librarySearchPath: String? = null,
    parent: ClassLoader
) : BaseDexClassLoader("", null, librarySearchPath, parent) {

    private var addDexPathMethod: Method? = null

    init {
        try {
            // Android 8.0+ 的 addDexPath 方法名与签名
            addDexPathMethod = BaseDexClassLoader::class.java.getDeclaredMethod("addDexPath", String::class.java)
            addDexPathMethod?.isAccessible = true
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * 将生成的 Dex/Jar 路径动态添加到当前加载器的搜索路径中。
     */
    fun loadDex(dexFile: File) {
        if (!dexFile.exists()) return
        synchronized(this) {
            try {
                addDexPathMethod?.invoke(this, dexFile.absolutePath)
            } catch (e: Exception) {
                android.util.Log.e("ClassLoader", "Failed to add dex path: ${dexFile.absolutePath}", e)
            }
        }
    }

    /**
     * 核心加载逻辑重写。
     * 默认的委派模型是先父类后子类，但为了热更新，我们需要先从当前的动态 Dex 中查找类。
     */
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        // 对于用户编译的类（如 PlaygroundKt），我们优先尝试在当前 Dex 中查找
        // 这样可以确保代码修改后，加载的是最新的字节码
        var c = findLoadedClass(name)
        if (c == null) {
            try {
                // 尝试从当前动态添加的 Dex 路径加载
                c = findClass(name)
            } catch (e: ClassNotFoundException) {
                // 如果找不到，再委派给父类（加载 Material3, Compose Runtime 等宿主库）
                c = super.loadClass(name, resolve)
            }
        }
        
        if (resolve) {
            resolveClass(c)
        }
        return c!!
    }
}