package io.github.utubesummary.ui.page.history

import io.github.utubesummary.model.HistorySort
import androidx.activity.compose.BackHandler
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.launch
import io.github.utubesummary.R
import io.github.utubesummary.model.HistoryLengthResult
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.SummaryProfile
import io.github.utubesummary.ui.component.rememberIsWideLayout
import io.github.utubesummary.ui.page.history.search.HistoryEmptySearchResults
import io.github.utubesummary.ui.page.history.search.HistoryFilterChips
import io.github.utubesummary.ui.page.history.search.HistorySearchBar
import io.github.utubesummary.ui.page.history.search.rememberHistorySearchState
import io.github.utubesummary.ui.page.history.section.HistoryItemRow
import io.github.utubesummary.ui.page.history.section.HistorySectionHeader
import io.github.utubesummary.ui.page.history.section.groupPositionOf
import io.github.utubesummary.ui.page.history.section.historySectionKey
import io.github.utubesummary.ui.page.history.sheet.HistoryDetailContent
import io.github.utubesummary.ui.page.history.sheet.HistoryDetailSheet
import io.github.utubesummary.vm.HistoryViewModel
import io.github.utubesummary.ui.component.ContentMaxWidth

/**
 * Material 3 Expressive History Screen with Android 16 search bar, connected shapes, and tonal badges
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun HistoryScreen(
    onBack: () -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel<HistoryViewModel>(),
    showThumbnails: Boolean = true,
    onSummarizeAgain: (String) -> Unit = {},
    profiles: List<SummaryProfile> = emptyList(),
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val searchState = rememberHistorySearchState(viewModel = viewModel)
    val historySummaries = viewModel.historySummaries.collectAsLazyPagingItems()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    // Only the id is saved, so the open entry survives rotation without a large saved state.
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSummary by remember { mutableStateOf<HistorySummary?>(null) }
    fun select(summary: HistorySummary?) {
        selectedSummary = summary
        selectedId = summary?.id
    }
    LaunchedEffect(selectedId) {
        val id = selectedId
        if (id != null && selectedSummary?.id != id) {
            // Entry deleted in the meantime: nothing stays selected.
            viewModel.loadSummary(id) { select(it) }
        }
    }

    val deletedMessage = stringResource(id = R.string.deleted)
    val undoMessage = stringResource(id = R.string.undo)
    val pinnedTitle = stringResource(id = R.string.history_pinned)
    val selectedIds by viewModel.selectedIds.collectAsState()
    val selecting = selectedIds.isNotEmpty()
    BackHandler(enabled = selecting) { viewModel.clearSelection() }
    val selectedCountText = stringResource(id = R.string.history_selected_count, selectedIds.size)
    val removedSummaryMessage = stringResource(id = R.string.history_summary_removed)

    LaunchedEffect(searchState.query, searchState.selectedFilter, searchState.selectedProfileId, searchState.pinnedOnly, searchState.sort) {
        lazyListState.scrollToItem(0)
    }

    val wide = rememberIsWideLayout()
    val showSnackbar: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val onRemoveResult: (HistoryLengthResult) -> Unit = { result ->
        selectedSummary?.let { original ->
            viewModel.removeResult(original, result) { updated ->
                // Show what is left; the detail closes when the entry is gone.
                select(updated)
            }
            scope.launch {
                val outcome = snackbarHostState.showSnackbar(
                    message = removedSummaryMessage,
                    actionLabel = undoMessage,
                    duration = SnackbarDuration.Long
                )
                if (outcome == SnackbarResult.ActionPerformed) {
                    viewModel.undoRemoveResult(original)
                    select(original)
                }
            }
        }
    }
    val onPinnedChange: (Boolean) -> Unit = { pinned ->
        selectedSummary?.let { viewModel.setPinned(it.id, pinned) }
    }
    // Open the source on Home, where profile, language and length can be changed.
    val onSummarizeAgainAction: (() -> Unit)? = (selectedSummary?.sourceLink ?: selectedSummary?.sourceText)
        ?.takeIf { it.isNotBlank() }
        ?.let { source ->
            {
                select(null)
                onSummarizeAgain(source)
            }
        }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(text = if (selecting) selectedCountText else stringResource(id = R.string.history))
                },
                navigationIcon = {
                    IconButton(onClick = if (selecting) viewModel::clearSelection else onBack) {
                        Icon(
                            imageVector = if (selecting) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(if (selecting) R.string.close else R.string.go_back)
                        )
                    }
                },
                actions = {
                    if (selecting) {
                        IconButton(onClick = { viewModel.setSelectedPinned(true) }) {
                            Icon(
                                imageVector = Icons.Outlined.PushPin,
                                contentDescription = stringResource(R.string.history_pin)
                            )
                        }
                        IconButton(onClick = { viewModel.setSelectedPinned(false) }) {
                            Icon(
                                imageVector = Icons.Outlined.PushPin,
                                contentDescription = stringResource(R.string.history_unpin),
                                modifier = Modifier.alpha(0.5f)
                            )
                        }
                        IconButton(onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.deleteSelected { deleted ->
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = deletedMessage,
                                        actionLabel = undoMessage,
                                        duration = SnackbarDuration.Long
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreSummaries(deleted)
                                    }
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.delete)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        // Phones show the entry in a bottom sheet; wide screens show it in the right pane.
        if (!wide) {
            HistoryDetailSheet(
                summary = selectedSummary,
                onDismissRequest = { select(null) },
                onShowSnackbar = showSnackbar,
                onRemoveResult = onRemoveResult,
                onPinnedChange = onPinnedChange,
                onSummarizeAgain = onSummarizeAgainAction
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .weight(if (wide) 0.4f else 1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (wide) Modifier else Modifier.widthIn(max = ContentMaxWidth))
                        .padding(horizontal = 16.dp),
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Search Bar
                    item(key = "search_bar") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            HistorySearchBar(
                                query = searchState.query,
                                onQueryChange = searchState.onQueryChange,
                                onClear = searchState.onClear
                            )

                            HistoryFilterChips(
                                selectedFilter = searchState.selectedFilter,
                                onFilterChanged = searchState.onFilterChanged,
                                pinnedOnly = searchState.pinnedOnly,
                                onPinnedOnlyChanged = searchState.onPinnedOnlyChanged,
                                profiles = profiles,
                                selectedProfileId = searchState.selectedProfileId,
                                onProfileFilterChanged = searchState.onProfileFilterChanged,
                                sort = searchState.sort,
                                onSortChanged = searchState.onSortChanged,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            )
                        }
                    }

                    // Initial loading state
                    if (historySummaries.loadState.refresh is LoadState.Loading) {
                        item(key = "loading_initial") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(56.dp))
                            }
                        }
                    }

                    // Empty state
                    if (historySummaries.loadState.refresh is LoadState.NotLoading && historySummaries.itemCount == 0) {
                        item(key = "empty_state") {
                            HistoryEmptySearchResults(
                                query = searchState.query,
                                isSearching = searchState.isSearching
                            )
                        }
                    }

                    // Grouped History Items with connected shapes
                    items(
                        count = historySummaries.itemCount,
                        key = historySummaries.itemKey { it.id }
                    ) { index ->
                        val summary = historySummaries[index]
                        if (summary != null) {
                            // Pinned items form their own section at the top.
                            // Sorted by title there are no date headers.
                            val byTitle = searchState.sort == HistorySort.TITLE
                            val sectionOf = { item: HistorySummary ->
                                historySectionKey(item.pinned, item.createdOn, byTitle, pinnedTitle)
                            }
                            val currentSection = sectionOf(summary)
                            val prevSection = if (index > 0) historySummaries.peek(index - 1)?.let(sectionOf) else null
                            val nextSection = if (index < historySummaries.itemCount - 1) {
                                historySummaries.peek(index + 1)?.let(sectionOf)
                            } else null

                            val isFirstInSection = prevSection != currentSection
                            val position = groupPositionOf(prevSection, currentSection, nextSection)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(
                                        fadeInSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        placementSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        fadeOutSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                            ) {
                                if (isFirstInSection && currentSection.isNotBlank()) {
                                    HistorySectionHeader(
                                        title = currentSection,
                                        modifier = Modifier.padding(
                                            top = if (index > 0) 14.dp else 4.dp,
                                            bottom = 6.dp
                                        )
                                    )
                                }

                                HistoryItemRow(
                                    summary = summary,
                                    position = position,
                                    showThumbnails = showThumbnails,
                                    selected = summary.id in selectedIds,
                                    onClick = {
                                        if (selecting) viewModel.toggleSelection(summary.id) else select(summary)
                                    },
                                    onLongClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.toggleSelection(summary.id)
                                    },
                                    onDismiss = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.deleteSummary(summary.id)

                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = deletedMessage,
                                                actionLabel = undoMessage,
                                                duration = SnackbarDuration.Long
                                            )
                                            when (result) {
                                                SnackbarResult.ActionPerformed -> {
                                                    viewModel.restoreSummary(summary)
                                                    snackbarHostState.currentSnackbarData?.dismiss()
                                                }

                                                SnackbarResult.Dismissed -> {}
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Pagination append loading
                    if (historySummaries.loadState.append is LoadState.Loading) {
                        item(key = "loading_append") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                LinearWavyProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (wide) {
                VerticalDivider(modifier = Modifier.padding(top = innerPadding.calculateTopPadding()))
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val selected = selectedSummary
                    if (selected == null) {
                        Text(
                            text = stringResource(R.string.history_select_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp)
                        )
                    } else {
                        HistoryDetailContent(
                            summary = selected,
                            onShowSnackbar = showSnackbar,
                            onPinnedChange = onPinnedChange,
                            onRemoveResult = onRemoveResult,
                            onSummarizeAgain = onSummarizeAgainAction
                        )
                    }
                }
            }
        }
    }
}
