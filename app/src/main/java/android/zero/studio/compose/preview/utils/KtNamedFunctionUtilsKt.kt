package android.zero.studio.compose.preview.utils

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

/**
 * 针对 Kotlin PSI 函数节点的工具扩展。
 * 用于计算函数在编译为 JVM 字节码后的宿主类名。
 * 
 * @author android_zero
 */

fun KtNamedFunction.className(): String {
    // 查找该函数所在的源文件节点
    val ktFile = this.getParentOfType<KtFile>(true)
    val fileName = ktFile?.name ?: "Unknown.kt"
    
    // 获取基础类名 (文件名首字母大写 + Kt)
    val baseName = fileName.substringBeforeLast(".kt").let {
        if (it.isEmpty()) "File" else it
    } + "Kt"
    
    // 获取包名
    val packageName = ktFile?.packageFqName?.asString()
    
    // 组合成完整类名
    return if (packageName.isNullOrEmpty()) {
        baseName
    } else {
        "$packageName.$baseName"
    }
}