package io.github.utubesummary.domain.usecase

import io.github.utubesummary.domain.provider.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.utubesummary.domain.importer.VideoSummaryBackupParser
import io.github.utubesummary.domain.provider.TextFileReader
import io.github.utubesummary.domain.repository.HistoryRepository
import io.github.utubesummary.model.HistoryLengthResult
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.ImportResult
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryType
import io.github.utubesummary.model.VideoSubtype
import io.github.utubesummary.model.youtubeVideoId
import javax.inject.Inject

private const val MAX_BACKUP_BYTES = 25L * 1024 * 1024

/** Imports video summaries from another app's JSON backup, skipping videos already in history. */
interface ImportVideoSummariesUseCase {
    /**
     * @param uri the chosen file
     * Fails with `InvalidBackupException` (not a backup file) or `FileTooLargeException`.
     */
    suspend operator fun invoke(uri: String): Result<ImportResult>
}

class ImportVideoSummariesUseCaseImpl @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val textFileReader: TextFileReader,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ImportVideoSummariesUseCase {

    override suspend fun invoke(uri: String): Result<ImportResult> = withContext(ioDispatcher) {
        runCatching {
            val text = textFileReader.readText(uri, MAX_BACKUP_BYTES)
            val parsed = VideoSummaryBackupParser.parse(text)

            // Same source = same URL; for YouTube, the same video whatever the link form.
            val known = historyRepository.getAllSourceLinks()
                .mapNotNull { duplicateKey(it) }
                .toMutableSet()

            val toImport = mutableListOf<HistorySummary>()
            var skipped = 0
            for (video in parsed.videos) {
                val key = duplicateKey(video.sourceLink)
                if (key != null && !known.add(key)) {
                    skipped++
                    continue
                }
                toImport += HistorySummary(
                    title = video.title,
                    author = video.author,
                    createdOn = video.createdOn,
                    length = SummaryLength.NONE,
                    type = SummaryType.VIDEO,
                    subtype = VideoSubtype.YOUTUBE,
                    sourceLink = video.sourceLink,
                    lengthResults = mapOf(
                        SummaryLength.NONE to HistoryLengthResult(
                            length = SummaryLength.NONE,
                            summary = video.summary,
                        )
                    ),
                )
            }
            if (toImport.isNotEmpty()) historyRepository.addSummaries(toImport)

            ImportResult(
                imported = toImport.size,
                skippedExisting = skipped,
                invalid = parsed.invalid,
            )
        }
    }

    private fun duplicateKey(link: String?): String? =
        youtubeVideoId(link)?.let { "yt:$it" }
            ?: link?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
}
