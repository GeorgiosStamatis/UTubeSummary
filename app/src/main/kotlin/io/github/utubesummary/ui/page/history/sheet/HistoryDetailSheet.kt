package io.github.utubesummary.ui.page.history.sheet

import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryOutput
import androidx.compose.ui.res.stringResource
import io.github.utubesummary.R
import java.util.Date
import java.text.DateFormat
import kotlinx.coroutines.delay
import io.github.utubesummary.ui.component.summaryLanguageLabel
import io.github.utubesummary.ui.component.historyProfileName
import io.github.utubesummary.model.HistoryLengthResult
import io.github.utubesummary.model.isSameVariant
import io.github.utubesummary.vm.UpdateUiState
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.ExperimentalFoundationApi
import io.github.utubesummary.ui.component.SummaryCard

/**
 * Bottom sheet to view complete summary details, playback TTS, and share
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailSheet(
    summary: HistorySummary?,
    onDismissRequest: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier,
    onPinnedChange: (Boolean) -> Unit = {},
    onRemoveResult: ((HistoryLengthResult) -> Unit)? = null,
    onSummarizeAgain: (() -> Unit)? = null,
    onUpdate: (() -> Unit)? = null,
    updateProgress: UpdateUiState.Running? = null,
    updateBusy: Boolean = false,
    onCancelUpdate: () -> Unit = {},
    sheetState: SheetState = rememberBottomSheetState(
        initialValue = SheetValue.Expanded,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    ),
) {
    if (summary == null) return

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            HistoryDetailContent(
                summary = summary,
                onShowSnackbar = onShowSnackbar,
                onPinnedChange = onPinnedChange,
                onRemoveResult = onRemoveResult,
                onSummarizeAgain = onSummarizeAgain,
                onUpdate = onUpdate,
                updateProgress = updateProgress,
                updateBusy = updateBusy,
                onCancelUpdate = onCancelUpdate
            )
        }
    }
}

/**
 * The sections of one History entry with pin, remove and "Summarize again". Shown in the bottom
 * sheet on phones and in the right pane on wide screens.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryDetailContent(
    summary: HistorySummary,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier,
    onPinnedChange: (Boolean) -> Unit = {},
    onRemoveResult: ((HistoryLengthResult) -> Unit)? = null,
    onSummarizeAgain: (() -> Unit)? = null,
    onUpdate: (() -> Unit)? = null,
    updateProgress: UpdateUiState.Running? = null,
    updateBusy: Boolean = false,
    onCancelUpdate: () -> Unit = {},
) {
    // Every result of the item, oldest first: a new summary is appended after the last one.
    val results = remember(summary) { summary.allResultsOldestFirst() }

    // The view shows a snapshot of the item, so the pin state is tracked here while it is open.
    var pinned by remember(summary.id) { mutableStateOf(summary.pinned) }

    // Only one section is read aloud at a time.
    var playingIndex by remember(summary) { mutableStateOf<Int?>(null) }
    val scrollState = rememberScrollState()
    val newestSection = remember(summary) { BringIntoViewRequester() }

    LaunchedEffect(summary) {
        scrollState.scrollTo(0)
        if (results.size > 1) {
            delay(SCROLL_TO_NEWEST_DELAY_MS)
            newestSection.bringIntoView()
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            TextButton(
                onClick = {
                    pinned = !pinned
                    onPinnedChange(pinned)
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(if (pinned) R.string.history_unpin else R.string.history_pin))
            }

            results.forEachIndexed { index, result ->
                val isNewest = index == results.lastIndex
                val header = historyResultHeader(result)
                key(index) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .then(if (isNewest) Modifier.bringIntoViewRequester(newestSection) else Modifier)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = header,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            if (onRemoveResult != null) {
                                IconButton(onClick = { onRemoveResult(result) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = stringResource(R.string.history_remove_summary),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        SummaryCard(
                            modifier = Modifier.fillMaxWidth(),
                            summary = SummaryOutput(
                                title = summary.title,
                                summary = result.summary,
                                author = summary.author,
                                sourceLink = summary.sourceLink,
                                isYoutubeLink = summary.isYoutubeLink,
                                isBiliBiliLink = summary.isBiliBiliLink,
                                length = result.length,
                                provider = result.provider ?: summary.provider,
                                model = result.model ?: summary.model,
                                overview = result.overview,
                                keyPoints = result.keyPoints,
                                tags = result.tags,
                            ),
                            cardColors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            // Older sections start collapsed so a long history stays readable.
                            isExpandedByDefault = isNewest,
                            isPlaying = playingIndex == index,
                            onPlayRequest = {
                                playingIndex = if (playingIndex == index) null else index
                            },
                            onShowSnackbar = onShowSnackbar
                        )
                    }
                }
            }

            if (onSummarizeAgain != null) {
                FilledTonalButton(
                    onClick = onSummarizeAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(stringResource(R.string.summarize_again))
                }
            }

            if (onUpdate != null) {
                // An update makes each distinct profile / language / length summary again.
                val variantCount = remember(summary) {
                    results.fold(emptyList<HistoryLengthResult>()) { distinct, result ->
                        if (distinct.any { isSameVariant(it, result) }) distinct else distinct + result
                    }.size
                }
                HistoryUpdateControls(
                    summaryCount = variantCount,
                    progress = updateProgress,
                    busy = updateBusy,
                    onUpdate = onUpdate,
                    onCancel = onCancelUpdate,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

private const val SCROLL_TO_NEWEST_DELAY_MS = 200L

/** "Profile · Language · Length · date" for one stored result. */
@Composable
private fun historyResultHeader(result: HistoryLengthResult): String {
    val profile = historyProfileName(result.profileId, result.profileName)
    val language = result.language?.let { summaryLanguageLabel(it) }
    val length = result.length.takeIf { it != SummaryLength.NONE }
        ?.name?.lowercase()?.replaceFirstChar { it.titlecase() }
    val date = result.createdOn.takeIf { it > 0L }
        ?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) }
    return listOfNotNull(profile, language, length, date).joinToString(" · ")
}
