package io.github.utubesummary.util

/**
 * Decodes the bytes of a text file. A byte order mark selects UTF-8, UTF-16 LE or UTF-16 BE and is
 * removed; without one the text is read as UTF-8. With [normalizeLineEndings] Windows (`\r\n`) and
 * old Mac (`\r`) line endings become `\n`.
 */
fun decodeTextBytes(bytes: ByteArray, normalizeLineEndings: Boolean = false): String {
    val text = when {
        bytes.startsWith(0xEF, 0xBB, 0xBF) -> String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        bytes.startsWith(0xFF, 0xFE) -> String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        bytes.startsWith(0xFE, 0xFF) -> String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        else -> String(bytes, Charsets.UTF_8)
    }
    return if (normalizeLineEndings) text.replace("\r\n", "\n").replace('\r', '\n') else text
}

private fun ByteArray.startsWith(vararg prefix: Int): Boolean =
    size >= prefix.size && prefix.indices.all { (this[it].toInt() and 0xFF) == prefix[it] }
