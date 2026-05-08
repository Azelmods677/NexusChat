package com.Azelmods.App.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// ╔══════════════════════════════════════════════════════╗
// ║  MANUAL STEP — Firebase Console → Realtime Database  ║
// ║  → Rules → Paste these rules:                        ║
// ╚══════════════════════════════════════════════════════╝
//
// {
//   "rules": {
//     "users": {
//       ".read": "auth != null",
//       "$uid": {
//         ".write": "auth != null && auth.uid == $uid"
//       }
//     },
//     "conversations": {
//       "$convId": {
//         ".read": "auth != null &&
//           (data.child('participants').child(auth.uid).exists()
//           || !data.exists())",
//         ".write": "auth != null"
//       }
//     },
//     "messages": {
//       "$convId": {
//         ".read": "auth != null &&
//           root.child('conversations').child($convId)
//             .child('participants').child(auth.uid).exists()",
//         ".write": "auth != null &&
//           root.child('conversations').child($convId)
//             .child('participants').child(auth.uid).exists()"
//       }
//     },
//     "chats": {
//       "$chatId": {
//         ".read": "auth != null",
//         ".write": "auth != null"
//       }
//     },
//     "stories": {
//       ".read": "auth != null",
//       "$storyId": {
//         ".write": "auth != null"
//       }
//     },
//     "calls": {
//       "$callId": {
//         ".read": "auth != null",
//         ".write": "auth != null"
//       }
//     },
//     "friendRequests": {
//       ".read": "auth != null",
//       ".write": "auth != null"
//     }
//   }
// }

@Singleton
class RealtimeDatabaseRepository @Inject constructor() {

    private val database: DatabaseReference by lazy { FirebaseDatabase.getInstance().reference }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    /**
     * Get messages for a chat in real-time
     */
    @Suppress("UNCHECKED_CAST")
    fun getChatMessages(chatId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<Map<String, Any>>()
                snapshot.children.forEach { messageSnapshot ->
                    val messageData = messageSnapshot.value as? Map<String, Any>
                    if (messageData != null) {
                        messages.add(messageData.plus("messageId" to messageSnapshot.key!!))
                    }
                }
                trySend(messages.sortedBy { it["timestamp"] as? Long ?: 0L })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        database.child("chats").child(chatId).child("messages")
            .addValueEventListener(listener)

        awaitClose {
            database.child("chats").child(chatId).child("messages")
                .removeEventListener(listener)
        }
    }

    /**
     * Send a text message
     * ✅ FIXED: Now creates chat if it doesn't exist
     */
    suspend fun sendMessage(chatId: String, content: String, replyTo: String? = null) {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        // ✅ Ensure chat exists
        val chatRef = database.child("chats").child(chatId)
        val chatSnapshot = chatRef.get().await()
        
        if (!chatSnapshot.exists()) {
            val members = chatId.split("_")
            val chatData = mapOf(
                "chatId" to chatId,
                "members" to members,
                "createdAt" to ServerValue.TIMESTAMP,
                "lastMessage" to "",
                "lastMessageTime" to ServerValue.TIMESTAMP,
                "lastMessageSenderId" to ""
            )
            chatRef.setValue(chatData).await()
            android.util.Log.d("RealtimeDB", "Chat created: $chatId")
        }

        // Generate message ID
        val messageId = chatRef.child("messages").push().key
            ?: throw Exception("Failed to generate message ID")

        // Create message data
        val messageData = hashMapOf(
            "messageId" to messageId,
            "senderId" to currentUserId,
            "content" to content,
            "timestamp" to ServerValue.TIMESTAMP,
            "status" to "sent",
            "reactions" to emptyMap<String, String>()
        )

        if (replyTo != null) {
            messageData["replyTo"] = replyTo
        }

        // Save message
        chatRef.child("messages").child(messageId).setValue(messageData).await()
        android.util.Log.d("RealtimeDB", "Message sent: $messageId")

        // Update last message
        val lastMessageData = mapOf(
            "lastMessage" to content,
            "lastMessageTime" to ServerValue.TIMESTAMP,
            "lastMessageSenderId" to currentUserId
        )

        chatRef.updateChildren(lastMessageData).await()
        android.util.Log.d("RealtimeDB", "Last message updated for chat: $chatId")
    }

