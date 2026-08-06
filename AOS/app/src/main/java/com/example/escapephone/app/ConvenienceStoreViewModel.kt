package com.example.escapephone.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.escapephone.BuildConfig
import com.example.escapephone.core.game.BarcodePuzzleEngine
import com.example.escapephone.core.game.CctvPuzzleEngine
import com.example.escapephone.core.game.CustomerPatternPuzzleEngine
import com.example.escapephone.core.game.IncidentTimelinePuzzleEngine
import com.example.escapephone.core.game.InventoryPuzzleEngine
import com.example.escapephone.core.game.ReceiptPuzzleEngine
import com.example.escapephone.core.game.ShelfDifferencePuzzleEngine
import com.example.escapephone.core.game.TiltObjectPuzzleEngine
import com.example.escapephone.core.model.AnalyticsConsentStatus
import com.example.escapephone.core.model.ConvenienceStoreEndingType
import com.example.escapephone.core.model.ConvenienceStoreIds
import com.example.escapephone.core.model.ConvenienceStoreProgress
import com.example.escapephone.core.model.ConvenienceStoreStage
import com.example.escapephone.core.model.PendingAnalyticsUpload
import com.example.escapephone.core.model.PlayerFeedback
import com.example.escapephone.core.model.PlaytestReport
import com.example.escapephone.core.model.PlaytestUploadEnvelope
import com.example.escapephone.core.model.PuzzleAnalytics
import com.example.escapephone.core.model.PuzzleExitEvent
import com.example.escapephone.core.model.PuzzleExitReason
import com.example.escapephone.core.model.TiltControlMode
import com.example.escapephone.core.persistence.SystemTimeProvider
import com.example.escapephone.core.persistence.TimeProvider
import com.example.escapephone.core.services.NoOpPlaytestAnalyticsUploader
import com.example.escapephone.core.services.PlaytestAnalyticsUploader
import com.example.escapephone.core.theme.ThemeId
import com.example.escapephone.core.theme.ThemeProgress
import com.example.escapephone.core.theme.ThemeProgressStore
import com.example.escapephone.core.theme.ThemeSpecificState
import com.example.escapephone.core.theme.ThemeStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class ConvenienceStoreScreen(val screenId: String) {
    intro("convenience_store_intro"),
    home(ConvenienceStoreIds.convenience_store_home),
    receipt(ConvenienceStoreIds.receipt_price),
    barcode(ConvenienceStoreIds.barcode_rule),
    shelfDifference(ConvenienceStoreIds.shelf_difference),
    cctv(ConvenienceStoreIds.cctv_sequence),
    inventory(ConvenienceStoreIds.inventory_crosscheck),
    customerPattern(ConvenienceStoreIds.customer_pattern),
    incidentTimeline(ConvenienceStoreIds.incident_timeline),
    finalDecision(ConvenienceStoreIds.convenience_store_final_decision),
    ending("convenience_store_ending")
}

val ConvenienceStoreScreen.puzzleId: String? get() = when (this) {
    ConvenienceStoreScreen.receipt -> ConvenienceStoreIds.receipt_price
    ConvenienceStoreScreen.barcode -> ConvenienceStoreIds.barcode_rule
    ConvenienceStoreScreen.shelfDifference -> ConvenienceStoreIds.shelf_difference
    ConvenienceStoreScreen.cctv -> ConvenienceStoreIds.cctv_sequence
    ConvenienceStoreScreen.inventory -> ConvenienceStoreIds.inventory_crosscheck
    ConvenienceStoreScreen.customerPattern -> ConvenienceStoreIds.customer_pattern
    ConvenienceStoreScreen.incidentTimeline -> ConvenienceStoreIds.incident_timeline
    else -> null
}

data class ConvenienceStoreUiState(
    val progress: ConvenienceStoreProgress = ConvenienceStoreProgress(),
    val currentScreen: ConvenienceStoreScreen = ConvenienceStoreScreen.intro,
    val hintText: String? = null,
    val notice: String? = null
)

