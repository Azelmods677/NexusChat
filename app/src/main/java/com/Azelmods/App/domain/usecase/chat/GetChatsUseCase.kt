package com.Azelmods.App.domain.usecase.chat

import com.Azelmods.App.data.model.Chat
import com.Azelmods.App.data.repository.ChatRepository
import com.Azelmods.App.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChatsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(userId: String): Flow<Resource<List<Chat>>> {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        
        return chatRepository.getChats(userId)
    }
}
