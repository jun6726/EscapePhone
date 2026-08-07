package com.example.escapephone.app

import android.graphics.Paint
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escapephone.BuildConfig
import com.example.escapephone.core.game.FlashlightPuzzleEngine
import com.example.escapephone.core.model.FlashlightControlMode
import com.example.escapephone.core.model.GameStage
import com.example.escapephone.core.model.AnalyticsConsentStatus
import com.example.escapephone.core.services.AdResult
import com.example.escapephone.core.services.FakeAdGateway
import com.example.escapephone.core.services.FakePuzzleDeviceConnector
import com.example.escapephone.designsystem.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable fun EscapePhoneApp(viewModel: GameViewModel) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.currentScreen) { viewModel.startPuzzleSession(state.currentScreen.puzzleId) }
    BackHandler(enabled = state.currentScreen != Screen.mainMenu) { viewModel.handleBackNavigation() }
    Surface(Modifier.fillMaxSize(), color = Background) {
        when (state.currentScreen) {
            Screen.mainMenu -> MainMenuScreen(viewModel)
            Screen.intro -> IntroScreen(viewModel)
            Screen.phoneHome -> PhoneHomeScreen(state, viewModel)
            Screen.messengerPuzzle -> MessengerPuzzleScreen(state, viewModel)
            Screen.flashlightPuzzle -> FlashlightPuzzleScreen(state, viewModel)
            Screen.encryptedNote -> EncryptedNoteScreen(state, viewModel)
            Screen.audioRecordPuzzle -> AudioRecordPuzzleScreen(state, viewModel)
            Screen.commitGraphPuzzle -> CommitGraphPuzzleScreen(state, viewModel)
            Screen.accessLogPuzzle -> AccessLogPuzzleScreen(state, viewModel)
            Screen.serverConsole -> ServerConsoleScreen(state, viewModel)
            Screen.finalDecision -> FinalDecisionScreen(state, viewModel)
            Screen.ending -> EndingScreen(state, viewModel)
            Screen.settings -> SettingsScreen(state, viewModel)
            Screen.deviceAnalytics -> DeviceAnalyticsScreen(state, viewModel)
            Screen.developerMenu -> if (BuildConfig.DEBUG) DeveloperMenuScreen(state, viewModel) else MainMenuScreen(viewModel)
        }
    }
    state.notice?.let { AlertDialog(onDismissRequest = viewModel::dismissNotice, confirmButton = { TextButton(onClick = viewModel::dismissNotice) { Text("확인") } }, title = { Text("알림") }, text = { Text(it) }) }
    if (state.gameProgress.analyticsConsentStatus == AnalyticsConsentStatus.notDetermined) AnalyticsConsentDialog(viewModel::grantAnalyticsConsent, viewModel::denyAnalyticsConsent)
}

@Composable fun MainMenuScreen(viewModel: GameViewModel, onBackToThemeSelection: (() -> Unit)? = null) {
    var confirmNew by remember { mutableStateOf(false) }; var confirmReset by remember { mutableStateOf(false) }; var about by remember { mutableStateOf(false) }; var taps by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("⌁", fontSize = 72.sp, color = Primary)
            Text("THE LAST COMMIT", fontSize = 34.sp, lineHeight = 38.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("사라진 개발자의 휴대폰", color = TextSecondary)
            Text("SYSTEM // RECOVERY MODE", color = Primary, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 24.dp).clickable { if (BuildConfig.DEBUG && ++taps >= 5) { taps = 0; viewModel.navigate(Screen.developerMenu) } })
            PrimaryAction("새 게임") { if (viewModel.hasSavedGame()) confirmNew = true else viewModel.startNewGame() }
            Spacer(Modifier.height(10.dp)); PrimaryAction("이어하기", viewModel.hasSavedGame(), viewModel::continueGame)
            Spacer(Modifier.height(10.dp)); SecondaryAction("설정") { viewModel.navigate(Screen.settings) }; SecondaryAction("게임 소개") { about = true }
            if (viewModel.hasSavedGame()) DesignGhostAction("게임 초기화", color = Error) { confirmReset = true }
            Text("25–35분 · 익명 분석 선택 · 센서 또는 터치", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        if (onBackToThemeSelection != null) BackToThemeSelectionButton(onBackToThemeSelection, Modifier.align(Alignment.TopStart).padding(20.dp))
    }
    if (confirmNew) DesignConfirmDialog("새 게임", "저장된 진행을 지우고 새 게임을 시작할까요?", { confirmNew = false }, { confirmNew = false; viewModel.startNewGame() }, confirmLabel = "새 게임 시작")
    if (confirmReset) DesignConfirmDialog("게임 초기화", "모든 진행 정보를 삭제할까요?", { confirmReset = false }, { confirmReset = false; viewModel.reset() }, confirmLabel = "초기화")
    if (about) DesignInfoDialog("게임 소개", "출시 하루 전 사라진 개발자의 가상 휴대폰에서 손상된 기록을 복구하는 방탈출 데모입니다. 동의한 경우에만 익명 플레이 분석 JSON을 비동기로 전송합니다.") { about = false }
}

