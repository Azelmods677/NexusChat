package com.Azelmods.App.data.local

import com.Azelmods.App.data.local.dao.ChatDao
import com.Azelmods.App.data.local.dao.MessageDao
import com.Azelmods.App.data.local.dao.UserDao
import com.Azelmods.App.data.local.entity.CachedChatEntity
import com.Azelmods.App.data.local.entity.CachedMessageEntity
import com.Azelmods.App.data.local.entity.CachedUserEntity
import com.Azelmods.App.data.model.Chat
import com.Azelmods.App.data.model.Message
import com.Azelmods.App.data.model.MessageStatus
import com.Azelmods.App.data.model.User
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates all Room cache operations.
 *
 * Provides methods to:
 * - Save/retrieve messages from local cache
 * - Save/retrieve chats from local cache
 * - Save/retrieve user profiles from local cache
 */
@Singleton
class CacheManager @Inject constructor(
    private val db: AppDatabase
) {
    private val messageDao: MessageDao = db.messageDao()
    private val chatDao: ChatDao = db.chatDao()
    private val userDao: UserDao = db.userDao()

    // ════════════════════════════════════════════════════════════
    //  MESSAGES
    // ════════════════════════════════════════════════════════════

    /** Flow of cached messages for a chat (Room auto-updates). */
    fun observeMessages(chatId: String): Flow<List<CachedMessageEntity>> =
        messageDao.getMessages(chatId)

    /** Single-shot: get all cached messages for a chat (for initial load). */
    suspend fun getCachedMessages(chatId: String): List<Message> =
        messageDao.getMessagesOnce(chatId).map { it.toMessage() }

    /** Save a batch of messages (replaces existing for upsert). */
    suspend fun cacheMessages(messages: List<Message>) {
        messageDao.insertMessages(messages.map { it.toEntity() })
    }

    /** Cache a single message. */
    suspend fun cacheMessage(message: Message) {
        messageDao.insertMessage(message.toEntity())
    }

    /** Remove all messages for a given chat (e.g. on chat deletion). */
    suspend fun clearChatMessages(chatId: String) {
        messageDao.deleteMessagesByChat(chatId)
    }

    // ════════════════════════════════════════════════════════════
    //  CHATS
    // ════════════════════════════════════════════════════════════

    /** Flow of all cached chats. */
    fun observeChats(): Flow<List<CachedChatEntity>> = chatDao.getChats()

    /** Single-shot: get all cached chats. */
    suspend fun getCachedChats(): List<CachedChatEntity> = chatDao.getChatsOnce()

    /** Cache a list of Chat objects (converts to entities internally). */
    suspend fun cacheChats(chats: List<Chat>) {
        chatDao.insertChats(chats.map { it.toEntity() })
    }

    /** Cache a single Chat. */
    suspend fun cacheChat(chat: Chat) {
        chatDao.insertChat(chat.toEntity())
    }

    /** Remove all cached chats. */
    suspend fun clearAllChats() {
        chatDao.deleteAllChats()
    }

    // ════════════════════════════════════════════════════════════
    //  USERS
    // ════════════════════════════════════════════════════════════

    /** Single-shot: get a cached user profile by UID. */
    suspend fun getCachedUser(userId: String): User? =
        userDao.getUserById(userId)?.toUser()

    /** Cache a user profile. */
    suspend fun cacheUser(user: User) {
        userDao.insertUser(user.toEntity())
    }

    /** Cache a batch of user profiles. */
    suspend fun cacheUsers(users: List<User>) {
        userDao.insertUsers(users.map { it.toEntity() })
    }

    // ════════════════════════════════════════════════════════════
    //  CLEAR ALL (e.g. logout)
    // ════════════════════════════════════════════════════════════

    suspend fun clearAll() {
        messageDao.deleteAllMessages()
        chatDao.deleteAllChats()
        userDao.deleteAllUsers()
    }
}

