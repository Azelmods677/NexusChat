package com.Azelmods.App.di

import android.content.Context
import androidx.credentials.CredentialManager
import com.Azelmods.App.data.demo.DemoAccountManager
import com.Azelmods.App.data.local.AppDatabase
import com.Azelmods.App.data.local.CacheManager
import com.Azelmods.App.data.preferences.TutorialPreferences
import com.Azelmods.App.data.preferences.UserPreferences
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton
import com.Azelmods.App.R

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context
    
    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
    
    @Provides
    @Singleton
    fun provideCredentialManager(@ApplicationContext context: Context): CredentialManager {
        return CredentialManager.create(context)
    }
    
    @Provides
    @Singleton
    fun provideGoogleClientId(@ApplicationContext context: Context): String {
        return context.getString(R.string.default_web_client_id)
    }
    
    @Provides
    @Singleton
    fun provideTutorialPreferences(@ApplicationContext context: Context): TutorialPreferences {
        return TutorialPreferences(context)
    }
    
    @Provides
    @Singleton
    fun provideAppBackgroundManager(
        @ApplicationContext context: Context
    ): com.Azelmods.App.data.manager.AppBackgroundManager {
        return com.Azelmods.App.data.manager.AppBackgroundManager(context)
    }
    
    @Provides
    @Singleton
    fun provideBotPreferences(
        @ApplicationContext context: Context
    ): com.Azelmods.App.data.preferences.BotPreferences {
        return com.Azelmods.App.data.preferences.BotPreferences(context)
    }
    
    @Provides
    @Singleton
    fun provideInternalBotRepository(
        botPreferences: com.Azelmods.App.data.preferences.BotPreferences
    ): com.Azelmods.App.data.repository.InternalBotRepository {
        return com.Azelmods.App.data.repository.InternalBotRepository(botPreferences)
    }

    // ── Room Database (offline cache) ───────────────────────

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideCacheManager(
        db: AppDatabase
    ): CacheManager {
        return CacheManager(db)
    }
}
