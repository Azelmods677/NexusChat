package com.Azelmods.App.data.model

/**
 * Group settings model for welcome messages and other configurations.
 */
data class GroupSettings(
    val groupId: String = "",
    val welcomeEnabled: Boolean = false,
    val welcomeMessage: String = "👋 Welcome to the group, {name}!",
    val adminIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Formats the welcome message with the user's name
     */
    fun formatWelcomeMessage(userName: String): String {
        return welcomeMessage.replace("{name}", userName)
    }
}
