package com.scenevo.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scenevo.core.designsystem.component.BrandMark
import com.scenevo.core.designsystem.component.FilmPanel
import com.scenevo.core.designsystem.component.ScenevoBackdrop
import com.scenevo.core.designsystem.component.ScenevoPrimaryButton
import com.scenevo.core.designsystem.component.ScenevoSecondaryButton
import com.scenevo.core.designsystem.component.ScreenSection
import com.scenevo.core.designsystem.component.SegmentedChoice
import com.scenevo.core.designsystem.theme.ScenevoColors
import com.scenevo.domain.model.AiProvider
import com.scenevo.domain.model.VoiceProvider

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ScenevoBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BrandMark(compact = true)
            Spacer(Modifier.height(8.dp))
            ScreenSection(
                eyebrow = "Privacy bay",
                title = "Offline first",
                body = "Core montage tetap lokal. Fitur opsional butuh consent eksplisit.",
            )

            SettingsGroup(title = "Stock (Pexels BYOK)") {
                ToggleRow("Izinkan stock search", state.stockConsent, viewModel::setStockConsent)
                ToggleRow("Stock hanya lewat Wi‑Fi", state.stockWifiOnly, viewModel::setStockWifiOnly)
                SettingsField(
                    value = state.pexelsKeyInput,
                    onValueChange = viewModel::setPexelsKeyInput,
                    label = "Pexels API key (BYO)",
                    password = true,
                )
                ScenevoSecondaryButton("Simpan Pexels key", onClick = viewModel::savePexelsKey)
                Text(
                    "Default stock = foto. Video stock opsional di Create wizard.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ScenevoColors.MistDim,
                )
            }

            SettingsGroup(title = "Narasi") {
                Text(
                    "Default: Android TTS offline. ElevenLabs = BYOK opsional.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ScenevoColors.MistDim,
                )
                SegmentedChoice(
                    options = listOf("Offline TTS", "ElevenLabs BYOK"),
                    selectedIndex = if (state.narrationProvider == VoiceProvider.ANDROID_TTS) 0 else 1,
                    onSelect = { index ->
                        viewModel.setNarrationProvider(
                            if (index == 0) {
                                VoiceProvider.ANDROID_TTS
                            } else {
                                VoiceProvider.ELEVENLABS_USER_KEY
                            },
                        )
                    },
                )
                SettingsField(
                    value = state.elevenLabsKeyInput,
                    onValueChange = viewModel::setElevenLabsKeyInput,
                    label = "ElevenLabs API key (BYO)",
                    password = true,
                )
                SettingsField(
                    value = state.elevenLabsVoiceId,
                    onValueChange = viewModel::setElevenLabsVoiceId,
                    label = "ElevenLabs voice ID",
                )
                ScenevoSecondaryButton("Simpan ElevenLabs", onClick = viewModel::saveElevenLabsKey)
            }

            SettingsGroup(title = "Piper / voice pack") {
                ToggleRow("Prefer Piper local voice", state.preferPiper, viewModel::setPreferPiper)
                state.voicePacks.forEach { pack ->
                    Text(
                        "${pack.displayName} · ${if (pack.installed) "installed" else "not installed"} · ${pack.sizeLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ScenevoColors.MistDim,
                    )
                }
                state.packProgress?.let { progress ->
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = ScenevoColors.Cue,
                        trackColor = ScenevoColors.Line,
                    )
                }
                state.packMessage?.let { Text(it, color = ScenevoColors.Signal) }
                ScenevoPrimaryButton("Download Piper pack (Wi‑Fi)", onClick = viewModel::installPiperPack)
                ScenevoSecondaryButton("Request Play Asset pack", onClick = viewModel::requestPadPack)
                ScenevoSecondaryButton("Uninstall Piper pack", onClick = viewModel::uninstallPiperPack)
            }

            SettingsGroup(title = "Optional AI") {
                ToggleRow("Enable optional AI", state.aiEnabled, viewModel::setAiEnabled)

                if (state.aiEnabled) {
                    Text(
                        "Provider",
                        style = MaterialTheme.typography.labelLarge,
                        color = ScenevoColors.MistDim,
                    )
                    val providers = listOf(
                        AiProvider.OPENAI_USER_KEY to "OpenAI",
                        AiProvider.ANTHROPIC_USER_KEY to "Anthropic",
                        AiProvider.GEMINI_USER_KEY to "Gemini",
                        AiProvider.OLLAMA_LOCAL to "Ollama",
                    )
                    SegmentedChoice(
                        options = providers.map { it.second },
                        selectedIndex = providers.indexOfFirst { it.first == state.provider }
                            .coerceAtLeast(0),
                        onSelect = { index -> viewModel.setProvider(providers[index].first) },
                    )

                    if (state.provider == AiProvider.OLLAMA_LOCAL) {
                        SettingsField(
                            value = state.baseUrl,
                            onValueChange = viewModel::setBaseUrl,
                            label = "Ollama base URL",
                        )
                    } else {
                        SettingsField(
                            value = state.apiKeyInput,
                            onValueChange = viewModel::setApiKeyInput,
                            label = "API key",
                            password = true,
                        )
                    }

                    ScenevoPrimaryButton("Simpan AI settings", onClick = viewModel::save)
                }
            }

            if (state.savedMessage != null) {
                Text(state.savedMessage!!, color = ScenevoColors.Signal)
            }

            ScenevoSecondaryButton("Kembali", onClick = onBack)
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = ScenevoColors.Cue,
        )
        FilmPanel {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label, color = ScenevoColors.Mist, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ScenevoColors.Ink,
                checkedTrackColor = ScenevoColors.Cue,
                uncheckedTrackColor = ScenevoColors.Line,
            ),
        )
    }
}

@Composable
private fun SettingsField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    password: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (password) {
            PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ScenevoColors.Cue,
            unfocusedBorderColor = ScenevoColors.Line,
            focusedLabelColor = ScenevoColors.Cue,
            unfocusedLabelColor = ScenevoColors.MistDim,
            cursorColor = ScenevoColors.Cue,
            focusedTextColor = ScenevoColors.Mist,
            unfocusedTextColor = ScenevoColors.Mist,
            focusedContainerColor = ScenevoColors.Panel,
            unfocusedContainerColor = ScenevoColors.Panel,
        ),
    )
}
