package com.scenevo.feature.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scenevo.domain.model.AspectRatio
import com.scenevo.domain.model.MotionEffect
import com.scenevo.domain.model.Project
import com.scenevo.domain.model.Timeline
import com.scenevo.domain.repository.ProjectRepository
import com.scenevo.engine.timeline.TimelineComposer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorUiState(
    val project: Project? = null,
    val timeline: Timeline? = null,
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val projectId: String = checkNotNull(savedStateHandle["projectId"])

    val uiState: StateFlow<EditorUiState> = projectRepository
        .observeProject(projectId)
        .map { project ->
            EditorUiState(
                project = project,
                timeline = project?.let { TimelineComposer.compose(it) },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditorUiState())

    fun setAspectRatio(ratio: AspectRatio) = updateProject { it.copy(aspectRatio = ratio) }

    fun setSubtitlesEnabled(enabled: Boolean) = updateProject {
        it.copy(subtitleStyle = it.subtitleStyle.copy(enabled = enabled, burnIn = enabled))
    }

    fun cycleMotion(sceneId: String) = updateProject { project ->
        val motions = MotionEffect.entries
        val scenes = project.scenes.map { scene ->
            if (scene.id != sceneId) scene
            else {
                val next = motions[(motions.indexOf(scene.motion) + 1) % motions.size]
                scene.copy(motion = next)
            }
        }
        project.copy(scenes = scenes)
    }

    private fun updateProject(transform: (Project) -> Project) {
        viewModelScope.launch {
            val current = projectRepository.getProject(projectId) ?: return@launch
            projectRepository.upsert(
                transform(current).copy(updatedAt = System.currentTimeMillis()),
            )
        }
    }
}
