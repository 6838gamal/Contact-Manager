package gamalsolutions.gamalprojects.contactmanager.presentation.edit

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import gamalsolutions.gamalprojects.R
import gamalsolutions.gamalprojects.contactmanager.data.model.ContactCategory
import gamalsolutions.gamalprojects.contactmanager.data.model.ContactPlatform
import gamalsolutions.gamalprojects.contactmanager.utils.PermissionHelper

/**
 * شاشة إضافة أو تعديل جهة اتصال مع خيارات التصنيف والمنصة المفضلة والمفضلة واختيار صورة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContactScreen(
    contactId: Long?,
    viewModel: EditContactViewModel,
    onBack: () -> Unit,
    onSavedSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler(onBack = onBack)

    LaunchedEffect(contactId) {
        viewModel.loadContact(contactId)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onPhotoSelected(uri.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (state.contactId == 0L) R.string.add_contact else R.string.edit_contact
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("edit_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onFavoriteChange(!state.isFavorite) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("edit_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (state.isFavorite) Icons.Filled.Star else Icons.Filled.StarOutline,
                            contentDescription = stringResource(R.string.favorite_toggle),
                            tint = if (state.isFavorite) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        if (!state.photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = state.photoUri,
                                contentDescription = state.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("pick_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(R.string.choose_photo))
                        }

                        if (!state.photoUri.isNullOrBlank()) {
                            TextButton(
                                onClick = { viewModel.onPhotoSelected(null) },
                                modifier = Modifier.testTag("remove_photo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = stringResource(R.string.remove_photo))
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text(stringResource(R.string.field_name)) },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    isError = state.nameErrorRes != null,
                    supportingText = state.nameErrorRes?.let { res ->
                        { Text(stringResource(res)) }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_name_input")
                )

                OutlinedTextField(
                    value = state.phone,
                    onValueChange = viewModel::onPhoneChange,
                    label = { Text(stringResource(R.string.field_phone)) },
                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                    isError = state.phoneErrorRes != null,
                    supportingText = state.phoneErrorRes?.let { res ->
                        { Text(stringResource(res)) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_phone_input")
                )

                // اختيار التصنيف
                Text(
                    text = stringResource(R.string.category_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ContactCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = state.category == cat,
                            onClick = { viewModel.onCategoryChange(cat) },
                            label = { Text(stringResource(cat.labelRes)) },
                            modifier = Modifier.testTag("edit_category_${cat.storageKey.lowercase()}")
                        )
                    }
                }

                // اختيار المنصة المفضلة
                Text(
                    text = stringResource(R.string.platform_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ContactPlatform.entries.forEach { plat ->
                        FilterChip(
                            selected = state.preferredPlatform == plat,
                            onClick = { viewModel.onPlatformChange(plat) },
                            label = { Text(stringResource(plat.labelRes)) },
                            modifier = Modifier.testTag("edit_platform_${plat.storageKey.lowercase()}")
                        )
                    }
                }

                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text(stringResource(R.string.field_email)) },
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                    isError = state.emailErrorRes != null,
                    supportingText = state.emailErrorRes?.let { res ->
                        { Text(stringResource(res)) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_email_input")
                )

                OutlinedTextField(
                    value = state.company,
                    onValueChange = viewModel::onCompanyChange,
                    label = { Text(stringResource(R.string.field_company)) },
                    leadingIcon = { Icon(Icons.Filled.Business, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_company_input")
                )

                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::onNotesChange,
                    label = { Text(stringResource(R.string.field_notes)) },
                    leadingIcon = { Icon(Icons.Filled.Notes, contentDescription = null) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_notes_input")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.favorite_toggle),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = state.isFavorite,
                        onCheckedChange = viewModel::onFavoriteChange,
                        modifier = Modifier.testTag("edit_favorite_switch")
                    )
                }

                if (!state.isSyncedToDevice) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = state.alsoSaveToPhonebook,
                            onCheckedChange = viewModel::onAlsoSaveToPhonebookChange,
                            modifier = Modifier.testTag("save_to_phonebook_checkbox")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.save_to_phonebook_also),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("cancel_edit_button")
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }

                    Button(
                        onClick = {
                            viewModel.saveContact(
                                hasWritePermission = PermissionHelper.hasWriteContactsPermission(context),
                                onSaved = onSavedSuccess,
                                onError = onError
                            )
                        },
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("save_contact_button")
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(text = stringResource(R.string.save))
                        }
                    }
                }
            }
        }
    }
}
