package com.Azelmods.App.ui.screens.terminal

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

data class TerminalEntry(
    val id: String = "",
    val input: String = "",
    val output: String = "",
    val isError: Boolean = false,
    val timestamp: Long = 0L
)

class TerminalRepository {
    
    private val db = FirebaseDatabase.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    // Execute command with root (libsu)
    suspend fun executeRoot(command: String): String =
        withContext(Dispatchers.IO) {
            try {
                val result = Shell.cmd(command).exec()
                if (result.isSuccess) result.out.joinToString("\n")
                else result.err.joinToString("\n")
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    
    // Execute command without root (ProcessBuilder)
    suspend fun executeShell(command: String): String =
        withContext(Dispatchers.IO) {
            try {
                val parts = command.trim().split("\\s+".toRegex())
                val process = ProcessBuilder(parts)
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream
                    .bufferedReader().readText()
                process.waitFor()
                output.ifBlank { "[Sin salida]" }
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    
    // Save session to Firebase (real-time)
    suspend fun saveEntry(entry: TerminalEntry) {
        val ref = db.getReference("terminal/$uid/sessions").push()
        ref.setValue(entry.copy(id = ref.key ?: ""))
    }
    
    // Get history in real-time
    fun getHistory(): Flow<List<TerminalEntry>> = callbackFlow {
        val ref = db.getReference("terminal/$uid/sessions")
            .orderByChild("timestamp").limitToLast(500)
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                trySend(snap.children
                    .mapNotNull { it.getValue(TerminalEntry::class.java) })
            }
            override fun onCancelled(e: DatabaseError) {
                close(e.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
    
    suspend fun clearHistory() {
        db.getReference("terminal/$uid/sessions").removeValue()
    }
}
