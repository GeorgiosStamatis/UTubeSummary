package io.github.utubesummary.domain.usecase

import io.github.utubesummary.llm.SummarizationProgress
import io.github.utubesummary.model.SummarizationPhase
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** The phase of the running summarization, or null when none is running. */
class ObserveSummarizationPhaseUseCase @Inject constructor(
    private val progress: SummarizationProgress,
) {
    operator fun invoke(): StateFlow<SummarizationPhase?> = progress.phase
}
