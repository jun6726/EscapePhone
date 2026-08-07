package com.example.escapephone.feature.themes.convenienceStore

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escapephone.app.ConvenienceStoreScreen
import com.example.escapephone.app.ConvenienceStoreUiState
import com.example.escapephone.app.ConvenienceStoreViewModel
import com.example.escapephone.app.puzzleId
import com.example.escapephone.core.model.ConvenienceStoreSpec
import com.example.escapephone.core.model.TiltControlMode
import com.example.escapephone.designsystem.Background
import com.example.escapephone.designsystem.HintSection
import com.example.escapephone.designsystem.LocalReorderDragReporter
import com.example.escapephone.designsystem.ReorderControls
import com.example.escapephone.designsystem.ReorderableList
import com.example.escapephone.designsystem.Success
import com.example.escapephone.designsystem.Surface
import com.example.escapephone.designsystem.TextPrimary
import com.example.escapephone.designsystem.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun ConvenienceStoreApp(viewModel: ConvenienceStoreViewModel, onExitToThemeMenu: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.startPuzzleSession(state.currentScreen.puzzleId) }
    BackHandler(enabled = true) {
        if (state.currentScreen == ConvenienceStoreScreen.intro || state.currentScreen == ConvenienceStoreScreen.home) onExitToThemeMenu()
        else viewModel.navigate(ConvenienceStoreScreen.home)
    }
    Surface(Modifier.fillMaxSize(), color = Background) {
        when (state.currentScreen) {
            ConvenienceStoreScreen.intro -> ConvenienceStoreIntroScreen(viewModel)
            ConvenienceStoreScreen.home -> ConvenienceStoreHomeScreen(state, viewModel, onExitToThemeMenu)
            ConvenienceStoreScreen.receipt -> ReceiptPuzzleScreen(viewModel)
            ConvenienceStoreScreen.barcode -> BarcodePuzzleScreen(viewModel)
            ConvenienceStoreScreen.shelfDifference -> ShelfDifferencePuzzleScreen(viewModel)
            ConvenienceStoreScreen.cctv -> CctvPuzzleScreen(viewModel)
            ConvenienceStoreScreen.inventory -> InventoryPuzzleScreen(viewModel)
            ConvenienceStoreScreen.customerPattern -> CustomerPatternPuzzleScreen(viewModel)
            ConvenienceStoreScreen.incidentTimeline -> IncidentTimelinePuzzleScreen(viewModel)
            ConvenienceStoreScreen.finalDecision -> ConvenienceStoreFinalDecisionScreen(viewModel)
            ConvenienceStoreScreen.ending -> ConvenienceStoreEndingScreen(state, viewModel, onExitToThemeMenu)
        }
    }
}

private fun syncCctvOrder(viewModel: ConvenienceStoreViewModel, targetOrder: List<String>) {
    if (viewModel.cctvEngine.recordOrder == targetOrder) return
    val maxIterations = targetOrder.size * targetOrder.size
    var iterations = 0
    while (viewModel.cctvEngine.recordOrder != targetOrder && iterations < maxIterations) {
        iterations++
        val liveOrder = viewModel.cctvEngine.recordOrder
        val mismatchIndex = liveOrder.indices.firstOrNull { liveOrder[it] != targetOrder[it] } ?: break
        val targetId = targetOrder[mismatchIndex]
        val currentIndex = liveOrder.indexOf(targetId)
        if (currentIndex < 0) break
        if (currentIndex > mismatchIndex) viewModel.cctvEngine.moveCctvRecordUp(currentIndex) else viewModel.cctvEngine.moveCctvRecordDown(currentIndex)
    }
}

