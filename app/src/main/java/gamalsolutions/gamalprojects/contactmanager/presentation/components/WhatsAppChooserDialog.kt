package gamalsolutions.gamalprojects.contactmanager.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import gamalsolutions.gamalprojects.R
import gamalsolutions.gamalprojects.contactmanager.data.model.Contact
import gamalsolutions.gamalprojects.contactmanager.data.model.WhatsAppType
import gamalsolutions.gamalprojects.ui.theme.WhatsAppBusinessTeal
import gamalsolutions.gamalprojects.ui.theme.WhatsAppGreen

/**
 * نافذة اختيار تطبيق واتساب (العادي أو للأعمال) لفتح محادثة مباشرة عبر wa.me.
 */
@Composable
fun WhatsAppChooserDialog(
    contact: Contact,
    onDismiss: () -> Unit,
    onSelect: (WhatsAppType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.open_whatsapp_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${contact.name} (${contact.phone})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    onClick = { onSelect(WhatsAppType.STANDARD) },
                    shape = MaterialTheme.shapes.medium,
                    color = WhatsAppGreen.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("open_whatsapp_standard_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Chat,
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.open_whatsapp_normal),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }

                Surface(
                    onClick = { onSelect(WhatsAppType.BUSINESS) },
                    shape = MaterialTheme.shapes.medium,
                    color = WhatsAppBusinessTeal.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("open_whatsapp_business_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BusinessCenter,
                            contentDescription = null,
                            tint = WhatsAppBusinessTeal,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.open_whatsapp_business),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("whatsapp_chooser_cancel")
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}
