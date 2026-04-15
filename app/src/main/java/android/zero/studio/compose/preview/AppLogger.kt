package android.zero.studio.compose.preview

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.Executors
import java.util.regex.Pattern
import kotlin.jvm.internal.Intrinsics

/**
 * A singleton logging utility that captures system logcat output and persists it to a local file.
 * This logger monitors the process output, parses log levels via regex, and categorizes them.
 * 
 * @author android_zero
 */
object AppLogger {
    private const val DEBUG: String = "DEBUG"
    private const val WARNING: String = "WARNING"
    private const val ERROR: String = "ERROR"
    private const val INFO: String = "INFO"
    private const val LOG_FILE_NAME: String = "composeuilab_logs.txt"

    private val TYPE_PATTERN: Pattern = Pattern.compile("^(.*\\d) ([ADEIW]) (.*): (.*)")

    private var mInitialized: Boolean = false
    private lateinit var mContext: Context

    /**
     * Initializes the logger. Can only be called once.
     * 
     * @param context The context used to obtain the application context.
     */
    fun initialize(context: Context) {
        if (!mInitialized) {
            mInitialized = true
            mContext = context.applicationContext
            this.start()
        }
    }

    /**
     * Starts the logcat monitoring thread.
     * 
     * 修复说明:
     * 使用函数引用 ::`start$lambda$0` 代替匿名 Lambda 块，
     * 以避免生成的合成方法名与手动定义的 `start$lambda$0` 冲突。
     */
    private fun start() {
        // Restoration of AppLogger$$ExternalSyntheticLambda0
        Executors.newSingleThreadExecutor().execute(::`start$lambda$0`)
    }

    /**
     * Internal thread routine for logcat capture and parsing.
     * 1:1 Restoration of the complex control flow and exception handling from decompiled bytecode.
     */
    @JvmStatic
    private fun `start$lambda$0`() {
        try {
            // INSTANCE.clear()
            clear()
        } catch (var33: IOException) {
            error("IOException occurred on Logger: ${var33.message}")
            return
        }

        var context: Context
        try {
            context = mContext
        } catch (var32: IOException) {
            error("IOException occurred on Logger: ${var32.message}")
            return
        }

        // Manual lateinit check as seen in bytecode
        if (!::mContext.isInitialized) {
            try {
                Intrinsics.throwUninitializedPropertyAccessException("mContext")
            } catch (var31: IOException) {
                error("IOException occurred on Logger: ${var31.message}")
                return
            }
        } else {
            context = mContext
        }

        try {
            val logFile = File(context.getExternalFilesDir(null), LOG_FILE_NAME)
            logFile.createNewFile()

            val process = Runtime.getRuntime().exec("logcat -f ${logFile.absolutePath}")
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            while (true) {
                val line = try {
                    reader.readLine()
                } catch (var10: IOException) {
                    error("IOException occurred on Logger: ${var10.message}")
                    break
                } ?: break

                val matcher = TYPE_PATTERN.matcher(line)
                if (!matcher.matches()) {
                    continue
                }

                val logLevelChar = try {
                    matcher.group(2)
                } catch (var7: IOException) {
                    error("IOException occurred on Logger: ${var7.message}")
                    break
                }

                if (logLevelChar != null) {
                    // 1:1 Restoration of hash-based branch logic
                    when (logLevelChar) {
                        "D" -> debug(line)
                        "E" -> error(line)
                        "I" -> info(line)
                        "W" -> warning(line)
                        else -> debug(line)
                    }
                } else {
                    debug(line)
                }
            }
        } catch (var28: IOException) {
            error("IOException occurred on Logger: ${var28.message}")
        }
    }

    /**
     * Clears the current logcat buffer.
     */
    private fun clear() {
        try {
            Runtime.getRuntime().exec("logcat -c")
        } catch (var2: IOException) {
            this.error("IOException occurred while clearing logcat: ${var2.message}")
        }
    }

    private fun debug(message: String) {
        this.writeLogToFile("DEBUG", message)
    }

    private fun error(message: String) {
        this.writeLogToFile("ERROR", message)
    }

    private fun info(message: String) {
        this.writeLogToFile("INFO", message)
    }

    private fun warning(message: String) {
        this.writeLogToFile("WARNING", message)
    }

    /**
     * Writes log to file with manual property validation.
     */
    private fun writeLogToFile(type: String, message: String) {
        var context: Context
        try {
            if (!::mContext.isInitialized) {
                Intrinsics.throwUninitializedPropertyAccessException("mContext")
            }
            context = mContext
        } catch (var11: Exception) {
            // Simplified catch for the Intrinsic exception to prevent recursive error
            return
        }

        try {
            val logFile = File(context.getExternalFilesDir(null), LOG_FILE_NAME)
            val writer = FileWriter(logFile, true)
            writer.write("[$type] $message\n")
            writer.flush()
            writer.close()
        } catch (var6: IOException) {
            // Original logic logs error to stderr via this.error
            android.util.Log.e("AppLogger", "IOException occurred while writing log to file: ${var6.message}")
        }
    }
}