package io.github.utubesummary.domain.usecase

import io.github.utubesummary.domain.provider.IoDispatcher
import io.github.utubesummary.domain.provider.SourceTextStore
import io.github.utubesummary.domain.repository.UserPreferencesRepository
import io.github.utubesummary.exception.SummaryException
import io.github.utubesummary.exception.toSummaryException
import io.github.utubesummary.llm.LlmGateway
import io.github.utubesummary.model.DEFAULT_PROFILE_ID
import io.github.utubesummary.model.HistoryLengthResult
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummarySource
import io.github.utubesummary.model.SummaryType
import io.github.utubesummary.model.contentFingerprint
import io.github.utubesummary.model.isSameVariant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** How far an update is: [done] of [total] summaries have been made again. */
data class UpdateProgress(val done: Int, val total: Int)

/**
 * What an update did: [updated] summaries were made again, [upToDate] already matched the new
 * content, [skipped] belong to a profile that no longer exists, [failed] could not be made.
 */
data class UpdateOutcome(val updated: Int, val upToDate: Int, val skipped: Int, val failed: Int)

/** Which summaries of an entry an update has to make again. */
data class UpdatePlan(val toRun: List<HistoryLengthResult>, val upToDate: Int, val skipped: Int)

/**
 * Plans an update of [entry] for new content with [freshFingerprint]. Every distinct variant
 * (profile, language, length) is looked at once, using its newest result. A variant whose profile is
 * not in [knownProfileIds] is skipped, one made from the same content is up to date, every other one is
 * to run, oldest first so the newest summary stays the current one of its length.
 */
fun planUpdate(entry: HistorySummary, freshFingerprint: String, knownProfileIds: Set<String>): UpdatePlan {
    val newestPerVariant = mutableListOf<HistoryLengthResult>()
    for (result in entry.allResultsOldestFirst().asReversed()) {
        if (newestPerVariant.none { isSameVariant(it, result) }) newestPerVariant += result
    }
    val toRun = mutableListOf<HistoryLengthResult>()
    var upToDate = 0
    var skipped = 0
    for (result in newestPerVariant.asReversed()) {
        when {
            (result.profileId ?: DEFAULT_PROFILE_ID) !in knownProfileIds -> skipped++
            result.sourceFingerprint == freshFingerprint -> upToDate++
            else -> toRun += result
        }
    }
    return UpdatePlan(toRun, upToDate, skipped)
}

/** Fetches the source of a History entry again and makes its summaries again from the new content. */
interface UpdateHistoryEntryUseCase {
    suspend operator fun invoke(
        entry: HistorySummary,
        onProgress: (UpdateProgress) -> Unit = {},
    ): Result<UpdateOutcome>
}

class UpdateHistoryEntryUseCaseImpl @Inject constructor(
    private val llmGateway: LlmGateway,
    private val summarizeContent: SummarizeContentUseCase,
    private val sourceTextStore: SourceTextStore,
    private val userPreferencesRepository: UserPreferencesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : UpdateHistoryEntryUseCase {

    override suspend fun invoke(
        entry: HistorySummary,
        onProgress: (UpdateProgress) -> Unit,
    ): Result<UpdateOutcome> = withContext(ioDispatcher) {
        runCatching {
            val link = entry.sourceLink?.takeIf { it.isNotBlank() } ?: throw SummaryException.InvalidLinkException()
            val source = when (entry.type) {
                SummaryType.VIDEO -> SummarySource.Video(link)
                SummaryType.ARTICLE -> SummarySource.Article(link)
                else -> throw SummaryException.InvalidLinkException()
            }
            val prefs = userPreferencesRepository.preferencesFlow.first()

            val fresh = llmGateway.extractContent(source)
            // Kept for the summaries below: they find the new text here, so it is fetched only once.
            sourceTextStore.putContent(link, fresh)

            val plan = planUpdate(
                entry = entry,
                freshFingerprint = contentFingerprint(fresh.content),
                knownProfileIds = resolveProfiles(prefs).map { it.id }.toSet(),
            )

            var updated = 0
            var failed = 0
            var lastError: Throwable? = null
            plan.toRun.forEachIndexed { index, result ->
                onProgress(UpdateProgress(done = index, total = plan.toRun.size))
                summarizeContent(
                    text = link,
                    overrideLength = result.length.takeIf { it != SummaryLength.NONE },
                    profileId = result.profileId ?: DEFAULT_PROFILE_ID,
                    language = result.language,
                ).onSuccess {
                    updated++
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    failed++
                    lastError = error
                }
            }
            onProgress(UpdateProgress(done = plan.toRun.size, total = plan.toRun.size))

            // Nothing could be made again: show why (for example no provider is selected).
            if (updated == 0 && failed > 0) throw lastError!!
            UpdateOutcome(updated = updated, upToDate = plan.upToDate, skipped = plan.skipped, failed = failed)
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error ->
                if (error is CancellationException) throw error
                Result.failure(error.toSummaryException())
            },
        )
    }
}
