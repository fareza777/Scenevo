package com.scenevo.feature.create

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scenevo.core.common.PersistableUri
import com.scenevo.domain.model.MediaType
import com.scenevo.domain.model.MotionEffect
import com.scenevo.domain.model.MusicTrack
import com.scenevo.domain.model.ProjectStatus
import com.scenevo.domain.model.Scene
import com.scenevo.domain.model.TransitionType
import com.scenevo.domain.model.VisualAsset
import com.scenevo.domain.model.VoiceProvider
import com.scenevo.domain.model.VoiceTrack
import com.scenevo.domain.repository.ProjectRepository
import com.scenevo.domain.usecase.CreateProjectUseCase
import com.scenevo.domain.usecase.SplitScriptIntoScenesUseCase
import com.scenevo.engine.tts.NarrationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val isSynthesizing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CreateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val splitScript: SplitScriptIntoScenesUseCase,
    private val createProject: CreateProjectUseCase,
    private val projectRepository: ProjectRepository,
    private val narrationEngine: NarrationEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateUiState())
    val uiState: StateFlow<CreateUiState> = _uiState.asStateFlow()

    fun updateTitle(value: String) = _uiState.update { it.copy(title = value, error = null) }
    fun updateScript(value: String) = _uiState.update { it.copy(script = value, error = null) }

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

    fun nextStep() = _uiState.update { it.copy(step = (it.step + 1).coerceAtMost(4)) }
    fun prevStep() = _uiState.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }

    fun attachVisuals(uris: List<String>) {
        if (uris.isEmpty()) return
        PersistableUri.takeReadAll(context, uris)
        val assets = uris.mapIndexed { index, uri ->
            val isVideo = uri.contains("video", ignoreCase = true)
            VisualAsset(
                id = UUID.randomUUID().toString(),
                uri = uri,
                type = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
                displayName = "Asset ${index + 1}",
            )
        }
        val scenes = _uiState.value.scenes.mapIndexed { index, scene ->
            val asset = assets[index % assets.size]
            scene.copy(
                visual = asset,
                transition = if (index == 0) TransitionType.CUT else TransitionType.CROSSFADE,
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
        viewModelScope.launch {
            _uiState.update { it.copy(isSynthesizing = true, error = null, voiceStatus = null) }
            runCatching { narrationEngine.synthesize(script) }
                .onSuccess { result ->
                    val track = VoiceTrack(
                        id = UUID.randomUUID().toString(),
                        uri = result.audioFile.absolutePath,
                        provider = VoiceProvider.ANDROID_TTS,
                        durationMs = result.durationMsEstimate,
                    )
                    _uiState.update {
                        it.copy(
                            isSynthesizing = false,
                            voiceTrack = track,
                            voiceStatus = "Voice ready (~${result.durationMsEstimate / 1000}s)",
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
        PersistableUri.takeRead(context, uri)
        val name = uri.substringAfterLast('/').substringAfterLast('%').ifBlank { "Background music" }
        _uiState.update {
            it.copy(
                musicTrack = MusicTrack(
                    id = UUID.randomUUID().toString(),
                    uri = uri,
                    displayName = UriDecode(name),
                    volume = it.musicVolume,
                ),
                error = null,
            )
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
            _uiState.update { it.copy(isSaving = true, error = null) }
            runCatching {
                val state = _uiState.value
                val base = createProject(
                    title = state.title.ifBlank { "Untitled Montage" },
                    script = state.script,
                )
                val project = base.copy(
                    scenes = state.scenes,
                    voiceTrack = state.voiceTrack,
                    musicTrack = state.musicTrack,
                    status = ProjectStatus.READY,
                    updatedAt = System.currentTimeMillis(),
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
