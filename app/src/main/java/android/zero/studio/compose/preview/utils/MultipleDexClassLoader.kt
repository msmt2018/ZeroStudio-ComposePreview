package android.zero.studio.compose.preview.utils

import dalvik.system.BaseDexClassLoader
import java.io.File
import java.lang.reflect.Method

/**
 * A custom ClassLoader that supports dynamic addition of DEX/JAR files.
 * 
 * @author android_zero
 */
class MultipleDexClassLoader(
    private val librarySearchPath: String? = null,
    classLoader: ClassLoader = ClassLoader.getSystemClassLoader()
) : BaseDexClassLoader("", null, librarySearchPath, classLoader) {

    private var addDexPathMethod: Method? = null
    private val ownPackagePrefix: String

    init {
        // Retrieve the hidden 'addDexPath' method via reflection
        try {
            addDexPathMethod = BaseDexClassLoader::class.java.getDeclaredMethod("addDexPath", String::class.java)
            addDexPathMethod?.isAccessible = true
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        // Determine the package prefix to avoid conflicts
        val pkg = this.javaClass.`package`
        ownPackagePrefix = if (pkg != null && pkg.name.isNotBlank()) "${pkg.name}." else ""
    }

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(this) {
            // Prevent self-loading conflicts for classes in the same package
            if (ownPackagePrefix.isNotEmpty() && name.startsWith(ownPackagePrefix)) {
                try {
                    return super.loadClass(name, resolve)
                } catch (e: ClassNotFoundException) {
                    // Fall through
                }
            }

            // Standard delegation or finding loaded classes
            var result = findLoadedClass(name)
            if (result == null) {
                try {
                    result = super.loadClass(name, resolve)
                } catch (e: ClassNotFoundException) {
                    result = findClass(name)
                }
            }

            if (resolve) {
                resolveClass(result)
            }
            return result!!
        }
    }

    /**
     * Loads a DEX file from the specified File object.
     */
    fun loadDex(dexFile: File) {
        loadDex(dexFile.absolutePath)
    }

    /**
     * Loads a DEX file from the specified path string.
     */
    fun loadDex(path: String) {
        synchronized(this) {
            val method = addDexPathMethod 
                ?: throw UnsupportedOperationException("addDexPath method unavailable on this platform")
            try {
                method.invoke(this, path)
            } catch (e: Exception) {
                throw RuntimeException("Failed to add dex path: $path", e)
            }
        }
    }

    companion object {
        val INSTANCE: MultipleDexClassLoader by lazy {
            MultipleDexClassLoader(null, MultipleDexClassLoader::class.java.classLoader!!)
        }
    }
}