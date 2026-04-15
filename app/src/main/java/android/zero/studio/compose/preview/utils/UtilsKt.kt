package android.zero.studio.compose.preview.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.math.BigInteger
import java.security.MessageDigest

/**
 * General utilities for the application.
 *
 * @author android_zero
 */

/**
 * Extracts a file from the app assets to a target file on disk.
 */
fun Context.extractFromAsset(assetName: String, targetFile: File) {
    try {
        assets.open(assetName).use { inputStream ->
            FileOutputStream(targetFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    } catch (e: Exception) {
        Log.e("Utils", "Failed to extract from asset: $assetName", e)
    }
}

/**
 * Calculates the SHA-256 checksum of an [InputStream].
 */
fun calculateChecksum(inputStream: InputStream): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(8192)
    var bytesRead: Int
    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
        digest.update(buffer, 0, bytesRead)
    }
    return BigInteger(1, digest.digest()).toString(16).padStart(64, '0')
}

/**
 * Generates an MD5 hash of the string and returns it as a hex string.
 */
fun String.getFileHash(): String {
    val digest = MessageDigest.getInstance("MD5")
    val hashBytes = digest.digest(this.toByteArray(Charsets.UTF_8))
    return hashBytes.joinToString("") { "%02x".format(it) }
}