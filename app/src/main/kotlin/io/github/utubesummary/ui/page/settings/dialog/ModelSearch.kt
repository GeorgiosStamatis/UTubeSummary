package io.github.utubesummary.ui.page.settings.dialog

/** The models whose name contains [query] (ignoring case and surrounding spaces), in their order. */
fun filterModels(models: List<String>, query: String): List<String> {
    val needle = query.trim()
    if (needle.isEmpty()) return models
    return models.filter { it.contains(needle, ignoreCase = true) }
}

/**
 * The name the user typed when it can be used as a model of its own: not blank and not exactly one
 * of [models]. It lets a model the provider does not list (a new one, an alias) still be chosen.
 */
fun typedModelToUse(models: List<String>, query: String): String? =
    query.trim().takeIf { it.isNotEmpty() && it !in models }
