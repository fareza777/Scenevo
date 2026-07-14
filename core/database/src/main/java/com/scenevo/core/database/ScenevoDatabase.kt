package com.scenevo.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.scenevo.core.database.dao.AssetDao
import com.scenevo.core.database.dao.ProjectDao
import com.scenevo.core.database.dao.RenderJobDao
import com.scenevo.core.database.entity.AssetEntity
import com.scenevo.core.database.entity.ProjectEntity
import com.scenevo.core.database.entity.RenderJobEntity

@Database(
    entities = [ProjectEntity::class, AssetEntity::class, RenderJobEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ScenevoDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun assetDao(): AssetDao
    abstract fun renderJobDao(): RenderJobDao
}
