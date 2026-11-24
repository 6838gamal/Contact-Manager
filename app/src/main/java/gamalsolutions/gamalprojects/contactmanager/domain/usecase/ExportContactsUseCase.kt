package gamalsolutions.gamalprojects.contactmanager.domain.usecase

import android.content.Context
import android.net.Uri
import gamalsolutions.gamalprojects.contactmanager.data.model.Contact
import gamalsolutions.gamalprojects.contactmanager.data.repository.ContactRepository
import gamalsolutions.gamalprojects.contactmanager.utils.FileProviderHelper
import gamalsolutions.gamalprojects.contactmanager.utils.VCardHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * حالة استخدام لتصدير جهات الاتصال إلى ملف .vcf داخل cacheDir وإنشاء Uri عبر FileProvider.
 */
class ExportContactsUseCase(
    private val context: Context,
    private val repository: ContactRepository
) {
    suspend operator fun invoke(
        contacts: List<Contact>,
        fileNamePrefix: String = "contacts"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        if (contacts.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No contacts to export"))
        }
        val vCardContent = VCardHelper.toVCardString(contacts)
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

    suspend fun writeBackupToUri(targetUri: Uri, contacts: List<Contact>): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (contacts.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No contacts to backup"))
            }
            val vCardContent = VCardHelper.toVCardString(contacts)
            val result = FileProviderHelper.writeTextToUri(context, targetUri, vCardContent)
            if (result.isSuccess) {
                repository.recordBackupCreated()
            }
            result
        }
}
