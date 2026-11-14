package io.github.utubesummary.data.converters

import androidx.room3.ColumnTypeConverter
import io.github.utubesummary.model.SummaryLength

class SummaryLengthConverter {
    @ColumnTypeConverter
    fun fromSummaryLength(value: SummaryLength): String {
        return value.name
    }

    @ColumnTypeConverter
    fun toSummaryLength(value: String): SummaryLength {
        return SummaryLength.valueOf(value)
    }
}