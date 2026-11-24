package gamalsolutions.gamalprojects.contactmanager.presentation.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import gamalsolutions.gamalprojects.R
import gamalsolutions.gamalprojects.contactmanager.data.model.ExportTarget
import gamalsolutions.gamalprojects.contactmanager.utils.ShareHelper
import gamalsolutions.gamalprojects.ui.theme.TelegramBlue
import gamalsolutions.gamalprojects.ui.theme.WhatsAppBusinessTeal
import gamalsolutions.gamalprojects.ui.theme.WhatsAppGreen

/**
 * قائمة سفلية (ModalBottomSheet) لتصدير ومشاركة جهات الاتصال بصيغة vCard 3.0.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    exportCount: Int,
    isSelectionMode: Boolean,
    onDismiss: () -> Unit,
    onSelectTarget: (ExportTarget) -> Unit
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

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(R.string.export_sheet_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            ExportTargetButton(
                title = stringResource(R.string.share_general),
                statusBadge = null,
                icon = Icons.Filled.Share,
                accentColor = MaterialTheme.colorScheme.primary,
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
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = accentColor.copy(alpha = 0.10f),
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
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
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
