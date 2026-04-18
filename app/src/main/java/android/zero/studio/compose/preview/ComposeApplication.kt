package android.zero.studio.compose.preview

import android.app.Application
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import android.zero.studio.compose.preview.provider.theme.ThemeProvider
import android.zero.studio.compose.preview.utils.*
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 应用程序主入口。负责配置 StrictMode、主题管理以及核心编译器资产的自动部署。
 * 
 * 工作流程线路图:
 * 1. 基础环境初始化: 启动日志系统 (AppLogger) 并映射文件系统路径 (FileUtil)。
 * 2. 资产同步 (deployCompilerAssets): 
 *    - 遍历 Assets 中的编译器依赖 (jvm, libs, plugins)。
 *    - 检查 targetFile 的存在性与 Hash 校验。
 *    - 调用 extractFromAsset 将 JAR 部署到私有存储目录。
 * 3. 编译器就绪: 资产部署完成后，后续的 KotlinLanguage 才能正确执行符号分析。
 * 
 * @author android_zero
 */
class ComposeApplication : Application() {

    override fun onCreate() {
        // 允许在开发阶段进行主线程文件操作以简化初始化流程
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
        
        instance = this
        AppLogger.initialize(this)
        FileUtil.init(this)
        
        super.onCreate()
        
        setupTheme()
        deployCompilerAssets()
    }

    private fun setupTheme() {
        themeProvider = ThemeProvider(this)
        AppCompatDelegate.setDefaultNightMode(themeProvider.getTheme())
    }

    /**
     * 自动部署编译器运行时所需的资产文件（JAR/ZIP）。
     * 目标目录包括：classpath/jvm (基础库) 和 classpath/libs (Compose 运行时)。
     */
    private fun deployCompilerAssets() {
        Executors.newSingleThreadExecutor().execute {
            var success = true
            // 定义需要从 Assets 同步到本地存储的文件夹
            val assetFolders = listOf("jvm", "libs", "plugins")
            
            assetFolders.forEach { folderName ->
                val assetPath = "classpath/$folderName"
                val targetDir = File(FileUtil.classpathDir, folderName)
                
                // 确保目标目录存在
                if (!targetDir.exists()) targetDir.mkdirs()

                // 列出 Assets 下的文件
                val assetFiles = try { assets.list(assetPath) } catch (e: Exception) { null }
                
                assetFiles?.forEach { fileName ->
                    val fullAssetPath = "$assetPath/$fileName"
                    val targetFile = File(targetDir, fileName)
                    
                    // 核心逻辑：只有在必要时才更新文件，防止冷启动时的 IO 性能损耗
                    if (!targetFile.exists() || assetNeedsUpdate(fullAssetPath, targetFile)) {
                        try {
                            // 执行从 Assets 到磁盘的流复制
                            extractFromAsset(fullAssetPath, targetFile)
                        } catch (e: Exception) {
                            success = false
                            android.util.Log.e("App", "Failed to deploy: $fileName")
                        }
                    }
                }
            }

            compilerAssetsReady.complete(success)
        }
    }

    /**
     * 通过 SHA-256 算法对比文件内容。
     * 确保即使 Assets 中的 Jar 包更新了（例如升级了 Compose 库），本地文件也能同步更新。
     */
    private fun assetNeedsUpdate(assetName: String, targetFile: File): Boolean {
        return try {
            val assetHash = assets.open(assetName).use { calculateChecksum(it) }
            val fileHash = FileInputStream(targetFile).use { calculateChecksum(it) }
            assetHash != fileHash
        } catch (e: Exception) {
            true // 发生异常时强制更新，确保可靠性
        }
    }

    companion object {
        private val compilerAssetsReady = CompletableFuture<Boolean>()

        lateinit var instance: ComposeApplication
            internal set

        lateinit var themeProvider: ThemeProvider
            internal set

        val sharedPreferences by lazy {
            PreferenceManager.getDefaultSharedPreferences(instance)
        }

        /**
         * 等待编译资产（classpath/jvm, libs, plugins）部署完成。
         * 解决首次安装时 classpath 尚未落盘导致的 import 解析失败问题。
         */
        fun awaitCompilerAssets(timeoutSeconds: Long = 30): Boolean {
            return try {
                compilerAssetsReady.get(timeoutSeconds, TimeUnit.SECONDS)
            } catch (_: Exception) {
                false
            }
        }
    }
}
