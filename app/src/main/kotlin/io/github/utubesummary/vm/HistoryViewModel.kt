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
import io.github.utubesummary.domain.usecase.DeleteHistorySummariesUseCase
import io.github.utubesummary.domain.usecase.DeleteHistorySummaryUseCase
import io.github.utubesummary.domain.usecase.GetHistorySummariesUseCase
import io.github.utubesummary.domain.usecase.GetHistorySummaryUseCase
import io.github.utubesummary.domain.usecase.RemoveHistoryResultUseCase
import io.github.utubesummary.domain.usecase.RestoreHistorySummaryUseCase
import io.github.utubesummary.domain.usecase.SetHistoryPinnedUseCase
import io.github.utubesummary.model.HistoryFilter
import io.github.utubesummary.model.HistoryLengthResult
import io.github.utubesummary.model.HistorySort
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.SummaryType
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import io.github.utubesummary.domain.usecase.UpdateHistoryEntryUseCase
import io.github.utubesummary.exception.toSummaryException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

private const val SEARCH_DEBOUNCE_MILLIS = 300L

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistorySummariesUseCase: GetHistorySummariesUseCase,
    private val deleteHistorySummaryUseCase: DeleteHistorySummaryUseCase,
    private val restoreHistorySummaryUseCase: RestoreHistorySummaryUseCase,
    private val setHistoryPinnedUseCase: SetHistoryPinnedUseCase,
    private val removeHistoryResultUseCase: RemoveHistoryResultUseCase,
    private val deleteHistorySummariesUseCase: DeleteHistorySummariesUseCase,
    private val getHistorySummaryUseCase: GetHistorySummaryUseCase,
    private val updateHistoryEntryUseCase: UpdateHistoryEntryUseCase,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow<SummaryType?>(null)
    val filterType: StateFlow<SummaryType?> = _filterType.asStateFlow()

    private val _profileFilter = MutableStateFlow<String?>(null)
    val profileFilter: StateFlow<String?> = _profileFilter.asStateFlow()

    private val _pinnedOnly = MutableStateFlow(false)
    val pinnedOnly: StateFlow<Boolean> = _pinnedOnly.asStateFlow()

    private val _sort = MutableStateFlow(HistorySort.NEWEST)
    val sort: StateFlow<HistorySort> = _sort.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())

    /** Entries selected for a bulk action; empty when not selecting. */
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    @OptIn(FlowPreview::class)
    private val debouncedSearchText = _searchQuery
        .debounce(SEARCH_DEBOUNCE_MILLIS.milliseconds)

    @OptIn(ExperimentalCoroutinesApi::class)
    val historySummaries: Flow<PagingData<HistorySummary>> =
        combine(debouncedSearchText, _filterType, _profileFilter, _pinnedOnly, _sort) { text, type, profile, pinned, sort ->
            text to HistoryFilter(type = type, profileId = profile, pinnedOnly = pinned, sort = sort)
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

    fun onSortChanged(sort: HistorySort) {
        _sort.value = sort
    }

    fun onPinnedOnlyChanged(pinnedOnly: Boolean) {
        _pinnedOnly.value = pinnedOnly
    }

    fun setPinned(id: String, pinned: Boolean) {
        viewModelScope.launch {
            setHistoryPinnedUseCase(id, pinned)
        }
    }

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)

    /** Progress of the running update of an entry, or idle. */
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    private var updateJob: Job? = null

    /**
     * Fetches the source of [entry] again and makes its summaries again from the new content.
     * [onFinished] gets how it ended and the entry as it is now (null if it no longer exists).
     */
    fun updateEntry(entry: HistorySummary, onFinished: (UpdateResult, HistorySummary?) -> Unit) {
        if (updateJob?.isActive == true) return
        updateJob = viewModelScope.launch {
            _updateState.value = UpdateUiState.Running(entry.id, done = 0, total = 0)
            var result: UpdateResult = UpdateResult.Cancelled
            try {
                result = updateHistoryEntryUseCase(entry) { progress ->
                    _updateState.value = UpdateUiState.Running(entry.id, progress.done, progress.total)
                }.fold(
                    onSuccess = { updateResultOf(it) },
                    onFailure = { UpdateResult.Failed(it.toSummaryException()) },
                )
            } finally {
                // Also after a cancel: some summaries may have been made again already.
                withContext(NonCancellable) {
                    _updateState.value = UpdateUiState.Idle
                    onFinished(result, getHistorySummaryUseCase(entry.id))
                }
            }
        }
    }

    fun cancelUpdate() {
        updateJob?.cancel()
    }

    /** Loads the entry with [id], for example to show it again after the screen was recreated. */
    fun loadSummary(id: String, onLoaded: (HistorySummary?) -> Unit) {
        viewModelScope.launch {
            onLoaded(getHistorySummaryUseCase(id))
        }
    }

    /** Removes one summary of [summary]; [onDone] gets the entry as it is now (null if deleted). */
    fun removeResult(
        summary: HistorySummary,
        result: HistoryLengthResult,
        onDone: (HistorySummary?) -> Unit = {},
    ) {
        viewModelScope.launch {
            onDone(removeHistoryResultUseCase(summary, result))
        }
    }

    fun undoRemoveResult(original: HistorySummary) {
        viewModelScope.launch {
            removeHistoryResultUseCase.undo(original)
        }
    }

    fun toggleSelection(id: String) {
        _selectedIds.value = _selectedIds.value.let { if (id in it) it - id else it + id }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun setSelectedPinned(pinned: Boolean) {
        val ids = _selectedIds.value
        viewModelScope.launch {
            setHistoryPinnedUseCase(ids, pinned)
            clearSelection()
        }
    }

    /** Deletes the selected entries; [onDeleted] gets them so the deletion can be undone. */
    fun deleteSelected(onDeleted: (List<HistorySummary>) -> Unit) {
        val ids = _selectedIds.value
        viewModelScope.launch {
            val deleted = deleteHistorySummariesUseCase(ids)
            clearSelection()
            onDeleted(deleted)
        }
    }

    fun restoreSummaries(summaries: List<HistorySummary>) {
        viewModelScope.launch {
            restoreHistorySummaryUseCase(summaries)
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