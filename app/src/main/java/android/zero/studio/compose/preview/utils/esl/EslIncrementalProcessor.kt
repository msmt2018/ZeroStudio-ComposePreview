package android.zero.studio.compose.preview.utils.esl

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * ESL (ExternalSyntheticLambda) 增量处理器。
 *
 * 说明：
 * - 为避免在部分环境下缺失 dexlib2/smali 依赖导致编译失败，这里使用纯 ZIP + 字节扫描方案。
 * - 对 compose-like 方法数量使用轻量启发式统计。
 * - 对 ESL 重映射提供安全降级实现：若无法可靠重写则不改写并返回 0。
 */
object EslIncrementalProcessor {
    private val dexEntryRegex = Regex("classes\\d*\\.dex")

    private val composableMarker = "Landroidx/compose/runtime/Composable;".toByteArray()
    private val composableName = "composable".toByteArray()

    fun remapHotDexUsingBaseline(
        hotDexZip: File,
        baselineDexZip: File,
        changedClassPrefixes: Set<String>,
        minApiLevel: Int
    ): Int {
        if (!hotDexZip.exists() || !baselineDexZip.exists()) return 0
        if (changedClassPrefixes.isEmpty()) return 0

        val hotPrimary = extractPrimaryDex(hotDexZip) ?: return 0
        val baselinePrimary = extractPrimaryDex(baselineDexZip) ?: return 0

        return try {
            // 轻量一致性探测：若两份 dex 均包含 ESL 特征且热更新体积更小，按原样保留（不做危险改写）。
            val hotBytes = hotPrimary.readBytes()
            val baselineBytes = baselinePrimary.readBytes()
            val hotEsl = countMarker(hotBytes, "ExternalSyntheticLambda".toByteArray())
            val baseEsl = countMarker(baselineBytes, "ExternalSyntheticLambda".toByteArray())

            if (hotEsl == 0 || baseEsl == 0) return 0
            if (hotBytes.contentEquals(baselineBytes)) return 0

            // 当前降级策略：不直接重写 dex，避免在无 dexlib2 条件下生成非法字节码。
            // 预留：后续可在存在 dexlib2 时启用精确 rewrite。
            0
        } finally {
            hotPrimary.delete()
            baselinePrimary.delete()
        }
    }

    fun countComposableLikeMethods(hotDexZip: File, minApiLevel: Int): Int {
        if (!hotDexZip.exists()) return 0
        var score = 0
        ZipFile(hotDexZip).use { zip ->
            zip.entries().asSequence()
                .filter { !it.isDirectory && dexEntryRegex.matches(it.name) }
                .forEach { entry ->
                    zip.getInputStream(entry).use { input ->
                        val bytes = input.readBytes()
                        score += countMarker(bytes, composableMarker)
                        score += countMarker(bytes, composableName)
                    }
                }
        }
        return score
    }

    private fun countMarker(haystack: ByteArray, marker: ByteArray): Int {
        if (haystack.isEmpty() || marker.isEmpty() || marker.size > haystack.size) return 0
        var count = 0
        var i = 0
        while (i <= haystack.size - marker.size) {
            var matched = true
            for (j in marker.indices) {
                val a = haystack[i + j]
                val b = marker[j]
                if (a != b && !isAsciiCaseEqual(a, b)) {
                    matched = false
                    break
                }
            }
            if (matched) {
                count++
                i += marker.size
            } else {
                i++
            }
        }
        return count
    }

    private fun isAsciiCaseEqual(a: Byte, b: Byte): Boolean {
        fun lower(x: Byte): Byte = if (x in 0x41..0x5A) (x + 32).toByte() else x
        return lower(a) == lower(b)
    }

    private fun extractPrimaryDex(zipFile: File): File? {
        ZipFile(zipFile).use { zip ->
            val entry = zip.entries().asSequence().firstOrNull { dexEntryRegex.matches(it.name) } ?: return null
            val temp = File.createTempFile("preview-esl-", ".dex")
            zip.getInputStream(entry).use { input -> FileOutputStream(temp).use { input.copyTo(it) } }
            return temp
        }
    }

    @Suppress("unused")
    private fun replacePrimaryDexEntry(targetZip: File, newDex: File) {
        val tempZip = File(targetZip.parentFile, "${targetZip.name}.tmp")
        ZipFile(targetZip).use { zip ->
            ZipOutputStream(FileOutputStream(tempZip)).use { zos ->
                zip.entries().asSequence().forEach { entry ->
                    val name = entry.name
                    zos.putNextEntry(ZipEntry(name))
                    if (!entry.isDirectory) {
                        if (dexEntryRegex.matches(name)) {
                            newDex.inputStream().use { it.copyTo(zos) }
                        } else {
                            zip.getInputStream(entry).use { it.copyTo(zos) }
                        }
                    }
                    zos.closeEntry()
                }
            }
        }
        tempZip.copyTo(targetZip, overwrite = true)
        tempZip.delete()
    }
}
