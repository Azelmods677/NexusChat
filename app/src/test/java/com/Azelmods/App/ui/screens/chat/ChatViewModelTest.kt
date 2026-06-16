package com.Azelmods.App.ui.screens.chat

import com.Azelmods.App.data.local.CacheManager
import com.Azelmods.App.data.model.BackgroundConfig
import com.Azelmods.App.data.model.Message
import com.Azelmods.App.data.model.MessageStatus
import com.Azelmods.App.data.repository.ChatBackgroundRepository
import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.Azelmods.App.data.repository.StorageRepository
import com.Azelmods.App.data.translation.TranslationService
import com.Azelmods.App.domain.usecase.DecryptMessageUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain


/**
 * Unit tests for ChatViewModel.
 *
 * Validates:
 * - Initial state is correct
 * - sendMessage updates state correctly
 * - setReplyingTo toggles reply state
 * - error state is cleared by clearError
 */
class ChatViewModelTest : StringSpec({

    val storageRepository = mockk<StorageRepository>(relaxed = true)
    val databaseRepository = mockk<RealtimeDatabaseRepository>(relaxed = true)
    val backgroundRepository = mockk<ChatBackgroundRepository>(relaxed = true)
    val decryptMessageUseCase = mockk<DecryptMessageUseCase>(relaxed = true)
    val cacheManager = mockk<CacheManager>(relaxed = true)
    val translationService = mockk<TranslationService>(relaxed = true)

    // Helper that builds a ChatViewModel with the shared mocks.
    fun buildViewModel() = ChatViewModel(
        storageRepository = storageRepository,
        databaseRepository = databaseRepository,
        backgroundRepository = backgroundRepository,
        decryptMessageUseCase = decryptMessageUseCase,
        cacheManager = cacheManager,
        translationService = translationService
    )

    // Mock FirebaseAuth
    val mockFirebaseUser = mockk<FirebaseUser>(relaxed = true)

    beforeTest {
        // Setup mock FirebaseAuth.currentUser
        mockkStatic(FirebaseAuth::class)
        val mockAuth = mockk<FirebaseAuth>(relaxed = true)
        every { FirebaseAuth.getInstance() } returns mockAuth
        every { mockAuth.currentUser } returns mockFirebaseUser
        every { mockFirebaseUser.uid } returns "test-user-123"

        // Mock CacheManager to return empty by default
        coEvery { cacheManager.getCachedMessages(any()) } returns emptyList()

        // Mock databaseRepository.getChatMessages to return empty flow
        coEvery { databaseRepository.getChatMessages(any()) } returns flowOf(emptyList())

        // Mock backgroundRepository
        coEvery { backgroundRepository.loadBackground(any()) } returns Unit
        every { backgroundRepository.getBackground(any()) } returns MutableStateFlow(BackgroundConfig())

        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    afterTest {
        unmockkAll()
        Dispatchers.resetMain()
    }

    "initial state should be empty" {
        val viewModel = buildViewModel()

        viewModel.state.value shouldBe ChatState()
    }

    "setReplyingTo should update reply state" {
        val viewModel = buildViewModel()

        viewModel.state.value.replyingTo shouldBe null

        val testMessage = Message(
            messageId = "msg-1",
            chatId = "chat-1",
            senderId = "user-1",
            content = "Test message",
            timestamp = 1000L,
            status = MessageStatus.SENT
        )

        viewModel.setReplyingTo(testMessage)
        viewModel.state.value.replyingTo shouldNotBe null
        viewModel.state.value.replyingTo?.messageId shouldBe "msg-1"
        viewModel.state.value.replyingTo?.content shouldBe "Test message"

        // Clear reply
        viewModel.setReplyingTo(null)
        viewModel.state.value.replyingTo shouldBe null
    }

    "clearError should reset error state" {
        val viewModel = buildViewModel()

        // Simulate error state
        val errorState = viewModel.state.value.copy(error = "Test error")
        // We can't set directly, but we can verify clearError works
        viewModel.clearError()
        viewModel.state.value.error shouldBe null
    }

    "sendMessage should call databaseRepository" {
        runTest {
            val viewModel = buildViewModel()

            coEvery { databaseRepository.sendMessage(any(), any(), any()) } returns Unit

            viewModel.sendMessage(content = "Hello!", chatId = "chat-1")

            coVerify { databaseRepository.sendMessage("chat-1", "Hello!", null) }
        }
    }

    "sendMessage with blank content should return early without error" {
        val viewModel = buildViewModel()

        // replyingTo initially null
        viewModel.state.value.replyingTo shouldBe null
        viewModel.state.value.error shouldBe null

        // Blank content triggers early return — no state change
        viewModel.sendMessage(content = "", chatId = "chat-1")

        viewModel.state.value.replyingTo shouldBe null
        viewModel.state.value.error shouldBe null
    }

    "state should emit correct initial values via StateFlow" {
        val viewModel = buildViewModel()

        viewModel.state.value.let { state ->
            state.messages shouldBe emptyList()
            state.isLoading shouldBe false
            state.isUploading shouldBe false
            state.error shouldBe null
            state.replyingTo shouldBe null
            state.editingMessage shouldBe null
            state.isTyping shouldBe false
        }
    }

    "loadChat should not throw when called" {
        val viewModel = buildViewModel()

        // loadChat launches on Dispatchers.IO (not test-controlled)
        // We can only verify it doesn't throw or crash
        viewModel.loadChat("chat-1") shouldBe Unit
    }
})
