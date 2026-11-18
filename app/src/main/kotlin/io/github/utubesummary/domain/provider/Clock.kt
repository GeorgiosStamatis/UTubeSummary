package io.github.utubesummary.domain.provider

import java.util.UUID

/** The current time, so code that stamps entries can be tested with a fixed time. */
interface Clock {
    fun nowMillis(): Long
}

/** Creates ids for new entries, so tests can predict them. */
interface IdGenerator {
    fun newId(): String
}

/** The system clock. */
object SystemClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

/** Random UUIDs. */
object RandomIdGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
