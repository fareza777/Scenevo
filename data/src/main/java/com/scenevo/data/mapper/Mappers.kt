package com.scenevo.data.mapper

import com.scenevo.core.database.entity.AssetEntity
import com.scenevo.core.database.entity.ProjectEntity
import com.scenevo.core.database.entity.RenderJobEntity
import com.scenevo.domain.model.AssetSource
import com.scenevo.domain.model.AspectRatio
import com.scenevo.domain.model.MediaType
import com.scenevo.domain.model.Project
import com.scenevo.domain.model.ProjectStatus
import com.scenevo.domain.model.RenderJob
import com.scenevo.domain.model.RenderStatus
import com.scenevo.domain.model.VisualAsset
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun Project.toEntity(): ProjectEntity = ProjectEntity(
    id = id,
    title = title,
    script = script,
    aspectRatio = aspectRatio.name,
    status = status.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    payloadJson = json.encodeToString(this),
)

fun ProjectEntity.toDomain(): Project {
    return runCatching { json.decodeFromString<Project>(payloadJson) }.getOrElse {
        Project(
            id = id,
            title = title,
            script = script,
            aspectRatio = AspectRatio.valueOf(aspectRatio),
            status = ProjectStatus.valueOf(status),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}

fun VisualAsset.toEntity(createdAt: Long = System.currentTimeMillis()): AssetEntity = AssetEntity(
    id = id,
    uri = uri,
    type = type.name,
    displayName = displayName,
    width = width,
    height = height,
    durationMs = durationMs,
    source = source.name,
    createdAt = createdAt,
)

fun AssetEntity.toDomain(): VisualAsset = VisualAsset(
    id = id,
    uri = uri,
    type = MediaType.valueOf(type),
    displayName = displayName,
    width = width,
    height = height,
    durationMs = durationMs,
    source = AssetSource.valueOf(source),
)

fun RenderJob.toEntity(): RenderJobEntity = RenderJobEntity(
    id = id,
    projectId = projectId,
    status = status.name,
    progress = progress,
    outputUri = outputUri,
    errorMessage = errorMessage,
    startedAt = startedAt,
    finishedAt = finishedAt,
)

fun RenderJobEntity.toDomain(): RenderJob = RenderJob(
    id = id,
    projectId = projectId,
    status = RenderStatus.valueOf(status),
    progress = progress,
    outputUri = outputUri,
    errorMessage = errorMessage,
    startedAt = startedAt,
    finishedAt = finishedAt,
)
