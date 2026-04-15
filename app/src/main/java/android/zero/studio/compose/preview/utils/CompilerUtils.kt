package android.zero.studio.compose.preview.utils

import com.android.tools.r8.CompilationFailedException
import com.android.tools.r8.CompilationMode
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Path
import java.util.ArrayList

/**
 * 编译器工具类：封装 Google D8 编译器，用于将字节码 (.class) 转换为 Dalvik 字节码 (.dex)。
 * 
 * @author android_zero
 */
object CompilerUtils {

    /**
     * 将输入的类文件或 Jar 编译为 Dex 压缩包。
     * 
     * @param inputDir 输入源，建议为打包好的 .jar 文件。
     * @param outputDir 输出目录。
     * @param classpath 编译所需的依赖项列表（如 Material3, Compose UI 库）。
     * @param library 平台库（通常是 android.jar）。
     * @param minApiLevel 目标最小 API 级别，建议 26 (Android 8.0)。
     */
    fun compileDex(
        inputDir: Any,
        outputDir: Any,
        classpath: List<File> = emptyList(),
        library: List<File> = emptyList(),
        outputFileName: String? = null,
        compilationMode: CompilationMode = CompilationMode.DEBUG,
        outputMode: OutputMode = OutputMode.DexIndexed,
        minApiLevel: Int = 26,
        callback: ((String?, CompilationFailedException?) -> Unit)? = null
    ) {
        val finalOutputFile = if (outputDir is File) {
            File(outputDir, outputFileName ?: "classes.dex.zip")
        } else {
            File(outputDir.toString(), outputFileName ?: "classes.dex.zip")
        }

        // 清理旧产物
        if (finalOutputFile.exists()) finalOutputFile.delete()
        finalOutputFile.parentFile?.mkdirs()

        val builder = D8Command.builder()

        // 1. 添加 Program 文件 (即我们要编译的目标类)
        when (inputDir) {
            is File -> builder.addProgramFiles(inputDir.toPath())
            is String -> builder.addProgramFiles(File(inputDir).toPath())
            is List<*> -> builder.addProgramFiles(inputDir.filterIsInstance<File>().map { it.toPath() })
        }

        // 2. 添加 Classpath 和 Library (不参与编译，但用于符号解析)
        builder.addClasspathFiles(classpath.map { it.toPath() })
        builder.addLibraryFiles(library.map { it.toPath() })

        // 3. 配置编译参数
        builder.setMinApiLevel(minApiLevel)
            .setMode(compilationMode)
            .setOutput(finalOutputFile.toPath(), outputMode)

        // 4. 重定向输出并执行
        val outBaos = ByteArrayOutputStream()
        val errBaos = ByteArrayOutputStream()
        val originalOut = System.`out`
        val originalErr = System.`err`
        
        var caughtException: CompilationFailedException? = null

        try {
            System.setOut(PrintStream(outBaos))
            System.setErr(PrintStream(errBaos))

            D8.run(builder.build())

        } catch (e: CompilationFailedException) {
            caughtException = e
        } finally {
            System.setErr(originalErr)
            System.setOut(originalOut)
        }

        callback?.invoke(if (caughtException == null) outBaos.toString() else errBaos.toString(), caughtException)
    }
}