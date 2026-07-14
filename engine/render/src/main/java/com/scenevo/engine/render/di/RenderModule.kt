package com.scenevo.engine.render.di

import com.scenevo.engine.render.FfmpegTransitionBridge
import com.scenevo.engine.render.Media3VideoRenderer
import com.scenevo.engine.render.NoOpFfmpegTransitionBridge
import com.scenevo.engine.render.VideoRenderer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RenderModule {
    @Binds
    @Singleton
    abstract fun bindVideoRenderer(impl: Media3VideoRenderer): VideoRenderer

    @Binds
    @Singleton
    abstract fun bindFfmpegBridge(impl: NoOpFfmpegTransitionBridge): FfmpegTransitionBridge
}
