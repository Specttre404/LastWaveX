package com.lastwave.app.di

import com.lastwave.app.data.ai.AiRepository
import com.lastwave.app.data.ai.AiRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindAiRepository(
        impl: AiRepositoryImpl,
    ): AiRepository
}
