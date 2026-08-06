package com.example.escapephone.core.theme

import com.example.escapephone.core.model.ConvenienceStoreProgress
import kotlinx.serialization.Serializable

@Serializable
enum class ThemeId(val storageValue: String) {
    theLastCommit("the_last_commit"),
    convenienceStoreLoop("convenience_store_loop");

    companion object {
        fun fromStorageValue(value: String): ThemeId? = entries.firstOrNull { it.storageValue == value }
    }
}

@Serializable
enum class ThemeStatus { notStarted, inProgress, completed }

enum class ThemeDifficulty { easy, normal, hard }

data class ThemeMetadata(
    val themeId: ThemeId,
    val title: String,
    val subtitle: String,
    val description: String,
    val estimatedPlayTimeMinutes: Int,
    val difficulty: String,
    val isAvailable: Boolean,
    val thumbnailResourceName: String
)

@Serializable
sealed class ThemeSpecificState {
    @Serializable
    data class TheLastCommitState(val placeholder: Boolean = true) : ThemeSpecificState()

    @Serializable
    data class ConvenienceStoreLoopState(val progress: ConvenienceStoreProgress = ConvenienceStoreProgress()) : ThemeSpecificState()
}

@Serializable
data class ThemeProgress(
    val themeId: ThemeId,
    val themeStatus: ThemeStatus = ThemeStatus.notStarted,
    val currentStageId: String = "not_started",
    val startedAt: Long? = null,
    val lastSavedAt: Long? = null,
    val completedAt: Long? = null,
    val hintCount: Int = 0,
    val collectedEvidenceIds: Set<String> = emptySet(),
    val endingId: String? = null,
    val themeSpecificState: ThemeSpecificState
)

object ThemeScreenIds {
    const val theme_selection = "theme_selection"
}

object ThemeRegistry {
    val themes: List<ThemeMetadata> = listOf(
        ThemeMetadata(
            themeId = ThemeId.theLastCommit,
            title = "The Last Commit",
            subtitle = "사라진 개발자의 휴대폰",
            description = "실종된 개발자의 휴대폰에서 조작된 기록과 사건의 진실을 찾아낸다.",
            estimatedPlayTimeMinutes = 30,
            difficulty = "보통",
            isAvailable = true,
            thumbnailResourceName = "theme_the_last_commit"
        ),
        ThemeMetadata(
            themeId = ThemeId.convenienceStoreLoop,
            title = "02:17",
            subtitle = "반복되는 편의점의 밤",
            description = "새벽 2시 17분이 반복되는 편의점에서 사라진 손님의 흔적을 복원한다.",
            estimatedPlayTimeMinutes = 30,
            difficulty = "보통",
            isAvailable = true,
            thumbnailResourceName = "theme_convenience_store_loop"
        )
    )

    fun metadataFor(themeId: ThemeId): ThemeMetadata = themes.first { it.themeId == themeId }

    fun defaultProgress(themeId: ThemeId): ThemeProgress = ThemeProgress(
        themeId = themeId,
        themeSpecificState = when (themeId) {
            ThemeId.theLastCommit -> ThemeSpecificState.TheLastCommitState()
            ThemeId.convenienceStoreLoop -> ThemeSpecificState.ConvenienceStoreLoopState()
        }
    )
}
