package android.zero.studio.compose.preview.utils.esl

data class EslFingerprint(
    val outerClass: String,
    val interfaces: List<String>,
    val constructorParams: List<String>,
    val bytecodeHash: Int
)
