package io.github.utubesummary.model

/**
 * A named base prompt that shapes how content is summarized.
 *
 * @property id Stable identifier. Built-in profiles use fixed ids (see [DEFAULT_PROFILE_ID]).
 * @property name Display name. Null for built-in profiles that have not been renamed, in which case
 * the UI shows the localized built-in name.
 * @property basePrompt Base prompt, may contain `[Language instructions]` and `[Length instructions]`.
 * @property isBuiltIn True for shipped presets (they can be edited and reset, but not deleted).
 * @property isModified True for a built-in preset the user has edited.
 */
data class SummaryProfile(
    val id: String,
    val name: String?,
    val basePrompt: String,
    val isBuiltIn: Boolean,
    val isModified: Boolean = false,
    /** Ask for overview, key points and tags (the structured card) instead of free-form text. */
    val structuredOutput: Boolean = false,
)

const val DEFAULT_PROFILE_ID = "default"
