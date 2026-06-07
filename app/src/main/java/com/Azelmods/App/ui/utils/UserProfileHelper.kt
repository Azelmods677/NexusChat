package com.Azelmods.App.ui.utils

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot

/**
 * Centralized helper for resolving user display data with sensible fallbacks.
 *
 * Priority for the display name: displayName > email prefix > phone tail > "Anónimo".
 * Never returns the generic "User"/"Usuario" placeholder.
 */
object UserProfileHelper {

    fun getDisplayName(user: FirebaseUser?): String =
        user?.displayName?.trim()?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@")
                ?.replaceFirstChar { it.uppercaseChar() }
            ?: user?.phoneNumber?.takeLast(4)?.let { "User •••$it" }
            ?: "Anónimo"

    fun getDisplayNameFromSnapshot(snapshot: DataSnapshot): String =
        snapshot.child("displayName").getValue(String::class.java)
            ?.trim()?.takeIf { it.isNotBlank() }
            ?: snapshot.child("name").getValue(String::class.java)
                ?.trim()?.takeIf { it.isNotBlank() }
            ?: snapshot.child("email").getValue(String::class.java)
                ?.substringBefore("@")?.replaceFirstChar { it.uppercaseChar() }
            ?: "Anónimo"

    /**
     * Resolves a display name from already-fetched fields (e.g. Realtime DB map).
     */
    fun resolveDisplayName(
        displayName: String?,
        name: String? = null,
        email: String? = null
    ): String =
        displayName?.trim()?.takeIf { it.isNotBlank() }
            ?: name?.trim()?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore("@")?.replaceFirstChar { it.uppercaseChar() }
            ?: "Anónimo"

    fun getPhotoUrl(user: FirebaseUser?): String? =
        user?.photoUrl?.toString()?.takeIf { it.isNotBlank() }

    fun getPhotoUrlFromSnapshot(snapshot: DataSnapshot): String? =
        snapshot.child("photoUrl").getValue(String::class.java)
            ?.takeIf { it.isNotBlank() && it.startsWith("http") }

    /**
     * Generates up to two uppercase initials for an avatar placeholder.
     */
    fun getInitials(name: String): String {
        val words = name.trim().split(" ").filter { it.isNotBlank() }
        return when (words.size) {
            0 -> "?"
            1 -> words[0].take(2).uppercase()
            else -> "${words[0].first()}${words.last().first()}".uppercase()
        }
    }
}
