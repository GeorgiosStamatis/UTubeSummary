package io.github.utubesummary.domain.provider

import java.util.Locale

interface AppLocaleProvider {
    fun getCurrentLocale(): Locale
}
