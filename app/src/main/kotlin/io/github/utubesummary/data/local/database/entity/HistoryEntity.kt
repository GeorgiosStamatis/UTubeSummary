package io.github.utubesummary.data.local.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.ColumnTypeConverters
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import io.github.utubesummary.data.converters.HistoryLengthResultsConverter
import io.github.utubesummary.data.converters.SummaryLengthConverter
import io.github.utubesummary.data.converters.SummaryTypeConverter
import io.github.utubesummary.data.converters.VideoSubtypeConverter
import io.github.utubesummary.model.HistoryLengthResult
import io.github.utubesummary.model.SummaryLength
import io.github.utubesummary.model.SummaryType
import io.github.utubesummary.model.VideoSubtype

@Entity(tableName = "history")
@ColumnTypeConverters(
    SummaryLengthConverter::class,
    SummaryTypeConverter::class,
    VideoSubtypeConverter::class,
    HistoryLengthResultsConverter::class,
)
data class HistoryEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val summary: String,
    val author: String = "",
    val createdOn: Long,
    val length: SummaryLength,
    val type: SummaryType,
    val subtype: VideoSubtype? = null,
    val sourceLink: String? = null,
    val sourceText: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val provider: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val model: String? = null,
    @ColumnInfo(defaultValue = "NULL")
    val lengthResults: Map<SummaryLength, HistoryLengthResult>? = null,
)