@Composable fun IntroScreen(viewModel: GameViewModel) {
    val lines = listOf("NOVACODE의 앱 Trace 출시를 앞두고 개발자 한도윤이 사라졌다.", "그가 남긴 휴대폰에는 동의 없는 행동 데이터 수집의 흔적이 있다.", "서버가 증거를 지우기 전에 조작된 커밋을 추적해야 한다."); var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { for (index in lines.indices) { delay(650); shown = index + 1 } }
    Column(Modifier.fillMaxSize().padding(24.dp)) { Text("RECOVERING LOG…", color = Primary, fontFamily = FontFamily.Monospace); Spacer(Modifier.height(30.dp)); lines.forEachIndexed { index, text -> AnimatedVisibility(index < shown, enter = fadeIn()) { Text(text, fontSize = 22.sp, modifier = Modifier.padding(vertical = 12.dp)) } }; Spacer(Modifier.weight(1f)); PrimaryAction(if (shown == lines.size) "휴대폰 열기" else "건너뛰기", action = viewModel::completeIntro) }
}

private data class VirtualApp(val id: String, val title: String, val icon: String, val unlocked: Boolean, val reason: String)
@Composable fun PhoneHomeScreen(state: GameUiState, viewModel: GameViewModel) {
    val apps = listOf(VirtualApp("messenger", "메신저", "▣", true, ""), VirtualApp("photos", "손전등", "▧", state.gameProgress.messengerSolved, "메신저 기록을 먼저 복원하세요."), VirtualApp("encrypted", "암호 메모", "≡", state.gameProgress.flashlightSolved, "조작 커밋 번호를 먼저 찾으세요."), VirtualApp("audio", "음성 기록", "≋", state.gameProgress.encryptedNoteSolved, "암호 메모를 먼저 해독하세요."), VirtualApp("graph", "커밋 그래프", "⌘", state.gameProgress.audioRecordSolved, "음성 기록을 먼저 복원하세요."), VirtualApp("logs", "접근 로그", "☷", state.gameProgress.commitGraphSolved, "커밋 그래프를 먼저 복구하세요."), VirtualApp("server", "서버", ">_", state.gameProgress.accessLogSolved, "접근 로그를 먼저 대조하세요."))
    ScreenColumn("GHOST OS // ${state.currentStage.name}", onBack = { viewModel.navigate(Screen.mainMenu) }) {
        LazyVerticalGrid(GridCells.Fixed(2), modifier = Modifier.fillMaxWidth().height(650.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(apps) { app -> Card(Modifier.fillMaxWidth().height(140.dp).clickable { val route = when (app.id) { "messenger" -> Screen.messengerPuzzle; "photos" -> Screen.flashlightPuzzle; "encrypted" -> Screen.encryptedNote; "audio" -> Screen.audioRecordPuzzle; "graph" -> Screen.commitGraphPuzzle; "logs" -> Screen.accessLogPuzzle; else -> Screen.serverConsole }; viewModel.navigate(route) }.semantics { contentDescription = if (app.unlocked) app.title else "${app.title}, 잠김, ${app.reason}" }, colors = CardDefaults.cardColors(containerColor = Surface)) { Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(app.icon, fontSize = 32.sp, color = if (app.unlocked) Primary else TextSecondary); Text(app.title); if (!app.unlocked) Text("🔒 잠김", color = TextSecondary, style = MaterialTheme.typography.bodySmall) } } }
        }
        Text("증거 수집 ${state.gameProgress.collectedEvidenceIds.size}개", color = TextSecondary)
    }
}

