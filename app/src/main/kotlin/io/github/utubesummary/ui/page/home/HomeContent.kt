package io.github.utubesummary.ui.page.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.utubesummary.R
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryOutput
import io.github.utubesummary.ui.component.LanguageSelector
import io.github.utubesummary.ui.component.LengthSelector
import io.github.utubesummary.ui.component.ProfileSelector
import io.github.utubesummary.ui.component.rememberIsWideLayout
import io.github.utubesummary.ui.page.home.input.InputSection
import io.github.utubesummary.ui.page.home.result.SummaryResultSection
import io.github.utubesummary.ui.theme.UTubeSummaryTheme
import io.github.utubesummary.vm.ChatState
import io.github.utubesummary.vm.SettingsUiState
import io.github.utubesummary.vm.SummarizationState
import io.github.utubesummary.ui.component.ContentMaxWidth
import io.github.utubesummary.ui.component.TwoPaneMaxWidth

@Composable
internal fun HomeContent(
    innerPadding: PaddingValues,
    listState: LazyListState,
    urlOrText: String,
    documentFilename: String?,
    focusRequester: FocusRequester,
    settings: SettingsUiState,
    summarizationState: SummarizationState,
    isPlaying: Boolean,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    chatState: ChatState = ChatState(),
) {
    val wide = rememberIsWideLayout()

    Box(
        modifier = modifier
            .fillMaxSize()
            .consumeWindowInsets(innerPadding),
        contentAlignment = Alignment.TopCenter
    ) {
        if (wide) {
            // Input and options on the left, the result on the right; each side scrolls on its own.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = TwoPaneMaxWidth)
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HomeInputControls(
                        urlOrText = urlOrText,
                        documentFilename = documentFilename,
                        focusRequester = focusRequester,
                        settings = settings,
                        summarizationState = summarizationState,
                        actions = actions,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HomeResult(
                        summarizationState = summarizationState,
                        isPlaying = isPlaying,
                        actions = actions,
                        chatState = chatState,
                    )
                    Spacer(modifier = Modifier.height(88.dp))
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = ContentMaxWidth),
                contentPadding = innerPadding
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HomeInputControls(
                            urlOrText = urlOrText,
                            documentFilename = documentFilename,
                            focusRequester = focusRequester,
                            settings = settings,
                            summarizationState = summarizationState,
                            actions = actions,
                        )
                        HomeResult(
                            summarizationState = summarizationState,
                            isPlaying = isPlaying,
                            actions = actions,
                            chatState = chatState,
                        )
                        Spacer(modifier = Modifier.height(88.dp))
                    }
                }
            }
        }
    }
}

/** Input field with the profile, language and length selectors. */
@Composable
private fun HomeInputControls(
    urlOrText: String,
    documentFilename: String?,
    focusRequester: FocusRequester,
    settings: SettingsUiState,
    summarizationState: SummarizationState,
    actions: HomeActions,
) {
    val lengthOptions = listOf(
        stringResource(id = R.string.short_length),
        stringResource(id = R.string.middle_length),
        stringResource(id = R.string.long_length)
    )

    InputSection(
        urlOrText = urlOrText,
        onUrlChange = actions.onUrlChange,
        onSummarize = actions.onSummarize,
        documentFilename = documentFilename,
        error = summarizationState.error,
        apiKey = settings.apiKey,
        onClear = actions.onClearInput,
        focusRequester = focusRequester,
        isLoading = summarizationState.isLoading,
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (settings.profiles.size > 1) {
            ProfileSelector(
                profiles = settings.profiles,
                selectedProfileId = settings.selectedProfileId,
                onProfileSelect = actions.onProfileSelect,
                enabled = !summarizationState.isLoading,
            )
        }
        LanguageSelector(
            selectedLanguage = settings.summaryLanguage,
            onLanguageSelect = actions.onLanguageSelect,
            enabled = !summarizationState.isLoading,
        )
    }

    if (settings.showLength) {
        LengthSelector(
            selectedIndex = SummaryLength.userSelectable.indexOf(settings.summaryLength)
                .coerceAtLeast(0),
            onSelectedIndexChange = { actions.onLengthSelect(SummaryLength.userSelectable[it]) },
            options = lengthOptions,
            enabled = !summarizationState.isLoading,
        )
    }
}

/** The summary, its progress or error, and the question box. */
@Composable
private fun HomeResult(
    summarizationState: SummarizationState,
    isPlaying: Boolean,
    actions: HomeActions,
    chatState: ChatState,
) {
    SummaryResultSection(
        summarizationState = summarizationState,
        isPlaying = isPlaying,
        onCopySummary = actions.onCopySummary,
        onShowSnackBar = actions.onShowSnackBar,
        onPlaySummary = actions.onPlaySummary,
        chatState = chatState,
        onAskQuestion = actions.onAskQuestion,
        onCopyAnswer = actions.onCopySummary,
        onCancel = actions.onCancelSummarize,
    )
}

@Preview
@Composable
private fun HomeContentPreview() {
    UTubeSummaryTheme {
        HomeContent(
            innerPadding = PaddingValues(0.dp),
            listState = rememberLazyListState(),
            urlOrText = "https://example.com/article",
            documentFilename = null,
            focusRequester = remember { FocusRequester() },
            settings = SettingsUiState(
                activeProvider = AIProvider.OPENAI,
                activeModel = "gpt-4o",
                showLength = true,
                summaryLength = SummaryLength.MEDIUM
            ),
            summarizationState = SummarizationState(
                summaryResult = SummaryOutput(
                    title = "Sample Summary",
                    summary = "This is a sample summary content for testing preview.",
                    author = "Author Name",
                    sourceLink = "https://example.com",
                    isYoutubeLink = false,
                    isBiliBiliLink = false,
                    length = SummaryLength.MEDIUM,
                    provider = AIProvider.OPENAI.name,
                    model = "gpt-4o"
                )
            ),
            isPlaying = false,
            actions = HomeActions(
                onSummarize = {},
                onClearInput = {},
                onUrlChange = {},
                onLengthSelect = {},
                onPasteFromClipboard = {},
                onPlaySummary = {},
                onCopySummary = {},
                onShowSnackBar = {},
                onLaunchFilePicker = {},
                onLaunchImagePicker = {},
                onLaunchCamera = {},
                onSelectProvider = {},
                onSelectModel = {},
                onShowProviderModelSheet = {},
                onDismissProviderModelSheet = {}
            )
        )
    }
}
