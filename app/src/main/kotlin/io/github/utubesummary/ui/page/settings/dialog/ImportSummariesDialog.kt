package io.github.utubesummary.ui.page.settings.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.utubesummary.R
import io.github.utubesummary.vm.ImportFailure
import io.github.utubesummary.vm.ImportState

/** Shows the progress and the outcome of importing video summaries. */
@Composable
fun ImportSummariesDialog(
    state: ImportState,
    onDismiss: () -> Unit,
) {
    if (state is ImportState.Idle) return
    val importing = state is ImportState.Importing

    AlertDialog(
        // Can't be dismissed while the import is running.
        onDismissRequest = { if (!importing) onDismiss() },
        title = { Text(stringResource(R.string.importSummaries)) },
        text = {
            when (state) {
                ImportState.Idle -> Unit
                ImportState.Importing -> Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.importingSummaries))
                }

                is ImportState.Done -> Text(
                    stringResource(
                        R.string.importDone,
                        state.result.imported,
                        state.result.skippedExisting,
                        state.result.invalid
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )

                is ImportState.Failed -> Text(
                    stringResource(
                        when (state.reason) {
                            ImportFailure.INVALID_FILE -> R.string.importInvalidFile
                            ImportFailure.TOO_LARGE -> R.string.importTooLarge
                            ImportFailure.UNREADABLE -> R.string.importUnreadable
                        }
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            if (!importing) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
            }
        },
    )
}
