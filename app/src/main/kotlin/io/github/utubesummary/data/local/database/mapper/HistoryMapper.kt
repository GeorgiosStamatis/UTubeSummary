package io.github.utubesummary.data.local.database.mapper

import io.github.utubesummary.data.local.database.entity.HistoryEntity
import io.github.utubesummary.model.HistoryLengthResult
import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.variantKeys

fun HistoryEntity.toDomain(): HistorySummary {
    val results = lengthResults?.takeIf { it.isNotEmpty() } ?: mapOf(
        length to HistoryLengthResult(
            length = length,
            summary = summary,
            provider = provider,
            model = model,
        )
    )
    return HistorySummary(
        id = id,
        title = title,
        author = author,
        createdOn = createdOn,
        length = length,
        type = type,
        subtype = subtype,
        sourceLink = sourceLink,
        sourceText = sourceText,
        lengthResults = results,
        alternatives = alternatives.orEmpty(),
        pinned = pinned,
    )
}

fun HistorySummary.toEntity(): HistoryEntity = HistoryEntity(
    id = id,
    title = title,
    summary = summary,
    author = author,
    createdOn = createdOn,
    length = length,
    type = type,
    subtype = subtype,
    sourceLink = sourceLink,
    sourceText = sourceText,
    provider = provider,
    model = model,
    lengthResults = lengthResults.takeIf { it.isNotEmpty() },
    alternatives = alternatives.takeIf { it.isNotEmpty() },
    pinned = pinned,
    variantKeys = variantKeys(),
)
