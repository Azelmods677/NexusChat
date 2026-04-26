package com.Azelmods.App.domain.usecase.chat

import com.Azelmods.App.data.model.Message
import com.Azelmods.App.data.repository.ChatRepository
import com.Azelmods.App.util.Resource
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, message: Message): Resource<Unit> {
        require(chatId.isNotBlank()) { "Chat ID cannot be blank" }
        require(message.content.isNotBlank()) { "Message must have content" }
        
        return chatRepository.sendMessage(chatId, message)
    }
}
