package io.github.utubesummary.llm.tools

import io.github.utubesummary.exception.SummaryException

/** What one way of asking YouTube led to. */
internal sealed interface AttemptResult<out T> {
    data class Success<T>(val value: T) : AttemptResult<T>

    /** YouTube refuses the video; [retryable] when another client may get a different answer. */
    data class Restricted(val reason: String, val retryable: Boolean = false) : AttemptResult<Nothing>

    /** The video is playable but has no subtitles. */
    data object NoCaptions : AttemptResult<Nothing>

    /** The request failed (HTTP error, unexpected answer, empty subtitles); [detail] says how. */
    data class Failed(val detail: String) : AttemptResult<Nothing>
}

/** One way of asking YouTube, named for logs and error details. */
internal class Attempt<T>(val label: String, val run: suspend () -> AttemptResult<T>)

/**
 * Runs [attempts] in order until one succeeds.
 *
 * A refusal that no other client can change (private, removed) ends it at once; a "sign in" refusal and
 * ordinary failures go on to the next attempt. When nothing succeeds, the error is the most specific
 * one: no subtitles, then an unavailable video, then an extraction failure listing every attempt.
 * Exceptions thrown by an attempt (no internet, cancellation) are not caught: another client would
 * not help.
 */
internal suspend fun <T> runAttempts(attempts: List<Attempt<T>>): T {
    var noCaptions = false
    var restrictedReason: String? = null
    val failures = mutableListOf<String>()
    for (attempt in attempts) {
        when (val result = attempt.run()) {
            is AttemptResult.Success -> return result.value
            is AttemptResult.Restricted -> {
                if (!result.retryable) throw SummaryException.VideoUnavailableException(result.reason)
                restrictedReason = restrictedReason ?: result.reason
                failures += "${attempt.label} · ${result.reason}"
            }
            AttemptResult.NoCaptions -> noCaptions = true
            is AttemptResult.Failed -> failures += "${attempt.label} · ${result.detail}"
        }
    }
    if (noCaptions) throw SummaryException.NoTranscriptException()
    restrictedReason?.let { throw SummaryException.VideoUnavailableException(it) }
    throw SummaryException.ExtractionFailedException(failures.joinToString(" | ").ifEmpty { "no way to ask YouTube" })
}
