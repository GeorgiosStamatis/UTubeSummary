package io.github.utubesummary.domain.usecase

import io.github.utubesummary.model.CRITICAL_ANALYST_PROFILE_ID
import io.github.utubesummary.model.DEFAULT_PROFILE_ID
import io.github.utubesummary.model.HistoryLengthResult
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.QUICK_BRIEF_PROFILE_ID
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UpdatePlanTest {

    private val known = setOf(DEFAULT_PROFILE_ID, QUICK_BRIEF_PROFILE_ID, CRITICAL_ANALYST_PROFILE_ID)

    private fun result(
        profile: String?,
        createdOn: Long,
        fingerprint: String? = "old",
        length: SummaryLength = SummaryLength.MEDIUM,
        language: String? = "content",
    ) = HistoryLengthResult(
        length = length,
        summary = "summary $profile $createdOn",
        profileId = profile,
        language = language,
        createdOn = createdOn,
        sourceFingerprint = fingerprint,
    )

    private fun entry(current: HistoryLengthResult, vararg alternatives: HistoryLengthResult) = HistorySummary(
        title = "t",
        type = SummaryType.ARTICLE,
        sourceLink = "https://example.com/post",
        length = current.length,
        lengthResults = mapOf(current.length to current),
        alternatives = alternatives.toList(),
    )

    private fun profiles(plan: UpdatePlan) = plan.toRun.map { it.profileId ?: DEFAULT_PROFILE_ID }

    @Test
    fun `every outdated summary is planned, oldest first`() {
        val entry = entry(
            current = result(CRITICAL_ANALYST_PROFILE_ID, 3),
            result(DEFAULT_PROFILE_ID, 1),
            result(QUICK_BRIEF_PROFILE_ID, 2),
        )

        val plan = planUpdate(entry, "new", known)

        assertEquals(listOf(DEFAULT_PROFILE_ID, QUICK_BRIEF_PROFILE_ID, CRITICAL_ANALYST_PROFILE_ID), profiles(plan))
        assertEquals(0, plan.upToDate)
        assertEquals(0, plan.skipped)
    }

    @Test
    fun `summaries made from the same content are up to date and not planned`() {
        val entry = entry(
            current = result(QUICK_BRIEF_PROFILE_ID, 2, fingerprint = "new"),
            result(DEFAULT_PROFILE_ID, 1, fingerprint = "new"),
        )

        val plan = planUpdate(entry, "new", known)

        assertEquals(emptyList<HistoryLengthResult>(), plan.toRun)
        assertEquals(2, plan.upToDate)
    }

    @Test
    fun `only the outdated ones of a mixed entry are planned`() {
        val entry = entry(
            current = result(QUICK_BRIEF_PROFILE_ID, 2, fingerprint = "new"),
            result(DEFAULT_PROFILE_ID, 1, fingerprint = "old"),
        )

        val plan = planUpdate(entry, "new", known)

        assertEquals(listOf(DEFAULT_PROFILE_ID), profiles(plan))
        assertEquals(1, plan.upToDate)
    }

    @Test
    fun `a summary without a fingerprint is outdated`() {
        val plan = planUpdate(entry(result(DEFAULT_PROFILE_ID, 1, fingerprint = null)), "new", known)

        assertEquals(1, plan.toRun.size)
    }

    @Test
    fun `a profile that no longer exists is skipped`() {
        val entry = entry(
            current = result("deleted_custom_profile", 2),
            result(DEFAULT_PROFILE_ID, 1),
        )

        val plan = planUpdate(entry, "new", known)

        assertEquals(listOf(DEFAULT_PROFILE_ID), profiles(plan))
        assertEquals(1, plan.skipped)
    }

    @Test
    fun `an old result without a profile counts as the default profile`() {
        val plan = planUpdate(entry(result(null, 1)), "new", known)

        assertEquals(1, plan.toRun.size)
        assertEquals(0, plan.skipped)
    }

    @Test
    fun `the same variant is planned once, with its newest result`() {
        val entry = entry(
            current = result(QUICK_BRIEF_PROFILE_ID, 5),
            result(QUICK_BRIEF_PROFILE_ID, 1),
        )

        val plan = planUpdate(entry, "new", known)

        assertEquals(1, plan.toRun.size)
        assertEquals(5L, plan.toRun.single().createdOn)
    }

    @Test
    fun `other lengths and languages are other variants`() {
        val entry = entry(
            current = result(DEFAULT_PROFILE_ID, 3, length = SummaryLength.LONG),
            result(DEFAULT_PROFILE_ID, 1, length = SummaryLength.MEDIUM),
            result(DEFAULT_PROFILE_ID, 2, language = "de"),
        )

        val plan = planUpdate(entry, "new", known)

        assertEquals(3, plan.toRun.size)
    }

    @Test
    fun `an entry without results needs nothing`() {
        val plan = planUpdate(HistorySummary(title = "t", type = SummaryType.ARTICLE), "new", known)

        assertEquals(UpdatePlan(emptyList(), upToDate = 0, skipped = 0), plan)
    }
}
