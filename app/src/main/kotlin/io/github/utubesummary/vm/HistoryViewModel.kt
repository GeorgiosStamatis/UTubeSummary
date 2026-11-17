package io.github.utubesummary.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import io.github.utubesummary.domain.usecase.DeleteHistorySummaryUseCase
import io.github.utubesummary.domain.usecase.GetHistorySummariesUseCase
import io.github.utubesummary.domain.usecase.RestoreHistorySummaryUseCase
import io.github.utubesummary.domain.usecase.SetHistoryPinnedUseCase
import io.github.utubesummary.model.HistoryFilter
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.SummaryType
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val SEARCH_DEBOUNCE_MILLIS = 300L

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistorySummariesUseCase: GetHistorySummariesUseCase,
    private val deleteHistorySummaryUseCase: DeleteHistorySummaryUseCase,
    private val restoreHistorySummaryUseCase: RestoreHistorySummaryUseCase,
    private val setHistoryPinnedUseCase: SetHistoryPinnedUseCase,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow<SummaryType?>(null)
    val filterType: StateFlow<SummaryType?> = _filterType.asStateFlow()

    private val _profileFilter = MutableStateFlow<String?>(null)
    val profileFilter: StateFlow<String?> = _profileFilter.asStateFlow()

    private val _pinnedOnly = MutableStateFlow(false)
    val pinnedOnly: StateFlow<Boolean> = _pinnedOnly.asStateFlow()

    @OptIn(FlowPreview::class)
    private val debouncedSearchText = _searchQuery
        .debounce(SEARCH_DEBOUNCE_MILLIS.milliseconds)

    @OptIn(ExperimentalCoroutinesApi::class)
    val historySummaries: Flow<PagingData<HistorySummary>> =
        combine(debouncedSearchText, _filterType, _profileFilter, _pinnedOnly) { text, type, profile, pinned ->
            text to HistoryFilter(type = type, profileId = profile, pinnedOnly = pinned)
        }.flatMapLatest { (text, filter) ->
            getHistorySummariesUseCase(text, filter)
        }.cachedIn(viewModelScope)

    fun onSearchTextChanged(text: String) {
        _searchQuery.value = text
    }

    fun onFilterChanged(type: SummaryType) {
        _filterType.value = if (_filterType.value == type) null else type
    }

    /** Selecting the active profile again clears the filter. */
    fun onProfileFilterChanged(profileId: String?) {
        _profileFilter.value = if (_profileFilter.value == profileId) null else profileId
    }

    fun onPinnedOnlyChanged(pinnedOnly: Boolean) {
        _pinnedOnly.value = pinnedOnly
    }

    fun setPinned(id: String, pinned: Boolean) {
        viewModelScope.launch {
            setHistoryPinnedUseCase(id, pinned)
        }
    }

    fun deleteSummary(id: String) {
        viewModelScope.launch {
            deleteHistorySummaryUseCase(id)
        }
    }

    fun restoreSummary(summary: HistorySummary) {
        viewModelScope.launch {
            restoreHistorySummaryUseCase(summary)
        }
    }
}