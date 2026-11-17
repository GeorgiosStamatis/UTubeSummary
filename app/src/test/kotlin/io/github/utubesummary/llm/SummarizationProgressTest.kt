package io.github.utubesummary.llm

import io.github.utubesummary.model.SummarizationPhase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SummarizationProgressTest {

    private val progress = SummarizationProgress()

    @Test
    fun `a request starts extracting, can report phases and clears when it finishes`() {
        val id = progress.start()
        assertEquals(SummarizationPhase.EXTRACTING, progress.phase.value)

        progress.report(SummarizationPhase.SUMMARIZING)
        assertEquals(SummarizationPhase.SUMMARIZING, progress.phase.value)

        progress.finish(id)
        assertNull(progress.phase.value)
    }

    @Test
    fun `an older request finishing does not clear the phase of the newer one`() {
        val first = progress.start()
        val second = progress.start()
        progress.report(SummarizationPhase.SUMMARIZING)

        progress.finish(first)

        assertEquals(SummarizationPhase.SUMMARIZING, progress.phase.value)

        progress.finish(second)
        assertNull(progress.phase.value)
    }
}
