package com.lastwave.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastwave.app.data.ai.AiCredentialStore
import com.lastwave.app.data.ai.AiMode
import com.lastwave.app.data.ai.AiProviderType
import com.lastwave.app.data.ai.AiRepository
import com.lastwave.app.data.ai.AiResult
import com.lastwave.app.data.ai.AiSettings
import com.lastwave.app.data.ai.AiSettingsRepository
import com.lastwave.app.data.ai.AiUserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiSettingsUiState(
    val validationStatus: String? = null,
    val isValidating: Boolean = false,
    val keySaved: Boolean = false,
)

@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val aiSettingsRepository: AiSettingsRepository,
    private val credentialStore: AiCredentialStore,
) : ViewModel() {

    val settings: StateFlow<AiSettings> = aiSettingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiSettings())

    private val _uiState = MutableStateFlow(AiSettingsUiState())
    val uiState: StateFlow<AiSettingsUiState> = _uiState.asStateFlow()

    init {
        checkKeySaved()
    }

    private fun checkKeySaved() {
        val currentProvider = settings.value.providerType
        _uiState.update { it.copy(keySaved = credentialStore.hasApiKey(currentProvider)) }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            aiSettingsRepository.updateSettings { it.copy(enabled = enabled) }
        }
    }

    fun setMode(mode: AiMode) {
        viewModelScope.launch {
            val defaultProvider = if (mode == AiMode.FREE) AiProviderType.FREE_FIREBASE_GEMINI else AiProviderType.BYOK_GEMINI
            aiSettingsRepository.updateSettings { it.copy(mode = mode, providerType = defaultProvider) }
            checkKeySaved()
        }
    }

    fun setProviderType(providerType: AiProviderType) {
        viewModelScope.launch {
            val mode = if (providerType == AiProviderType.FREE_FIREBASE_GEMINI) AiMode.FREE else AiMode.BYOK
            val defaultModel = providerType.defaultModel
            aiSettingsRepository.updateSettings { it.copy(providerType = providerType, mode = mode, byokModel = defaultModel) }
            checkKeySaved()
        }
    }

    fun setUserLevel(userLevel: AiUserProfile) {
        viewModelScope.launch {
            aiSettingsRepository.updateSettings { it.copy(userLevel = userLevel) }
        }
    }

    fun setByokModel(model: String) {
        viewModelScope.launch {
            aiSettingsRepository.updateSettings { it.copy(byokModel = model.trim()) }
        }
    }

    fun setCustomEndpoint(endpoint: String) {
        viewModelScope.launch {
            aiSettingsRepository.updateSettings { it.copy(customEndpoint = endpoint.trim()) }
            credentialStore.saveCustomEndpoint(endpoint.trim())
        }
    }

    fun validateAndSaveKey(apiKey: String) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            _uiState.update { it.copy(validationStatus = "API key cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isValidating = true, validationStatus = "Validating key...") }
            val currentProvider = settings.value.providerType
            val customEndpoint = settings.value.customEndpoint

            val result = aiRepository.validateKey(currentProvider, cleanKey, customEndpoint)
            when (result) {
                is AiResult.Success -> {
                    credentialStore.saveApiKey(currentProvider, cleanKey)
                    _uiState.update { it.copy(isValidating = false, validationStatus = "Key validated and encrypted in Android Keystore!", keySaved = true) }
                }
                is AiResult.InvalidKey -> {
                    _uiState.update { it.copy(isValidating = false, validationStatus = "Invalid Key: ${result.providerDetails}") }
                }
                is AiResult.RateLimited -> {
                    // Valid key but rate limited
                    credentialStore.saveApiKey(currentProvider, cleanKey)
                    _uiState.update { it.copy(isValidating = false, validationStatus = "Key saved! (Note: Rate limit/quota currently reached on provider)", keySaved = true) }
                }
                is AiResult.Error -> {
                    _uiState.update { it.copy(isValidating = false, validationStatus = "Validation Error: ${result.message}") }
                }
                else -> {
                    _uiState.update { it.copy(isValidating = false, validationStatus = "Validation failed. Please check key/network.") }
                }
            }
        }
    }

    fun clearKey() {
        val currentProvider = settings.value.providerType
        credentialStore.clearApiKey(currentProvider)
        _uiState.update { it.copy(keySaved = false, validationStatus = "Key removed from Keystore") }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            aiSettingsRepository.clearChatHistory()
            _uiState.update { it.copy(validationStatus = "AI conversation history cleared") }
        }
    }

    fun clearValidationStatus() {
        _uiState.update { it.copy(validationStatus = null) }
    }
}
