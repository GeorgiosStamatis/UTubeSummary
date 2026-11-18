package io.github.utubesummary.domain.usecase

import io.github.utubesummary.domain.provider.AppLocaleProvider
import io.github.utubesummary.domain.provider.ProviderConnectionChecker
import io.github.utubesummary.domain.provider.TextFileWriter
import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.model.HistoryFilter
import io.github.utubesummary.model.HistoryLengthResult
import io.github.utubesummary.model.HistorySort
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.ProviderTestResult
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryType
import io.github.utubesummary.testutil.FakeHistoryRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

class HistoryUseCasesTest {

    private val history = FakeHistoryRepository()

    private fun entry(id: String, link: String? = null) = HistorySummary(
        id = id,
        title = "Title $id",
        type = SummaryType.ARTICLE,
        sourceLink = link,
        length = SummaryLength.MEDIUM,
        lengthResults = mapOf(SummaryLength.MEDIUM to HistoryLengthResult(SummaryLength.MEDIUM, "text $id")),
    )

    @Test
    fun `bulk delete removes every chosen entry and returns them for undo`() = runTest {
        history.added += listOf(entry("a"), entry("b"), entry("c"))

        val deleted = DeleteHistorySummariesUseCase(history)(listOf("a", "c"))

        assertEquals(listOf("a", "c"), deleted.map { it.id })
        assertEquals(listOf("a", "c"), history.deleted)
    }

    @Test
    fun `bulk delete skips ids that no longer exist`() = runTest {
        history.added += entry("a")

        val deleted = DeleteHistorySummariesUseCase(history)(listOf("a", "missing"))

        assertEquals(listOf("a"), deleted.map { it.id })
        assertEquals(listOf("a"), history.deleted)
    }

    @Test
    fun `pinning applies to one entry or to many`() = runTest {
        val setPinned = SetHistoryPinnedUseCase(history)

        setPinned("one", true)
        setPinned(listOf("x", "y"), false)

        assertEquals(listOf("one" to true, "x" to false, "y" to false), history.pinCalls)
    }

    @Test
    fun `search text and filter reach the repository unchanged`() {
        val filter = HistoryFilter(pinnedOnly = true, sort = HistorySort.TITLE)

        GetHistorySummariesUseCase(history)("kotlin", filter)

        assertEquals("kotlin", history.lastQuery)
        assertEquals(filter, history.lastFilter)
    }

    private class RecordingWriter(private val fail: Boolean = false) : TextFileWriter {
        val written = mutableListOf<Pair<String, String>>()
        override suspend fun writeText(uriString: String, text: String) {
            if (fail) error("disk full")
            written += uriString to text
        }
    }

    private fun youtube(id: String) = entry("v-$id", "https://www.youtube.com/watch?v=$id")

    @Test
    fun `export writes the youtube videos and reports how many`() = runTest {
        history.added += listOf(youtube("dQw4w9WgXcQ"), entry("article", "https://example.com/post"))
        val writer = RecordingWriter()

        val result = ExportHistoryUseCaseImpl(history, writer)("content://file")

        assertEquals(1, result.getOrNull())
        assertEquals("content://file", writer.written.single().first)
        assertTrue("dQw4w9WgXcQ" in writer.written.single().second)
    }

    @Test
    fun `export with nothing to export writes no file`() = runTest {
        history.added += entry("article", "https://example.com/post")
        val writer = RecordingWriter()

        val result = ExportHistoryUseCaseImpl(history, writer)("content://file")

        assertEquals(0, result.getOrNull())
        assertTrue(writer.written.isEmpty())
    }

    @Test
    fun `a write failure is returned as a failed result`() = runTest {
        history.added += youtube("dQw4w9WgXcQ")

        val result = ExportHistoryUseCaseImpl(history, RecordingWriter(fail = true))("content://file")

        assertTrue(result.isFailure)
    }

    private class RecordingChecker : ProviderConnectionChecker {
        var provider: AIProvider? = null
        var baseUrl: String? = null
        var apiKey: String? = null
        var locale: Locale? = null
        override suspend fun check(
            provider: AIProvider,
            baseUrl: String,
            apiKey: String,
            locale: Locale,
        ): ProviderTestResult {
            this.provider = provider
            this.baseUrl = baseUrl
            this.apiKey = apiKey
            this.locale = locale
            return ProviderTestResult.Success(keyChecked = apiKey.isNotEmpty())
        }
    }

    @Test
    fun `the connection test normalizes the url, trims the key and uses the app locale`() = runTest {
        val checker = RecordingChecker()
        val useCase = TestProviderConnectionUseCase(checker, object : AppLocaleProvider {
            override fun getCurrentLocale(): Locale = Locale.GERMAN
        })

        val result = useCase(AIProvider.LM_STUDIO, "192.168.1.10:1234", "  tok  ")

        assertEquals(AIProvider.LM_STUDIO, checker.provider)
        assertEquals("http://192.168.1.10:1234", checker.baseUrl)
        assertEquals("tok", checker.apiKey)
        assertEquals(Locale.GERMAN, checker.locale)
        assertEquals(ProviderTestResult.Success(keyChecked = true), result)
    }
}
