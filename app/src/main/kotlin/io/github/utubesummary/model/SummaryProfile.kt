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

// Ids of the profiles that ship with the app (their prompts are in llm/BuiltInProfiles.kt).
const val TECHNICAL_ANALYSIS_PROFILE_ID = "technical_analysis"
const val CRITICAL_ANALYST_PROFILE_ID = "critical_analyst"
const val CURIOUS_LEARNER_PROFILE_ID = "curious_learner"
const val TECHNICAL_CONTENT_EVALUATOR_PROFILE_ID = "technical_content_evaluator"
const val QUICK_BRIEF_PROFILE_ID = "quick_brief"
