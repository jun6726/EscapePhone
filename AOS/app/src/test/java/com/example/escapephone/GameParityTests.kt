package com.example.escapephone

import com.example.escapephone.app.GameViewModel
import com.example.escapephone.app.Screen
import com.example.escapephone.core.game.FlashlightPuzzleEngine
import com.example.escapephone.core.game.MessengerPuzzleEngine
import com.example.escapephone.core.game.ServerCodeEngine
import com.example.escapephone.core.game.ServerCodeResult
import com.example.escapephone.core.game.AudioRecordPuzzleEngine
import com.example.escapephone.core.game.CommitGraphPuzzleEngine
import com.example.escapephone.core.game.AccessLogPuzzleEngine
import com.example.escapephone.core.model.GameProgress
import com.example.escapephone.core.model.GameStage
import com.example.escapephone.core.model.EndingType
import com.example.escapephone.core.motion.MockMotionController
import com.example.escapephone.core.motion.relativeRotationMatrix
import com.example.escapephone.core.persistence.InMemoryGameProgressStore
import com.example.escapephone.core.persistence.TimeProvider
import com.example.escapephone.core.services.NoOpAdGateway
import com.example.escapephone.core.services.NoOpHapticProvider
import com.example.escapephone.core.services.NoOpPuzzleDeviceConnector
import org.junit.Assert.*
import org.junit.Test
import kotlinx.serialization.json.Json

class GameParityTests {
    private fun makeViewModel(store: InMemoryGameProgressStore = InMemoryGameProgressStore()) = GameViewModel(store, MockMotionController(), NoOpHapticProvider, NoOpAdGateway, NoOpPuzzleDeviceConnector(), TimeProvider { 1_700_000_000_000 })
    @Test fun startNewGame_returnsInitialProgress() { val viewModel = makeViewModel(); viewModel.startNewGame(); assertEquals(GameStage.notStarted, viewModel.uiState.value.currentStage); assertEquals(1_700_000_000_000, viewModel.uiState.value.gameProgress.startedAt) }
    @Test fun completeMessengerPuzzle_unlocksPhotoApp() { val viewModel = makeViewModel(); viewModel.completeMessengerPuzzle(); assertTrue(viewModel.uiState.value.gameProgress.messengerSolved); viewModel.navigate(Screen.flashlightPuzzle); assertEquals(Screen.flashlightPuzzle, viewModel.uiState.value.currentScreen) }
    @Test fun completeFlashlightPuzzle_unlocksEncryptedNote() { val viewModel = makeViewModel(); viewModel.completeFlashlightPuzzle(); viewModel.navigate(Screen.encryptedNote); assertEquals(Screen.encryptedNote, viewModel.uiState.value.currentScreen) }
    @Test fun submitServerCode_with417121_completesGame() { val viewModel = makeViewModel(); viewModel.completeAccessLogPuzzle(); listOf(4, 1, 7, 1, 2, 1).forEach(viewModel::appendServerCodeDigit); assertEquals(ServerCodeResult.success, viewModel.submitServerCode()); assertEquals(GameStage.gameCompleted, viewModel.uiState.value.currentStage); assertEquals(Screen.finalDecision, viewModel.uiState.value.currentScreen) }
    @Test fun saveAndLoad_restoresGameProgress() { val store = InMemoryGameProgressStore(); val first = makeViewModel(store); first.startNewGame(); first.completeIntro(); assertEquals(GameStage.introCompleted, makeViewModel(store).uiState.value.currentStage) }
    @Test fun reset_clearsGameProgress() { val store = InMemoryGameProgressStore(GameProgress(startedAt = 1)); val viewModel = makeViewModel(store); viewModel.reset(); assertNull(store.gameProgress) }
}

class MessengerParityTests {
    @Test fun submitMessengerOrder_withCorrectOrder_succeeds() { assertTrue(MessengerPuzzleEngine(MessengerPuzzleEngine.messages).submitMessengerOrder()) }
    @Test fun submitMessengerOrder_withWrongOrder_fails() { assertFalse(MessengerPuzzleEngine(MessengerPuzzleEngine.messages.reversed()).submitMessengerOrder()) }
    @Test fun moveMessageUp_atFirstPosition_doesNothing() { val engine = MessengerPuzzleEngine(MessengerPuzzleEngine.messages); engine.moveMessageUp(0); assertEquals(MessengerPuzzleEngine.messages, engine.messages) }
    @Test fun moveMessageDown_atLastPosition_doesNothing() { val engine = MessengerPuzzleEngine(MessengerPuzzleEngine.messages); engine.moveMessageDown(3); assertEquals(MessengerPuzzleEngine.messages, engine.messages) }
}

