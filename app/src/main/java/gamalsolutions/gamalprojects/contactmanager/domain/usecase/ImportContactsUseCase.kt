package gamalsolutions.gamalprojects.contactmanager.domain.usecase

import android.content.Context
import android.net.Uri
import gamalsolutions.gamalprojects.contactmanager.data.model.Contact
import gamalsolutions.gamalprojects.contactmanager.data.model.ImportOptions
import gamalsolutions.gamalprojects.contactmanager.data.model.ImportPreview
import gamalsolutions.gamalprojects.contactmanager.data.model.ImportPreviewItem
import gamalsolutions.gamalprojects.contactmanager.data.model.ImportSummary
import gamalsolutions.gamalprojects.contactmanager.data.repository.ContactRepository
import gamalsolutions.gamalprojects.contactmanager.utils.FileProviderHelper
import gamalsolutions.gamalprojects.contactmanager.utils.VCardHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * حالة استخدام لمعاينة واستيراد جهات الاتصال من ملف .vcf مع خيارات التنظيم وسياسة التكرار.
 */
class ImportContactsUseCase(
    private val context: Context,
    private val repository: ContactRepository
) {
    suspend fun previewFromUri(uri: Uri): Result<ImportPreview> = withContext(Dispatchers.IO) {
        val textResult = FileProviderHelper.readTextFromUri(context, uri)
        if (textResult.isFailure) {
            return@withContext Result.failure(
                textResult.exceptionOrNull() ?: IllegalStateException("Cannot read vCard file")
            )
        }

        val parsed = VCardHelper.parseVCardString(textResult.getOrDefault(""))
        if (parsed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No valid contacts in vCard"))
        }

        val existingPhones = repository.getAllNormalizedPhones()
        val seenInFile = mutableSetOf<String>()
        val items = mutableListOf<ImportPreviewItem>()

        parsed.forEachIndexed { index, contact ->
            val norm = VCardHelper.normalizePhone(contact.phone)
            if (norm.isNotBlank()) {
                val isDuplicate = existingPhones.contains(norm) || !seenInFile.add(norm)
                items.add(
                    ImportPreviewItem(
                        key = "${index}_$norm",
                        contact = contact,
                        isDuplicate = isDuplicate
                    )
                )
            }
        }

        if (items.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No valid contacts in vCard"))
        }

        Result.success(ImportPreview(items = items))
    }

    suspend fun confirmImport(
        selectedContacts: List<Contact>,
        options: ImportOptions
    ): Result<ImportSummary> {
        return repository.importContactsWithOptions(
            contacts = selectedContacts,
            options = options
        )
    }
}
