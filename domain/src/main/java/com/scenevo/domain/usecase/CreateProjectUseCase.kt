package com.scenevo.domain.usecase

import com.scenevo.domain.model.Project
import com.scenevo.domain.repository.ProjectRepository
import java.util.UUID
import javax.inject.Inject

class CreateProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
) {
    suspend operator fun invoke(title: String, script: String = ""): Project {
        val now = System.currentTimeMillis()
        val project = Project(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "Untitled Montage" },
            script = script,
            createdAt = now,
            updatedAt = now,
        )
        projectRepository.upsert(project)
        return project
    }
}