// ═══════════════════════════════════════════════════════════════
//  EXTENSION FUNCTIONS: Model ↔ Entity conversion
// ═══════════════════════════════════════════════════════════════

private fun Message.toEntity() = CachedMessageEntity(
    messageId = messageId,
    chatId = chatId,
    senderId = senderId,
    senderName = senderName,
    content = content,
    isEncrypted = isEncrypted,
    encryptedPayload = encryptedPayload,
    timestamp = timestamp,
    status = status.name,
    isEdited = isEdited || edited,
    editedAt = editedAt,
    replyTo = replyTo,
    reactionsJson = Converters.mapToString(reactions),
    mediaUrl = mediaUrl,
    mediaType = mediaType,
    deletedForJson = Converters.booleanMapToString(deletedFor),
    deletedForEveryone = deletedForEveryone,
    forwardedFrom = forwardedFrom
)

private fun CachedMessageEntity.toMessage() = Message(
    messageId = messageId,
    chatId = chatId,
    senderId = senderId,
    senderName = senderName,
    content = content,
    isEncrypted = isEncrypted,
    encryptedPayload = encryptedPayload,
    timestamp = timestamp,
    status = try { MessageStatus.valueOf(status) } catch (_: Exception) { MessageStatus.SENT },
    isEdited = isEdited,
    edited = isEdited,
    editedAt = editedAt,
    replyTo = replyTo,
    reactions = Converters.stringToMap(reactionsJson),
    mediaUrl = mediaUrl,
    mediaType = mediaType,
    deletedFor = Converters.stringToBooleanMap(deletedForJson),
    deletedForEveryone = deletedForEveryone,
    forwardedFrom = forwardedFrom
)

private fun Chat.toEntity(): CachedChatEntity {
    val participantsList = participants.ifEmpty { participantIds }
    return CachedChatEntity(
        chatId = chatId,
        lastMessage = lastMessage,
        lastMessageTime = lastMessageTime,
        lastMessageSenderId = lastMessageSenderId,
        participantIdsJson = Converters.listToString(participantsList),
        participantNamesJson = Converters.mapToString(participantNames),
        participantPhotosJson = Converters.mapToString(participantPhotos),
        chatType = chatType.name,
        isPinned = isPinned,
        isMuted = isMuted,
        isArchived = isArchived,
        isE2EE = isE2EE,
        unreadCount = unreadCount.values.sum()
    )
}

private fun User.toEntity() = CachedUserEntity(
    uid = uid,
    name = name,
    displayName = displayName,
    username = username,
    email = email,
    phone = phone,
    photoUrl = photoUrl ?: "",
    coverUrl = coverUrl ?: "",
    bio = bio,
    statusText = status,
    isOnline = isOnline,
    lastSeen = lastSeen
)

private fun CachedUserEntity.toUser() = User(
    uid = uid,
    name = name,
    displayName = displayName,
    username = username,
    email = email,
    phone = phone,
    photoUrl = photoUrl,
    coverUrl = coverUrl,
    bio = bio,
    status = statusText,
    isOnline = isOnline,
    lastSeen = lastSeen
)

internal fun CachedChatEntity.toChat(): Chat {
    val participantIds = Converters.stringToStringList(participantIdsJson)
    val names = Converters.stringToMap(participantNamesJson)
    val photos = Converters.stringToMap(participantPhotosJson)
    return Chat(
        chatId = chatId,
        participants = participantIds,
        participantIds = participantIds,
        participantNames = names,
        participantPhotos = photos,
        lastMessage = lastMessage,
        lastMessageTime = lastMessageTime,
        lastMessageSenderId = lastMessageSenderId,
        unreadCount = emptyMap(),
        isTyping = emptyMap(),
        chatType = try { com.Azelmods.App.data.model.ChatType.valueOf(chatType) } catch (_: Exception) { com.Azelmods.App.data.model.ChatType.PRIVATE },
        isPinned = isPinned,
        isMuted = isMuted,
        isArchived = isArchived,
        isE2EE = isE2EE
    )
}
