package com.scenevo.engine.stock.di

import com.scenevo.domain.repository.StockRepository
import com.scenevo.engine.stock.PexelsStockRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StockModule {
    @Binds
    @Singleton
    abstract fun bindStockRepository(impl: PexelsStockRepository): StockRepository
}
