package com.Azelmods.App.di

import com.Azelmods.App.data.firebase.FirebaseManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
    fun provideFirebaseDatabase(): FirebaseDatabase {
        val database = FirebaseDatabase.getInstance()
        database.setPersistenceEnabled(true)
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
    fun provideFirebaseManager(
        auth: FirebaseAuth,
        database: FirebaseDatabase,
        storage: FirebaseStorage,
        messaging: FirebaseMessaging
    ): FirebaseManager = FirebaseManager(auth, database, storage, messaging)
}
