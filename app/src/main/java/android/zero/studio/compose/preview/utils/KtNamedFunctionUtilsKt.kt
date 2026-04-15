package android.zero.studio.compose.preview.utils

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
// 1:1 导入反编译源码中定义的扩展属性
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

/**
 * Utility extensions for KtNamedFunction PSI elements to resolve class naming.
 * 
 * 工作流程线路图 (1:1 还原自反编译字节码逻辑):
 * 1. 访问 [containingClassOrObject] 扩展属性（对应字节码中的 KtPsiUtilKt.getContainingClassOrObject(this)）。
 * 2. 如果函数位于类或对象内：
 *    - 调用 getFqName()。如果非空，返回其全路径字符串。
 *    - 如果 FQN 为空，退而求其次调用 getName() 获取类名。
 * 3. 如果函数是顶层的 (Top-level)：
 *    - 获取所属的 [KtFile]。
 *    - 获取文件的包名属性 [packageFqName] 并转为字符串。
 *    - 获取文件名并执行后缀处理逻辑 (removeSuffix ".kt" 和 ".kts")。
 *    - 按照 Kotlin 编译器标准追加 "Kt" 后缀生成类名。
 *    - 拼接包名与生成的类名，形成最终全路径。
 * 
 * @author android_zero
 */

/**
 * Resolves the full class name for this function, handling both member functions
 * and top-level functions (which get the FileKt suffix).
 */
fun KtNamedFunction.className(): String {
    // 1:1 Restoration: This property access maps to KtPsiUtilKt.getContainingClassOrObject(this) in bytecode
    val parentClass: KtClassOrObject? = this.containingClassOrObject
    
    if (parentClass != null) {
        // If the function is defined inside a class, object, or companion object
        val fqName: FqName? = parentClass.getFqName()
        if (fqName != null) {
            val fqNameString: String? = fqName.asString()
            if (fqNameString != null) {
                return fqNameString
            }
        }
        
        // Return class name or empty string if FQName resolution fails
        return parentClass.name ?: ""
    } else {
        // Handle top-level functions (outside of any class)
        val psiFile = this.containingFile
        val ktFile = if (psiFile is KtFile) psiFile else null
        
        var packageName = ""
        if (ktFile != null) {
            // Accessing the packageFqName property (mapped from getPackageFqName())
            val pkgFqName: FqName = ktFile.packageFqName
            val pkgString = pkgFqName.asString()
            if (pkgString != null) {
                packageName = pkgString
            }
        }

        var fileNameBase: String
        val rawFileName = ktFile?.name
        if (rawFileName != null) {
            // Implementation matching the decompiled logic:
            // Removes ".kt" then removes ".kts" if it exists
            val tempName = rawFileName.removeSuffix(".kt")
            fileNameBase = tempName.removeSuffix(".kts")
        } else {
            fileNameBase = "Unknown"
        }

        // Standard naming: Top-level functions are compiled into a class named "FilenameKt"
        val finalClassName = "${fileNameBase}Kt"
        
        return if (packageName.isEmpty()) {
            finalClassName
        } else {
            "$packageName.$finalClassName"
        }
    }
}