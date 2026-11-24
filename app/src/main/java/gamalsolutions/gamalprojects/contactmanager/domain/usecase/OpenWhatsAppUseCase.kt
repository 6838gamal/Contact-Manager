package gamalsolutions.gamalprojects.contactmanager.domain.usecase

import android.content.Context
import gamalsolutions.gamalprojects.contactmanager.data.model.WhatsAppType
import gamalsolutions.gamalprojects.contactmanager.data.repository.ContactRepository
import gamalsolutions.gamalprojects.contactmanager.utils.ShareHelper
import gamalsolutions.gamalprojects.contactmanager.utils.VCardHelper

/**
 * حالة استخدام للتحقق من صحة الرقم الدولي وفتح محادثة مباشرة عبر wa.me في واتساب أو واتساب للأعمال.
 */
class OpenWhatsAppUseCase(
    private val context: Context,
    private val repository: ContactRepository
) {
    suspend operator fun invoke(
        rawPhone: String,
        whatsAppType: WhatsAppType
    ): Result<Boolean> {
        val normalized = VCardHelper.normalizePhone(rawPhone)
        val digitsOnly = normalized.filter { it.isDigit() }

        // التحقق من الحد الأدنى لطول الرقم الدولي (8 إلى 15 رقماً حسب معيار E.164)
        if (digitsOnly.length < 8 || digitsOnly.length > 15) {
            return Result.failure(
                IllegalArgumentException("Invalid international phone number")
            )
        }

        val result = ShareHelper.openWhatsAppChat(
            context = context,
            internationalDigits = digitsOnly,
            whatsAppType = whatsAppType
        )
        if (result.isSuccess) {
            repository.recordWhatsAppOpened()
        }
        return result
    }
}