class FlashlightParityTests {
    private val first = FlashlightPuzzleEngine.defaultTargets.first()
    @Test fun updateFlashlightPosition_outsideTarget_doesNotDiscoverDigit() { val engine = FlashlightPuzzleEngine(); engine.updateFlashlightPosition(1f, 1f, 1.0); assertTrue(engine.discoveredDigits.isEmpty()) }
    @Test fun updateFlashlightPosition_insideTarget_beforeDuration_doesNotDiscoverDigit() { val engine = FlashlightPuzzleEngine(); engine.updateFlashlightPosition(first.x, first.y, .1); assertTrue(engine.discoveredDigits.isEmpty()) }
    @Test fun updateFlashlightPosition_insideTarget_afterDuration_discoversDigit() { val engine = FlashlightPuzzleEngine(); repeat(10) { engine.updateFlashlightPosition(first.x, first.y, .1) }; assertEquals(listOf(4), engine.discoveredDigits) }
    @Test fun updateFlashlightPosition_withinExpandedRecognitionRange_discoversDigit() { val engine = FlashlightPuzzleEngine(); repeat(10) { engine.updateFlashlightPosition(first.x + .17f, first.y, .1) }; assertEquals(listOf(4), engine.discoveredDigits) }
    @Test fun updateFlashlightPosition_afterLeavingTarget_resetsProgress() { val engine = FlashlightPuzzleEngine(); repeat(7) { engine.updateFlashlightPosition(first.x, first.y, .1) }; engine.updateFlashlightPosition(0f, 0f, .1); engine.updateFlashlightPosition(first.x, first.y, .1); assertTrue(engine.discoveredDigits.isEmpty()) }
    @Test fun completeFlashlightPuzzle_discovers417() { val engine = FlashlightPuzzleEngine(); FlashlightPuzzleEngine.defaultTargets.forEach { target -> repeat(10) { engine.updateFlashlightPosition(target.x, target.y, .1) } }; assertEquals(listOf(4, 1, 7), engine.discoveredDigits); assertTrue(engine.isSolved) }

    @Test fun tickFlashlightPuzzle_usesLatestTouchPosition_withoutCenterOverride() {
        val viewModel = GameViewModel(InMemoryGameProgressStore(), MockMotionController(), NoOpHapticProvider, NoOpAdGateway, NoOpPuzzleDeviceConnector(), TimeProvider { 1_700_000_000_000 })
        viewModel.completeMessengerPuzzle(); viewModel.navigate(Screen.flashlightPuzzle); viewModel.setControlMode(com.example.escapephone.core.model.FlashlightControlMode.touch)
        viewModel.updateFlashlightPosition(first.x, first.y, 0.0)
        repeat(10) { viewModel.tickFlashlightPuzzle(.1) }
        assertEquals(first.x, viewModel.uiState.value.flashlightX); assertEquals(first.y, viewModel.uiState.value.flashlightY)
        assertEquals(listOf(4), viewModel.uiState.value.gameProgress.discoveredDigits)
    }

    @Test fun touchMode_ignoresLateMotionSensorEvent() {
        val viewModel = GameViewModel(InMemoryGameProgressStore(), MockMotionController(), NoOpHapticProvider, NoOpAdGateway, NoOpPuzzleDeviceConnector(), TimeProvider { 1_700_000_000_000 })
        viewModel.completeMessengerPuzzle(); viewModel.navigate(Screen.flashlightPuzzle); viewModel.setControlMode(com.example.escapephone.core.model.FlashlightControlMode.touch)
        viewModel.updateFlashlightPosition(.2f, .25f, 0.0); viewModel.debugSetAttitude(.6, .6, 0.0)
        assertEquals(.2f, viewModel.uiState.value.flashlightX); assertEquals(.25f, viewModel.uiState.value.flashlightY)
    }

    @Test fun relativeRotationMatrix_sameTiltedPose_returnsCenteredIdentity() {
        val tiltedPose = floatArrayOf(1f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f)
        val relative = relativeRotationMatrix(tiltedPose, tiltedPose)
        assertArrayEquals(floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f), relative, .0001f)
    }
}

