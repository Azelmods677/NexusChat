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
        // Punto ÚNICO donde se habilita la persistencia offline de Firebase.
        // Debe ejecutarse antes de cualquier otro uso de la instancia; gracias a
        // la inyección temprana de Hilt (DemoAccountManager inyecta FirebaseDatabase
        // durante Application.onCreate), este provider corre primero. Aun así lo
        // envolvemos en try/catch: si algún path llamara a FirebaseDatabase.getInstance()
        // y usara una referencia ANTES que este provider, setPersistenceEnabled()
        // lanzaría DatabaseException; preferimos loguear y seguir (sin cache offline)
        // antes que un crash duro en el arranque.
        try {
            database.setPersistenceEnabled(true)
            // Cap del archivo SQLite de persistencia (SqlPersistenceStorageEngine).
            // Evita crecimiento sin límite del cache que presiona las escrituras
            // (saveUserOverwrite) y reduce la superficie de fallos de disco.
            database.setPersistenceCacheSizeBytes(20L * 1024 * 1024) // 20 MB
        } catch (e: Exception) {
            android.util.Log.e(
                "FirebaseModule",
                "No se pudo habilitar la persistencia offline (¿uso previo de FirebaseDatabase?)",
                e
            )
        }
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
