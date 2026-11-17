package io.github.utubesummary.llm

import io.github.utubesummary.model.SummarizationPhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The phase of the summarization in progress; null while nothing is running. A new request
 * ([start]) replaces the running one, and the old one finishing later ([finish]) must not clear the
 * phase of the new one, so every request gets an id.
 */
@Singleton
class SummarizationProgress @Inject constructor() {
    private val _phase = MutableStateFlow<SummarizationPhase?>(null)
    val phase: StateFlow<SummarizationPhase?> = _phase.asStateFlow()

    private var currentRequest = 0L

    /** Begins a request: it becomes the current one and starts in [SummarizationPhase.EXTRACTING]. */
    @Synchronized
    fun start(): Long {
        currentRequest++
        _phase.value = SummarizationPhase.EXTRACTING
        return currentRequest
    }

    /** Sets the phase of the current request. */
    fun report(phase: SummarizationPhase?) {
        _phase.value = phase
    }

    /** Ends request [id]; nothing changes if a newer request has started since. */
    @Synchronized
    fun finish(id: Long) {
        if (id == currentRequest) _phase.value = null
    }
}
