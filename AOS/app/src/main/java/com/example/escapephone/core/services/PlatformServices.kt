package com.example.escapephone.core.services

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.delay

enum class HapticEvent { success, error, selection, digitDiscovered }
interface HapticProvider { fun play(event: HapticEvent) }
class PlatformHapticProvider(context: Context) : HapticProvider {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java).defaultVibrator else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
    override fun play(event: HapticEvent) { val duration = if (event == HapticEvent.error) 70L else 30L; vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)) }
}
object NoOpHapticProvider : HapticProvider { override fun play(event: HapticEvent) = Unit }

sealed interface AdResult { data object rewarded : AdResult; data object skipped : AdResult; data object unavailable : AdResult; data class failed(val message: String) : AdResult }
interface AdGateway { suspend fun showRewardedAd(): AdResult }
object NoOpAdGateway : AdGateway { override suspend fun showRewardedAd() = AdResult.unavailable }
class FakeAdGateway(var result: AdResult = AdResult.rewarded) : AdGateway { override suspend fun showRewardedAd(): AdResult { delay(300); return result } }

enum class DeviceCommand { ping, arm, unlock, reset }
sealed interface PuzzleDeviceState { data object disconnected : PuzzleDeviceState; data object connecting : PuzzleDeviceState; data object connected : PuzzleDeviceState; data class error(val message: String) : PuzzleDeviceState }
interface PuzzleDeviceConnector { val state: PuzzleDeviceState; suspend fun connect(sessionId: String); suspend fun send(command: DeviceCommand); suspend fun disconnect() }
class NoOpPuzzleDeviceConnector : PuzzleDeviceConnector { override val state = PuzzleDeviceState.disconnected; override suspend fun connect(sessionId: String) = Unit; override suspend fun send(command: DeviceCommand) = Unit; override suspend fun disconnect() = Unit }
class FakePuzzleDeviceConnector : PuzzleDeviceConnector { override var state: PuzzleDeviceState = PuzzleDeviceState.disconnected; override suspend fun connect(sessionId: String) { state = PuzzleDeviceState.connecting; state = PuzzleDeviceState.connected }; override suspend fun send(command: DeviceCommand) = Unit; override suspend fun disconnect() { state = PuzzleDeviceState.disconnected } }
interface PuzzleOutput { suspend fun puzzleDidComplete(id: String) }
object NoOpPuzzleOutput : PuzzleOutput { override suspend fun puzzleDidComplete(id: String) = Unit }
