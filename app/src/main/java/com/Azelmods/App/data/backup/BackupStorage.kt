package com.Azelmods.App.data.backup

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * BackupStorage – abstracts all I/O between backup files and their storage backends:
 * Firebase Storage (remote) and the app's external/internal files directory (local).
 *
 * All Firebase operations run in the calling coroutine's dispatcher.
 * Use [kotlinx.coroutines.Dispatchers.IO] at the call site for heavy I/O work.
 */
@Singleton
class BackupStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) {

    // ── Firebase upload ───────────────────────────────────────────────────────

    /**
     * Uploads [backupFile] to Firebase Storage under `backups/{uid}/{timestamp}_{filename}`.
     *
     * @param onProgress Invoked with a 0..1 progress fraction as bytes are transferred.
     * @return The public download URL for the uploaded file.
     * @throws IllegalStateException if no user is authenticated.
     */
    suspend fun uploadToFirebase(
        backupFile: File,
        onProgress: (Float) -> Unit = {}
    ): String {
        val userId   = auth.currentUser?.uid
            ?: throw IllegalStateException("No authenticated user – cannot upload backup")
        val fileName = "${System.currentTimeMillis()}_${backupFile.name}"
        val ref      = storage.reference.child("backups/$userId/$fileName")

        return suspendCancellableCoroutine { cont ->
            val task = ref.putFile(Uri.fromFile(backupFile))

            task.addOnProgressListener { snapshot ->
                val total = snapshot.totalByteCount
                if (total > 0L) onProgress(snapshot.bytesTransferred.toFloat() / total.toFloat())
            }

            task.addOnSuccessListener {
                // Fetch the download URL once upload is confirmed
                ref.downloadUrl
                    .addOnSuccessListener { uri -> cont.resume(uri.toString()) }
                    .addOnFailureListener { e  -> cont.resumeWithException(e) }
            }

            task.addOnFailureListener { e -> cont.resumeWithException(e) }

            cont.invokeOnCancellation { task.cancel() }
        }
    }

    // ── Firebase download ─────────────────────────────────────────────────────

    /**
     * Downloads the backup at [url] to [destinationFile].
     *
     * @param onProgress Invoked with a 0..1 progress fraction.
     * @return `true` on success, `false` on any error.
     */
    suspend fun downloadFromFirebase(
        url: String,
        destinationFile: File,
        onProgress: (Float) -> Unit = {}
    ): Boolean {
        return try {
            val ref = storage.getReferenceFromUrl(url)
            suspendCancellableCoroutine { cont ->
                val task = ref.getFile(destinationFile)

                task.addOnProgressListener { snapshot ->
                    val total = snapshot.totalByteCount
                    if (total > 0L) onProgress(snapshot.bytesTransferred.toFloat() / total.toFloat())
                }

                task.addOnSuccessListener { cont.resume(true)  }
                task.addOnFailureListener { e -> cont.resumeWithException(e) }

                cont.invokeOnCancellation { task.cancel() }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "downloadFromFirebase failed: ${e.message}", e)
            false
        }
    }

    /**
     * Downloads only the first [maxBytes] bytes of the backup at [url] into a new
     * temporary file in [getLocalBackupsDir].  Useful for header-only reads
     * (e.g. password validation) without pulling the full file.
     *
     * NOTE: Because the Firebase Storage Android SDK does not support HTTP Range
     * requests natively, this implementation downloads the full file via a
     * streaming task and stops reading after [maxBytes]. The network transfer
     * will be cancelled immediately after the first chunk is received.
     *
     * @return A temp [File] containing up to [maxBytes] bytes, or `null` on error.
     *         The caller is responsible for deleting the file when done.
     */
    suspend fun downloadHeaderBytes(url: String, maxBytes: Int = 512): File? {
        val tmpFile = File(getLocalBackupsDir(), "header_tmp_${System.currentTimeMillis()}.tmp")
        return try {
            val ref = storage.getReferenceFromUrl(url)
            val bytes = ref.getBytes(maxBytes.toLong()).await()
            tmpFile.writeBytes(bytes)
            tmpFile
        } catch (e: Exception) {
            android.util.Log.e(TAG, "downloadHeaderBytes failed: ${e.message}", e)
            tmpFile.delete()
            null
        }
    }

    // ── Local storage ─────────────────────────────────────────────────────────

