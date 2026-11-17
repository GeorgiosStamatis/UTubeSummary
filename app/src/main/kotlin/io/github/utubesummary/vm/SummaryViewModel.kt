package io.github.utubesummary.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.github.utubesummary.domain.usecase.AskAboutContentUseCase
import io.github.utubesummary.domain.usecase.SummarizeContentUseCase
import io.github.utubesummary.exception.SummaryException
import io.github.utubesummary.exception.toSummaryException
import io.github.utubesummary.model.ChatExchange
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryOutput
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val summarizeContentUseCase: SummarizeContentUseCase,
    private val askAboutContentUseCase: AskAboutContentUseCase,
) : ViewModel() {

    private val _summarizationState = MutableStateFlow(SummarizationState())
    val summarizationState: StateFlow<SummarizationState> = _summarizationState.asStateFlow()

    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    private var currentInput: String? = null
    private var currentProfileId: String? = null
    private var currentLanguage: String? = null
    private var summarizeJob: Job? = null

    /** Questions about the current content; the text itself stays in memory in the use case. */
    fun askQuestion(question: String) {
        val input = currentInput ?: return
        val trimmed = question.trim()
        val chat = _chatState.value
        if (trimmed.isEmpty() || chat.isAsking || !chat.canAsk) return

        val history = chat.turns.mapNotNull { turn ->
            turn.answer?.let { ChatExchange(turn.question, it) }
        }
        _chatState.update { it.copy(turns = it.turns + ChatTurn(trimmed), isAsking = true) }

        viewModelScope.launch {
            askAboutContentUseCase(input, history, trimmed)
                .onSuccess { answer -> finishLastTurn(answer = answer) }
                .onFailure { throwable ->
                    Log.e(TAG, "Question failed", throwable)
                    finishLastTurn(error = throwable.toSummaryException())
                }
        }
    }

    private fun finishLastTurn(answer: String? = null, error: SummaryException? = null) {
        _chatState.update { state ->
            if (state.turns.isEmpty()) return@update state.copy(isAsking = false)
            val updated = state.turns.last().copy(answer = answer, error = error)
            state.copy(turns = state.turns.dropLast(1) + updated, isAsking = false)
        }
    }

    private fun resetChat(input: String?) {
        _chatState.value = ChatState(
            canAsk = input != null && askAboutContentUseCase.isAvailable(input)
        )
    }

    /** Stops the running summarization, if any. Results already cached stay available. */
    fun cancelSummarize() {
        summarizeJob?.cancel()
        summarizeJob = null
        _summarizationState.update { it.copy(isLoading = false) }
    }

    fun clearCurrentSummary() {
        summarizeJob?.cancel()
        summarizeJob = null
        currentInput = null
        currentProfileId = null
        currentLanguage = null
        resetChat(null)
        _summarizationState.update {
            it.copy(
                isLoading = false,
                summaryResult = null,
                error = null,
                lengthResults = emptyMap()
            )
        }
    }

    fun setExistingSummary(
        output: SummaryOutput,
        input: String? = null,
        lengthResults: Map<SummaryLength, SummaryOutput> = emptyMap(),
    ) {
        summarizeJob?.cancel()
        summarizeJob = null
        currentInput = input ?: output.sourceLink
        currentProfileId = null
        currentLanguage = null
        resetChat(currentInput)
        _summarizationState.update {
            val combinedResults = if (lengthResults.isNotEmpty()) {
                lengthResults + (output.length to output)
            } else {
                mapOf(output.length to output)
            }
            it.copy(
                isLoading = false,
                summaryResult = output,
                lengthResults = combinedResults,
                profileId = null,
                language = null,
                error = null,
            )
        }
    }

    fun switchLength(length: SummaryLength) {
        if (_summarizationState.value.isLoading) return
        val currentResults = _summarizationState.value.lengthResults.ifEmpty {
            val current = _summarizationState.value.summaryResult
            if (current?.length != null) mapOf(current.length to current) else emptyMap()
        }
        if (currentResults.isEmpty()) return
        val cachedResult = currentResults[length] ?: currentResults[SummaryLength.NONE]
        _summarizationState.update {
            it.copy(
                summaryResult = cachedResult,
                lengthResults = if (it.lengthResults.isEmpty() && currentResults.isNotEmpty()) currentResults else it.lengthResults,
                error = null
            )
        }
    }

    fun summarize(
        text: String,
        overrideLength: SummaryLength? = null,
        profileId: String? = null,
        language: String? = null,
    ) {
        // A newer request replaces the running one, so a late result can never overwrite it.
        summarizeJob?.cancel()
        summarizeJob = viewModelScope.launch {
            if (currentInput != text || currentProfileId != profileId || currentLanguage != language) {
                // New content, profile or language: cached results no longer apply.
                if (currentInput != text) resetChat(null)
                currentInput = text
                currentProfileId = profileId
                currentLanguage = language
                _summarizationState.update {
                    it.copy(
                        isLoading = true,
                        summaryResult = null,
                        error = null,
                        lengthResults = emptyMap()
                    )
                }
            } else {
                _summarizationState.update {
                    it.copy(
                        isLoading = true,
                        error = null
                    )
                }
            }

            summarizeContentUseCase(text, overrideLength, profileId, language)
                .onSuccess { output ->
                    // The text of this input is now in memory: questions can be asked.
                    _chatState.update { it.copy(canAsk = askAboutContentUseCase.isAvailable(text)) }
                    _summarizationState.update {
                        val updatedLengthResults = it.lengthResults + (output.length to output)
                        it.copy(
                            summaryResult = output,
                            lengthResults = updatedLengthResults,
                            profileId = profileId,
                            language = language,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    Log.e(TAG, "Summarization failed", throwable)
                    val error = throwable.toSummaryException()
                    _summarizationState.update {
                        it.copy(
                            isLoading = false,
                            error = error,
                        )
                    }
                }
        }
    }

    companion object {
        private const val TAG = "SummaryViewModel"
    }
}