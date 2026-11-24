package gamalsolutions.gamalprojects.contactmanager.presentation.import

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import gamalsolutions.gamalprojects.R
import gamalsolutions.gamalprojects.contactmanager.data.model.ContactCategory
import gamalsolutions.gamalprojects.contactmanager.data.model.ContactPlatform
import gamalsolutions.gamalprojects.contactmanager.data.model.DuplicateStrategy
import gamalsolutions.gamalprojects.contactmanager.data.model.ImportOptions
import gamalsolutions.gamalprojects.contactmanager.data.model.ImportPreview
import gamalsolutions.gamalprojects.contactmanager.data.model.ImportPreviewItem
import gamalsolutions.gamalprojects.contactmanager.presentation.contacts.ContactListState
import gamalsolutions.gamalprojects.ui.theme.TelegramBlue
import gamalsolutions.gamalprojects.ui.theme.WhatsAppBusinessTeal
import gamalsolutions.gamalprojects.ui.theme.WhatsAppGreen

/**
 * شاشة التصدير والاستيراد المخصص (vCard .vcf) مع التحكم في المنصة والتصنيف، سياسة التكرار، واختيار الجهات فردياً.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportExportScreen(
    state: ContactListState,
    onOpenExportSheet: () -> Unit,
    onOpenExportSheetWithPlatform: (ContactPlatform) -> Unit,
    onPickVcfFile: (Uri) -> Unit,
    onUpdateImportOptions: (ImportOptions) -> Unit,
    onToggleImportItem: (String) -> Unit,
    onSelectAllImportItems: () -> Unit,
    onSelectNewImportItemsOnly: () -> Unit,
    onConfirmImport: () -> Unit,
    onClearPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onPickVcfFile(uri)
        }
    }

    val importOptions = state.importOptions
    val preview = state.importPreview

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .testTag("import_export_lazy_column"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // بطاقة التصدير السريع والمخصص إلى واتساب / واتساب الأعمال / تيليجرام
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.FileUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.export_sheet_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = stringResource(R.string.export_sheet_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )

                        // اختصارات تصدير سريعة مفلترة حسب المنصة
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { onOpenExportSheetWithPlatform(ContactPlatform.WHATSAPP) },
                                modifier = Modifier.testTag("quick_export_whatsapp_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Chat,
                                    contentDescription = null,
                                    tint = WhatsAppGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.platform_whatsapp))
                            }
                            OutlinedButton(
                                onClick = { onOpenExportSheetWithPlatform(ContactPlatform.WHATSAPP_BUSINESS) },
                                modifier = Modifier.testTag("quick_export_wab_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.BusinessCenter,
                                    contentDescription = null,
                                    tint = WhatsAppBusinessTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.platform_whatsapp_business))
                            }
                            OutlinedButton(
                                onClick = { onOpenExportSheetWithPlatform(ContactPlatform.TELEGRAM) },
                                modifier = Modifier.testTag("quick_export_telegram_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    tint = TelegramBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.platform_telegram))
                            }
                        }

                        Button(
                            onClick = onOpenExportSheet,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("open_export_sheet_button")
                        ) {
                            Icon(Icons.Filled.FileUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.export_all_action,
                                    state.contactsToExport.size
                                )
                            )
                        }
                    }
                }
            }

            // بطاقة الاستيراد وخيارات التنظيم عبر SAF
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.FileDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.import_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        // اختيار مصدر الملف (واتساب، واتساب الأعمال، تيليجرام)
                        Text(
                            text = stringResource(R.string.import_source_platform),
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
                                    selected = importOptions.sourcePlatform == plat,
                                    onClick = { onUpdateImportOptions(importOptions.copy(sourcePlatform = plat)) },
                                    label = { Text(stringResource(plat.labelRes)) },
                                    modifier = Modifier.testTag("import_opt_platform_${plat.storageKey.lowercase()}")
                                )
                            }
                        }

                        // تصنيف الجهات المستوردة
                        Text(
                            text = stringResource(R.string.import_assign_category),
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
                                    selected = importOptions.assignCategory == cat,
                                    onClick = { onUpdateImportOptions(importOptions.copy(assignCategory = cat)) },
                                    label = { Text(stringResource(cat.labelRes)) },
                                    modifier = Modifier.testTag("import_opt_cat_${cat.storageKey.lowercase()}")
                                )
                            }
                        }

                        // سياسة الأرقام المكررة
                        Text(
                            text = stringResource(R.string.import_duplicate_policy),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = importOptions.duplicateStrategy == DuplicateStrategy.SKIP,
                                onClick = {
                                    onUpdateImportOptions(
                                        importOptions.copy(duplicateStrategy = DuplicateStrategy.SKIP)
                                    )
                                },
                                label = { Text(stringResource(R.string.import_duplicate_skip)) },
                                modifier = Modifier.testTag("import_dup_skip_chip")
                            )
                            FilterChip(
                                selected = importOptions.duplicateStrategy == DuplicateStrategy.UPDATE_EXISTING,
                                onClick = {
                                    onUpdateImportOptions(
                                        importOptions.copy(duplicateStrategy = DuplicateStrategy.UPDATE_EXISTING)
                                    )
                                },
                                label = { Text(stringResource(R.string.import_duplicate_update)) },
                                modifier = Modifier.testTag("import_dup_update_chip")
                            )
                        }

                        // بادئة اختيارية للأسماء المستوردة
                        OutlinedTextField(
                            value = importOptions.namePrefix,
                            onValueChange = { onUpdateImportOptions(importOptions.copy(namePrefix = it)) },
                            label = { Text(stringResource(R.string.import_name_prefix)) },
                            placeholder = { Text(stringResource(R.string.export_name_prefix_hint)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("import_prefix_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("WA -", "WAB -", "TG -").forEach { quickPrefix ->
                                SuggestionChip(
                                    onClick = {
                                        val next = if (importOptions.namePrefix == quickPrefix) "" else quickPrefix
                                        onUpdateImportOptions(importOptions.copy(namePrefix = next))
                                    },
                                    label = { Text(quickPrefix) }
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = {
                                openDocumentLauncher.launch(
                                    arrayOf("text/x-vcard", "text/vcard", "*/*")
                                )
                            },
                            enabled = !state.isParsingImport && !state.isImporting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pick_vcf_file_button")
                        ) {
                            if (state.isParsingImport) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(Icons.Filled.FolderOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = stringResource(R.string.import_pick_file))
                            }
                        }
                    }
                }
            }

            // قسم معاينة الملف المختار أو الوارد عبر Intent مع إمكانية اختيار كل جهة على حدة
            if (preview != null) {
                item {
                    ImportPreviewSummaryCard(
                        preview = preview,
                        selectedCount = state.selectedImportKeys.size,
                        alsoSaveToPhonebook = importOptions.alsoSaveToPhonebook,
                        isImporting = state.isImporting,
                        onSelectAll = onSelectAllImportItems,
                        onSelectNewOnly = onSelectNewImportItemsOnly,
                        onTogglePhonebook = { checked ->
                            onUpdateImportOptions(importOptions.copy(alsoSaveToPhonebook = checked))
                        },
                        onConfirm = onConfirmImport,
                        onCancel = onClearPreview
                    )
                }

                items(preview.items, key = { it.key }) { item ->
                    val isChecked = state.selectedImportKeys.contains(item.key)
                    PreviewContactRow(
                        item = item,
                        isChecked = isChecked,
                        onToggle = { onToggleImportItem(item.key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportPreviewSummaryCard(
    preview: ImportPreview,
    selectedCount: Int,
    alsoSaveToPhonebook: Boolean,
    isImporting: Boolean,
    onSelectAll: () -> Unit,
    onSelectNewOnly: () -> Unit,
    onTogglePhonebook: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.import_preview_header),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.import_total_found, preview.totalCount),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.import_new_count, preview.newCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.import_duplicate_count, preview.duplicateCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(R.string.import_selected_preview, selectedCount),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onSelectAll,
                    modifier = Modifier.testTag("import_select_all_button")
                ) {
                    Text(stringResource(R.string.select_all))
                }
                TextButton(
                    onClick = onSelectNewOnly,
                    modifier = Modifier.testTag("import_select_new_only_button")
                ) {
                    Text(stringResource(R.string.import_select_new_only))
                }
            }

            HorizontalDivider()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = alsoSaveToPhonebook,
                    onCheckedChange = onTogglePhonebook,
                    modifier = Modifier.testTag("import_phonebook_checkbox")
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.import_also_phonebook),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isImporting,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("cancel_import_button")
                ) {
                    Text(text = stringResource(R.string.cancel))
                }

                Button(
                    onClick = onConfirm,
                    enabled = !isImporting && selectedCount > 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("confirm_import_button")
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(text = stringResource(R.string.import_confirm, selectedCount))
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewContactRow(
    item: ImportPreviewItem,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = MaterialTheme.shapes.medium,
        color = if (item.isDuplicate) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("import_preview_row_${item.key}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("import_preview_checkbox_${item.key}")
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.contact.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = item.contact.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (item.isDuplicate) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            ) {
                Text(
                    text = stringResource(
                        if (item.isDuplicate) R.string.import_badge_duplicate else R.string.import_badge_new
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.isDuplicate) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
