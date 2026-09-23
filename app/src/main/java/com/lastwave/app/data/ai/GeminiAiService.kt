package com.lastwave.app.data.ai

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AiEntryPoint {
    fun geminiAiService(): GeminiAiService
}

sealed interface AiResult {
    data class Success(val text: String) : AiResult
    data class Unavailable(val reason: String) : AiResult
    data object RateLimited : AiResult
    data class AppCheckFailure(val details: String) : AiResult
    data object SafetyRefusal : AiResult
    data class Error(val message: String) : AiResult
}

@Singleton
class GeminiAiService @Inject constructor(
    private val aiRepository: AiRepository,
) {
    var userProfile: AiUserProfile
        get() = aiRepository.userProfile
        set(value) {
            aiRepository.userProfile = value
        }

    suspend fun explainSong(title: String, artist: String): AiResult =
        aiRepository.explainSong(title, artist)

    suspend fun explainArtist(artist: String): AiResult =
        aiRepository.explainArtist(artist)

    suspend fun explainLyrics(title: String, artist: String, lyricsSnippet: String): AiResult =
        aiRepository.explainLyrics(title, artist, lyricsSnippet)

    suspend fun interpretSearchPrompt(query: String): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            val clean = query.trim()
            listOf(clean, "$clean acoustic", "$clean live", "$clean mix")
        }.getOrDefault(listOf(query))
    }
}