    /**
     * Get user chats in real-time
     */
    @Suppress("UNCHECKED_CAST")
    fun getUserChats(userId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chats = mutableListOf<Map<String, Any>>()
                snapshot.children.forEach { chatSnapshot ->
                    val chatData = chatSnapshot.value as? Map<String, Any>
                    if (chatData != null) {
                        val members = chatData["members"] as? List<String> ?: emptyList()
                        if (members.contains(userId)) {
                            chats.add(chatData.plus("chatId" to chatSnapshot.key!!))
                        }
                    }
                }
                trySend(chats.sortedByDescending { it["lastMessageTime"] as? Long ?: 0L })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        database.child("chats").addValueEventListener(listener)

        awaitClose {
            database.child("chats").removeEventListener(listener)
        }
    }

    /**
     * Get all stories in real-time
     */
    @Suppress("UNCHECKED_CAST")
    fun getAllStories(): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val stories = mutableListOf<Map<String, Any>>()
                val now = System.currentTimeMillis()

                android.util.Log.d("StoriesDebug", "Total stories in Firebase: ${snapshot.childrenCount}")

                snapshot.children.forEach { storySnapshot ->
                    try {
                        val storyData = storySnapshot.value as? Map<*, *>

                        android.util.Log.d("StoriesDebug", "Story key: ${storySnapshot.key}")
                        android.util.Log.d("StoriesDebug", "Story data: $storyData")

                        if (storyData != null) {
                            // Convert Map<*, *> to Map<String, Any>
                            val convertedData = storyData.entries.associate {
                                it.key.toString() to (it.value ?: "")
                            }.toMutableMap()

                            // Add storyId
                            convertedData["storyId"] = storySnapshot.key ?: ""

                            // Check expiration
                            val expiresAt = when (val exp = convertedData["expiresAt"]) {
                                is Long -> exp
                                is Double -> exp.toLong()
                                is String -> exp.toLongOrNull() ?: 0L
                                else -> 0L
                            }

                            android.util.Log.d(
                                "StoriesDebug",
                                "ExpiresAt: $expiresAt, Now: $now, Valid: ${expiresAt > now}"
                            )

                            if (expiresAt > now) {
                                stories.add(convertedData)
                            } else {
                                android.util.Log.d("StoriesDebug", "Story expired, skipping")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("StoriesDebug", "Error parsing story: ${e.message}", e)
                    }
                }

                android.util.Log.d("StoriesDebug", "Valid stories count: ${stories.size}")
                trySend(stories.sortedByDescending {
                    when (val ts = it["timestamp"]) {
                        is Long -> ts
                        is Double -> ts.toLong()
                        is String -> ts.toLongOrNull() ?: 0L
                        else -> 0L
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("StoriesDebug", "Database error: ${error.message}")
                close(error.toException())
            }
        }

        database.child("stories").addValueEventListener(listener)

        awaitClose {
            database.child("stories").removeEventListener(listener)
        }
    }

    /**
     * Add reaction to message
     */
    suspend fun addReaction(chatId: String, messageId: String, emoji: String) {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        database.child("chats").child(chatId).child("messages").child(messageId)
            .child("reactions").child(currentUserId).setValue(emoji).await()
    }

    /**
     * Create a new group in Realtime Database
     */
    suspend fun createGroup(groupName: String, memberIds: List<String>): String {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        val groupId = database.child("chats").push().key ?: throw Exception("Failed to generate group ID")

        val groupData = hashMapOf(
            "name" to groupName,
            "members" to (memberIds + currentUserId),
            "createdBy" to currentUserId,
            "createdAt" to ServerValue.TIMESTAMP,
            "lastMessage" to "",
            "lastMessageTime" to ServerValue.TIMESTAMP,
            "type" to "group"
        )

        database.child("chats").child(groupId).setValue(groupData).await()
        return groupId
    }

    /**
     * Search for a user by username
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun searchUserByUsername(username: String): Map<String, Any>? {
        val snapshot = database.child("users")
            .orderByChild("username")
            .equalTo(username)
            .limitToFirst(1)
            .get()
            .await()

        return if (snapshot.exists()) {
            val userId = snapshot.children.first().key ?: return null
            val userData = snapshot.children.first().value as? Map<String, Any> ?: return null
            userData.plus("uid" to userId)
        } else {
            null
        }
    }

    /**
     * Send a friend request
     */
    suspend fun sendFriendRequest(toUserId: String): String {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        val requestId = database.child("friendRequests").push().key
            ?: throw Exception("Failed to generate request ID")

        val requestData = hashMapOf(
            "fromUserId" to currentUserId,
            "toUserId" to toUserId,
            "status" to "pending",
            "timestamp" to ServerValue.TIMESTAMP
        )

        database.child("friendRequests").child(requestId).setValue(requestData).await()
        return requestId
    }

    /**
     * Update user profile photo URL
     */
    suspend fun updateProfilePhoto(userId: String, photoUrl: String) {
        database.child("users").child(userId).child("photoUrl").setValue(photoUrl).await()
    }

    /**
     * Create a story in Realtime Database
     */
    suspend fun createStory(mediaUrl: String, caption: String, isVideo: Boolean): String {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        val storyId = database.child("stories").push().key
            ?: throw Exception("Failed to generate story ID")

        val storyData = hashMapOf(
            "userId" to currentUserId,
            "mediaUrl" to mediaUrl,
            "caption" to caption,
            "isVideo" to isVideo,
            "timestamp" to ServerValue.TIMESTAMP,
            "expiresAt" to (System.currentTimeMillis() + (24 * 60 * 60 * 1000)),
            "views" to emptyList<String>()
        )

        database.child("stories").child(storyId).setValue(storyData).await()
        return storyId
    }

    /**
     * Create a story with custom data
     */
    fun createStory(storyData: Map<String, Any>): Flow<Result<String>> = callbackFlow {
        val storyId = database.child("stories").push().key
            ?: throw Exception("Failed to generate story ID")

        database.child("stories").child(storyId).setValue(storyData)
            .addOnSuccessListener {
                trySend(Result.success(storyId))
                close()
            }
            .addOnFailureListener { exception ->
                trySend(Result.failure(exception))
                close()
            }

        awaitClose { }
    }

    /**
     * Send a message with media attachment
     * ✅ FIXED: Now creates chat if it doesn't exist
     */
    suspend fun sendMediaMessage(
        chatId: String,
        mediaUrl: String,
        mediaType: String,
        caption: String = ""
    ) {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        // ✅ Ensure chat exists
        val chatRef = database.child("chats").child(chatId)
        val chatSnapshot = chatRef.get().await()
        
        if (!chatSnapshot.exists()) {
            val members = chatId.split("_")
            val chatData = mapOf(
                "chatId" to chatId,
                "members" to members,
                "createdAt" to ServerValue.TIMESTAMP,
                "lastMessage" to "",
                "lastMessageTime" to ServerValue.TIMESTAMP,
                "lastMessageSenderId" to ""
            )
            chatRef.setValue(chatData).await()
            android.util.Log.d("RealtimeDB", "Chat created: $chatId")
        }

        // Generate message ID
        val messageId = chatRef.child("messages").push().key
            ?: throw Exception("Failed to generate message ID")

        // Create message data
        val messageData = hashMapOf(
            "messageId" to messageId,
            "senderId" to currentUserId,
            "content" to caption,
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "timestamp" to ServerValue.TIMESTAMP,
            "status" to "sent",
            "reactions" to emptyMap<String, String>()
        )

        // Save message
        chatRef.child("messages").child(messageId).setValue(messageData).await()
        android.util.Log.d("RealtimeDB", "Media message sent: $messageId")

        // Update last message
        val lastMessageText = when (mediaType) {
            "IMAGE" -> "📷 Foto"
            "VIDEO" -> "🎥 Video"
            "AUDIO" -> "🎤 Audio"
            else -> if (caption.isNotEmpty()) caption else "[$mediaType]"
        }
        
        val lastMessageData = mapOf(
            "lastMessage" to lastMessageText,
            "lastMessageTime" to ServerValue.TIMESTAMP,
            "lastMessageSenderId" to currentUserId
        )

        chatRef.updateChildren(lastMessageData).await()
        android.util.Log.d("RealtimeDB", "Last message updated for chat: $chatId")
    }

    /**
     * Get user data by ID
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun getUserById(userId: String): Map<String, Any>? {
        return try {
            val snapshot = database.child("users").child(userId).get().await()

            if (snapshot.exists()) {
                val userData = snapshot.value as? Map<String, Any> ?: return null
                userData.plus("uid" to userId)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get user profile as Flow
     */
    @Suppress("UNCHECKED_CAST")
    fun getUserProfile(userId: String): Flow<Result<Map<String, Any>>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userData = snapshot.value as? Map<String, Any>
                    if (userData != null) {
                        trySend(Result.success(userData.plus("uid" to userId)))
                    } else {
                        trySend(Result.failure(Exception("Invalid user data")))
                    }
                } else {
                    trySend(Result.failure(Exception("User not found")))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }

        database.child("users").child(userId).addValueEventListener(listener)

        awaitClose {
            database.child("users").child(userId).removeEventListener(listener)
        }
    }

