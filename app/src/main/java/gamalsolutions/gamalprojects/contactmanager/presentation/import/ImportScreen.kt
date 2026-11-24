package gamalsolutions.gamalprojects.contactmanager.presentation.import

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import gamalsolutions.gamalprojects.R
import gamalsolutions.gamalprojects.contactmanager.data.model.Contact
import gamalsolutions.gamalprojects.contactmanager.data.model.ImportPreview
import gamalsolutions.gamalprojects.contactmanager.presentation.contacts.ContactListState

/**
 * شاشة التصدير والاستيراد (vCard .vcf) مع معاينة الجهات ومنع التكرار حسب رقم الهاتف.
 */
@Composable
fun ImportExportScreen(
    state: ContactListState,
    onOpenExportSheet: () -> Unit,
    onPickVcfFile: (android.net.Uri) -> Unit,
    onConfirmImport: (alsoSaveToPhonebook: Boolean) -> Unit,
    onClearPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    var alsoSaveToPhonebook by remember { mutableStateOf(true) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onPickVcfFile(uri)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // بطاقة التصدير السريع إلى واتساب / تيليجرام / مشاركة عامة
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
                                    state.contacts.size
                                )
                            )
                        }
                    }
                }
            }

            // بطاقة الاستيراد عبر SAF
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

            // قسم معاينة الملف المختار أو الوارد عبر Intent
            val preview = state.importPreview
            if (preview != null) {
                item {
                    ImportPreviewSummaryCard(
                        preview = preview,
                        alsoSaveToPhonebook = alsoSaveToPhonebook,
                        isImporting = state.isImporting,
                        onTogglePhonebook = { alsoSaveToPhonebook = it },
                        onConfirm = { onConfirmImport(alsoSaveToPhonebook) },
                        onCancel = onClearPreview
                    )
                }

                items(preview.newContacts, key = { "new_${it.phone}_${it.name}" }) { contact ->
                    PreviewContactRow(contact = contact, isDuplicate = false)
                }

                items(preview.duplicateContacts, key = { "dup_${it.phone}_${it.name}" }) { contact ->
                    PreviewContactRow(contact = contact, isDuplicate = true)
                }
            }
        }
    }
}

@Composable
private fun ImportPreviewSummaryCard(
    preview: ImportPreview,
    alsoSaveToPhonebook: Boolean,
    isImporting: Boolean,
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
                    enabled = !isImporting && preview.newCount > 0,
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
                        Text(text = stringResource(R.string.import_confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewContactRow(
    contact: Contact,
    isDuplicate: Boolean
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isDuplicate) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = if (isDuplicate) Icons.Filled.ContentCopy else Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (isDuplicate) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = contact.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
