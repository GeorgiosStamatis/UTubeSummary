package io.github.utubesummary.domain.importer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import io.github.utubesummary.model.youtubeVideoId

/** The file is not a video summaries backup (not JSON, or not a JSON array). */
class InvalidBackupException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/** One valid entry of a backup, ready to become a history item. */
data class ImportedVideo(
    val videoId: String,
    val title: String,
    val author: String,
    val createdOn: Long,
    val summary: String,
) {
    val sourceLink: String get() = "https://www.youtube.com/watch?v=$videoId"
}

data class ParsedBackup(val videos: List<ImportedVideo>, val invalid: Int)

@Serializable
private data class BackupEntry(
    @SerialName("video_id") val videoId: String? = null,
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("video_title") val title: String? = null,
    @SerialName("video_uploader") val uploader: String? = null,
    @SerialName("update_time") val updateTime: Long? = null,
    @SerialName("video_summary") val summary: String? = null,
)

/**
 * Reads the JSON backup of another video summarizing app: an array of objects with
 * `video_id`, `video_url`, `video_title`, `video_uploader`, `update_time` (Unix seconds) and
 * `video_summary` (a JSON string holding `[{"Chapter": "<text>"}]`). Other keys are ignored.
 */
object VideoSummaryBackupParser {
    private val json = Json { ignoreUnknownKeys = true }

    /** @throws InvalidBackupException if the text is not a JSON array. */
    fun parse(text: String, fallbackTime: Long = System.currentTimeMillis()): ParsedBackup {
        val root = try {
            json.parseToJsonElement(text)
        } catch (e: Exception) {
            throw InvalidBackupException("Not valid JSON", e)
        }
        if (root !is JsonArray) throw InvalidBackupException("Expected a JSON array")

        val videos = mutableListOf<ImportedVideo>()
        var invalid = 0
        for (element in root) {
            val video = runCatching {
                json.decodeFromJsonElement(BackupEntry.serializer(), element)
            }.getOrNull()?.toVideo(fallbackTime)
            if (video != null) videos += video else invalid++
        }
        return ParsedBackup(videos, invalid)
    }

    private fun BackupEntry.toVideo(fallbackTime: Long): ImportedVideo? {
        val id = videoId?.trim()?.takeIf { youtubeVideoId("https://youtu.be/$it") != null }
            ?: youtubeVideoId(videoUrl)
            ?: return null
        val name = title?.trim().orEmpty().ifEmpty { return null }
        val text = unwrapSummary(summary.orEmpty()).ifEmpty { return null }
        return ImportedVideo(
            videoId = id,
            title = name,
            author = uploader?.trim().orEmpty(),
            createdOn = updateTime?.let { it * 1000 } ?: fallbackTime,
            summary = text,
        )
    }

    /** `[{"Chapter": "a"}, {"Chapter": "b"}]` -> "a\n\nb"; plain text is kept as it is. */
    private fun unwrapSummary(raw: String): String {
        val array = runCatching { json.parseToJsonElement(raw) }.getOrNull() as? JsonArray
            ?: return raw.trim()
        return array.mapNotNull { (it as? JsonObject)?.get("Chapter")?.jsonPrimitive?.contentOrNull }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n\n")
    }
}
