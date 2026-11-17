package io.github.utubesummary.domain.importer

import io.github.utubesummary.model.HistorySummary
import io.github.utubesummary.model.youtubeVideoId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** The text of a backup file and how many videos it holds. */
data class ExportedBackup(val json: String, val count: Int)

private val prettyJson = Json { prettyPrint = true }

/**
 * Writes the YouTube videos of [summaries] in the format [VideoSummaryBackupParser] reads, so an
 * export can be imported again. Only videos fit this format; other entries are left out. The text
 * of each video is its current (newest) summary.
 */
fun buildVideoSummaryBackup(summaries: List<HistorySummary>): ExportedBackup {
    val videos = summaries.mapNotNull { summary ->
        val videoId = youtubeVideoId(summary.sourceLink) ?: return@mapNotNull null
        val text = summary.summary.trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        buildJsonObject {
            put("video_id", videoId)
            put("video_url", "https://www.youtube.com/watch?v=$videoId")
            put("video_title", summary.title)
            put("video_uploader", summary.author)
            put("update_time", summary.createdOn / 1000)
            // The importer expects the summary as a JSON string holding a list of chapters.
            put(
                "video_summary",
                buildJsonArray { add(buildJsonObject { put("Chapter", text) }) }.toString()
            )
        }
    }
    return ExportedBackup(
        json = prettyJson.encodeToString(JsonElement.serializer(), JsonArray(videos)),
        count = videos.size,
    )
}
