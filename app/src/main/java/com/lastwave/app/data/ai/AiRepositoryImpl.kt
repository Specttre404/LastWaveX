package com.lastwave.app.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val settingsRepository: AiSettingsRepository,
    private val credentialStore: AiCredentialStore,
    private val freeGeminiProvider: FreeGeminiProvider,
    private val geminiByokProvider: GeminiByokProvider,
    private val openAiByokProvider: OpenAiByokProvider,
) : AiRepository {

    override val isAvailable: Boolean = true

    override val currentSettings: Flow<AiSettings> = settingsRepository.settings

    override suspend fun updateSettings(transform: (AiSettings) -> AiSettings) {
        settingsRepository.updateSettings(transform)
    }

    override suspend fun generateResponse(prompt: String, context: AiContextInfo?): AiResult {
        val results = mutableListOf<AiResult>()
        generateStreamResponse(prompt, context).collect { results.add(it) }
        return results.lastOrNull { it is AiResult.Success || it is AiResult.Error || it is AiResult.RateLimited || it is AiResult.InvalidKey }
            ?: AiResult.Error("No response received")
    }

    override fun generateStreamResponse(prompt: String, context: AiContextInfo?): Flow<AiResult> = flow {
        val current = settingsRepository.settings.first()
        if (!current.enabled) {
            emit(AiResult.Unavailable("LASTWAVEX AI is currently disabled in Settings."))
            return@flow
        }

        val systemPrompt = AiSystemPrompt.buildSystemPrompt(current.userLevel, context)

        if (current.mode == AiMode.FREE) {
            freeGeminiProvider.generateStream(
                modelName = current.freeModel,
                systemPrompt = systemPrompt,
                userPrompt = prompt,
            ).collect { emit(it) }
        } else {
            // BYOK Mode
            val providerType = current.providerType
            val apiKey = credentialStore.getApiKey(providerType)
            if (apiKey.isBlank()) {
                emit(AiResult.InvalidKey("No API key configured for ${providerType.displayName}. Please enter your key in Settings -> AI."))
                return@flow
            }

            when (providerType) {
                AiProviderType.FREE_FIREBASE_GEMINI -> {
                    freeGeminiProvider.generateStream(
                        modelName = current.freeModel,
                        systemPrompt = systemPrompt,
                        userPrompt = prompt,
                    ).collect { emit(it) }
                }
                AiProviderType.BYOK_GEMINI -> {
                    geminiByokProvider.generateStream(
                        apiKey = apiKey,
                        modelName = current.byokModel,
                        systemPrompt = systemPrompt,
                        userPrompt = prompt,
                        baseUrl = current.providerType.defaultEndpoint,
                    ).collect { emit(it) }
                }
                AiProviderType.BYOK_OPENAI -> {
                    openAiByokProvider.generateStream(
                        apiKey = apiKey,
                        modelName = current.byokModel,
                        systemPrompt = systemPrompt,
                        userPrompt = prompt,
                        endpointUrl = current.providerType.defaultEndpoint,
                    ).collect { emit(it) }
                }
                AiProviderType.BYOK_OPENAI_COMPATIBLE -> {
                    val endpoint = current.customEndpoint.ifBlank { credentialStore.getCustomEndpoint() }.ifBlank { current.providerType.defaultEndpoint }
                    openAiByokProvider.generateStream(
                        apiKey = apiKey,
                        modelName = current.byokModel,
                        systemPrompt = systemPrompt,
                        userPrompt = prompt,
                        endpointUrl = endpoint,
                    ).collect { emit(it) }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun validateKey(
        providerType: AiProviderType,
        apiKey: String,
        customEndpoint: String?,
    ): AiResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext AiResult.InvalidKey("Key cannot be empty")
        val testPrompt = "Ping"
        val testSystemPrompt = "Respond with one word: Pong."

        val results = mutableListOf<AiResult>()
        when (providerType) {
            AiProviderType.FREE_FIREBASE_GEMINI -> return@withContext AiResult.Success("Free mode requires no key")
            AiProviderType.BYOK_GEMINI -> {
                geminiByokProvider.generateStream(
                    apiKey = apiKey,
                    modelName = "gemini-2.5-flash",
                    systemPrompt = testSystemPrompt,
                    userPrompt = testPrompt,
                ).collect { results.add(it) }
            }
            AiProviderType.BYOK_OPENAI -> {
                openAiByokProvider.generateStream(
                    apiKey = apiKey,
                    modelName = "gpt-4o-mini",
                    systemPrompt = testSystemPrompt,
                    userPrompt = testPrompt,
                    endpointUrl = providerType.defaultEndpoint,
                ).collect { results.add(it) }
            }
            AiProviderType.BYOK_OPENAI_COMPATIBLE -> {
                val endpoint = customEndpoint?.ifBlank { providerType.defaultEndpoint } ?: providerType.defaultEndpoint
                openAiByokProvider.generateStream(
                    apiKey = apiKey,
                    modelName = "gpt-4o-mini",
                    systemPrompt = testSystemPrompt,
                    userPrompt = testPrompt,
                    endpointUrl = endpoint,
                ).collect { results.add(it) }
            }
        }

        results.lastOrNull { it is AiResult.Success || it is AiResult.InvalidKey || it is AiResult.Error || it is AiResult.RateLimited }
            ?: AiResult.Error("Key validation produced no response")
    }

    override suspend fun explainSong(title: String, artist: String): AiResult {
        val context = AiContextInfo(
            currentScreen = "Player",
            trackSummary = "'$title' by '$artist'",
        )
        val prompt = "Provide a 2-sentence background overview for the song '$title' by '$artist'."
        return generateResponse(prompt, context)
    }

    override suspend fun explainArtist(artistName: String): AiResult {
        val context = AiContextInfo(
            currentScreen = "Artist Detail",
            trackSummary = "Artist: '$artistName'",
        )
        val prompt = "Provide a 2-sentence background overview for the music artist '$artistName'."
        return generateResponse(prompt, context)
    }

    override suspend fun explainLyrics(title: String, artistName: String, lyricsSnippet: String): AiResult {
        val context = AiContextInfo(
            currentScreen = "Lyrics View",
            trackSummary = "'$title' by '$artistName'",
        )
        val snippet = lyricsSnippet.take(150).replace("\n", " ")
        val prompt = "Briefly explain the theme of these lyrics from '$title' by '$artistName': \"$snippet\"."
        return generateResponse(prompt, context)
    }

    override suspend fun explainSetting(settingTitle: String, currentValue: String): AiResult {
        val context = AiContextInfo(
            currentScreen = "Settings",
            settingsSummary = "Setting: '$settingTitle', Current Value: '$currentValue'",
        )
        val prompt = "Explain the LASTWAVEX setting '$settingTitle' (currently set to '$currentValue') and describe the trade-offs of changing it."
        return generateResponse(prompt, context)
    }

    override suspend fun diagnoseTroubleshooting(userDescription: String, logExcerpt: String?): AiResult {
        val context = AiContextInfo(
            currentScreen = "Troubleshooting",
            logExcerpt = logExcerpt?.take(500),
        )
        val prompt = "Diagnose this issue reported by the user: '$userDescription'. List probable causes and safe troubleshooting steps."
        return generateResponse(prompt, context)
    }

    override suspend fun proposeConfiguration(goalDescription: String): AiResult {
        val context = AiContextInfo(
            currentScreen = "Configuration Assistant",
        )
        val prompt = "Propose an optimal LASTWAVEX configuration for this user goal: '$goalDescription'."
        return generateResponse(prompt, context)
    }
}
