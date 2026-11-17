package io.github.utubesummary.domain.usecase

import io.github.utubesummary.domain.importer.buildVideoSummaryBackup
import io.github.utubesummary.domain.provider.TextFileWriter
import io.github.utubesummary.domain.repository.HistoryRepository
import javax.inject.Inject

/** Exports the video summaries of the History to a file the user chose; returns how many. */
interface ExportHistoryUseCase {
    suspend operator fun invoke(uri: String): Result<Int>
}

class ExportHistoryUseCaseImpl @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val textFileWriter: TextFileWriter,
) : ExportHistoryUseCase {
    override suspend operator fun invoke(uri: String): Result<Int> = runCatching {
        val backup = buildVideoSummaryBackup(historyRepository.getAllSummaries())
        if (backup.count > 0) textFileWriter.writeText(uri, backup.json)
        backup.count
    }
}
