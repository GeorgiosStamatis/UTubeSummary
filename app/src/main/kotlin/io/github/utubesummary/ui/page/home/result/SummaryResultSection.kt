package io.github.utubesummary.ui.page.home.result

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.utubesummary.R
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryOutput
import io.github.utubesummary.ui.component.SummaryCard
import io.github.utubesummary.ui.theme.UTubeSummaryTheme
import io.github.utubesummary.vm.ChatState
import io.github.utubesummary.vm.SummarizationState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SummaryResultSection(
    summarizationState: SummarizationState,
    isPlaying: Boolean,
    onCopySummary: (String) -> Unit,
    onShowSnackBar: (String) -> Unit,
    onPlaySummary: () -> Unit,
    modifier: Modifier = Modifier,
    chatState: ChatState = ChatState(),
    onAskQuestion: (String) -> Unit = {},
    onCopyAnswer: (String) -> Unit = {},
    onCancel: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (summarizationState.isLoading) {
            LinearWavyProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            if (onCancel != null) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        summarizationState.summaryResult?.takeIf { it.summary.isNotEmpty() }?.let { summaryOutput ->
            key(summaryOutput.length) {
                SummaryCard(
                    modifier = Modifier.padding(vertical = 8.dp),
                    isExpandedByDefault = true,
                    summary = summaryOutput,
                    onLongClick = { onCopySummary(summaryOutput.summary) },
                    onShowSnackbar = onShowSnackBar,
                    isPlaying = isPlaying,
                    onPlayRequest = onPlaySummary
                )
            }

            if (chatState.canAsk && !summarizationState.isLoading) {
                QuestionSection(
                    chatState = chatState,
                    onAsk = onAskQuestion,
                    onCopyAnswer = onCopyAnswer,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun SummaryResultSectionPreview() {
    UTubeSummaryTheme {
        SummaryResultSection(
            summarizationState = SummarizationState(
                summaryResult = SummaryOutput(
                    title = "Sample Summary",
                    summary = "This is a preview summary demonstrating the result container.",
                    author = "Author",
                    sourceLink = "https://example.com",
                    isYoutubeLink = false,
                    isBiliBiliLink = false,
                    length = SummaryLength.MEDIUM,
                    provider = AIProvider.OPENAI.name,
                    model = "gpt-4o"
                )
            ),
            isPlaying = false,
            onCopySummary = {},
            onShowSnackBar = {},
            onPlaySummary = {}
        )
    }
}
