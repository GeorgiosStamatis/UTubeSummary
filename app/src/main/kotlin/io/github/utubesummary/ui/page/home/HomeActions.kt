package io.github.utubesummary.ui.page.home

import io.github.utubesummary.llm.AIProvider
import io.github.utubesummary.model.SummaryLength

object MimeTypes {
    const val PDF = "application/pdf"
    const val DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    const val PNG = "image/png"
    const val JPEG = "image/jpeg"
    const val JPG = "image/jpg"
    const val WEBP = "image/webp"
    const val TEXT = "text/plain"
    const val MARKDOWN = "text/markdown"
    const val MARKDOWN_LEGACY = "text/x-markdown"

    val allSupported = arrayOf(PDF, DOCX, PNG, JPEG, JPG, WEBP, TEXT, MARKDOWN, MARKDOWN_LEGACY)
}

data class HomeActions(
    val onSummarize: () -> Unit,
    val onClearInput: () -> Unit,
    val onUrlChange: (String) -> Unit,
    val onLengthSelect: (SummaryLength) -> Unit,
    val onProfileSelect: (String) -> Unit = {},
    val onLanguageSelect: (String) -> Unit = {},
    val onCancelSummarize: () -> Unit = {},
    val onPasteFromClipboard: () -> Unit,
    val onPlaySummary: () -> Unit,
    val onCopySummary: (String) -> Unit,
    val onShowSnackBar: (String) -> Unit,
    val onLaunchFilePicker: () -> Unit,
    val onLaunchImagePicker: () -> Unit,
    val onLaunchCamera: () -> Unit,
    val onSelectProvider: (AIProvider) -> Unit,
    val onSelectModel: (String) -> Unit,
    val onShowProviderModelSheet: () -> Unit,
    val onDismissProviderModelSheet: () -> Unit,
    val onSelectProviderAndModel: (AIProvider, String) -> Unit = { _, _ -> },
    val onAskQuestion: (String) -> Unit = {},
)
