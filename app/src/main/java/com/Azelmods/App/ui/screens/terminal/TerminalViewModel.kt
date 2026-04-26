package com.Azelmods.App.ui.screens.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor() : ViewModel() {
    
    private val repo = TerminalRepository()
    
    val history: StateFlow<List<TerminalEntry>> = repo.getHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    val hasRoot: Boolean get() = Shell.getShell().isRoot
    
    fun execute(command: String, useRoot: Boolean = false) {
        if (command.isBlank() || _isRunning.value) return
        viewModelScope.launch {
            _isRunning.value = true
            val output = if (useRoot && hasRoot)
                repo.executeRoot(command)
            else
                repo.executeShell(command)
            
            repo.saveEntry(
                TerminalEntry(
                    input = command,
                    output = output,
                    isError = output.startsWith("Error"),
                    timestamp = System.currentTimeMillis()
                )
            )
            _isRunning.value = false
        }
    }
    
    fun clear() {
        viewModelScope.launch { repo.clearHistory() }
    }
}
