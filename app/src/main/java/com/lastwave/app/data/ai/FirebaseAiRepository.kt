package com.lastwave.app.data.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.ai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAiRepository @Inject constructor() : AiRepository {

    override var userProfile: AiUserProfile = AiUserProfile.CASUAL

    private val generativeModel by lazy {
        runCatching {
            Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel(DEFAULT_GEMINI_MODEL)
        }.getOrNull()
    }

    override val isAvailable: Boolean
        get() = generativeModel != null

    override suspend fun generateResponse(prompt: String): AiResult = withContext(Dispatchers.IO) {
        val modelInstance = generativeModel ?: return@withContext AiResult.Unavailable("Firebase AI Logic not initialized")
        runCatching {
            val response = modelInstance.generateContent(prompt)
            val text = response.text
            if (!text.isNullOrBlank()) {
                AiResult.Success(text)
            } else {
                AiResult.Error("Empty response from Gemini model")
            }
        }.getOrElse { e ->
            val msg = e.localizedMessage ?: "AI Service Error"
            when {
                msg.contains("AppCheck", ignoreCase = true) -> AiResult.AppCheckFailure(msg)
                msg.contains("429", ignoreCase = true) || msg.contains("quota", ignoreCase = true) -> AiResult.RateLimited
                msg.contains("safety", ignoreCase = true) || msg.contains("blocked", ignoreCase = true) -> AiResult.SafetyRefusal
                else -> AiResult.Error(msg)
            }
        }
    }

    override suspend fun explainSong(title: String, artist: String): AiResult {
        if (!isAvailable) {
            return AiResult.Unavailable("AI assistance is currently unavailable")
        }
        val prompt = "Provide a 2-sentence musical overview for the song '$title' by '$artist'."
        return generateResponse(prompt)
    }

    override suspend fun explainArtist(artistName: String): AiResult {
        if (!isAvailable) {
            return AiResult.Unavailable("AI assistance is currently unavailable")
        }
        val prompt = "Provide a 2-sentence background for the music artist '$artistName'."
        return generateResponse(prompt)
    }

    override suspend fun explainLyrics(title: String, artistName: String, lyricsSnippet: String): AiResult {
        if (!isAvailable) {
            return AiResult.Unavailable("AI assistance is currently unavailable")
        }
        val snippet = lyricsSnippet.take(150).replace("\n", " ")
        val prompt = "Briefly summarize the theme of these lyrics from '$title' by '$artistName': \"$snippet\"."
        return generateResponse(prompt)
    }
}
