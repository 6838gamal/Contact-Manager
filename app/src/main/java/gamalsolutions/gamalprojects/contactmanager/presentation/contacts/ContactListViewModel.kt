package gamalsolutions.gamalprojects.contactmanager.presentation.contacts

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gamalsolutions.gamalprojects.R
import gamalsolutions.gamalprojects.contactmanager.data.model.AppStats
import gamalsolutions.gamalprojects.contactmanager.data.model.Contact
import gamalsolutions.gamalprojects.contactmanager.data.model.ContactCategory
import gamalsolutions.gamalprojects.contactmanager.data.model.ContactPlatform
import gamalsolutions.gamalprojects.contactmanager.data.model.DuplicateStrategy
import gamalsolutions.gamalprojects.contactmanager.data.model.ExportOptions
import gamalsolutions.gamalprojects.contactmanager.data.model.ExportTarget
import gamalsolutions.gamalprojects.contactmanager.data.model.ImportOptions
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

private data class ListFilterCriteria(
    val query: String,
    val onlySynced: Boolean,
    val platform: ContactPlatform,
    val category: ContactCategory?,
    val onlyFavorites: Boolean
)

/**
 * ViewModel الرئيسي لإدارة قائمة جهات الاتصال، التنظيم والتصنيف، التصدير والاستيراد المخصص، والنسخ الاحتياطي.
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
    private val platformFilterFlow = MutableStateFlow(ContactPlatform.ALL)
    private val categoryFilterFlow = MutableStateFlow<ContactCategory?>(null)
    private val onlyFavoritesFilterFlow = MutableStateFlow(false)

    init {
        observeData()
    }

    private fun observeData() {
        val filtersFlow = combine(
            searchQueryFlow,
            showOnlySyncedFlow,
            platformFilterFlow,
            categoryFilterFlow,
            onlyFavoritesFilterFlow
        ) { query, onlySynced, platform, category, onlyFavorites ->
            ListFilterCriteria(
                query = query,
                onlySynced = onlySynced,
                platform = platform,
                category = category,
                onlyFavorites = onlyFavorites
            )
        }

        viewModelScope.launch {
            combine(
                repository.allContacts,
                repository.preferencesFlow,
                filtersFlow
            ) { allContacts, prefs, criteria ->
                val q = criteria.query.trim()
                val filtered = allContacts.filter { contact ->
                    val matchesSync = !criteria.onlySynced || contact.isSyncedToDevice
                    val matchesPlatform = criteria.platform == ContactPlatform.ALL ||
                        contact.preferredPlatform == criteria.platform
                    val matchesCategory = criteria.category == null ||
                        contact.category == criteria.category
                    val matchesFavorite = !criteria.onlyFavorites || contact.isFavorite
                    val matchesQuery = q.isEmpty() ||
                        contact.name.contains(q, ignoreCase = true) ||
                        contact.phone.contains(q, ignoreCase = true) ||
                        contact.company.contains(q, ignoreCase = true) ||
                        contact.email.contains(q, ignoreCase = true) ||
                        contact.category.storageKey.contains(q, ignoreCase = true) ||
                        contact.preferredPlatform.storageKey.contains(q, ignoreCase = true)
                    matchesSync && matchesPlatform && matchesCategory && matchesFavorite && matchesQuery
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

    fun onPlatformFilterChange(platform: ContactPlatform) {
        platformFilterFlow.value = platform
        _uiState.update { it.copy(selectedPlatformFilter = platform) }
    }

    fun onCategoryFilterChange(category: ContactCategory?) {
        categoryFilterFlow.value = category
        _uiState.update { it.copy(selectedCategoryFilter = category) }
    }

    fun toggleFavoritesFilter() {
        val next = !onlyFavoritesFilterFlow.value
        onlyFavoritesFilterFlow.value = next
        _uiState.update { it.copy(onlyFavoritesFilter = next) }
    }

    fun toggleFavorite(contact: Contact) {
        viewModelScope.launch {
            repository.toggleFavorite(contact).onFailure { err ->
                showMessage(
                    UiMessage.Resource(
                        R.string.msg_error_generic,
                        listOf(err.localizedMessage.orEmpty())
                    )
                )
            }
        }
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

    fun openBatchOrganizeDialog() {
        if (_uiState.value.selectedIds.isNotEmpty()) {
            _uiState.update { it.copy(showBatchOrganizeDialog = true) }
        }
    }

    fun dismissBatchOrganizeDialog() {
        _uiState.update { it.copy(showBatchOrganizeDialog = false) }
    }

    fun assignCategoryToSelected(category: ContactCategory) {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.updateCategoryForIds(ids, category).fold(
                onSuccess = {
                    _uiState.update { it.copy(showBatchOrganizeDialog = false, selectedIds = emptySet()) }
                    showMessage(UiMessage.Resource(R.string.msg_batch_updated, listOf(ids.size)))
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

    fun assignPlatformToSelected(platform: ContactPlatform) {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.updatePlatformForIds(ids, platform).fold(
                onSuccess = {
                    _uiState.update { it.copy(showBatchOrganizeDialog = false, selectedIds = emptySet()) }
                    showMessage(UiMessage.Resource(R.string.msg_batch_updated, listOf(ids.size)))
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

    fun openExportSheet(initialPlatform: ContactPlatform? = null) {
        if (_uiState.value.contacts.isEmpty()) {
            showMessage(UiMessage.Resource(R.string.msg_export_empty))
            return
        }
        _uiState.update { current ->
            val updatedOptions = if (initialPlatform != null) {
                current.exportOptions.copy(platformFilter = initialPlatform)
            } else {
                current.exportOptions
            }
            current.copy(
                isExportSheetVisible = true,
                exportOptions = updatedOptions
            )
        }
    }

    fun closeExportSheet() {
        _uiState.update { it.copy(isExportSheetVisible = false) }
    }

    fun updateExportOptions(options: ExportOptions) {
        _uiState.update { it.copy(exportOptions = options) }
    }

    fun exportAndShare(target: ExportTarget, chooserTitle: String, singleContact: Contact? = null) {
        viewModelScope.launch {
            val state = _uiState.value
            val options = state.exportOptions
            val contacts = singleContact?.let { listOf(it) } ?: exportContactsUseCase.filterForExport(
                allContacts = state.contacts,
                selectedIds = state.selectedIds,
                options = options
            )
            if (contacts.isEmpty()) {
                showMessage(UiMessage.Resource(R.string.msg_export_empty))
                return@launch
            }
            val prefix = if (contacts.size == 1) contacts.first().name else "contacts"
            val uriResult = exportContactsUseCase(
                contacts = contacts,
                options = if (singleContact != null) ExportOptions() else options,
                fileNamePrefix = prefix
            )
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

    fun saveExportToUri(uri: Uri) {
        viewModelScope.launch {
            val state = _uiState.value
            val options = state.exportOptions
            val contacts = exportContactsUseCase.filterForExport(
                allContacts = state.contacts,
                selectedIds = state.selectedIds,
                options = options
            )
            if (contacts.isEmpty()) {
                showMessage(UiMessage.Resource(R.string.msg_export_empty))
                return@launch
            }
            exportContactsUseCase.writeExportToUri(
                targetUri = uri,
                contacts = contacts,
                options = options,
                isFullBackup = false
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isExportSheetVisible = false) }
                    showMessage(UiMessage.Resource(R.string.msg_export_saved_file))
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

    fun openTelegram(contact: Contact) {
        viewModelScope.launch {
            _uiState.update { it.copy(whatsAppTargetContact = null) }
            openWhatsAppUseCase.openTelegram(contact.phone).onFailure {
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
                    _uiState.update { current ->
                        val defaultKeys = if (current.importOptions.duplicateStrategy == DuplicateStrategy.UPDATE_EXISTING) {
                            preview.items.map { it.key }.toSet()
                        } else {
                            preview.items.filter { !it.isDuplicate }.map { it.key }.toSet()
                        }
                        current.copy(
                            importPreview = preview,
                            selectedImportKeys = defaultKeys
                        )
                    }
                    onReady()
                },
                onFailure = {
                    showMessage(UiMessage.Resource(R.string.import_empty_error))
                }
            )
        }
    }

    fun updateImportOptions(options: ImportOptions) {
        _uiState.update { current ->
            val preview = current.importPreview
            val updatedKeys = if (preview != null && options.duplicateStrategy != current.importOptions.duplicateStrategy) {
                if (options.duplicateStrategy == DuplicateStrategy.UPDATE_EXISTING) {
                    preview.items.map { it.key }.toSet()
                } else {
                    preview.items.filter { !it.isDuplicate }.map { it.key }.toSet()
                }
            } else {
                current.selectedImportKeys
            }
            current.copy(
                importOptions = options,
                selectedImportKeys = updatedKeys
            )
        }
    }

    fun toggleImportItem(key: String) {
        _uiState.update { current ->
            val next = current.selectedImportKeys.toMutableSet()
            if (!next.add(key)) {
                next.remove(key)
            }
            current.copy(selectedImportKeys = next)
        }
    }

    fun selectAllImportItems() {
        _uiState.update { current ->
            val preview = current.importPreview ?: return@update current
            current.copy(selectedImportKeys = preview.items.map { it.key }.toSet())
        }
    }

    fun selectNewImportItemsOnly() {
        _uiState.update { current ->
            val preview = current.importPreview ?: return@update current
            current.copy(
                selectedImportKeys = preview.items.filter { !it.isDuplicate }.map { it.key }.toSet()
            )
        }
    }

    fun confirmImport(onDone: () -> Unit = {}) {
        val state = _uiState.value
        val preview = state.importPreview ?: return
        val selectedContacts = preview.items
            .filter { state.selectedImportKeys.contains(it.key) }
            .map { it.contact }
        if (selectedContacts.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            val result = importContactsUseCase.confirmImport(selectedContacts, state.importOptions)
            _uiState.update {
                it.copy(
                    isImporting = false,
                    importPreview = null,
                    selectedImportKeys = emptySet()
                )
            }
            result.fold(
                onSuccess = { summary ->
                    showMessage(
                        UiMessage.Resource(
                            R.string.import_success,
                            listOf(summary.importedCount, summary.updatedCount, summary.skippedCount)
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
        _uiState.update { it.copy(importPreview = null, selectedImportKeys = emptySet()) }
    }

    fun createBackupToUri(uri: Uri) {
        viewModelScope.launch {
            val allContacts = repository.getAllContactsSnapshot()
            if (allContacts.isEmpty()) {
                showMessage(UiMessage.Resource(R.string.msg_export_empty))
                return@launch
            }
            exportContactsUseCase.writeExportToUri(
                targetUri = uri,
                contacts = allContacts,
                options = ExportOptions(),
                isFullBackup = true
            ).fold(
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
