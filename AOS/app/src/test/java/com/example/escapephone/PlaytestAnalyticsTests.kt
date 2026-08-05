package com.example.escapephone

import com.example.escapephone.app.GameViewModel
import com.example.escapephone.core.model.PuzzleExitReason
import com.example.escapephone.core.motion.MockMotionController
import com.example.escapephone.core.persistence.InMemoryGameProgressStore
import com.example.escapephone.core.persistence.TimeProvider
import com.example.escapephone.core.services.NoOpAdGateway
import com.example.escapephone.core.services.NoOpHapticProvider
import com.example.escapephone.core.services.NoOpPuzzleDeviceConnector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaytestAnalyticsTests {
    private class MutableTimeProvider(var current: Long) : TimeProvider { override fun now() = current }
    private fun makeViewModel(store: InMemoryGameProgressStore, time: MutableTimeProvider, consent: Boolean = true) = GameViewModel(store, MockMotionController(), NoOpHapticProvider, NoOpAdGateway, NoOpPuzzleDeviceConnector(), time).also { if (consent) it.grantAnalyticsConsent() }

    @Test fun completePuzzleSession_savesElapsedTime() {
        val store = InMemoryGameProgressStore(); val time = MutableTimeProvider(1_000); val viewModel = makeViewModel(store, time)
        viewModel.startPuzzleSession("encrypted_note"); time.current = 4_500; viewModel.completePuzzleSession("encrypted_note"); viewModel.save()
        assertEquals(3_500L, store.gameProgress?.puzzleAnalytics?.get("encrypted_note")?.elapsedMs)
    }

    @Test fun recordWrongAttempt_savesReason() {
        val store = InMemoryGameProgressStore(); val viewModel = makeViewModel(store, MutableTimeProvider(1_000))
        viewModel.recordWrongAttempt("audio_record", "audioOrderIncorrect")
        val analytics = store.gameProgress?.puzzleAnalytics?.get("audio_record"); assertEquals(1, analytics?.wrongAttemptCount); assertEquals(1, analytics?.wrongReasonCounts?.get("audioOrderIncorrect"))
    }

    @Test fun requestHint_incrementsPuzzleHintViews() {
        val store = InMemoryGameProgressStore(); val viewModel = makeViewModel(store, MutableTimeProvider(1_000))
        viewModel.requestHint("commit_graph.1", "hint"); viewModel.requestHint("commit_graph.1", "hint")
        assertEquals(2, store.gameProgress?.puzzleAnalytics?.get("commit_graph")?.hintViewCount); assertEquals(1, store.gameProgress?.hintCount)
    }

    @Test fun recordPuzzleExit_savesBackNavigation() {
        val store = InMemoryGameProgressStore(); val time = MutableTimeProvider(1_000); val viewModel = makeViewModel(store, time)
        viewModel.startPuzzleSession("access_log"); time.current = 2_250; viewModel.recordPuzzleExit(PuzzleExitReason.backNavigation)
        val event = store.gameProgress?.puzzleAnalytics?.get("access_log")?.exitEvents?.single(); assertEquals(PuzzleExitReason.backNavigation, event?.reason); assertEquals(1_250L, event?.elapsedMsAtExit)
    }

    @Test fun submitPlayerFeedback_savesDifficultyAndComment() {
        val store = InMemoryGameProgressStore(); val viewModel = makeViewModel(store, MutableTimeProvider(1_000))
        assertTrue(viewModel.submitPlayerFeedback(4, "커밋 그래프가 어려웠어요")); assertEquals(4, store.gameProgress?.playerFeedback?.difficultyRating); assertNotNull(store.gameProgress?.playerFeedback)
    }

    @Test fun denyAnalyticsConsent_doesNotRecordPuzzleAnalytics() {
        val store = InMemoryGameProgressStore(); val viewModel = makeViewModel(store, MutableTimeProvider(1_000), consent = false)
        viewModel.denyAnalyticsConsent(); viewModel.startPuzzleSession("encrypted_note"); viewModel.recordWrongAttempt("encrypted_note", "noteWordOrderIncorrect")
        assertTrue(store.gameProgress?.puzzleAnalytics?.isEmpty() == true)
    }

    @Test fun submitPlayerFeedback_enqueuesFinalJson() {
        val store = InMemoryGameProgressStore(); val viewModel = makeViewModel(store, MutableTimeProvider(1_000))
        viewModel.submitPlayerFeedback(3, "좋아요")
        assertTrue(store.gameProgress?.pendingAnalyticsUploads?.last()?.envelope?.isFinal == true)
    }
}
