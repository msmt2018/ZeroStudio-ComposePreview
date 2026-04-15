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
 * 应用程序单例日志记录器。负责捕获系统 logcat 输出并持久化到本地文件。
 * 
 * 工作流程线路图:
 * 1. 初始化 (initialize): 获取 Context 并创建输出文件。
 * 2. 启动监控 (start): 开启单线程池执行 logcat 命令。
 * 3. 实时解析 (`start$lambda$0`): 
 *    - 运行 `logcat -f` 命令将日志流重定向。
 *    - 利用正则表达式识别日志等级 (D/E/I/W)。
 * 4. 写入存储 (writeLogToFile): 将解析后的带有等级标记的日志追加到物理文件。
 * 
 * 上下文关系:
 * - 用于捕获 Kotlin 编译器前端与 R8 后端在执行过程中抛出的异常日志。
 * 
 * @author android_zero
 */
object AppLogger {
    private const val DEBUG = "DEBUG"
    private const val WARNING = "WARNING"
    private const val ERROR = "ERROR"
    private const val INFO = "INFO"
    private const val LOG_FILE_NAME = "compose_preview_logs.txt"

    private val TYPE_PATTERN: Pattern = Pattern.compile("^(.*\\d) ([ADEIW]) (.*): (.*)")

    private var mInitialized = false
    private lateinit var mContext: Context

    /**
     * 初始化日志系统。
     * @param context 全局上下文。
     */
    fun initialize(context: Context) {
        if (!mInitialized) {
            mInitialized = true
            mContext = context.applicationContext
            this.start()
        }
    }

    private fun start() {
        // 使用独立线程执行阻塞式的日志读取任务
        Executors.newSingleThreadExecutor().execute(::`start$lambda$0`)
    }

    @JvmStatic
    private fun `start$lambda$0`() {
        try {
            clear()
        } catch (e: IOException) {
            return
        }

        if (!::mContext.isInitialized) return

        try {
            val logFile = File(mContext.getExternalFilesDir(null), LOG_FILE_NAME)
            if (!logFile.exists()) logFile.createNewFile()

            // 启动 logcat 进程。-f 参数将输出保存到文件，本逻辑作为备份同时进行流读取
            val process = Runtime.getRuntime().exec("logcat -v time")
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            while (true) {
                val line = reader.readLine() ?: break
                val matcher = TYPE_PATTERN.matcher(line)
                
                if (matcher.matches()) {
                    val logLevelChar = matcher.group(2)
                    when (logLevelChar) {
                        "D" -> writeLogToFile(DEBUG, line)
                        "E" -> writeLogToFile(ERROR, line)
                        "I" -> writeLogToFile(INFO, line)
                        "W" -> writeLogToFile(WARNING, line)
                        else -> writeLogToFile(DEBUG, line)
                    }
                }
            }
        } catch (e: IOException) {
            // 运行时 IO 异常，静默处理以防影响主线程
        }
    }

    /**
     * 清理系统 logcat 缓冲区。
     */
    private fun clear() {
        try {
            Runtime.getRuntime().exec("logcat -c")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 将格式化的日志行写入本地文件。
     */
    private fun writeLogToFile(type: String, message: String) {
        if (!::mContext.isInitialized) return
        
        try {
            val logFile = File(mContext.getExternalFilesDir(null), LOG_FILE_NAME)
            FileWriter(logFile, true).use { writer ->
                writer.write("[$type] $message\n")
                writer.flush()
            }
        } catch (e: IOException) {
            // 忽略写入故障
        }
    }
}