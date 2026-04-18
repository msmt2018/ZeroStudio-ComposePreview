package android.zero.studio.compose.preview.utils.esl

import org.smali.dexlib2.DexFileFactory
import org.smali.dexlib2.Opcodes
import org.smali.dexlib2.iface.ClassDef
import org.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import org.smali.dexlib2.iface.instruction.ReferenceInstruction
import org.smali.dexlib2.iface.instruction.WideLiteralInstruction
import org.smali.dexlib2.iface.reference.FieldReference
import org.smali.dexlib2.iface.reference.MethodReference
import org.smali.dexlib2.iface.reference.TypeReference
import org.smali.dexlib2.rewriter.DexRewriter
import org.smali.dexlib2.rewriter.Rewriter
import org.smali.dexlib2.rewriter.RewriterModule
import org.smali.dexlib2.rewriter.Rewriters
import org.smali.dexlib2.rewriter.TypeRewriter
import org.smali.dexlib2.writer.io.FileDataStore
import org.smali.dexlib2.writer.pool.DexPool
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object EslIncrementalProcessor {
    private const val ESL_MARKER = "\$\$ExternalSyntheticLambda"
    private const val ESL_PLACEHOLDER = "ESL_PLACEHOLDER"
    private val dexEntryRegex = Regex("classes\\d*\\.dex")

    fun remapHotDexUsingBaseline(
        hotDexZip: File,
        baselineDexZip: File,
        changedClassPrefixes: Set<String>,
        minApiLevel: Int
    ): Int {
        if (!hotDexZip.exists() || !baselineDexZip.exists()) return 0
        val hotDex = extractPrimaryDex(hotDexZip) ?: return 0
        val baselineDex = extractPrimaryDex(baselineDexZip) ?: return 0

        return try {
            val baselinePrints = buildFingerprintsFromDex(baselineDex, minApiLevel)
            val hotPrints = buildFingerprintsFromDex(hotDex, minApiLevel)
            val remapping = buildRemapping(baselinePrints, hotPrints, changedClassPrefixes)
            if (remapping.isEmpty()) return 0

            rewriteDexTypes(hotDex, remapping, minApiLevel)
            replacePrimaryDexEntry(hotDexZip, hotDex)
            remapping.size
        } finally {
            hotDex.delete()
            baselineDex.delete()
        }
    }

    fun countComposableLikeMethods(hotDexZip: File, minApiLevel: Int): Int {
        val hotDex = extractPrimaryDex(hotDexZip) ?: return 0
        return try {
            val dex = DexFileFactory.loadDexFile(hotDex, Opcodes.forApi(minApiLevel))
            dex.classes.sumOf { classDef ->
                classDef.methods.count { method ->
                    method.annotations.any { it.type == "Landroidx/compose/runtime/Composable;" } ||
                        method.name.contains("composable", ignoreCase = true)
                }
            }
        } finally {
            hotDex.delete()
        }
    }

    private fun buildFingerprintsFromDex(
        dexFile: File,
        minApiLevel: Int
    ): Map<EslFingerprint, MutableList<String>> {
        val result = linkedMapOf<EslFingerprint, MutableList<String>>()
        val dex = DexFileFactory.loadDexFile(dexFile, Opcodes.forApi(minApiLevel))
        dex.classes.forEach { classDef ->
            val descriptor = classDef.type
            if (descriptor.contains("ExternalSyntheticLambda")) {
                val fingerprint = buildSingleFingerprint(classDef) ?: return@forEach
                result.getOrPut(fingerprint) { mutableListOf() }.add(descriptor)
            }
        }
        return result
    }

    private fun buildSingleFingerprint(classDef: ClassDef): EslFingerprint? {
        val descriptor = classDef.type
        val eslIndex = descriptor.indexOf(ESL_MARKER)
        if (eslIndex < 0) return null
        val outerClass = descriptor.substring(0, eslIndex) + ";"
        val interfaces = classDef.interfaces.map { it.toString() }
        val constructorParams = classDef.methods.firstOrNull { it.name == "<init>" }
            ?.parameterTypes?.map { it.toString() }
            ?: emptyList()
        val bytecodeHash = computeNormalizedBytecodeHash(classDef)
        return EslFingerprint(outerClass, interfaces, constructorParams, bytecodeHash)
    }

    private fun buildRemapping(
        baselineFingerprints: Map<EslFingerprint, List<String>>,
        hotFingerprints: Map<EslFingerprint, List<String>>,
        changedClassPrefixes: Set<String>
    ): Map<String, String> {
        val remapping = linkedMapOf<String, String>()
        hotFingerprints.forEach { (fingerprint, hotDescriptors) ->
            if (!belongsToChangedFiles(hotDescriptors, changedClassPrefixes)) return@forEach
            val baselineDescriptors = baselineFingerprints[fingerprint] ?: return@forEach
            if (hotDescriptors.size != baselineDescriptors.size) return@forEach

            val sortedHot = hotDescriptors.sortedBy { extractEslNumber(it) }
            val sortedBase = baselineDescriptors.sortedBy { extractEslNumber(it) }
            sortedHot.indices.forEach { idx ->
                if (sortedHot[idx] != sortedBase[idx]) {
                    remapping[sortedHot[idx]] = sortedBase[idx]
                }
            }
        }
        return remapping
    }

    private fun rewriteDexTypes(
        dexFile: File,
        remapping: Map<String, String>,
        minApiLevel: Int
    ) {
        val originalDex = DexFileFactory.loadDexFile(dexFile, Opcodes.forApi(minApiLevel))
        val rewriter = DexRewriter(object : RewriterModule() {
            override fun getTypeRewriter(rewriters: Rewriters): Rewriter<String> {
                return object : TypeRewriter() {
                    override fun rewriteUnwrappedType(value: String): String {
                        return remapping[value] ?: value
                    }
                }
            }
        })
        val rewrittenDex = rewriter.dexFileRewriter.rewrite(originalDex)
        val dexPool = DexPool(Opcodes.forApi(minApiLevel))
        rewrittenDex.classes.forEach { dexPool.internClass(it) }
        dexPool.writeTo(FileDataStore(dexFile))
    }

    private fun extractEslNumber(descriptor: String): Int {
        val idx = descriptor.indexOf(ESL_MARKER)
        if (idx < 0) return 0
        return descriptor.substring(idx + ESL_MARKER.length).removeSuffix(";").toIntOrNull() ?: 0
    }

    private fun belongsToChangedFiles(descriptors: List<String>, classNamePrefixes: Set<String>): Boolean {
        return descriptors.any { descriptor ->
            val simpleName = descriptor.substringAfterLast('/').removeSuffix(";")
            classNamePrefixes.any { prefix ->
                (simpleName.startsWith("${prefix}$") && simpleName.contains(ESL_MARKER)) ||
                    simpleName.startsWith("ComposableSingletons\$$prefix\$\$ExternalSyntheticLambda")
            }
        }
    }

    private fun computeNormalizedBytecodeHash(classDef: ClassDef): Int {
        var hash = 17
        classDef.methods.forEach { method ->
            hash = (hash * 31) + method.name.hashCode()
            hash = (hash * 31) + normalizeType(method.returnType).hashCode()
            method.parameterTypes.forEach { hash = (hash * 31) + normalizeType(it.toString()).hashCode() }
            method.implementation?.instructions?.forEach { instruction ->
                hash = (hash * 31) + instruction.opcode.hashCode()
                if (instruction is ReferenceInstruction) {
                    val ref = instruction.reference
                    val normalizedRef = when (ref) {
                        is TypeReference -> normalizeType(ref.type)
                        is MethodReference -> {
                            val owner = normalizeType(ref.definingClass)
                            val params = ref.parameterTypes.joinToString(",") { normalizeType(it.toString()) }
                            val ret = normalizeType(ref.returnType)
                            "$owner.${ref.name}($params)$ret"
                        }
                        is FieldReference -> {
                            val owner = normalizeType(ref.definingClass)
                            val type = normalizeType(ref.type)
                            "$owner.${ref.name}:$type"
                        }
                        else -> ref.toString()
                    }
                    hash = (hash * 31) + normalizedRef.hashCode()
                }
                if (instruction is NarrowLiteralInstruction) hash = (hash * 31) + instruction.narrowLiteral
                if (instruction is WideLiteralInstruction) hash = (hash * 31) + instruction.wideLiteral.hashCode()
            }
        }
        return hash
    }

    private fun normalizeType(type: String): String {
        val idx = type.indexOf(ESL_MARKER)
        if (idx < 0) return type
        return "${type.substring(0, idx)}\$\$$ESL_PLACEHOLDER;"
    }

    private fun extractPrimaryDex(zipFile: File): File? {
        if (!zipFile.exists()) return null
        ZipFile(zipFile).use { zip ->
            val entry = zip.entries().asSequence().firstOrNull { dexEntryRegex.matches(it.name) } ?: return null
            val temp = File.createTempFile("preview-esl-", ".dex")
            zip.getInputStream(entry).use { input -> FileOutputStream(temp).use { input.copyTo(it) } }
            return temp
        }
    }

    private fun replacePrimaryDexEntry(targetZip: File, newDex: File) {
        val tempZip = File(targetZip.parentFile, "${targetZip.name}.tmp")
        ZipFile(targetZip).use { zip ->
            ZipOutputStream(FileOutputStream(tempZip)).use { zos ->
                zip.entries().asSequence().forEach { entry ->
                    val name = entry.name
                    if (dexEntryRegex.matches(name)) {
                        val newEntry = ZipEntry(name)
                        zos.putNextEntry(newEntry)
                        newDex.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    } else {
                        val newEntry = ZipEntry(name)
                        zos.putNextEntry(newEntry)
                        zip.getInputStream(entry).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
        }
        tempZip.copyTo(targetZip, overwrite = true)
        tempZip.delete()
    }
}
