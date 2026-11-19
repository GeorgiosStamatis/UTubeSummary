package io.github.utubesummary.ui.page.history.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.utubesummary.R
import io.github.utubesummary.vm.UpdateUiState

/**
 * The "Update" button of a History entry: asks for confirmation (every summary is a model call),
 * then shows the progress of the update with a way to cancel it.
 *
 * @param summaryCount how many summaries an update would make again
 * @param progress the running update of **this** entry, or null
 * @param busy true while any entry is being updated
 */
@Composable
fun HistoryUpdateControls(
    summaryCount: Int,
    progress: UpdateUiState.Running?,
    busy: Boolean,
    onUpdate: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showConfirm by remember { mutableStateOf(false) }

    if (progress != null) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(
                text = if (progress.total == 0) {
                    stringResource(R.string.history_updating_fetching)
                } else {
                    stringResource(R.string.history_updating, progress.done, progress.total)
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        }
    } else {
        OutlinedButton(
            onClick = { showConfirm = true },
            enabled = !busy,
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            Text(stringResource(R.string.history_update))
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(R.string.history_update_title)) },
            text = { Text(stringResource(R.string.history_update_confirm, summaryCount)) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    onUpdate()
                }) {
                    Text(stringResource(R.string.history_update))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
