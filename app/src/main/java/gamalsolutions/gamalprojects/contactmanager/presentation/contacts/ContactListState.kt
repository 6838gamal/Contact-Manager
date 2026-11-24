package gamalsolutions.gamalprojects.contactmanager.presentation.contacts

import androidx.annotation.StringRes
import gamalsolutions.gamalprojects.contactmanager.data.model.AppStats
import gamalsolutions.gamalprojects.contactmanager.data.model.Contact
import gamalsolutions.gamalprojects.contactmanager.data.model.ImportPreview

/**
 * رسالة واجهة تدعم الترجمة الديناميكية عبر معرفات الموارد النصية.
 */
sealed class UiMessage {
    data class Resource(@StringRes val resId: Int, val args: List<Any> = emptyList()) : UiMessage()
    data class Text(val message: String) : UiMessage()
}

/**
 * الحالة غير القابلة للتغيير لشاشة جهات الاتصال والعمليات المرتبطة بها.
 */
data class ContactListState(
    val contacts: List<Contact> = emptyList(),
    val filteredContacts: List<Contact> = emptyList(),
    val searchQuery: String = "",
    val selectedIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val isExportSheetVisible: Boolean = false,
    val showOnlySynced: Boolean = false,
    val qrContact: Contact? = null,
    val whatsAppTargetContact: Contact? = null,
    val importPreview: ImportPreview? = null,
    val isParsingImport: Boolean = false,
    val isImporting: Boolean = false,
    val stats: AppStats = AppStats(),
    val appLanguage: String = "ar",
    val snackbarMessage: UiMessage? = null
) {
    val isSelectionMode: Boolean get() = selectedIds.isNotEmpty()
    val contactsToExport: List<Contact>
        get() = if (selectedIds.isNotEmpty()) {
            contacts.filter { selectedIds.contains(it.id) }
        } else {
            contacts
        }
}
