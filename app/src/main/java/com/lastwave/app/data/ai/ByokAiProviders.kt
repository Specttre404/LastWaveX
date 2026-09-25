package com.lastwave.app.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiByokProvider @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun generateStream(
        apiKey: String,
        modelName: String,
        systemPrompt: String,
        userPrompt: String,
        baseUrl: String = "https://generativelanguage.googleapis.com",
    ): Flow<AiResult> = flow {
        if (apiKey.isBlank()) {
            emit(AiResult.InvalidKey("Gemini API Key is empty."))
            return@flow
        }

        val effectiveModel = modelName.ifBlank { "gemini-2.5-flash" }
        val cleanBaseUrl = baseUrl.ifBlank { "https://generativelanguage.googleapis.com" }.trimEnd('/')
        val url = "$cleanBaseUrl/v1beta/models/$effectiveModel:streamGenerateContent?key=$apiKey&alt=sse"

        val payload = """
            {
              "systemInstruction": {
                "parts": [{"text": ${json.encodeToString(kotlinx.serialization.serializer(), systemPrompt)}}]
              },
              "contents": [
                {"role": "user", "parts": [{"text": ${json.encodeToString(kotlinx.serialization.serializer(), userPrompt)}}]}
              ]
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        runCatching {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val code = response.code
                val errBody = response.body?.string().orEmpty()
                when (code) {
                    401, 403 -> emit(AiResult.InvalidKey("Invalid Gemini API key ($code)"))
                    429 -> emit(AiResult.RateLimited)
                    else -> emit(AiResult.Error("Gemini BYOK Error ($code): $errBody"))
                }
                return@flow
            }

            val inputStream = response.body?.byteStream() ?: run {
                emit(AiResult.Error("No response body received from Gemini"))
                return@flow
            }

            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val fullText = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line?.trim().orEmpty()
                if (currentLine.startsWith("data:")) {
                    val dataJson = currentLine.removePrefix("data:").trim()
                    if (dataJson.isEmpty()) continue
                    runCatching {
                        val root = json.parseToJsonElement(dataJson).jsonObject
                        val candidates = root["candidates"]?.jsonArray
                        val textChunk = candidates?.firstOrNull()?.jsonObject
                            ?.get("content")?.jsonObject
                            ?.get("parts")?.jsonArray?.firstOrNull()?.jsonObject
                            ?.get("text")?.jsonPrimitive?.content.orEmpty()

                        if (textChunk.isNotEmpty()) {
                            fullText.append(textChunk)
                            emit(AiResult.Streaming(chunk = textChunk, fullTextSoFar = fullText.toString()))
                        }
                    }
                }
            }

            if (fullText.isNotEmpty()) {
                emit(AiResult.Success(fullText.toString()))
            } else {
                emit(AiResult.Error("Empty response from Gemini BYOK provider"))
            }
        }.getOrElse { e ->
            emit(AiResult.Error(e.localizedMessage ?: "Network error connecting to Gemini BYOK"))
        }
    }.flowOn(Dispatchers.IO)
}

@Singleton
class OpenAiByokProvider @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun generateStream(
        apiKey: String,
        modelName: String,
        systemPrompt: String,
        userPrompt: String,
        endpointUrl: String = "https://api.openai.com/v1",
    ): Flow<AiResult> = flow {
        if (apiKey.isBlank()) {
            emit(AiResult.InvalidKey("API Key is empty."))
            return@flow
        }

        val effectiveEndpoint = endpointUrl.ifBlank { "https://api.openai.com/v1" }.trimEnd('/')
        val url = if (effectiveEndpoint.endsWith("/chat/completions")) effectiveEndpoint else "$effectiveEndpoint/chat/completions"
        val effectiveModel = modelName.ifBlank { "gpt-4o-mini" }

        val payload = """
            {
              "model": ${json.encodeToString(kotlinx.serialization.serializer(), effectiveModel)},
              "stream": true,
              "messages": [
                {"role": "system", "content": ${json.encodeToString(kotlinx.serialization.serializer(), systemPrompt)}},
                {"role": "user", "content": ${json.encodeToString(kotlinx.serialization.serializer(), userPrompt)}}
              ]
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        runCatching {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val code = response.code
                val errBody = response.body?.string().orEmpty()
                when (code) {
                    401, 403 -> emit(AiResult.InvalidKey("Invalid API Key / Unauthorized ($code)"))
                    429 -> emit(AiResult.RateLimited)
                    else -> emit(AiResult.Error("OpenAI/Compatible BYOK Error ($code): $errBody"))
                }
                return@flow
            }

            val inputStream = response.body?.byteStream() ?: run {
                emit(AiResult.Error("No response body received from provider"))
                return@flow
            }

            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val fullText = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line?.trim().orEmpty()
                if (currentLine.startsWith("data:")) {
                    val dataContent = currentLine.removePrefix("data:").trim()
                    if (dataContent == "[DONE]") break
                    if (dataContent.isEmpty()) continue
                    runCatching {
                        val root = json.parseToJsonElement(dataContent).jsonObject
                        val choices = root["choices"]?.jsonArray
                        val deltaContent = choices?.firstOrNull()?.jsonObject
                            ?.get("delta")?.jsonObject
                            ?.get("content")?.jsonPrimitive?.content.orEmpty()

                        if (deltaContent.isNotEmpty()) {
                            fullText.append(deltaContent)
                            emit(AiResult.Streaming(chunk = deltaContent, fullTextSoFar = fullText.toString()))
                        }
                    }
                }
            }

            if (fullText.isNotEmpty()) {
                emit(AiResult.Success(fullText.toString()))
            } else {
                emit(AiResult.Error("Empty response received from AI provider"))
            }
        }.getOrElse { e ->
            emit(AiResult.Error(e.localizedMessage ?: "Network error connecting to provider"))
        }
    }.flowOn(Dispatchers.IO)
}
