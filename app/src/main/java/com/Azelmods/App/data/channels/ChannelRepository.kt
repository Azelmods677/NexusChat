package com.Azelmods.App.data.channels

import android.net.Uri
import com.Azelmods.App.data.repository.StorageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class Channel(
    val id: String,
    val name: String,
    val handle: String,
    val description: String,
    val ownerId: String,
    val subscribers: Int,
    val lastPost: String,
    val lastPostTime: Long
)

data class ChannelPost(val id: String, val text: String, val imageUrl: String?, val at: Long)

/**
 * Canales: una persona publica y cualquiera se suscribe. Son públicos a propósito, así que a
 * diferencia de los chats no van cifrados de extremo a extremo.
 */
@Singleton
class ChannelRepository @Inject constructor(
    private val storage: StorageRepository
) {
    private val db = FirebaseDatabase.getInstance()
    private val me get() = FirebaseAuth.getInstance().currentUser?.uid

    fun observeSubscribed(): Flow<List<Channel>> = callbackFlow {
        val uid = me
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val ref = db.getReference("userChannels/$uid")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ids = snapshot.children.mapNotNull { it.key }
                if (ids.isEmpty()) trySend(emptyList())
                val found = java.util.concurrent.ConcurrentHashMap<String, Channel>()
                var pending = ids.size
                ids.forEach { id ->
                    db.getReference("channels/$id").get().addOnCompleteListener { task ->
                        task.result?.let(::parseChannel)?.let { found[id] = it }
                        if (--pending == 0) trySend(found.values.sortedByDescending { it.lastPostTime })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeChannel(id: String): Flow<Channel?> = callbackFlow {
        val ref = db.getReference("channels/$id")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(parseChannel(snapshot))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observePosts(id: String): Flow<List<ChannelPost>> = callbackFlow {
        val query = db.getReference("channelPosts/$id").orderByChild("at").limitToLast(MAX_POSTS)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(
                    snapshot.children.mapNotNull { post ->
                        ChannelPost(
                            id = post.key ?: return@mapNotNull null,
                            text = post.child("text").getValue(String::class.java).orEmpty(),
                            imageUrl = post.child("imageUrl").getValue(String::class.java),
                            at = post.child("at").getValue(Long::class.java) ?: 0L
                        )
                    }
                )
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    /** Devuelve el id del canal, o null si ese @nombre ya está cogido. */
    suspend fun create(name: String, handle: String, description: String): String? {
        val uid = me ?: return null
        val clean = normalizeHandle(handle)
        val handleRef = db.getReference("channelHandles/$clean")
        if (handleRef.get().await().exists()) return null
        val id = db.getReference("channels").push().key ?: return null
        db.getReference("channels/$id").setValue(
            mapOf(
                "name" to name.trim(),
                "handle" to clean,
                "description" to description.trim(),
                "ownerId" to uid,
                "subscribers" to 1,
                "createdAt" to ServerValue.TIMESTAMP,
                "lastPostTime" to ServerValue.TIMESTAMP
            )
        ).await()
        handleRef.setValue(id).await()
        subscribe(id)
        return id
    }

    suspend fun findByHandle(handle: String): Channel? {
        val id = db.getReference("channelHandles/${normalizeHandle(handle)}").get().await().getValue(String::class.java) ?: return null
        return parseChannel(db.getReference("channels/$id").get().await())
    }

    suspend fun isSubscribed(id: String): Boolean {
        val uid = me ?: return false
        return db.getReference("userChannels/$uid/$id").get().await().exists()
    }

    suspend fun subscribe(id: String) {
        val uid = me ?: return
        db.reference.updateChildren(mapOf("userChannels/$uid/$id" to true, "channelSubscribers/$id/$uid" to true)).await()
        FirebaseMessaging.getInstance().subscribeToTopic(topic(id))
    }

    suspend fun unsubscribe(id: String) {
        val uid = me ?: return
        db.reference.updateChildren(mapOf("userChannels/$uid/$id" to null, "channelSubscribers/$id/$uid" to null)).await()
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic(id))
    }

    suspend fun publish(id: String, text: String, image: Uri?) {
        val uid = me ?: return
        val imageUrl = image?.let { storage.uploadUserBackground(it, uid, false) }
        val post = buildMap<String, Any> {
            put("text", text.trim())
            put("at", ServerValue.TIMESTAMP)
            if (imageUrl != null) put("imageUrl", imageUrl)
        }
        db.getReference("channelPosts/$id").push().setValue(post).await()
        db.getReference("channels/$id").updateChildren(
            mapOf("lastPost" to text.trim().ifBlank { "Foto" }, "lastPostTime" to ServerValue.TIMESTAMP)
        ).await()
    }

    suspend fun deletePost(id: String, postId: String) {
        db.getReference("channelPosts/$id/$postId").removeValue().await()
    }

    private fun parseChannel(snapshot: DataSnapshot): Channel? {
        val id = snapshot.key ?: return null
        val name = snapshot.child("name").getValue(String::class.java) ?: return null
        return Channel(
            id = id,
            name = name,
            handle = snapshot.child("handle").getValue(String::class.java).orEmpty(),
            description = snapshot.child("description").getValue(String::class.java).orEmpty(),
            ownerId = snapshot.child("ownerId").getValue(String::class.java).orEmpty(),
            subscribers = snapshot.child("subscribers").getValue(Int::class.java) ?: 0,
            lastPost = snapshot.child("lastPost").getValue(String::class.java).orEmpty(),
            lastPostTime = snapshot.child("lastPostTime").getValue(Long::class.java) ?: 0L
        )
    }

    companion object {
        private const val MAX_POSTS = 100
        fun topic(id: String) = "channel_${id.replace(Regex("[^a-zA-Z0-9_-]"), "_")}"
    }
}

/** "@Mi Canal!" → "micanal": solo minúsculas, números y guion bajo. */
internal fun normalizeHandle(handle: String): String =
    handle.trim().removePrefix("@").lowercase().replace(Regex("[^a-z0-9_]"), "")
