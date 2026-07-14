package com.scenevo.feature.export

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scenevo.domain.model.ExportResolution
import com.scenevo.domain.model.Project
import com.scenevo.domain.model.ProjectStatus
import com.scenevo.domain.model.RenderStatus
import com.scenevo.domain.repository.ProjectRepository
import com.scenevo.engine.render.ExportPublisher
import com.scenevo.engine.render.VideoRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExportUiState(
    val project: Project? = null,
    val status: RenderStatus = RenderStatus.QUEUED,
    val progress: Float = 0f,
    val message: String = "Pilih kualitas, lalu render.",
    val outputPath: String? = null,
    val galleryUri: String? = null,
    val shareReady: Boolean = false,
    val error: String? = null,
    val started: Boolean = false,
    val publishMessage: String? = null,
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val videoRenderer: VideoRenderer,
    private val exportPublisher: ExportPublisher,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val projectId: String = checkNotNull(savedStateHandle["projectId"])
    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private var lastShareIntent: Intent? = null

    init {
        viewModelScope.launch {
            val project = projectRepository.getProject(projectId)
            _uiState.update { it.copy(project = project) }
        }
    }

    fun setResolution(resolution: ExportResolution) {
        viewModelScope.launch {
            val current = projectRepository.getProject(projectId) ?: return@launch
            val updated = current.copy(
                exportSettings = current.exportSettings.copy(resolution = resolution),
                updatedAt = System.currentTimeMillis(),
            )
            projectRepository.upsert(updated)
            _uiState.update { it.copy(project = updated) }
        }
    }

    fun setIncludeMusic(include: Boolean) {
        viewModelScope.launch {
            val current = projectRepository.getProject(projectId) ?: return@launch
            val updated = current.copy(
                exportSettings = current.exportSettings.copy(includeMusic = include),
                updatedAt = System.currentTimeMillis(),
            )
            projectRepository.upsert(updated)
            _uiState.update { it.copy(project = updated) }
        }
    }

    fun startExport() {
        if (_uiState.value.started) return
        _uiState.update { it.copy(started = true, message = "Preparing…") }
        viewModelScope.launch {
            val project = projectRepository.getProject(projectId)
            if (project == null) {
                _uiState.update {
                    it.copy(
                        status = RenderStatus.FAILED,
                        error = "Project not found",
                        message = "Failed",
                    )
                }
                return@launch
            }
            projectRepository.upsert(project.copy(status = ProjectStatus.RENDERING))
            val result = videoRenderer.render(project) { progress ->
                _uiState.update {
                    it.copy(
                        status = progress.job.status,
                        progress = progress.job.progress,
                        message = progress.message,
                        outputPath = progress.job.outputUri,
                        error = progress.job.errorMessage,
                    )
                }
            }
            val nextStatus = when (result.status) {
                RenderStatus.COMPLETED -> ProjectStatus.EXPORTED
                else -> ProjectStatus.FAILED
            }
            projectRepository.upsert(
                project.copy(
                    status = nextStatus,
                    updatedAt = System.currentTimeMillis(),
                ),
            )

            if (result.status == RenderStatus.COMPLETED) {
                val outputUri = result.outputUri
                if (outputUri != null) {
                    runCatching { exportPublisher.publish(outputUri) }
                        .onSuccess { published ->
                            lastShareIntent = exportPublisher.shareIntent(
                                published.shareUri,
                                published.displayName,
                            )
                            _uiState.update {
                                it.copy(
                                    status = result.status,
                                    progress = result.progress,
                                    outputPath = outputUri,
                                    galleryUri = published.galleryUri?.toString(),
                                    shareReady = true,
                                    publishMessage = if (published.galleryUri != null) {
                                        "Saved to Movies/Scenevo"
                                    } else {
                                        "Ready to share"
                                    },
                                    message = "Export complete",
                                )
                            }
                        }
                        .onFailure { err ->
                            _uiState.update {
                                it.copy(
                                    status = result.status,
                                    progress = result.progress,
                                    outputPath = outputUri,
                                    message = "Export complete",
                                    publishMessage = err.message ?: "Gallery save skipped",
                                )
                            }
                        }
                } else {
                    _uiState.update {
                        it.copy(
                            status = result.status,
                            progress = result.progress,
                            message = "Export complete",
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        status = result.status,
                        progress = result.progress,
                        outputPath = result.outputUri,
                        error = result.errorMessage,
                        message = result.errorMessage ?: "Export finished",
                    )
                }
            }
        }
    }

    fun consumeShareIntent(): Intent? = lastShareIntent
}
