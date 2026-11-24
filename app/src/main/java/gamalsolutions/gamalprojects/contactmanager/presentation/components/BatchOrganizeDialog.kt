package gamalsolutions.gamalprojects.contactmanager.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import gamalsolutions.gamalprojects.R
import gamalsolutions.gamalprojects.contactmanager.data.model.ContactCategory
import gamalsolutions.gamalprojects.contactmanager.data.model.ContactPlatform

/**
 * نافذة تنظيم دفعة من جهات الاتصال المحددة (تعيين التصنيف أو المنصة المفضلة دفعة واحدة).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BatchOrganizeDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onAssignCategory: (ContactCategory) -> Unit,
    onAssignPlatform: (ContactPlatform) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.batch_organize),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.selected_count, selectedCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = stringResource(R.string.batch_assign_category),
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ContactCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = false,
                            onClick = { onAssignCategory(cat) },
                            label = { Text(stringResource(cat.labelRes)) },
                            modifier = Modifier.testTag("batch_cat_${cat.storageKey.lowercase()}")
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    text = stringResource(R.string.batch_assign_platform),
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ContactPlatform.entries.forEach { plat ->
                        FilterChip(
                            selected = false,
                            onClick = { onAssignPlatform(plat) },
                            label = { Text(stringResource(plat.labelRes)) },
                            modifier = Modifier.testTag("batch_plat_${plat.storageKey.lowercase()}")
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("batch_organize_close")
            ) {
                Text(text = stringResource(R.string.close))
            }
        }
    )
}
