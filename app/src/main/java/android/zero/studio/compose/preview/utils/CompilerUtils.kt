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
 * Utility for interacting with the D8 tool to compile class files into DEX format.
 * This class handles complex input types, log redirection, and output management.
 * 
 * 工作流程线路图:
 * 1. 确定输出文件路径：基于 outputDir 类型和文件名。
 * 2. 准备路径集合：将 Classpath, Library 和 Program 文件转换为 NIO Path。
 * 3. 构建 D8 指令：设置编译模式、API 级别和输出模式。
 * 4. 重定向输出流：捕获编译过程中的 StdOut 和 StdErr。
 * 5. 执行 D8.run() 并处理 CompilationFailedException。
 * 6. 还原流并回调结果。
 *
 * @author android_zero
 */
object CompilerUtils {

    /**
     * Compiles the provided input files into a Dalvik Executable (DEX) file.
     * 
     * @param inputDir The source files to compile. Can be [File], [String], [Path], or [List<File>].
     * @param outputDir The destination directory or file. Can be [File] or [String].
     * @param classpath A list of dependency JARs to include in the classpath.
     * @param library A list of library JARs (e.g., android.jar).
     * @param outputFileName The name of the resulting DEX zip file. Defaults to "classes.dex.zip".
     * @param compilationMode DEBUG or RELEASE mode.
     * @param outputMode DEX output strategy (e.g., DexIndexed).
     * @param minApiLevel Minimum Android API level supported by the resulting DEX.
     * @param callback Callback providing the log output and any caught [CompilationFailedException].
     */
    fun compileDex(
        inputDir: Any,
        outputDir: Any,
        classpath: List<File> = emptyList(),
        library: List<File> = emptyList(),
        outputFileName: String? = null,
        compilationMode: CompilationMode = CompilationMode.RELEASE,
        outputMode: OutputMode = OutputMode.DexIndexed,
        minApiLevel: Int = 26,
        callback: ((String?, CompilationFailedException?) -> Unit)? = null
    ) {
        // 1. Determine final output file location
        var finalOutputFile: File
        val defaultName = "classes.dex.zip"
        
        if (outputDir is File) {
            val name = outputFileName ?: defaultName
            finalOutputFile = File(outputDir, name)
        } else if (outputDir is String) {
            val baseDir = File(outputDir)
            val name = outputFileName ?: defaultName
            finalOutputFile = File(baseDir, name)
        } else {
            // Fallback for unexpected types
            finalOutputFile = File.createTempFile("classes.dex", ".zip")
        }

        // 2. Clean up and prepare directories
        if (finalOutputFile.exists()) {
            finalOutputFile.delete()
        }
        finalOutputFile.parentFile?.mkdirs()

        // 3. Convert File lists to java.nio.file.Path as required by D8 API
        val classpathPaths = ArrayList<Path>(classpath.size)
        for (f in classpath) {
            classpathPaths.add(f.toPath())
        }

        val libraryPaths = ArrayList<Path>(library.size)
        for (f in library) {
            libraryPaths.add(f.toPath())
        }

        // 4. Build D8 command
        val builder = D8Command.builder()
        builder.addClasspathFiles(classpathPaths)
        builder.addLibraryFiles(libraryPaths)

        // Handle various inputDir types for program files
        if (inputDir is File || inputDir is String || inputDir is Path) {
            val programFile = File(inputDir.toString())
            builder.addProgramFiles(listOf(programFile.toPath()))
        } else if (inputDir is List<*>) {
            // Verify if all elements are Files
            var allFiles = true
            for (item in inputDir) {
                if (item !is File) {
                    allFiles = false
                    break
                }
            }
            if (allFiles) {
                val programPaths = ArrayList<Path>(inputDir.size)
                for (item in inputDir) {
                    programPaths.add((item as File).toPath())
                }
                builder.addProgramFiles(programPaths)
            }
        }

        builder.setMinApiLevel(minApiLevel)
        builder.setMode(compilationMode)
        builder.setOutput(finalOutputFile.toPath(), outputMode)

        // 5. Execution with log redirection
        val outBaos = ByteArrayOutputStream()
        val errBaos = ByteArrayOutputStream()
        val originalOut = System.`out`
        val originalErr = System.`err`
        
        var caughtException: CompilationFailedException? = null

        try {
            // Redirect system streams to capture logs
            System.setOut(PrintStream(outBaos))
            System.setErr(PrintStream(errBaos))

            // Execute the compilation
            D8.run(builder.build())

        } catch (e: CompilationFailedException) {
            caughtException = e
        } finally {
            // 6. Restore original streams regardless of success or failure
            System.setErr(originalErr)
            System.setOut(originalOut)
        }

        // 7. Invoke callback with logs and potential exception
        if (callback != null) {
            if (caughtException == null) {
                // Success: return captured normal output
                callback.invoke(outBaos.toString(), null)
            } else {
                // Failure: return captured error output and the exception
                callback.invoke(errBaos.toString(), caughtException)
            }
        }
    }
}