package com.Azelmods.App.di

import android.content.Context
import com.Azelmods.App.data.security.tor.FirebaseProxyConfigurator
import com.Azelmods.App.data.security.tor.TorDnsResolver
import com.Azelmods.App.data.security.tor.TorService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
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
 * - TorService for Orbot detection and proxy status
 * - TorDnsResolver for DNS-over-Tor (prevents DNS leaks)
 * - FirebaseProxyConfigurator for Firebase-over-Tor
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideTorService(
        @ApplicationContext context: Context,
        torDnsResolver: TorDnsResolver,
        firebaseProxyConfigurator: FirebaseProxyConfigurator
    ): TorService {
        return TorService(context, torDnsResolver, firebaseProxyConfigurator)
    }

    @Provides
    @Singleton
    fun provideTorDnsResolver(): TorDnsResolver {
        return TorDnsResolver()
    }

    @Provides
    @Singleton
    fun provideFirebaseProxyConfigurator(
        database: FirebaseDatabase,
        auth: FirebaseAuth,
        storage: FirebaseStorage
    ): FirebaseProxyConfigurator {
        return FirebaseProxyConfigurator(database, auth, storage)
    }
}
