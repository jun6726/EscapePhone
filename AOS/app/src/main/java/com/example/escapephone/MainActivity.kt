package com.example.escapephone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.escapephone.app.EscapePhoneApp
import com.example.escapephone.app.GameViewModel
import com.example.escapephone.app.puzzleId
import com.example.escapephone.core.motion.AndroidMotionController
import com.example.escapephone.core.persistence.PlatformGameProgressStore
import com.example.escapephone.core.services.FakeAdGateway
import com.example.escapephone.core.services.FakePuzzleDeviceConnector
import com.example.escapephone.core.services.PlatformHapticProvider
import com.example.escapephone.core.services.HttpPlaytestAnalyticsUploader
import com.example.escapephone.designsystem.EscapePhoneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val gameViewModel: GameViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = GameViewModel(PlatformGameProgressStore(applicationContext), AndroidMotionController(this@MainActivity), PlatformHapticProvider(applicationContext), FakeAdGateway(), FakePuzzleDeviceConnector(), analyticsUploader = HttpPlaytestAnalyticsUploader(BuildConfig.PLAYTEST_ANALYTICS_ENDPOINT)) as T
            })
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            DisposableEffect(lifecycle) {
                val observer = LifecycleEventObserver { _, event -> when (event) { Lifecycle.Event.ON_RESUME -> { gameViewModel.startPuzzleSession(gameViewModel.uiState.value.currentScreen.puzzleId); gameViewModel.retryPendingAnalyticsUploads(); gameViewModel.start() }; Lifecycle.Event.ON_PAUSE -> gameViewModel.handleAppBackground(); else -> Unit } }
                lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer); gameViewModel.handleAppBackground() }
            }
            EscapePhoneTheme { EscapePhoneApp(gameViewModel) }
        }
    }
}
