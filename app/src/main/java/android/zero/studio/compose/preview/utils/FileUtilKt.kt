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

    val classpathJvmDir: File
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
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.widget.Toast
import android.content.Context
import androidx.compose.ui.platform.LocalContext

@Composable
fun HelloCompose() {
    Text("Hello Jetpack Compose!")
}
@Composable
fun ButtonExamples() {
    Column(
        modifier = Modifier
            .padding(48.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Filled button:")
        FilledButtonExample(onClick = { Log.d("Filled button", "Filled button clicked.") })

    }
}

@Composable
fun FilledButtonExample(onClick: () -> Unit) {
    Button(onClick = { onClick() }) { 

    val context = LocalContext.current

    Button(onClick = {

        val text = "Hello toast!"
        val duration = Toast.LENGTH_SHORT
        Toast.makeText(context, text, duration).show()
    }) {
        Text("Filled ")
    }
}
}

@Preview()
@Composable
fun Main() {
    ButtonExamples()
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
 * 
 * @param targetJar The output zip/jar file.
 * @author android_zero
 */
fun File.zipToJar(targetJar: File) {
    if (!this.exists() || !this.isDirectory) return
    
    // 如果目标文件存在则先删除，确保写入全新的内容
    if (targetJar.exists()) {
        targetJar.delete()
    }
    
    FileOutputStream(targetJar).use { fos ->
        ZipOutputStream(fos).use { zos ->
            this.walkTopDown()
                .filter { it.isFile }
                .filter { it.absolutePath != targetJar.absolutePath }
                .forEach { file ->
                    // 获取相对路径作为 Zip 内部的包结构 (例如: PlaygroundKt.class 或 META-INF/...)
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