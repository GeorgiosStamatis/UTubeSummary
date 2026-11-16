package io.github.utubesummary.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** AES-256-GCM with a non-exportable Android Keystore key. Never logs secrets. */
object SecurityUtil : SecretCipher {
    private const val TAG = "SecurityUtil"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "utubesummary_api_key_alias"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128

    private var cachedKey: SecretKey? = null

    /** Encodes/decodes secrets for storage; see [SecureValueCodec]. */
    val codec = SecureValueCodec(this)

    @Synchronized
    private fun getSecretKey(): SecretKey {
        cachedKey?.let { return it }

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }

        return (keyStore.getKey(KEY_ALIAS, null) as SecretKey).also { cachedKey = it }
    }

    /** Deletes an unusable key so a new one is created on next use. */
    @Synchronized
    private fun resetKey() {
        cachedKey = null
        runCatching {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(KEY_ALIAS)
        }
    }

    override fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            encryptOnce(plainText)
        } catch (e: Exception) {
            if (e is KeyPermanentlyInvalidatedException ||
                e is UnrecoverableKeyException ||
                e is InvalidKeyException
            ) {
                // The key can no longer be used: start over with a new one, once.
                Log.w(TAG, "Keystore key unusable, creating a new one", e)
                resetKey()
                try {
                    encryptOnce(plainText)
                } catch (retryError: Exception) {
                    throw SecureStorageException("Encryption failed", retryError)
                }
            } else {
                throw SecureStorageException("Encryption failed", e)
            }
        }
    }

    private fun encryptOnce(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    override fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val combined = Base64.decode(encryptedText, Base64.DEFAULT)
            if (combined.size <= IV_SIZE) throw IllegalArgumentException("Value too short")
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = combined.copyOfRange(0, IV_SIZE)
            val encryptedBytes = combined.copyOfRange(IV_SIZE, combined.size)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), GCMParameterSpec(TAG_SIZE, iv))
            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            throw SecureStorageException("Decryption failed", e)
        }
    }
}
