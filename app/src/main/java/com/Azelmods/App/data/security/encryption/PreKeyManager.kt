package com.Azelmods.App.data.security.encryption

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.InvalidKeyException
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.state.PreKeyBundle
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages PreKey distribution via Firebase Realtime Database.
 *
 * PreKeys are uploaded to Firebase so other users can fetch them to establish
 * encrypted sessions. This implements the "PreKey server" component of Signal Protocol.
 *
 * ## Firebase Structure
 * ```
 * /prekeys/
 *   /{userId}/
 *     /identityKey: "base64..."
 *     /registrationId: 12345
 *     /signedPreKey/
 *       /keyId: 1
 *       /publicKey: "base64..."
 *       /signature: "base64..."
 *     /preKeys/
 *       /{preKeyId}/
 *         /keyId: 1
 *         /publicKey: "base64..."
 * ```
 *
 * Requirements: 1.3, 1.4
 */
@Singleton
class PreKeyManager @Inject constructor(
    private val database: FirebaseDatabase
) {

    companion object {
        private const val TAG = "PreKeyManager"
        private const val PREKEYS_PATH = "prekeys"
    }

    /**
     * Uploads PreKeys to Firebase for the current user.
     *
     * This should be called after Signal Protocol initialization and whenever
     * PreKeys are replenished.
     *
     * @param userId The current user's ID
     * @param identityKey The user's identity public key
     * @param registrationId The user's registration ID
     * @param signedPreKey The user's signed PreKey
     * @param preKeys List of OneTime PreKeys to upload
     */
    suspend fun uploadPreKeys(
        userId: String,
        identityKey: IdentityKey,
        registrationId: Int,
        signedPreKey: SignedPreKeyRecord,
        preKeys: List<PreKeyRecord>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val userPreKeysRef = database.getReference("$PREKEYS_PATH/$userId")

            // Upload identity key
            userPreKeysRef.child("identityKey")
                .setValue(android.util.Base64.encodeToString(
                    identityKey.serialize(),
                    android.util.Base64.DEFAULT
                ))
                .await()

            // Upload registration ID
            userPreKeysRef.child("registrationId")
                .setValue(registrationId)
                .await()

            // Upload signed PreKey
            val signedPreKeyData = mapOf(
                "keyId" to signedPreKey.id,
                "publicKey" to android.util.Base64.encodeToString(
                    signedPreKey.keyPair.publicKey.serialize(),
                    android.util.Base64.DEFAULT
                ),
                "signature" to android.util.Base64.encodeToString(
                    signedPreKey.signature,
                    android.util.Base64.DEFAULT
                )
            )
            userPreKeysRef.child("signedPreKey")
                .setValue(signedPreKeyData)
                .await()

            // Upload OneTime PreKeys
            val preKeysData = preKeys.associate { preKey ->
                preKey.id.toString() to mapOf(
                    "keyId" to preKey.id,
                    "publicKey" to android.util.Base64.encodeToString(
                        preKey.keyPair.publicKey.serialize(),
                        android.util.Base64.DEFAULT
                    )
                )
            }
            userPreKeysRef.child("preKeys")
                .setValue(preKeysData)
                .await()

            Log.d(TAG, "Uploaded PreKeys for user: $userId (${preKeys.size} OneTime PreKeys)")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error uploading PreKeys", e)
            false
        }
    }

    /**
     * Fetches a PreKey bundle for a specific user from Firebase.
     *
     * This is called when establishing a new encrypted session with another user.
     * After fetching, the OneTime PreKey is deleted from Firebase (single-use).
     *
     * @param userId The user whose PreKey bundle to fetch
     * @return [PreKeyBundle] or null if not available
     */
    suspend fun fetchPreKeyBundle(userId: String): PreKeyBundle? = withContext(Dispatchers.IO) {
        try {
            val userPreKeysRef = database.getReference("$PREKEYS_PATH/$userId")

            // Fetch all PreKey data
            val snapshot = userPreKeysRef.get().await()

            if (!snapshot.exists()) {
                Log.w(TAG, "No PreKeys found for user: $userId")
                return@withContext null
            }

            // Parse identity key
            val identityKeyBase64 = snapshot.child("identityKey").getValue(String::class.java)
                ?: throw InvalidKeyException("Missing identity key")
            val identityKey = IdentityKey(
                android.util.Base64.decode(identityKeyBase64, android.util.Base64.DEFAULT)
            )

            // Parse registration ID
            val registrationId = snapshot.child("registrationId").getValue(Int::class.java)
                ?: throw InvalidKeyException("Missing registration ID")

            // Parse signed PreKey
            val signedPreKeySnapshot = snapshot.child("signedPreKey")
            val signedPreKeyId = signedPreKeySnapshot.child("keyId").getValue(Int::class.java)
                ?: throw InvalidKeyException("Missing signed PreKey ID")
            val signedPreKeyPublicBase64 = signedPreKeySnapshot.child("publicKey")
                .getValue(String::class.java)
                ?: throw InvalidKeyException("Missing signed PreKey public key")
            val signedPreKeySignatureBase64 = signedPreKeySnapshot.child("signature")
                .getValue(String::class.java)
                ?: throw InvalidKeyException("Missing signed PreKey signature")

            val signedPreKeyPublic = Curve.decodePoint(
                android.util.Base64.decode(signedPreKeyPublicBase64, android.util.Base64.DEFAULT),
                0
            )
            val signedPreKeySignature = android.util.Base64.decode(
                signedPreKeySignatureBase64,
                android.util.Base64.DEFAULT
            )

            // Parse OneTime PreKey (take first available)
            val preKeysSnapshot = snapshot.child("preKeys")
            val firstPreKeyEntry = preKeysSnapshot.children.firstOrNull()
                ?: throw InvalidKeyException("No OneTime PreKeys available")

            val preKeyId = firstPreKeyEntry.child("keyId").getValue(Int::class.java)
                ?: throw InvalidKeyException("Missing PreKey ID")
            val preKeyPublicBase64 = firstPreKeyEntry.child("publicKey")
                .getValue(String::class.java)
                ?: throw InvalidKeyException("Missing PreKey public key")

            val preKeyPublic = Curve.decodePoint(
                android.util.Base64.decode(preKeyPublicBase64, android.util.Base64.DEFAULT),
                0
            )

            // Delete the used OneTime PreKey
            firstPreKeyEntry.ref.removeValue().await()
            Log.d(TAG, "Consumed OneTime PreKey $preKeyId for user: $userId")

            // Construct PreKey bundle
            PreKeyBundle(
                registrationId,
                1, // deviceId (always 1 for now)
                preKeyId,
                preKeyPublic,
                signedPreKeyId,
                signedPreKeyPublic,
                signedPreKeySignature,
                identityKey
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching PreKey bundle for user: $userId", e)
            null
        }
    }

    /**
     * Checks how many OneTime PreKeys are available for a user.
     *
     * @param userId The user to check
     * @return Number of available PreKeys
     */
    suspend fun getAvailablePreKeyCount(userId: String): Int = withContext(Dispatchers.IO) {
        try {
            val preKeysRef = database.getReference("$PREKEYS_PATH/$userId/preKeys")
            val snapshot = preKeysRef.get().await()

            snapshot.childrenCount.toInt()

        } catch (e: Exception) {
            Log.e(TAG, "Error getting PreKey count", e)
            0
        }
    }

    /**
     * Deletes all PreKeys for a user (e.g., on account deletion).
     *
     * @param userId The user whose PreKeys to delete
     */
    suspend fun deleteAllPreKeys(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val userPreKeysRef = database.getReference("$PREKEYS_PATH/$userId")
            userPreKeysRef.removeValue().await()

            Log.d(TAG, "Deleted all PreKeys for user: $userId")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error deleting PreKeys", e)
            false
        }
    }

    /**
     * Updates the signed PreKey for a user (called during rotation).
     *
     * @param userId The user ID
     * @param signedPreKey The new signed PreKey
     */
    suspend fun updateSignedPreKey(
        userId: String,
        signedPreKey: SignedPreKeyRecord
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val signedPreKeyRef = database.getReference("$PREKEYS_PATH/$userId/signedPreKey")

            val signedPreKeyData = mapOf(
                "keyId" to signedPreKey.id,
                "publicKey" to android.util.Base64.encodeToString(
                    signedPreKey.keyPair.publicKey.serialize(),
                    android.util.Base64.DEFAULT
                ),
                "signature" to android.util.Base64.encodeToString(
                    signedPreKey.signature,
                    android.util.Base64.DEFAULT
                )
            )

            signedPreKeyRef.setValue(signedPreKeyData).await()

            Log.d(TAG, "Updated signed PreKey for user: $userId")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error updating signed PreKey", e)
            false
        }
    }

    /**
     * Adds new OneTime PreKeys to Firebase (called during replenishment).
     *
     * @param userId The user ID
     * @param preKeys List of new PreKeys to add
     */
    suspend fun addPreKeys(
        userId: String,
        preKeys: List<PreKeyRecord>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val preKeysRef = database.getReference("$PREKEYS_PATH/$userId/preKeys")

            val preKeysData = preKeys.associate { preKey ->
                preKey.id.toString() to mapOf(
                    "keyId" to preKey.id,
                    "publicKey" to android.util.Base64.encodeToString(
                        preKey.keyPair.publicKey.serialize(),
                        android.util.Base64.DEFAULT
                    )
                )
            }

            preKeysRef.updateChildren(preKeysData).await()

            Log.d(TAG, "Added ${preKeys.size} new PreKeys for user: $userId")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error adding PreKeys", e)
            false
        }
    }
}
