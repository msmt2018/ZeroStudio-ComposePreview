package android.zero.studio.compose.preview.kotlin

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
import java.io.File
import java.lang.reflect.Field

/**
 * Manages the Kotlin compiler environment for static analysis and bytecode generation.
 * This class handles the initialization of the Kotlin Core Environment and provides
 * methods to trigger compilation and analysis.
 *
 * @property configuration The internal compiler configuration.
 * @author android_zero
 */
class KotlinEnvironment private constructor(private val configuration: CompilerConfiguration) {

    /** The actual Kotlin core environment used by the compiler components. */
    lateinit var core: KotlinCoreEnvironment
        internal set

    /** 
     * Lazy cache for PSI files. 
     */
    val cache: PsiFileCache by lazy {
        PsiFileCache(core.project)
    }

    /**
     * Injects the current cached Kotlin files into the compiler's internal source set.
     */
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
        if (code != null) {
            val name = file.name
            cache.getOrUpdate(name, code)
        } else {
            cache.getOrUpdate(file)
        }
        injectKtFiles()
    }

    /**
     * Performs static analysis on the specified file.
     */
    fun analyze(file: File, code: String? = null) {
        prepare(file, code ?: file.readText())
        KotlinToJVMBytecodeCompiler.analyze(core)
    }

    /**
     * Compiles the specified file and generates JVM bytecode.
     */
    fun analyzeAndGenerate(file: File, code: String? = null) {
        prepare(file, code ?: file.readText())
        KotlinToJVMBytecodeCompiler.compileBunchOfSources(core)
    }

    companion object {
        /**
         * Creates a builder to configure and instantiate a [KotlinEnvironment].
         */
        @JvmStatic
        fun home(homeDir: File): Builder {
            return Builder(homeDir)
        }

        /**
         * Builder class for [KotlinEnvironment].
         */
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

                // FIX: Use put with MESSAGE_COLLECTOR_KEY instead of unresolved setMessageCollector
                configuration.put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, object : MessageCollector {
                    private var hasErrors: Boolean = false

                    override fun clear() {
                        hasErrors = false
                    }

                    override fun hasErrors(): Boolean = hasErrors

                    override fun report(
                        severity: CompilerMessageSeverity,
                        message: String,
                        location: CompilerMessageSourceLocation?
                    ) {
                        if (!hasErrors && severity.isError) {
                            hasErrors = true
                        }

                        if (location != null) {
                            val path = location.path
                            val cacheKey = if (path.startsWith("/")) path.substring(1) else path
                            val cachedFile = env.cache.get(cacheKey)
                            
                            if (cachedFile != null) {
                                val report = AnalysisReport(
                                    path = path,
                                    startOffset = cachedFile.offsetFor(location.line - 1, location.column - 1),
                                    endOffset = cachedFile.offsetFor(location.lineEnd - 1, location.columnEnd - 1),
                                    message = message,
                                    severity = severity
                                )
                                analysisReportListener.invoke(report)
                            }
                        }
                    }
                })

                // FIX: Align with 5-parameter signature: (Paths, Options, Configs, Configuration, Disposable)
                PluginCliParser.loadPluginsSafe(
                    pluginsJar,
                    emptyList<String>(),
                    emptyList<String>(),
                    configuration,
                    object : Disposable {
                        override fun dispose() {}
                    }
                )

                env.core = KotlinCoreEnvironment.createForProduction(
                    object : Disposable {
                        override fun dispose() {}
                    },
                    configuration,
                    EnvironmentConfigFiles.JVM_CONFIG_FILES
                )

                return env
            }
        }
    }
}