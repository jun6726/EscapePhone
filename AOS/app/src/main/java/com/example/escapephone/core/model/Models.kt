package com.example.escapephone.core.model

import kotlinx.serialization.Serializable

@Serializable enum class GameStage { notStarted, introCompleted, messengerSolved, flashlightSolved, encryptedNoteSolved, audioRecordSolved, commitGraphSolved, accessLogSolved, gameCompleted }
@Serializable enum class FlashlightControlMode { motion, touch }
@Serializable enum class EndingType { publicDisclosure, encryptedArchive }
@Serializable enum class PuzzleExitReason { backNavigation, appBackgroundOrClosed }
@Serializable enum class AnalyticsConsentStatus { notDetermined, granted, denied }

@Serializable
data class PuzzleExitEvent(
    val occurredAt: Long,
    val reason: PuzzleExitReason,
    val elapsedMsAtExit: Long
)

@Serializable
data class PuzzleAnalytics(
    val puzzleId: String,
    val firstStartedAt: Long? = null,
    val completedAt: Long? = null,
    val elapsedMs: Long = 0,
    val sessionCount: Int = 0,
    val wrongAttemptCount: Int = 0,
    val wrongReasonCounts: Map<String, Int> = emptyMap(),
    val hintViewCount: Int = 0,
    val exitEvents: List<PuzzleExitEvent> = emptyList()
)

@Serializable
data class PlayerFeedback(
    val difficultyRating: Int,
    val comment: String,
    val submittedAt: Long
)

@Serializable
data class PlaytestReport(
    val sessionStartedAt: Long?,
    val completedAt: Long?,
    val endingType: String?,
    val puzzleAnalytics: Map<String, PuzzleAnalytics>,
    val playerFeedback: PlayerFeedback?
)

@Serializable
data class PlaytestUploadEnvelope(
    val schemaVersion: Int = 1,
    val sessionId: String,
    val sequence: Int,
    val platform: String,
    val appVersion: String,
    val consentVersion: Int,
    val isFinal: Boolean,
    val createdAt: Long,
    val themeId: String = "the_last_commit",
    val report: PlaytestReport
)

@Serializable
data class PendingAnalyticsUpload(
    val id: String,
    val envelope: PlaytestUploadEnvelope,
    val attemptCount: Int = 0
)

@Serializable
data class GameProgress(
    val currentStage: GameStage = GameStage.notStarted,
    val messengerSolved: Boolean = false,
    val flashlightSolved: Boolean = false,
    val encryptedNoteSolved: Boolean = false,
    val audioRecordSolved: Boolean = false,
    val commitGraphSolved: Boolean = false,
    val accessLogSolved: Boolean = false,
    val endingType: EndingType? = null,
    val collectedEvidenceIds: Set<String> = emptySet(),
    val discoveredDigits: List<Int> = emptyList(),
    val hintCount: Int = 0,
    val startedAt: Long? = null,
    val lastSavedAt: Long? = null,
    val completedAt: Long? = null,
    val controlMode: FlashlightControlMode = FlashlightControlMode.motion,
    val seenHintIds: Set<String> = emptySet(),
    val puzzleAnalytics: Map<String, PuzzleAnalytics> = emptyMap(),
    val playerFeedback: PlayerFeedback? = null,
    val playtestHistory: List<PlaytestReport> = emptyList(),
    val analyticsConsentStatus: AnalyticsConsentStatus = AnalyticsConsentStatus.notDetermined,
    val analyticsConsentVersion: Int = 0,
    val anonymousSessionId: String? = null,
    val analyticsUploadSequence: Int = 0,
    val pendingAnalyticsUploads: List<PendingAnalyticsUpload> = emptyList(),
    val lastAnalyticsUploadAt: Long? = null,
    val lastAnalyticsUploadError: String? = null
)