private fun syncIncidentOrder(viewModel: ConvenienceStoreViewModel, targetOrder: List<String>) {
    if (viewModel.incidentTimelineEngine.eventOrder == targetOrder) return
    val maxIterations = targetOrder.size * targetOrder.size
    var iterations = 0
    while (viewModel.incidentTimelineEngine.eventOrder != targetOrder && iterations < maxIterations) {
        iterations++
        val liveOrder = viewModel.incidentTimelineEngine.eventOrder
        val mismatchIndex = liveOrder.indices.firstOrNull { liveOrder[it] != targetOrder[it] } ?: break
        val targetId = targetOrder[mismatchIndex]
        val currentIndex = liveOrder.indexOf(targetId)
        if (currentIndex < 0) break
        val direction = if (currentIndex > mismatchIndex) -1 else 1
        viewModel.incidentTimelineEngine.moveIncidentEvent(targetId, (currentIndex + direction).coerceIn(0, viewModel.incidentTimelineEngine.eventOrder.lastIndex))
    }
}

@Composable
fun StoreScreenColumn(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
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
            .onSizeChanged { contentHeightPx = it.height }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("←", color = StoreAccent, fontSize = 20.sp) }
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
    }
}

@Composable
fun ConvenienceStoreIntroScreen(viewModel: ConvenienceStoreViewModel) {
    val lines = listOf(
        "지인의 부탁으로 새벽 편의점 근무를 대신 맡았다.",
        "오전 2시 17분, 매장 전원이 꺼지고 다시 켜지자 시계는 오전 2시로 돌아가 있었다.",
        "같은 17분이 반복된다. 하지만 매번 조금씩 달라진다."
    )
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { for (index in lines.indices) { delay(650); shown = index + 1 } }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("02:17", color = StoreAccent, fontFamily = FontFamily.Monospace, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        lines.forEachIndexed { index, text -> AnimatedVisibility(index < shown, enter = fadeIn()) { Text(text, fontSize = 20.sp, color = TextPrimary, modifier = Modifier.padding(vertical = 10.dp)) } }
        Spacer(Modifier.weight(1f))
        Button(onClick = viewModel::completeIntro, colors = ButtonDefaults.buttonColors(containerColor = StoreAccent)) { Text(if (shown == lines.size) "관리 태블릿 열기" else "건너뛰기") }
    }
}

private data class StoreApp(val id: String, val title: String, val icon: String, val unlocked: Boolean, val reason: String, val screen: ConvenienceStoreScreen)

