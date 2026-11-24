package gamalsolutions.gamalprojects.contactmanager.presentation.edit

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gamalsolutions.gamalprojects.R
import gamalsolutions.gamalprojects.contactmanager.data.model.Contact
import gamalsolutions.gamalprojects.contactmanager.data.model.ContactCategory
import gamalsolutions.gamalprojects.contactmanager.data.model.ContactPlatform
import gamalsolutions.gamalprojects.contactmanager.data.repository.ContactRepository
import gamalsolutions.gamalprojects.contactmanager.utils.VCardHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditContactState(
    val contactId: Long = 0L,
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val company: String = "",
    val notes: String = "",
    val photoUri: String? = null,
    val category: ContactCategory = ContactCategory.GENERAL,
    val preferredPlatform: ContactPlatform = ContactPlatform.ALL,
    val isFavorite: Boolean = false,
    val systemContactId: Long? = null,
    val isSyncedToDevice: Boolean = false,
    val alsoSaveToPhonebook: Boolean = true,
    @StringRes val nameErrorRes: Int? = null,
    @StringRes val phoneErrorRes: Int? = null,
    @StringRes val emailErrorRes: Int? = null,
    val isSaving: Boolean = false
)

/**
 * ViewModel لإضافة أو تعديل جهة اتصال مع التنظيم (التصنيف، المنصة المفضلة، المفضلة) والتحقق الفوري.
 */
class EditContactViewModel(
    private val repository: ContactRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EditContactState())
    val state: StateFlow<EditContactState> = _state.asStateFlow()

    fun loadContact(contactId: Long?) {
        if (contactId == null || contactId <= 0L) {
            _state.value = EditContactState()
            return
        }
        viewModelScope.launch {
            val contact = repository.getContactById(contactId)
            if (contact != null) {
                _state.value = EditContactState(
                    contactId = contact.id,
                    name = contact.name,
                    phone = contact.phone,
                    email = contact.email,
                    company = contact.company,
                    notes = contact.notes,
                    photoUri = contact.photoUri,
                    category = contact.category,
                    preferredPlatform = contact.preferredPlatform,
                    isFavorite = contact.isFavorite,
                    systemContactId = contact.systemContactId,
                    isSyncedToDevice = contact.isSyncedToDevice,
                    alsoSaveToPhonebook = contact.isSyncedToDevice
                )
            }
        }
    }

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value, nameErrorRes = null) }
    }

    fun onPhoneChange(value: String) {
        _state.update { it.copy(phone = value, phoneErrorRes = null) }
    }

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, emailErrorRes = null) }
    }

    fun onCompanyChange(value: String) {
        _state.update { it.copy(company = value) }
    }

    fun onNotesChange(value: String) {
        _state.update { it.copy(notes = value) }
    }

    fun onPhotoSelected(uriString: String?) {
        _state.update { it.copy(photoUri = uriString) }
    }

    fun onCategoryChange(category: ContactCategory) {
        _state.update { it.copy(category = category) }
    }

    fun onPlatformChange(platform: ContactPlatform) {
        _state.update { it.copy(preferredPlatform = platform) }
    }

    fun onFavoriteChange(isFavorite: Boolean) {
        _state.update { it.copy(isFavorite = isFavorite) }
    }

    fun onAlsoSaveToPhonebookChange(checked: Boolean) {
        _state.update { it.copy(alsoSaveToPhonebook = checked) }
    }

    fun saveContact(
        hasWritePermission: Boolean,
        onSaved: () -> Unit,
        onError: (String) -> Unit
    ) {
        val current = _state.value
        val nameErr = if (current.name.isBlank()) R.string.error_name_required else null

        val normalizedDigits = VCardHelper.normalizePhone(current.phone).filter { it.isDigit() }
        val phoneErr = when {
            current.phone.isBlank() -> R.string.error_phone_required
            normalizedDigits.length < 7 || normalizedDigits.length > 15 -> R.string.error_phone_invalid
            else -> null
        }

        val emailErr = if (current.email.isNotBlank() &&
            !Patterns.EMAIL_ADDRESS.matcher(current.email.trim()).matches()
        ) {
            R.string.error_email_invalid
        } else {
            null
        }

        if (nameErr != null || phoneErr != null || emailErr != null) {
            _state.update {
                it.copy(
                    nameErrorRes = nameErr,
                    phoneErrorRes = phoneErr,
                    emailErrorRes = emailErr
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val contact = Contact(
                id = current.contactId,
                name = current.name.trim(),
                phone = current.phone.trim(),
                email = current.email.trim(),
                company = current.company.trim(),
                notes = current.notes.trim(),
                photoUri = current.photoUri,
                category = current.category,
                preferredPlatform = current.preferredPlatform,
                isFavorite = current.isFavorite,
                systemContactId = current.systemContactId,
                isSyncedToDevice = current.isSyncedToDevice
            )
            val result = repository.saveContact(
                contact = contact,
                alsoSaveToPhonebook = current.alsoSaveToPhonebook && hasWritePermission
            )
            _state.update { it.copy(isSaving = false) }
            result.fold(
                onSuccess = { onSaved() },
                onFailure = { err -> onError(err.localizedMessage.orEmpty()) }
            )
        }
    }
}
