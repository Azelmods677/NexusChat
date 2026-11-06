package com.Azelmods.App.data.chat

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ChatFolder(val id: String, val name: String, val chatIds: Set<String>)

/**
 * Carpetas y chats ocultos. Se guardan solo en este teléfono: el servidor no se entera de cómo
 * organiza nadie sus conversaciones ni de cuáles esconde.
 */
object ChatOrganizer {
    private const val PREFS = "chat_organizer"
    private const val KEY_FOLDERS = "folders"
    private const val KEY_HIDDEN = "hidden"

    private var prefs: SharedPreferences? = null

    private val _folders = MutableStateFlow<List<ChatFolder>>(emptyList())
    val folders: StateFlow<List<ChatFolder>> = _folders.asStateFlow()

    private val _hidden = MutableStateFlow<Set<String>>(emptySet())
    val hidden: StateFlow<Set<String>> = _hidden.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val stored = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = stored
        _hidden.value = stored.getStringSet(KEY_HIDDEN, emptySet()).orEmpty().toSet()
        _folders.value = runCatching { parseFolders(stored.getString(KEY_FOLDERS, null)) }.getOrDefault(emptyList())
    }

    fun isHidden(chatId: String?): Boolean = chatId != null && chatId in _hidden.value

    fun setHidden(chatId: String, hidden: Boolean) {
        _hidden.value = if (hidden) _hidden.value + chatId else _hidden.value - chatId
        prefs?.edit()?.putStringSet(KEY_HIDDEN, _hidden.value)?.apply()
    }

    fun createFolder(name: String, firstChat: String?) {
        val folder = ChatFolder(UUID.randomUUID().toString(), name.trim(), setOfNotNull(firstChat))
        save(_folders.value + folder)
    }

    fun deleteFolder(id: String) = save(_folders.value.filterNot { it.id == id })

    fun toggleInFolder(id: String, chatId: String) = save(
        _folders.value.map { folder ->
            if (folder.id != id) folder
            else folder.copy(chatIds = if (chatId in folder.chatIds) folder.chatIds - chatId else folder.chatIds + chatId)
        }
    )

    private fun save(list: List<ChatFolder>) {
        _folders.value = list
        prefs?.edit()?.putString(KEY_FOLDERS, encodeFolders(list))?.apply()
    }

    internal fun encodeFolders(list: List<ChatFolder>): String = JSONArray().apply {
        list.forEach { folder ->
            put(
                JSONObject()
                    .put("id", folder.id)
                    .put("name", folder.name)
                    .put("chats", JSONArray(folder.chatIds.toList()))
            )
        }
    }.toString()

    internal fun parseFolders(raw: String?): List<ChatFolder> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = JSONArray(raw)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val chats = item.getJSONArray("chats")
            ChatFolder(
                id = item.getString("id"),
                name = item.getString("name"),
                chatIds = List(chats.length()) { chats.getString(it) }.toSet()
            )
        }
    }
}
