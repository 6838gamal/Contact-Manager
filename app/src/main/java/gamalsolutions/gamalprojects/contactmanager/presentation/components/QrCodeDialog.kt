package gamalsolutions.gamalprojects.contactmanager.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gamalsolutions.gamalprojects.R
import gamalsolutions.gamalprojects.contactmanager.data.model.Contact
import gamalsolutions.gamalprojects.contactmanager.utils.QrCodeHelper

/**
 * نافذة عرض رمز QR لجهة اتصال واحدة (vCard 3.0) باستخدام ZXing.
 */
@Composable
fun QrCodeDialog(
    contact: Contact,
    onDismiss: () -> Unit,
    onShareVCard: () -> Unit
) {
    val qrBitmap = remember(contact) {
        QrCodeHelper.generateContactQrBitmap(contact).getOrNull()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.qr_code_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = contact.name,
                            modifier = Modifier.size(196.dp)
                        )
                    }
                }
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = contact.phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.qr_code_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onShareVCard,
                modifier = Modifier.testTag("qr_share_vcard_button")
            ) {
                Text(text = stringResource(R.string.share_vcard_single))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("qr_close_button")
            ) {
                Text(text = stringResource(R.string.close))
            }
        }
    )
}
