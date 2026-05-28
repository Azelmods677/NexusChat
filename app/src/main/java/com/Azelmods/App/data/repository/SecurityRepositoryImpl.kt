package com.Azelmods.App.data.repository

import android.content.Context
import com.Azelmods.App.data.security.tor.OrbotDetector
import com.Azelmods.App.data.security.tor.TorService
import com.Azelmods.App.data.security.tor.TorState
import com.Azelmods.App.domain.repository.SecurityRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de SecurityRepository que delega operaciones
 * a TorService (detección de Orbot) en lugar de un TorServiceManager embebido.
 */
@Singleton
class SecurityRepositoryImpl @Inject constructor(
    private val torService: TorService,
    @ApplicationContext private val context: Context
) : SecurityRepository {

    override fun startTor(): Flow<TorState> {
        torService.startTor()
        return torService.torState
    }

    override suspend fun stopTor() {
        torService.stopTor()
    }

    override fun getTorState(): StateFlow<TorState> {
        return torService.torState
    }

    override fun getTorStatus(): String {
        return OrbotDetector.getStatus(context)
    }
}
