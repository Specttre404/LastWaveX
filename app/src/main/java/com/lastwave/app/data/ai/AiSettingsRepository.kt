package com.lastwave.app.data.ai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "lastwavex_ai_settings")

@Singleton
class AiSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_ENABLED = booleanPreferencesKey("ai_enabled")
        private val KEY_MODE = stringPreferencesKey("ai_mode")
        private val KEY_PROVIDER_TYPE = stringPreferencesKey("ai_provider_type")
        private val KEY_USER_LEVEL = stringPreferencesKey("ai_user_level")
        private val KEY_FREE_MODEL = stringPreferencesKey("ai_free_model")
        private val KEY_BYOK_MODEL = stringPreferencesKey("ai_byok_model")
        private val KEY_CUSTOM_ENDPOINT = stringPreferencesKey("ai_custom_endpoint")
        private val KEY_CHAT_HISTORY = stringPreferencesKey("ai_chat_history_json")
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val settings: Flow<AiSettings> = context.aiSettingsDataStore.data.map { prefs ->
        AiSettings(
            enabled = prefs[KEY_ENABLED] ?: true,
            mode = runCatching { AiMode.valueOf(prefs[KEY_MODE] ?: AiMode.FREE.name) }.getOrDefault(AiMode.FREE),
            providerType = runCatching {
                AiProviderType.valueOf(prefs[KEY_PROVIDER_TYPE] ?: AiProviderType.FREE_FIREBASE_GEMINI.name)
            }.getOrDefault(AiProviderType.FREE_FIREBASE_GEMINI),
            userLevel = AiUserProfile.fromName(prefs[KEY_USER_LEVEL]),
            freeModel = prefs[KEY_FREE_MODEL] ?: "gemini-2.5-flash",
            byokModel = prefs[KEY_BYOK_MODEL] ?: "gemini-2.5-flash",
            customEndpoint = prefs[KEY_CUSTOM_ENDPOINT] ?: "",
        )
    }

    suspend fun updateSettings(transform: (AiSettings) -> AiSettings) {
        context.aiSettingsDataStore.edit { prefs ->
            val current = AiSettings(
                enabled = prefs[KEY_ENABLED] ?: true,
                mode = runCatching { AiMode.valueOf(prefs[KEY_MODE] ?: AiMode.FREE.name) }.getOrDefault(AiMode.FREE),
                providerType = runCatching {
                    AiProviderType.valueOf(prefs[KEY_PROVIDER_TYPE] ?: AiProviderType.FREE_FIREBASE_GEMINI.name)
                }.getOrDefault(AiProviderType.FREE_FIREBASE_GEMINI),
                userLevel = AiUserProfile.fromName(prefs[KEY_USER_LEVEL]),
                freeModel = prefs[KEY_FREE_MODEL] ?: "gemini-2.5-flash",
                byokModel = prefs[KEY_BYOK_MODEL] ?: "gemini-2.5-flash",
                customEndpoint = prefs[KEY_CUSTOM_ENDPOINT] ?: "",
            )
            val updated = transform(current)
            prefs[KEY_ENABLED] = updated.enabled
            prefs[KEY_MODE] = updated.mode.name
            prefs[KEY_PROVIDER_TYPE] = updated.providerType.name
            prefs[KEY_USER_LEVEL] = updated.userLevel.name
            prefs[KEY_FREE_MODEL] = updated.freeModel
            prefs[KEY_BYOK_MODEL] = updated.byokModel
            prefs[KEY_CUSTOM_ENDPOINT] = updated.customEndpoint
        }
    }

    val chatHistory: Flow<List<AiChatMessage>> = context.aiSettingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_CHAT_HISTORY] ?: ""
        if (raw.isBlank()) emptyList()
        else runCatching { json.decodeFromString<List<AiChatMessage>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun saveChatHistory(history: List<AiChatMessage>) {
        val sanitized = history.takeLast(30) // Cap history to last 30 messages
        val serialized = json.encodeToString(sanitized)
        context.aiSettingsDataStore.edit { prefs ->
            prefs[KEY_CHAT_HISTORY] = serialized
        }
    }

    suspend fun clearChatHistory() {
        context.aiSettingsDataStore.edit { prefs ->
            prefs.remove(KEY_CHAT_HISTORY)
        }
    }
}
