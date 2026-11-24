package gamalsolutions.gamalprojects

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamalsolutions.gamalprojects.contactmanager.di.AppModule
import gamalsolutions.gamalprojects.contactmanager.presentation.components.ExportBottomSheet
import gamalsolutions.gamalprojects.contactmanager.presentation.components.QrCodeDialog
import gamalsolutions.gamalprojects.contactmanager.presentation.components.WhatsAppChooserDialog
import gamalsolutions.gamalprojects.contactmanager.presentation.contacts.ContactListScreen
import gamalsolutions.gamalprojects.contactmanager.presentation.contacts.ContactListViewModel
import gamalsolutions.gamalprojects.contactmanager.presentation.contacts.UiMessage
import gamalsolutions.gamalprojects.contactmanager.presentation.edit.EditContactScreen
import gamalsolutions.gamalprojects.contactmanager.presentation.edit.EditContactViewModel
import gamalsolutions.gamalprojects.contactmanager.presentation.import.ImportExportScreen
import gamalsolutions.gamalprojects.contactmanager.presentation.settings.SettingsAndStatsScreen
import gamalsolutions.gamalprojects.contactmanager.presentation.splash.SplashScreen
import gamalsolutions.gamalprojects.contactmanager.utils.LocaleHelper
import gamalsolutions.gamalprojects.contactmanager.utils.PermissionHelper
import gamalsolutions.gamalprojects.ui.theme.ContactHubTheme

private enum class MainTab {
    CONTACTS,
    TRANSFER,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private lateinit var appModule: AppModule
    private lateinit var listViewModel: ContactListViewModel
    private lateinit var editViewModel: EditContactViewModel
    private var pendingIncomingTabSwitch by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appModule = AppModule.getInstance(applicationContext)
        val factory = appModule.viewModelFactory()
        listViewModel = ViewModelProvider(this, factory)[ContactListViewModel::class.java]
        editViewModel = ViewModelProvider(this, factory)[EditContactViewModel::class.java]

        val launchedFromVCardIntent = handleIncomingVCardIntent(intent)

        setContent {
            val state by listViewModel.uiState.collectAsStateWithLifecycle()
            val localizedContext = remember(state.appLanguage, this) {
                LocaleHelper.createLocalizedContext(this, state.appLanguage)
            }
            val layoutDirection = LocaleHelper.layoutDirectionFor(state.appLanguage)
            var showSplash by rememberSaveable { mutableStateOf(!launchedFromVCardIntent) }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalLayoutDirection provides layoutDirection,
                LocalActivityResultRegistryOwner provides this,
                LocalOnBackPressedDispatcherOwner provides this
            ) {
                ContactHubTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ContactManagerApp(
                            listViewModel = listViewModel,
                            editViewModel = editViewModel,
                            switchToTransferTab = pendingIncomingTabSwitch,
                            onConsumedTransferTab = { pendingIncomingTabSwitch = false }
                        )

                        AnimatedVisibility(
                            visible = showSplash,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            SplashScreen(
                                onTimeout = { showSplash = false }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingVCardIntent(intent)
    }

    private fun handleIncomingVCardIntent(intent: Intent?): Boolean {
        if (intent == null) return false
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                } ?: intent.clipData?.getItemAt(0)?.uri
            }
            else -> null
        }

