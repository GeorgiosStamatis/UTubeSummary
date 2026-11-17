package io.github.utubesummary.llm.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class YouTubeTranscriptToolTest {

    private val id = "dQw4w9WgXcQ"

    @Test
    fun `video id is read from every common link form`() {
        listOf(
            "https://www.youtube.com/watch?v=$id",
            "https://www.youtube.com/watch?v=$id&t=42s",
            "https://m.youtube.com/watch?feature=share&v=$id",
            "https://youtu.be/$id",
            "https://youtu.be/$id?si=tracking",
            "https://www.youtube.com/shorts/$id",
            "https://www.youtube.com/embed/$id",
            "https://www.youtube.com/live/$id",
            "https://www.youtube.com/$id",
            "youtube.com/watch?v=$id",
        ).forEach { link ->
            assertEquals(id, YouTubeTranscriptTool.extractVideoId(link), link)
        }
    }

    @Test
    fun `non youtube hosts and malformed ids give null`() {
        assertNull(YouTubeTranscriptTool.extractVideoId("https://example.com/watch?v=$id"))
        assertNull(YouTubeTranscriptTool.extractVideoId("https://www.youtube.com/watch?v=tooShort"))
        assertNull(YouTubeTranscriptTool.extractVideoId("https://www.youtube.com/"))
    }

    private fun track(language: String, auto: Boolean = false) = CaptionTrack(
        baseUrl = "https://example.com/$language${if (auto) "-asr" else ""}",
        name = CaptionTrack.Name(language),
        languageCode = language,
        kind = if (auto) "asr" else null,
    )

    @Test
    fun `a manual track in the spoken language is preferred`() {
        val tracks = listOf(track("en"), track("fr", auto = true), track("fr"))

        assertEquals("fr", selectCaptionTrack(tracks)!!.languageCode)
        assertNull(selectCaptionTrack(tracks)!!.kind)
    }

    @Test
    fun `without a manual spoken track a manual english track wins over the auto one`() {
        val tracks = listOf(track("fr", auto = true), track("en"))

        assertEquals("en", selectCaptionTrack(tracks)!!.languageCode)
    }

    @Test
    fun `only an auto generated track is used when nothing else exists`() {
        val tracks = listOf(track("de", auto = true))

        assertEquals("asr", selectCaptionTrack(tracks)!!.kind)
        assertNull(selectCaptionTrack(emptyList()))
    }

    @Test
    fun `playability problems are reported with their reason`() {
        val restricted = Json.parseToJsonElement(
            """{"playabilityStatus":{"status":"LOGIN_REQUIRED","reason":"Sign in to confirm your age"}}"""
        ).jsonObject
        val fine = Json.parseToJsonElement("""{"playabilityStatus":{"status":"OK"}}""").jsonObject

        assertEquals("Sign in to confirm your age", playabilityProblem(restricted))
        assertNull(playabilityProblem(fine))
        assertNull(playabilityProblem(Json.parseToJsonElement("{}").jsonObject))
    }
}
