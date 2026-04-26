package com.Azelmods.App.domain.usecase

import com.Azelmods.App.data.backup.BackupInfo
import com.Azelmods.App.data.backup.BackupManager
import com.Azelmods.App.data.backup.RestoreResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for restoring encrypted backups.
 *
 * Handles:
 * - Download from storage location
 * - Decryption with password
 * - Data restoration (messages, contacts, settings)
 * - Progress tracking
 * - Rollback on error
 *
 * Requirements: 10.3, 10.4
 */
class RestoreBackupUseCase @Inject constructor(
    private val backupManager: BackupManager
) {

    /**
     * Restores an encrypted backup.
     *
     * @param backupInfo The backup to restore
     * @param password The decryption password
     * @return Flow emitting restore progress and result
     */
    operator fun invoke(
        backupInfo: BackupInfo,
        password: String
    ): Flow<RestoreResult> {
        return backupManager.restoreBackup(backupInfo, password)
    }
}
