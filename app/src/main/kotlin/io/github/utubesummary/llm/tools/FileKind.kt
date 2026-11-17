package io.github.utubesummary.llm.tools

/** The kinds of files the app can read text from. */
internal enum class FileKind { PDF, DOCX, IMAGE, TEXT }

private val textExtensions = listOf(".txt", ".md", ".markdown", ".text")
private val textMimeTypes = setOf("text/plain", "text/markdown", "text/x-markdown")
private val imageExtensions = listOf(".jpg", ".jpeg", ".png")

/** Decides how a picked file is read, from its name first and its MIME type second. */
internal fun fileKindOf(filename: String, mimeType: String?): FileKind? = when {
    filename.endsWith(".pdf", ignoreCase = true) -> FileKind.PDF
    filename.endsWith(".docx", ignoreCase = true) -> FileKind.DOCX
    mimeType?.startsWith("image/") == true ||
            imageExtensions.any { filename.endsWith(it, ignoreCase = true) } -> FileKind.IMAGE

    textExtensions.any { filename.endsWith(it, ignoreCase = true) } ||
            mimeType?.lowercase() in textMimeTypes -> FileKind.TEXT

    else -> null
}
