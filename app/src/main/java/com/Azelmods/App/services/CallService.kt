package com.Azelmods.App.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.Azelmods.App.MainActivity
import com.Azelmods.App.R
import com.Azelmods.App.data.model.CallType
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CallService : Service() {
    
    private val TAG = "CallService"
    private val CHANNEL_ID = "call_service_channel"
    private val NOTIFICATION_ID = 1001
    
    companion object {
        const val ACTION_START_CALL = "ACTION_START_CALL"
        const val ACTION_END_CALL = "ACTION_END_CALL"
        const val EXTRA_CALL_ID = "EXTRA_CALL_ID"
        const val EXTRA_CALL_TYPE = "EXTRA_CALL_TYPE"
        const val EXTRA_CONTACT_NAME = "EXTRA_CONTACT_NAME"
        const val CALL_TYPE_AUDIO = "audio"
        const val CALL_TYPE_VIDEO = "video"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "CallService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CALL -> {
                val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: ""
                val callType = intent.getStringExtra(EXTRA_CALL_TYPE) ?: "audio"
                val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: "Unknown"
                
                startForegroundService(callId, callType, contactName)
            }
            ACTION_END_CALL -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    private fun startForegroundService(callId: String, callType: String, contactName: String) {
        val notification = createNotification(callId, callType, contactName)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ (API 34+)
            val foregroundServiceType = when (callType) {
                "video" -> ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or 
                          ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                          ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                else -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                       ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            }
            startForeground(NOTIFICATION_ID, notification, foregroundServiceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        Log.d(TAG, "Foreground service started for call: $callId")
    }
    
    private fun createNotification(callId: String, callType: String, contactName: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val endCallIntent = Intent(this, CallService::class.java).apply {
            action = ACTION_END_CALL
        }
        
        val endCallPendingIntent = PendingIntent.getService(
            this,
            1,
            endCallIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val callTypeText = if (callType == "video") "Video call" else "Audio call"
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$callTypeText in progress")
            .setContentText("Talking with $contactName")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "End Call",
                endCallPendingIntent
            )
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notification for ongoing calls"
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "CallService destroyed")
    }
}
