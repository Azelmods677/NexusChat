package com.Azelmods.App.domain.usecase

import com.Azelmods.App.data.backup.BackupManager
import com.Azelmods.App.data.backup.BackupResult
import com.Azelmods.App.data.backup.StorageLocation
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for creating encrypted backups.
 *
 * Handles:
 * - Data collection (messages, contacts, settings)
 * - Encryption with password
 * - Upload to storage location
 * - Progress tracking
 *
 * Requirements: 10.1, 10.2
 */
class CreateBackupUseCase @Inject constructor(
    private val backupManager: BackupManager
) {

    /**
     * Creates an encrypted backup.
     *
     * @param password The encryption password
     * @param location Where to store the backup (LOCAL or FIREBASE)
     * @param includeMedia Whether to include media files
     * @return Flow emitting backup progress and result
     */
    operator fun invoke(
        password: String,
        location: StorageLocation,
        includeMedia: Boolean = false
    ): Flow<BackupResult> {
        return backupManager.createBackup(password, location, includeMedia)
    }
}
