package gamalsolutions.gamalprojects.contactmanager.presentation.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import gamalsolutions.gamalprojects.R
import gamalsolutions.gamalprojects.contactmanager.data.model.ContactCategory
import gamalsolutions.gamalprojects.contactmanager.data.model.ContactPlatform
import gamalsolutions.gamalprojects.contactmanager.data.model.ExportOptions
import gamalsolutions.gamalprojects.contactmanager.data.model.ExportTarget
import gamalsolutions.gamalprojects.contactmanager.utils.ShareHelper
import gamalsolutions.gamalprojects.ui.theme.TelegramBlue
import gamalsolutions.gamalprojects.ui.theme.WhatsAppBusinessTeal
import gamalsolutions.gamalprojects.ui.theme.WhatsAppGreen

/**
 * قائمة سفلية (ModalBottomSheet) لتخصيص التصدير والمشاركة بصيغة vCard 3.0 إلى واتساب، واتساب الأعمال، وتيليجرام.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    exportCount: Int,
    isSelectionMode: Boolean,
    exportOptions: ExportOptions,
    onUpdateOptions: (ExportOptions) -> Unit,
    onDismiss: () -> Unit,
    onSelectTarget: (ExportTarget) -> Unit,
    onSaveToUri: (Uri) -> Unit
) {
    val context = LocalContext.current
    val waInstalled = remember {
        ShareHelper.isPackageInstalled(context, ExportTarget.WHATSAPP.packageName!!)
    }
    val wabInstalled = remember {
        ShareHelper.isPackageInstalled(context, ExportTarget.WHATSAPP_BUSINESS.packageName!!)
    }
    val tgInstalled = remember {
        ShareHelper.isPackageInstalled(context, ExportTarget.TELEGRAM.packageName!!)
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/x-vcard")
    ) { uri ->
        if (uri != null) {
            onSaveToUri(uri)
        }
    }

    val canExport = exportCount > 0

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.export_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (isSelectionMode) {
                    stringResource(R.string.export_selected_action, exportCount)
                } else {
                    stringResource(R.string.export_all_action, exportCount)
                },
                style = MaterialTheme.typography.titleSmall,
                color = if (canExport) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("export_matching_count_text")
            )

            Text(
                text = stringResource(R.string.export_sheet_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

            Text(
                text = stringResource(R.string.export_options_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            // تصفية حسب المنصة
            Text(
                text = stringResource(R.string.export_filter_platform),
                style = MaterialTheme.typography.labelMedium,
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
                        selected = exportOptions.platformFilter == plat,
                        onClick = { onUpdateOptions(exportOptions.copy(platformFilter = plat)) },
                        label = { Text(stringResource(plat.labelRes)) },
                        modifier = Modifier.testTag("export_opt_platform_${plat.storageKey.lowercase()}")
                    )
                }
            }

            // تصفية حسب التصنيف + المفضلة فقط
            Text(
                text = stringResource(R.string.export_filter_category),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = exportOptions.categoryFilter == null,
                    onClick = { onUpdateOptions(exportOptions.copy(categoryFilter = null)) },
                    label = { Text(stringResource(R.string.filter_all)) },
                    modifier = Modifier.testTag("export_opt_cat_all")
                )
                ContactCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = exportOptions.categoryFilter == cat,
                        onClick = { onUpdateOptions(exportOptions.copy(categoryFilter = cat)) },
                        label = { Text(stringResource(cat.labelRes)) },
                        modifier = Modifier.testTag("export_opt_cat_${cat.storageKey.lowercase()}")
                    )
                }
            }

            FilterChip(
                selected = exportOptions.onlyFavorites,
                onClick = { onUpdateOptions(exportOptions.copy(onlyFavorites = !exportOptions.onlyFavorites)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text(stringResource(R.string.export_only_favorites)) },
                modifier = Modifier.testTag("export_opt_favorites")
            )

            // بادئة الاسم اختيارية + اختصارات سريعة
            OutlinedTextField(
                value = exportOptions.namePrefix,
                onValueChange = { onUpdateOptions(exportOptions.copy(namePrefix = it)) },
                label = { Text(stringResource(R.string.export_name_prefix)) },
                placeholder = { Text(stringResource(R.string.export_name_prefix_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_prefix_input")
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
                            val next = if (exportOptions.namePrefix == quickPrefix) "" else quickPrefix
                            onUpdateOptions(exportOptions.copy(namePrefix = next))
                        },
                        label = { Text(quickPrefix) }
                    )
                }
            }

            // مفاتيح تضمين الحقول
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.export_include_email),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = exportOptions.includeEmail,
                    onCheckedChange = { onUpdateOptions(exportOptions.copy(includeEmail = it)) },
                    modifier = Modifier.testTag("export_include_email_switch")
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.export_include_company),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = exportOptions.includeCompanyAndNotes,
                    onCheckedChange = { onUpdateOptions(exportOptions.copy(includeCompanyAndNotes = it)) },
                    modifier = Modifier.testTag("export_include_company_switch")
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

            // حفظ مباشرة كملف على الجهاز
            OutlinedButton(
                onClick = {
                    val fileTag = when (exportOptions.platformFilter) {
                        ContactPlatform.WHATSAPP -> "whatsapp_contacts.vcf"
                        ContactPlatform.WHATSAPP_BUSINESS -> "wa_business_contacts.vcf"
                        ContactPlatform.TELEGRAM -> "telegram_contacts.vcf"
                        ContactPlatform.ALL -> "contacts_export.vcf"
                    }
                    saveFileLauncher.launch(fileTag)
                },
                enabled = canExport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("export_save_to_device_button")
            ) {
                Icon(Icons.Filled.SaveAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.export_save_to_device))
            }

            ExportTargetButton(
                title = stringResource(R.string.share_general),
                statusBadge = null,
                icon = Icons.Filled.Share,
                accentColor = MaterialTheme.colorScheme.primary,
                enabled = canExport,
                testTag = "export_general_button",
                onClick = { onSelectTarget(ExportTarget.GENERAL) }
            )

            ExportTargetButton(
                title = stringResource(R.string.share_whatsapp),
                statusBadge = stringResource(
                    if (waInstalled) R.string.app_installed else R.string.app_not_installed
                ),
                icon = Icons.Filled.Chat,
                accentColor = WhatsAppGreen,
                enabled = canExport,
                testTag = "export_whatsapp_button",
                onClick = { onSelectTarget(ExportTarget.WHATSAPP) }
            )

            ExportTargetButton(
                title = stringResource(R.string.share_whatsapp_business),
                statusBadge = stringResource(
                    if (wabInstalled) R.string.app_installed else R.string.app_not_installed
                ),
                icon = Icons.Filled.BusinessCenter,
                accentColor = WhatsAppBusinessTeal,
                enabled = canExport,
                testTag = "export_whatsapp_business_button",
                onClick = { onSelectTarget(ExportTarget.WHATSAPP_BUSINESS) }
            )

            ExportTargetButton(
                title = stringResource(R.string.share_telegram),
                statusBadge = stringResource(
                    if (tgInstalled) R.string.app_installed else R.string.app_not_installed
                ),
                icon = Icons.AutoMirrored.Filled.Send,
                accentColor = TelegramBlue,
                enabled = canExport,
                testTag = "export_telegram_button",
                onClick = { onSelectTarget(ExportTarget.TELEGRAM) }
            )
        }
    }
}

@Composable
private fun ExportTargetButton(
    title: String,
    statusBadge: String?,
    icon: ImageVector,
    accentColor: Color,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = if (enabled) {
            accentColor.copy(alpha = 0.10f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                if (statusBadge != null) {
                    Text(
                        text = statusBadge,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
