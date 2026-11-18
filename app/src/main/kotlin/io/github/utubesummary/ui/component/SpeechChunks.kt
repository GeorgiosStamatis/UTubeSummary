package io.github.utubesummary.ui.component

/** A piece of text for the speech engine and where it starts in the whole text. */
data class SpeechChunk(val start: Int, val text: String)

/**
 * Splits [text] from [startOffset] into pieces of at most [maxLength] characters, because speech
 * engines refuse longer input. A piece ends after a space when there is one inside the limit, so
 * words are not cut; a piece without any space is cut at the limit.
 */
fun speechChunks(text: String, startOffset: Int, maxLength: Int): List<SpeechChunk> {
    require(maxLength > 0) { "maxLength must be positive" }
    val chunks = mutableListOf<SpeechChunk>()
    var start = startOffset.coerceIn(0, text.length)
    while (start < text.length) {
        var end = minOf(start + maxLength, text.length)
        if (end < text.length) {
            val breakAt = text.lastIndexOf(' ', end - 1)
            if (breakAt > start) end = breakAt + 1
        }
        chunks += SpeechChunk(start, text.substring(start, end))
        start = end
    }
    return chunks
}
