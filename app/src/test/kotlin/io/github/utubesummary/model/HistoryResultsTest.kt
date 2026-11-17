package io.github.utubesummary.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
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
    fun `all results are listed oldest first across lengths with the current one last`() {
        val summary = HistorySummary(
            title = "t",
            type = SummaryType.ARTICLE,
            length = SummaryLength.MEDIUM,
            lengthResults = mapOf(SummaryLength.MEDIUM to result("newest", createdOn = 300L)),
            alternatives = listOf(
                result("middle", profileId = "a", createdOn = 200L),
                result("oldest", profileId = "b", createdOn = 100L),
                result("other length", SummaryLength.LONG, createdOn = 150L),
            ),
        )

        assertEquals(
            listOf("oldest", "other length", "middle", "newest"),
            summary.allResultsOldestFirst().map { it.summary }
        )
    }

    @Test
    fun `entries from before timestamps end with the result that was made last`() {
        val summary = HistorySummary(
            title = "t",
            type = SummaryType.ARTICLE,
            length = SummaryLength.SHORT,
            lengthResults = mapOf(
                SummaryLength.LONG to result("long", SummaryLength.LONG),
                SummaryLength.SHORT to result("short", SummaryLength.SHORT),
            ),
        )

        assertEquals(listOf("long", "short"), summary.allResultsOldestFirst().map { it.summary })
    }

    @Test
    fun `a single result stays single`() {
        val summary = HistorySummary(
            title = "t",
            type = SummaryType.ARTICLE,
            length = SummaryLength.MEDIUM,
            lengthResults = mapOf(SummaryLength.MEDIUM to result("only")),
        )

        assertEquals(listOf("only"), summary.allResultsOldestFirst().map { it.summary })
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

    private fun item(
        current: Map<SummaryLength, HistoryLengthResult>,
        alternatives: List<HistoryLengthResult> = emptyList(),
        length: SummaryLength = current.keys.first(),
    ) = HistorySummary(
        title = "t",
        type = SummaryType.ARTICLE,
        length = length,
        lengthResults = current,
        alternatives = alternatives,
    )

    @Test
    fun `removing an alternative keeps the current results`() {
        val current = result("current", createdOn = 300L)
        val alternative = result("alt", profileId = "a", createdOn = 100L)

        val updated = item(mapOf(SummaryLength.MEDIUM to current), listOf(alternative)).withoutResult(alternative)!!

        assertEquals(mapOf(SummaryLength.MEDIUM to current), updated.lengthResults)
        assertTrue(updated.alternatives.isEmpty())
    }

    @Test
    fun `removing the current result promotes the newest alternative of that length`() {
        val current = result("current", createdOn = 300L)
        val older = result("older", profileId = "a", createdOn = 100L)
        val newer = result("newer", profileId = "b", createdOn = 200L)

        val updated = item(mapOf(SummaryLength.MEDIUM to current), listOf(older, newer)).withoutResult(current)!!

        assertEquals("newer", updated.lengthResults.getValue(SummaryLength.MEDIUM).summary)
        assertEquals(listOf(older), updated.alternatives)
    }

    @Test
    fun `removing the last result of a length moves the entry to another length`() {
        val short = result("short", SummaryLength.SHORT, createdOn = 100L)
        val long = result("long", SummaryLength.LONG, createdOn = 200L)

        val updated = item(
            mapOf(SummaryLength.SHORT to short, SummaryLength.LONG to long),
            length = SummaryLength.LONG,
        ).withoutResult(long)!!

        assertEquals(SummaryLength.SHORT, updated.length)
        assertEquals(setOf(SummaryLength.SHORT), updated.lengthResults.keys)
    }

    @Test
    fun `removing the only result leaves nothing and an unknown result changes nothing`() {
        val only = result("only")
        val summary = item(mapOf(SummaryLength.MEDIUM to only))

        assertNull(summary.withoutResult(only))
        assertEquals(summary, summary.withoutResult(result("not stored")))
    }
}
