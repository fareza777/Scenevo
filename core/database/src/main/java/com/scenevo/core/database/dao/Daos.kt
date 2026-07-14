package com.scenevo.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.scenevo.core.database.entity.AssetEntity
import com.scenevo.core.database.entity.ProjectEntity
import com.scenevo.core.database.entity.RenderJobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeById(id: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AssetEntity)

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface RenderJobDao {
    @Query("SELECT * FROM render_jobs WHERE projectId = :projectId ORDER BY startedAt DESC")
    fun observeByProject(projectId: String): Flow<List<RenderJobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RenderJobEntity)

    @Query("SELECT * FROM render_jobs WHERE id = :id")
    suspend fun getById(id: String): RenderJobEntity?
}
