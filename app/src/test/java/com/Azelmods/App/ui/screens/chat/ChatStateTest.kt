package com.Azelmods.App.ui.screens.chat

import com.Azelmods.App.data.model.Message
import com.Azelmods.App.data.model.MessageStatus
import com.Azelmods.App.data.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ChatState logic.
 *
 * These tests verify the core state management of the chat feature
 * without requiring Firebase, Room, or other Android framework dependencies.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatStateTest {

    @Test
    fun `initial state is empty with no error`() = runTest {
        val state = ChatState()
        assertEquals(true, state.messages.isEmpty())
        assertNull(state.error)
        assertEquals(false, state.isLoading)
        assertEquals(false, state.isTyping)
    }

    @Test
    fun `adding a message updates state`() = runTest {
        val state = ChatState()
        val message = Message(
            messageId = "msg_1",
            senderId = "user_1",
            content = "Hello",
            timestamp = 1000L,
            status = MessageStatus.SENT
        )
        val newState = state.copy(messages = state.messages + message)
        assertEquals(1, newState.messages.size)
        assertEquals("Hello", newState.messages.first().content)
    }

    @Test
    fun `translated message appears in translatedMessages map`() = runTest {
        val state = ChatState()
        val newState = state.copy(
            translatedMessages = mapOf("msg_1" to "Hola")
        )
        assertEquals("Hola", newState.translatedMessages["msg_1"])
        assertNull(newState.translatedMessages["msg_2"])
    }

    @Test
    fun `replyingTo state is cleared after send`() = runTest {
        val replyMessage = Message(
            messageId = "reply_to",
            senderId = "user_2",
            content = "Original",
            timestamp = 1000L,
            status = MessageStatus.SENT
        )
        val state = ChatState(replyingTo = replyMessage)
        val clearedState = state.copy(replyingTo = null)
        assertNull(clearedState.replyingTo)
    }

    @Test
    fun `error state is set correctly`() = runTest {
        val state = ChatState(error = "Network error")
        assertEquals("Network error", state.error)
    }

    @Test
    fun `typing indicator appears when isTyping is true`() = runTest {
        val state = ChatState(isTyping = true)
        assertTrue(state.isTyping)
    }

    @Test
    fun `translatingMessageIds tracks active translations`() = runTest {
        val state = ChatState(translatingMessageIds = setOf("msg_1", "msg_2"))
        assertTrue("msg_1" in state.translatingMessageIds)
        assertTrue("msg_2" in state.translatingMessageIds)
        assertEquals(2, state.translatingMessageIds.size)
    }

    @Test
    fun `pagination state is tracked correctly`() = runTest {
        val state = ChatState(
            hasMoreMessages = true,
            isLoadingMore = false,
            earliestMessageTimestamp = 1000L,
            earliestMessageId = "first_msg"
        )
        assertTrue(state.hasMoreMessages)
        assertEquals(false, state.isLoadingMore)
        assertEquals(1000L, state.earliestMessageTimestamp)
        assertEquals("first_msg", state.earliestMessageId)
    }

    @Test
    fun `ephemeral mode state`() = runTest {
        val state = ChatState(
            isEphemeralMode = true,
            ephemeralDuration = 30L
        )
        assertTrue(state.isEphemeralMode)
        assertEquals(30L, state.ephemeralDuration)
    }

    @Test
    fun `contact info is stored in state`() = runTest {
        val contact = User(
            uid = "user_123",
            displayName = "Alice",
            email = "alice@example.com",
            photoUrl = "https://example.com/photo.jpg"
        )
        val state = ChatState(contact = contact)
        assertNotNull(state.contact)
        assertEquals("Alice", state.contact?.displayName)
        assertEquals("user_123", state.contact?.uid)
    }

    @Test
    fun `editing message state`() = runTest {
        val editingMessage = Message(
            messageId = "selected",
            senderId = "user_1",
            content = "Selected text",
            timestamp = 1000L,
            status = MessageStatus.SENT
        )
        val state = ChatState(editingMessage = editingMessage)
        assertNotNull(state.editingMessage)
        assertEquals("Selected text", state.editingMessage?.content)
    }
}
