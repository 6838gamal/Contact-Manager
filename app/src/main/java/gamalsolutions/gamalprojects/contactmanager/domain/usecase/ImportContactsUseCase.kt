package gamalsolutions.gamalprojects.contactmanager.domain.usecase

import android.content.Context
import android.net.Uri
import gamalsolutions.gamalprojects.contactmanager.data.model.ImportPreview
import gamalsolutions.gamalprojects.contactmanager.data.repository.ContactRepository
import gamalsolutions.gamalprojects.contactmanager.utils.FileProviderHelper
import gamalsolutions.gamalprojects.contactmanager.utils.VCardHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * حالة استخدام لمعاينة واستيراد جهات الاتصال من ملف .vcf مع منع التكرار حسب رقم الهاتف.
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

        val existingPhones = repository.getAllNormalizedPhones().toMutableSet()
        val seenInFile = mutableSetOf<String>()
        val newContacts = mutableListOf<gamalsolutions.gamalprojects.contactmanager.data.model.Contact>()
        val duplicateContacts = mutableListOf<gamalsolutions.gamalprojects.contactmanager.data.model.Contact>()

        for (contact in parsed) {
            val norm = VCardHelper.normalizePhone(contact.phone)
            if (norm.isBlank()) continue
            if (existingPhones.contains(norm) || !seenInFile.add(norm)) {
                duplicateContacts.add(contact)
            } else {
                newContacts.add(contact)
            }
        }

        Result.success(
            ImportPreview(
                allParsedContacts = parsed,
                newContacts = newContacts,
                duplicateContacts = duplicateContacts
            )
        )
    }

    suspend fun confirmImport(
        preview: ImportPreview,
        alsoSaveToPhonebook: Boolean
    ): Result<Int> {
        return repository.insertImportedContacts(
            contacts = preview.newContacts,
            alsoSaveToPhonebook = alsoSaveToPhonebook
        )
    }
}
