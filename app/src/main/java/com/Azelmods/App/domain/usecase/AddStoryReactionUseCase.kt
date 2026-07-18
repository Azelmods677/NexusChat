package com.Azelmods.App.domain.usecase

import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for adding emoji reactions to stories.
 *
 * Features:
 * - Quick emoji reaction while viewing story
 * - Stores reaction in stories_reactions/{ownerId}/{storyId}/{uid}
 * - One reaction per user per story (reacting again replaces the emoji)
 *
 * La versión anterior escribía en stories/{storyId}/reactions — ruta que no
 * existe en el esquema real (stories/{ownerId}/{storyId}) y que las reglas de
 * seguridad rechazan para cualquier usuario distinto del dueño. Ahora delega
 * en el repositorio, que usa el nodo stories_reactions con reglas propias.
 */
class AddStoryReactionUseCase @Inject constructor(
    private val repository: RealtimeDatabaseRepository
) {
    /**
     * Adds an emoji reaction to a story.
     *
     * @param storyOwnerId The story author's user ID
     * @param storyId The story ID to react to
     * @param emoji The emoji reaction
     * @return Result with success or error
     */
    suspend operator fun invoke(
        storyOwnerId: String,
        storyId: String,
        emoji: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            repository.addStoryReaction(storyOwnerId, storyId, emoji)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
