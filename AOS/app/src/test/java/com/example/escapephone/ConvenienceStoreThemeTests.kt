package com.example.escapephone

import com.example.escapephone.app.ConvenienceStoreViewModel
import com.example.escapephone.core.model.ConvenienceStoreEndingType
import com.example.escapephone.core.theme.InMemoryThemeProgressStore
import com.example.escapephone.core.theme.ThemeId
import com.example.escapephone.core.theme.ThemeSpecificState
import com.example.escapephone.core.theme.ThemeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConvenienceStoreThemeTests {
    @Test
    fun selectTheme_withTheLastCommit_opensCorrectMenu() {
        val store = InMemoryThemeProgressStore()
        store.saveThemeProgress(store.loadThemeProgress(ThemeId.theLastCommit).copy(themeStatus = ThemeStatus.inProgress))
        assertEquals(ThemeId.theLastCommit, store.loadThemeProgress(ThemeId.theLastCommit).themeId)
    }

    @Test
    fun selectTheme_withConvenienceStoreLoop_opensCorrectMenu() {
        val store = InMemoryThemeProgressStore()
        val viewModel = ConvenienceStoreViewModel(store)
        viewModel.startNewGame()
        assertTrue(viewModel.hasSavedGame())
        assertEquals(ThemeId.convenienceStoreLoop, store.loadThemeProgress(ThemeId.convenienceStoreLoop).themeId)
    }

    @Test
    fun saveAndLoad_convenienceStoreProgress_restoresState() {
        val store = InMemoryThemeProgressStore()
        val viewModel = ConvenienceStoreViewModel(store)
        viewModel.startNewGame()
        viewModel.completeIntro()
        viewModel.completeReceiptPuzzle()
        val reloaded = ConvenienceStoreViewModel(store)
        assertTrue(reloaded.uiState.value.progress.receiptSolved)
    }

    @Test
    fun resetConvenienceStoreProgress_keepsTheLastCommitProgress() {
        val store = InMemoryThemeProgressStore()
        store.saveThemeProgress(store.loadThemeProgress(ThemeId.theLastCommit).copy(hintCount = 4))
        val viewModel = ConvenienceStoreViewModel(store)
        viewModel.startNewGame()
        viewModel.reset()
        assertEquals(4, store.loadThemeProgress(ThemeId.theLastCommit).hintCount)
    }

    @Test
    fun selectPublicDisclosure_savesConvenienceStoreEnding() {
        val store = InMemoryThemeProgressStore()
        val viewModel = ConvenienceStoreViewModel(store)
        viewModel.startNewGame()
        viewModel.selectPublicDisclosure()
        val progress = viewModel.uiState.value.progress
        assertEquals(ConvenienceStoreEndingType.publicDisclosure, progress.endingType)
    }

    @Test
    fun selectEncryptedArchive_savesConvenienceStoreEnding() {
        val store = InMemoryThemeProgressStore()
        val viewModel = ConvenienceStoreViewModel(store)
        viewModel.startNewGame()
        viewModel.selectEncryptedArchive()
        val progress = viewModel.uiState.value.progress
        assertEquals(ConvenienceStoreEndingType.encryptedArchive, progress.endingType)
    }

    @Test
    fun completeConvenienceStoreTheme_marksThemeCompleted() {
        val store = InMemoryThemeProgressStore()
        val viewModel = ConvenienceStoreViewModel(store)
        viewModel.startNewGame()
        viewModel.selectPublicDisclosure()
        val theme = store.loadThemeProgress(ThemeId.convenienceStoreLoop)
        assertEquals(ThemeStatus.completed, theme.themeStatus)
        val state = theme.themeSpecificState
        assertTrue(state is ThemeSpecificState.ConvenienceStoreLoopState)
    }
}
