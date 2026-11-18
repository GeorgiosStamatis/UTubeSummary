package io.github.utubesummary.llm.tools

import io.github.utubesummary.exception.SummaryException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class YouTubeFallbackTest {

    private val calls = mutableListOf<String>()

    private fun attempt(label: String, result: AttemptResult<String>) = Attempt(label) {
        calls += label
        result
    }

    private suspend fun failureOf(attempts: List<Attempt<String>>): SummaryException =
        try {
            runAttempts(attempts)
            throw AssertionError("expected a failure")
        } catch (e: SummaryException) {
            e
        }

    @Test
    fun `the first success wins and later attempts do not run`() = runTest {
        val value = runAttempts(
            listOf(
                attempt("A", AttemptResult.Success("first")),
                attempt("B", AttemptResult.Success("second")),
            )
        )

        assertEquals("first", value)
        assertEquals(listOf("A"), calls)
    }

    @Test
    fun `a failed attempt is followed by the next one`() = runTest {
        val value = runAttempts(
            listOf(
                attempt("A", AttemptResult.Failed("player API: HTTP 400")),
                attempt("B", AttemptResult.Success("second")),
            )
        )

        assertEquals("second", value)
        assertEquals(listOf("A", "B"), calls)
    }

    @Test
    fun `a removed or private video stops everything`() = runTest {
        val failure = failureOf(
            listOf(
                attempt("A", AttemptResult.Restricted("Private video")),
                attempt("B", AttemptResult.Success("never")),
            )
        )

        assertTrue(failure is SummaryException.VideoUnavailableException)
        assertEquals(listOf("A"), calls)
    }

    @Test
    fun `a sign in answer is retried with the next client`() = runTest {
        val value = runAttempts(
            listOf(
                attempt("A", AttemptResult.Restricted("Sign in", retryable = true)),
                attempt("B", AttemptResult.Success("second")),
            )
        )

        assertEquals("second", value)
    }

    @Test
    fun `when every client wants a sign in the video counts as unavailable`() = runTest {
        val failure = failureOf(
            listOf(
                attempt("A", AttemptResult.Restricted("Sign in to confirm", retryable = true)),
                attempt("B", AttemptResult.Failed("player API: HTTP 400")),
            )
        )

        assertTrue(failure is SummaryException.VideoUnavailableException)
        assertEquals("Sign in to confirm", failure.message)
    }

    @Test
    fun `a playable video without subtitles is reported as no transcript`() = runTest {
        val failure = failureOf(
            listOf(
                attempt("A", AttemptResult.NoCaptions),
                attempt("B", AttemptResult.Failed("player API: HTTP 400")),
            )
        )

        assertTrue(failure is SummaryException.NoTranscriptException)
        assertEquals(listOf("A", "B"), calls)
    }

    @Test
    fun `when every attempt fails the details list them all`() = runTest {
        val failure = failureOf(
            listOf(
                attempt("ANDROID +key", AttemptResult.Failed("watch page: no API key found")),
                attempt("ANDROID no key", AttemptResult.Failed("player API: HTTP 400")),
                attempt("IOS no key", AttemptResult.Failed("subtitle download: empty answer")),
            )
        )

        assertTrue(failure is SummaryException.ExtractionFailedException)
        assertEquals(
            "ANDROID +key · watch page: no API key found | ANDROID no key · player API: HTTP 400 | IOS no key · subtitle download: empty answer",
            failure.technicalDetails
        )
    }

    @Test
    fun `no attempts is an extraction failure`() = runTest {
        val failure = failureOf(emptyList())

        assertTrue(failure is SummaryException.ExtractionFailedException)
    }

    @Test
    fun `an exception of an attempt is not hidden by the next one`() = runTest {
        val attempts = listOf(
            Attempt<String>("A") { throw IllegalStateException("no internet") },
            attempt("B", AttemptResult.Success("second")),
        )

        val thrown = assertThrows(IllegalStateException::class.java) {
            runBlocking { runAttempts(attempts) }
        }

        assertEquals("no internet", thrown.message)
        assertTrue(calls.isEmpty())
    }

    @Test
    fun `cancellation passes through`() = runTest {
        val attempts = listOf(
            Attempt<String>("A") { throw CancellationException("cancelled") },
            attempt("B", AttemptResult.Success("second")),
        )

        val thrown = assertThrows(CancellationException::class.java) {
            runBlocking { runAttempts(attempts) }
        }

        assertEquals("cancelled", thrown.message)
        assertTrue(calls.isEmpty())
    }
}