    /**
     * Get all users from Firebase in real-time (excluding current user)
     * Returns Flow that emits list of users whenever data changes
     */
    @Suppress("UNCHECKED_CAST")
    fun getAllUsers(): Flow<Result<List<Map<String, Any>>>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = mutableListOf<Map<String, Any>>()

                snapshot.children.forEach { userSnapshot ->
                    val userId = userSnapshot.key
                    val userData = userSnapshot.value as? Map<String, Any>

                    // Exclude current user from the list
                    if (userId != null && userData != null && userId != currentUserId) {
                        users.add(userData.plus("uid" to userId))
                    }
                }

                trySend(Result.success(users))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }

        database.child("users").addValueEventListener(listener)

        awaitClose {
            database.child("users").removeEventListener(listener)
        }
    }

    /**
     * Update user profile
     */
    fun updateUserProfile(userId: String, profileData: Map<String, Any>): Flow<Result<Unit>> = callbackFlow {
        database.child("users").child(userId).updateChildren(profileData)
            .addOnSuccessListener {
                trySend(Result.success(Unit))
                close()
            }
            .addOnFailureListener { exception ->
                trySend(Result.failure(exception))
                close()
            }

        awaitClose { }
    }

    // ==================== CALL FUNCTIONS ====================

    /**
     * Create a new call in Firebase
     */
    suspend fun createCall(callData: Map<String, Any>): String {
        val callId = database.child("calls").push().key
            ?: throw Exception("Failed to generate call ID")

        database.child("calls").child(callId).setValue(callData).await()
        return callId
    }

    /**
     * Listen to call updates in real-time
     */
    @Suppress("UNCHECKED_CAST")
    fun listenToCall(callId: String): Flow<Map<String, Any>?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val callData = snapshot.value as? Map<String, Any>
                trySend(callData)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        database.child("calls").child(callId).addValueEventListener(listener)

        awaitClose {
            database.child("calls").child(callId).removeEventListener(listener)
        }
    }

    /**
     * Update call status
     */
    suspend fun updateCallStatus(callId: String, status: String) {
        database.child("calls").child(callId).child("status").setValue(status).await()
    }

    /**
     * Set call offer (SDP)
     */
    suspend fun setCallOffer(callId: String, offer: String) {
        database.child("calls").child(callId).child("offer").setValue(offer).await()
    }

    /**
     * Set call answer (SDP)
     */
    suspend fun setCallAnswer(callId: String, answer: String) {
        database.child("calls").child(callId).child("answer").setValue(answer).await()
    }

    /**
     * Add ICE candidate to call
     */
    suspend fun addIceCandidate(callId: String, candidate: Map<String, Any>) {
        val candidateId = database.child("calls").child(callId).child("iceCandidates").push().key
            ?: throw Exception("Failed to generate candidate ID")

        database.child("calls").child(callId).child("iceCandidates").child(candidateId)
            .setValue(candidate).await()
    }

    /**
     * Listen to ICE candidates
     */
    @Suppress("UNCHECKED_CAST")
    fun listenToIceCandidates(callId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = object : ChildEventListener {
            private val candidates = mutableListOf<Map<String, Any>>()

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val candidate = snapshot.value as? Map<String, Any>
                if (candidate != null) {
                    candidates.add(candidate)
                    trySend(candidates.toList())
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        database.child("calls").child(callId).child("iceCandidates")
            .addChildEventListener(listener)

        awaitClose {
            database.child("calls").child(callId).child("iceCandidates")
                .removeEventListener(listener)
        }
    }

    /**
     * End call
     */
    suspend fun endCall(callId: String) {
        val endData = mapOf(
            "status" to "ENDED",
            "endTime" to ServerValue.TIMESTAMP
        )
        database.child("calls").child(callId).updateChildren(endData).await()
    }

    // ==================== FIREBASE STORAGE FUNCTIONS ====================

    /**
     * Upload file to Firebase Storage
     */
    suspend fun uploadFile(
        uri: android.net.Uri,
        folder: String,
        fileName: String
    ): String {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
        val fileRef = storageRef.child("$folder/$currentUserId/$fileName")

        fileRef.putFile(uri).await()
        return fileRef.downloadUrl.await().toString()
    }

    /**
     * Send image message
     * ✅ FIXED: Now creates chat if it doesn't exist and uses mediaType instead of type
     */
    suspend fun sendImageMessage(
        chatId: String,
        imageUri: android.net.Uri,
        caption: String = ""
    ) {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        // ✅ Ensure chat exists
        val chatRef = database.child("chats").child(chatId)
        val chatSnapshot = chatRef.get().await()
        
        if (!chatSnapshot.exists()) {
            val members = chatId.split("_")
            val chatData = mapOf(
                "chatId" to chatId,
                "members" to members,
                "createdAt" to ServerValue.TIMESTAMP,
                "lastMessage" to "",
                "lastMessageTime" to ServerValue.TIMESTAMP,
                "lastMessageSenderId" to ""
            )
            chatRef.setValue(chatData).await()
            android.util.Log.d("RealtimeDB", "Chat created: $chatId")
        }

        // Upload image to Storage
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        val imageUrl = uploadFile(imageUri, "images", fileName)

        // Generate message ID
        val messageId = chatRef.child("messages").push().key
            ?: throw Exception("Failed to generate message ID")

        // Create message data with mediaType (not type)
        val messageData = hashMapOf(
            "messageId" to messageId,
            "senderId" to currentUserId,
            "content" to caption,
            "mediaType" to "IMAGE",
            "mediaUrl" to imageUrl,
            "timestamp" to ServerValue.TIMESTAMP,
            "status" to "sent",
            "reactions" to emptyMap<String, String>()
        )

        // Save message
        chatRef.child("messages").child(messageId).setValue(messageData).await()
        android.util.Log.d("RealtimeDB", "Image message sent: $messageId")

        // Update last message
        val lastMessageData = mapOf(
            "lastMessage" to if (caption.isNotEmpty()) caption else "📷 Foto",
            "lastMessageTime" to ServerValue.TIMESTAMP,
            "lastMessageSenderId" to currentUserId
        )
        chatRef.updateChildren(lastMessageData).await()
        android.util.Log.d("RealtimeDB", "Last message updated for chat: $chatId")
    }

    /**
     * Send video message
     * ✅ FIXED: Now creates chat if it doesn't exist and uses mediaType instead of type
     */
    suspend fun sendVideoMessage(
        chatId: String,
        videoUri: android.net.Uri,
        caption: String = "",
        duration: Long = 0
    ) {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        // ✅ Ensure chat exists
        val chatRef = database.child("chats").child(chatId)
        val chatSnapshot = chatRef.get().await()
        
        if (!chatSnapshot.exists()) {
            val members = chatId.split("_")
            val chatData = mapOf(
                "chatId" to chatId,
                "members" to members,
                "createdAt" to ServerValue.TIMESTAMP,
                "lastMessage" to "",
                "lastMessageTime" to ServerValue.TIMESTAMP,
                "lastMessageSenderId" to ""
            )
            chatRef.setValue(chatData).await()
            android.util.Log.d("RealtimeDB", "Chat created: $chatId")
        }

        // Upload video to Storage
        val fileName = "VID_${System.currentTimeMillis()}.mp4"
        val videoUrl = uploadFile(videoUri, "videos", fileName)

        // Generate message ID
        val messageId = chatRef.child("messages").push().key
            ?: throw Exception("Failed to generate message ID")

        // Create message data with mediaType (not type)
        val messageData = hashMapOf(
            "messageId" to messageId,
            "senderId" to currentUserId,
            "content" to caption,
            "mediaType" to "VIDEO",
            "mediaUrl" to videoUrl,
            "duration" to duration,
            "timestamp" to ServerValue.TIMESTAMP,
            "status" to "sent",
            "reactions" to emptyMap<String, String>()
        )

        // Save message
        chatRef.child("messages").child(messageId).setValue(messageData).await()
        android.util.Log.d("RealtimeDB", "Video message sent: $messageId")

        // Update last message
        val lastMessageData = mapOf(
            "lastMessage" to if (caption.isNotEmpty()) caption else "🎥 Video",
            "lastMessageTime" to ServerValue.TIMESTAMP,
            "lastMessageSenderId" to currentUserId
        )
        chatRef.updateChildren(lastMessageData).await()
        android.util.Log.d("RealtimeDB", "Last message updated for chat: $chatId")
    }

    /**
     * Send document message
     * ✅ FIXED: Now creates chat if it doesn't exist and uses mediaType instead of type
     */
    suspend fun sendDocumentMessage(
        chatId: String,
        documentUri: android.net.Uri,
        fileName: String,
        fileSize: Long,
        mimeType: String
    ) {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        // ✅ Ensure chat exists
        val chatRef = database.child("chats").child(chatId)
        val chatSnapshot = chatRef.get().await()
        
        if (!chatSnapshot.exists()) {
            val members = chatId.split("_")
            val chatData = mapOf(
                "chatId" to chatId,
                "members" to members,
                "createdAt" to ServerValue.TIMESTAMP,
                "lastMessage" to "",
                "lastMessageTime" to ServerValue.TIMESTAMP,
                "lastMessageSenderId" to ""
            )
            chatRef.setValue(chatData).await()
            android.util.Log.d("RealtimeDB", "Chat created: $chatId")
        }

        // Upload document to Storage
        val documentUrl = uploadFile(documentUri, "documents", fileName)

        // Generate message ID
        val messageId = chatRef.child("messages").push().key
            ?: throw Exception("Failed to generate message ID")

        // Create message data with mediaType (not type)
        val messageData = hashMapOf(
            "messageId" to messageId,
            "senderId" to currentUserId,
            "content" to fileName,
            "mediaType" to "DOCUMENT",
            "mediaUrl" to documentUrl,
            "fileName" to fileName,
            "fileSize" to fileSize,
            "mimeType" to mimeType,
            "timestamp" to ServerValue.TIMESTAMP,
            "status" to "sent",
            "reactions" to emptyMap<String, String>()
        )

        // Save message
        chatRef.child("messages").child(messageId).setValue(messageData).await()
        android.util.Log.d("RealtimeDB", "Document message sent: $messageId")

        // Update last message
        val lastMessageData = mapOf(
            "lastMessage" to "📄 $fileName",
            "lastMessageTime" to ServerValue.TIMESTAMP,
            "lastMessageSenderId" to currentUserId
        )
        chatRef.updateChildren(lastMessageData).await()
        android.util.Log.d("RealtimeDB", "Last message updated for chat: $chatId")
    }

    /**
     * Send audio message
     * ✅ FIXED: Now creates chat if it doesn't exist and uses mediaType instead of type
     */
    suspend fun sendAudioMessage(
        chatId: String,
        audioUri: android.net.Uri,
        duration: Long
    ) {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        // ✅ Ensure chat exists
        val chatRef = database.child("chats").child(chatId)
        val chatSnapshot = chatRef.get().await()
        
        if (!chatSnapshot.exists()) {
            val members = chatId.split("_")
            val chatData = mapOf(
                "chatId" to chatId,
                "members" to members,
                "createdAt" to ServerValue.TIMESTAMP,
                "lastMessage" to "",
                "lastMessageTime" to ServerValue.TIMESTAMP,
                "lastMessageSenderId" to ""
            )
            chatRef.setValue(chatData).await()
            android.util.Log.d("RealtimeDB", "Chat created: $chatId")
        }

        // Upload audio to Storage
        val fileName = "AUD_${System.currentTimeMillis()}.m4a"
        val audioUrl = uploadFile(audioUri, "audio", fileName)

        // Generate message ID
        val messageId = chatRef.child("messages").push().key
            ?: throw Exception("Failed to generate message ID")

        // Create message data with mediaType (not type)
        val messageData = hashMapOf(
            "messageId" to messageId,
            "senderId" to currentUserId,
            "content" to "",
            "mediaType" to "AUDIO",
            "mediaUrl" to audioUrl,
            "duration" to duration,
            "timestamp" to ServerValue.TIMESTAMP,
            "status" to "sent",
            "reactions" to emptyMap<String, String>()
        )

        // Save message
        chatRef.child("messages").child(messageId).setValue(messageData).await()
        android.util.Log.d("RealtimeDB", "Audio message sent: $messageId")

        // Update last message
        val lastMessageData = mapOf(
            "lastMessage" to "🎤 Audio",
            "lastMessageTime" to ServerValue.TIMESTAMP,
            "lastMessageSenderId" to currentUserId
        )
        chatRef.updateChildren(lastMessageData).await()
        android.util.Log.d("RealtimeDB", "Last message updated for chat: $chatId")
    }

    /**
     * Send location message
     * ✅ FIXED: Now creates chat if it doesn't exist
     */
    suspend fun sendLocationMessage(
        chatId: String,
        latitude: Double,
        longitude: Double,
        address: String = ""
    ) {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        // ✅ Ensure chat exists
        val chatRef = database.child("chats").child(chatId)
        val chatSnapshot = chatRef.get().await()
        
        if (!chatSnapshot.exists()) {
            val members = chatId.split("_")
            val chatData = mapOf(
                "chatId" to chatId,
                "members" to members,
                "createdAt" to ServerValue.TIMESTAMP,
                "lastMessage" to "",
                "lastMessageTime" to ServerValue.TIMESTAMP,
                "lastMessageSenderId" to ""
            )
            chatRef.setValue(chatData).await()
            android.util.Log.d("RealtimeDB", "Chat created: $chatId")
        }

        // Generate message ID
        val messageId = chatRef.child("messages").push().key
            ?: throw Exception("Failed to generate message ID")

        // Create message data
        val messageData = hashMapOf(
            "messageId" to messageId,
            "senderId" to currentUserId,
            "content" to address,
            "mediaType" to "LOCATION",
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to ServerValue.TIMESTAMP,
            "status" to "sent",
            "reactions" to emptyMap<String, String>()
        )

        // Save message
        chatRef.child("messages").child(messageId).setValue(messageData).await()
        android.util.Log.d("RealtimeDB", "Location message sent: $messageId")

        // Update last message
        val lastMessageData = mapOf(
            "lastMessage" to "📍 Ubicación",
            "lastMessageTime" to ServerValue.TIMESTAMP,
            "lastMessageSenderId" to currentUserId
        )
        chatRef.updateChildren(lastMessageData).await()
        android.util.Log.d("RealtimeDB", "Last message updated for chat: $chatId")
    }

    /**
     * Send sticker message
     * ✅ FIXED: Now creates chat if it doesn't exist
     */
    suspend fun sendStickerMessage(
        chatId: String,
        stickerUrl: String,
        stickerPack: String = ""
    ) {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        // ✅ Ensure chat exists
        val chatRef = database.child("chats").child(chatId)
        val chatSnapshot = chatRef.get().await()
        
        if (!chatSnapshot.exists()) {
            val members = chatId.split("_")
            val chatData = mapOf(
                "chatId" to chatId,
                "members" to members,
                "createdAt" to ServerValue.TIMESTAMP,
                "lastMessage" to "",
                "lastMessageTime" to ServerValue.TIMESTAMP,
                "lastMessageSenderId" to ""
            )
            chatRef.setValue(chatData).await()
            android.util.Log.d("RealtimeDB", "Chat created: $chatId")
        }

        // Generate message ID
        val messageId = chatRef.child("messages").push().key
            ?: throw Exception("Failed to generate message ID")

        // Create message data
        val messageData = hashMapOf(
            "messageId" to messageId,
            "senderId" to currentUserId,
            "content" to stickerUrl,
            "mediaType" to "STICKER",
            "stickerUrl" to stickerUrl,
            "stickerPack" to stickerPack,
            "timestamp" to ServerValue.TIMESTAMP,
            "status" to "sent",
            "reactions" to emptyMap<String, String>()
        )

        // Save message
        chatRef.child("messages").child(messageId).setValue(messageData).await()
        android.util.Log.d("RealtimeDB", "Sticker message sent: $messageId")

        // Update last message
        val lastMessageData = mapOf(
            "lastMessage" to "Sticker",
            "lastMessageTime" to ServerValue.TIMESTAMP,
            "lastMessageSenderId" to currentUserId
        )
        chatRef.updateChildren(lastMessageData).await()
        android.util.Log.d("RealtimeDB", "Last message updated for chat: $chatId")
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Stories
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Mark a story as viewed by the current user.
     * Path: stories/{storyId}/views/{userId} = true
     */
    suspend fun markStoryAsViewed(storyId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        database.child("stories").child(storyId).child("views").child(currentUserId)
            .setValue(true).await()
    }

    /**
     * Send a reply to a story (stored as a message in a special chat).
     * Path: storyReplies/{storyOwnerId}/{storyId}/{messageId}
     */
    suspend fun sendStoryReply(storyOwnerId: String, storyId: String, replyText: String) {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("Not authenticated")
        val msgRef = database.child("storyReplies").child(storyOwnerId).child(storyId).push()
        val msgId = msgRef.key ?: throw Exception("Failed to generate ID")
        val data = mapOf(
            "messageId" to msgId,
            "senderId" to currentUserId,
            "content" to replyText,
            "timestamp" to ServerValue.TIMESTAMP
        )
        msgRef.setValue(data).await()
    }

    /**
     * Get story viewers list.
     * Path: stories/{storyId}/views (map of userId → true)
     */
    suspend fun getStoryViewers(storyId: String): List<Map<String, Any>> {
        val snapshot = database.child("stories").child(storyId).child("views").get().await()
        val viewerIds = snapshot.children.mapNotNull { it.key }
        return viewerIds.mapNotNull { uid ->
            getUserById(uid)?.plus("uid" to uid)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Media Gallery
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Get all IMAGE and VIDEO messages for a chat (media gallery).
     * Path: chats/{chatId}/messages filtered by mediaType == IMAGE or VIDEO
     */
    suspend fun getChatMediaGallery(chatId: String): List<Map<String, Any>> {
        val snapshot = database.child("chats").child(chatId).child("messages").get().await()
        val media = mutableListOf<Map<String, Any>>()
        snapshot.children.forEach { child ->
            @Suppress("UNCHECKED_CAST")
            val data = child.value as? Map<String, Any> ?: return@forEach
            val mediaType = data["mediaType"] as? String
            if (mediaType == "IMAGE" || mediaType == "VIDEO") {
                media.add(data.plus("messageId" to (child.key ?: "")))
            }
        }
        return media.sortedByDescending { it["timestamp"] as? Long ?: 0L }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Presence
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Update online presence for the current user.
     * Call from MainActivity onResume / onPause via ViewModel.
     * When going online also registers onDisconnect handlers so Firebase
     * automatically marks the user offline if the connection drops.
     */
    suspend fun updatePresence(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val ref = database.child("users").child(uid)
        if (isOnline) {
            ref.child("isOnline").setValue(true).await()
            // Auto-set offline on disconnect
            ref.child("isOnline").onDisconnect().setValue(false)
            ref.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP)
        } else {
            ref.child("isOnline").setValue(false).await()
            ref.child("lastSeen").setValue(ServerValue.TIMESTAMP).await()
        }
    }

    /**
     * Observe another user's online status in real-time.
     * Emits Pair(isOnline, lastSeenTimestamp).
     */
    fun observeUserPresence(userId: String): Flow<Pair<Boolean, Long>> = callbackFlow {
        val ref = database.child("users").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false
                val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                trySend(Pair(isOnline, lastSeen))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Pair(false, 0L))
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Typing indicators
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Observe typing indicators for a chat.
     * Emits a map of userId → isTyping for every change under typing/{chatId}.
     */
    fun observeTyping(chatId: String): Flow<Map<String, Boolean>> = callbackFlow {
        val ref = database.child("typing").child(chatId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val typingMap = mutableMapOf<String, Boolean>()
                snapshot.children.forEach { child ->
                    val uid = child.key ?: return@forEach
                    val isTyping = child.getValue(Boolean::class.java) ?: false
                    typingMap[uid] = isTyping
                }
                trySend(typingMap)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyMap())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Set the current user's typing status in a chat.
     * Path: typing/{chatId}/{uid} = isTyping
     * Also registers an onDisconnect handler to clear the flag if the connection drops.
     */
    suspend fun setTypingStatus(chatId: String, isTyping: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        database.child("typing").child(chatId).child(uid).setValue(isTyping).await()
        if (isTyping) {
            // Auto-clear typing flag if the client disconnects unexpectedly
            database.child("typing").child(chatId).child(uid).onDisconnect().setValue(false)
        }
    }

}
