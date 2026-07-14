package com.scenevo.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scenevo.core.datastore.SettingsDataSource
import com.scenevo.domain.model.AiProvider
import com.scenevo.domain.model.AiProviderConfig
import com.scenevo.domain.model.AppPreferences
import com.scenevo.domain.model.VoicePackInfo
import com.scenevo.domain.repository.SettingsRepository
import com.scenevo.domain.repository.VoicePackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val aiEnabled: Boolean = false,
    val provider: AiProvider = AiProvider.NONE,
    val apiKeyInput: String = "",
    val baseUrl: String = "http://127.0.0.1:11434",
    val stockConsent: Boolean = false,
    val stockWifiOnly: Boolean = true,
    val pexelsKeyInput: String = "",
    val preferPiper: Boolean = false,
    val voicePacks: List<VoicePackInfo> = emptyList(),
    val packProgress: Float? = null,
    val packMessage: String? = null,
    val savedMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val voicePackRepository: VoicePackRepository,
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
        viewModelScope.launch {
            settingsRepository.observeAppPreferences().collect { prefs ->
                _uiState.update {
                    it.copy(
                        stockConsent = prefs.stockConsent,
                        stockWifiOnly = prefs.stockWifiOnly,
                        preferPiper = prefs.preferPiper,
                    )
                }
            }
        }
        viewModelScope.launch {
            voicePackRepository.observePacks().collect { packs ->
                _uiState.update { it.copy(voicePacks = packs) }
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
    fun setPexelsKeyInput(value: String) = _uiState.update { it.copy(pexelsKeyInput = value) }

    fun setStockConsent(value: Boolean) {
        viewModelScope.launch {
            val current = currentPrefs()
            settingsRepository.updateAppPreferences(current.copy(stockConsent = value))
        }
    }

    fun setStockWifiOnly(value: Boolean) {
        viewModelScope.launch {
            val current = currentPrefs()
            settingsRepository.updateAppPreferences(current.copy(stockWifiOnly = value))
        }
    }

    fun setPreferPiper(value: Boolean) {
        viewModelScope.launch {
            val current = currentPrefs()
            settingsRepository.updateAppPreferences(current.copy(preferPiper = value))
        }
    }

    fun savePexelsKey() {
        viewModelScope.launch {
            val key = _uiState.value.pexelsKeyInput.trim()
            if (key.isNotBlank()) {
                settingsRepository.saveApiKey(SettingsDataSource.PEXELS_KEY, key)
                _uiState.update {
                    it.copy(pexelsKeyInput = "", savedMessage = "Pexels key saved on device.")
                }
            }
        }
    }

    fun installPiperPack() {
        viewModelScope.launch {
            _uiState.update { it.copy(packProgress = 0f, packMessage = "Downloading Piper pack…", savedMessage = null) }
            runCatching {
                voicePackRepository.installDefaultPack { p ->
                    _uiState.update { it.copy(packProgress = p) }
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(packProgress = null, packMessage = "Piper pack ready (ONNX on device).")
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(packProgress = null, packMessage = err.message ?: "Download failed")
                }
            }
        }
    }

    fun requestPadPack() {
        viewModelScope.launch {
            _uiState.update { it.copy(packProgress = 0f, packMessage = "Requesting Play Asset pack…") }
            runCatching {
                voicePackRepository.requestPlayAssetPack { p ->
                    _uiState.update { it.copy(packProgress = p) }
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(packProgress = null, packMessage = "PAD pack request finished.")
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(packProgress = null, packMessage = err.message ?: "PAD failed")
                }
            }
        }
    }

    fun uninstallPiperPack() {
        viewModelScope.launch {
            voicePackRepository.uninstall()
            _uiState.update { it.copy(packMessage = "Piper pack removed.") }
        }
    }

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

    private suspend fun currentPrefs(): AppPreferences =
        settingsRepository.observeAppPreferences().first()
}