package com.Azelmods.App.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
class StorageRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    /**
     * Tamaño en bytes de un content:// URI (−1 si no se puede determinar).
     * Permite validar límites antes de subir a Storage.
     */
    fun getFileSizeBytes(uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
        } catch (e: Exception) {
            -1L
        }
    }
    
    /**
     * Upload image to Firebase Storage
     * Pattern: chats/{chatId}/images/{timestamp}.jpg
     */
    suspend fun uploadChatImage(imageUri: Uri, chatId: String): String = suspendCoroutine { continuation ->
        try {
            // Verify user is authenticated
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                continuation.resumeWithException(
                    Exception("User not authenticated. Please log in to upload files.")
                )
                return@suspendCoroutine
            }
            
            val timestamp = System.currentTimeMillis()
            val storageRef = storage.reference
            val fileRef = storageRef.child("chats/$chatId/images/$timestamp.jpg")
            
            fileRef.putFile(imageUri)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        continuation.resume(uri.toString())
                    }.addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
    
    /**
     * Upload audio to Firebase Storage
     * Pattern: chats/{chatId}/audio/{timestamp}.aac
     */
    suspend fun uploadChatAudio(audioUri: Uri, chatId: String): String = suspendCoroutine { continuation ->
        try {
            // Verify user is authenticated
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                continuation.resumeWithException(
                    Exception("User not authenticated. Please log in to upload files.")
                )
                return@suspendCoroutine
            }
            
            val timestamp = System.currentTimeMillis()
            val storageRef = storage.reference
            val fileRef = storageRef.child("chats/$chatId/audio/$timestamp.aac")
            
            fileRef.putFile(audioUri)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        continuation.resume(uri.toString())
                    }.addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
    
    /**
     * Upload video to Firebase Storage
     * Pattern: chats/{chatId}/videos/{timestamp}.mp4
     */
    suspend fun uploadChatVideo(videoUri: Uri, chatId: String): String = suspendCoroutine { continuation ->
        try {
            // Verify user is authenticated
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                continuation.resumeWithException(
                    Exception("User not authenticated. Please log in to upload files.")
                )
                return@suspendCoroutine
            }
            
            val timestamp = System.currentTimeMillis()
            val storageRef = storage.reference
            val fileRef = storageRef.child("chats/$chatId/videos/$timestamp.mp4")
            
            fileRef.putFile(videoUri)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        continuation.resume(uri.toString())
                    }.addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
    
    /**
     * Upload an app-wide background (image or video) for the current user.
     * Pattern: user_backgrounds/{userId}/{timestamp}.{jpg|mp4}
     *
     * Used by the APP-scope wallpaper so the reference survives process death and
     * permission revocation — a local content:// URI does not, which is why picking
     * an image/video for the background "did not work" before.
     */
    suspend fun uploadUserBackground(uri: Uri, userId: String, isVideo: Boolean): String = suspendCoroutine { continuation ->
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                continuation.resumeWithException(
                    Exception("User not authenticated. Please log in to upload files.")
                )
                return@suspendCoroutine
            }

            val timestamp = System.currentTimeMillis()
            val ext = if (isVideo) "mp4" else "jpg"
            val fileRef = storage.reference.child("user_backgrounds/$userId/$timestamp.$ext")

            fileRef.putFile(uri)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        continuation.resume(downloadUri.toString())
                    }.addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    /**
     * Upload story to Firebase Storage
     * Pattern: stories/{userId}/{timestamp}.jpg
     */
    suspend fun uploadStory(imageUri: Uri, userId: String): String = suspendCoroutine { continuation ->
        try {
            // Verify user is authenticated
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                continuation.resumeWithException(
                    Exception("User not authenticated. Please log in to upload files.")
                )
                return@suspendCoroutine
            }
            
            val timestamp = System.currentTimeMillis()
            val storageRef = storage.reference
            val fileRef = storageRef.child("stories/$userId/$timestamp.jpg")
            
            fileRef.putFile(imageUri)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        continuation.resume(uri.toString())
                    }.addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
    
    /**
     * Upload profile photo to Firebase Storage
     * Pattern: user_photos/{userId}/profile.jpg
     */
    suspend fun uploadProfilePhoto(imageUri: Uri, userId: String): String = suspendCoroutine { continuation ->
        try {
            // Verify user is authenticated
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                continuation.resumeWithException(
                    Exception("User not authenticated. Please log in to upload files.")
                )
                return@suspendCoroutine
            }
            
            // Validate userId
            if (userId.isBlank()) {
                continuation.resumeWithException(Exception("User ID is required"))
                return@suspendCoroutine
            }
            
            val storageRef = storage.reference
            val fileRef = storageRef.child("user_photos/$userId/profile.jpg")
            
            fileRef.putFile(imageUri)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        continuation.resume(uri.toString())
                    }.addOnFailureListener { exception ->
                        continuation.resumeWithException(
                            Exception("Failed to get download URL: ${exception.message}", exception)
                        )
                    }
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(
                        Exception("Failed to upload profile photo: ${exception.message}", exception)
                    )
                }
        } catch (e: Exception) {
            continuation.resumeWithException(
                Exception("Upload error: ${e.message}", e)
            )
        }
    }
    
    /**
     * Upload cover photo to Firebase Storage
     * Pattern: user_photos/{userId}/cover.jpg
     */
    suspend fun uploadCoverPhoto(imageUri: Uri, userId: String): String = suspendCoroutine { continuation ->
        try {
            // Verify user is authenticated
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                continuation.resumeWithException(
                    Exception("User not authenticated. Please log in to upload files.")
                )
                return@suspendCoroutine
            }
            
            // Validate userId
            if (userId.isBlank()) {
                continuation.resumeWithException(Exception("User ID is required"))
                return@suspendCoroutine
            }
            
            val storageRef = storage.reference
            val fileRef = storageRef.child("user_photos/$userId/cover.jpg")
            
            fileRef.putFile(imageUri)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        continuation.resume(uri.toString())
                    }.addOnFailureListener { exception ->
                        continuation.resumeWithException(
                            Exception("Failed to get download URL: ${exception.message}", exception)
                        )
                    }
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(
                        Exception("Failed to upload cover photo: ${exception.message}", exception)
                    )
                }
        } catch (e: Exception) {
            continuation.resumeWithException(
                Exception("Upload error: ${e.message}", e)
            )
        }
    }
    
    /**
     * Upload document to Firebase Storage
     * Pattern: chats/{chatId}/documents/{timestamp}_{fileName}
     */
    suspend fun uploadChatDocument(documentUri: Uri, chatId: String, fileName: String): String = suspendCoroutine { continuation ->
        try {
            // Verify user is authenticated
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                continuation.resumeWithException(
                    Exception("User not authenticated. Please log in to upload files.")
                )
                return@suspendCoroutine
            }
            
            val timestamp = System.currentTimeMillis()
            val safeFileName = fileName.replace("[", "_").replace("]", "_").replace(" ", "_")
            val storageRef = storage.reference
            val fileRef = storageRef.child("chats/$chatId/documents/${timestamp}_$safeFileName")
            
            fileRef.putFile(documentUri)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        continuation.resume(uri.toString())
                    }.addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
    
    /**
     * Upload story video to Firebase Storage
     * Pattern: stories/{userId}/{timestamp}.mp4
     */
    suspend fun uploadStoryVideo(videoUri: Uri, userId: String): String = suspendCoroutine { continuation ->
        try {
            // Verify user is authenticated
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                continuation.resumeWithException(
                    Exception("User not authenticated. Please log in to upload files.")
                )
                return@suspendCoroutine
            }
            
            val timestamp = System.currentTimeMillis()
            val storageRef = storage.reference
            val fileRef = storageRef.child("stories/$userId/$timestamp.mp4")
            
            fileRef.putFile(videoUri)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        continuation.resume(uri.toString())
                    }.addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
    
    /**
     * Delete file from Firebase Storage
     */
    suspend fun deleteFile(downloadUrl: String) {
        try {
            val storageRef = storage.getReferenceFromUrl(downloadUrl)
            storageRef.delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
