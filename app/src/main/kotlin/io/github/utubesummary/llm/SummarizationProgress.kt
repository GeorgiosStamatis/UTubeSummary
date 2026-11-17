package io.github.utubesummary.llm

import io.github.utubesummary.model.SummarizationPhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The phase of the summarization in progress. Requests run one at a time (a new one replaces the
 * running one), so a single app-wide value is enough; it is null while nothing is running.
 */
@Singleton
class SummarizationProgress @Inject constructor() {
    private val _phase = MutableStateFlow<SummarizationPhase?>(null)
    val phase: StateFlow<SummarizationPhase?> = _phase.asStateFlow()

    fun report(phase: SummarizationPhase?) {
        _phase.value = phase
    }
}
