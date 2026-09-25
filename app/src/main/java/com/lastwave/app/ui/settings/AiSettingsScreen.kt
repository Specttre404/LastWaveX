package com.lastwave.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastwave.app.data.ai.AiMode
import com.lastwave.app.data.ai.AiProviderType
import com.lastwave.app.data.ai.AiUserProfile
import com.lastwave.app.ui.common.ExpressiveHeader
import com.lastwave.app.ui.common.adaptiveContentWidth
import com.lastwave.app.ui.common.safeHorizontalContentPadding
import com.lastwave.app.ui.player.LocalMiniPlayerScrollClearance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    onBack: () -> Unit,
    viewModel: AiSettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var apiKeyInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .adaptiveContentWidth(maxWidth = 860.dp),
    ) {
        ExpressiveHeader(title = "LASTWAVEX AI Assistant", onBack = onBack)

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 32.dp + LocalMiniPlayerScrollClearance.current,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().safeHorizontalContentPadding(),
        ) {
            // Master Toggle Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    "Enable LASTWAVEX AI",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "App-wide intelligent assistant for settings, audio, and discovery",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Switch(
                            checked = settings.enabled,
                            onCheckedChange = viewModel::setEnabled,
                        )
                    }
                }
            }

            if (settings.enabled) {
                // Mode Selector Card
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                "AI Provider Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Choose between Free mode or Bring Your Own Key (BYOK)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))

                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = settings.mode == AiMode.FREE,
                                    onClick = { viewModel.setMode(AiMode.FREE) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                ) {
                                    Text("Free Mode (Firebase)")
                                }
                                SegmentedButton(
                                    selected = settings.mode == AiMode.BYOK,
                                    onClick = { viewModel.setMode(AiMode.BYOK) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                ) {
                                    Text("BYOK Mode")
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            if (settings.mode == AiMode.FREE) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        "Free AI access uses Firebase AI Logic + Gemini Developer API free tier. Subject to provider quotas and rate limits.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(12.dp),
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        "BYOK connects directly from your device to your selected provider. Usage is governed by your provider's pricing and quota.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(12.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // Expertise Level Selector
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "User Expertise Level",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "AI explanations adapt technical depth to your selected profile level",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                AiUserProfile.entries.forEach { profile ->
                                    val selected = settings.userLevel == profile
                                    Surface(
                                        onClick = { viewModel.setUserLevel(profile) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    profile.displayName,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                )
                                                Text(
                                                    profile.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            if (selected) {
                                                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // BYOK Configuration Section
                if (settings.mode == AiMode.BYOK) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "BYOK Credentials",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Spacer(Modifier.height(12.dp))

                                // Provider Chips
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AiProviderType.entries.filter { it.isByok }.forEach { provider ->
                                        val selected = settings.providerType == provider
                                        FilterChip(
                                            selected = selected,
                                            onClick = { viewModel.setProviderType(provider) },
                                            label = { Text(provider.displayName) },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // Model Name Input
                                OutlinedTextField(
                                    value = settings.byokModel,
                                    onValueChange = viewModel::setByokModel,
                                    label = { Text("Model Identifier") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                if (settings.providerType == AiProviderType.BYOK_OPENAI_COMPATIBLE) {
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = settings.customEndpoint,
                                        onValueChange = viewModel::setCustomEndpoint,
                                        label = { Text("Custom Endpoint Base URL") },
                                        placeholder = { Text("https://api.openai.com/v1") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }

                                Spacer(Modifier.height(12.dp))

                                // API Key Input
                                OutlinedTextField(
                                    value = apiKeyInput,
                                    onValueChange = { apiKeyInput = it },
                                    label = { Text("${settings.providerType.displayName} API Key") },
                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    trailingIcon = {
                                        IconButton(onClick = { showPassword = !showPassword }) {
                                            Icon(
                                                if (showPassword) Icons.Filled.KeyOff else Icons.Filled.Lock,
                                                contentDescription = "Toggle key visibility",
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                Spacer(Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Button(
                                        onClick = { viewModel.validateAndSaveKey(apiKeyInput) },
                                        enabled = !uiState.isValidating && apiKeyInput.isNotBlank(),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        if (uiState.isValidating) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        Text("Validate & Encrypt Key")
                                    }

                                    if (uiState.keySaved) {
                                        OutlinedButton(onClick = viewModel::clearKey) {
                                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Remove")
                                        }
                                    }
                                }

                                uiState.validationStatus?.let { status ->
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        status,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (uiState.keySaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }

                // Privacy & Security Card
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Privacy & Credential Security",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "• BYOK API keys are encrypted locally using hardware-backed Android Keystore AES-256.\n" +
                                "• Keys are NEVER stored in plaintext, DataStore, logs, or crash reports.\n" +
                                "• Keys are NEVER uploaded to LASTWAVEX or Firebase servers.\n" +
                                "• Requests are made directly from your device to your selected provider endpoint.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = viewModel::clearAllHistory) {
                                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Clear AI Conversation History")
                            }
                        }
                    }
                }
            }
        }
    }
}
