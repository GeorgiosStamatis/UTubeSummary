package io.github.utubesummary.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SecureValueCodecTest {

    /** Reversible fake: "cipher(x)" <-> x. Fails for anything else, like a lost key. */
    private class FakeCipher(var failEncrypt: Boolean = false) : SecretCipher {
        override fun encrypt(plainText: String): String {
            if (failEncrypt) throw SecureStorageException("boom")
            return "cipher($plainText)"
        }

        override fun decrypt(encryptedText: String): String {
            if (!encryptedText.startsWith("cipher(") || !encryptedText.endsWith(")")) {
                throw SecureStorageException("bad blob")
            }
            return encryptedText.removePrefix("cipher(").removeSuffix(")")
        }
    }

    private val cipher = FakeCipher()
    private val codec = SecureValueCodec(cipher)

    @Test
    fun `round trip`() {
        val stored = codec.encode("secret")
        assertTrue(stored.startsWith("enc1:"))
        assertEquals(DecodedSecret.Value("secret"), codec.decode(stored, legacyIsPlain = false))
    }

    @Test
    fun `empty stays empty and is never an error`() {
        assertEquals("", codec.encode(""))
        assertEquals(DecodedSecret.Value(""), codec.decode("", legacyIsPlain = true))
        assertEquals(DecodedSecret.Value(""), codec.decode("", legacyIsPlain = false))
    }

    @Test
    fun `legacy plain text is read and flagged for migration`() {
        assertEquals(
            DecodedSecret.Value("old-cookie", needsMigration = true),
            codec.decode("old-cookie", legacyIsPlain = true)
        )
    }

    @Test
    fun `legacy unprefixed ciphertext is decrypted and flagged for migration`() {
        assertEquals(
            DecodedSecret.Value("old-key", needsMigration = true),
            codec.decode("cipher(old-key)", legacyIsPlain = false)
        )
    }

    @Test
    fun `undecryptable values are reported as unreadable`() {
        assertEquals(DecodedSecret.Unreadable, codec.decode("enc1:garbage", legacyIsPlain = false))
        assertEquals(DecodedSecret.Unreadable, codec.decode("garbage", legacyIsPlain = false))
    }

    @Test
    fun `encryption failure is not swallowed`() {
        cipher.failEncrypt = true
        assertThrows(SecureStorageException::class.java) { codec.encode("secret") }
    }
}