@Composable fun MessengerPuzzleScreen(state: GameUiState, viewModel: GameViewModel) {
    var showError by remember { mutableStateOf(false) }
    ScreenColumn("메신저 복구", viewModel::handleBackNavigation) {
        Text("손상된 대화를 시간과 내용의 흐름에 맞게 정렬하세요.", color = TextSecondary)
        ReorderableList(
            items = state.messages,
            itemId = { it.id },
            enabled = !state.isSolved,
            onReorder = { reordered -> syncMessageOrder(viewModel, reordered.map { it.id }) }
        ) { message, proxy ->
            Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Row { Text(message.sender, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text(message.displayedTime, color = Primary, fontFamily = FontFamily.Monospace) }; Text(message.body) }
                    ReorderControls(proxy)
                }
            }
        }
        if (state.isSolved || state.gameProgress.messengerSolved) { Text("✓ 기록 복원 완료 · 사진첩 잠금 해제", color = Success); PrimaryAction("휴대폰 홈으로") { viewModel.navigate(Screen.phoneHome) } } else {
            PrimaryAction("순서 확인") { showError = !viewModel.submitMessengerOrder() }
            Text("순서가 맞지 않습니다.", color = Error, modifier = Modifier.alpha(if (showError) 1f else 0f))
            HintSection(listOf("메시지에 적힌 시간과 행동의 순서를 비교해 보세요.", "사진 확인은 서버 백업과 마지막 커밋보다 먼저입니다.")) { level -> viewModel.requestHint("messenger_order.$level", if (level == 1) "메시지에 적힌 시간과 행동의 순서를 비교해 보세요." else "사진 확인은 서버 백업과 마지막 커밋보다 먼저입니다.") }
        }
    }
}

