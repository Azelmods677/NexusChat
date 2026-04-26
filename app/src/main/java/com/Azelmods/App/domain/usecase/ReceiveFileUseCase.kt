package com.Azelmods.App.domain.usecase

import com.Azelmods.App.data.file.FileDownloadState
import com.Azelmods.App.data.file.FileMetadata
import com.Azelmods.App.data.file.SecureFileManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for receiving files securely in chats.
 *
 * Handles:
 * - File download
 * - Decryption
 * - Integrity verification
 * - Progress tracking
 *
 * Requirements: 6.1, 6.2, 6.3
 */
class ReceiveFileUseCase @Inject constructor(
    private val secureFileManager: SecureFileManager
) {

    /**
     * Receives a file securely.
     *
     * @param metadata File metadata including URL and encryption keys
     * @return Flow emitting download progress and result
     */
    operator fun invoke(
        metadata: FileMetadata
    ): Flow<FileDownloadState> {
        return secureFileManager.downloadFile(metadata)
    }
}
