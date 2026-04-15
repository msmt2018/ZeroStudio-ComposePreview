import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.HashMap

plugins {
    id("java")
}



// 定义依赖配置
val pluginToDex by configurations.creating
val d8Compiler by configurations.creating

dependencies {
    // 目标插件版本
    pluginToDex("org.jetbrains.kotlin:kotlin-compose-compiler-plugin-embeddable:2.2.20")
    // D8 编译器版本
    d8Compiler("com.android.tools:r8:8.13.19")
}

/**
 */
val dexComposePlugin = tasks.register<JavaExec>("dexComposePlugin") {
    group = "build"
    description = "Downloads Compose plugin, dexes it, and moves to assets."

    val outputDir = layout.buildDirectory.dir("dex-output").get().asFile
    val dexOutputDir = File(outputDir, "dexes")
    val targetAssetsDir = rootProject.file("app/src/main/assets/classpath/plugins")
    val pluginJar = configurations.getByName("pluginToDex").singleFile

    // 配置 JavaExec 的参数（用于运行 D8）
    this.classpath = configurations.getByName("d8Compiler")
    this.mainClass.set("com.android.tools.r8.D8")
    
    // 设置 D8 运行参数
    this.args(
        "--release",
        "--output", dexOutputDir.absolutePath,
        "--min-api", "26",
        pluginJar.absolutePath
    )

    // 在执行 D8 之前清理目录
    doFirst {
        if (outputDir.exists()) outputDir.deleteRecursively()
        dexOutputDir.mkdirs()
        if (!targetAssetsDir.exists()) targetAssetsDir.mkdirs()

        // 清理旧版本
        targetAssetsDir.listFiles()?.forEach { 
            if (it.name.contains("kotlin-compose-compiler-plugin-embeddable")) it.delete() 
        }
    }

    // 在 D8 执行完毕（生成 classes.dex）后，将其塞回 Jar 并放入 Assets
    doLast {
        println("✅ D8 compilation finished. Injecting DEX into JAR...")

        val finalJarFile = File(targetAssetsDir, "kotlin-compose-compiler-plugin-embeddable-2.2.20.jar")
        pluginJar.copyTo(finalJarFile, overwrite = true)

        // 使用 ZipFileSystem 处理 Jar 包内部文件
        val uri = URI.create("jar:${finalJarFile.toURI()}")
        val env = HashMap<String, String>()
        env["create"] = "false"

        try {
            FileSystems.newFileSystem(uri, env).use { fs: FileSystem ->
                val dexFiles = dexOutputDir.listFiles()?.filter { it.extension == "dex" }
                dexFiles?.forEach { dexFile ->
                    // 塞入 Jar 的根目录
                    val targetPathInZip = fs.getPath("/${dexFile.name}")
                    Files.copy(
                        dexFile.toPath(), 
                        targetPathInZip, 
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            }
            println("🎉 Successfully packaged: ${finalJarFile.absolutePath}")
        } catch (e: Exception) {
            throw GradleException("Failed to inject DEX into JAR: ${e.message}", e)
        }
    }
}