package com.scenevo.data.repository

import com.scenevo.core.database.dao.AssetDao
import com.scenevo.core.database.dao.ProjectDao
import com.scenevo.core.database.dao.RenderJobDao
import com.scenevo.core.datastore.SettingsDataSource
import com.scenevo.data.mapper.toDomain
import com.scenevo.data.mapper.toEntity
import com.scenevo.domain.model.AiProviderConfig
import com.scenevo.domain.model.AppPreferences
import com.scenevo.domain.model.AssetSource
import com.scenevo.domain.model.MediaType
import com.scenevo.domain.model.Project
import com.scenevo.domain.model.RenderJob
import com.scenevo.domain.model.RenderStatus
import com.scenevo.domain.model.VisualAsset
import com.scenevo.domain.repository.AssetRepository
import com.scenevo.domain.repository.ProjectRepository
import com.scenevo.domain.repository.RenderRepository
import com.scenevo.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
) : ProjectRepository {
    override fun observeProjects(): Flow<List<Project>> =
        projectDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeProject(id: String): Flow<Project?> =
        projectDao.observeById(id).map { it?.toDomain() }

    override suspend fun getProject(id: String): Project? = projectDao.getById(id)?.toDomain()

    override suspend fun upsert(project: Project) = projectDao.upsert(project.toEntity())

    override suspend fun delete(id: String) = projectDao.delete(id)
}

@Singleton
class AssetRepositoryImpl @Inject constructor(
    private val assetDao: AssetDao,
) : AssetRepository {
    override fun observeAssets(): Flow<List<VisualAsset>> =
        assetDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun importAsset(uri: String, displayName: String): VisualAsset {
        val type = when {
            displayName.endsWith(".mp4", true) ||
                displayName.endsWith(".mov", true) ||
                displayName.endsWith(".webm", true) -> MediaType.VIDEO
            displayName.endsWith(".mp3", true) ||
                displayName.endsWith(".wav", true) ||
                displayName.endsWith(".m4a", true) -> MediaType.AUDIO
            else -> MediaType.IMAGE
        }
        val asset = VisualAsset(
            id = UUID.randomUUID().toString(),
            uri = uri,
            type = type,
            displayName = displayName,
            source = AssetSource.IMPORTED,
        )
        assetDao.upsert(asset.toEntity())
        return asset
    }

    override suspend fun deleteAsset(id: String) = assetDao.delete(id)
}

@Singleton
class RenderRepositoryImpl @Inject constructor(
    private val renderJobDao: RenderJobDao,
) : RenderRepository {
    override fun observeJobs(projectId: String): Flow<List<RenderJob>> =
        renderJobDao.observeByProject(projectId).map { list -> list.map { it.toDomain() } }

    override suspend fun enqueue(projectId: String): RenderJob {
        val job = RenderJob(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            status = RenderStatus.QUEUED,
            progress = 0f,
            startedAt = System.currentTimeMillis(),
        )
        renderJobDao.upsert(job.toEntity())
        return job
    }

    override suspend fun cancel(jobId: String) {
        val existing = renderJobDao.getById(jobId) ?: return
        renderJobDao.upsert(
            existing.copy(
                status = RenderStatus.CANCELLED.name,
                finishedAt = System.currentTimeMillis(),
            ),
        )
    }
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataSource: SettingsDataSource,
) : SettingsRepository {
    override fun observeAiConfig(): Flow<AiProviderConfig> = settingsDataSource.observeAiConfig()

    override suspend fun updateAiConfig(config: AiProviderConfig) =
        settingsDataSource.updateAiConfig(config)

    override fun observeAppPreferences(): Flow<AppPreferences> =
        settingsDataSource.observeAppPreferences()

    override suspend fun updateAppPreferences(prefs: AppPreferences) =
        settingsDataSource.updateAppPreferences(prefs)

    override suspend fun saveApiKey(providerKey: String, rawKey: String) =
        settingsDataSource.saveApiKey(providerKey, rawKey)

    override suspend fun clearApiKey(providerKey: String) =
        settingsDataSource.clearApiKey(providerKey)

    override suspend fun getApiKey(providerKey: String): String? =
        settingsDataSource.getApiKey(providerKey)
}
