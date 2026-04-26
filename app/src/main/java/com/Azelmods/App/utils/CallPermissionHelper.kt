package com.Azelmods.App.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.Azelmods.App.services.CallService

/**
 * Helper for managing call permissions in Jetpack Compose
 * Handles Android 16 (API 36) permission requirements
 */
object CallPermissionHelper {
    
    /**
     * Required permissions for audio calls
     */
    val AUDIO_CALL_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.MODIFY_AUDIO_SETTINGS
    )
    
    /**
     * Required permissions for video calls
     */
    val VIDEO_CALL_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.MODIFY_AUDIO_SETTINGS
    )
    
    /**
     * Additional permissions for Android 12+ (API 31+)
     */
    val BLUETOOTH_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        emptyArray()
    }
    
    /**
     * Check if audio call permissions are granted
     */
    fun hasAudioCallPermissions(context: Context): Boolean {
        return AUDIO_CALL_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Check if video call permissions are granted
     */
    fun hasVideoCallPermissions(context: Context): Boolean {
        return VIDEO_CALL_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Start audio call foreground service
     */
    fun startAudioCallService(context: Context) {
        val intent = Intent(context, CallService::class.java).apply {
            action = CallService.ACTION_START_CALL
            putExtra(CallService.EXTRA_CALL_TYPE, CallService.CALL_TYPE_AUDIO)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
    
    /**
     * Start video call foreground service
     */
    fun startVideoCallService(context: Context) {
        val intent = Intent(context, CallService::class.java).apply {
            action = CallService.ACTION_START_CALL
            putExtra(CallService.EXTRA_CALL_TYPE, CallService.CALL_TYPE_VIDEO)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
    
    /**
     * Stop call foreground service
     */
    fun stopCallService(context: Context) {
        val intent = Intent(context, CallService::class.java).apply {
            action = CallService.ACTION_END_CALL
        }
        context.startService(intent)
    }
}

/**
 * Composable function to handle audio call permissions
 * Usage:
 * ```
 * val audioCallPermissionState = rememberAudioCallPermissionState(
 *     onPermissionsGranted = { 
 *         // Start audio call
 *     },
 *     onPermissionsDenied = {
 *         // Show error message
 *     }
 * )
 * 
 * Button(onClick = { audioCallPermissionState.launchPermissionRequest() }) {
 *     Text("Start Audio Call")
 * }
 * ```
 */
@Composable
fun rememberAudioCallPermissionState(
    onPermissionsGranted: () -> Unit,
    onPermissionsDenied: () -> Unit
): CallPermissionState {
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            onPermissionsGranted()
        } else {
            onPermissionsDenied()
        }
    }
    
    return remember {
        CallPermissionState(
            context = context,
            permissions = CallPermissionHelper.AUDIO_CALL_PERMISSIONS + CallPermissionHelper.BLUETOOTH_PERMISSIONS,
            launcher = launcher,
            onPermissionsGranted = onPermissionsGranted
        )
    }
}

/**
 * Composable function to handle video call permissions
 * Usage:
 * ```
 * val videoCallPermissionState = rememberVideoCallPermissionState(
 *     onPermissionsGranted = { 
 *         // Start video call
 *     },
 *     onPermissionsDenied = {
 *         // Show error message
 *     }
 * )
 * 
 * Button(onClick = { videoCallPermissionState.launchPermissionRequest() }) {
 *     Text("Start Video Call")
 * }
 * ```
 */
@Composable
fun rememberVideoCallPermissionState(
    onPermissionsGranted: () -> Unit,
    onPermissionsDenied: () -> Unit
): CallPermissionState {
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            onPermissionsGranted()
        } else {
            onPermissionsDenied()
        }
    }
    
    return remember {
        CallPermissionState(
            context = context,
            permissions = CallPermissionHelper.VIDEO_CALL_PERMISSIONS + CallPermissionHelper.BLUETOOTH_PERMISSIONS,
            launcher = launcher,
            onPermissionsGranted = onPermissionsGranted
        )
    }
}

/**
 * State holder for call permissions
 */
class CallPermissionState(
    private val context: Context,
    private val permissions: Array<String>,
    private val launcher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    private val onPermissionsGranted: () -> Unit
) {
    
    /**
     * Check if all permissions are granted
     */
    fun hasPermissions(): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Launch permission request or execute action if already granted
     */
    fun launchPermissionRequest() {
        if (hasPermissions()) {
            onPermissionsGranted()
        } else {
            launcher.launch(permissions)
        }
    }
}
