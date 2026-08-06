package com.example.escapephone.app

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escapephone.core.game.*
import com.example.escapephone.core.model.EndingType
import com.example.escapephone.designsystem.*

@Composable fun EncryptedNoteScreen(state: GameUiState, viewModel: GameViewModel) {
    val engine = remember { EncryptedNotePuzzleEngine() }; var code by remember { mutableStateOf("") }; var revision by remember { mutableIntStateOf(0) }; var hint by remember { mutableIntStateOf(0) }; var error by remember { mutableStateOf(false) }
    PuzzleColumn("암호화 메모", viewModel::handleBackNavigation) {
        Text("TRACE // NOTE-417", color = Primary, fontFamily = FontFamily.Monospace)
        if (!engine.isUnlocked) {
            Text("손전등에서 찾은 조작 커밋 번호를 입력하세요.", color = TextSecondary)
            OutlinedTextField(code, { code = it.filter(Char::isDigit).take(3); error = false }, label = { Text("3자리 커밋 번호") }, isError = error, singleLine = true)
            FullButton("메모 열기", code.length == 3) { error = !engine.unlock(code); if (error) viewModel.recordWrongAttempt("encrypted_note", "unlockCodeIncorrect"); revision++ }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = Surface)) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("복호화된 메모 조각", color = Primary, fontWeight = FontWeight.Bold)
                Text("Trace 출시 빌드의 백그라운드 상태 표시 문자열이 손상됐다.")
                Text("남은 로그: 대상은 증거 기록 / 동작은 데이터 수집 / 상태는 아직 진행 중", color = TextSecondary)
                Text("형식: [대상] [동작] [진행 상태]", fontFamily = FontFamily.Monospace, color = Primary)
            } }
            Text("로그의 뜻에 맞는 단어를 골라 아래 세 칸을 완성하세요.")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(3) { index -> Box(Modifier.weight(1f).height(58.dp).background(Surface, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text(engine.selectedWords.getOrNull(index) ?: "___", fontSize = 20.sp, fontFamily = FontFamily.Monospace, color = if (engine.selectedWords.indices.contains(index)) Primary else TextSecondary) } } }
            FlowWords(engine.noteWords) { engine.selectNoteWord(it); revision++ }
            Row { OutlinedButton({ engine.removeNoteWord(); revision++ }) { Text("마지막 단어 제거") }; Spacer(Modifier.width(8.dp)); Button({ if (engine.submitEncryptedNote()) viewModel.completeEncryptedNotePuzzle() else { error = true; viewModel.recordWrongAttempt("encrypted_note", if (engine.selectedWords.size < 3) "noteWordsIncomplete" else "noteWordOrderIncorrect") }; revision++ }) { Text("문장 확인") } }
            if (error) Text("문장 순서가 맞지 않습니다.", color = Error)
            if (state.gameProgress.encryptedNoteSolved) { Text("✓ ‘기록 수집 중’ · 음성 기록 잠금 해제", color = Success); FullButton("음성 기록 열기") { viewModel.navigate(Screen.audioRecordPuzzle) } }
        }
        HintButton("encrypted_note", hint, { hint = it }, viewModel, listOf("메모의 형식은 [대상] [동작] [진행 상태]로, 빈칸은 총 세 칸입니다.", "대상=기록, 동작=수집, 진행 상태=중입니다. 따라서 ‘기록 수집 중’입니다."))
        Text(revision.toString(), color = Color.Transparent, fontSize = 1.sp)
    }
}

