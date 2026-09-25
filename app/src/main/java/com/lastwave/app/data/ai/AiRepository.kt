package com.lastwave.app.data.ai

import kotlinx.coroutines.flow.Flow

const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"

interface AiRepository {
    val isAvailable: Boolean
    val currentSettings: Flow<AiSettings>
    suspend fun updateSettings(transform: (AiSettings) -> AiSettings)
    suspend fun generateResponse(prompt: String, context: AiContextInfo? = null): AiResult
    fun generateStreamResponse(prompt: String, context: AiContextInfo? = null): Flow<AiResult>
    suspend fun validateKey(providerType: AiProviderType, apiKey: String, customEndpoint: String? = null): AiResult
    suspend fun explainSong(title: String, artist: String): AiResult
    suspend fun explainArtist(artistName: String): AiResult
    suspend fun explainLyrics(title: String, artistName: String, lyricsSnippet: String): AiResult
    suspend fun explainSetting(settingTitle: String, currentValue: String): AiResult
    suspend fun diagnoseTroubleshooting(userDescription: String, logExcerpt: String? = null): AiResult
    suspend fun proposeConfiguration(goalDescription: String): AiResult
}