class ConvenienceStoreViewModel(
    private val themeProgressStore: ThemeProgressStore,
    private val timeProvider: TimeProvider = SystemTimeProvider,
    private val analyticsUploader: PlaytestAnalyticsUploader = NoOpPlaytestAnalyticsUploader
) : ViewModel() {
    private var progress: ConvenienceStoreProgress = loadStoredProgress()
    val receiptEngine = ReceiptPuzzleEngine()
    val barcodeEngine = BarcodePuzzleEngine()
    val shelfDifferenceEngine = ShelfDifferencePuzzleEngine()
    val tiltEngine = TiltObjectPuzzleEngine()
    val cctvEngine = CctvPuzzleEngine()
    val inventoryEngine = InventoryPuzzleEngine()
    val customerPatternEngine = CustomerPatternPuzzleEngine()
    val incidentTimelineEngine = IncidentTimelinePuzzleEngine()

    private var activePuzzleId: String? = null
    private var activePuzzleStartedAt: Long? = null
    private var uploadJob: Job? = null
    private val analyticsJson = Json { prettyPrint = false; encodeDefaults = true }

    private val _uiState = MutableStateFlow(ConvenienceStoreUiState(progress = progress, currentScreen = routeForProgress(progress)))
    val uiState: StateFlow<ConvenienceStoreUiState> = _uiState.asStateFlow()

    private fun loadStoredProgress(): ConvenienceStoreProgress {
        val theme = themeProgressStore.loadThemeProgress(ThemeId.convenienceStoreLoop)
        return (theme.themeSpecificState as? ThemeSpecificState.ConvenienceStoreLoopState)?.progress ?: ConvenienceStoreProgress()
    }

    private fun routeForProgress(progress: ConvenienceStoreProgress): ConvenienceStoreScreen = when (progress.currentStage) {
        ConvenienceStoreStage.notStarted -> ConvenienceStoreScreen.intro
        ConvenienceStoreStage.introCompleted -> ConvenienceStoreScreen.home
        ConvenienceStoreStage.receiptSolved -> ConvenienceStoreScreen.home
        ConvenienceStoreStage.barcodeSolved -> ConvenienceStoreScreen.home
        ConvenienceStoreStage.shelfDifferenceSolved -> ConvenienceStoreScreen.home
        ConvenienceStoreStage.cctvSolved -> ConvenienceStoreScreen.home
        ConvenienceStoreStage.inventorySolved -> ConvenienceStoreScreen.home
        ConvenienceStoreStage.customerPatternSolved -> ConvenienceStoreScreen.home
        ConvenienceStoreStage.timelineSolved -> ConvenienceStoreScreen.finalDecision
        ConvenienceStoreStage.gameCompleted -> if (progress.endingType == null) ConvenienceStoreScreen.finalDecision else ConvenienceStoreScreen.ending
    }

    fun hasSavedGame() = progress.startedAt != null
    val isAnalyticsConsentGranted get() = progress.analyticsConsentStatus == AnalyticsConsentStatus.granted

    fun startNewGame() {
        activePuzzleId = null; activePuzzleStartedAt = null
        val history = if (isAnalyticsConsentGranted) archivedHistory() else emptyList()
        val sessionId = if (isAnalyticsConsentGranted) java.util.UUID.randomUUID().toString() else null
        progress = ConvenienceStoreProgress(
            controlMode = progress.controlMode,
            startedAt = timeProvider.now(),
            playtestHistory = history,
            analyticsConsentStatus = progress.analyticsConsentStatus,
            analyticsConsentVersion = progress.analyticsConsentVersion,
            anonymousSessionId = sessionId,
            pendingAnalyticsUploads = progress.pendingAnalyticsUploads
        )
        save()
        _uiState.value = ConvenienceStoreUiState(progress = progress, currentScreen = ConvenienceStoreScreen.intro)
        flushAnalyticsUploads(1_500)
    }

    fun continueGame() { navigate(routeForProgress(progress)) }

    fun completeIntro() {
        if (progress.currentStage == ConvenienceStoreStage.notStarted) progress = progress.copy(currentStage = ConvenienceStoreStage.introCompleted, startedAt = progress.startedAt ?: timeProvider.now())
        save(); navigate(ConvenienceStoreScreen.home)
    }

    fun completeReceiptPuzzle() { if (!progress.receiptSolved) { completePuzzleSession(ConvenienceStoreIds.receipt_price); progress = progress.copy(currentStage = ConvenienceStoreStage.receiptSolved, receiptSolved = true, discoveredCodes = progress.discoveredCodes + "0217"); save() } }
    fun completeBarcodePuzzle() { if (!progress.barcodeSolved) { completePuzzleSession(ConvenienceStoreIds.barcode_rule); progress = progress.copy(currentStage = ConvenienceStoreStage.barcodeSolved, barcodeSolved = true, discoveredCodes = progress.discoveredCodes + "냉장고 4번 하단"); save() } }
    fun completeShelfDifferencePuzzle() { if (!progress.shelfDifferenceSolved) { completePuzzleSession(ConvenienceStoreIds.shelf_difference); progress = progress.copy(currentStage = ConvenienceStoreStage.shelfDifferenceSolved, shelfDifferenceSolved = true, collectedEvidenceIds = progress.collectedEvidenceIds + "shelf_pouch_message"); save() } }
    fun completeCctvPuzzle() { if (!progress.cctvSolved) { completePuzzleSession(ConvenienceStoreIds.cctv_sequence); progress = progress.copy(currentStage = ConvenienceStoreStage.cctvSolved, cctvSolved = true, collectedEvidenceIds = progress.collectedEvidenceIds + "central7_trace"); save() } }
    fun completeInventoryPuzzle() { if (!progress.inventorySolved) { completePuzzleSession(ConvenienceStoreIds.inventory_crosscheck); progress = progress.copy(currentStage = ConvenienceStoreStage.inventorySolved, inventorySolved = true, collectedEvidenceIds = progress.collectedEvidenceIds + "evidence_box_location"); save() } }
    fun completeCustomerPatternPuzzle() { if (!progress.customerPatternSolved) { completePuzzleSession(ConvenienceStoreIds.customer_pattern); progress = progress.copy(currentStage = ConvenienceStoreStage.customerPatternSolved, customerPatternSolved = true, collectedEvidenceIds = progress.collectedEvidenceIds + "customer_pattern_message"); save() } }
    fun completeIncidentTimelinePuzzle() { if (!progress.timelineSolved) { completePuzzleSession(ConvenienceStoreIds.incident_timeline); progress = progress.copy(currentStage = ConvenienceStoreStage.timelineSolved, timelineSolved = true); save(); navigate(ConvenienceStoreScreen.finalDecision) } }

    fun selectPublicDisclosure() = selectFinalDecision(ConvenienceStoreEndingType.publicDisclosure)
    fun selectEncryptedArchive() = selectFinalDecision(ConvenienceStoreEndingType.encryptedArchive)
    private fun selectFinalDecision(endingType: ConvenienceStoreEndingType) {
        progress = progress.copy(currentStage = ConvenienceStoreStage.gameCompleted, endingType = endingType, completedAt = progress.completedAt ?: timeProvider.now())
        save(); navigate(ConvenienceStoreScreen.ending)
    }

    fun requestHint(hintId: String, text: String) {
        val puzzleId = hintId.substringBefore('.')
        if (isAnalyticsConsentGranted) updatePuzzleAnalytics(puzzleId) { it.copy(hintViewCount = it.hintViewCount + 1) }
        if (hintId !in progress.seenHintIds) progress = progress.copy(seenHintIds = progress.seenHintIds + hintId)
        save(); _uiState.update { it.copy(hintText = text) }
    }
    fun clearHint() { _uiState.update { it.copy(hintText = null) } }

    fun setControlMode(mode: TiltControlMode) { progress = progress.copy(controlMode = mode); save() }

    fun navigate(screen: ConvenienceStoreScreen) {
        startPuzzleSession(screen.puzzleId)
        _uiState.update { it.copy(currentScreen = screen, notice = null) }
    }
    fun dismissNotice() { _uiState.update { it.copy(notice = null) } }

    fun reset() {
        activePuzzleId = null; activePuzzleStartedAt = null
        val consent = progress.analyticsConsentStatus; val consentVersion = progress.analyticsConsentVersion
        progress = ConvenienceStoreProgress(controlMode = progress.controlMode, analyticsConsentStatus = consent, analyticsConsentVersion = consentVersion)
        save()
        _uiState.value = ConvenienceStoreUiState(progress = progress, currentScreen = ConvenienceStoreScreen.intro)
    }

    fun startPuzzleSession(puzzleId: String?) {
        if (!isAnalyticsConsentGranted || puzzleId == null || activePuzzleId == puzzleId || progress.puzzleAnalytics[puzzleId]?.completedAt != null) return
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

    fun recordWrongAttempt(puzzleId: String, reason: String) {
        if (!isAnalyticsConsentGranted) return
        updatePuzzleAnalytics(puzzleId) { analytics -> analytics.copy(wrongAttemptCount = analytics.wrongAttemptCount + 1, wrongReasonCounts = analytics.wrongReasonCounts + (reason to ((analytics.wrongReasonCounts[reason] ?: 0) + 1))) }
        save()
    }

    fun recordPuzzleExit(reason: PuzzleExitReason) {
        val puzzleId = activePuzzleId ?: return
        val now = timeProvider.now(); val added = (now - (activePuzzleStartedAt ?: now)).coerceAtLeast(0)
        updatePuzzleAnalytics(puzzleId) { analytics -> val elapsed = analytics.elapsedMs + added; analytics.copy(elapsedMs = elapsed, exitEvents = analytics.exitEvents + PuzzleExitEvent(now, reason, elapsed)) }
        activePuzzleId = null; activePuzzleStartedAt = null; save()
    }

    fun submitPlayerFeedback(difficultyRating: Int, comment: String): Boolean {
        if (!isAnalyticsConsentGranted || difficultyRating !in 1..5) return false
        progress = progress.copy(playerFeedback = PlayerFeedback(difficultyRating, comment.trim().take(1000), timeProvider.now()))
        enqueueAnalyticsUpload(isFinal = true, delayMs = 0); save(); return true
    }

    fun grantAnalyticsConsent() {
        progress = progress.copy(analyticsConsentStatus = AnalyticsConsentStatus.granted, analyticsConsentVersion = 1, anonymousSessionId = progress.anonymousSessionId ?: java.util.UUID.randomUUID().toString())
        save(); startPuzzleSession(_uiState.value.currentScreen.puzzleId); enqueueAnalyticsUpload(isFinal = false, delayMs = 1_500)
    }
    fun denyAnalyticsConsent() {
        activePuzzleId = null; activePuzzleStartedAt = null
        progress = progress.copy(analyticsConsentStatus = AnalyticsConsentStatus.denied, analyticsConsentVersion = 1, anonymousSessionId = null, puzzleAnalytics = emptyMap(), playerFeedback = null, playtestHistory = emptyList(), pendingAnalyticsUploads = emptyList(), analyticsUploadSequence = 0, lastAnalyticsUploadAt = null, lastAnalyticsUploadError = null)
        uploadJob?.cancel(); save()
    }
    fun retryPendingAnalyticsUploads() = flushAnalyticsUploads(1_500)
    fun retryAnalyticsUploadNow() { if (progress.pendingAnalyticsUploads.isEmpty()) enqueueAnalyticsUpload(isFinal = progress.playerFeedback != null, delayMs = 0) else flushAnalyticsUploads(0) }
    fun handleAppBackground() { recordPuzzleExit(PuzzleExitReason.appBackgroundOrClosed); if (isAnalyticsConsentGranted) enqueueAnalyticsUpload(isFinal = false, delayMs = 0); save() }

    fun save() {
        val status = when {
            progress.currentStage == ConvenienceStoreStage.gameCompleted -> ThemeStatus.completed
            progress.currentStage != ConvenienceStoreStage.notStarted -> ThemeStatus.inProgress
            else -> ThemeStatus.notStarted
        }
        themeProgressStore.saveThemeProgress(
            ThemeProgress(
                themeId = ThemeId.convenienceStoreLoop,
                themeStatus = status,
                currentStageId = progress.currentStage.stageId,
                startedAt = progress.startedAt,
                lastSavedAt = timeProvider.now(),
                completedAt = progress.completedAt,
                hintCount = progress.seenHintIds.size,
                collectedEvidenceIds = progress.collectedEvidenceIds,
                endingId = progress.endingType?.name,
                themeSpecificState = ThemeSpecificState.ConvenienceStoreLoopState(progress)
            )
        )
        _uiState.update { it.copy(progress = progress) }
    }

    private fun updatePuzzleAnalytics(puzzleId: String, transform: (PuzzleAnalytics) -> PuzzleAnalytics) {
        val current = progress.puzzleAnalytics[puzzleId] ?: PuzzleAnalytics(puzzleId)
        progress = progress.copy(puzzleAnalytics = progress.puzzleAnalytics + (puzzleId to transform(current)))
    }

    private fun archivedHistory(): List<PlaytestReport> {
        val current = if (progress.startedAt != null && progress.puzzleAnalytics.isNotEmpty()) PlaytestReport(progress.startedAt, progress.completedAt, progress.endingType?.name, progress.puzzleAnalytics, progress.playerFeedback) else null
        return (progress.playtestHistory + listOfNotNull(current)).takeLast(20)
    }

    private fun enqueueAnalyticsUpload(isFinal: Boolean, delayMs: Long) {
        if (!isAnalyticsConsentGranted) return
        val sessionId = progress.anonymousSessionId ?: java.util.UUID.randomUUID().toString()
        val sequence = progress.analyticsUploadSequence + 1
        val report = PlaytestReport(progress.startedAt, progress.completedAt, progress.endingType?.name, progress.puzzleAnalytics, progress.playerFeedback)
        val envelope = PlaytestUploadEnvelope(sessionId = sessionId, sequence = sequence, platform = "android", appVersion = BuildConfig.VERSION_NAME, consentVersion = progress.analyticsConsentVersion, isFinal = isFinal, createdAt = timeProvider.now(), themeId = ThemeId.convenienceStoreLoop.storageValue, report = report)
        val pending = PendingAnalyticsUpload("$sessionId-$sequence", envelope)
        progress = progress.copy(anonymousSessionId = sessionId, analyticsUploadSequence = sequence, pendingAnalyticsUploads = (progress.pendingAnalyticsUploads + pending).takeLast(20)); save(); flushAnalyticsUploads(delayMs)
    }

    private fun flushAnalyticsUploads(delayMs: Long) {
        if (!isAnalyticsConsentGranted || !analyticsUploader.isConfigured || uploadJob?.isActive == true) return
        uploadJob = viewModelScope.launch {
            if (delayMs > 0) delay(delayMs)
            while (isAnalyticsConsentGranted && progress.pendingAnalyticsUploads.isNotEmpty()) {
                val pending = progress.pendingAnalyticsUploads.first()
                if (analyticsUploader.upload(analyticsJson.encodeToString(pending.envelope))) progress = progress.copy(pendingAnalyticsUploads = progress.pendingAnalyticsUploads.drop(1), lastAnalyticsUploadAt = timeProvider.now(), lastAnalyticsUploadError = null)
                else { progress = progress.copy(pendingAnalyticsUploads = listOf(pending.copy(attemptCount = pending.attemptCount + 1)) + progress.pendingAnalyticsUploads.drop(1), lastAnalyticsUploadError = "서버 연결 또는 응답 실패"); save(); break }
                save()
            }
        }
    }
}
