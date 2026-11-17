package io.github.utubesummary.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HistoryResultsTest {

    private fun result(
        text: String,
        length: SummaryLength = SummaryLength.MEDIUM,
        profileId: String? = null,
        language: String? = null,
        createdOn: Long = 0L,
    ) = HistoryLengthResult(
        length = length,
        summary = text,
        profileId = profileId,
        language = language,
        createdOn = createdOn,
    )

    @Test
    fun `an older entry without a profile counts as the Default profile`() {
        assertTrue(isSameVariant(result("a"), result("b", profileId = DEFAULT_PROFILE_ID)))
        assertFalse(isSameVariant(result("a"), result("b", profileId = "quick_brief")))
        assertFalse(isSameVariant(result("a", language = "fr"), result("b", language = "content")))
        assertFalse(isSameVariant(result("a"), result("b", length = SummaryLength.LONG)))
    }

    @Test
    fun `a result for a new length is simply added`() {
        val (current, alternatives) = mergeHistoryResult(
            mapOf(SummaryLength.SHORT to result("short", SummaryLength.SHORT)),
            emptyList(),
            result("long", SummaryLength.LONG),
        )

        assertEquals(setOf(SummaryLength.SHORT, SummaryLength.LONG), current.keys)
        assertTrue(alternatives.isEmpty())
    }

    @Test
    fun `another profile displaces the current result into the alternatives`() {
        val old = result("old")
        val (current, alternatives) = mergeHistoryResult(
            mapOf(SummaryLength.MEDIUM to old), emptyList(), result("new", profileId = "quick_brief")
        )

        assertEquals("new", current.getValue(SummaryLength.MEDIUM).summary)
        assertEquals(listOf(old), alternatives)
    }

    @Test
    fun `the same variant is replaced everywhere without duplicates`() {
        val brief = result("brief 1", profileId = "quick_brief")
        val default = result("default")
        val (current, alternatives) = mergeHistoryResult(
            mapOf(SummaryLength.MEDIUM to default), listOf(brief), result("brief 2", profileId = "quick_brief")
        )

        assertEquals("brief 2", current.getValue(SummaryLength.MEDIUM).summary)
        assertEquals(listOf(default), alternatives)
    }

    @Test
    fun `results of a length are listed oldest first with the current one last`() {
        val summary = HistorySummary(
            title = "t",
            type = SummaryType.ARTICLE,
            length = SummaryLength.MEDIUM,
            lengthResults = mapOf(SummaryLength.MEDIUM to result("newest", createdOn = 300L)),
            alternatives = listOf(
                result("middle", profileId = "a", createdOn = 200L),
                result("oldest", profileId = "b", createdOn = 100L),
                result("other length", SummaryLength.LONG, createdOn = 50L),
            ),
        )

        assertEquals(listOf("oldest", "middle", "newest"), summary.resultsFor(SummaryLength.MEDIUM).map { it.summary })
    }

    @Test
    fun `variant keys list each profile once`() {
        val summary = HistorySummary(
            title = "t",
            type = SummaryType.ARTICLE,
            lengthResults = mapOf(SummaryLength.MEDIUM to result("a", profileId = "quick_brief")),
            alternatives = listOf(result("b"), result("c", profileId = "quick_brief")),
        )

        assertEquals(" quick_brief default ", summary.variantKeys())
        assertEquals("", HistorySummary(title = "t", type = SummaryType.ARTICLE).variantKeys())
    }
}