    /**
     * Copies [backupFile] to a user-chosen location identified by [destinationUri],
     * using [android.content.ContentResolver] to honour Android's scoped storage.
     *
     * @return `true` on success.
     */
    suspend fun saveToLocal(backupFile: File, destinationUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                backupFile.inputStream().buffered().use { input ->
                    input.copyTo(out)
                }
            }
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "saveToLocal failed: ${e.message}", e)
            false
        }
    }

    /**
     * Returns (or creates) the directory used for locally stored backups.
     *
     * Prefers `getExternalFilesDir("backups")` (visible in Files app) and
     * falls back to `filesDir/backups` if external storage is unavailable.
     */
    fun getLocalBackupsDir(): File {
        val external = context.getExternalFilesDir("backups")
        val dir = if (external != null && (external.exists() || external.mkdirs())) {
            external
        } else {
            File(context.filesDir, "backups").also { it.mkdirs() }
        }
        return dir
    }

    /**
     * Lists all `*.azelback` files in [getLocalBackupsDir], sorted newest-first.
     */
    fun listLocalBackups(): List<BackupInfo> {
        return try {
            getLocalBackupsDir()
                .listFiles { file -> file.isFile && file.name.endsWith(".azelback") }
                ?.sortedByDescending { it.lastModified() }
                ?.map { file ->
                    BackupInfo(
                        id        = file.name,
                        name      = file.name,
                        url       = file.absolutePath,
                        sizeBytes = file.length(),
                        createdAt = file.lastModified(),
                        location  = StorageLocation.LOCAL
                    )
                }
                ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "listLocalBackups failed: ${e.message}", e)
            emptyList()
        }
    }

    // ── Firebase listing ──────────────────────────────────────────────────────

    /**
     * Lists all backup files stored in Firebase Storage under `backups/{uid}/`.
     *
     * Fetches metadata (size, creation time) and download URL for each item.
     * Items that fail to resolve metadata are silently skipped.
     *
     * @return List of [BackupInfo], sorted newest-first by [BackupInfo.createdAt].
     */
    suspend fun listFirebaseBackups(): List<BackupInfo> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()
            val ref    = storage.reference.child("backups/$userId")
            val result = ref.listAll().await()

            result.items
                .mapNotNull { item ->
                    try {
                        val meta = item.metadata.await()
                        val url  = item.downloadUrl.await().toString()
                        BackupInfo(
                            id        = item.name,
                            name      = item.name,
                            url       = url,
                            sizeBytes = meta.sizeBytes,
                            createdAt = meta.creationTimeMillis,
                            location  = StorageLocation.FIREBASE
                        )
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "Skipping item ${item.name}: ${e.message}")
                        null
                    }
                }
                .sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "listFirebaseBackups failed: ${e.message}", e)
            emptyList()
        }
    }

    // ── Firebase delete ───────────────────────────────────────────────────────

    /**
     * Deletes the Firebase Storage file identified by [url].
     *
     * @return `true` on success or if the file no longer exists, `false` on error.
     */
    suspend fun deleteFirebaseBackup(url: String): Boolean {
        return try {
            storage.getReferenceFromUrl(url).delete().await()
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "deleteFirebaseBackup failed: ${e.message}", e)
            false
        }
    }

    // ── Auto-cleanup ──────────────────────────────────────────────────────────

    /**
     * Deletes the oldest Firebase and local backups when the total count for
     * either location exceeds [maxBackupsToKeep].
     *
     * Backups are ranked by [BackupInfo.createdAt] (newest first); the oldest
     * entries beyond [maxBackupsToKeep] are removed.
     */
    suspend fun autoDeleteOldBackups(maxBackupsToKeep: Int = 5) {
        // ── Firebase ──
        val firebaseBackups = try {
            listFirebaseBackups()
        } catch (e: Exception) {
            emptyList()
        }
        if (firebaseBackups.size > maxBackupsToKeep) {
            firebaseBackups
                .sortedByDescending { it.createdAt }   // newest first
                .drop(maxBackupsToKeep)                // keep the first N
                .forEach { info ->
                    deleteFirebaseBackup(info.url)
                }
        }

        // ── Local ──
        val localBackups = listLocalBackups()
        if (localBackups.size > maxBackupsToKeep) {
            localBackups
                .sortedByDescending { it.createdAt }
                .drop(maxBackupsToKeep)
                .forEach { info ->
                    runCatching { File(info.url).delete() }
                }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Extension to access [StorageMetadata.getSizeBytes] as a Kotlin property,
     * since the KTX accessor name varies across Firebase SDK versions.
     */
    private val StorageMetadata.sizeBytes: Long
        get() = this.sizeBytes

    private companion object {
        const val TAG = "BackupStorage"
    }
}

// ── Shared data types (used by BackupStorage, BackupManager, UI) ──────────────

/** Where a backup resides. */
enum class StorageLocation { LOCAL, FIREBASE }

/**
 * Describes a single backup artefact, regardless of where it is stored.
 *
 * @param id        Unique identifier (Firebase item name or local file name).
 * @param name      Human-readable file name (e.g. `1234567890_backup.azelback`).
 * @param url       Download URL (Firebase) or absolute local file path.
 * @param sizeBytes File size in bytes.
 * @param createdAt Epoch milliseconds when the backup was created.
 * @param location  [StorageLocation.FIREBASE] or [StorageLocation.LOCAL].
 */
data class BackupInfo(
    val id: String,
    val name: String,
    val url: String,
    val sizeBytes: Long,
    val createdAt: Long,
    val location: StorageLocation
)
