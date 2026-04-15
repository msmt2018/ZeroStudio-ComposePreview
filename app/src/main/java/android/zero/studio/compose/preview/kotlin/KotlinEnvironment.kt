package android.zero.studio.compose.preview.kotlin

import android.zero.studio.compose.preview.utils.classPathFiles
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.addJvmSdkRoots
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import java.io.File
import java.lang.reflect.Field

/**
 * 封装了 Kotlin 编译器的核心环境，支持在 Android 运行时进行动态分析与字节码生成。
 * 
 * @author android_zero
 */
class KotlinEnvironment private constructor(private val configuration: CompilerConfiguration) {

    lateinit var core: KotlinCoreEnvironment
        internal set

    val cache: PsiFileCache by lazy { PsiFileCache(core.project) }

    private fun injectKtFiles() {
        try {
            val field: Field = KotlinCoreEnvironment::class.java.getDeclaredField("sourceFiles")
            field.isAccessible = true
            field.set(core, cache.ktFiles)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun prepare(file: File, code: String?) {
        if (code != null) cache.getOrUpdate(file.name, code) else cache.getOrUpdate(file)
        injectKtFiles()
    }

    fun analyze(file: File, code: String? = null) {
        prepare(file, code)
        KotlinToJVMBytecodeCompiler.analyze(core)
    }

    fun analyzeAndGenerate(file: File, code: String? = null) {
        prepare(file, code)
        // 关键：此方法不仅分析语法，还会根据配置的输出目录生成 .class 文件
        KotlinToJVMBytecodeCompiler.compileBunchOfSources(core)
    }

    companion object {
        @JvmStatic
        fun home(homeDir: File): Builder = Builder(homeDir)

        class Builder(private val homeDir: File) {
            private val configuration = CompilerConfiguration()
            private val pluginsJar = mutableListOf<String>()
            private var analysisReportListener: (AnalysisReport) -> Unit = { }

            fun withAnalysisReportListener(listener: (AnalysisReport) -> Unit): Builder {
                this.analysisReportListener = listener
                return this
            }

            fun withConfiguration(block: (CompilerConfiguration) -> Unit): Builder {
                block(this.configuration)
                return this
            }

            /**
             * 核心修复：添加 JVM 和 Libs 两个目录下的所有 JAR 作为依赖。
             */
            fun withJvmClasspathRoots(jars: List<File>): Builder {
                configuration.addJvmClasspathRoots(jars)
                return this
            }

            fun withJvmSdkRoots(jars: List<File>): Builder {
                configuration.addJvmSdkRoots(jars)
                return this
            }

            fun withPlugins(jars: List<File>): Builder {
                jars.forEach {
                    it.setReadOnly()
                    pluginsJar.add(it.absolutePath)
                }
                return this
            }

            fun create(): KotlinEnvironment {
                setIdeaIoUseFallback()
                setupIdeaStandaloneExecution()

                val env = KotlinEnvironment(configuration)

                configuration.put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, object : MessageCollector {
                    private var hasErrors: Boolean = false
                    override fun clear() { hasErrors = false }
                    override fun hasErrors(): Boolean = hasErrors
                    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
                        if (severity.isError) hasErrors = true
                        if (location != null) {
                            val cachedFile = env.cache.get(location.path.substringAfterLast("/"))
                            if (cachedFile != null) {
                                analysisReportListener(AnalysisReport(
                                    location.path,
                                    cachedFile.offsetFor(location.line - 1, location.column - 1),
                                    cachedFile.offsetFor(location.lineEnd - 1, location.columnEnd - 1),
                                    message, severity
                                ))
                            }
                        }
                    }
                })

                // 加载插件
                PluginCliParser.loadPluginsSafe(pluginsJar, emptyList(), emptyList(), configuration, object : Disposable { override fun dispose() {} })

                // 创建生产环境
                env.core = KotlinCoreEnvironment.createForProduction(
                    object : Disposable { override fun dispose() {} },
                    configuration,
                    EnvironmentConfigFiles.JVM_CONFIG_FILES
                )

                return env
            }
        }
    }
}