package com.lastwave.app.data.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FreeGeminiProvider @Inject constructor() {

    fun generateStream(
        modelName: String,
        systemPrompt: String,
        userPrompt: String,
    ): Flow<AiResult> = flow {
        runCatching {
            val generativeModel = Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel(modelName = modelName.ifBlank { "gemini-2.5-flash" })

            val fullPrompt = "$systemPrompt\n\nUser Question:\n$userPrompt"
            val responseFlow = generativeModel.generateContentStream(fullPrompt)
            val fullText = StringBuilder()

            responseFlow.collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    fullText.append(chunkText)
                    emit(AiResult.Streaming(chunk = chunkText, fullTextSoFar = fullText.toString()))
                }
            }

            if (fullText.isNotEmpty()) {
                emit(AiResult.Success(fullText.toString()))
            } else {
                emit(AiResult.Error("Free AI provider returned an empty response."))
            }
        }.getOrElse { e ->
            val msg = e.localizedMessage ?: "Free AI Provider Error"
            when {
                msg.contains("429", ignoreCase = true) || msg.contains("quota", ignoreCase = true) || msg.contains("resource_exhausted", ignoreCase = true) -> {
                    emit(AiResult.RateLimited)
                }
                msg.contains("AppCheck", ignoreCase = true) -> {
                    emit(AiResult.AppCheckFailure(msg))
                }
                msg.contains("safety", ignoreCase = true) || msg.contains("blocked", ignoreCase = true) -> {
                    emit(AiResult.SafetyRefusal)
                }
                else -> {
                    emit(AiResult.Error(msg))
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
