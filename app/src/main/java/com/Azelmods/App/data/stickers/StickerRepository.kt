package com.Azelmods.App.data.stickers

import android.net.Uri
import com.Azelmods.App.data.repository.StorageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class StickerPackData(val id: String, val name: String, val stickers: List<String>, val shareCode: String?)

/**
 * Paquetes de stickers de cada usuario. Siguen a la cuenta, así que aparecen en otro teléfono.
 * Un paquete se puede publicar con un código para que otras personas lo añadan.
 */
@Singleton
class StickerRepository @Inject constructor(
    private val storage: StorageRepository
) {
    private val db = FirebaseDatabase.getInstance()
    private val me get() = FirebaseAuth.getInstance().currentUser?.uid

    fun observePacks(): Flow<List<StickerPackData>> = callbackFlow {
        val uid = me
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val ref = db.getReference("userStickerPacks/$uid")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.children.mapNotNull(::parsePack).sortedBy { it.name.lowercase() })
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    private fun parsePack(snapshot: DataSnapshot): StickerPackData? {
        val id = snapshot.key ?: return null
        return StickerPackData(
            id = id,
            name = snapshot.child("name").getValue(String::class.java) ?: return null,
            stickers = snapshot.child("stickers").children.mapNotNull { it.getValue(String::class.java) },
            shareCode = snapshot.child("shareCode").getValue(String::class.java)
        )
    }

    suspend fun createPack(name: String): String? {
        val uid = me ?: return null
        val ref = db.getReference("userStickerPacks/$uid").push()
        ref.setValue(mapOf("name" to name.trim(), "createdAt" to ServerValue.TIMESTAMP)).await()
        return ref.key
    }

    /** El paquete donde van los stickers recibidos y los creados sin elegir otro. */
    suspend fun defaultPack(): String? {
        val uid = me ?: return null
        val packs = db.getReference("userStickerPacks/$uid").get().await()
        packs.children.firstOrNull { it.child("name").getValue(String::class.java) == DEFAULT_PACK }?.key?.let { return it }
        return createPack(DEFAULT_PACK)
    }

    /** Sube la imagen del sticker y la añade al paquete. */
    suspend fun addImage(packId: String, image: Uri): String? {
        val uid = me ?: return null
        val url = storage.uploadUserBackground(image, uid, false)
        addExisting(packId, url)
        return url
    }

    /** Guarda en un paquete un sticker que ya existe: por ejemplo, uno recibido en un chat. */
    suspend fun addExisting(packId: String, sticker: String) {
        val uid = me ?: return
        val ref = db.getReference("userStickerPacks/$uid/$packId/stickers")
        val existing = ref.get().await().children.mapNotNull { it.getValue(String::class.java) }
        if (sticker !in existing) ref.push().setValue(sticker).await()
    }

    suspend fun saveReceived(sticker: String) {
        defaultPack()?.let { addExisting(it, sticker) }
    }

    suspend fun removeSticker(packId: String, sticker: String) {
        val uid = me ?: return
        val ref = db.getReference("userStickerPacks/$uid/$packId/stickers")
        ref.get().await().children.firstOrNull { it.getValue(String::class.java) == sticker }?.ref?.removeValue()?.await()
    }

    suspend fun deletePack(packId: String) {
        val uid = me ?: return
        db.getReference("userStickerPacks/$uid/$packId").removeValue().await()
    }

    /** Publica una copia del paquete y devuelve el código para compartirlo. */
    suspend fun publish(pack: StickerPackData): String? {
        val uid = me ?: return null
        val code = pack.shareCode ?: db.getReference("publicStickerPacks").push().key?.takeLast(CODE_LENGTH)?.lowercase() ?: return null
        db.getReference("publicStickerPacks/$code").setValue(
            mapOf("name" to pack.name, "owner" to uid, "stickers" to pack.stickers)
        ).await()
        db.getReference("userStickerPacks/$uid/${pack.id}/shareCode").setValue(code).await()
        return code
    }

    /** Añade a mis paquetes el de otra persona. Devuelve su nombre, o null si el código no existe. */
    suspend fun install(code: String): String? {
        val uid = me ?: return null
        val shared = db.getReference("publicStickerPacks/${code.trim().lowercase()}").get().await()
        val name = shared.child("name").getValue(String::class.java) ?: return null
        val stickers = shared.child("stickers").children.mapNotNull { it.getValue(String::class.java) }
        db.getReference("userStickerPacks/$uid").push().setValue(
            mapOf("name" to name, "stickers" to stickers.associateBy { db.reference.push().key.orEmpty() })
        ).await()
        return name
    }

    companion object {
        const val DEFAULT_PACK = "Mis stickers"
        private const val CODE_LENGTH = 8
    }
}
