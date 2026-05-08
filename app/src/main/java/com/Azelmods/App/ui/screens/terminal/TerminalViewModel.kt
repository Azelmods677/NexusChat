package com.Azelmods.App.ui.screens.terminal

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * TerminalViewModel - REAL TERMINAL using libsu
 * 
 * This ViewModel wraps the RealTerminalEmulator to provide
 * a real terminal experience with full command execution support.
 */
@HiltViewModel
class TerminalViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {

    // Use the real terminal emulator
    private val terminalEmulator = RealTerminalEmulator(context)
    
    // Expose terminal state
    val lines: StateFlow<List<RealTerminalEmulator.TerminalLine>> = terminalEmulator.lines
    val isRoot: StateFlow<Boolean> = terminalEmulator.isRoot
    
    // Type alias for compatibility
    typealias TerminalLine = RealTerminalEmulator.TerminalLine
    
    fun execute(command: String) {
        viewModelScope.launch {
            terminalEmulator.execute(command)
        }
    }
    
    fun clear() {
        terminalEmulator.clear()
    }
    
    override fun onCleared() {
        super.onCleared()
        terminalEmulator.close()
    }
}
