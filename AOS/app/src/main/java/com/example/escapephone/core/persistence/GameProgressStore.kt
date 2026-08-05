package com.example.escapephone.core.persistence

import android.content.Context
import com.example.escapephone.core.model.GameProgress
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface GameProgressStore { fun load(): GameProgress?; fun save(gameProgress: GameProgress); fun reset() }

class PlatformGameProgressStore(context: Context) : GameProgressStore {
    private val preferences = context.getSharedPreferences("escape_phone", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    override fun load(): GameProgress? = preferences.getString(STORAGE_KEY, null)?.let { value -> try { json.decodeFromString<GameProgress>(value) } catch (_: SerializationException) { null } catch (_: IllegalArgumentException) { null } }
    override fun save(gameProgress: GameProgress) { preferences.edit().putString(STORAGE_KEY, json.encodeToString(gameProgress)).apply() }
    override fun reset() { preferences.edit().remove(STORAGE_KEY).apply() }
    companion object { const val STORAGE_KEY = "escape_phone_game_progress_v1" }
}

class InMemoryGameProgressStore(var gameProgress: GameProgress? = null) : GameProgressStore {
    override fun load() = gameProgress
    override fun save(gameProgress: GameProgress) { this.gameProgress = gameProgress }
    override fun reset() { gameProgress = null }
}

fun interface TimeProvider { fun now(): Long }
object SystemTimeProvider : TimeProvider { override fun now() = System.currentTimeMillis() }
