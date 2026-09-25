package com.lastwave.app.data.ai

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

enum class AiMode {
    FREE,
    BYOK,
}

enum class AiProviderType(val displayName: String, val defaultModel: String, val defaultEndpoint: String) {
    FREE_FIREBASE_GEMINI("Free Firebase Gemini", "gemini-2.5-flash", ""),
    BYOK_GEMINI("Gemini API (BYOK)", "gemini-2.5-flash", "https://generativelanguage.googleapis.com"),
    BYOK_OPENAI("OpenAI API (BYOK)", "gpt-4o-mini", "https://api.openai.com/v1"),
    BYOK_OPENAI_COMPATIBLE("Custom OpenAI-Compatible (BYOK)", "gpt-4o-mini", "https://api.openai.com/v1");

    val isByok: Boolean get() = this != FREE_FIREBASE_GEMINI
}

enum class AiUserProfile(val displayName: String, val description: String) {
    BEGINNER("Beginner", "Simple analogies and everyday language. Avoids technical jargon."),
    CASUAL("Casual", "Friendly, clear explanations with practical tips."),
    INTERMEDIATE("Intermediate", "Balanced insights explaining both features and underlying concepts."),
    ADVANCED("Advanced", "Detailed technical explanations of audio pipelines, codecs, and settings."),
    POWER_USER("Power User", "Deep technical analysis, exact parameter trade-offs, and signal paths."),
    AUDIOPHILE("Audiophile", "Audio-focused insights covering sampling rates, bit depths, DAC routing, and acoustics.");

    companion object {
        fun fromName(name: String?): AiUserProfile =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: CASUAL
    }
}

@Immutable
@Serializable
data class AiSettings(
    val enabled: Boolean = true,
    val mode: AiMode = AiMode.FREE,
    val providerType: AiProviderType = AiProviderType.FREE_FIREBASE_GEMINI,
    val userLevel: AiUserProfile = AiUserProfile.CASUAL,
    val freeModel: String = "gemini-2.5-flash",
    val byokModel: String = "gemini-2.5-flash",
    val customEndpoint: String = "",
)

@Immutable
data class AiContextInfo(
    val currentScreen: String? = null,
    val settingsSummary: String? = null,
    val playbackSummary: String? = null,
    val trackSummary: String? = null,
    val downloadSummary: String? = null,
    val scrobbleSummary: String? = null,
    val logExcerpt: String? = null,
)

sealed interface AiResult {
    data class Success(val text: String) : AiResult
    data class Streaming(val chunk: String, val fullTextSoFar: String) : AiResult
    data class Unavailable(val reason: String) : AiResult
    data object RateLimited : AiResult
    data class AppCheckFailure(val details: String) : AiResult
    data object SafetyRefusal : AiResult
    data class InvalidKey(val providerDetails: String) : AiResult
    data class Error(val message: String) : AiResult
}

@Immutable
@Serializable
data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user" or "assistant"
    val content: String,
    val timestampMillis: Long = System.currentTimeMillis(),
)

@Immutable
@Serializable
data class ProposedConfig(
    val settingKey: String,
    val title: String,
    val currentValue: String,
    val proposedValue: String,
    val reason: String,
)
