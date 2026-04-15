package android.zero.studio.compose.preview.kotlin

import org.jetbrains.kotlin.com.intellij.lang.Language
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * PSI 文件缓存管理器：维护内存中的虚拟文件系统以供编译器快速分析。
 * 
 * 工作流程线路图:
 * 1. 存储: 利用 ConcurrentHashMap 存储文件名与 KtPsiFile 的映射。
 * 2. 更新: 当编辑器内容变化时，计算 Hash，如果不一致则通过 PsiFileFactory 重新构建 PSI 树。
 * 3. 提取: 提供 ktFiles 属性供 KotlinCoreEnvironment 注入源文件集合。
 * 
 * @author android_zero
 */
class PsiFileCache(private val project: Project) {

    private val factory: PsiFileFactory by lazy {
        PsiFileFactory.getInstance(project)
    }

    private val cache = ConcurrentHashMap<String, KtPsiFile>()

    /**
     * 获取所有缓存的 KtFile 实例。
     */
    val ktFiles: List<KtFile>
        get() = cache.values.map { it.ktFile }

    /**
     * 根据名称获取缓存。会处理路径中的斜杠，确保 key 的唯一性。
     */
    fun get(name: String): KtPsiFile? {
        val key = name.substringAfterLast("/")
        return cache[key]
    }

    fun getOrUpdate(file: File): KtPsiFile {
        return getOrUpdate(file.name, file.readText())
    }

    /**
     * 更新缓存。
     * 如果文本 Hash 未改变，则直接返回旧实例，优化性能。
     */
    fun getOrUpdate(name: String, code: String): KtPsiFile {
        val key = name.substringAfterLast("/")
        val currentHash = code.hashCode()
        val existing = cache[key]
        
        if (existing != null && existing.textHash == currentHash) {
            return existing
        } else {
            // 通过 Kotlin 插件提供的工厂创建虚拟 PSI 文件
            val psiFile = factory.createFileFromText(
                key,
                KotlinLanguage.INSTANCE as Language,
                code
            )
            
            val ktPsiFile = KtPsiFile(psiFile as KtFile, currentHash)
            cache[key] = ktPsiFile
            return ktPsiFile
        }
    }
}