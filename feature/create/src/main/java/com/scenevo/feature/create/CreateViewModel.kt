package com.scenevo.feature.create

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scenevo.core.common.LocalMediaCache
import com.scenevo.core.common.MediaUris
import com.scenevo.core.common.PersistableUri
import com.scenevo.domain.model.MediaType
import com.scenevo.domain.model.MotionEffect
import com.scenevo.domain.model.MusicTrack
import com.scenevo.domain.model.Project
import com.scenevo.domain.model.ProjectStatus
import com.scenevo.domain.model.Scene
import com.scenevo.domain.model.StockKind
import com.scenevo.domain.model.StockMedia
import com.scenevo.domain.model.TransitionType
import com.scenevo.domain.model.VisualAsset
import com.scenevo.domain.model.VoiceProvider
import com.scenevo.domain.model.VoiceTrack
import com.scenevo.domain.repository.ProjectRepository
import com.scenevo.domain.repository.StockRepository
import com.scenevo.domain.usecase.SplitScriptIntoScenesUseCase
import com.scenevo.engine.tts.SmartNarrationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class CreateUiState(
    val step: Int = 0,
    val title: String = "",
    val script: String = "",
    val scenes: List<Scene> = emptyList(),
    val attachedCount: Int = 0,
    val visuals: List<VisualAsset> = emptyList(),
    val voiceTrack: VoiceTrack? = null,
    val voiceStatus: String? = null,
    val musicTrack: MusicTrack? = null,
    val musicVolume: Float = 0.25f,
    val stockQuery: String = "",
    val stockKind: StockKind = StockKind.IMAGE,
    val stockResults: List<StockMedia> = emptyList(),
    val voiceProvider: VoiceProvider = VoiceProvider.ANDROID_TTS,
    val isSearchingStock: Boolean = false,
    val isCachingStock: Boolean = false,
    val isSynthesizing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CreateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val splitScript: SplitScriptIntoScenesUseCase,
    private val projectRepository: ProjectRepository,
    private val narrationEngine: SmartNarrationEngine,
    private val stockRepository: StockRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateUiState())
    val uiState: StateFlow<CreateUiState> = _uiState.asStateFlow()

    fun updateTitle(value: String) = _uiState.update { it.copy(title = value, error = null) }
    fun updateScript(value: String) = _uiState.update { it.copy(script = value, error = null) }
    fun updateStockQuery(value: String) = _uiState.update { it.copy(stockQuery = value) }
    fun setStockKind(kind: StockKind) = _uiState.update {
        it.copy(stockKind = kind, stockResults = emptyList(), error = null)
    }
    fun setVoiceProvider(provider: VoiceProvider) = _uiState.update {
        it.copy(voiceProvider = provider, error = null)
    }

    fun splitScenes() {
        val script = _uiState.value.script
        if (script.isBlank()) {
            _uiState.update { it.copy(error = "Script cannot be empty") }
            return
        }
        val scenes = splitScript(script)
        _uiState.update {
            it.copy(scenes = scenes, step = 1, error = null)
        }
    }

    fun nextStep() = _uiState.update { it.copy(step = (it.step + 1).coerceAtMost(4), error = null) }
    fun prevStep() = _uiState.update { it.copy(step = (it.step - 1).coerceAtLeast(0), error = null) }

    fun continueFromVisuals() {
        if (_uiState.value.attachedCount == 0 || _uiState.value.scenes.none { it.visual != null }) {
            _uiState.update { it.copy(error = "Pasang minimal 1 visual (galeri atau stock) dulu.") }
            return
        }
        nextStep()
    }

    fun attachVisuals(uris: List<String>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCachingStock = true, error = null) }
            val assets = withContext(Dispatchers.IO) {
                uris.mapIndexedNotNull { index, uriString ->
                    PersistableUri.takeRead(context, uriString)
                    val cached = LocalMediaCache.import(context, uriString) ?: return@mapIndexedNotNull null
                    VisualAsset(
                        id = UUID.randomUUID().toString(),
                        uri = cached.fileUri,
                        type = if (cached.isVideo) MediaType.VIDEO else MediaType.IMAGE,
                        displayName = "Asset ${index + 1}",
                    )
                }
            }
            if (assets.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isCachingStock = false,
                        error = "Gagal import visual dari galeri. Coba foto JPG/PNG lain.",
                    )
                }
                return@launch
            }
            applyVisuals(assets)
            _uiState.update { it.copy(isCachingStock = false) }
        }
    }

    fun searchStock() {
        val query = _uiState.value.stockQuery
        val kind = _uiState.value.stockKind
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingStock = true, error = null) }
            runCatching { stockRepository.search(query, kind) }
                .onSuccess { media ->
                    _uiState.update {
                        it.copy(isSearchingStock = false, stockResults = media)
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isSearchingStock = false, error = err.message ?: "Stock search failed")
                    }
                }
        }
    }

    fun attachStock(media: StockMedia) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCachingStock = true, error = null) }
            runCatching { stockRepository.cache(media) }
                .onSuccess { asset ->
                    val merged = _uiState.value.visuals + asset
                    applyVisuals(merged)
                    _uiState.update { it.copy(isCachingStock = false) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isCachingStock = false, error = err.message ?: "Cache failed")
                    }
                }
        }
    }

    private fun applyVisuals(assets: List<VisualAsset>) {
        if (assets.isEmpty()) return
        val scenes = _uiState.value.scenes.mapIndexed { index, scene ->
            val asset = assets[index % assets.size]
            scene.copy(
                visual = asset,
                transition = TransitionType.CUT,
                motion = MotionEffect.KEN_BURNS_ZOOM_IN,
            )
        }
        _uiState.update {
            it.copy(
                visuals = assets,
                scenes = scenes,
                attachedCount = assets.size,
                error = null,
            )
        }
    }

    fun synthesizeVoice() {
        val script = _uiState.value.script
        if (script.isBlank()) return
        val preferred = _uiState.value.voiceProvider
        viewModelScope.launch {
            _uiState.update { it.copy(isSynthesizing = true, error = null, voiceStatus = null) }
            runCatching { narrationEngine.synthesizeSmart(script, preferred = preferred) }
                .onSuccess { outcome ->
                    val track = VoiceTrack(
                        id = UUID.randomUUID().toString(),
                        uri = MediaUris.fileUri(outcome.result.audioFile),
                        provider = outcome.provider,
                        durationMs = outcome.result.durationMsEstimate,
                    )
                    _uiState.update {
                        it.copy(
                            isSynthesizing = false,
                            voiceTrack = track,
                            voiceStatus = "${outcome.provider.name} ready (~${outcome.result.durationMsEstimate / 1000}s)",
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isSynthesizing = false,
                            error = err.message ?: "TTS failed",
                        )
                    }
                }
        }
    }

    fun attachMusic(uri: String) {
        viewModelScope.launch {
            PersistableUri.takeRead(context, uri)
            val cached = withContext(Dispatchers.IO) {
                LocalMediaCache.import(context, uri)
            }
            val resolvedUri = cached?.fileUri ?: uri
            val name = uri.substringAfterLast('/').substringAfterLast('%').ifBlank { "Background music" }
            _uiState.update {
                it.copy(
                    musicTrack = MusicTrack(
                        id = UUID.randomUUID().toString(),
                        uri = resolvedUri,
                        displayName = UriDecode(name),
                        volume = it.musicVolume,
                    ),
                    error = null,
                )
            }
        }
    }

    fun setMusicVolume(volume: Float) {
        _uiState.update { state ->
            state.copy(
                musicVolume = volume,
                musicTrack = state.musicTrack?.copy(volume = volume),
            )
        }
    }

    fun clearMusic() = _uiState.update { it.copy(musicTrack = null) }

    fun saveProject(onDone: (String) -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.scenes.none { it.visual != null }) {
                _uiState.update { it.copy(error = "Tidak bisa simpan: belum ada visual di scene.") }
                return@launch
            }
            _uiState.update { it.copy(isSaving = true, error = null) }
            runCatching {
                val now = System.currentTimeMillis()
                val project = Project(
                    id = UUID.randomUUID().toString(),
                    title = state.title.ifBlank { "Untitled Montage" },
                    script = state.script,
                    scenes = state.scenes,
                    voiceTrack = state.voiceTrack,
                    musicTrack = state.musicTrack,
                    status = ProjectStatus.READY,
                    createdAt = now,
                    updatedAt = now,
                )
                projectRepository.upsert(project)
                project
            }.onSuccess { project ->
                _uiState.update { it.copy(isSaving = false) }
                onDone(project.id)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isSaving = false, error = err.message ?: "Save failed")
                }
            }
        }
    }
}

private fun UriDecode(value: String): String = try {
    java.net.URLDecoder.decode(value, Charsets.UTF_8.name())
} catch (_: Exception) {
    value
}
