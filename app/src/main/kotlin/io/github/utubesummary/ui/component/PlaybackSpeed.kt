package io.github.utubesummary.ui.component

/** Speech rates the user can cycle through; [rate] goes to the speech engine. */
enum class PlaybackSpeed(val rate: Float, val label: String) {
    HALF(0.5f, "0.5x"),
    NORMAL(1.0f, "1x"),
    FAST(1.25f, "1.25x"),
    FASTER(1.5f, "1.5x"),
    FASTEST(1.75f, "1.75x"),
    DOUBLE(2.0f, "2x");

    fun next(): PlaybackSpeed {
        val values = entries.toTypedArray()
        val nextOrdinal = (ordinal + 1) % values.size
        return values[nextOrdinal]
    }
}
