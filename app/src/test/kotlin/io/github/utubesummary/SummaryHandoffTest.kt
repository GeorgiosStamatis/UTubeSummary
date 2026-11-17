package io.github.utubesummary

import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryOutput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SummaryHandoffTest {

    private fun entry(text: String): SummaryHandoff.Entry {
        val output = SummaryOutput(
            title = text,
            author = "",
            summary = text,
            isYoutubeLink = false,
            isBiliBiliLink = false,
            length = SummaryLength.MEDIUM,
        )
        return SummaryHandoff.Entry(output, mapOf(SummaryLength.MEDIUM to output))
    }

    @Test
    fun `an entry can be taken once`() {
        val id = SummaryHandoff.put(entry("one"))

        assertEquals("one", SummaryHandoff.take(id)!!.summary.summary)
        assertNull(SummaryHandoff.take(id))
    }

    @Test
    fun `an unknown id gives nothing`() {
        assertNull(SummaryHandoff.take("does-not-exist"))
    }

    @Test
    fun `only the most recent entries are kept`() {
        val first = SummaryHandoff.put(entry("first"))
        repeat(3) { SummaryHandoff.put(entry("later $it")) }

        assertNull(SummaryHandoff.take(first))
    }
}
