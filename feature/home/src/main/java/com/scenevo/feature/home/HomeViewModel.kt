package com.scenevo.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scenevo.domain.model.Project
import com.scenevo.domain.model.ProjectStatus
import com.scenevo.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class HomeUiExtras(
    val actionTarget: Project? = null,
    val pendingDelete: Project? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
) : ViewModel() {
    val projects: StateFlow<List<Project>> = projectRepository
        .observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _extras = MutableStateFlow(HomeUiExtras())
    val extras: StateFlow<HomeUiExtras> = _extras.asStateFlow()

    fun openActions(project: Project) {
        _extras.update { it.copy(actionTarget = project) }
    }

    fun dismissActions() {
        _extras.update { it.copy(actionTarget = null) }
    }

    fun requestDeleteFromActions() {
        val target = _extras.value.actionTarget ?: return
        _extras.update { it.copy(actionTarget = null, pendingDelete = target) }
    }

    fun dismissDelete() {
        _extras.update { it.copy(pendingDelete = null) }
    }

    fun confirmDelete() {
        val target = _extras.value.pendingDelete ?: return
        viewModelScope.launch {
            projectRepository.delete(target.id)
            _extras.update { it.copy(pendingDelete = null) }
        }
    }

    fun duplicateFromActions() {
        val source = _extras.value.actionTarget ?: return
        _extras.update { it.copy(actionTarget = null) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val copy = source.copy(
                id = UUID.randomUUID().toString(),
                title = "${source.title} (copy)",
                status = ProjectStatus.DRAFT,
                createdAt = now,
                updatedAt = now,
                scenes = source.scenes.map { scene ->
                    scene.copy(
                        id = UUID.randomUUID().toString(),
                        visual = scene.visual?.copy(id = UUID.randomUUID().toString()),
                    )
                },
                voiceTrack = source.voiceTrack?.copy(id = UUID.randomUUID().toString()),
                musicTrack = source.musicTrack?.copy(id = UUID.randomUUID().toString()),
            )
            projectRepository.upsert(copy)
        }
    }
}
