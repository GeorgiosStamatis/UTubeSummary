package io.github.utubesummary.model

import java.net.URI

private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

/**
 * The 11-character id of the YouTube video a link points to, or null if it is not a
 * recognizable YouTube video link. Handles `watch?v=`, `youtu.be/`, `shorts/`, `embed/`,
 * `live/` and `v/`, with or without extra parameters such as `si=`.
 */
fun youtubeVideoId(link: String?): String? {
    val raw = link?.trim().orEmpty()
    if (raw.isEmpty()) return null
    val uri = runCatching { URI(if (raw.contains("://")) raw else "https://$raw") }
        .getOrNull() ?: return null
    val host = uri.host?.lowercase() ?: return null
    val segments = uri.path.orEmpty().split('/').filter { it.isNotEmpty() }

    val candidate = when {
        host == "youtu.be" -> segments.firstOrNull()
        host == "youtube.com" || host.endsWith(".youtube.com") -> when (segments.firstOrNull()) {
            "watch" -> uri.rawQuery.orEmpty().split('&')
                .firstOrNull { it.startsWith("v=") }?.removePrefix("v=")

            "shorts", "embed", "live", "v" -> segments.getOrNull(1)
            else -> null
        }

        else -> null
    }
    return candidate?.takeIf { VIDEO_ID.matches(it) }
}
