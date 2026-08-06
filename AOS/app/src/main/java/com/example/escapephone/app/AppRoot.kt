package com.example.escapephone.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.escapephone.core.model.GameStage
import com.example.escapephone.core.theme.ThemeId
import com.example.escapephone.core.theme.ThemeProgress
import com.example.escapephone.core.theme.ThemeProgressStore
import com.example.escapephone.core.theme.ThemeRegistry
import com.example.escapephone.core.theme.ThemeSpecificState
import com.example.escapephone.core.theme.ThemeStatus
import com.example.escapephone.designsystem.Background
import com.example.escapephone.feature.themes.convenienceStore.ConvenienceStoreApp
import com.example.escapephone.feature.themeselection.ThemeMainMenuScreen
import com.example.escapephone.feature.themeselection.ThemeSelectionScreen

sealed class AppLaunchRoute {
    object ThemeSelection : AppLaunchRoute()
    object GlobalSettings : AppLaunchRoute()
    data class ThemeMainMenu(val themeId: ThemeId) : AppLaunchRoute()
    data class TheLastCommit(val themeId: ThemeId = ThemeId.theLastCommit) : AppLaunchRoute()
    object ConvenienceStorePlaying : AppLaunchRoute()
}

@Composable
fun AppRoot(
    themeProgressStore: ThemeProgressStore,
    theLastCommitViewModel: GameViewModel,
    convenienceStoreViewModelFactory: () -> ConvenienceStoreViewModel
) {
    LaunchedEffect(Unit) { themeProgressStore.migrateLegacyProgressIfNeeded() }
    var route by remember { mutableStateOf<AppLaunchRoute>(AppLaunchRoute.ThemeSelection) }
    var progressVersion by remember { mutableIntStateOf(0) }
    val convenienceStoreViewModel = remember { convenienceStoreViewModelFactory() }

    Surface(Modifier.fillMaxSize().safeDrawingPadding(), color = Background) {
        when (val current = route) {
            is AppLaunchRoute.ThemeSelection -> {
                key(progressVersion) {
                    ThemeSelectionScreen(
                        themeMetadataList = ThemeRegistry.themes,
                        progressFor = { themeId -> themeProgressStore.loadThemeProgress(themeId) },
                        onStartTheme = { themeId -> startTheme(themeId, theLastCommitViewModel); route = openThemeDetails(themeId) },
                        onContinueTheme = { themeId -> continueTheme(themeId, theLastCommitViewModel); route = openThemeDetails(themeId) },
                        onOpenSettings = { route = AppLaunchRoute.GlobalSettings },
                        onResetAll = { themeProgressStore.resetAllThemeProgress(); theLastCommitViewModel.reset(); convenienceStoreViewModel.reset(); progressVersion++ }
                    )
                }
            }
            is AppLaunchRoute.GlobalSettings -> {
                BackHandler(enabled = true) { route = AppLaunchRoute.ThemeSelection }
                val settingsState by theLastCommitViewModel.uiState.collectAsState()
                SettingsScreen(settingsState, theLastCommitViewModel, onBack = { route = AppLaunchRoute.ThemeSelection })
            }
            is AppLaunchRoute.ThemeMainMenu -> {
                BackHandler(enabled = true) { route = AppLaunchRoute.ThemeSelection }
                ThemeMainMenuScreen(
                    themeId = current.themeId,
                    themeMetadata = ThemeRegistry.metadataFor(current.themeId),
                    themeProgress = themeProgressStore.loadThemeProgress(current.themeId),
                    theLastCommitViewModel = theLastCommitViewModel,
                    convenienceStoreViewModel = convenienceStoreViewModel,
                    onBackToThemeSelection = {
                        syncThemeProgressFromGameProgress(themeProgressStore, theLastCommitViewModel)
                        progressVersion++
                        route = AppLaunchRoute.ThemeSelection
                    },
                    onResetTheme = { progressVersion++ },
                    onEnterConvenienceStore = { route = AppLaunchRoute.ConvenienceStorePlaying }
                )
            }
            is AppLaunchRoute.ConvenienceStorePlaying -> {
                ConvenienceStoreApp(convenienceStoreViewModel, onExitToThemeMenu = { progressVersion++; route = AppLaunchRoute.ThemeMainMenu(ThemeId.convenienceStoreLoop) })
            }
            is AppLaunchRoute.TheLastCommit -> {
                BackHandler(enabled = true) {
                    if (theLastCommitViewModel.uiState.value.currentScreen == Screen.mainMenu) {
                        syncThemeProgressFromGameProgress(themeProgressStore, theLastCommitViewModel)
                        progressVersion++
                        route = AppLaunchRoute.ThemeSelection
                    } else {
                        theLastCommitViewModel.handleBackNavigation()
                    }
                }
                EscapePhoneApp(theLastCommitViewModel)
            }
        }
    }
}

private fun openThemeDetails(themeId: ThemeId): AppLaunchRoute = when (themeId) {
    ThemeId.theLastCommit -> AppLaunchRoute.TheLastCommit(themeId)
    ThemeId.convenienceStoreLoop -> AppLaunchRoute.ThemeMainMenu(themeId)
}

fun selectTheme(themeId: ThemeId, viewModel: GameViewModel) {
    when (themeId) {
        ThemeId.theLastCommit -> if (viewModel.hasSavedGame()) viewModel.continueGame() else viewModel.navigate(Screen.mainMenu)
        ThemeId.convenienceStoreLoop -> Unit
    }
}

private fun startTheme(themeId: ThemeId, viewModel: GameViewModel) {
    when (themeId) {
        ThemeId.theLastCommit -> viewModel.startNewGame()
        ThemeId.convenienceStoreLoop -> Unit
    }
}

fun continueTheme(themeId: ThemeId, viewModel: GameViewModel) {
    when (themeId) {
        ThemeId.theLastCommit -> if (viewModel.hasSavedGame()) viewModel.continueGame() else viewModel.navigate(Screen.mainMenu)
        ThemeId.convenienceStoreLoop -> Unit
    }
}

fun resetSelectedTheme(themeId: ThemeId, store: ThemeProgressStore, viewModel: GameViewModel) {
    store.resetThemeProgress(themeId)
    if (themeId == ThemeId.theLastCommit) viewModel.reset()
}

private fun syncThemeProgressFromGameProgress(store: ThemeProgressStore, viewModel: GameViewModel) {
    val progress = viewModel.uiState.value.gameProgress
    val status = when {
        progress.currentStage == GameStage.gameCompleted -> ThemeStatus.completed
        progress.currentStage != GameStage.notStarted -> ThemeStatus.inProgress
        else -> ThemeStatus.notStarted
    }
    store.saveThemeProgress(
        ThemeProgress(
            themeId = ThemeId.theLastCommit,
            themeStatus = status,
            currentStageId = progress.currentStage.name,
            startedAt = progress.startedAt,
            lastSavedAt = progress.lastSavedAt,
            completedAt = progress.completedAt,
            hintCount = progress.hintCount,
            collectedEvidenceIds = progress.collectedEvidenceIds,
            endingId = progress.endingType?.name,
            themeSpecificState = ThemeSpecificState.TheLastCommitState()
        )
    )
}
