package android.zero.studio.compose.preview.kotlin

import org.jetbrains.kotlin.com.intellij.lang.Language
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages an in-memory cache of Kotlin PSI files to optimize repeated analysis.
 * 
 * @property project The project context required by the PSI factory.
 * @author android_zero
 */
class PsiFileCache(private val project: Project) {

    /** 
     * Factory for creating PSI files from text.
     * Restoration of PsiFileCache$$ExternalSyntheticLambda0 logic.
     */
    private val factory: PsiFileFactory by lazy {
        PsiFileFactory.getInstance(project)
    }

    /** Thread-safe storage for cached PSI files. */
    private val cache: ConcurrentHashMap<String, KtPsiFile> = ConcurrentHashMap()

    /** 
     * Provides a list of all currently cached Kotlin files.
     * 
     * 工作流程:
     * 1. 访问底层 ConcurrentHashMap 的所有值。
     * 2. 将每个 KtPsiFile 实例映射为其内部持有的 KtFile。
     * 3. 返回包含所有 KtFile 的列表。
     */
    val ktFiles: List<KtFile>
        get() {
            val values = this.cache.values
            val result = ArrayList<KtFile>(values.size)
            for (item in values) {
                result.add(item.ktFile)
            }
            return result
        }

    /**
     * Retrieves a cached Kotlin PSI file by its name.
     */
    fun get(name: String): KtPsiFile? {
        return this.cache[name]
    }

    /**
     * Retrieves a cached file or updates the cache if the file content has changed.
     */
    fun getOrUpdate(file: File): KtPsiFile {
        val fileName = file.name
        val content = file.readText()
        return this.getOrUpdate(fileName, content)
    }

    /**
     * Checks the provided text against the cache. If changed, recreates the PSI tree.
     * 
     * @param name The unique identifier for the file (usually the filename).
     * @param code The source code text.
     * @return The cached or newly created [KtPsiFile].
     */
    fun getOrUpdate(name: String, code: String): KtPsiFile {
        val currentHash = code.hashCode()
        val existing = this.cache[name]
        
        // If the file exists and the content hash matches, return the cached version
        if (existing != null && existing.textHash == currentHash) {
            return existing
        } else {
            // Re-create the PSI file using the factory
            val psiFile = this.factory.createFileFromText(
                name,
                KotlinLanguage.INSTANCE as Language,
                code
            )
            
            val ktPsiFile = KtPsiFile(psiFile as KtFile, currentHash)
            this.cache[name] = ktPsiFile
            return ktPsiFile
        }
    }
}