package gamalsolutions.gamalprojects.contactmanager.domain.usecase

import android.content.Context
import android.net.Uri
import gamalsolutions.gamalprojects.contactmanager.data.model.Contact
import gamalsolutions.gamalprojects.contactmanager.data.model.ContactPlatform
import gamalsolutions.gamalprojects.contactmanager.data.model.ExportOptions
import gamalsolutions.gamalprojects.contactmanager.data.repository.ContactRepository
import gamalsolutions.gamalprojects.contactmanager.utils.FileProviderHelper
import gamalsolutions.gamalprojects.contactmanager.utils.VCardHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * حالة استخدام لتصفية جهات الاتصال وتصديرها إلى ملف .vcf وفق خيارات مخصصة.
 */
class ExportContactsUseCase(
    private val context: Context,
    private val repository: ContactRepository
) {
    fun filterForExport(
        allContacts: List<Contact>,
        selectedIds: Set<Long>,
        options: ExportOptions
    ): List<Contact> {
        val base = if (selectedIds.isNotEmpty()) {
            allContacts.filter { selectedIds.contains(it.id) }
        } else {
            allContacts
        }
        return base.filter { contact ->
            val matchesPlatform = options.platformFilter == ContactPlatform.ALL ||
                contact.preferredPlatform == options.platformFilter
            val matchesCategory = options.categoryFilter == null ||
                contact.category == options.categoryFilter
            val matchesFavorite = !options.onlyFavorites || contact.isFavorite
            matchesPlatform && matchesCategory && matchesFavorite
        }
    }

    suspend operator fun invoke(
        contacts: List<Contact>,
        options: ExportOptions = ExportOptions(),
        fileNamePrefix: String = "contacts"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        if (contacts.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No contacts to export"))
        }
        val vCardContent = VCardHelper.toVCardString(contacts, options)
        val uriResult = FileProviderHelper.createVCardFileUri(
            context = context,
            vCardContent = vCardContent,
            fileNamePrefix = fileNamePrefix
        )
        if (uriResult.isSuccess) {
            repository.recordExport()
        }
        uriResult
    }

    suspend fun writeExportToUri(
        targetUri: Uri,
        contacts: List<Contact>,
        options: ExportOptions = ExportOptions(),
        isFullBackup: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (contacts.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No contacts to export"))
        }
        val vCardContent = VCardHelper.toVCardString(contacts, options)
        val result = FileProviderHelper.writeTextToUri(context, targetUri, vCardContent)
        if (result.isSuccess) {
            if (isFullBackup) {
                repository.recordBackupCreated()
            } else {
                repository.recordExport()
            }
        }
        result
    }
}
