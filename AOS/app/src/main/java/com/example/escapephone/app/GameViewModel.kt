package com.example.escapephone.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.escapephone.BuildConfig
import com.example.escapephone.core.game.FlashlightPuzzleEngine
import com.example.escapephone.core.game.FlashlightPuzzleEvent
import com.example.escapephone.core.game.LowPassFilter
import com.example.escapephone.core.game.MessengerPuzzleEngine
import com.example.escapephone.core.game.ServerCodeEngine
import com.example.escapephone.core.game.ServerCodeResult
import com.example.escapephone.core.model.DeviceAttitude
import com.example.escapephone.core.model.FlashlightControlMode
import com.example.escapephone.core.model.GameProgress
import com.example.escapephone.core.model.GameStage
import com.example.escapephone.core.model.EndingType
import com.example.escapephone.core.model.PuzzleMessage
import com.example.escapephone.core.model.PlayerFeedback
import com.example.escapephone.core.model.PlaytestReport
import com.example.escapephone.core.model.PuzzleAnalytics
import com.example.escapephone.core.model.PuzzleExitEvent
import com.example.escapephone.core.model.PuzzleExitReason
import com.example.escapephone.core.model.AnalyticsConsentStatus
import com.example.escapephone.core.model.PendingAnalyticsUpload
import com.example.escapephone.core.model.PlaytestUploadEnvelope
import com.example.escapephone.core.motion.MotionController
import com.example.escapephone.core.persistence.GameProgressStore
import com.example.escapephone.core.persistence.SystemTimeProvider
import com.example.escapephone.core.persistence.TimeProvider
import com.example.escapephone.core.services.AdGateway
import com.example.escapephone.core.services.HapticEvent
import com.example.escapephone.core.services.HapticProvider
import com.example.escapephone.core.services.PuzzleDeviceConnector
import com.example.escapephone.core.services.NoOpPlaytestAnalyticsUploader
import com.example.escapephone.core.services.PlaytestAnalyticsUploader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class Screen(val screenId: String) { mainMenu("main_menu"), intro("intro"), phoneHome("phone_home"), messengerPuzzle("messenger_puzzle"), flashlightPuzzle("flashlight_puzzle"), encryptedNote("encrypted_note_screen"), audioRecordPuzzle("audio_record_puzzle"), commitGraphPuzzle("commit_graph_puzzle"), accessLogPuzzle("access_log_puzzle"), serverConsole("server_console"), finalDecision("final_decision"), ending("ending"), settings("settings"), deviceAnalytics("device_analytics"), developerMenu("developer_menu") }

val Screen.puzzleId: String? get() = when (this) {
    Screen.messengerPuzzle -> "messenger_order"
    Screen.flashlightPuzzle -> "flashlight_search"
    Screen.encryptedNote -> "encrypted_note"
    Screen.audioRecordPuzzle -> "audio_record"
    Screen.commitGraphPuzzle -> "commit_graph"
    Screen.accessLogPuzzle -> "access_log"
    Screen.serverConsole -> "server_code"
    else -> null
}

data class GameUiState(
    val gameProgress: GameProgress = GameProgress(),
    val currentScreen: Screen = Screen.mainMenu,
    val messages: List<PuzzleMessage> = MessengerPuzzleEngine.messages,
    val serverCodeInput: String = "",
    val flashlightX: Float = .5f,
    val flashlightY: Float = .5f,
    val pitch: Double = 0.0,
    val roll: Double = 0.0,
    val yaw: Double = 0.0,
    val hintText: String? = null,
    val notice: String? = null,
    val isSolved: Boolean = false
) { val currentStage get() = gameProgress.currentStage }

