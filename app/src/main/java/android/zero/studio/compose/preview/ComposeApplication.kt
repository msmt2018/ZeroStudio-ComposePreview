package android.zero.studio.compose.preview

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy.Builder
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import android.zero.studio.compose.preview.provider.theme.ThemeProvider
import android.zero.studio.compose.preview.utils.* // FIX: Corrected import to include all top-level functions
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.Executors

/**
 * Main application class responsible for global configuration, theme management, 
 * and infrastructure setup such as extracting required compiler assets.
 * 
 * @author android_zero
 */
class ComposeApplication : Application() {

    override fun onCreate() {
        StrictMode.setThreadPolicy(Builder().permitAll().build())
        
        instance = this
        AppLogger.initialize(this)
        FileUtil.init(this)
        
        super.onCreate()
        
        setupTheme()
        extractFiles()
    }

    private fun setupTheme() {
        themeProvider = ThemeProvider(this)
        AppCompatDelegate.setDefaultNightMode(themeProvider.getTheme())
    }

    private fun extractFiles() {
        Executors.newSingleThreadExecutor().execute {
            listOf("jvm", "libs", "plugins").forEach { folderName ->
                val assetPath = "classpath/$folderName"
                val targetDir = File(FileUtil.classpathDir, folderName)
                targetDir.mkdirs()

                val assetFiles = try { assets.list(assetPath) } catch (e: Exception) { null }
                if (assetFiles != null) {
                    for (fileName in assetFiles) {
                        val fullAssetPath = "$assetPath/$fileName"
                        val targetFile = File(targetDir, fileName)
                        
                        if (!targetFile.exists() || assetNeedsUpdate(fullAssetPath, targetFile)) {
                            if (targetFile.exists()) {
                                targetFile.delete()
                            }
                            // FIX: Corrected call to extension function
                            extractFromAsset(fullAssetPath, targetFile)
                        }
                    }
                }
            }
        }
    }

    /**
     * Determines if a target file on disk needs to be updated based on its asset counterpart.
     * 1:1 Restoration of checksum comparison logic using idiomatic Kotlin.
     */
    fun assetNeedsUpdate(assetName: String, targetFile: File): Boolean {
        return try {
            targetFile.setWritable(true)
            targetFile.setReadable(true)

            // FIX: Replaced CloseableKt.closeFinally with idiomatic .use blocks
            val assetHash = assets.open(assetName).use { calculateChecksum(it) }
            val fileHash = FileInputStream(targetFile).use { calculateChecksum(it) }

            assetHash != fileHash
        } catch (e: Exception) {
            true
        }
    }

    companion object {
        lateinit var instance: ComposeApplication
            internal set

        lateinit var themeProvider: ThemeProvider
            internal set

        val sharedPreferences: SharedPreferences by lazy {
            PreferenceManager.getDefaultSharedPreferences(instance)
        }
    }
}