package com.scenevo.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scenevo.domain.model.AiProvider
import com.scenevo.domain.model.AiProviderConfig
import com.scenevo.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val aiEnabled: Boolean = false,
    val provider: AiProvider = AiProvider.NONE,
    val apiKeyInput: String = "",
    val baseUrl: String = "http://127.0.0.1:11434",
    val savedMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeAiConfig().collect { config ->
                _uiState.update {
                    it.copy(
                        aiEnabled = config.enabled,
                        provider = config.provider,
                        baseUrl = config.baseUrl ?: "http://127.0.0.1:11434",
                    )
                }
            }
        }
    }

    fun setAiEnabled(enabled: Boolean) = _uiState.update {
        it.copy(aiEnabled = enabled, savedMessage = null)
    }

    fun setProvider(provider: AiProvider) = _uiState.update {
        it.copy(provider = provider, savedMessage = null)
    }

    fun setApiKeyInput(value: String) = _uiState.update { it.copy(apiKeyInput = value) }
    fun setBaseUrl(value: String) = _uiState.update { it.copy(baseUrl = value) }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            val provider = if (state.aiEnabled) state.provider else AiProvider.NONE
            if (state.aiEnabled &&
                provider != AiProvider.OLLAMA_LOCAL &&
                provider != AiProvider.NONE &&
                state.apiKeyInput.isNotBlank()
            ) {
                settingsRepository.saveApiKey("api_key_${provider.name.lowercase()}", state.apiKeyInput)
            }
            settingsRepository.updateAiConfig(
                AiProviderConfig(
                    enabled = state.aiEnabled,
                    provider = provider,
                    encryptedApiKeyRef = if (provider != AiProvider.NONE) {
                        "api_key_${provider.name.lowercase()}"
                    } else {
                        null
                    },
                    baseUrl = state.baseUrl.takeIf { provider == AiProvider.OLLAMA_LOCAL },
                ),
            )
            _uiState.update {
                it.copy(savedMessage = "Saved. Keys never leave this device unless you call a provider.", apiKeyInput = "")
            }
        }
    }
}
