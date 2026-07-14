package com.scenevo.domain.repository

import com.scenevo.domain.model.AiProviderConfig
import com.scenevo.domain.model.Project
import com.scenevo.domain.model.RenderJob
import com.scenevo.domain.model.VisualAsset
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun observeProjects(): Flow<List<Project>>
    fun observeProject(id: String): Flow<Project?>
    suspend fun getProject(id: String): Project?
    suspend fun upsert(project: Project)
    suspend fun delete(id: String)
}

interface AssetRepository {
    fun observeAssets(): Flow<List<VisualAsset>>
    suspend fun importAsset(uri: String, displayName: String): VisualAsset
    suspend fun deleteAsset(id: String)
}

interface RenderRepository {
    fun observeJobs(projectId: String): Flow<List<RenderJob>>
    suspend fun enqueue(projectId: String): RenderJob
    suspend fun cancel(jobId: String)
}

interface SettingsRepository {
    fun observeAiConfig(): Flow<AiProviderConfig>
    suspend fun updateAiConfig(config: AiProviderConfig)
    suspend fun saveApiKey(providerKey: String, rawKey: String)
    suspend fun clearApiKey(providerKey: String)
    suspend fun getApiKey(providerKey: String): String?
}
