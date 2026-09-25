package com.lastwave.app.data.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android Keystore-backed encrypted credential store for BYOK (Bring Your Own Key) secrets.
 *
 * Plaintext keys are NEVER written to disk, DataStore, SharedPreferences, database,
 * log files, crash reports, or exported settings.
 *
 * Encryption algorithm: AES-256-GCM using keys stored securely in the hardware-backed
 * [AndroidKeyStore].
 */
@Singleton
class AiCredentialStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "lastwavex_ai_encrypted_creds"
        private const val KEY_ALIAS = "lastwavex_ai_keystore_alias"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128

        private const val KEY_PREFIX_API_KEY = "byok_key_"
        private const val KEY_CUSTOM_ENDPOINT = "byok_custom_endpoint"
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Store IV length (1 byte) + IV + encrypted bytes
        val combined = ByteArray(1 + iv.size + encryptedBytes.size)
        combined[0] = iv.size.toByte()
        System.arraycopy(iv, 0, combined, 1, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, 1 + iv.size, encryptedBytes.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        return runCatching {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.isEmpty()) return ""
            val ivSize = combined[0].toInt()
            val iv = ByteArray(ivSize)
            System.arraycopy(combined, 1, iv, 0, ivSize)

            val encryptedSize = combined.size - 1 - ivSize
            val encryptedBytes = ByteArray(encryptedSize)
            System.arraycopy(combined, 1 + ivSize, encryptedBytes, 0, encryptedSize)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        }.getOrDefault("")
    }

    fun saveApiKey(providerType: AiProviderType, apiKey: String) {
        val storageKey = KEY_PREFIX_API_KEY + providerType.name
        val encrypted = encrypt(apiKey.trim())
        prefs.edit().putString(storageKey, encrypted).apply()
    }

    fun getApiKey(providerType: AiProviderType): String {
        val storageKey = KEY_PREFIX_API_KEY + providerType.name
        val encrypted = prefs.getString(storageKey, null) ?: return ""
        return decrypt(encrypted)
    }

    fun clearApiKey(providerType: AiProviderType) {
        val storageKey = KEY_PREFIX_API_KEY + providerType.name
        prefs.edit().remove(storageKey).apply()
    }

    fun hasApiKey(providerType: AiProviderType): Boolean {
        return getApiKey(providerType).isNotBlank()
    }

    fun saveCustomEndpoint(endpoint: String) {
        val encrypted = encrypt(endpoint.trim())
        prefs.edit().putString(KEY_CUSTOM_ENDPOINT, encrypted).apply()
    }

    fun getCustomEndpoint(): String {
        val encrypted = prefs.getString(KEY_CUSTOM_ENDPOINT, null) ?: return ""
        return decrypt(encrypted)
    }

    fun clearCustomEndpoint() {
        prefs.edit().remove(KEY_CUSTOM_ENDPOINT).apply()
    }

    fun clearAllCredentials() {
        prefs.edit().clear().apply()
        runCatching {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
        }
    }
}