@Composable fun AudioRecordPuzzleScreen(state: GameUiState, viewModel: GameViewModel) {
    val engine = remember { AudioRecordPuzzleEngine() }; var revision by remember { mutableIntStateOf(0) }; var hint by remember { mutableIntStateOf(0) }; var error by remember { mutableStateOf(false) }
    val pulse by rememberInfiniteTransition(label = "wave").animateFloat(.25f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "wavePulse")
    PuzzleColumn("손상된 음성 기록", viewModel::handleBackNavigation) {
        Text("실제 마이크를 사용하지 않습니다. 조각을 재생해 자막의 대화 흐름을 복원하세요.", color = TextSecondary)
        ReorderableList(
            items = engine.fragments,
            itemId = { it.id },
            enabled = !engine.isSolved,
            onReorder = { reordered -> syncAudioFragmentOrder(engine, reordered.map { it.id }); revision++ }
        ) { fragment, proxy ->
            val index = engine.fragments.indexOfFirst { it.id == fragment.id }
            Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.width(62.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) { repeat(6) { bar -> Box(Modifier.width(5.dp).height(if (engine.playingFragmentId == fragment.id) (10 + bar * pulse * 4).dp else 8.dp).background(Primary)) } }
                    Column(Modifier.weight(1f)) { Text("조각 ${index + 1}", fontWeight = FontWeight.Bold); Text(fragment.subtitle) }
                    TextButton({ engine.playAudioFragment(fragment.id); revision++ }) { Text("▶") }
                    ReorderControls(proxy)
                }
            }
        }
        FullButton("음성 순서 확인") { if (engine.submitAudioOrder()) viewModel.completeAudioRecordPuzzle() else { error = true; viewModel.recordWrongAttempt("audio_record", "audioOrderIncorrect") }; revision++ }
        if (error) Text("대화의 원인과 반응 순서를 다시 확인하세요.", color = Error)
        if (state.gameProgress.audioRecordSolved) { Text("✓ 복원 완료 · 강민석은 원상 복구를 지시했다", color = Success); FullButton("커밋 그래프 열기") { viewModel.navigate(Screen.commitGraphPuzzle) } }
        HintButton("audio_record", hint, { hint = it }, viewModel, listOf("승인 여부를 묻는 말이 대화의 시작입니다.", "‘위에서 결정’ → 책임 경고 → 원상 복구 지시 순서입니다."))
        Text(revision.toString(), color = Color.Transparent, fontSize = 1.sp)
    }
}

@Composable fun CommitGraphPuzzleScreen(state: GameUiState, viewModel: GameViewModel) {
    val engine = remember { CommitGraphPuzzleEngine() }; var revision by remember { mutableIntStateOf(0) }; var hint by remember { mutableIntStateOf(0) }; var error by remember { mutableStateOf(false) }
    PuzzleColumn("커밋 그래프 복구", viewModel::handleBackNavigation) {
        Text("노드를 올바른 브랜치에 배치하고 핵심 조작 흐름을 연결하세요.", color = TextSecondary)
        val orderedNodes = engine.nodeOrder.mapNotNull { id -> engine.nodes.firstOrNull { it.id == id } }
        ReorderableList(
            items = orderedNodes,
            itemId = { it.id },
            enabled = !engine.isSolved,
            onReorder = { reordered -> syncCommitNodeOrder(engine, reordered.map { it.id }); revision++ }
        ) { node, proxy ->
            Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${node.number}  ${node.title}", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        ReorderControls(proxy)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { engine.branches.forEach { branch -> FilterChip(engine.placements[node.id] == branch, { engine.moveCommitNode(node.id, branch); revision++ }, { Text(branch, fontSize = 10.sp) }) } }
                }
            }
        }
        Row { OutlinedButton({ engine.connectCommitNode("417", "418"); revision++ }) { Text("417 → 418") }; Spacer(Modifier.width(8.dp)); OutlinedButton({ engine.connectCommitNode("418", "420"); revision++ }) { Text("418 → 420") } }
        Text("연결: ${engine.connections.joinToString()}", color = TextSecondary, fontFamily = FontFamily.Monospace)
        FullButton("그래프 검증") { if (engine.submitCommitGraph()) viewModel.completeCommitGraphPuzzle() else { error = true; val reason = when { engine.nodes.any { engine.placements[it.id] != it.correctBranch } -> "commitBranchIncorrect"; engine.nodeOrder != engine.nodes.sortedBy { it.correctOrder }.map { it.id } -> "commitOrderIncorrect"; else -> "commitConnectionMissing" }; viewModel.recordWrongAttempt("commit_graph", reason) }; revision++ }
        if (error) Text("브랜치 또는 핵심 연결이 틀렸습니다.", color = Error)
        if (state.gameProgress.commitGraphSolved) { Text("✓ ROOT-M · 01:14 기록 발견", color = Success); FullButton("접근 로그 열기") { viewModel.navigate(Screen.accessLogPuzzle) } }
        HintButton("commit_graph", hint, { hint = it }, viewModel, listOf("실험과 백그라운드 수집은 analytics 브랜치에 있습니다.", "작성자 조작 418은 417 다음이며, 420에서 main에 병합됩니다."))
        Text(revision.toString(), color = Color.Transparent, fontSize = 1.sp)
    }
}

