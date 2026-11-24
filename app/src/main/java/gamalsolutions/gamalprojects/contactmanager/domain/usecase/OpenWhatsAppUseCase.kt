package gamalsolutions.gamalprojects.contactmanager.domain.usecase

import android.content.Context
import gamalsolutions.gamalprojects.contactmanager.data.model.WhatsAppType
import gamalsolutions.gamalprojects.contactmanager.data.repository.ContactRepository
import gamalsolutions.gamalprojects.contactmanager.utils.ShareHelper
import gamalsolutions.gamalprojects.contactmanager.utils.VCardHelper

/**
 * حالة استخدام للتحقق من صحة الرقم الدولي وفتح محادثة مباشرة في واتساب أو واتساب الأعمال أو تيليجرام.
 */
class OpenWhatsAppUseCase(
    private val context: Context,
    private val repository: ContactRepository
) {
    suspend operator fun invoke(
        rawPhone: String,
        whatsAppType: WhatsAppType
    ): Result<Boolean> {
        val digitsOnly = extractValidDigits(rawPhone)
            ?: return Result.failure(IllegalArgumentException("Invalid international phone number"))

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

    suspend fun openTelegram(rawPhone: String): Result<Boolean> {
        val digitsOnly = extractValidDigits(rawPhone)
            ?: return Result.failure(IllegalArgumentException("Invalid international phone number"))

        val result = ShareHelper.openTelegramChat(
            context = context,
            internationalDigits = digitsOnly
        )
        if (result.isSuccess) {
            repository.recordWhatsAppOpened()
        }
        return result
    }

    private fun extractValidDigits(rawPhone: String): String? {
        val normalized = VCardHelper.normalizePhone(rawPhone)
        val digitsOnly = normalized.filter { it.isDigit() }
        return if (digitsOnly.length in 8..15) digitsOnly else null
    }
}
