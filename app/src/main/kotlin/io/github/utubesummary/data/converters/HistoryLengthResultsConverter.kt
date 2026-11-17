package io.github.utubesummary.data.converters

import androidx.room3.ColumnTypeConverter
import kotlinx.serialization.json.Json
import io.github.utubesummary.model.HistoryLengthResult
import io.github.utubesummary.model.SummaryLength

class HistoryLengthResultsConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @ColumnTypeConverter
    fun fromLengthResults(value: Map<SummaryLength, HistoryLengthResult>?): String? {
        return value?.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) }
    }

    @ColumnTypeConverter
    fun fromResultList(value: List<HistoryLengthResult>?): String? {
        return value?.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) }
    }

    @ColumnTypeConverter
    fun toResultList(value: String?): List<HistoryLengthResult>? {
        if (value.isNullOrBlank()) return null
        return runCatching { json.decodeFromString<List<HistoryLengthResult>>(value) }.getOrNull()
    }

    @ColumnTypeConverter
    fun toLengthResults(value: String?): Map<SummaryLength, HistoryLengthResult>? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            json.decodeFromString<Map<SummaryLength, HistoryLengthResult>>(value)
        }.getOrNull()
    }
}
