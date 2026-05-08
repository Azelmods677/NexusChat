package com.Azelmods.App.data.backup

sealed class BackupResult {
    data class Progress(val percentage: Int, val message: String) : BackupResult()
    data class Success(val backupId: String) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

sealed class RestoreResult {
    data class Progress(val percentage: Int, val message: String) : RestoreResult()
    object Success : RestoreResult()
    data class Error(val message: String) : RestoreResult()
}
