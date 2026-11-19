package io.github.utubesummary.model

import java.security.MessageDigest

private const val FINGERPRINT_LENGTH = 16

/**
 * A short fingerprint (the first 16 hex characters of the SHA-256) of an extracted text. It is stored
 * with a summary to tell later whether the source changed; the text itself is never stored.
 */
fun contentFingerprint(text: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
        .take(FINGERPRINT_LENGTH)
