package io.github.utubesummary.llm.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.Locale

class YouTubeTrackChoiceTest {

    private fun track(language: String, auto: Boolean = false) = CaptionTrack(
        baseUrl = "https://example.com/$language",
        name = CaptionTrack.Name(language),
        languageCode = language,
        kind = if (auto) "asr" else null,
    )

    private fun choose(
        tracks: List<CaptionTrack>,
        preferred: List<String> = listOf("en"),
        defaultIndex: Int? = null,
    ) = selectCaptionTrack(tracks, preferred, defaultIndex)?.languageCode

    @Test
    fun `the manual track in the spoken language still wins`() {
        assertEquals("fr", choose(listOf(track("fr", auto = true), track("fr"), track("en"))))
    }

    @Test
    fun `a manual track in the wanted language beats the auto one`() {
        assertEquals("fr", choose(listOf(track("de", auto = true), track("fr")), preferred = listOf("fr", "en")))
    }

    @Test
    fun `wanted languages are tried in order`() {
        val tracks = listOf(track("ar"), track("en"), track("es"))

        assertEquals("es", choose(tracks, preferred = listOf("es", "en")))
        assertEquals("en", choose(tracks, preferred = listOf("de", "en")))
    }

    @Test
    fun `without spoken or wanted tracks YouTube's default track is used`() {
        assertEquals("de", choose(listOf(track("ar"), track("de"), track("es")), defaultIndex = 1))
    }

    @Test
    fun `the default hint names the spoken language when several auto tracks exist`() {
        val tracks = listOf(track("ar", auto = true), track("en", auto = true), track("en"))

        assertEquals("en", choose(tracks, preferred = listOf("de"), defaultIndex = 1))
    }

    @Test
    fun `a video with automatic dubbing gives the manual track of the original language`() {
        // 20 auto-generated tracks in many languages, one manual track: the first auto track is Arabic.
        val auto = listOf("ar", "bn", "nl", "en", "fr", "de", "hi", "ja", "ko", "pt", "ru", "es", "uk")
            .map { track(it, auto = true) }
        val tracks = auto.take(4) + track("en-US") + auto.drop(4)

        assertEquals("en-US", choose(tracks))
        // The spoken language (English: it has a manual and an auto track) wins over a wanted auto-only language.
        assertEquals("en-US", choose(tracks, preferred = listOf("de", "en")))
    }

    @Test
    fun `several auto tracks without a manual one give the wanted language, not the first`() {
        val tracks = listOf(track("ar", auto = true), track("en", auto = true), track("de", auto = true))

        assertEquals("en", choose(tracks))
        assertEquals("de", choose(tracks, preferred = listOf("de", "en")))
    }

    @Test
    fun `the original audio language decides when it is known`() {
        val tracks = listOf(track("ar", auto = true), track("en", auto = true))

        assertEquals("en", selectCaptionTrack(tracks, preferredLanguages = listOf("de"), originalAudioLanguage = "en")?.languageCode)
        assertEquals("ar", selectCaptionTrack(tracks, preferredLanguages = listOf("de"), originalAudioLanguage = "ar-EG")?.languageCode)
    }

    @Test
    fun `a regional manual track counts as its language`() {
        assertEquals("pt-BR", choose(listOf(track("fr", auto = true), track("pt-BR")), preferred = listOf("pt", "en")))
    }

    @Test
    fun `a default hint outside the list is ignored`() {
        assertEquals("ar", choose(listOf(track("ar"), track("de")), defaultIndex = 7))
        assertEquals("ar", choose(listOf(track("ar"), track("de")), defaultIndex = -1))
    }

    @Test
    fun `a regional variant of a wanted language is used before the first track`() {
        assertEquals("en-GB", choose(listOf(track("ar"), track("en-GB"))))
    }

    @Test
    fun `with nothing to go on the first track is used and no tracks give none`() {
        assertEquals("ar", choose(listOf(track("ar"), track("de"))))
        assertNull(choose(emptyList()))
    }

    private fun player(json: String) = Json.parseToJsonElement(json).jsonObject

    @Test
    fun `the default caption track is read from the audio tracks`() {
        val response = player(
            """{"captions":{"playerCaptionsTracklistRenderer":{"audioTracks":[{"captionTrackIndices":[0,1],"defaultCaptionTrackIndex":1}]}}}"""
        )

        assertEquals(1, defaultCaptionTrackIndex(response))
    }

    @Test
    fun `the default audio track decides which hint is used`() {
        val response = player(
            """{"captions":{"playerCaptionsTracklistRenderer":{"defaultAudioTrackIndex":1,"audioTracks":[
                {"defaultCaptionTrackIndex":0},{"defaultCaptionTrackIndex":2}]}}}"""
        )

        assertEquals(2, defaultCaptionTrackIndex(response))
    }

    @Test
    fun `no hint when the answer has none or a nonsense one`() {
        assertNull(defaultCaptionTrackIndex(player("{}")))
        assertNull(defaultCaptionTrackIndex(player("""{"captions":{"playerCaptionsTracklistRenderer":{}}}""")))
        assertNull(
            defaultCaptionTrackIndex(
                player("""{"captions":{"playerCaptionsTracklistRenderer":{"audioTracks":[{"defaultCaptionTrackIndex":-3}]}}}""")
            )
        )
    }

    @Test
    fun `the video language mode prefers no subtitle language`() {
        assertNull(preferredTranscriptLanguage(useContentLanguage = true, summaryLanguage = null, appLanguage = Locale.GERMAN))
    }

    @Test
    fun `a chosen or app language is preferred for the subtitles`() {
        assertEquals("ar", preferredTranscriptLanguage(false, Locale.forLanguageTag("ar"), Locale.ENGLISH))
        assertEquals("de", preferredTranscriptLanguage(false, null, Locale.GERMANY))
        assertNull(preferredTranscriptLanguage(false, null, Locale.ROOT))
    }

    @Test
    fun `the tool call carries no preferred language unless one is given`() {
        assertNull(YouTubeTranscript("https://youtu.be/dQw4w9WgXcQ").preferredLanguage)
        assertEquals("de", YouTubeTranscript("https://youtu.be/dQw4w9WgXcQ", "de").preferredLanguage)
    }
}
