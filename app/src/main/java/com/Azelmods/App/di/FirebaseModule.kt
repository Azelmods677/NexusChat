package com.Azelmods.App.di

import android.content.Context
import com.Azelmods.App.data.firebase.FirebaseManager
import com.Azelmods.App.data.firebase.FirebasePersistenceGuard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    
    // REMOVED: FirebaseFirestore - This app uses Realtime Database ONLY
    
    @Provides
    @Singleton
    fun provideFirebaseDatabase(
        @ApplicationContext context: Context
    ): FirebaseDatabase {
        val database = FirebaseDatabase.getInstance()
        // Punto ÚNICO y más temprano donde se toca la persistencia offline.
        // El guard activa la persistencia con arranque seguro: si el arranque
        // anterior crasheó con la persistencia activa (SqlPersistenceStorageEngine),
        // entra en modo seguro, no la activa y purga escrituras pendientes
        // envenenadas, evitando el bucle de crashes al entrar a la app.
        FirebasePersistenceGuard.enable(context, database)
        return database
    }
    
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()
    
    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()

    @Provides
    @Singleton
    fun provideChatBackgroundRepository(
        database: FirebaseDatabase
    ): com.Azelmods.App.data.repository.ChatBackgroundRepository {
        return com.Azelmods.App.data.repository.ChatBackgroundRepository(database)
    }

    @Provides
    @Singleton
    fun provideFirebaseManager(
        auth: FirebaseAuth,
        database: FirebaseDatabase,
        storage: FirebaseStorage,
        messaging: FirebaseMessaging
    ): FirebaseManager = FirebaseManager(auth, database, storage, messaging)
}
