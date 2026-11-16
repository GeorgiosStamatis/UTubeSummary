package io.github.utubesummary.model

/** Outcome of importing video summaries from a backup file. */
data class ImportResult(
    val imported: Int,
    /** Videos already in the history (or repeated in the file), left untouched. */
    val skippedExisting: Int,
    /** Entries that were missing required data and could not be imported. */
    val invalid: Int,
)
