package android.zero.studio.compose.preview.utils

import android.util.Log
import android.zero.studio.compose.preview.utils.esl.EslIncrementalProcessor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.File
import java.util.zip.ZipFile

object DexHotReloadEnhancer {
    data class EnhancementReport(
        val asmComposableMethods: Int,
        val dexComposableLikeMethods: Int,
        val eslRemappedTypes: Int
    )

    fun enhance(
        compiledJar: File,
        hotDexZip: File,
        baselineDexZip: File?,
        changedClassPrefixes: Set<String>,
        minApiLevel: Int
    ): EnhancementReport {
        val asmComposableMethods = scanComposableMethodsWithAsm(compiledJar)
        val dexComposableLikeMethods = EslIncrementalProcessor.countComposableLikeMethods(hotDexZip, minApiLevel)
        val eslRemappedTypes = if (baselineDexZip != null) {
            EslIncrementalProcessor.remapHotDexUsingBaseline(
                hotDexZip = hotDexZip,
                baselineDexZip = baselineDexZip,
                changedClassPrefixes = changedClassPrefixes,
                minApiLevel = minApiLevel
            )
        } else {
            0
        }
        Log.i(
            "DexHotReloadEnhancer",
            "hot-reload enhanced: asmComposable=$asmComposableMethods, dexComposableLike=$dexComposableLikeMethods, eslRemapped=$eslRemappedTypes"
        )
        return EnhancementReport(
            asmComposableMethods = asmComposableMethods,
            dexComposableLikeMethods = dexComposableLikeMethods,
            eslRemappedTypes = eslRemappedTypes
        )
    }

    private fun scanComposableMethodsWithAsm(compiledJar: File): Int {
        if (!compiledJar.exists()) return 0
        var count = 0
        ZipFile(compiledJar).use { zip ->
            zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.endsWith(".class") }
                .forEach { entry ->
                    zip.getInputStream(entry).use { input ->
                        val reader = ClassReader(input)
                        reader.accept(object : ClassVisitor(Opcodes.ASM9) {
                            override fun visitMethod(
                                access: Int,
                                name: String?,
                                descriptor: String?,
                                signature: String?,
                                exceptions: Array<out String>?
                            ): MethodVisitor {
                                return object : MethodVisitor(Opcodes.ASM9) {
                                    override fun visitAnnotation(descriptor: String?, visible: Boolean) {
                                        if (descriptor == "Landroidx/compose/runtime/Composable;") {
                                            count += 1
                                        }
                                    }
                                }
                            }
                        }, ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
                    }
                }
        }
        return count
    }
}