@Composable fun AccessLogPuzzleScreen(state: GameUiState, viewModel: GameViewModel) {
    val engine = remember { AccessLogPuzzleEngine() }; var revision by remember { mutableIntStateOf(0) }; var hint by remember { mutableIntStateOf(0) }; var error by remember { mutableStateOf(false) }
    val questions = listOf("커밋 조작 시점에 한도윤이 사무실에 있었는가?" to listOf("있었다", "없었다"), "01:14 메시지는 실시간 메시지인가 예약 메시지인가?" to listOf("실시간 메시지다", "예약 메시지다"), "ROOT-M은 개인 계정인가 관리자 공용 계정인가?" to listOf("개인 계정이다", "관리자 공용 계정이다"))
    PuzzleColumn("접근 로그 교차 검증", viewModel::handleBackNavigation) {
        listOf("출입 기록 · 한도윤 00:38 퇴실 / 재입실 없음", "서버 로그인 · 01:12 ROOT-M / CTO 회의실 관리자 단말기", "메신저 · 01:14 예약 전송 플래그 / 삭제 예약 01:21").forEachIndexed { index, line -> Card(Modifier.fillMaxWidth().clickable { engine.selectLogEvidence("log_${index + 1}"); revision++ }, colors = CardDefaults.cardColors(containerColor = if ("log_${index + 1}" in engine.selectedEvidenceIds) Primary.copy(alpha = .18f) else Surface)) { Text(line, Modifier.padding(14.dp), fontFamily = FontFamily.Monospace) } }
        questions.forEachIndexed { index, item -> Text("${index + 1}. ${item.first}", fontWeight = FontWeight.Bold); Row { item.second.forEach { answer -> FilterChip(engine.answers[index] == answer, { engine.answerLogQuestion(index, answer); revision++ }, { Text(answer) }); Spacer(Modifier.width(6.dp)) } } }
        FullButton("답변 검증") { if (engine.validateAccessLogAnswers() && engine.completeAccessLogPuzzle()) viewModel.completeAccessLogPuzzle(engine.selectedEvidenceIds) else { error = true; viewModel.recordWrongAttempt("access_log", if (engine.answers.size < 3) "logAnswersIncomplete" else "logAnswerIncorrect") }; revision++ }
        if (error) Text("세 기록의 시각과 계정 유형을 다시 대조하세요.", color = Error)
        if (state.gameProgress.accessLogSolved) { Text("✓ 실제 조작 단말기: CTO 회의실 관리용 단말기", color = Success); FullButton("서버 콘솔 열기") { viewModel.navigate(Screen.serverConsole) } }
        HintButton("access_log", hint, { hint = it }, viewModel, listOf("퇴실 시각과 서버 로그인 시각을 먼저 비교하세요.", "예약 전송 플래그와 ROOT-M의 계정 유형을 함께 확인하세요."))
        Text(revision.toString(), color = Color.Transparent, fontSize = 1.sp)
    }
}

