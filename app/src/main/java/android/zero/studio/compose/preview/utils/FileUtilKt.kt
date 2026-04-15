package android.zero.studio.compose.preview.utils

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Singleton utility for managing project files and classpath directories.
 * Also provides file system and compression utilities essential for compilation.
 *
 * @author android_zero
 */
object FileUtil {
    private lateinit var mContext: Context

    val context: Context
        get() = mContext

    val kotlinHomeDir: File
        get() = File(context.getExternalFilesDir(null), "kotlin")

    val srcDir: File
        get() = File(context.getExternalFilesDir(null), "src")

    val classpathDir: File
        get() = File(context.getExternalFilesDir(null), "classpath")

    private val classpathJvmDir: File
        get() = File(classpathDir, "jvm")

    val androidJar: File
        get() = File(classpathJvmDir, "android.jar")

    val coreLambdaStubs: File
        get() = File(classpathJvmDir, "core-lambda-stubs.jar")

    val javaBaseJar: File
        get() = File(classpathJvmDir, "java-base.jar")

    val classpathLibsDir: File
        get() = File(classpathDir, "libs")

    val classpathPluginsDir: File
        get() = File(classpathDir, "plugins")

    val classesOutDir: File
        get() = File(context.getExternalFilesDir(null), "classes")

    val classesJarOut: File
        get() = File(classesOutDir, "classes.jar")

    val dexOutDir: File
        get() = File(context.getExternalFilesDir(null), "dex")

    val classesJarDex: File
        get() = File(dexOutDir, "classes.dex.zip")

    /**
     * The default playground code file. Created lazily.
     */
    val playgroundCode: File by lazy {
        File(srcDir, "Playground.kt").also {
            ensureFileCodeContent(it)
        }
    }

    /**
     * Initializes the FileUtil with the application context and creates necessary directories.
     */
    fun init(context: Context) {
        mContext = context.applicationContext
        
        listOf(
            kotlinHomeDir, classpathDir, classpathJvmDir, 
            classpathLibsDir, classpathPluginsDir, 
            classesOutDir, srcDir, dexOutDir
        ).forEach { dir ->
            if (!dir.exists()) dir.mkdirs()
        }
    }

    private fun ensureFileCodeContent(file: File) {
        if (!file.exists()) {
            file.createNewFile()
            file.writeText(
                """
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.*
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
@Composable
fun HelloCompose() {
    Text("Hello Jetpack Composes!")
        val tools =
        listOf(
            Triple(Icons.Default.Settings, Color(0x8F9FCCBC)) {
              
            },
            
        )
        
           LazyRow(
        horizontalArrangement = Arrangement.spacedBy(50.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
    ) {
      items(tools) { (icon, color, action) ->
        // 工具与服务按钮
        Surface(
            onClick = action,
            modifier = Modifier.size(42.dp),
            color = color,
            shape = RoundedCornerShape(10.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            // 工具与服务内部Icon的尺寸
            Icon(
                icon,
                null,
                tint = Color.DarkGray.copy(alpha = 1.8f),
                modifier = Modifier.size(54.dp),
            )
          }
        }
      }
    }
}

@Preview()
@Composable
fun Main() {
    HelloCompose()
}
                """.trimIndent()
            )
        }
    }
}

/**
 * Returns a list of all .jar files in the directory.
 */
fun File.classPathFiles(): List<File> = files("jar")

/**
 * Recursively lists files with the given extension.
 */
fun File.files(extension: String): List<File> {
    return if (exists()) {
        walkTopDown().filter { it.isFile && it.extension == extension }.toList()
    } else {
        emptyList()
    }
}

/**
 * Asynchronously deletes the file or directory.
 */
fun File.deleteFile(callback: ((Boolean) -> Unit)? = null) {
    CoroutineScope(Dispatchers.IO).launch {
        val success = deleteRecursively()
        callback?.invoke(success)
    }
}

/**
 * Zips the content of the current directory into the specified target jar/zip file.
 * This is crucial for D8 compilation, which prefers processing bundled class files.
 * 
 * @param targetJar The output zip/jar file.
 * @author android_zero
 */
fun File.zipToJar(targetJar: File) {
    if (!this.exists() || !this.isDirectory) return
    
    FileOutputStream(targetJar).use { fos ->
        ZipOutputStream(fos).use { zos ->
            this.walkTopDown().filter { it.isFile }.forEach { file ->
                val entryName = file.relativeTo(this).path.replace('\\', '/')
                val zipEntry = ZipEntry(entryName)
                zos.putNextEntry(zipEntry)
                FileInputStream(file).use { fis ->
                    fis.copyTo(zos)
                }
                zos.closeEntry()
            }
        }
    }
}