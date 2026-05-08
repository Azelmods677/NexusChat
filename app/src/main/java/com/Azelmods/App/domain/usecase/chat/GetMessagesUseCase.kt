package com.Azelmods.App.domain.usecase.chat

import com.Azelmods.App.data.model.Message
import com.Azelmods.App.data.repository.ChatRepository
import com.Azelmods.App.util.Resource
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(chatId: String): Flow<Resource<List<Message>>> {
        require(chatId.isNotBlank()) { "Chat ID cannot be blank" }
        
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        return chatRepository.getMessages(chatId, currentUserId)
    }
}
