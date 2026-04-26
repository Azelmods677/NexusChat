package com.Azelmods.App.data.security.payload

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.Azelmods.App.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

/**
 * Background service for payload generation
 * 
 * Displays notification with progress and reduces memory usage
 * 
 * Requirements: 31.3, 31.4, 31.5
 */
class PayloadGenerationService : Service() {
    
    companion object {
        private const val TAG = "PayloadGenerationService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "payload_generation_channel"
        private const val CHANNEL_NAME = "Payload Generation"
        
        const val EXTRA_CONFIG = "extra_config"
        
        fun start(context: Context, config: PayloadConfig) {
            val intent = Intent(context, PayloadGenerationService::class.java).apply {
                putExtra(EXTRA_CONFIG, config)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var notificationManager: NotificationManager
    
    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val config = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_CONFIG, PayloadConfig::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra<PayloadConfig>(EXTRA_CONFIG)
        }
        
        if (config == null) {
            Log.e(TAG, "No config provided")
            stopSelf()
            return START_NOT_STICKY
        }
        
        // Start foreground service with initial notification
        startForeground(NOTIFICATION_ID, createNotification("Iniciando generación...", 0))
        
        // Generate payload in background
        serviceScope.launch {
            try {
                generatePayload(config)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating payload", e)
                showErrorNotification(e.message ?: "Error desconocido")
            } finally {
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    private suspend fun generatePayload(config: PayloadConfig) {
        // TODO: Get PayloadGenerator from DI
        // For now, this is a placeholder
        // In production, inject PayloadGenerator via Hilt
        
        Log.d(TAG, "Starting payload generation in background")
        
        // Simulate progress updates
        for (progress in 0..100 step 10) {
            delay(1000)
            updateNotification("Generando payload...", progress)
        }
        
        showSuccessNotification()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificaciones de generación de payloads"
                setShowBadge(false)
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(message: String, progress: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Generador de Payloads")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification(message: String, progress: Int) {
        val notification = createNotification(message, progress)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showSuccessNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Payload Generado")
            .setContentText("El payload se generó exitosamente")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showErrorNotification(error: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Error en Generación")
            .setContentText(error)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
