package com.example.escapephone.core.theme

import android.content.Context
import com.example.escapephone.core.model.GameProgress
import com.example.escapephone.core.model.GameStage
import com.example.escapephone.core.persistence.SystemTimeProvider
import com.example.escapephone.core.persistence.TimeProvider
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface ThemeProgressStore {
    fun loadThemeProgress(themeId: ThemeId): ThemeProgress
    fun saveThemeProgress(themeProgress: ThemeProgress)
    fun resetThemeProgress(themeId: ThemeId)
    fun resetAllThemeProgress()
    fun migrateLegacyProgressIfNeeded()
}

private val json = Json { ignoreUnknownKeys = true }

class PlatformThemeProgressStore(
    context: Context,
    private val timeProvider: TimeProvider = SystemTimeProvider
) : ThemeProgressStore {
    private val preferences = context.getSharedPreferences("escape_phone_themes", Context.MODE_PRIVATE)
    private val legacyPreferences = context.getSharedPreferences("escape_phone", Context.MODE_PRIVATE)

    override fun loadThemeProgress(themeId: ThemeId): ThemeProgress {
        val stored = preferences.getString(keyFor(themeId), null)
        val decoded = stored?.let { value ->
            try { json.decodeFromString<ThemeProgress>(value) } catch (_: SerializationException) { null } catch (_: IllegalArgumentException) { null }
        }
        return decoded ?: ThemeRegistry.defaultProgress(themeId)
    }

    override fun saveThemeProgress(themeProgress: ThemeProgress) {
        preferences.edit().putString(keyFor(themeProgress.themeId), json.encodeToString(themeProgress)).apply()
    }

    override fun resetThemeProgress(themeId: ThemeId) {
        preferences.edit().remove(keyFor(themeId)).apply()
    }

    override fun resetAllThemeProgress() {
        preferences.edit().clear().apply()
        preferences.edit().putBoolean(MIGRATION_FLAG_KEY, true).apply()
    }

    override fun migrateLegacyProgressIfNeeded() {
        if (preferences.getBoolean(MIGRATION_FLAG_KEY, false)) return
        preferences.edit().putBoolean(MIGRATION_FLAG_KEY, true).apply()

        val hasTheLastCommitProgress = preferences.contains(keyFor(ThemeId.theLastCommit))
        if (hasTheLastCommitProgress) return

        val legacyRaw = legacyPreferences.getString("escape_phone_game_progress_v1", null) ?: return
        val legacy = try { json.decodeFromString<GameProgress>(legacyRaw) } catch (_: SerializationException) { null } catch (_: IllegalArgumentException) { null } ?: return

        val migrated = migrateLegacyGameProgress(legacy, timeProvider)
        saveThemeProgress(migrated)
    }

    private fun keyFor(themeId: ThemeId) = "theme_progress_${themeId.storageValue}"

    companion object { const val MIGRATION_FLAG_KEY = "legacy_migration_done" }
}

fun migrateLegacyGameProgress(legacy: GameProgress, timeProvider: TimeProvider = SystemTimeProvider): ThemeProgress {
    val status = when {
        legacy.currentStage == GameStage.gameCompleted -> ThemeStatus.completed
        legacy.currentStage != GameStage.notStarted -> ThemeStatus.inProgress
        else -> ThemeStatus.notStarted
    }
    return ThemeProgress(
        themeId = ThemeId.theLastCommit,
        themeStatus = status,
        currentStageId = legacy.currentStage.name,
        startedAt = legacy.startedAt,
        lastSavedAt = legacy.lastSavedAt ?: timeProvider.now(),
        completedAt = legacy.completedAt,
        hintCount = legacy.hintCount,
        collectedEvidenceIds = legacy.collectedEvidenceIds,
        endingId = legacy.endingType?.name,
        themeSpecificState = ThemeSpecificState.TheLastCommitState()
    )
}

class InMemoryThemeProgressStore(
    private val progressByTheme: MutableMap<ThemeId, ThemeProgress> = mutableMapOf(),
    private var legacyGameProgress: GameProgress? = null,
    private val timeProvider: TimeProvider = SystemTimeProvider
) : ThemeProgressStore {
    private var migrationDone = false

    override fun loadThemeProgress(themeId: ThemeId): ThemeProgress = progressByTheme[themeId] ?: ThemeRegistry.defaultProgress(themeId)
    override fun saveThemeProgress(themeProgress: ThemeProgress) { progressByTheme[themeProgress.themeId] = themeProgress }
    override fun resetThemeProgress(themeId: ThemeId) { progressByTheme.remove(themeId) }
    override fun resetAllThemeProgress() { progressByTheme.clear(); migrationDone = true }
    override fun migrateLegacyProgressIfNeeded() {
        if (migrationDone) return
        migrationDone = true
        if (progressByTheme.containsKey(ThemeId.theLastCommit)) return
        val legacy = legacyGameProgress ?: return
        progressByTheme[ThemeId.theLastCommit] = migrateLegacyGameProgress(legacy, timeProvider)
    }

    fun setLegacyGameProgress(gameProgress: GameProgress?) { legacyGameProgress = gameProgress }
}
