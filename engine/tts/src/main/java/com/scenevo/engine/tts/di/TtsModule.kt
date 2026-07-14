package com.scenevo.engine.tts.di

import com.scenevo.engine.tts.AndroidTtsNarrationEngine
import com.scenevo.engine.tts.NarrationEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TtsModule {
    @Binds
    @Singleton
    abstract fun bindNarrationEngine(impl: AndroidTtsNarrationEngine): NarrationEngine
}