        if (uri != null) {
            listViewModel.loadImportPreview(uri) {
                pendingIncomingTabSwitch = true
            }
            return true
        }
        return false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactManagerApp(
    listViewModel: ContactListViewModel,
    editViewModel: EditContactViewModel,
    switchToTransferTab: Boolean,
    onConsumedTransferTab: () -> Unit
) {
    val state by listViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by rememberSaveable { mutableStateOf(MainTab.CONTACTS) }
    var editingContactId by rememberSaveable { mutableStateOf<Long?>(null) }
    var isEditScreenOpen by rememberSaveable { mutableStateOf(false) }

    val syncPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            listViewModel.syncWithDevicePhonebook()
        } else {
            listViewModel.notifyMessage(R.string.permission_denied_msg)
        }
    }

    LaunchedEffect(switchToTransferTab) {
        if (switchToTransferTab) {
            currentTab = MainTab.TRANSFER
            onConsumedTransferTab()
        }
    }

    LaunchedEffect(state.snackbarMessage, state.appLanguage) {
        val msg = state.snackbarMessage ?: return@LaunchedEffect
        val resolved = when (msg) {
            is UiMessage.Resource -> {
                if (msg.args.isEmpty()) {
                    context.getString(msg.resId)
                } else {
                    context.getString(msg.resId, *msg.args.toTypedArray())
                }
            }
            is UiMessage.Text -> msg.message
        }
        snackbarHostState.showSnackbar(resolved)
        listViewModel.consumeSnackbarMessage()
    }

    if (isEditScreenOpen) {
        EditContactScreen(
            contactId = editingContactId,
            viewModel = editViewModel,
            onBack = { isEditScreenOpen = false },
            onSavedSuccess = {
                isEditScreenOpen = false
                listViewModel.notifyMessage(R.string.msg_contact_saved)
            },
            onError = { err ->
                listViewModel.notifyMessage(R.string.msg_error_generic, err)
            }
        )
        return
    }

    if (currentTab != MainTab.CONTACTS) {
        BackHandler { currentTab = MainTab.CONTACTS }
    }

    val chooserTitle = stringResource(R.string.share_general)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    title = {
                        if (state.isSelectionMode && currentTab == MainTab.CONTACTS) {
                            Text(
                                text = stringResource(R.string.selected_count, state.selectedIds.size),
                                style = MaterialTheme.typography.titleLarge
                            )
                        } else {
                            Text(
                                text = when (currentTab) {
                                    MainTab.CONTACTS -> stringResource(R.string.app_name)
                                    MainTab.TRANSFER -> stringResource(R.string.nav_transfer)
                                    MainTab.SETTINGS -> stringResource(R.string.nav_settings)
                                },
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    actions = {
                        if (currentTab == MainTab.CONTACTS) {
                            if (state.isSelectionMode) {
                                IconButton(
                                    onClick = listViewModel::selectAllVisible,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("select_all_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SelectAll,
                                        contentDescription = stringResource(R.string.select_all)
                                    )
                                }
                                IconButton(
                                    onClick = listViewModel::openExportSheet,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("export_selected_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FileUpload,
                                        contentDescription = stringResource(R.string.export_selected)
                                    )
                                }
                                IconButton(
                                    onClick = listViewModel::deleteSelectedContacts,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("delete_selected_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.delete_selected),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                IconButton(
                                    onClick = listViewModel::clearSelection,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("clear_selection_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.clear_selection)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        if (PermissionHelper.hasReadContactsPermission(context)) {
                                            listViewModel.syncWithDevicePhonebook()
                                        } else {
                                            syncPermissionLauncher.launch(PermissionHelper.CONTACTS_PERMISSIONS)
                                        }
                                    },
                                    enabled = !state.isSyncing,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("sync_phonebook_button")
                                ) {
                                    if (state.isSyncing) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Sync,
                                            contentDescription = stringResource(R.string.sync_device_contacts)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = listViewModel::openExportSheet,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("top_export_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FileUpload,
                                        contentDescription = stringResource(R.string.nav_transfer)
                                    )
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentTab == MainTab.CONTACTS,
                            onClick = { currentTab = MainTab.CONTACTS },
                            icon = { Icon(Icons.Filled.Contacts, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_contacts)) },
                            modifier = Modifier.testTag("nav_tab_contacts")
                        )
                        NavigationBarItem(
                            selected = currentTab == MainTab.TRANSFER,
                            onClick = { currentTab = MainTab.TRANSFER },
                            icon = { Icon(Icons.Filled.ImportExport, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_transfer)) },
                            modifier = Modifier.testTag("nav_tab_transfer")
                        )
                        NavigationBarItem(
                            selected = currentTab == MainTab.SETTINGS,
                            onClick = { currentTab = MainTab.SETTINGS },
                            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_settings)) },
                            modifier = Modifier.testTag("nav_tab_settings")
                        )
                    }
                }
            },
            floatingActionButton = {
                if (currentTab == MainTab.CONTACTS && !state.isSelectionMode) {
                    FloatingActionButton(
                        onClick = {
                            editingContactId = null
                            isEditScreenOpen = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("add_contact_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.add_contact)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail {
                        NavigationRailItem(
                            selected = currentTab == MainTab.CONTACTS,
                            onClick = { currentTab = MainTab.CONTACTS },
                            icon = { Icon(Icons.Filled.Contacts, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_contacts)) }
                        )
                        NavigationRailItem(
                            selected = currentTab == MainTab.TRANSFER,
                            onClick = { currentTab = MainTab.TRANSFER },
                            icon = { Icon(Icons.Filled.ImportExport, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_transfer)) }
                        )
                        NavigationRailItem(
                            selected = currentTab == MainTab.SETTINGS,
                            onClick = { currentTab = MainTab.SETTINGS },
                            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_settings)) }
                        )
                    }
                }

                when (currentTab) {
                    MainTab.CONTACTS -> {
                        ContactListScreen(
                            state = state,
                            onSearchQueryChange = listViewModel::onSearchQueryChange,
                            onToggleSyncedFilter = listViewModel::toggleSyncedFilter,
                            onToggleSelection = listViewModel::toggleSelection,
                            onEditContact = { contact ->
                                editingContactId = contact.id
                                isEditScreenOpen = true
                            },
                            onDeleteContact = listViewModel::deleteContact,
                            onWhatsAppClick = listViewModel::requestWhatsAppChat,
                            onQrClick = listViewModel::showQrCode,
                            onShareSingleClick = { contact ->
                                listViewModel.exportAndShare(
                                    target = gamalsolutions.gamalprojects.contactmanager.data.model.ExportTarget.GENERAL,
                                    chooserTitle = chooserTitle,
                                    singleContact = contact
                                )
                            },
                            onSyncPhonebook = listViewModel::syncWithDevicePhonebook,
                            onPermissionDenied = {
                                listViewModel.notifyMessage(R.string.permission_denied_msg)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    MainTab.TRANSFER -> {
                        ImportExportScreen(
                            state = state,
                            onOpenExportSheet = listViewModel::openExportSheet,
                            onPickVcfFile = { uri -> listViewModel.loadImportPreview(uri) },
                            onConfirmImport = { alsoPhonebook ->
                                listViewModel.confirmImport(alsoPhonebook) {
                                    currentTab = MainTab.CONTACTS
                                }
                            },
                            onClearPreview = listViewModel::clearImportPreview,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    MainTab.SETTINGS -> {
                        SettingsAndStatsScreen(
                            state = state,
                            onCreateBackup = listViewModel::createBackupToUri,
                            onRestoreBackup = { uri ->
                                listViewModel.loadImportPreview(uri) {
                                    currentTab = MainTab.TRANSFER
                                }
                            },
                            onChangeLanguage = listViewModel::setAppLanguage,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (state.isExportSheetVisible) {
        ExportBottomSheet(
            exportCount = state.contactsToExport.size,
            isSelectionMode = state.isSelectionMode,
            onDismiss = listViewModel::closeExportSheet,
            onSelectTarget = { target ->
                listViewModel.exportAndShare(target = target, chooserTitle = chooserTitle)
            }
        )
    }

    state.qrContact?.let { qrContact ->
        QrCodeDialog(
            contact = qrContact,
            onDismiss = listViewModel::dismissQrCode,
            onShareVCard = {
                listViewModel.dismissQrCode()
                listViewModel.exportAndShare(
                    target = gamalsolutions.gamalprojects.contactmanager.data.model.ExportTarget.GENERAL,
                    chooserTitle = chooserTitle,
                    singleContact = qrContact
                )
            }
        )
    }

    state.whatsAppTargetContact?.let { targetContact ->
        WhatsAppChooserDialog(
            contact = targetContact,
            onDismiss = listViewModel::dismissWhatsAppChooser,
            onSelect = { type -> listViewModel.openWhatsApp(targetContact, type) }
        )
    }
}
