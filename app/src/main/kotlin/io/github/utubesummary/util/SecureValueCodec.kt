package io.github.utubesummary.util

/** The encryption/decryption primitive behind [SecureValueCodec]. Both throw on failure. */
interface SecretCipher {
    /** @throws SecureStorageException if the value cannot be encrypted. */
    fun encrypt(plainText: String): String

    /** @throws SecureStorageException if the value cannot be decrypted. */
    fun decrypt(encryptedText: String): String
}

/** Secure storage is unusable (e.g. the Keystore key was lost or cannot be created). */
class SecureStorageException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

sealed interface DecodedSecret {
    /**
     * A readable value. [needsMigration] is true for values stored in an older format
     * (plain text or unprefixed), which should be saved again with [SecureValueCodec.encode].
     */
    data class Value(val text: String, val needsMigration: Boolean = false) : DecodedSecret

    /** The stored value exists but cannot be decrypted any more. */
    data object Unreadable : DecodedSecret
}

/**
 * Stored secrets look like `enc1:<ciphertext>`. Older builds stored API keys as a bare
 * ciphertext and the BiliBili cookie as plain text; both are still read and flagged for
 * migration.
 */
class SecureValueCodec(private val cipher: SecretCipher) {

    /** Empty stays empty; anything else is encrypted. May throw [SecureStorageException]. */
    fun encode(plain: String): String =
        if (plain.isEmpty()) "" else PREFIX + cipher.encrypt(plain)

    /**
     * @param legacyIsPlain how to treat a value without the prefix: `true` = plain text
     * (BiliBili cookie), `false` = bare ciphertext (API keys from older builds).
     */
    fun decode(stored: String, legacyIsPlain: Boolean): DecodedSecret {
        if (stored.isEmpty()) return DecodedSecret.Value("")
        if (stored.startsWith(PREFIX)) {
            return decrypt(stored.removePrefix(PREFIX), needsMigration = false)
        }
        return if (legacyIsPlain) {
            DecodedSecret.Value(stored, needsMigration = true)
        } else {
            decrypt(stored, needsMigration = true)
        }
    }

    private fun decrypt(ciphertext: String, needsMigration: Boolean): DecodedSecret =
        try {
            DecodedSecret.Value(cipher.decrypt(ciphertext), needsMigration)
        } catch (e: SecureStorageException) {
            DecodedSecret.Unreadable
        }

    private companion object {
        const val PREFIX = "enc1:"
    }
}
