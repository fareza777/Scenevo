package com.scenevo.engine.tts.di

import com.scenevo.domain.repository.VoicePackRepository
import com.scenevo.engine.tts.NarrationEngine
import com.scenevo.engine.tts.PiperVoicePackRepository
import com.scenevo.engine.tts.PreferLocalNarrationEngine
import com.scenevo.engine.tts.SmartNarrationEngine
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
    abstract fun bindSmartNarrationEngine(impl: PreferLocalNarrationEngine): SmartNarrationEngine

    @Binds
    @Singleton
    abstract fun bindNarrationEngine(impl: PreferLocalNarrationEngine): NarrationEngine

    @Binds
    @Singleton
    abstract fun bindVoicePackRepository(impl: PiperVoicePackRepository): VoicePackRepository
}
