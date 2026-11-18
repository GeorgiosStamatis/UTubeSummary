package io.github.utubesummary.llm.tools

import io.github.utubesummary.R
import io.github.utubesummary.exception.SummaryException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class YouTubeClientTest {

    private fun response(json: String) = Json.parseToJsonElement(json).jsonObject

    @Test
    fun `the request body is the one the app has always sent`() {
        val expected = response(
            """{"context": {"client": {"clientName": "ANDROID", "clientVersion": "20.10.38"}}, "videoId": "dQw4w9WgXcQ"}"""
        )

        assertEquals(expected, playerRequestBody(ANDROID_LEGACY, "dQw4w9WgXcQ"))
    }

    @Test
    fun `the client name and version come from the profile`() {
        val body = playerRequestBody(InnertubeClient("IOS", "9.9.9"), "abc")

        val client = body["context"]!!.jsonObject["client"]!!.jsonObject
        assertEquals("\"IOS\"", client["clientName"].toString())
        assertEquals("\"9.9.9\"", client["clientVersion"].toString())
        assertEquals("IOS 9.9.9", InnertubeClient("IOS", "9.9.9").label)
    }

    @Test
    fun `a playable video is playable`() {
        val playable = response("""{"playabilityStatus":{"status":"OK"},"videoDetails":{"title":"t"}}""")

        assertTrue(classifyPlayerResponse(playable) is PlayerOutcome.Playable)
        assertTrue(classifyPlayerResponse(response("{}")) is PlayerOutcome.Playable)
    }

    @Test
    fun `private, restricted and removed videos are restricted with YouTube's reason`() {
        listOf("LOGIN_REQUIRED", "UNPLAYABLE", "ERROR").forEach { status ->
            val restricted = response("""{"playabilityStatus":{"status":"$status","reason":"Because $status"}}""")

            val outcome = classifyPlayerResponse(restricted)

            assertEquals(PlayerOutcome.Restricted("Because $status"), outcome)
        }
    }

    @Test
    fun `only a restriction is reported as an unavailable video`() {
        val failure = PlayerOutcome.Restricted("Private video").toFailure(ANDROID_LEGACY)

        assertTrue(failure is SummaryException.VideoUnavailableException)
        assertEquals("Private video", failure.message)
    }

    @Test
    fun `an http error names the client, the step and the status`() {
        val failure = PlayerOutcome.HttpError("player API", 400).toFailure(ANDROID_LEGACY)

        assertTrue(failure is SummaryException.ExtractionFailedException)
        assertEquals("ANDROID 20.10.38 · player API: HTTP 400", failure.technicalDetails)
        assertEquals(R.string.youtubeExtractionFailed, failure.getUserMessageResId())
    }

    @Test
    fun `a missing key and an unreadable answer are extraction failures`() {
        val noKey = PlayerOutcome.NoApiKey("watch page").toFailure(ANDROID_LEGACY)
        val badBody = PlayerOutcome.BadBody("player API").toFailure(ANDROID_LEGACY)

        assertEquals("ANDROID 20.10.38 · watch page: no API key found", noKey.technicalDetails)
        assertEquals("ANDROID 20.10.38 · player API: unexpected answer", badBody.technicalDetails)
        assertTrue(noKey is SummaryException.ExtractionFailedException)
        assertTrue(badBody is SummaryException.ExtractionFailedException)
    }

    @Test
    fun `the client constant carries no secret`() {
        assertEquals("ANDROID", ANDROID_LEGACY.name)
        assertTrue(ANDROID_LEGACY.version.matches(Regex("""\d+(\.\d+)+""")))
    }

    @Test
    fun `the legacy client sends no extra headers`() {
        assertEquals(emptyMap<String, String>(), requestHeaders(ANDROID_LEGACY))
    }

    @Test
    fun `the full android client sends the headers of the real app`() {
        assertEquals(
            mapOf(
                "User-Agent" to "com.google.android.youtube/20.10.38 (Linux; U; Android 11) gzip",
                "X-YouTube-Client-Name" to "3",
                "X-YouTube-Client-Version" to "20.10.38",
            ),
            requestHeaders(ANDROID_FULL)
        )
    }

    @Test
    fun `the full android client adds device, os, language and region to the context`() {
        val body = playerRequestBody(ANDROID_FULL, "abc")

        val client = body["context"]!!.jsonObject["client"]!!.jsonObject
        assertEquals("\"ANDROID\"", client["clientName"].toString())
        assertEquals("30", client["androidSdkVersion"].toString())
        assertEquals("\"Android\"", client["osName"].toString())
        assertEquals("\"11\"", client["osVersion"].toString())
        assertEquals("\"en\"", client["hl"].toString())
        assertEquals("\"US\"", client["gl"].toString())
    }

    @Test
    fun `the two android profiles differ only in how they identify themselves`() {
        assertEquals(ANDROID_LEGACY.name, ANDROID_FULL.name)
        assertEquals(ANDROID_LEGACY.version, ANDROID_FULL.version)
        assertTrue(ANDROID_FULL.userAgent!!.contains(ANDROID_FULL.version))
    }

    @Test
    fun `no profile contains a key or token`() {
        listOf(ANDROID_LEGACY, ANDROID_FULL).forEach { client ->
            val text = client.toString() + requestHeaders(client) + playerRequestBody(client, "abc")
            assertTrue(!text.contains("AIza"), "$client looks like it holds an API key")
        }
    }
}
