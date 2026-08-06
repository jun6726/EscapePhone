package com.example.escapephone.feature.themeselection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escapephone.app.ConvenienceStoreViewModel
import com.example.escapephone.app.GameViewModel
import com.example.escapephone.app.MainMenuScreen
import com.example.escapephone.core.theme.ThemeId
import com.example.escapephone.core.theme.ThemeMetadata
import com.example.escapephone.core.theme.ThemeProgress
import com.example.escapephone.designsystem.BackToThemeSelectionButton
import com.example.escapephone.designsystem.DesignConfirmDialog
import com.example.escapephone.designsystem.DesignGhostAction
import com.example.escapephone.designsystem.DesignInfoDialog
import com.example.escapephone.designsystem.DesignPrimaryAction
import com.example.escapephone.designsystem.DesignSecondaryAction
import com.example.escapephone.designsystem.Error
import com.example.escapephone.designsystem.TextPrimary
import com.example.escapephone.designsystem.TextSecondary
import com.example.escapephone.feature.themes.convenienceStore.StoreAccent

@Composable
fun ThemeMainMenuScreen(
    themeId: ThemeId,
    themeMetadata: ThemeMetadata,
    themeProgress: ThemeProgress,
    theLastCommitViewModel: GameViewModel,
    convenienceStoreViewModel: ConvenienceStoreViewModel? = null,
    onBackToThemeSelection: () -> Unit,
    onResetTheme: (() -> Unit)? = null,
    onEnterConvenienceStore: (() -> Unit)? = null
) {
    when (themeId) {
        ThemeId.theLastCommit -> MainMenuScreen(theLastCommitViewModel, onBackToThemeSelection)
        ThemeId.convenienceStoreLoop -> {
            requireNotNull(convenienceStoreViewModel)
            ConvenienceStoreMainMenu(themeMetadata, convenienceStoreViewModel, onBackToThemeSelection, onResetTheme, onEnterConvenienceStore)
        }
    }
}

@Composable
private fun ConvenienceStoreMainMenu(
    themeMetadata: ThemeMetadata,
    viewModel: ConvenienceStoreViewModel,
    onBackToThemeSelection: () -> Unit,
    onResetTheme: (() -> Unit)?,
    onEnterConvenienceStore: (() -> Unit)?
) {
    var confirmNew by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var about by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("⏱", fontSize = 64.sp, color = StoreAccent)
            Text(themeMetadata.title, fontSize = 34.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = TextPrimary)
            Text(themeMetadata.subtitle, color = TextSecondary)
            Spacer(Modifier.height(24.dp))
            DesignPrimaryAction("새 게임", accent = StoreAccent) { if (viewModel.hasSavedGame()) confirmNew = true else { viewModel.startNewGame(); onEnterConvenienceStore?.invoke() } }
            Spacer(Modifier.height(10.dp))
            DesignPrimaryAction("이어하기", enabled = viewModel.hasSavedGame(), accent = StoreAccent) { viewModel.continueGame(); onEnterConvenienceStore?.invoke() }
            Spacer(Modifier.height(10.dp))
            DesignSecondaryAction("테마 소개") { about = true }
            if (viewModel.hasSavedGame()) DesignGhostAction("테마 진행 초기화", color = Error) { confirmReset = true }
            Text("${themeMetadata.estimatedPlayTimeMinutes}분 · 센서 또는 터치", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        BackToThemeSelectionButton(onBackToThemeSelection, Modifier.align(Alignment.TopStart).padding(20.dp))
    }
    if (confirmNew) DesignConfirmDialog("새 게임", "저장된 진행을 지우고 새 게임을 시작할까요?", { confirmNew = false }, { confirmNew = false; viewModel.startNewGame(); onEnterConvenienceStore?.invoke() }, confirmLabel = "새 게임 시작")
    if (confirmReset) DesignConfirmDialog("테마 초기화", "02:17 테마의 진행 정보만 삭제됩니다.", { confirmReset = false }, { confirmReset = false; viewModel.reset(); onResetTheme?.invoke() }, confirmLabel = "초기화")
    if (about) DesignInfoDialog("게임 소개", themeMetadata.description) { about = false }
}