@Composable fun FlashlightPuzzleScreen(state: GameUiState, viewModel: GameViewModel) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }; var showInstructions by rememberSaveable { mutableStateOf(!state.gameProgress.flashlightSolved) }
    LaunchedEffect(Unit) { viewModel.calibrate() }
    LaunchedEffect(state.currentScreen, state.gameProgress.flashlightSolved) {
        while (viewModel.uiState.value.currentScreen == Screen.flashlightPuzzle && !viewModel.uiState.value.gameProgress.flashlightSolved) {
            delay(50)
            viewModel.tickFlashlightPuzzle(.05)
        }
    }
    ScreenColumn("어두운 사진", { viewModel.stop(); viewModel.handleBackNavigation() }) {
        Row { Text(if (state.gameProgress.controlMode == FlashlightControlMode.motion) "◉ 기울기 조작" else "☝ 터치 조작"); Spacer(Modifier.weight(1f)); Text(state.gameProgress.discoveredDigits.joinToString(" · "), color = Primary, fontSize = 22.sp, fontFamily = FontFamily.Monospace) }
        Canvas(Modifier.fillMaxWidth().height(420.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black).onSizeChanged { canvasSize = it }.pointerInput(state.gameProgress.controlMode, canvasSize) { if (state.gameProgress.controlMode == FlashlightControlMode.touch) detectDragGestures { change, _ -> if (canvasSize.width > 0 && canvasSize.height > 0) viewModel.updateFlashlightPosition(change.position.x / canvasSize.width, change.position.y / canvasSize.height, 0.0) } }.semantics { contentDescription = "어두운 사진. 발견한 숫자 ${state.gameProgress.discoveredDigits.joinToString()}" }) {
            FlashlightPuzzleEngine.defaultTargets.forEach { target -> val distance = FlashlightPuzzleEngine.distance(state.flashlightX, state.flashlightY, target.x, target.y); drawIntoCanvas { canvas -> val paint = Paint().apply { color = android.graphics.Color.WHITE; alpha = if (distance < .18f) 230 else 10; textSize = 58.sp.toPx(); textAlign = Paint.Align.CENTER; isFakeBoldText = true }; canvas.nativeCanvas.drawText(target.digit.toString(), target.x * size.width, target.y * size.height, paint) } }
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = .42f), Primary.copy(alpha = .12f), Color.Transparent)), radius = 82.dp.toPx(), center = Offset(state.flashlightX * size.width, state.flashlightY * size.height)); drawCircle(Primary.copy(alpha = .6f), radius = 76.dp.toPx(), center = Offset(state.flashlightX * size.width, state.flashlightY * size.height), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        }
        if (!viewModel.isAvailable) Text("센서를 사용할 수 없어 터치 모드로 동작합니다.", color = TextSecondary)
        Row { OutlinedButton(onClick = viewModel::calibrate, enabled = state.gameProgress.controlMode == FlashlightControlMode.motion) { Text("기준 다시 맞추기") }; Spacer(Modifier.width(8.dp)); OutlinedButton(onClick = { viewModel.setControlMode(if (state.gameProgress.controlMode == FlashlightControlMode.motion) FlashlightControlMode.touch else FlashlightControlMode.motion) }, enabled = viewModel.isAvailable || state.gameProgress.controlMode == FlashlightControlMode.motion) { Text(if (state.gameProgress.controlMode == FlashlightControlMode.motion) "터치로 전환" else "기울기로 전환") } }
        run {
            val motion = state.gameProgress.controlMode == FlashlightControlMode.motion
            val hints = if (motion) listOf("사진을 밝히는 방법이 화면 터치만은 아닐 수 있습니다.", "휴대폰을 천천히 기울여 사진의 구석을 확인하세요.") else listOf("빛을 사진의 어두운 부분으로 이동해 보세요.", "빛을 천천히 드래그해 사진의 구석을 확인하세요.")
            HintSection(hints) { level -> viewModel.requestHint("flashlight_search.${state.gameProgress.controlMode}.$level", hints[level - 1]) }
        }
        if (state.gameProgress.flashlightSolved) { Text("✓ 조작 커밋 417 발견 · 암호 메모 잠금 해제", color = Success); PrimaryAction("암호 메모 열기") { viewModel.navigate(Screen.encryptedNote) } }
    }
    if (showInstructions) AlertDialog(onDismissRequest = {}, confirmButton = { Button({ showInstructions = false }) { Text("확인") } }, title = { Text("손전등 사용 방법") }, text = { Text("스마트폰의 기울기에 따라서 손전등의 표시가 움직입니다. 특정 문자 위에서 1초이상 있으면 우측 상단에 기록됩니다.\n\n위에서 부터 힌트를 찾아주세요.", fontWeight = FontWeight.Bold) })
}

