package android.zero.studio.compose.preview.kotlin.parser

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * 预览函数解析器：用于在 Kotlin 源码中定位具备 @Preview 和 @Composable 资格的函数。
 * 
 * 
 * @author android_zero
 */
class PreviewComposableFunctionParser private constructor(private val visitor: PreviewComposableVisitor) {

    /**
     * 获取解析出的函数列表。
     */
    fun parse(): List<KtNamedFunction> = visitor.functions

    companion object {
        /**
         * 初始化解析器并开始执行 PSI 扫描。
         * 
         * @param ktFile 目标源文件的 PSI 根节点。
         */
        fun initialize(ktFile: KtFile): PreviewComposableFunctionParser {
            val visitor = PreviewComposableVisitor()
            // 启动访问者模式遍历整个抽象语法树 (AST)
            ktFile.accept(visitor)
            return PreviewComposableFunctionParser(visitor)
        }
    }
}