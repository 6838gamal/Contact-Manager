package gamalsolutions.gamalprojects.contactmanager.presentation.contacts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gamalsolutions.gamalprojects.R
import gamalsolutions.gamalprojects.contactmanager.data.model.Contact
import gamalsolutions.gamalprojects.contactmanager.presentation.components.ContactItem
import gamalsolutions.gamalprojects.contactmanager.presentation.components.ContactsPermissionCard
import gamalsolutions.gamalprojects.contactmanager.utils.PermissionHelper

/**
 * الشاشة الرئيسية لقائمة جهات الاتصال مع البحث الفوري والتصفية والتحديد المتعدد.
 */
@Composable
fun ContactListScreen(
    state: ContactListState,
    onSearchQueryChange: (String) -> Unit,
    onToggleSyncedFilter: () -> Unit,
    onToggleSelection: (Long) -> Unit,
    onEditContact: (Contact) -> Unit,
    onDeleteContact: (Contact) -> Unit,
    onWhatsAppClick: (Contact) -> Unit,
    onQrClick: (Contact) -> Unit,
    onShareSingleClick: (Contact) -> Unit,
    onSyncPhonebook: () -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermissions by remember {
        mutableStateOf(PermissionHelper.hasAllContactsPermissions(context))
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
        ) {
            // شريط البحث الفوري
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.clear_selection)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("search_contacts_input")
            )

            // شرائح التصفية وملخص العدد
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !state.showOnlySynced,
                    onClick = { if (state.showOnlySynced) onToggleSyncedFilter() },
                    label = {
                        Text("${stringResource(R.string.nav_contacts)} (${state.stats.totalContacts})")
                    },
                    modifier = Modifier.testTag("filter_all_chip")
                )
                FilterChip(
                    selected = state.showOnlySynced,
                    onClick = { if (!state.showOnlySynced) onToggleSyncedFilter() },
                    label = {
                        Text("${stringResource(R.string.stat_synced_contacts)} (${state.stats.syncedContacts})")
                    },
                    modifier = Modifier.testTag("filter_synced_chip")
                )
            }

            AnimatedVisibility(visible = !hasPermissions) {
                ContactsPermissionCard(
                    onPermissionsGranted = {
                        hasPermissions = true
                        onSyncPhonebook()
                    },
                    onPermissionsDenied = onPermissionDenied,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.filteredContacts.isEmpty() -> {
                    EmptyContactsState(
                        isSearching = state.searchQuery.isNotBlank(),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("contacts_lazy_column"),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = state.filteredContacts,
                            key = { it.id }
                        ) { contact ->
                            val isSelected = state.selectedIds.contains(contact.id)
                            ContactItem(
                                contact = contact,
                                isSelected = isSelected,
                                isSelectionMode = state.isSelectionMode,
                                onClick = {
                                    if (state.isSelectionMode) {
                                        onToggleSelection(contact.id)
                                    } else {
                                        onEditContact(contact)
                                    }
                                },
                                onLongClick = { onToggleSelection(contact.id) },
                                onWhatsAppClick = { onWhatsAppClick(contact) },
                                onQrClick = { onQrClick(contact) },
                                onShareSingleClick = { onShareSingleClick(contact) },
                                onEditClick = { onEditContact(contact) },
                                onDeleteClick = { onDeleteContact(contact) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyContactsState(
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_contacts),
            contentDescription = stringResource(R.string.empty_contacts_title),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 240.dp, height = 180.dp)
                .clip(MaterialTheme.shapes.large)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(
                if (isSearching) R.string.empty_search_title else R.string.empty_contacts_title
            ),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                if (isSearching) R.string.empty_search_subtitle else R.string.empty_contacts_subtitle
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
