package com.dg.electricitycounter.di

import android.content.Context
import com.dg.electricitycounter.data.local.PreferencesHelper
import com.dg.electricitycounter.data.repository.ReadingRepositoryImpl
import com.dg.electricitycounter.domain.repository.ReadingRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    
    @Binds
    @Singleton
    abstract fun bindReadingRepository(
        impl: ReadingRepositoryImpl
    ): ReadingRepository
}

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    
    @Provides
    @Singleton
    fun providePreferencesHelper(
        @ApplicationContext context: Context
    ): PreferencesHelper {
        return PreferencesHelper(context)
    }
}