@Composable fun ServerConsoleScreen(state: GameUiState, viewModel: GameViewModel) {
    var error by remember { mutableStateOf(false) }
    ScreenColumn("서버 콘솔", viewModel::handleBackNavigation) {
        Text("root@recovery:~$ authorize --code", color = Primary, fontFamily = FontFamily.Monospace)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { repeat(6) { index -> Box(Modifier.padding(3.dp).size(48.dp, 64.dp).background(if (error) Error.copy(alpha = .2f) else Surface, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text(state.serverCodeInput.getOrNull(index)?.toString() ?: "—", fontSize = 28.sp, fontFamily = FontFamily.Monospace) } } }
        LazyVerticalGrid(GridCells.Fixed(3), modifier = Modifier.fillMaxWidth().height(280.dp)) { items((1..9).toList()) { digit -> KeypadButton(digit.toString()) { viewModel.appendServerCodeDigit(digit) } }; item { KeypadButton("전체 삭제", viewModel::clearServerCode) }; item { KeypadButton("0") { viewModel.appendServerCodeDigit(0) } }; item { KeypadButton("지우기", viewModel::removeServerCodeDigit) } }
        Text("조작 커밋 417 + 증거 삭제 예약 01:21의 분 단위 121", color = TextSecondary)
        PrimaryAction("코드 제출", state.serverCodeInput.length == 6) { error = viewModel.submitServerCode().name == "incorrect" }
    }
}

@Composable fun EndingScreen(state: GameUiState, viewModel: GameViewModel) {
    val context = LocalContext.current
    val analyticsEnabled = state.gameProgress.analyticsConsentStatus == AnalyticsConsentStatus.granted
    val duration = if (state.gameProgress.startedAt != null && state.gameProgress.completedAt != null) ((state.gameProgress.completedAt - state.gameProgress.startedAt) / 1000).let { "${it / 60}분 ${it % 60}초" } else "기록 없음"
    var difficulty by remember { mutableIntStateOf(state.gameProgress.playerFeedback?.difficultyRating ?: 0) }; var feedback by remember { mutableStateOf(state.gameProgress.playerFeedback?.comment ?: "") }; var saved by remember { mutableStateOf(state.gameProgress.playerFeedback != null) }
    ScreenColumn("서버 복구 성공") { Text("✓", color = Success, fontSize = 72.sp); Text("한도윤의 증거 복구 절차가 완료되었습니다.", fontSize = 21.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(16.dp)).padding(20.dp)); InfoRow("최종 선택", if (state.gameProgress.endingType == com.example.escapephone.core.model.EndingType.publicDisclosure) "외부 감사 서버 공개" else "암호화 보관"); InfoRow("수집 증거", "${state.gameProgress.collectedEvidenceIds.size}개"); InfoRow("총 플레이 시간", duration); InfoRow("힌트 사용", "${state.gameProgress.hintCount}회"); InfoRow("손전등 조작", if (state.gameProgress.controlMode == FlashlightControlMode.motion) "기울기" else "터치"); HorizontalDivider(); Text("플레이 경험을 알려주세요", fontWeight = FontWeight.Bold); if (!analyticsEnabled) Text("설정에서 익명 분석 수집에 동의하면 난이도와 의견을 보낼 수 있습니다.", color = TextSecondary); Text("난이도 · 1 매우 쉬움 / 5 매우 어려움", color = TextSecondary); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { (1..5).forEach { value -> FilterChip(difficulty == value, { difficulty = value; saved = false }, { Text(value.toString()) }, enabled = analyticsEnabled) } }; OutlinedTextField(feedback, { feedback = it.take(1000); saved = false }, Modifier.fillMaxWidth(), enabled = analyticsEnabled, label = { Text("불편했던 점이나 개선 의견") }, minLines = 3, supportingText = { Text("${feedback.length}/1000") }); PrimaryAction(if (saved) "피드백 저장 완료" else "피드백 저장", analyticsEnabled && difficulty in 1..5 && !saved) { saved = viewModel.submitPlayerFeedback(difficulty, feedback) }; Text(if (BuildConfig.PLAYTEST_ANALYTICS_ENDPOINT.isBlank()) "서버가 설정되지 않아 분석 JSON을 암호화된 앱 저장소에 보관합니다." else "분석 JSON은 화면 로딩을 막지 않고 비동기로 전송됩니다.", color = TextSecondary, style = MaterialTheme.typography.bodySmall); if (analyticsEnabled) SecondaryAction("익명 분석 보고서 공유") { val intent = Intent(Intent.ACTION_SEND).apply { type = "application/json"; putExtra(Intent.EXTRA_TEXT, viewModel.exportPlaytestReport()) }; context.startActivity(Intent.createChooser(intent, "플레이테스트 보고서 공유")) }; PrimaryAction("다시 플레이", action = viewModel::startNewGame); SecondaryAction("첫 화면으로 이동") { viewModel.navigate(Screen.mainMenu) } }
}

