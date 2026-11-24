package gamalsolutions.gamalprojects.contactmanager.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * أداة إنشاء ملفات .vcf داخل cacheDir ومشاركتها عبر FileProvider بأمان بدون أذونات تخزين.
 */
object FileProviderHelper {

    fun createVCardFileUri(
        context: Context,
        vCardContent: String,
        fileNamePrefix: String = "contacts"
    ): Result<Uri> {
        return try {
            val exportDir = File(context.cacheDir, "exports").apply {
                if (!exists()) mkdirs()
            }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val safePrefix = fileNamePrefix.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(exportDir, "${safePrefix}_$timestamp.vcf")
            file.writeText(vCardContent, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun readTextFromUri(context: Context, uri: Uri): Result<String> {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return Result.failure(IllegalStateException("Unable to open file stream"))
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun writeTextToUri(context: Context, uri: Uri, content: String): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(content)
                }
            } ?: return Result.failure(IllegalStateException("Unable to open output stream"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