class GameViewModel(
    private val gameProgressStore: GameProgressStore,
    val motionController: MotionController,
    private val hapticProvider: HapticProvider,
    val adGateway: AdGateway,
    val puzzleDeviceConnector: PuzzleDeviceConnector,
    private val timeProvider: TimeProvider = SystemTimeProvider,
    private val analyticsUploader: PlaytestAnalyticsUploader = NoOpPlaytestAnalyticsUploader
) : ViewModel() {
    private var gameProgress = gameProgressStore.load() ?: GameProgress()
    private var messengerEngine = MessengerPuzzleEngine()
    private var flashlightEngine = FlashlightPuzzleEngine(discoveredDigits = gameProgress.discoveredDigits)
    private var serverEngine = ServerCodeEngine()
    private var activePuzzleId: String? = null
    private var activePuzzleStartedAt: Long? = null
    private var uploadJob: Job? = null
    private val analyticsJson = Json { prettyPrint = false; encodeDefaults = true }
    private val pitchFilter = LowPassFilter(); private val rollFilter = LowPassFilter()
    private val _uiState = MutableStateFlow(GameUiState(gameProgress = normalizedProgress(), messages = messengerEngine.messages))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
    val isAvailable get() = motionController.isAvailable

    init { motionController.onAttitudeChanged = ::handleAttitude; gameProgress = _uiState.value.gameProgress }
    private fun normalizedProgress(): GameProgress { if (!motionController.isAvailable && gameProgress.controlMode == FlashlightControlMode.motion) gameProgress = gameProgress.copy(controlMode = FlashlightControlMode.touch); return gameProgress }
    private fun publishProgress() { _uiState.update { it.copy(gameProgress = gameProgress) } }
    fun save() { gameProgress = gameProgress.copy(lastSavedAt = timeProvider.now()); gameProgressStore.save(gameProgress); publishProgress() }
    fun reset() { activePuzzleId = null; activePuzzleStartedAt = null; val consent = gameProgress.analyticsConsentStatus; val consentVersion = gameProgress.analyticsConsentVersion; gameProgressStore.reset(); gameProgress = GameProgress(controlMode = if (isAvailable) FlashlightControlMode.motion else FlashlightControlMode.touch, analyticsConsentStatus = consent, analyticsConsentVersion = consentVersion); resetEngines(); _uiState.value = GameUiState(gameProgress = gameProgress) }
    fun startNewGame() { activePuzzleId = null; activePuzzleStartedAt = null; val history = if (isAnalyticsConsentGranted) archivedHistory() else emptyList(); val sessionId = if (isAnalyticsConsentGranted) java.util.UUID.randomUUID().toString() else null; gameProgress = GameProgress(startedAt = timeProvider.now(), controlMode = if (isAvailable) FlashlightControlMode.motion else FlashlightControlMode.touch, playtestHistory = history, analyticsConsentStatus = gameProgress.analyticsConsentStatus, analyticsConsentVersion = gameProgress.analyticsConsentVersion, anonymousSessionId = sessionId, pendingAnalyticsUploads = gameProgress.pendingAnalyticsUploads); resetEngines(); save(); _uiState.value = GameUiState(gameProgress = gameProgress, currentScreen = Screen.intro, messages = messengerEngine.messages); flushAnalyticsUploads(1_500) }
    fun continueGame() { navigate(routeForProgress()) }
    fun completeIntro() { if (gameProgress.currentStage == GameStage.notStarted) gameProgress = gameProgress.copy(currentStage = GameStage.introCompleted, startedAt = gameProgress.startedAt ?: timeProvider.now()); save(); navigate(Screen.phoneHome) }
    fun completeMessengerPuzzle() { if (!gameProgress.messengerSolved) { completePuzzleSession("messenger_order"); gameProgress = gameProgress.copy(currentStage = GameStage.messengerSolved, messengerSolved = true); hapticProvider.play(HapticEvent.success); save() } }
    fun completeFlashlightPuzzle() { if (!gameProgress.flashlightSolved) { completePuzzleSession("flashlight_search"); gameProgress = gameProgress.copy(currentStage = GameStage.flashlightSolved, flashlightSolved = true, discoveredDigits = listOf(4, 1, 7)); hapticProvider.play(HapticEvent.success); save() } }
    fun completeEncryptedNotePuzzle() { if (!gameProgress.encryptedNoteSolved) { completePuzzleSession("encrypted_note"); gameProgress = gameProgress.copy(currentStage = GameStage.encryptedNoteSolved, encryptedNoteSolved = true, collectedEvidenceIds = gameProgress.collectedEvidenceIds + "note_trace_collection"); hapticProvider.play(HapticEvent.success); save() } }
    fun completeAudioRecordPuzzle() { if (!gameProgress.audioRecordSolved) { completePuzzleSession("audio_record"); gameProgress = gameProgress.copy(currentStage = GameStage.audioRecordSolved, audioRecordSolved = true, collectedEvidenceIds = gameProgress.collectedEvidenceIds + "audio_restore_order"); hapticProvider.play(HapticEvent.success); save() } }
    fun completeCommitGraphPuzzle() { if (!gameProgress.commitGraphSolved) { completePuzzleSession("commit_graph"); gameProgress = gameProgress.copy(currentStage = GameStage.commitGraphSolved, commitGraphSolved = true, collectedEvidenceIds = gameProgress.collectedEvidenceIds + setOf("root_m", "message_0114")); hapticProvider.play(HapticEvent.success); save() } }
    fun completeAccessLogPuzzle(evidenceIds: Set<String> = emptySet()) { if (!gameProgress.accessLogSolved) { completePuzzleSession("access_log"); gameProgress = gameProgress.copy(currentStage = GameStage.accessLogSolved, accessLogSolved = true, collectedEvidenceIds = gameProgress.collectedEvidenceIds + evidenceIds + "cto_meeting_terminal"); hapticProvider.play(HapticEvent.success); save() } }
    fun moveMessageUp(index: Int) { messengerEngine.moveMessageUp(index); hapticProvider.play(HapticEvent.selection); _uiState.update { it.copy(messages = messengerEngine.messages) } }
    fun moveMessageDown(index: Int) { messengerEngine.moveMessageDown(index); hapticProvider.play(HapticEvent.selection); _uiState.update { it.copy(messages = messengerEngine.messages) } }
    fun submitMessengerOrder(): Boolean { val result = messengerEngine.submitMessengerOrder(); if (result) completeMessengerPuzzle() else { recordWrongAttempt("messenger_order", "messageOrderIncorrect"); hapticProvider.play(HapticEvent.error) }; _uiState.update { it.copy(isSolved = result) }; return result }
    fun appendServerCodeDigit(digit: Int) { serverEngine.appendServerCodeDigit(digit); publishServerCode() }
    fun removeServerCodeDigit() { serverEngine.removeServerCodeDigit(); publishServerCode() }
    fun clearServerCode() { serverEngine.clearServerCode(); publishServerCode() }
    fun submitServerCode(): ServerCodeResult { val result = serverEngine.submitServerCode(); if (result == ServerCodeResult.success && gameProgress.accessLogSolved) { completePuzzleSession("server_code"); gameProgress = gameProgress.copy(currentStage = GameStage.gameCompleted, completedAt = timeProvider.now()); hapticProvider.play(HapticEvent.success); save(); navigate(Screen.finalDecision) } else if (result == ServerCodeResult.incorrect) { recordWrongAttempt("server_code", "serverCodeIncorrect"); hapticProvider.play(HapticEvent.error) }; return result }
    fun selectPublicDisclosure() = selectEnding(EndingType.publicDisclosure)
    fun selectEncryptedArchive() = selectEnding(EndingType.encryptedArchive)
    private fun selectEnding(endingType: EndingType) { gameProgress = gameProgress.copy(currentStage = GameStage.gameCompleted, endingType = endingType, completedAt = gameProgress.completedAt ?: timeProvider.now()); save(); navigate(Screen.ending) }
    fun requestHint(hintId: String, text: String) { val puzzleId = hintId.substringBefore('.'); if (isAnalyticsConsentGranted) updatePuzzleAnalytics(puzzleId) { it.copy(hintViewCount = it.hintViewCount + 1) }; if (hintId !in gameProgress.seenHintIds) gameProgress = gameProgress.copy(hintCount = gameProgress.hintCount + 1, seenHintIds = gameProgress.seenHintIds + hintId); save(); showHint(text) }
    fun startPuzzleSession(puzzleId: String?) {
        if (!isAnalyticsConsentGranted || puzzleId == null || activePuzzleId == puzzleId || gameProgress.puzzleAnalytics[puzzleId]?.completedAt != null) return
        if (activePuzzleId != null) recordPuzzleExit(PuzzleExitReason.backNavigation)
        val now = timeProvider.now(); activePuzzleId = puzzleId; activePuzzleStartedAt = now
        updatePuzzleAnalytics(puzzleId) { it.copy(firstStartedAt = it.firstStartedAt ?: now, sessionCount = it.sessionCount + 1) }; save()
    }
    fun completePuzzleSession(puzzleId: String) {
        if (!isAnalyticsConsentGranted) return
        val now = timeProvider.now(); val added = if (activePuzzleId == puzzleId) (now - (activePuzzleStartedAt ?: now)).coerceAtLeast(0) else 0
        updatePuzzleAnalytics(puzzleId) { it.copy(completedAt = it.completedAt ?: now, elapsedMs = it.elapsedMs + added) }
        if (activePuzzleId == puzzleId) { activePuzzleId = null; activePuzzleStartedAt = null }
        enqueueAnalyticsUpload(isFinal = false, delayMs = 2_000)
    }
    fun recordWrongAttempt(puzzleId: String, reason: String) { if (!isAnalyticsConsentGranted) return; updatePuzzleAnalytics(puzzleId) { analytics -> analytics.copy(wrongAttemptCount = analytics.wrongAttemptCount + 1, wrongReasonCounts = analytics.wrongReasonCounts + (reason to ((analytics.wrongReasonCounts[reason] ?: 0) + 1))) }; save() }
    fun recordPuzzleExit(reason: PuzzleExitReason) {
        val puzzleId = activePuzzleId ?: return; val now = timeProvider.now(); val added = (now - (activePuzzleStartedAt ?: now)).coerceAtLeast(0)
        updatePuzzleAnalytics(puzzleId) { analytics -> val elapsed = analytics.elapsedMs + added; analytics.copy(elapsedMs = elapsed, exitEvents = analytics.exitEvents + PuzzleExitEvent(now, reason, elapsed)) }
        activePuzzleId = null; activePuzzleStartedAt = null; save()
    }
    fun submitPlayerFeedback(difficultyRating: Int, comment: String): Boolean { if (!isAnalyticsConsentGranted || difficultyRating !in 1..5) return false; gameProgress = gameProgress.copy(playerFeedback = PlayerFeedback(difficultyRating, comment.trim().take(1000), timeProvider.now())); enqueueAnalyticsUpload(isFinal = true, delayMs = 0); save(); return true }
    fun exportPlaytestReport(): String = Json { prettyPrint = true }.encodeToString(PlaytestReport(gameProgress.startedAt, gameProgress.completedAt, gameProgress.endingType?.name, gameProgress.puzzleAnalytics, gameProgress.playerFeedback))
    fun showHint(text: String) { _uiState.update { it.copy(hintText = text) } }
    fun clearHint() { _uiState.update { it.copy(hintText = null) } }
    fun updateFlashlightPosition(x: Float, y: Float, deltaTime: Double = 0.05) {
        val safeX = FlashlightPuzzleEngine.clamp(x); val safeY = FlashlightPuzzleEngine.clamp(y)
        val event = flashlightEngine.updateFlashlightPosition(safeX, safeY, deltaTime)
        _uiState.update { it.copy(flashlightX = safeX, flashlightY = safeY) }
        when (event) {
            is FlashlightPuzzleEvent.DigitDiscovered -> { gameProgress = gameProgress.copy(discoveredDigits = flashlightEngine.discoveredDigits); hapticProvider.play(HapticEvent.digitDiscovered); save() }
            FlashlightPuzzleEvent.Completed -> completeFlashlightPuzzle()
            null -> Unit
        }
    }
    fun tickFlashlightPuzzle(deltaTime: Double = 0.05) {
        val state = _uiState.value
        if (state.currentScreen == Screen.flashlightPuzzle && !gameProgress.flashlightSolved) updateFlashlightPosition(state.flashlightX, state.flashlightY, deltaTime)
    }
    fun calibrate() { motionController.calibrate(); pitchFilter.reset(); rollFilter.reset(); _uiState.update { it.copy(flashlightX = .5f, flashlightY = .5f) } }
    fun start() { if (_uiState.value.currentScreen == Screen.flashlightPuzzle && gameProgress.controlMode == FlashlightControlMode.motion) motionController.start() }
    fun stop() { motionController.stop(); save() }
    fun setControlMode(controlMode: FlashlightControlMode) { val safeMode = if (controlMode == FlashlightControlMode.motion && !isAvailable) FlashlightControlMode.touch else controlMode; gameProgress = gameProgress.copy(controlMode = safeMode); if (safeMode == FlashlightControlMode.touch) motionController.stop() else start(); save() }
    fun navigate(screen: Screen) { if (canOpen(screen)) { _uiState.update { it.copy(currentScreen = screen, notice = null) }; if (screen == Screen.flashlightPuzzle) start() else motionController.stop() } else _uiState.update { it.copy(notice = "아직 잠겨 있습니다. 앞선 기록부터 복구하세요.") } }
    fun handleBackNavigation() { recordPuzzleExit(PuzzleExitReason.backNavigation); navigate(if (_uiState.value.currentScreen == Screen.intro || _uiState.value.currentScreen == Screen.phoneHome) Screen.mainMenu else Screen.phoneHome) }
    fun handleAppBackground() { recordPuzzleExit(PuzzleExitReason.appBackgroundOrClosed); if (isAnalyticsConsentGranted) enqueueAnalyticsUpload(isFinal = false, delayMs = 0); motionController.stop(); save() }
    val isAnalyticsConsentGranted get() = gameProgress.analyticsConsentStatus == AnalyticsConsentStatus.granted
    fun grantAnalyticsConsent() { gameProgress = gameProgress.copy(analyticsConsentStatus = AnalyticsConsentStatus.granted, analyticsConsentVersion = 1, anonymousSessionId = gameProgress.anonymousSessionId ?: java.util.UUID.randomUUID().toString()); save(); startPuzzleSession(_uiState.value.currentScreen.puzzleId); enqueueAnalyticsUpload(isFinal = false, delayMs = 1_500) }
    fun denyAnalyticsConsent() { activePuzzleId = null; activePuzzleStartedAt = null; gameProgress = gameProgress.copy(analyticsConsentStatus = AnalyticsConsentStatus.denied, analyticsConsentVersion = 1, anonymousSessionId = null, puzzleAnalytics = emptyMap(), playerFeedback = null, playtestHistory = emptyList(), pendingAnalyticsUploads = emptyList(), analyticsUploadSequence = 0, lastAnalyticsUploadAt = null, lastAnalyticsUploadError = null); uploadJob?.cancel(); save() }
    fun retryPendingAnalyticsUploads() = flushAnalyticsUploads(1_500)
    fun retryAnalyticsUploadNow() { if (gameProgress.pendingAnalyticsUploads.isEmpty()) enqueueAnalyticsUpload(isFinal = gameProgress.playerFeedback != null, delayMs = 0) else flushAnalyticsUploads(0) }
    fun dismissNotice() { _uiState.update { it.copy(notice = null) } }
    fun hasSavedGame() = gameProgress.startedAt != null || gameProgress.currentStage != GameStage.notStarted
    fun resetEngines() { messengerEngine = MessengerPuzzleEngine(); flashlightEngine = FlashlightPuzzleEngine(); serverEngine = ServerCodeEngine() }
    fun debugSetAttitude(pitch: Double, roll: Double, yaw: Double) = handleAttitude(DeviceAttitude(pitch, roll, yaw))
    private fun handleAttitude(attitude: DeviceAttitude) {
        if (_uiState.value.currentScreen != Screen.flashlightPuzzle || gameProgress.controlMode != FlashlightControlMode.motion) return
        val pitch = pitchFilter.update(attitude.pitch); val roll = rollFilter.update(attitude.roll)
        _uiState.update { it.copy(pitch = attitude.pitch, roll = attitude.roll, yaw = attitude.yaw) }
        updateFlashlightPosition(.5f + (roll / .8).toFloat(), .5f + (pitch / .8).toFloat(), 0.0)
    }
    private fun publishServerCode() { _uiState.update { it.copy(serverCodeInput = serverEngine.serverCodeInput) } }
    private fun updatePuzzleAnalytics(puzzleId: String, transform: (PuzzleAnalytics) -> PuzzleAnalytics) { val current = gameProgress.puzzleAnalytics[puzzleId] ?: PuzzleAnalytics(puzzleId); gameProgress = gameProgress.copy(puzzleAnalytics = gameProgress.puzzleAnalytics + (puzzleId to transform(current))) }
    private fun archivedHistory(): List<PlaytestReport> { val current = if (gameProgress.startedAt != null && gameProgress.puzzleAnalytics.isNotEmpty()) PlaytestReport(gameProgress.startedAt, gameProgress.completedAt, gameProgress.endingType?.name, gameProgress.puzzleAnalytics, gameProgress.playerFeedback) else null; return (gameProgress.playtestHistory + listOfNotNull(current)).takeLast(20) }
    private fun enqueueAnalyticsUpload(isFinal: Boolean, delayMs: Long) {
        if (!isAnalyticsConsentGranted) return
        val sessionId = gameProgress.anonymousSessionId ?: java.util.UUID.randomUUID().toString()
        val sequence = gameProgress.analyticsUploadSequence + 1
        val report = PlaytestReport(gameProgress.startedAt, gameProgress.completedAt, gameProgress.endingType?.name, gameProgress.puzzleAnalytics, gameProgress.playerFeedback)
        val envelope = PlaytestUploadEnvelope(sessionId = sessionId, sequence = sequence, platform = "android", appVersion = BuildConfig.VERSION_NAME, consentVersion = gameProgress.analyticsConsentVersion, isFinal = isFinal, createdAt = timeProvider.now(), report = report)
        val pending = PendingAnalyticsUpload("$sessionId-$sequence", envelope)
        gameProgress = gameProgress.copy(anonymousSessionId = sessionId, analyticsUploadSequence = sequence, pendingAnalyticsUploads = (gameProgress.pendingAnalyticsUploads + pending).takeLast(20)); save(); flushAnalyticsUploads(delayMs)
    }
    private fun flushAnalyticsUploads(delayMs: Long) {
        if (!isAnalyticsConsentGranted || !analyticsUploader.isConfigured || uploadJob?.isActive == true) return
        uploadJob = viewModelScope.launch {
            if (delayMs > 0) delay(delayMs)
            while (isAnalyticsConsentGranted && gameProgress.pendingAnalyticsUploads.isNotEmpty()) {
                val pending = gameProgress.pendingAnalyticsUploads.first()
                if (analyticsUploader.upload(analyticsJson.encodeToString(pending.envelope))) gameProgress = gameProgress.copy(pendingAnalyticsUploads = gameProgress.pendingAnalyticsUploads.drop(1), lastAnalyticsUploadAt = timeProvider.now(), lastAnalyticsUploadError = null)
                else { gameProgress = gameProgress.copy(pendingAnalyticsUploads = listOf(pending.copy(attemptCount = pending.attemptCount + 1)) + gameProgress.pendingAnalyticsUploads.drop(1), lastAnalyticsUploadError = "서버 연결 또는 응답 실패"); save(); break }
                save()
            }
        }
    }
    private fun routeForProgress() = when (gameProgress.currentStage) { GameStage.notStarted -> Screen.intro; GameStage.introCompleted, GameStage.messengerSolved -> Screen.phoneHome; GameStage.flashlightSolved -> Screen.encryptedNote; GameStage.encryptedNoteSolved -> Screen.audioRecordPuzzle; GameStage.audioRecordSolved -> Screen.commitGraphPuzzle; GameStage.commitGraphSolved -> Screen.accessLogPuzzle; GameStage.accessLogSolved -> Screen.serverConsole; GameStage.gameCompleted -> if (gameProgress.endingType == null) Screen.finalDecision else Screen.ending }
    private fun canOpen(screen: Screen) = when (screen) { Screen.flashlightPuzzle -> gameProgress.messengerSolved; Screen.encryptedNote -> gameProgress.flashlightSolved; Screen.audioRecordPuzzle -> gameProgress.encryptedNoteSolved; Screen.commitGraphPuzzle -> gameProgress.audioRecordSolved; Screen.accessLogPuzzle -> gameProgress.commitGraphSolved; Screen.serverConsole -> gameProgress.accessLogSolved; Screen.finalDecision -> gameProgress.currentStage == GameStage.gameCompleted; Screen.ending -> gameProgress.currentStage == GameStage.gameCompleted && gameProgress.endingType != null; else -> true }
    override fun onCleared() { motionController.stop() }
}
