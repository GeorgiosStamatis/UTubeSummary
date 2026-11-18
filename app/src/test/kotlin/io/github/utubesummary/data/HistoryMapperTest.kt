package io.github.utubesummary.data

import io.github.utubesummary.data.local.database.entity.HistoryEntity
import io.github.utubesummary.data.local.database.mapper.toDomain
import io.github.utubesummary.data.local.database.mapper.toEntity
import io.github.utubesummary.model.HistoryLengthResult
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryType
import io.github.utubesummary.model.VideoSubtype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HistoryMapperTest {

    private fun result(text: String, profileId: String? = null, length: SummaryLength = SummaryLength.MEDIUM) =
        HistoryLengthResult(length = length, summary = text, profileId = profileId, createdOn = 5L)

    @Test
    fun `an entry with results, alternatives and a pin survives a round trip`() {
        val original = HistorySummary(
            id = "id-1",
            title = "Title",
            author = "Author",
            createdOn = 123L,
            length = SummaryLength.MEDIUM,
            type = SummaryType.VIDEO,
            subtype = VideoSubtype.YOUTUBE,
            sourceLink = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            lengthResults = mapOf(SummaryLength.MEDIUM to result("current", profileId = "default")),
            alternatives = listOf(result("older", profileId = "quick_brief")),
            pinned = true,
        )

        assertEquals(original, original.toEntity().toDomain())
    }

    @Test
    fun `the profile ids of every result are stored for filtering`() {
        val entity = HistorySummary(
            title = "t",
            type = SummaryType.ARTICLE,
            length = SummaryLength.MEDIUM,
            lengthResults = mapOf(SummaryLength.MEDIUM to result("a", profileId = "default")),
            alternatives = listOf(result("b", profileId = "quick_brief")),
        ).toEntity()

        assertTrue(" default " in entity.variantKeys)
        assertTrue(" quick_brief " in entity.variantKeys)
    }

    @Test
    fun `empty result collections are stored as null`() {
        val entity = HistorySummary(title = "t", type = SummaryType.TEXT).toEntity()

        assertNull(entity.lengthResults)
        assertNull(entity.alternatives)
    }

    @Test
    fun `an old entry without result maps builds its single result from the columns`() {
        val entity = HistoryEntity(
            id = "old",
            title = "Old",
            summary = "old text",
            createdOn = 1L,
            length = SummaryLength.SHORT,
            type = SummaryType.ARTICLE,
            provider = "OPENAI",
            model = "gpt",
        )

        val domain = entity.toDomain()

        assertEquals("old text", domain.summary)
        assertEquals("OPENAI", domain.provider)
        assertEquals("gpt", domain.model)
        assertEquals(listOf(SummaryLength.SHORT), domain.lengthResults.keys.toList())
        assertEquals(emptyList<HistoryLengthResult>(), domain.alternatives)
        assertEquals(false, domain.pinned)
    }
}
