package com.scenevo.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scenevo.domain.model.Project
import com.scenevo.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiExtras(
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

    fun requestDelete(project: Project) {
        _extras.update { it.copy(pendingDelete = project) }
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
}
