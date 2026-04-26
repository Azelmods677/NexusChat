package com.Azelmods.App.di

import android.content.Context
import com.Azelmods.App.data.security.tor.TorService
import com.Azelmods.App.data.security.tor.TorServiceManager
import com.Azelmods.App.data.security.tor.TorServiceManagerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for security-related dependencies
 * 
 * Provides:
 * - TorService for embedded Tor integration
 * - TorServiceManager for Tor integration
 * - PayloadGenerator for payload generation
 * - TorPreferences for settings persistence (automatically provided via @Inject constructor)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {
    
    @Binds
    @Singleton
    abstract fun bindTorServiceManager(
        torServiceManagerImpl: TorServiceManagerImpl
    ): TorServiceManager
    
    @Binds
    @Singleton
    abstract fun bindPayloadGenerator(
        payloadGeneratorImpl: com.Azelmods.App.data.security.payload.PayloadGeneratorImpl
    ): com.Azelmods.App.data.security.payload.PayloadGenerator
    
    companion object {
        @Provides
        @Singleton
        fun provideTorService(@ApplicationContext context: Context): TorService {
            return TorService(context)
        }
    }
}
