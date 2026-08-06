package com.example.escapephone

import com.example.escapephone.core.model.EndingType
import com.example.escapephone.core.model.GameProgress
import com.example.escapephone.core.model.GameStage
import com.example.escapephone.core.persistence.TimeProvider
import com.example.escapephone.core.theme.InMemoryThemeProgressStore
import com.example.escapephone.core.theme.ThemeId
import com.example.escapephone.core.theme.ThemeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FixedTimeProvider(private val value: Long) : TimeProvider { override fun now() = value }

class ThemeProgressTests {
    @Test
    fun migrateLegacyProgress_createsTheLastCommitProgress() {
        val legacy = GameProgress(currentStage = GameStage.messengerSolved, startedAt = 100L, hintCount = 2)
        val store = InMemoryThemeProgressStore(timeProvider = FixedTimeProvider(500L))
        store.setLegacyGameProgress(legacy)
        store.migrateLegacyProgressIfNeeded()
        val migrated = store.loadThemeProgress(ThemeId.theLastCommit)
        assertEquals(ThemeId.theLastCommit, migrated.themeId)
        assertEquals(ThemeStatus.inProgress, migrated.themeStatus)
        assertEquals(2, migrated.hintCount)
        assertEquals(100L, migrated.startedAt)
    }

    @Test
    fun migrateLegacyProgress_preservesCompletedStage() {
        val legacy = GameProgress(currentStage = GameStage.gameCompleted, completedAt = 900L, endingType = EndingType.publicDisclosure, collectedEvidenceIds = setOf("root_m"))
        val store = InMemoryThemeProgressStore(timeProvider = FixedTimeProvider(1000L))
        store.setLegacyGameProgress(legacy)
        store.migrateLegacyProgressIfNeeded()
        val migrated = store.loadThemeProgress(ThemeId.theLastCommit)
        assertEquals(ThemeStatus.completed, migrated.themeStatus)
        assertEquals(900L, migrated.completedAt)
        assertEquals("publicDisclosure", migrated.endingId)
        assertTrue(migrated.collectedEvidenceIds.contains("root_m"))
    }

    @Test
    fun migrateLegacyProgress_doesNotRunTwice() {
        val legacy = GameProgress(currentStage = GameStage.messengerSolved)
        val store = InMemoryThemeProgressStore(timeProvider = FixedTimeProvider(10L))
        store.setLegacyGameProgress(legacy)
        store.migrateLegacyProgressIfNeeded()
        store.resetThemeProgress(ThemeId.theLastCommit)
        store.migrateLegacyProgressIfNeeded()
        val progress = store.loadThemeProgress(ThemeId.theLastCommit)
        assertEquals(ThemeStatus.notStarted, progress.themeStatus)
        assertNull(progress.startedAt)
    }

    @Test
    fun resetThemeProgress_doesNotResetOtherTheme() {
        val store = InMemoryThemeProgressStore()
        store.saveThemeProgress(store.loadThemeProgress(ThemeId.theLastCommit).copy(hintCount = 3))
        store.saveThemeProgress(store.loadThemeProgress(ThemeId.convenienceStoreLoop).copy(hintCount = 5))
        store.resetThemeProgress(ThemeId.theLastCommit)
        assertEquals(0, store.loadThemeProgress(ThemeId.theLastCommit).hintCount)
        assertEquals(5, store.loadThemeProgress(ThemeId.convenienceStoreLoop).hintCount)
    }

    @Test
    fun resetAllThemeProgress_clearsEveryTheme() {
        val store = InMemoryThemeProgressStore()
        store.saveThemeProgress(store.loadThemeProgress(ThemeId.theLastCommit).copy(hintCount = 3))
        store.saveThemeProgress(store.loadThemeProgress(ThemeId.convenienceStoreLoop).copy(hintCount = 5))
        store.resetAllThemeProgress()
        assertEquals(0, store.loadThemeProgress(ThemeId.theLastCommit).hintCount)
        assertEquals(0, store.loadThemeProgress(ThemeId.convenienceStoreLoop).hintCount)
    }

    @Test
    fun themeSelection_returnsAvailableThemes() {
        val themes = com.example.escapephone.core.theme.ThemeRegistry.themes
        assertEquals(2, themes.size)
        assertTrue(themes.any { it.themeId == ThemeId.theLastCommit && it.isAvailable })
    }

    @Test
    fun themeProgress_isStoredSeparately() {
        val store = InMemoryThemeProgressStore()
        store.saveThemeProgress(store.loadThemeProgress(ThemeId.theLastCommit).copy(hintCount = 1))
        assertFalse(store.loadThemeProgress(ThemeId.convenienceStoreLoop).hintCount == 1)
    }
}
