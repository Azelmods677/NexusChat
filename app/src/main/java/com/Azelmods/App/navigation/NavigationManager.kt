package com.Azelmods.App.navigation

import androidx.navigation.NavController
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * NAVIGATION MANAGER — ENTERPRISE  2026
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Gestiona navegación segura sin crashes por:
 * • IllegalStateException en NavController
 * • Back stack corruption
 * • NavController usado fuera del scope correcto
 * 
 * @since 2026
 * @version 3.0.0 
 * @author AzelMods677
 * ═══════════════════════════════════════════════════════════════════════════
 */

sealed class NavigationEvent {
    data class Navigate(val route: String) : NavigationEvent()
    data class NavigateWithArgs(val route: String, val args: Map<String, Any>) : NavigationEvent()
    object NavigateBack : NavigationEvent()
    data class NavigateBackTo(val route: String, val inclusive: Boolean = false) : NavigationEvent()
}

@Singleton
class NavigationManager @Inject constructor() {
    
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val navigationEvent = _navigationEvent.asSharedFlow()
    
    fun navigate(event: NavigationEvent) {
        _navigationEvent.tryEmit(event)
    }
    
    fun navigate(route: String) {
        navigate(NavigationEvent.Navigate(route))
    }
    
    fun navigateBack() {
        navigate(NavigationEvent.NavigateBack)
    }
}

/**
 * Extension function para navegación segura
 * Previene crashes por NavController en estado inválido
 */
fun NavController.safeNavigate(route: String) {
    try {
        currentBackStackEntry?.let {
            navigate(route) {
                launchSingleTop = true
                restoreState = true
            }
        }
    } catch (e: IllegalArgumentException) {
        android.util.Log.e("NavController", "Route not found: $route", e)
    } catch (e: IllegalStateException) {
        android.util.Log.e("NavController", "NavController not ready: ${e.message}")
    } catch (e: Exception) {
        android.util.Log.e("NavController", "Navigation error", e)
    }
}

/**
 * Navegación segura hacia atrás
 */
fun NavController.safePopBackStack(): Boolean {
    return try {
        popBackStack()
    } catch (e: Exception) {
        android.util.Log.e("NavController", "PopBackStack error", e)
        false
    }
}

/**
 * Navegación segura a destino específico
 */
fun NavController.safePopBackStack(route: String, inclusive: Boolean = false): Boolean {
    return try {
        popBackStack(route, inclusive)
    } catch (e: Exception) {
        android.util.Log.e("NavController", "PopBackStack to $route error", e)
        false
    }
}
