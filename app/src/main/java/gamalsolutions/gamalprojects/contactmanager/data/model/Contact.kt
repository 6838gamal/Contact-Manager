package gamalsolutions.gamalprojects.contactmanager.data.model

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
 * نوع تطبيق واتساب عند فتح محادثة مباشرة.
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
 * نتيجة معاينة ملف vCard قبل تأكيد الاستيراد.
 */
data class ImportPreview(
    val allParsedContacts: List<Contact>,
    val newContacts: List<Contact>,
    val duplicateContacts: List<Contact>
) {
    val totalCount: Int get() = allParsedContacts.size
    val newCount: Int get() = newContacts.size
    val duplicateCount: Int get() = duplicateContacts.size
}
