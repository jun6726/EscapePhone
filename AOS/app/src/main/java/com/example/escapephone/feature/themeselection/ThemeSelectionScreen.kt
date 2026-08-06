package com.example.escapephone.feature.themeselection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escapephone.core.theme.ThemeId
import com.example.escapephone.core.theme.ThemeMetadata
import com.example.escapephone.core.theme.ThemeProgress
import com.example.escapephone.core.theme.ThemeRegistry
import com.example.escapephone.core.theme.ThemeStatus
import com.example.escapephone.designsystem.Background
import com.example.escapephone.designsystem.DesignConfirmDialog
import com.example.escapephone.designsystem.DesignGhostAction
import com.example.escapephone.designsystem.Elevated
import com.example.escapephone.designsystem.Error
import com.example.escapephone.designsystem.Hairline
import com.example.escapephone.designsystem.Primary
import com.example.escapephone.designsystem.Success
import com.example.escapephone.designsystem.Surface
import com.example.escapephone.designsystem.TextPrimary
import com.example.escapephone.designsystem.TextSecondary

data class ThemeSelectionState(
    val selectedThemeId: ThemeId? = null,
    val themeMetadataList: List<ThemeMetadata> = ThemeRegistry.themes,
    val themeProgressList: List<ThemeProgress> = emptyList(),
    val selectedThemeProgress: ThemeProgress? = null,
    val isResetConfirmationVisible: Boolean = false
)

private fun accentColorFor(themeId: ThemeId): Color = when (themeId) {
    ThemeId.theLastCommit -> Primary
    ThemeId.convenienceStoreLoop -> Color(0xFFB362FF)
}

@Composable
fun ThemeSelectionScreen(
    themeMetadataList: List<ThemeMetadata>,
    progressFor: (ThemeId) -> ThemeProgress,
    onStartTheme: (ThemeId) -> Unit,
    onContinueTheme: (ThemeId) -> Unit,
    onOpenSettings: () -> Unit,
    onResetAll: () -> Unit
) {
    var confirmResetAll by remember { mutableStateOf(false) }
    Surface(color = Background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
            Text("EscapePhone", fontSize = 32.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("모바일 미스터리 방탈출", color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp))
            themeMetadataList.forEach { metadata ->
                val progress = progressFor(metadata.themeId)
                ThemeCard(metadata, progress, accentColorFor(metadata.themeId), onStartTheme, onContinueTheme)
                Spacer(Modifier.height(16.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Elevated),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) { Text("설정", color = TextPrimary, fontWeight = FontWeight.Medium) }
                Box(Modifier.width(1.dp).height(24.dp).align(Alignment.CenterVertically).background(Hairline))
                TextButton(onClick = { confirmResetAll = true }, modifier = Modifier.weight(1f)) { Text("전체 데이터 초기화", color = Error, fontWeight = FontWeight.Medium) }
            }
        }
    }
    if (confirmResetAll) {
        DesignConfirmDialog(
            title = "전체 데이터 초기화",
            text = "모든 테마의 진행 정보가 삭제됩니다. 계속할까요?",
            cancel = { confirmResetAll = false },
            confirm = { confirmResetAll = false; onResetAll() },
            confirmLabel = "초기화"
        )
    }
}

@Composable
private fun ThemeCard(
    metadata: ThemeMetadata,
    progress: ThemeProgress,
    accent: Color,
    onStartTheme: (ThemeId) -> Unit,
    onContinueTheme: (ThemeId) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Hairline)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = .18f), Color.Transparent)))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(metadata.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                StatusBadge(progress.themeStatus)
            }
            Text(metadata.subtitle, color = accent, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(8.dp))
            Text(metadata.description, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Text("예상 플레이 시간 ${metadata.estimatedPlayTimeMinutes}분 · 난이도 ${metadata.difficulty}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (progress.themeStatus == ThemeStatus.notStarted) onStartTheme(metadata.themeId) else onContinueTheme(metadata.themeId) },
                enabled = metadata.isAvailable,
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (progress.themeStatus == ThemeStatus.notStarted) "시작하기" else "이어하기", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ThemeStatus) {
    val (label, color) = when (status) {
        ThemeStatus.notStarted -> return
        ThemeStatus.inProgress -> "진행 중" to Primary
        ThemeStatus.completed -> "완료" to Success
    }
    Text(label, color = color, style = MaterialTheme.typography.labelMedium)
}
