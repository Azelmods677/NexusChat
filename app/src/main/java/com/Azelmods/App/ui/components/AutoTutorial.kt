package com.Azelmods.App.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import com.Azelmods.App.data.preferences.TutorialPreferences
import com.Azelmods.App.data.tutorials.AppFeature
import com.Azelmods.App.data.tutorials.AppTutorials

/**
 * Auto Tutorial Composable
 * 
 * Automatically shows tutorial dialog on first visit to a feature
 * 
 * Usage:
 * ```kotlin
 * AutoTutorial(
 *     feature = AppFeature.MESSAGING,
 *     tutorialPreferences = tutorialPreferences
 * )
 * ```
 */
@Composable
fun AutoTutorial(
    feature: AppFeature,
    tutorialPreferences: TutorialPreferences
) {
    var showTutorial by remember { mutableStateOf(false) }
    
    // Check if tutorial should be shown
    LaunchedEffect(feature) {
        if (!tutorialPreferences.hasSeenTutorial(feature)) {
            showTutorial = true
        }
    }
    
    // Show tutorial dialog
    if (showTutorial) {
        FeatureTutorialDialog(
            title = AppTutorials.getTutorialTitle(feature),
            tutorialText = AppTutorials.getTutorial(feature),
            onDismiss = {
                tutorialPreferences.markTutorialAsSeen(feature)
                showTutorial = false
            }
        )
    }
}

/**
 * Manual Tutorial Button
 * 
 * Shows a help button that opens the tutorial on demand
 */
@Composable
fun TutorialButton(
    feature: AppFeature,
    onClick: () -> Unit = {}
) {
    var showTutorial by remember { mutableStateOf(false) }
    
    IconButton(
        onClick = {
            showTutorial = true
            onClick()
        }
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
            contentDescription = "Tutorial"
        )
    }
    
    if (showTutorial) {
        FeatureTutorialDialog(
            title = AppTutorials.getTutorialTitle(feature),
            tutorialText = AppTutorials.getTutorial(feature),
            onDismiss = { showTutorial = false }
        )
    }
}
