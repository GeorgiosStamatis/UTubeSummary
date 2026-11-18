package io.github.utubesummary

/** Share types whose content is a file (`EXTRA_STREAM`) instead of text (`EXTRA_TEXT`). */
fun isFileShare(mimeType: String?): Boolean {
    val type = mimeType.orEmpty()
    return type.startsWith("application/") || type.startsWith("image/")
}

/**
 * What a share intent asks to summarize: the file link for documents and images, the text
 * otherwise. The extras are read only when needed, so the other kind is never touched.
 */
fun sharedContentOf(
    mimeType: String?,
    text: () -> String?,
    streamUri: () -> String?,
): String? = if (isFileShare(mimeType)) streamUri() else text()

/**
 * A shared link starts summarizing at once when it was handed over from the instant dialog (or asked
 * to auto-start) but the finished summary is no longer in memory, for example after the app process
 * was restarted in between.
 */
fun shouldAutoTrigger(hasSummary: Boolean, autoTrigger: Boolean, handoffId: String?): Boolean =
    !hasSummary && (autoTrigger || handoffId != null)
