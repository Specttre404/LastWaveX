package com.lastwave.app.data.ai

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AiModelsTest {

    @Test
    fun aiUserProfile_fromName_defaultsToCasual() {
        val profile = AiUserProfile.fromName("UNKNOWN_PROFILE")
        assertThat(profile).isEqualTo(AiUserProfile.CASUAL)
    }

    @Test
    fun aiUserProfile_fromName_parsesExactNames() {
        assertThat(AiUserProfile.fromName("BEGINNER")).isEqualTo(AiUserProfile.BEGINNER)
        assertThat(AiUserProfile.fromName("AUDIOPHILE")).isEqualTo(AiUserProfile.AUDIOPHILE)
        assertThat(AiUserProfile.fromName("POWER_USER")).isEqualTo(AiUserProfile.POWER_USER)
    }

    @Test
    fun aiProviderType_isByok_correctlyIdentified() {
        assertThat(AiProviderType.FREE_FIREBASE_GEMINI.isByok).isFalse()
        assertThat(AiProviderType.BYOK_GEMINI.isByok).isTrue()
        assertThat(AiProviderType.BYOK_OPENAI.isByok).isTrue()
        assertThat(AiProviderType.BYOK_OPENAI_COMPATIBLE.isByok).isTrue()
    }

    @Test
    fun aiSystemPrompt_containsContextAndUserLevel() {
        val context = AiContextInfo(
            currentScreen = "Player",
            trackSummary = "'Song' by 'Artist'",
        )
        val prompt = AiSystemPrompt.buildSystemPrompt(AiUserProfile.AUDIOPHILE, context)

        assertThat(prompt).contains("LASTWAVEX AI")
        assertThat(prompt).contains("sampling rates")
        assertThat(prompt).contains("Active Screen: Player")
        assertThat(prompt).contains("Currently Selected Track: 'Song' by 'Artist'")
    }
}