@Serializable data class DeviceAttitude(val pitch: Double, val roll: Double, val yaw: Double)
@Serializable data class PuzzleMessage(val id: String, val sender: String, val body: String, val displayedTime: String, val correctOrder: Int)
@Serializable data class AudioFragment(val id: String, val subtitle: String, val correctOrder: Int)
@Serializable data class CommitNode(val id: String, val number: Int, val title: String, val correctBranch: String, val correctOrder: Int)
@Serializable data class FlashlightTarget(val id: String, val digit: Int, val x: Float, val y: Float, val requiredHoldDuration: Double)

@Serializable
enum class ConvenienceStoreStage(val stageId: String) {
    notStarted("not_started"),
    introCompleted("intro_completed"),
    receiptSolved("receipt_solved"),
    barcodeSolved("barcode_solved"),
    shelfDifferenceSolved("shelf_difference_solved"),
    cctvSolved("cctv_solved"),
    inventorySolved("inventory_solved"),
    customerPatternSolved("customer_pattern_solved"),
    timelineSolved("timeline_solved"),
    gameCompleted("game_completed")
}

@Serializable
enum class ConvenienceStoreEndingType { publicDisclosure, encryptedArchive }

@Serializable
enum class TiltControlMode { motion, touch }

@Serializable
data class ConvenienceStoreProgress(
    val currentStage: ConvenienceStoreStage = ConvenienceStoreStage.notStarted,
    val receiptSolved: Boolean = false,
    val barcodeSolved: Boolean = false,
    val shelfDifferenceSolved: Boolean = false,
    val cctvSolved: Boolean = false,
    val inventorySolved: Boolean = false,
    val customerPatternSolved: Boolean = false,
    val timelineSolved: Boolean = false,
    val discoveredCodes: List<String> = emptyList(),
    val selectedAnomalyIds: Set<String> = emptySet(),
    val collectedEvidenceIds: Set<String> = emptySet(),
    val hintLevels: Map<String, Int> = emptyMap(),
    val controlMode: TiltControlMode = TiltControlMode.motion,
    val endingType: ConvenienceStoreEndingType? = null,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val seenHintIds: Set<String> = emptySet(),
    val puzzleAnalytics: Map<String, PuzzleAnalytics> = emptyMap(),
    val playerFeedback: PlayerFeedback? = null,
    val playtestHistory: List<PlaytestReport> = emptyList(),
    val analyticsConsentStatus: AnalyticsConsentStatus = AnalyticsConsentStatus.notDetermined,
    val analyticsConsentVersion: Int = 0,
    val anonymousSessionId: String? = null,
    val analyticsUploadSequence: Int = 0,
    val pendingAnalyticsUploads: List<PendingAnalyticsUpload> = emptyList(),
    val lastAnalyticsUploadAt: Long? = null,
    val lastAnalyticsUploadError: String? = null
)

object ConvenienceStoreIds {
    const val convenience_store_home = "convenience_store_home"
    const val receipt_price = "receipt_price"
    const val barcode_rule = "barcode_rule"
    const val shelf_difference = "shelf_difference"
    const val cctv_sequence = "cctv_sequence"
    const val inventory_crosscheck = "inventory_crosscheck"
    const val customer_pattern = "customer_pattern"
    const val incident_timeline = "incident_timeline"
    const val convenience_store_final_decision = "convenience_store_final_decision"
}

object GameIds {
    const val messenger_order = "messenger_order"
    const val flashlight_search = "flashlight_search"
    const val encrypted_note = "encrypted_note"
    const val audio_record = "audio_record"
    const val commit_graph = "commit_graph"
    const val access_log = "access_log"
    const val server_code = "server_code"
    const val main_menu = "main_menu"
    const val intro = "intro"
    const val phone_home = "phone_home"
    const val messenger_puzzle = "messenger_puzzle"
    const val flashlight_puzzle = "flashlight_puzzle"
    const val encrypted_note_screen = "encrypted_note_screen"
    const val audio_record_puzzle = "audio_record_puzzle"
    const val commit_graph_puzzle = "commit_graph_puzzle"
    const val access_log_puzzle = "access_log_puzzle"
    const val server_console = "server_console"
    const val final_decision = "final_decision"
    const val ending = "ending"
    const val settings = "settings"
    const val device_analytics = "device_analytics"
    const val developer_menu = "developer_menu"
}