@Composable fun SettingsScreen(state: GameUiState, viewModel: GameViewModel, onBack: (() -> Unit)? = null) { ScreenColumn("설정", onBack ?: { viewModel.navigate(Screen.mainMenu) }, centerTitle = true) { Text("손전등 조작", fontWeight = FontWeight.Bold); Row { FilterChip(state.gameProgress.controlMode == FlashlightControlMode.motion, { viewModel.setControlMode(FlashlightControlMode.motion) }, { Text("기울기") }, enabled = viewModel.isAvailable); Spacer(Modifier.width(8.dp)); FilterChip(state.gameProgress.controlMode == FlashlightControlMode.touch, { viewModel.setControlMode(FlashlightControlMode.touch) }, { Text("터치") }) }; if (!viewModel.isAvailable) Text("현재 환경에서는 기울기 센서를 사용할 수 없습니다.", color = TextSecondary); HorizontalDivider(); Text("익명 플레이 분석", fontWeight = FontWeight.Bold); Text("퍼즐별 시간·오답 원인·힌트·이탈과 선택 입력한 피드백만 JSON으로 수집합니다. 계정, 위치, 사진, 연락처, 마이크와 광고 식별자는 포함하지 않습니다."); Text("현재 상태: ${if (state.gameProgress.analyticsConsentStatus == AnalyticsConsentStatus.granted) "동의함" else "동의하지 않음"} · 대기 ${state.gameProgress.pendingAnalyticsUploads.size}건", color = TextSecondary); OutlinedButton({ viewModel.navigate(Screen.deviceAnalytics) }) { Text("기기 분석 데이터 보기") }; if (state.gameProgress.analyticsConsentStatus == AnalyticsConsentStatus.granted) OutlinedButton(viewModel::denyAnalyticsConsent) { Text("동의 철회 및 기기 내 분석 데이터 삭제") } else Button(viewModel::grantAnalyticsConsent) { Text("익명 분석 수집 동의") }; Text("The Last Commit 1.2", color = TextSecondary) } }

