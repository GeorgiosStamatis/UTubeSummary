package io.github.utubesummary.llm.tools

import io.github.utubesummary.exception.SummaryException
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BiliBiliHttpFailureTest {

    @Test
    fun `refused access asks for a login`() {
        listOf(401, 403, 412).forEach {
            assertTrue(bilibiliHttpFailure(it) is SummaryException.BiliBiliLoginRequiredException, "$it")
        }
    }

    @Test
    fun `a missing video is an invalid link`() {
        assertTrue(bilibiliHttpFailure(404) is SummaryException.InvalidLinkException)
    }

    @Test
    fun `server problems are not reported as no internet`() {
        listOf(429, 500, 502, 503).forEach {
            val failure = bilibiliHttpFailure(it)
            assertTrue(failure !is SummaryException.NoInternetException, "$it")
            assertTrue(failure is SummaryException.UnknownException, "$it")
        }
    }
}
