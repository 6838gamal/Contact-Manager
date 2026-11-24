package gamalsolutions.gamalprojects.contactmanager.data.model

import androidx.annotation.StringRes
import gamalsolutions.gamalprojects.R

/**
 * تصنيفات تنظيم جهات الاتصال.
 */
enum class ContactCategory(val storageKey: String, @StringRes val labelRes: Int) {
    GENERAL("GENERAL", R.string.category_general),
    WORK("WORK", R.string.category_work),
    FAMILY("FAMILY", R.string.category_family),
    CLIENTS("CLIENTS", R.string.category_clients),
    VIP("VIP", R.string.category_vip);

    companion object {
        fun fromKey(key: String?): ContactCategory {
            return entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) } ?: GENERAL
        }
    }
}

/**
 * المنصة المفضلة أو مصدر جهة الاتصال (واتساب، واتساب الأعمال، تيليجرام).
 */
enum class ContactPlatform(val storageKey: String, @StringRes val labelRes: Int) {
    ALL("ALL", R.string.platform_all),
    WHATSAPP("WHATSAPP", R.string.platform_whatsapp),
    WHATSAPP_BUSINESS("WHATSAPP_BUSINESS", R.string.platform_whatsapp_business),
    TELEGRAM("TELEGRAM", R.string.platform_telegram);

    companion object {
        fun fromKey(key: String?): ContactPlatform {
            return entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) } ?: ALL
        }
    }
}

/**
 * سياسة التعامل مع أرقام الهواتف المكررة أثناء الاستيراد.
 */
enum class DuplicateStrategy {
    SKIP,
    UPDATE_EXISTING
}

/**
 * خيارات التصدير المخصصة قبل إنشاء ملف vCard.
 */
data class ExportOptions(
    val platformFilter: ContactPlatform = ContactPlatform.ALL,
    val categoryFilter: ContactCategory? = null,
    val onlyFavorites: Boolean = false,
    val includeEmail: Boolean = true,
    val includeCompanyAndNotes: Boolean = true,
    val namePrefix: String = ""
)

/**
 * خيارات الاستيراد والتنظيم قبل حفظ الجهات المستوردة.
 */
data class ImportOptions(
    val sourcePlatform: ContactPlatform = ContactPlatform.ALL,
    val assignCategory: ContactCategory = ContactCategory.GENERAL,
    val duplicateStrategy: DuplicateStrategy = DuplicateStrategy.SKIP,
    val namePrefix: String = "",
    val alsoSaveToPhonebook: Boolean = true
)

/**
 * ملخص نتيجة عملية الاستيراد.
 */
data class ImportSummary(
    val importedCount: Int,
    val updatedCount: Int,
    val skippedCount: Int
)

/**
 * نموذج المجال (Domain Model) لجهة الاتصال.
 */
data class Contact(
    val id: Long = 0L,
    val name: String,
    val phone: String,
    val email: String = "",
    val company: String = "",
    val photoUri: String? = null,
    val notes: String = "",
    val category: ContactCategory = ContactCategory.GENERAL,
    val preferredPlatform: ContactPlatform = ContactPlatform.ALL,
    val isFavorite: Boolean = false,
    val systemContactId: Long? = null,
    val isSyncedToDevice: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * الوجهات المدعومة لتصدير ومشاركة ملفات vCard.
 */
enum class ExportTarget(val packageName: String?) {
    GENERAL(null),
    WHATSAPP("com.whatsapp"),
    WHATSAPP_BUSINESS("com.whatsapp.w4b"),
    TELEGRAM("org.telegram.messenger")
}

/**
 * نوع تطبيق المراسلة عند فتح محادثة مباشرة.
 */
enum class WhatsAppType(val packageName: String) {
    STANDARD("com.whatsapp"),
    BUSINESS("com.whatsapp.w4b")
}

/**
 * إحصائيات التطبيق المخزنة محلياً.
 */
data class AppStats(
    val totalContacts: Int = 0,
    val syncedContacts: Int = 0,
    val exportedCount: Int = 0,
    val importedCount: Int = 0,
    val whatsAppOpenedCount: Int = 0,
    val lastBackupTimestamp: Long? = null
)

/**
 * عنصر معاينة قبل تأكيد الاستيراد مع حالة التكرار.
 */
data class ImportPreviewItem(
    val key: String,
    val contact: Contact,
    val isDuplicate: Boolean
)

/**
 * نتيجة معاينة ملف vCard قبل تأكيد الاستيراد.
 */
data class ImportPreview(
    val items: List<ImportPreviewItem>
) {
    val allParsedContacts: List<Contact> get() = items.map { it.contact }
    val newContacts: List<Contact> get() = items.filter { !it.isDuplicate }.map { it.contact }
    val duplicateContacts: List<Contact> get() = items.filter { it.isDuplicate }.map { it.contact }
    val totalCount: Int get() = items.size
    val newCount: Int get() = items.count { !it.isDuplicate }
    val duplicateCount: Int get() = items.count { it.isDuplicate }
}
