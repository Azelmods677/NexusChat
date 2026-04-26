package com.Azelmods.App.domain.usecase

import android.net.Uri
import com.Azelmods.App.data.file.FileUploadState
import com.Azelmods.App.data.file.SecureFileManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for sending files securely in chats.
 *
 * Handles:
 * - File encryption
 * - Upload to Firebase Storage
 * - Progress tracking
 * - Error handling
 *
 * Requirements: 6.1, 6.2
 */
class SendFileUseCase @Inject constructor(
    private val secureFileManager: SecureFileManager
) {

    /**
     * Sends a file securely.
     *
     * @param fileUri The local file URI to send
     * @param chatId The chat ID
     * @return Flow emitting upload progress and result
     */
    operator fun invoke(
        fileUri: Uri,
        chatId: String
    ): Flow<FileUploadState> {
        return secureFileManager.uploadFile(fileUri, chatId)
    }
}
