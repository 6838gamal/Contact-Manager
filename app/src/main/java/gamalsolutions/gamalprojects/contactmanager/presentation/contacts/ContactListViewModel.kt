package gamalsolutions.gamalprojects.contactmanager.presentation.contacts

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gamalsolutions.gamalprojects.R
import gamalsolutions.gamalprojects.contactmanager.data.model.AppStats
import gamalsolutions.gamalprojects.contactmanager.data.model.Contact
import gamalsolutions.gamalprojects.contactmanager.data.model.ExportTarget
import gamalsolutions.gamalprojects.contactmanager.data.model.WhatsAppType
import gamalsolutions.gamalprojects.contactmanager.data.repository.ContactRepository
import gamalsolutions.gamalprojects.contactmanager.domain.usecase.ExportContactsUseCase
import gamalsolutions.gamalprojects.contactmanager.domain.usecase.ImportContactsUseCase
import gamalsolutions.gamalprojects.contactmanager.domain.usecase.OpenWhatsAppUseCase
import gamalsolutions.gamalprojects.contactmanager.utils.ShareHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel الرئيسي لإدارة قائمة جهات الاتصال، المزامنة، التصدير، الاستيراد، والنسخ الاحتياطي.
 */
class ContactListViewModel(
    private val appContext: Context,
    private val repository: ContactRepository,
    private val exportContactsUseCase: ExportContactsUseCase,
    private val importContactsUseCase: ImportContactsUseCase,
    private val openWhatsAppUseCase: OpenWhatsAppUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactListState())
    val uiState: StateFlow<ContactListState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")
    private val showOnlySyncedFlow = MutableStateFlow(false)

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.allContacts,
                repository.preferencesFlow,
                searchQueryFlow,
                showOnlySyncedFlow
            ) { allContacts, prefs, query, onlySynced ->
                val filtered = allContacts.filter { contact ->
                    val matchesSync = !onlySynced || contact.isSyncedToDevice
                    val q = query.trim()
                    val matchesQuery = q.isEmpty() ||
                        contact.name.contains(q, ignoreCase = true) ||
                        contact.phone.contains(q, ignoreCase = true) ||
                        contact.company.contains(q, ignoreCase = true) ||
                        contact.email.contains(q, ignoreCase = true)
                    matchesSync && matchesQuery
                }

                val stats = AppStats(
                    totalContacts = allContacts.size,
                    syncedContacts = allContacts.count { it.isSyncedToDevice },
                    exportedCount = prefs.exportedCount,
                    importedCount = prefs.importedCount,
                    whatsAppOpenedCount = prefs.whatsAppOpenedCount,
                    lastBackupTimestamp = prefs.lastBackupTimestamp
                )

                Triple(allContacts, filtered, Pair(stats, prefs.appLanguage))
            }.collect { (allContacts, filtered, meta) ->
                _uiState.update { current ->
                    current.copy(
                        contacts = allContacts,
                        filteredContacts = filtered,
                        stats = meta.first,
                        appLanguage = meta.second,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQueryFlow.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSyncedFilter() {
        val next = !showOnlySyncedFlow.value
        showOnlySyncedFlow.value = next
        _uiState.update { it.copy(showOnlySynced = next) }
    }

    fun toggleSelection(contactId: Long) {
        _uiState.update { state ->
            val updated = state.selectedIds.toMutableSet()
            if (!updated.add(contactId)) {
                updated.remove(contactId)
            }
            state.copy(selectedIds = updated)
        }
    }

    fun selectAllVisible() {
        _uiState.update { state ->
            state.copy(selectedIds = state.filteredContacts.map { it.id }.toSet())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun openExportSheet() {
        if (_uiState.value.contacts.isEmpty()) {
            showMessage(UiMessage.Resource(R.string.msg_export_empty))
            return
        }
        _uiState.update { it.copy(isExportSheetVisible = true) }
    }

    fun closeExportSheet() {
        _uiState.update { it.copy(isExportSheetVisible = false) }
    }

    fun exportAndShare(target: ExportTarget, chooserTitle: String, singleContact: Contact? = null) {
        viewModelScope.launch {
            val contacts = singleContact?.let { listOf(it) } ?: _uiState.value.contactsToExport
            if (contacts.isEmpty()) {
                showMessage(UiMessage.Resource(R.string.msg_export_empty))
                return@launch
            }
            val prefix = if (contacts.size == 1) contacts.first().name else "contacts"
            val uriResult = exportContactsUseCase(contacts, prefix)
            uriResult.fold(
                onSuccess = { uri ->
                    _uiState.update { it.copy(isExportSheetVisible = false) }
                    ShareHelper.shareVCardUri(
                        context = appContext,
                        vCardUri = uri,
                        target = target,
                        chooserTitle = chooserTitle
                    ).onFailure { err ->
                        showMessage(
                            UiMessage.Resource(
                                R.string.msg_error_generic,
                                listOf(err.localizedMessage.orEmpty())
                            )
                        )
                    }
                },
                onFailure = { err ->
                    showMessage(
                        UiMessage.Resource(
                            R.string.msg_error_generic,
                            listOf(err.localizedMessage.orEmpty())
                        )
                    )
                }
            )
        }
    }

    fun syncWithDevicePhonebook() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val result = repository.syncFromDevicePhonebook()
            _uiState.update { it.copy(isSyncing = false) }
            result.fold(
                onSuccess = { count ->
                    showMessage(UiMessage.Resource(R.string.msg_sync_done, listOf(count)))
                },
                onFailure = { err ->
                    showMessage(
                        UiMessage.Resource(
                            R.string.msg_error_generic,
                            listOf(err.localizedMessage.orEmpty())
                        )
                    )
                }
            )
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            repository.deleteContact(contact).fold(
                onSuccess = {
                    showMessage(UiMessage.Resource(R.string.msg_contact_deleted))
                },
                onFailure = { err ->
                    showMessage(
                        UiMessage.Resource(
                            R.string.msg_error_generic,
                            listOf(err.localizedMessage.orEmpty())
                        )
                    )
                }
            )
        }
    }

    fun deleteSelectedContacts() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedIds.toList()
            if (ids.isEmpty()) return@launch
            repository.deleteContactsByIds(ids).fold(
                onSuccess = {
                    clearSelection()
                    showMessage(UiMessage.Resource(R.string.msg_contact_deleted))
                },
                onFailure = { err ->
                    showMessage(
                        UiMessage.Resource(
                            R.string.msg_error_generic,
                            listOf(err.localizedMessage.orEmpty())
                        )
                    )
                }
            )
        }
    }

    fun requestWhatsAppChat(contact: Contact) {
        _uiState.update { it.copy(whatsAppTargetContact = contact) }
    }

    fun dismissWhatsAppChooser() {
        _uiState.update { it.copy(whatsAppTargetContact = null) }
    }

    fun openWhatsApp(contact: Contact, type: WhatsAppType) {
        viewModelScope.launch {
            _uiState.update { it.copy(whatsAppTargetContact = null) }
            openWhatsAppUseCase(contact.phone, type).onFailure {
                showMessage(UiMessage.Resource(R.string.error_phone_invalid))
            }
        }
    }

    fun showQrCode(contact: Contact) {
        _uiState.update { it.copy(qrContact = contact) }
    }

    fun dismissQrCode() {
        _uiState.update { it.copy(qrContact = null) }
    }

    fun loadImportPreview(uri: Uri, onReady: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isParsingImport = true) }
            val result = importContactsUseCase.previewFromUri(uri)
            _uiState.update { it.copy(isParsingImport = false) }
            result.fold(
                onSuccess = { preview ->
                    _uiState.update { it.copy(importPreview = preview) }
                    onReady()
                },
                onFailure = {
                    showMessage(UiMessage.Resource(R.string.import_empty_error))
                }
            )
        }
    }

    fun confirmImport(alsoSaveToPhonebook: Boolean, onDone: () -> Unit = {}) {
        val preview = _uiState.value.importPreview ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            val result = importContactsUseCase.confirmImport(preview, alsoSaveToPhonebook)
            _uiState.update {
                it.copy(
                    isImporting = false,
                    importPreview = null
                )
            }
            result.fold(
                onSuccess = { importedCount ->
                    showMessage(
                        UiMessage.Resource(
                            R.string.import_success,
                            listOf(importedCount, preview.duplicateCount)
                        )
                    )
                    onDone()
                },
                onFailure = { err ->
                    showMessage(
                        UiMessage.Resource(
                            R.string.msg_error_generic,
                            listOf(err.localizedMessage.orEmpty())
                        )
                    )
                }
            )
        }
    }

    fun clearImportPreview() {
        _uiState.update { it.copy(importPreview = null) }
    }

    fun createBackupToUri(uri: Uri) {
        viewModelScope.launch {
            val allContacts = repository.getAllContactsSnapshot()
            if (allContacts.isEmpty()) {
                showMessage(UiMessage.Resource(R.string.msg_export_empty))
                return@launch
            }
            exportContactsUseCase.writeBackupToUri(uri, allContacts).fold(
                onSuccess = {
                    showMessage(UiMessage.Resource(R.string.msg_backup_saved))
                },
                onFailure = { err ->
                    showMessage(
                        UiMessage.Resource(
                            R.string.msg_error_generic,
                            listOf(err.localizedMessage.orEmpty())
                        )
                    )
                }
            )
        }
    }

    fun setAppLanguage(languageCode: String) {
        viewModelScope.launch {
            repository.setAppLanguage(languageCode)
        }
    }

    fun notifyMessage(resId: Int, vararg args: Any) {
        showMessage(UiMessage.Resource(resId, args.toList()))
    }

    fun consumeSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun showMessage(message: UiMessage) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }
}
