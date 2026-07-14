package com.scenevo.data.di

import android.content.Context
import androidx.room.Room
import com.scenevo.core.database.ScenevoDatabase
import com.scenevo.core.datastore.SettingsDataSource
import com.scenevo.data.repository.AssetRepositoryImpl
import com.scenevo.data.repository.ProjectRepositoryImpl
import com.scenevo.data.repository.RenderRepositoryImpl
import com.scenevo.data.repository.SettingsRepositoryImpl
import com.scenevo.domain.repository.AssetRepository
import com.scenevo.domain.repository.ProjectRepository
import com.scenevo.domain.repository.RenderRepository
import com.scenevo.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository

    @Binds @Singleton
    abstract fun bindAssetRepository(impl: AssetRepositoryImpl): AssetRepository

    @Binds @Singleton
    abstract fun bindRenderRepository(impl: RenderRepositoryImpl): RenderRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ScenevoDatabase =
        Room.databaseBuilder(context, ScenevoDatabase::class.java, "scenevo.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideProjectDao(db: ScenevoDatabase) = db.projectDao()
    @Provides fun provideAssetDao(db: ScenevoDatabase) = db.assetDao()
    @Provides fun provideRenderJobDao(db: ScenevoDatabase) = db.renderJobDao()

    @Provides
    @Singleton
    fun provideSettingsDataSource(@ApplicationContext context: Context) =
        SettingsDataSource(context)
}
