package com.lastwave.app.data.ai

const val DEFAULT_GEMINI_MODEL = "gemini-3.5-flash"

enum class AiUserProfile {
    BEGINNER,
    CASUAL,
    ADVANCED,
    POWER_USER,
    AUDIOPHILE,
}

interface AiRepository {
    val isAvailable: Boolean
    var userProfile: AiUserProfile
    suspend fun generateResponse(prompt: String): AiResult
    suspend fun explainSong(title: String, artist: String): AiResult
    suspend fun explainArtist(artistName: String): AiResult
    suspend fun explainLyrics(title: String, artistName: String, lyricsSnippet: String): AiResult
}