@Composable fun DeviceAnalyticsScreen(state: GameUiState, viewModel: GameViewModel) {
    val progress = state.gameProgress
    val context = LocalContext.current
    val status = when {
        progress.analyticsConsentStatus != AnalyticsConsentStatus.granted -> "수집 안 함"
        progress.pendingAnalyticsUploads.isNotEmpty() -> "기기에 저장됨 · 전송 대기"
        progress.lastAnalyticsUploadAt != null -> "서버 전송 완료"
        else -> "동의됨 · 로컬 기록 중"
    }
    ScreenColumn("기기 분석 데이터", { viewModel.navigate(Screen.settings) }) {
        Text("수집·전송 진단", fontWeight = FontWeight.Bold)
        InfoRow("현재 상태", status)
        InfoRow("퍼즐 기록", "${progress.puzzleAnalytics.size}개")
        InfoRow("전송 대기", "${progress.pendingAnalyticsUploads.size}건")
        InfoRow("생성한 전송", "${progress.analyticsUploadSequence}건")
        InfoRow("마지막 성공", progress.lastAnalyticsUploadAt?.let { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.KOREA).format(java.util.Date(it)) } ?: "없음")
        progress.lastAnalyticsUploadError?.let { Text("마지막 실패: $it", color = Error) }
        progress.anonymousSessionId?.let { Text("세션 ID\n$it", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
        PrimaryAction("현재 데이터 즉시 전송", progress.analyticsConsentStatus == AnalyticsConsentStatus.granted, viewModel::retryAnalyticsUploadNow)
        Text("대기가 1건 이상이면 JSON은 기기에 있으나 서버가 아직 받지 못한 상태입니다. 대기 0건과 마지막 성공 시각이 함께 보이면 정상 전송된 상태입니다.", color = TextSecondary)
        HorizontalDivider()
        Text("퍼즐별 기기 기록", fontWeight = FontWeight.Bold)
        if (progress.puzzleAnalytics.isEmpty()) Text("아직 저장된 퍼즐 기록이 없습니다.", color = TextSecondary)
        progress.puzzleAnalytics.toSortedMap().forEach { (id, analytics) ->
            val seconds = analytics.elapsedMs / 1_000
            Card(colors = CardDefaults.cardColors(containerColor = Surface)) { Column(Modifier.fillMaxWidth().padding(14.dp)) { Text(id, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace); Text("시간 ${seconds / 60}분 ${seconds % 60}초 · 세션 ${analytics.sessionCount}회"); Text("오답 ${analytics.wrongAttemptCount}회 · 힌트 ${analytics.hintViewCount}회 · 이탈 ${analytics.exitEvents.size}회"); Text(if (analytics.completedAt == null) "진행 중" else "해결 완료", color = if (analytics.completedAt == null) Primary else Success) } }
        }
        HorizontalDivider()
        Text("전송 대기 상세", fontWeight = FontWeight.Bold)
        if (progress.pendingAnalyticsUploads.isEmpty()) Text("대기 중인 JSON이 없습니다.", color = TextSecondary)
        progress.pendingAnalyticsUploads.forEach { Text("#${it.envelope.sequence} · 시도 ${it.attemptCount}회 · ${if (it.envelope.isFinal) "최종" else "중간"}", fontFamily = FontFamily.Monospace) }
        val json = viewModel.exportPlaytestReport()
        OutlinedButton({ context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "application/json"; putExtra(Intent.EXTRA_TEXT, json) }, "분석 JSON 공유")) }) { Text("JSON 공유") }
        Text(json, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable fun DeveloperMenuScreen(state: GameUiState, viewModel: GameViewModel) {
    if (!BuildConfig.DEBUG) return
    val scope = rememberCoroutineScope(); var pitch by remember { mutableFloatStateOf(0f) }; var roll by remember { mutableFloatStateOf(0f) }; var yaw by remember { mutableFloatStateOf(0f) }; var adStatus by remember { mutableStateOf("rewarded") }
    ScreenColumn("개발 메뉴", { viewModel.navigate(Screen.mainMenu) }) {
        Text(state.gameProgress.toString(), fontFamily = FontFamily.Monospace, fontSize = 12.sp); Text("센서 지원: ${viewModel.isAvailable}")
        Row { TextButton(onClick = { viewModel.completeMessengerPuzzle() }) { Text("메신저 성공") }; TextButton(onClick = { viewModel.completeFlashlightPuzzle() }) { Text("손전등 성공") }; TextButton(onClick = { viewModel.completeMessengerPuzzle(); viewModel.completeFlashlightPuzzle(); viewModel.completeEncryptedNotePuzzle(); viewModel.completeAudioRecordPuzzle(); viewModel.completeCommitGraphPuzzle(); viewModel.completeAccessLogPuzzle(); viewModel.navigate(Screen.serverConsole) }) { Text("서버 이동") } }
        Text("pitch ${state.pitch.format()} → ${(state.pitch * .16).format()}"); Slider(pitch, { pitch = it }, valueRange = -.8f..0.8f); Text("roll ${state.roll.format()} → ${(state.roll * .16).format()}"); Slider(roll, { roll = it }, valueRange = -.8f..0.8f); Text("yaw ${state.yaw.format()}"); Slider(yaw, { yaw = it }, valueRange = -1f..1f); Button(onClick = { viewModel.debugSetAttitude(pitch.toDouble(), roll.toDouble(), yaw.toDouble()) }) { Text("Mock 센서값 적용") }
        Row { listOf("rewarded", "skipped", "failed").forEach { choice -> FilterChip(adStatus == choice, { adStatus = choice; (viewModel.adGateway as? FakeAdGateway)?.result = if (choice == "rewarded") AdResult.rewarded else if (choice == "skipped") AdResult.skipped else AdResult.failed("Debug 실패") }, { Text(choice) }); Spacer(Modifier.width(4.dp)) } }
        Text("장치 상태: ${viewModel.puzzleDeviceConnector.state}"); Row { Button(onClick = { scope.launch { viewModel.puzzleDeviceConnector.connect("debug") } }) { Text("연결") }; Spacer(Modifier.width(8.dp)); Button(onClick = { scope.launch { viewModel.puzzleDeviceConnector.disconnect() } }) { Text("해제") } }
        TextButton(onClick = viewModel::reset) { Text("저장 데이터 삭제 및 전체 초기화", color = Error) }
    }
}

/** 사용자가 손을 뗀 순간의 시각적 배치를 실제 메신저 엔진 순서에 반영한다. */
private fun syncMessageOrder(viewModel: GameViewModel, targetOrder: List<String>) {
    val currentOrder = viewModel.uiState.value.messages.map { it.id }
    if (currentOrder == targetOrder) return
    val maxIterations = targetOrder.size * targetOrder.size
    var iterations = 0
    while (viewModel.uiState.value.messages.map { it.id } != targetOrder && iterations < maxIterations) {
        iterations++
        val liveOrder = viewModel.uiState.value.messages.map { it.id }
        val mismatchIndex = liveOrder.indices.firstOrNull { liveOrder[it] != targetOrder[it] } ?: break
        val targetId = targetOrder[mismatchIndex]
        val currentIndex = liveOrder.indexOf(targetId)
        if (currentIndex < 0) break
        if (currentIndex > mismatchIndex) viewModel.moveMessageUp(currentIndex) else viewModel.moveMessageDown(currentIndex)
    }
}

@Composable private fun ScreenColumn(title: String, onBack: (() -> Unit)? = null, centerTitle: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    var isReorderDragging by remember { mutableStateOf(false) }
    var contentHeightPx by remember { mutableIntStateOf(0) }
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    val scrollNeeded = contentHeightPx > viewportHeightPx
    CompositionLocalProvider(LocalReorderDragReporter provides { isReorderDragging = it }) {
        Column(
            Modifier
                .fillMaxSize()
                .onSizeChanged { viewportHeightPx = it.height }
                .verticalScroll(rememberScrollState(), enabled = scrollNeeded && !isReorderDragging)
                .padding(20.dp)
                .onSizeChanged { contentHeightPx = it.height },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (centerTitle) {
                Box(Modifier.fillMaxWidth()) {
                    if (onBack != null) IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) { Text("←", fontSize = 22.sp, color = TextPrimary) }
                    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) { if (onBack != null) TextButton(onClick = onBack) { Text("‹ 뒤로") }; Text(title, fontSize = 23.sp, fontWeight = FontWeight.Bold) }
            }
            content()
        }
    }
}
@Composable private fun PrimaryAction(text: String, enabled: Boolean = true, action: () -> Unit) { Button(action, Modifier.fillMaxWidth().height(52.dp), enabled = enabled, shape = RoundedCornerShape(16.dp)) { Text(text, fontWeight = FontWeight.Bold) } }
@Composable private fun SecondaryAction(text: String, action: () -> Unit) { OutlinedButton(action, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text(text) } }
@Composable private fun ConfirmDialog(text: String, cancel: () -> Unit, confirm: () -> Unit) { AlertDialog(cancel, confirmButton = { TextButton(confirm) { Text("확인", color = Error) } }, dismissButton = { TextButton(cancel) { Text("취소") } }, text = { Text(text) }) }
@Composable private fun AnalyticsConsentDialog(accept: () -> Unit, decline: () -> Unit) { AlertDialog(onDismissRequest = {}, confirmButton = { Button(accept) { Text("동의하고 계속") } }, dismissButton = { TextButton(decline) { Text("동의하지 않음") } }, title = { Text("익명 플레이 분석에 참여할까요?") }, text = { Text("게임 개선을 위해 퍼즐별 소요 시간, 오답 횟수와 원인, 힌트 조회, 뒤로가기·백그라운드 이탈, 선택 입력한 난이도와 의견을 익명 세션 ID와 함께 JSON으로 수집합니다. 계정·위치·사진·연락처·마이크·광고 식별자는 수집하지 않습니다. 동의는 설정에서 언제든 철회할 수 있습니다.") }) }
@Composable private fun KeypadButton(text: String, action: () -> Unit) { Button(action, Modifier.padding(5.dp).fillMaxWidth().height(62.dp), colors = ButtonDefaults.buttonColors(containerColor = Surface)) { Text(text, fontSize = if (text.length == 1) 24.sp else 11.sp) } }
@Composable private fun InfoRow(title: String, value: String) { Row(Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(12.dp)).padding(14.dp)) { Text(title, color = TextSecondary); Spacer(Modifier.weight(1f)); Text(value, fontFamily = FontFamily.Monospace) } }
private fun Double.format() = "%.2f".format(this)