class ServerParityTests {
    @Test fun submitServerCode_with417121_succeeds() { val engine = ServerCodeEngine(); listOf(4, 1, 7, 1, 2, 1).forEach(engine::appendServerCodeDigit); assertEquals(ServerCodeResult.success, engine.submitServerCode()) }
    @Test fun submitServerCode_withWrongCode_fails() { val engine = ServerCodeEngine(); listOf(1, 2, 3, 4, 5, 6).forEach(engine::appendServerCodeDigit); assertEquals(ServerCodeResult.incorrect, engine.submitServerCode()) }
    @Test fun appendServerCodeDigit_overLimit_isIgnored() { val engine = ServerCodeEngine(); listOf(4, 1, 7, 1, 2, 1, 9).forEach(engine::appendServerCodeDigit); assertEquals("417121", engine.serverCodeInput) }
    @Test fun clearServerCode_removesAllDigits() { val engine = ServerCodeEngine(); engine.appendServerCodeDigit(4); engine.clearServerCode(); assertEquals("", engine.serverCodeInput) }
}

class ExpandedPuzzleParityTests {
    private fun makeViewModel(store: InMemoryGameProgressStore = InMemoryGameProgressStore()) = GameViewModel(store, MockMotionController(), NoOpHapticProvider, NoOpAdGateway, NoOpPuzzleDeviceConnector(), TimeProvider { 1_700_000_000_000 })
    @Test fun completeEncryptedNotePuzzle_unlocksAudioRecord() { val viewModel = makeViewModel(); viewModel.completeEncryptedNotePuzzle(); viewModel.navigate(Screen.audioRecordPuzzle); assertTrue(viewModel.uiState.value.gameProgress.encryptedNoteSolved); assertEquals(Screen.audioRecordPuzzle, viewModel.uiState.value.currentScreen) }
    @Test fun submitAudioOrder_withCorrectOrder_succeeds() { assertTrue(AudioRecordPuzzleEngine(AudioRecordPuzzleEngine.fragments).submitAudioOrder()) }
    @Test fun submitAudioOrder_withWrongOrder_fails() { assertFalse(AudioRecordPuzzleEngine(AudioRecordPuzzleEngine.fragments.reversed()).submitAudioOrder()) }
    @Test fun validateCommitGraph_withCorrectGraph_succeeds() { val engine = CommitGraphPuzzleEngine(); engine.nodes.sortedBy { it.correctOrder }.forEachIndexed { index, node -> engine.moveCommitNode(node.id, node.correctBranch); engine.moveCommitNode(node.id, index) }; engine.connectCommitNode("417", "418"); engine.connectCommitNode("418", "420"); assertTrue(engine.validateCommitGraph()) }
    @Test fun validateCommitGraph_withWrongBranch_fails() { val engine = CommitGraphPuzzleEngine(); engine.nodes.sortedBy { it.correctOrder }.forEachIndexed { index, node -> engine.moveCommitNode(node.id, if (node.id == "417") "hotfix" else node.correctBranch); engine.moveCommitNode(node.id, index) }; engine.connectCommitNode("417", "418"); engine.connectCommitNode("418", "420"); assertFalse(engine.validateCommitGraph()) }
    @Test fun validateAccessLogAnswers_withCorrectAnswers_succeeds() { val engine = AccessLogPuzzleEngine(); engine.correctAnswers.forEachIndexed(engine::answerLogQuestion); assertTrue(engine.validateAccessLogAnswers()) }
    @Test fun selectPublicDisclosure_savesEndingType() { val store = InMemoryGameProgressStore(); val viewModel = makeViewModel(store); viewModel.selectPublicDisclosure(); assertEquals(EndingType.publicDisclosure, store.gameProgress?.endingType) }
    @Test fun selectEncryptedArchive_savesEndingType() { val store = InMemoryGameProgressStore(); val viewModel = makeViewModel(store); viewModel.selectEncryptedArchive(); assertEquals(EndingType.encryptedArchive, store.gameProgress?.endingType) }
    @Test fun loadLegacyProgress_appliesNewDefaultValues() { val legacy = """{"currentStage":"flashlightSolved","messengerSolved":true,"flashlightSolved":true,"discoveredDigits":[4,1,7]}"""; val progress = Json { ignoreUnknownKeys = true }.decodeFromString<GameProgress>(legacy); assertFalse(progress.encryptedNoteSolved); assertFalse(progress.audioRecordSolved); assertFalse(progress.commitGraphSolved); assertFalse(progress.accessLogSolved); assertNull(progress.endingType); assertTrue(progress.collectedEvidenceIds.isEmpty()); assertTrue(progress.puzzleAnalytics.isEmpty()); assertNull(progress.playerFeedback); assertTrue(progress.playtestHistory.isEmpty()); assertEquals(com.example.escapephone.core.model.AnalyticsConsentStatus.notDetermined, progress.analyticsConsentStatus); assertTrue(progress.pendingAnalyticsUploads.isEmpty()); assertNull(progress.lastAnalyticsUploadAt); assertNull(progress.lastAnalyticsUploadError) }
}
