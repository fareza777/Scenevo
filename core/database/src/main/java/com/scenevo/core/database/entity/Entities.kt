package com.scenevo.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val script: String,
    val aspectRatio: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val payloadJson: String,
)

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val type: String,
    val displayName: String,
    val width: Int?,
    val height: Int?,
    val durationMs: Long?,
    val source: String,
    val createdAt: Long,
)

@Entity(tableName = "render_jobs")
data class RenderJobEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val status: String,
    val progress: Float,
    val outputUri: String?,
    val errorMessage: String?,
    val startedAt: Long?,
    val finishedAt: Long?,
)
