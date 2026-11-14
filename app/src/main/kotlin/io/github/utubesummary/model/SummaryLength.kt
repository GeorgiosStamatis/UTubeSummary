package io.github.utubesummary.model

import kotlinx.serialization.Serializable

@Serializable
enum class SummaryLength {
    SHORT,
    MEDIUM,
    LONG,
    NONE;

    companion object {
        val userSelectable: List<SummaryLength> = listOf(SHORT, MEDIUM, LONG)
    }
}

