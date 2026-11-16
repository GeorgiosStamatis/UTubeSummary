package io.github.utubesummary.data.converters

import androidx.room3.ColumnTypeConverter
import io.github.utubesummary.model.SummaryType

class SummaryTypeConverter {
    @ColumnTypeConverter
    fun fromSummaryType(value: SummaryType): String {
        return value.name
    }

    @ColumnTypeConverter
    fun toSummaryType(value: String): SummaryType {
        return SummaryType.valueOf(value)
    }
}