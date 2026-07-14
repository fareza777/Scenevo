package com.scenevo.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scenevo.domain.model.Project
import com.scenevo.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    projectRepository: ProjectRepository,
) : ViewModel() {
    val projects: StateFlow<List<Project>> = projectRepository
        .observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