@Composable fun FinalDecisionScreen(state: GameUiState, viewModel: GameViewModel) {
    PuzzleColumn("최종 결정") {
        Text("한도윤은 서버의 증거 삭제를 막기 위해 실종을 계획했고, 신뢰할 수 있는 누군가가 이 절차로 증거를 복구하도록 했다.")
        Text("복구한 증거를 어떻게 처리할까요? 두 선택 모두 정상 엔딩입니다.", color = TextSecondary)
        FullButton("증거를 외부 감사 서버에 공개한다") { viewModel.selectPublicDisclosure() }
        OutlinedButton(viewModel::selectEncryptedArchive, Modifier.fillMaxWidth().height(56.dp)) { Text("증거를 암호화해 보관한다") }
        Text("현재 선택: ${state.gameProgress.endingType ?: "미선택"}", color = TextSecondary)
    }
}

private fun syncAudioFragmentOrder(engine: AudioRecordPuzzleEngine, targetOrder: List<String>) {
    if (engine.fragments.map { it.id } == targetOrder) return
    val maxIterations = targetOrder.size * targetOrder.size
    var iterations = 0
    while (engine.fragments.map { it.id } != targetOrder && iterations < maxIterations) {
        iterations++
        val liveOrder = engine.fragments.map { it.id }
        val mismatchIndex = liveOrder.indices.firstOrNull { liveOrder[it] != targetOrder[it] } ?: break
        val targetId = targetOrder[mismatchIndex]
        val currentIndex = liveOrder.indexOf(targetId)
        if (currentIndex < 0) break
        if (currentIndex > mismatchIndex) engine.moveAudioFragmentUp(currentIndex) else engine.moveAudioFragmentDown(currentIndex)
    }
}

private fun syncCommitNodeOrder(engine: CommitGraphPuzzleEngine, targetOrder: List<String>) {
    if (engine.nodeOrder == targetOrder) return
    val maxIterations = targetOrder.size * targetOrder.size
    var iterations = 0
    while (engine.nodeOrder != targetOrder && iterations < maxIterations) {
        iterations++
        val liveOrder = engine.nodeOrder
        val mismatchIndex = liveOrder.indices.firstOrNull { liveOrder[it] != targetOrder[it] } ?: break
        val targetId = targetOrder[mismatchIndex]
        val currentIndex = liveOrder.indexOf(targetId)
        if (currentIndex < 0) break
        val direction = if (currentIndex > mismatchIndex) -1 else 1
        engine.moveCommitNode(targetId, (currentIndex + direction).coerceIn(0, engine.nodeOrder.lastIndex))
    }
}

@Composable private fun PuzzleColumn(title: String, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
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
            Row(verticalAlignment = Alignment.CenterVertically) { if (onBack != null) TextButton(onBack) { Text("‹ 뒤로") }; Text(title, fontSize = 23.sp, fontWeight = FontWeight.Bold) }
            content()
        }
    }
}
@Composable private fun FullButton(text: String, enabled: Boolean = true, action: () -> Unit) { Button(action, Modifier.fillMaxWidth().height(52.dp), enabled = enabled, shape = RoundedCornerShape(16.dp)) { Text(text, fontWeight = FontWeight.Bold) } }
@Composable private fun FlowWords(words: List<String>, action: (String) -> Unit) { Column { words.chunked(3).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { word -> OutlinedButton({ action(word) }) { Text(word) } } } } } }
@Composable private fun HintButton(id: String, current: Int, update: (Int) -> Unit, viewModel: GameViewModel, hints: List<String>) { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) { hints.take(current).forEachIndexed { index, text -> Card(colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = .12f))) { Text("힌트 ${index + 1}. $text", Modifier.fillMaxWidth().padding(12.dp)) } }; TextButton({ val next = (current + 1).coerceAtMost(2); update(next); viewModel.requestHint("$id.$next", hints[next - 1]) }, enabled = current < 2) { Text(if (current == 0) "힌트 1 공개" else if (current == 1) "힌트 2 공개" else "힌트 모두 확인함") } } }
