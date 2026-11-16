package io.github.utubesummary.model

/**
 * Address of a small (320x180, 16:9, roughly 10 KB) thumbnail of the YouTube video that [link]
 * points to, or null if it is not a YouTube video link. Nothing is stored: it is derived from
 * the video id, so older and imported history items work the same way.
 */
fun youtubeThumbnailUrl(link: String?): String? =
    youtubeVideoId(link)?.let { "https://i.ytimg.com/vi/$it/mqdefault.jpg" }
