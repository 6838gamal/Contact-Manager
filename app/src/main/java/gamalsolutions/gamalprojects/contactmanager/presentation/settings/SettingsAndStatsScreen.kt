package gamalsolutions.gamalprojects.contactmanager.presentation.settings

import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import gamalsolutions.gamalprojects.R
import gamalsolutions.gamalprojects.contactmanager.presentation.contacts.ContactListState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * شاشة الإحصائيات والنسخ الاحتياطي وإعدادات اللغة والخصوصية.
 */
@Composable
fun SettingsAndStatsScreen(
    state: ContactListState,
    onCreateBackup: (Uri) -> Unit,
    onRestoreBackup: (Uri) -> Unit,
    onChangeLanguage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/x-vcard")
    ) { uri ->
        if (uri != null) {
            onCreateBackup(uri)
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onRestoreBackup(uri)
        }
    }

    val lastBackupFormatted = remember(state.stats.lastBackupTimestamp, state.appLanguage) {
        val ts = state.stats.lastBackupTimestamp
        if (ts == null) {
            null
        } else {
            val locale = Locale(state.appLanguage)
            SimpleDateFormat("yyyy/MM/dd HH:mm", locale).format(Date(ts))
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
            // بطاقة الإحصائيات
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                imageVector = Icons.Filled.Analytics,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.stats_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatTile(
                                label = stringResource(R.string.stat_total_contacts),
                                value = state.stats.totalContacts.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            StatTile(
                                label = stringResource(R.string.stat_synced_contacts),
                                value = state.stats.syncedContacts.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatTile(
                                label = stringResource(R.string.stat_exported),
                                value = state.stats.exportedCount.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            StatTile(
                                label = stringResource(R.string.stat_imported),
                                value = state.stats.importedCount.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            StatTile(
                                label = stringResource(R.string.stat_whatsapp_opened),
                                value = state.stats.whatsAppOpenedCount.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // بطاقة النسخ الاحتياطي والاستعادة
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                imageVector = Icons.Filled.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.backup_restore_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Text(
                            text = stringResource(
                                R.string.last_backup_label,
                                lastBackupFormatted ?: stringResource(R.string.never_backed_up)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                val datePart = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                                createBackupLauncher.launch("contacts_backup_$datePart.vcf")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("create_backup_button")
                        ) {
                            Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.backup_create))
                        }

                        OutlinedButton(
                            onClick = {
                                restoreBackupLauncher.launch(arrayOf("text/x-vcard", "text/vcard", "*/*"))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("restore_backup_button")
                        ) {
                            Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.backup_restore))
                        }
                    }
                }
            }

            // بطاقة اللغة والاتجاه (RTL / LTR)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                                imageVector = Icons.Filled.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.language_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = state.appLanguage == "ar",
                                onClick = { onChangeLanguage("ar") },
                                label = { Text(stringResource(R.string.language_arabic)) },
                                modifier = Modifier.testTag("lang_arabic_chip")
                            )
                            FilterChip(
                                selected = state.appLanguage == "en",
                                onClick = { onChangeLanguage("en") },
                                label = { Text(stringResource(R.string.language_english)) },
                                modifier = Modifier.testTag("lang_english_chip")
                            )
                        }
                    }
                }
            }

            // بطاقة ملاحظة الخصوصية والتكامل
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.privacy_note_title),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Text(
                            text = stringResource(R.string.privacy_note_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
