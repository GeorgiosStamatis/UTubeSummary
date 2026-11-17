package io.github.utubesummary.model

import java.util.Locale

/** The summary is written in the language of the video / content. */
const val SUMMARY_LANGUAGE_CONTENT = "content"

/** The summary is written in the language of the app. */
const val SUMMARY_LANGUAGE_APP = "app"

/** Languages offered next to "Video language" and "App language" (BCP-47 language tags). */
val summaryLanguageTags: List<String> = listOf(
    "en", "fr", "de", "es", "it", "pt", "nl", "el", "ru", "uk",
    "pl", "tr", "ar", "hi", "zh", "ja", "ko", "cs", "sv",
)

/**
 * The language setting in effect: the stored choice, or, before the user has picked one, the
 * previous "Original Language" switch.
 */
fun effectiveSummaryLanguage(prefs: UserPreferences): String = when {
    prefs.summaryLanguage.isNotBlank() -> prefs.summaryLanguage
    prefs.useOriginalLanguage -> SUMMARY_LANGUAGE_CONTENT
    else -> SUMMARY_LANGUAGE_APP
}

/**
 * The locale to name in the prompt, or null when the summary should follow the content's own
 * language. [appLocale] is used for [SUMMARY_LANGUAGE_APP].
 */
fun summaryPromptLocale(language: String, appLocale: Locale): Locale? = when (language) {
    SUMMARY_LANGUAGE_CONTENT -> null
    SUMMARY_LANGUAGE_APP -> appLocale
    else -> Locale.forLanguageTag(language)
}
