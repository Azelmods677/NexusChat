package com.Azelmods.App.data.work

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.Azelmods.App.data.local.AppDatabase
import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SendPendingMessagesWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val databaseRepository: RealtimeDatabaseRepository,
    private val appDatabase: AppDatabase
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "send_pending_messages"
        private const val TAG = "SendPendingWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            if (!isNetworkAvailable(applicationContext)) {
                Log.d(TAG, "No network available, retrying later")
                return@withContext Result.retry()
            }

            val pendingMessages = appDatabase.pendingMessageDao().getAll().collect { list ->
                if (list.isEmpty()) return@collect

                Log.d(TAG, "Sending ${list.size} pending messages")
                list.forEach { msg ->
                    try {
                        if (msg.isEphemeral) {
                            databaseRepository.sendEphemeralMessage(
                                chatId = msg.chatId,
                                content = msg.content,
                                replyTo = msg.replyTo,
                                isViewOnce = msg.isViewOnce,
                                selfDestructDuration = msg.selfDestructDuration
                            )
                        } else {
                            databaseRepository.sendMessage(
                                chatId = msg.chatId,
                                content = msg.content,
                                replyTo = msg.replyTo
                            )
                        }
                        appDatabase.pendingMessageDao().deleteById(msg.id)
                        Log.d(TAG, "Sent pending message ${msg.id}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send pending message ${msg.id}", e)
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed", e)
            Result.retry()
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
