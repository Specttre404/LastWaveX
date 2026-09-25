package com.lastwave.app.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastwave.app.data.ai.AiChatMessage
import com.lastwave.app.data.ai.AiContextInfo
import com.lastwave.app.data.ai.AiRepository
import com.lastwave.app.data.ai.AiResult
import com.lastwave.app.data.ai.AiSettingsRepository
import com.lastwave.app.data.ai.ProposedConfig
import com.lastwave.app.data.local.SettingsPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

data class LastWaveAiUiState(
    val messages: List<AiChatMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val streamingText: String = "",
    val error: String? = null,
    val proposedConfig: ProposedConfig? = null,
)

@HiltViewModel
class LastWaveAiViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val aiSettingsRepository: AiSettingsRepository,
    private val settingsPreferences: SettingsPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LastWaveAiUiState())
    val uiState: StateFlow<LastWaveAiUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true }

    init {
        viewModelScope.launch {
            aiSettingsRepository.chatHistory.collect { history ->
                _uiState.update { it.copy(messages = history) }
            }
        }
    }

    fun sendMessage(userText: String, contextInfo: AiContextInfo? = null) {
        val clean = userText.trim()
        if (clean.isBlank()) return

        val userMessage = AiChatMessage(role = "user", content = clean)
        val updatedHistory = _uiState.value.messages + userMessage
        _uiState.update { it.copy(messages = updatedHistory, isStreaming = true, streamingText = "", error = null, proposedConfig = null) }

        viewModelScope.launch {
            aiSettingsRepository.saveChatHistory(updatedHistory)
        }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            aiRepository.generateStreamResponse(clean, contextInfo).collect { result ->
                when (result) {
                    is AiResult.Streaming -> {
                        _uiState.update { it.copy(streamingText = result.fullTextSoFar) }
                    }
                    is AiResult.Success -> {
                        val assistantText = result.text
                        val (cleanText, config) = parseProposedConfig(assistantText)
                        val assistantMessage = AiChatMessage(role = "assistant", content = cleanText)
                        val finalHistory = _uiState.value.messages + assistantMessage

                        _uiState.update {
                            it.copy(
                                messages = finalHistory,
                                isStreaming = false,
                                streamingText = "",
                                proposedConfig = config,
                            )
                        }
                        aiSettingsRepository.saveChatHistory(finalHistory)
                    }
                    is AiResult.RateLimited -> {
                        _uiState.update {
                            it.copy(
                                isStreaming = false,
                                streamingText = "",
                                error = "Free AI quota limit reached for this period. You can try again later or switch to BYOK mode in Settings -> AI using your own API key.",
                            )
                        }
                    }
                    is AiResult.InvalidKey -> {
                        _uiState.update {
                            it.copy(
                                isStreaming = false,
                                streamingText = "",
                                error = "API Key Error: ${result.providerDetails}. Please check your key in Settings -> AI.",
                            )
                        }
                    }
                    is AiResult.Unavailable -> {
                        _uiState.update { it.copy(isStreaming = false, streamingText = "", error = result.reason) }
                    }
                    is AiResult.Error -> {
                        _uiState.update { it.copy(isStreaming = false, streamingText = "", error = result.message) }
                    }
                    else -> {
                        _uiState.update { it.copy(isStreaming = false, streamingText = "", error = "AI assistance unavailable") }
                    }
                }
            }
        }
    }

    private fun parseProposedConfig(text: String): Pair<String, ProposedConfig?> {
        val jsonStartIndex = text.indexOf("```json")
        val jsonEndIndex = text.lastIndexOf("```")
        if (jsonStartIndex != -1 && jsonEndIndex > jsonStartIndex) {
            val jsonBlock = text.substring(jsonStartIndex + 7, jsonEndIndex).trim()
            val cleanText = text.substring(0, jsonStartIndex).trim()
            runCatching {
                val root = json.parseToJsonElement(jsonBlock).jsonObject
                val proposedObj = root["proposed_config"]?.jsonObject ?: return@runCatching null
                val config = ProposedConfig(
                    settingKey = proposedObj["settingKey"]?.jsonPrimitive?.content.orEmpty(),
                    title = proposedObj["title"]?.jsonPrimitive?.content.orEmpty(),
                    currentValue = proposedObj["currentValue"]?.jsonPrimitive?.content.orEmpty(),
                    proposedValue = proposedObj["proposedValue"]?.jsonPrimitive?.content.orEmpty(),
                    reason = proposedObj["reason"]?.jsonPrimitive?.content.orEmpty(),
                )
                return cleanText to config
            }
        }
        return text to null
    }

    fun applyProposedConfig(config: ProposedConfig) {
        viewModelScope.launch {
            when (config.settingKey) {
                "losslessQuality" -> {
                    val quality = when {
                        config.proposedValue.contains("192") -> 27
                        config.proposedValue.contains("96") -> 7
                        config.proposedValue.contains("44.1") || config.proposedValue.contains("CD") -> 6
                        else -> 5
                    }
                    settingsPreferences.setLosslessQuality(quality)
                }
                "downloadQuality" -> {
                    val quality = when {
                        config.proposedValue.contains("192") -> 27
                        config.proposedValue.contains("96") -> 7
                        config.proposedValue.contains("44.1") || config.proposedValue.contains("CD") -> 6
                        else -> 5
                    }
                    settingsPreferences.setDownloadQuality(quality)
                }
                "wordByWordLyrics" -> {
                    val enabled = config.proposedValue.equals("true", ignoreCase = true) || config.proposedValue.contains("enable", ignoreCase = true)
                    settingsPreferences.setWordByWordLyrics(enabled)
                }
            }
            _uiState.update { it.copy(proposedConfig = null) }
        }
    }

    fun dismissProposedConfig() {
        _uiState.update { it.copy(proposedConfig = null) }
    }

    fun stopGeneration() {
        activeJob?.cancel()
        _uiState.update { it.copy(isStreaming = false, streamingText = "") }
    }

    fun clearHistory() {
        activeJob?.cancel()
        viewModelScope.launch {
            aiSettingsRepository.clearChatHistory()
            _uiState.update { LastWaveAiUiState() }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
