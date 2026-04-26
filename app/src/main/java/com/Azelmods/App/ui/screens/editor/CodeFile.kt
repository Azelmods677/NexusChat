package com.Azelmods.App.ui.screens.editor

data class CodeFile(
    val id: String = "",
    val name: String = "",
    val language: String = "python",
    val content: String = "",
    val userId: String = "",
    val timestamp: Long = 0L,
    val size: Long = 0L
)