@Composable
fun ConvenienceStoreHomeScreen(state: ConvenienceStoreUiState, viewModel: ConvenienceStoreViewModel, onExitToThemeMenu: () -> Unit) {
    val progress = state.progress
    val apps = listOf(
        StoreApp("pos", "POS", "🧾", true, "", ConvenienceStoreScreen.receipt),
        StoreApp("message", "점장 메시지", "💬", true, "", ConvenienceStoreScreen.home),
        StoreApp("product", "상품 조회", "📦", progress.receiptSolved, "영수증 이상 항목을 먼저 찾으세요.", ConvenienceStoreScreen.barcode),
        StoreApp("shelf", "진열대 기록", "🗄️", progress.barcodeSolved, "바코드 규칙을 먼저 복원하세요.", ConvenienceStoreScreen.shelfDifference),
        StoreApp("cctv", "CCTV", "🎥", progress.shelfDifferenceSolved, "진열대 차이를 먼저 확인하세요.", ConvenienceStoreScreen.cctv),
        StoreApp("inventory", "재고 관리", "📊", progress.cctvSolved, "CCTV 순서를 먼저 복원하세요.", ConvenienceStoreScreen.inventory),
        StoreApp("customer", "고객 기록", "🧍", progress.inventorySolved, "재고 불일치를 먼저 찾으세요.", ConvenienceStoreScreen.customerPattern),
        StoreApp("incident", "사건 기록", "🗂️", progress.customerPatternSolved, "구매 패턴을 먼저 해독하세요.", ConvenienceStoreScreen.incidentTimeline)
    )
    StoreScreenColumn("야간 관리 태블릿 // ${progress.currentStage.name}", onBack = onExitToThemeMenu) {
        LazyVerticalGrid(GridCells.Fixed(2), modifier = Modifier.fillMaxWidth().height(700.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(apps) { app ->
                Card(
                    Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(14.dp)).clickable(enabled = app.unlocked) { viewModel.navigate(app.screen) },
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(app.icon, fontSize = 30.sp)
                        Text(app.title, color = if (app.unlocked) TextPrimary else TextSecondary)
                        if (!app.unlocked) Text("🔒 ${app.reason}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Text("발견한 증거 ${progress.collectedEvidenceIds.size}개 · 코드 ${progress.discoveredCodes.size}개", color = TextSecondary)
    }
}

@Composable
fun ReceiptPuzzleScreen(viewModel: ConvenienceStoreViewModel) {
    var selected by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<Boolean?>(null) }
    StoreScreenColumn("영수증과 가격표 비교", onBack = { viewModel.navigate(ConvenienceStoreScreen.home) }) {
        Text("POS에 남은 영수증과 진열대 가격표를 비교해 잘못 계산된 상품을 찾으세요.", color = TextSecondary)
        Spacer(Modifier.height(12.dp))
        ConvenienceStoreSpec.receiptItems.forEach { item ->
            Card(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selected = item.id; viewModel.receiptEngine.selectReceiptItem(item.id) },
                colors = CardDefaults.cardColors(containerColor = if (selected == item.id) StoreAccent.copy(alpha = .25f) else Surface)
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${item.name} (${item.code})", color = TextPrimary)
                    Text("${item.price}원", color = StorePos, fontFamily = FontFamily.Monospace)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (viewModel.receiptEngine.isSolved || viewModel.uiState.collectAsState().value.progress.receiptSolved) {
            Text("✓ 이상 코드 0217 발견 · 상품 조회 잠금 해제", color = Success)
            Button(onClick = { viewModel.completeReceiptPuzzle(); viewModel.navigate(ConvenienceStoreScreen.home) }, colors = ButtonDefaults.buttonColors(containerColor = StoreAccent)) { Text("홈으로") }
        } else {
            Button(onClick = { result = viewModel.receiptEngine.submitReceiptAnswer(); if (result == true) viewModel.completeReceiptPuzzle() else viewModel.recordWrongAttempt("receipt_price", "receiptAnomalyIncorrect") }, colors = ButtonDefaults.buttonColors(containerColor = StoreAccent)) { Text("확인") }
            Text("일치하지 않습니다. 다시 확인하세요.", color = StoreWarning, modifier = Modifier.alpha(if (result == false) 1f else 0f))
            val hints = listOf("가격표와 영수증의 금액이 모두 같은지 확인하세요.", "실제 상품 목록에 존재하지 않는 네 자리 코드를 찾아보세요.")
            HintSection(hints) { level -> viewModel.requestHint("receipt_price.$level", hints[level - 1]) }
        }
    }
}

@Composable
fun BarcodePuzzleScreen(viewModel: ConvenienceStoreViewModel) {
    var category by remember { mutableStateOf("") }
    var shelfPosition by remember { mutableStateOf("") }
    var arrivalOrder by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Boolean?>(null) }
    StoreScreenColumn("바코드 규칙 복원", onBack = { viewModel.navigate(ConvenienceStoreScreen.home) }) {
        Text("바코드는 [분류][위치][입고순서] 형식입니다. 세 상품을 비교해 숨겨진 코드를 조합하세요.", color = TextSecondary)
        Spacer(Modifier.height(12.dp))
        ConvenienceStoreSpec.barcodeItems.forEach { item -> Text("${item.id}: ${item.category}-${item.shelfPosition}-${item.arrivalOrder}", color = TextPrimary, fontFamily = FontFamily.Monospace) }
        Spacer(Modifier.height(16.dp))
        listOf("99" to "분류", "40" to "위치", "01" to "입고순서").forEach { (value, label) ->
            Button(onClick = {
                when (label) { "분류" -> { category = value; viewModel.barcodeEngine.selectBarcodeSegment("category", value) }; "위치" -> { shelfPosition = value; viewModel.barcodeEngine.selectBarcodeSegment("shelfPosition", value) }; else -> { arrivalOrder = value; viewModel.barcodeEngine.selectBarcodeSegment("arrivalOrder", value) } }
            }, colors = ButtonDefaults.buttonColors(containerColor = Surface)) { Text("$label = $value") }
        }
        Text("조합: $category-$shelfPosition-$arrivalOrder", color = StorePos, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(16.dp))
        if (viewModel.barcodeEngine.isSolved) {
            Text("✓ 숨겨진 위치 발견: ${ConvenienceStoreSpec.hiddenLocation} · 진열대 기록 잠금 해제", color = Success)
            Button(onClick = { viewModel.completeBarcodePuzzle(); viewModel.navigate(ConvenienceStoreScreen.home) }, colors = ButtonDefaults.buttonColors(containerColor = StoreAccent)) { Text("홈으로") }
        } else {
            Button(onClick = { result = viewModel.barcodeEngine.submitBarcodeRule(); if (result == true) viewModel.completeBarcodePuzzle() else viewModel.recordWrongAttempt("barcode_rule", "barcodeSegmentsIncorrect") }, colors = ButtonDefaults.buttonColors(containerColor = StoreAccent)) { Text("확인") }
            Text("규칙이 맞지 않습니다.", color = StoreWarning, modifier = Modifier.alpha(if (result == false) 1f else 0f))
            val hints = listOf("바코드 숫자를 한 번에 읽지 말고 구간별로 나누어 보세요.", "상품 분류, 진열 위치, 입고 순서가 각각 다른 구간에 들어 있습니다.")
            HintSection(hints) { level -> viewModel.requestHint("barcode_rule.$level", hints[level - 1]) }
        }
    }
}

@Composable
fun ShelfDifferencePuzzleScreen(viewModel: ConvenienceStoreViewModel) {
    var touchMode by remember { mutableStateOf(true) }
    var revealMessage by remember { mutableStateOf(false) }
    val differenceLabels = mapOf(
        "drink_position" to "음료 한 병의 위치",
        "umbrella_count" to "우산 개수",
        "price_tag_direction" to "가격표 방향",
        "empty_slot" to "빈 상품 칸",
        "fridge_pouch" to "냉장고 하단의 작은 봉투"
    )
    StoreScreenColumn("반복 전후 진열대 차이", onBack = { viewModel.navigate(ConvenienceStoreScreen.home) }) {
        Text("같은 시각 두 기록을 비교해 차이를 모두 선택하세요.", color = TextSecondary)
        Spacer(Modifier.height(12.dp))
        differenceLabels.forEach { (id, label) ->
            val selected = id in viewModel.shelfDifferenceEngine.selectedDifferenceIds
            Card(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.shelfDifferenceEngine.selectShelfDifference(id); if (id == ConvenienceStoreSpec.finalShelfDifferenceId) revealMessage = true },
                colors = CardDefaults.cardColors(containerColor = if (selected) StoreAccent.copy(alpha = .25f) else Surface)
            ) { Text(label, Modifier.padding(14.dp), color = TextPrimary) }
        }
        if (revealMessage) Text("봉투 속 문구: \"${ConvenienceStoreSpec.pouchMessage}\"", color = StorePos, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(12.dp))
        Text("조작 방식: ${if (touchMode) "터치" else "기울기"}", color = TextSecondary)
        TextButton(onClick = { touchMode = !touchMode }) { Text("조작 방식 전환") }
        Spacer(Modifier.height(16.dp))
        if (viewModel.shelfDifferenceEngine.isSolved) {
            Text("✓ 모든 차이 발견 · CCTV 잠금 해제", color = Success)
            Button(onClick = { viewModel.completeShelfDifferencePuzzle(); viewModel.navigate(ConvenienceStoreScreen.home) }, colors = ButtonDefaults.buttonColors(containerColor = StoreAccent)) { Text("홈으로") }
        } else {
            Button(onClick = { if (!viewModel.shelfDifferenceEngine.submitShelfDifferences()) viewModel.recordWrongAttempt("shelf_difference", "shelfDifferencesIncomplete") }, colors = ButtonDefaults.buttonColors(containerColor = StoreAccent)) { Text("확인") }
            val hints = listOf("상품 개수뿐 아니라 가격표와 빈 공간도 비교하세요.", "냉장고 아래쪽에 이전 기록에는 없던 물체가 있습니다.")
            HintSection(hints) { level -> viewModel.requestHint("shelf_difference.$level", hints[level - 1]) }
        }
    }
}

@Composable
fun CctvPuzzleScreen(viewModel: ConvenienceStoreViewModel) {
    var order by remember { mutableStateOf(viewModel.cctvEngine.recordOrder) }
    var result by remember { mutableStateOf<Boolean?>(null) }
    StoreScreenColumn("CCTV 시간 순서 복원", onBack = { viewModel.navigate(ConvenienceStoreScreen.home) }) {
        Text("기록을 올바른 시간 순서로 정렬하세요.", color = TextSecondary)
        Spacer(Modifier.height(12.dp))
        val orderedRecords = order.mapNotNull { id -> ConvenienceStoreSpec.cctvRecords.firstOrNull { it.id == id } }
        ReorderableList(
            items = orderedRecords,
            itemId = { it.id },
            enabled = !viewModel.cctvEngine.isSolved,
            onReorder = { reordered ->
                syncCctvOrder(viewModel, reordered.map { it.id })
                order = viewModel.cctvEngine.recordOrder
            }
        ) { record, proxy ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(record.description, color = TextPrimary, modifier = Modifier.weight(1f))
                    ReorderControls(proxy)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (viewModel.cctvEngine.isSolved) {
            Text("✓ 순서 복원 완료 · CENTRAL-7 흔적 발견 · 재고 관리 잠금 해제", color = Success)
            Button(onClick = { viewModel.completeCctvPuzzle(); viewModel.navigate(ConvenienceStoreScreen.home) }, colors = ButtonDefaults.buttonColors(containerColor = StoreAccent)) { Text("홈으로") }
        } else {
            Button(onClick = { result = viewModel.cctvEngine.submitCctvSequence(); if (result == true) viewModel.completeCctvPuzzle() else viewModel.recordWrongAttempt("cctv_sequence", "cctvOrderIncorrect") }, colors = ButtonDefaults.buttonColors(containerColor = StoreAccent)) { Text("확인") }
            Text("순서가 맞지 않습니다.", color = StoreWarning, modifier = Modifier.alpha(if (result == false) 1f else 0f))
            val hints = listOf("각 화면의 매장 시계만으로는 순서를 확정할 수 없습니다.", "전자레인지와 POS 영수증 출력 상태를 함께 비교하세요.")
            HintSection(hints) { level -> viewModel.requestHint("cctv_sequence.$level", hints[level - 1]) }
        }
    }
}

@Composable
fun InventoryPuzzleScreen(viewModel: ConvenienceStoreViewModel) {
    var selected by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<Boolean?>(null) }
    StoreScreenColumn("POS 판매기록과 재고 교차검증", onBack = { viewModel.navigate(ConvenienceStoreScreen.home) }) {
        Text("현재 재고 = 이전 재고 + 입고 - 판매. 계산이 맞지 않는 상품을 찾으세요.", color = TextSecondary)
        Spacer(Modifier.height(12.dp))
        ConvenienceStoreSpec.inventoryItems.forEach { item ->
            val expected = viewModel.inventoryEngine.calculateExpectedInventory(item.id)
            Card(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selected = item.id; viewModel.inventoryEngine.selectInventoryItem(item.id) },
                colors = CardDefaults.cardColors(containerColor = if (selected == item.id) StoreAccent.copy(alpha = .25f) else Surface)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(item.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("예상 재고 $expected · 실제 재고 ${item.actualStock}", color = if (expected != item.actualStock) StoreWarning else TextSecondary)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (viewModel.inventoryEngine.isSolved) {
            Text("✓ 불일치 상품 발견 · 고객 기록 잠금 해제", color = Success)
            Button(onClick = { viewModel.completeInventoryPuzzle(); viewModel.navigate(ConvenienceStoreScreen.home) }, colors = ButtonDefaults.buttonColors(containerColor = StoreAccent)) { Text("홈으로") }
        } else {
            Button(onClick = { result = viewModel.inventoryEngine.submitInventoryDiscrepancy(); if (result == true) viewModel.completeInventoryPuzzle() else viewModel.recordWrongAttempt("inventory_crosscheck", "inventoryItemIncorrect") }, colors = ButtonDefaults.buttonColors(containerColor = StoreAccent)) { Text("확인") }
            Text("이 상품은 정상입니다.", color = StoreWarning, modifier = Modifier.alpha(if (result == false) 1f else 0f))
            val hints = listOf("현재 재고는 이전 재고에 입고를 더하고 판매를 빼서 계산합니다.", "계산 결과와 실제 수량이 1개 차이 나는 상품을 찾으세요.")
            HintSection(hints) { level -> viewModel.requestHint("inventory_crosscheck.$level", hints[level - 1]) }
        }
    }
}

@Composable
fun CustomerPatternPuzzleScreen(viewModel: ConvenienceStoreViewModel) {
    var candidate by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Boolean?>(null) }
    StoreScreenColumn("손님의 구매 패턴", onBack = { viewModel.navigate(ConvenienceStoreScreen.home) }) {
        Text("반복마다 동일 손님이 다른 상품을 구매합니다. 패턴을 해독해 메시지를 복원하세요.", color = TextSecondary)
        Spacer(Modifier.height(12.dp))
        ConvenienceStoreSpec.customerPurchases.forEach { purchase -> Text("${purchase.cycle}회차: ${purchase.items.joinToString(", ")}", color = TextPrimary) }
        Spacer(Modifier.height(16.dp))
        if (viewModel.customerPatternEngine.isSolved) {
            Text("✓ 메시지 복원: \"${viewModel.customerPatternEngine.decodedMessage}\" · 사건 기록 잠금 해제", color = Success)
            Button(onClick = { viewModel.completeCustomerPatternPuzzle(); viewModel.navigate(ConvenienceStoreScreen.home) }, colors = ButtonDefaults.buttonColors(containerColor = StoreAccent)) { Text("홈으로") }
        } else {
            Button(onClick = { candidate = viewModel.customerPatternEngine.decodePurchasePattern(); result = viewModel.customerPatternEngine.submitCustomerPattern(candidate); if (result == true) viewModel.completeCustomerPatternPuzzle() else viewModel.recordWrongAttempt("customer_pattern", "customerPatternIncorrect") }, colors = ButtonDefaults.buttonColors(containerColor = StoreAccent)) { Text("패턴 해독") }
            val hints = listOf("손님이 산 상품보다 상품이 놓인 위치에 주목하세요.", "각 반복에서 구매한 상품을 진열 순서로 변환하세요.")
            HintSection(hints) { level -> viewModel.requestHint("customer_pattern.$level", hints[level - 1]) }
        }
    }
}

@Composable
fun IncidentTimelinePuzzleScreen(viewModel: ConvenienceStoreViewModel) {
    var order by remember { mutableStateOf(viewModel.incidentTimelineEngine.eventOrder) }
    var result by remember { mutableStateOf<Boolean?>(null) }
    StoreScreenColumn("실종 당일 타임라인", onBack = { viewModel.navigate(ConvenienceStoreScreen.home) }) {
        Text("사건 카드를 시간순으로 배치하고 결론을 모두 확인하세요.", color = TextSecondary)
        Spacer(Modifier.height(12.dp))
        val orderedEvents = order.mapNotNull { id -> ConvenienceStoreSpec.incidentEvents.firstOrNull { it.id == id } }
        ReorderableList(
            items = orderedEvents,
            itemId = { it.id },
            enabled = !viewModel.incidentTimelineEngine.isSolved,
            onReorder = { reordered ->
                syncIncidentOrder(viewModel, reordered.map { it.id })
                order = viewModel.incidentTimelineEngine.eventOrder
            }
        ) { event, proxy ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(event.description, color = TextPrimary, modifier = Modifier.weight(1f))
                    ReorderControls(proxy)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { ConvenienceStoreSpec.incidentEvents.forEach { viewModel.incidentTimelineEngine.selectIncidentConclusion(it.id) } }, colors = ButtonDefaults.buttonColors(containerColor = Surface)) { Text("모든 결론 확인") }
        Spacer(Modifier.height(16.dp))
        if (viewModel.incidentTimelineEngine.isSolved) {
            Text("✓ 타임라인 복원 완료 · 최종 선택 잠금 해제", color = Success)
            Button(onClick = { viewModel.completeIncidentTimelinePuzzle() }, colors = ButtonDefaults.buttonColors(containerColor = StoreAccent)) { Text("계속") }
        } else {
            Button(onClick = { result = viewModel.incidentTimelineEngine.validateIncidentTimeline(); if (result == true) viewModel.completeIncidentTimelinePuzzle() else viewModel.recordWrongAttempt("incident_timeline", "incidentTimelineIncorrect") }, colors = ButtonDefaults.buttonColors(containerColor = StoreAccent)) { Text("확인") }
            Text("사건 순서가 맞지 않습니다.", color = StoreWarning, modifier = Modifier.alpha(if (result == false) 1f else 0f))
            val hints = listOf("기록 시간과 실제 사건 시간이 조작되었을 가능성을 고려하세요.", "CENTRAL-7 접속 이후 변경된 기록을 별도로 분리하세요.")
            HintSection(hints) { level -> viewModel.requestHint("incident_timeline.$level", hints[level - 1]) }
        }
    }
}

@Composable
fun ConvenienceStoreFinalDecisionScreen(viewModel: ConvenienceStoreViewModel) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("최종 결정", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(24.dp))
        Button(onClick = viewModel::selectPublicDisclosure, colors = ButtonDefaults.buttonColors(containerColor = StoreAccent), modifier = Modifier.fillMaxWidth()) { Text("복원한 기록을 외부 감사 기관으로 전송한다") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = viewModel::selectEncryptedArchive, colors = ButtonDefaults.buttonColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) { Text("불완전한 증거를 암호화해 보관한다") }
    }
}

@Composable
fun ConvenienceStoreEndingScreen(state: ConvenienceStoreUiState, viewModel: ConvenienceStoreViewModel, onExitToThemeMenu: () -> Unit) {
    val progress = state.progress
    val message = if (progress.endingType?.name == "publicDisclosure") "반복된 것은 시간이 아니라, 누군가 지우려 했던 17분의 기록이었다." else "마지막 기록은 아직 화면 밖에 남아 있다."
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("서버 복구 완료", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = StorePos)
        Spacer(Modifier.height(16.dp))
        Text(message, color = TextPrimary, fontSize = 18.sp)
        Spacer(Modifier.height(24.dp))
        Text("힌트 사용 ${progress.hintLevels.size}회 · 증거 ${progress.collectedEvidenceIds.size}개 · 조작 방식 ${progress.controlMode.name}", color = TextSecondary)
        Spacer(Modifier.height(24.dp))
        Button(onClick = viewModel::startNewGame, colors = ButtonDefaults.buttonColors(containerColor = StoreAccent)) { Text("다시 플레이") }
        TextButton(onClick = onExitToThemeMenu) { Text("테마 선택으로 돌아가기") }
    }
}
