package io.github.utubesummary.domain.provider

import javax.inject.Qualifier

/** Marks the coroutine dispatcher for blocking work (network, files); tests inject a test dispatcher. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
