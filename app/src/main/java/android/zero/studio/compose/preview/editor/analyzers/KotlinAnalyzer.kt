package android.zero.studio.compose.preview.editor.analyzers

import android.zero.studio.compose.preview.editor.CodeEditorView
import android.zero.studio.compose.preview.editor.language.KotlinLanguage
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import java.io.File

/**
 * 编译器前端分析器：充当编辑器与 Kotlin 语言支持层之间的桥梁。
 * 
 * @property editor 当前代码编辑器实例。
 * @property file 正在编辑的文件。
 * @author android_zero
 */
class KotlinAnalyzer(val editor: CodeEditorView, val file: File) {

    /** 存储最近一次分析产生的诊断结果 */
    private var newContainer: DiagnosticsContainer? = null

    /** 获取编辑器当前的语言支持实例，并转换为 Kotlin 专用实现 */
    val editorLanguage: KotlinLanguage by lazy {
        val lang = editor.editorLanguage
        if (lang is KotlinLanguage) {
            lang
        } else {
            throw IllegalStateException("Editor is not configured with KotlinLanguage")
        }
    }

    /**
     * 触发编译与分析管线。
     * 该方法会阻塞协程直到整个 (Analyze -> Class -> Jar -> Dex) 流程完成。
     */
    suspend fun analyze() {
        val lang = editorLanguage
        
        // 获取当前编辑器缓冲区中的最新文本
        val content = editor.text.toString()
        
        // 执行核心编译管线
        val result = lang.analyze(file, content)
        
        // 缓存本次编译生成的诊断信息（用于更新 UI 错误指示）
        newContainer = result
    }

    /**
     * 获取生成的诊断容器。
     */
    fun getDiagnosticsContainer(): DiagnosticsContainer? {
        return newContainer
    }

    /**
     * 重置分析器状态。
     */
    fun reset() {
        newContainer = null
    }
}