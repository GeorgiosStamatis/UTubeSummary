package io.github.utubesummary.ui.page.history.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import io.github.utubesummary.model.HistorySort
import io.github.utubesummary.model.SummaryType
import io.github.utubesummary.vm.HistoryViewModel

/**
 * State holder for History search and filter state
 */
class HistorySearchState(
    val query: String,
    val selectedFilter: SummaryType?,
    val onQueryChange: (String) -> Unit,
    val onClear: () -> Unit,
    val onFilterChanged: (SummaryType) -> Unit,
    val selectedProfileId: String? = null,
    val pinnedOnly: Boolean = false,
    val onProfileFilterChanged: (String?) -> Unit = {},
    val onPinnedOnlyChanged: (Boolean) -> Unit = {},
    val sort: HistorySort = HistorySort.NEWEST,
    val onSortChanged: (HistorySort) -> Unit = {},
) {
    val isSearching: Boolean
        get() = query.isNotBlank() || selectedFilter != null || selectedProfileId != null || pinnedOnly
}

@Composable
fun rememberHistorySearchState(viewModel: HistoryViewModel): HistorySearchState {
    val selectedFilter by viewModel.filterType.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val selectedProfileId by viewModel.profileFilter.collectAsState()
    val pinnedOnly by viewModel.pinnedOnly.collectAsState()
    val sort by viewModel.sort.collectAsState()

    return remember(query, selectedFilter, selectedProfileId, pinnedOnly, sort, viewModel) {
        HistorySearchState(
            query = query,
            selectedFilter = selectedFilter,
            onQueryChange = viewModel::onSearchTextChanged,
            onClear = { viewModel.onSearchTextChanged("") },
            onFilterChanged = viewModel::onFilterChanged,
            selectedProfileId = selectedProfileId,
            pinnedOnly = pinnedOnly,
            onProfileFilterChanged = viewModel::onProfileFilterChanged,
            onPinnedOnlyChanged = viewModel::onPinnedOnlyChanged,
            sort = sort,
            onSortChanged = viewModel::onSortChanged,
        )
    }
}
