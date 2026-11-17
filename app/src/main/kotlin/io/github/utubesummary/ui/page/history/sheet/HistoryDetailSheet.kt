package io.github.utubesummary.ui.page.history.sheet

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
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.ExperimentalFoundationApi
import io.github.utubesummary.ui.component.SummaryCard

/**
 * Bottom sheet to view complete summary details, playback TTS, and share
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryDetailSheet(
    summary: HistorySummary?,
    onDismissRequest: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSummarizeAgain: (() -> Unit)? = null,
    sheetState: SheetState = rememberBottomSheetState(
        initialValue = SheetValue.Expanded,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    ),
) {
    if (summary == null) return

    // Every result of the item, oldest first: a new summary is appended after the last one.
    val results = remember(summary) { summary.allResultsOldestFirst() }

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
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
                            Text(
                                text = header,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
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
