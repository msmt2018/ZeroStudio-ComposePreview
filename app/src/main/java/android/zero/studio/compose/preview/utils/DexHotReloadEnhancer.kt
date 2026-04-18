package android.zero.studio.compose.preview.utils

import android.util.Log
import org.objectweb.asm.ClassReader
import org.smali.dexlib2.DexFileFactory
import org.smali.dexlib2.Opcodes
import java.io.File
import java.util.zip.ZipFile

/**
 * 使用 ASM + dexlib2 对热重载产物进行轻量增强分析：
 * 1) ASM 扫描 classes.jar 中的 @Composable 函数数量，形成布局快照元信息。
 * 2) dexlib2 预加载并校验 classes.dex.zip 的可读性，降低首次渲染抖动。
 *
 * 当前版本以“可观测增强”为主，不改写业务字节码，保证稳定性优先。
 */
object DexHotReloadEnhancer {

    data class EnhancementReport(
        val composableMethods: Int,
        val dexClasses: Int
    )

    fun enhance(
        compiledJar: File,
        dexZip: File,
        minApiLevel: Int
    ): EnhancementReport {
        val composableMethods = scanComposableMethods(compiledJar)
        val dexClasses = loadDexClassCount(dexZip, minApiLevel)
        Log.i(
            "DexHotReloadEnhancer",
            "ASM/dexlib2 ready: composableMethods=$composableMethods, dexClasses=$dexClasses"
        )
        return EnhancementReport(composableMethods, dexClasses)
    }

    private fun scanComposableMethods(compiledJar: File): Int {
        if (!compiledJar.exists()) return 0
        var count = 0
        ZipFile(compiledJar).use { zip ->
            zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.endsWith(".class") }
                .forEach { entry ->
                    zip.getInputStream(entry).use { input ->
                        val reader = ClassReader(input)
                        val cp = reader.className
                        val bytes = reader.b
                        if (cp.isNotEmpty() && bytes.isNotEmpty()) {
                            count += countComposableDescriptorHits(bytes)
                        }
                    }
                }
        }
        return count
    }

    /**
     * 通过常量池字符串命中 @Composable 描述符，避免引入复杂 Visitor 带来的额外分配。
     */
    private fun countComposableDescriptorHits(classBytes: ByteArray): Int {
        val marker = "Landroidx/compose/runtime/Composable;"
        return classBytes.decodeToString().windowed(marker.length, 1, false).count { it == marker }
    }

    private fun loadDexClassCount(dexZip: File, minApiLevel: Int): Int {
        if (!dexZip.exists()) return 0
        val dexFile = DexFileFactory.loadDexFile(dexZip, Opcodes.forApi(minApiLevel))
        return dexFile.classes.size
    }
}
