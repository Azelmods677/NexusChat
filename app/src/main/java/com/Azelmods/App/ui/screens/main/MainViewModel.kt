package com.Azelmods.App.ui.screens.main

import androidx.lifecycle.ViewModel
import com.Azelmods.App.data.manager.AppBackgroundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val appBackgroundManager: AppBackgroundManager
) : ViewModel()
