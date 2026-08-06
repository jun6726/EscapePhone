package com.example.escapephone.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * 인스타그램 그리드 스타일의 자유 드래그 재정렬 리스트.
 *
 * 드래그 중인 카드는 손가락을 자유롭게 따라 위아래 어디로든 이동하며(범위 제한 없음),
 * 다른 카드들은 드래그 중인 카드가 자신의 자리를 지나칠 때마다 실시간으로 애니메이션되며
 * 자리를 비켜준다. 손을 떼는 순간의 배치가 곧바로 최종 순서로 커밋된다.
 *
 * 리스트 전체가 하나의 상태(현재 순서 + 드래그 중인 아이템의 오프셋)를 소유해야 다른
 * 카드들의 실시간 밀림 계산이 가능하므로, 개별 행이 아니라 이 컨테이너가 배열을 직접
 * 들고 있는다. 각 행의 높이는 균일하다고 가정하고 첫 번째 행에서 측정한 높이를 전체
 * 리스트의 기준 높이로 사용한다.
 */
/// 상위 스크롤 컨테이너([ScreenColumn], [PuzzleColumn], [StoreScreenColumn] 등)가 자신을
/// 통해 자식 [ReorderableList]의 드래그 진행 여부를 전달받기 위한 통로. 드래그 중에는
/// 스크롤과 드래그 제스처가 동시에 이벤트를 나눠 받아 서로 간섭하는 것을 막기 위해 사용한다.
val LocalReorderDragReporter = compositionLocalOf<(Boolean) -> Unit> { {} }

/// 드래그 재정렬 관련 실험적 수정을 개별적으로 켜고 끄기 위한 플래그 모음.
/// 특정 기능이 오류의 원인인지 확인하기 위해 하나씩 되돌렸다가 다시 켤 수 있도록
/// 분리해 두었다. 문제가 재발하지 않는다고 확인되면 이 스위치들은 제거해도 된다.
object ReorderDragFeatureFlags {
    /// 손을 대는 즉시가 아니라 롱프레스(기본 타임아웃, 약 500ms)가 끝난 뒤에만
    /// 드래그가 시작되도록 한다. false면 손을 대는 즉시 드래그가 시작되는 이전 방식.
    const val useLongPressBeforeDrag = false
    /// 드래그가 진행 중인 동안 상위 스크롤 컨테이너의 스크롤을 잠근다.
    /// false면 드래그 중에도 스크롤이 계속 가능한 이전 방식으로 동작한다.
    const val disableScrollWhileDragging = true
}

@Composable
fun <T> ReorderableList(
    items: List<T>,
    itemId: (T) -> String,
    enabled: Boolean,
    onReorder: (List<T>) -> Unit,
    rowContent: @Composable (item: T, proxy: DragHandleProxy) -> Unit
) {
    val density = LocalDensity.current
    val reportDragging = LocalReorderDragReporter.current
    var rowHeightPx by remember { mutableFloatStateOf(with(density) { 72.dp.toPx() }) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragTranslationPx by remember { mutableFloatStateOf(0f) }
    var dragStartIndex by remember { mutableStateOf(0) }

    // 반올림 대신 한 칸을 완전히 넘어야 다음 슬롯으로 인정하는 절삭 방식을 쓴다.
    // 반올림(50% 경계)을 쓰면 카드를 반 칸만 옮기고 손을 뗐을 때 slots가 0으로
    // 계산되어 finalTarget == dragStartIndex가 되고, onDragEnded의 변경 조건을
    // 통과하지 못해 "분명히 옮겼는데 순서가 그대로"인 것처럼 보이는 원인이 된다.
    val targetIndex: Int? = if (draggingId != null) {
        val slots = if (dragTranslationPx >= 0) {
            (dragTranslationPx / rowHeightPx).toInt()
        } else {
            -((-dragTranslationPx) / rowHeightPx).toInt()
        }
        (dragStartIndex + slots).coerceIn(0, items.lastIndex.coerceAtLeast(0))
    } else null

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEachIndexed { index, item ->
            val id = itemId(item)
            // key(id)로 감싸지 않으면 Compose는 아이템 순서가 바뀔 때 화면상 같은
            // 위치(index)에 있던 슬롯을 새 아이템에게 그대로 재사용한다. 이때 그 자리의
            // pointerInput(proxy.enabled)는 key(enabled)가 그대로면 재시작되지 않아,
            // 이전에 실행 중이던 제스처 코루틴이 캡처한 옛 아이템의 id/index/proxy
            // 클로저를 계속 참조하게 되고, 그 결과 재정렬을 한 번 성공한 뒤로는 드래그가
            // 엉뚱한 아이템을 대상으로 판정되어 순서가 바뀌지 않는 것처럼 보인다.
            key(id) {
            val isDragging = id == draggingId
            val offsetPx = offsetFor(items, itemId, id, draggingId, dragStartIndex, targetIndex, dragTranslationPx, rowHeightPx)
            val animatedOffset by animateFloatAsState(
                targetValue = offsetPx,
                // 손을 뗀 뒤 자리를 잡을 때 iOS(.interactiveSpring dampingFraction 0.86,
                // 오버슈트 없이 부드럽게 안착)와 동일한 인상을 주도록 DampingRatioNoBouncy를
                // 쓴다. 기존 LowBouncy는 목표 위치를 지나쳤다 되돌아오는 튐이 있어
                // "튀어오르는 느낌"으로 느껴졌다.
                animationSpec = if (isDragging) spring(stiffness = Spring.StiffnessHigh) else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                label = "reorderOffset"
            )
            val scale by animateFloatAsState(if (isDragging) 1.04f else 1f, label = "reorderScale")
            val elevation by animateFloatAsState(if (isDragging) 14f else 0f, label = "reorderElevation")

            val proxy = DragHandleProxy(
                isDragging = isDragging,
                enabled = enabled,
                onDragChanged = { deltaY ->
                    if (draggingId == null) {
                        draggingId = id
                        dragStartIndex = index
                        dragTranslationPx = 0f
                        if (ReorderDragFeatureFlags.disableScrollWhileDragging) reportDragging(true)
                    }
                    if (draggingId == id) dragTranslationPx += deltaY
                },
                onDragEnded = {
                    if (draggingId == id) {
                        val finalTarget = targetIndex
                        if (finalTarget != null && finalTarget != dragStartIndex) {
                            val reordered = items.toMutableList()
                            val moved = reordered.removeAt(dragStartIndex)
                            reordered.add(finalTarget, moved)
                            onReorder(reordered)
                        }
                        draggingId = null
                        dragTranslationPx = 0f
                        if (ReorderDragFeatureFlags.disableScrollWhileDragging) reportDragging(false)
                    }
                }
            )

            Box(
                modifier = Modifier
                    // 드래그 중에는 스케일/그림자로 인해 측정치가 흔들릴 수 있으므로
                    // 재측정을 멈춰, targetIndex 계산의 기준 높이가 드래그 도중
                    // 바뀌어 슬롯 경계가 흔들리는 것을 막는다.
                    .onSizeChanged { if (index == 0 && draggingId == null) rowHeightPx = it.height.toFloat() + with(density) { 12.dp.toPx() } }
                    .offset { IntOffset(0, animatedOffset.roundToInt()) }
                    .scale(scale)
                    .shadow(elevation.dp, RoundedCornerShape(12.dp), clip = false)
                    .zIndex(if (isDragging) 1f else 0f)
            ) {
                rowContent(item, proxy)
            }
            }
        }
    }
}

/// 드래그 중인 카드는 손가락 이동량을 그대로 따라가고(자유도), 그 외 카드는 드래그 카드가
/// 자신을 지나쳤을 때만 한 칸만큼 밀린다.
private fun <T> offsetFor(
    items: List<T>,
    itemId: (T) -> String,
    id: String,
    draggingId: String?,
    dragStartIndex: Int,
    targetIndex: Int?,
    dragTranslationPx: Float,
    rowHeightPx: Float
): Float {
    if (draggingId == null || targetIndex == null) return 0f
    if (id == draggingId) return dragTranslationPx
    val itemIndex = items.indexOfFirst { itemId(it) == id }
    if (itemIndex < 0 || itemIndex == dragStartIndex) return 0f
    val low = minOf(dragStartIndex, targetIndex)
    val high = maxOf(dragStartIndex, targetIndex)
    if (itemIndex !in low..high) return 0f
    return if (targetIndex > dragStartIndex) -rowHeightPx else rowHeightPx
}

/** 각 행에 전달되는, 드래그 핸들이 리스트 상태와 통신하기 위한 콜백 묶음. */
class DragHandleProxy(
    val isDragging: Boolean,
    val enabled: Boolean,
    val onDragChanged: (Float) -> Unit,
    val onDragEnded: () -> Unit
)

/**
 * 인스타그램 그리드 스타일 드래그 핸들 UI. 롱프레스로 드래그를 강조 표시한 뒤에만
 * 실제 재정렬이 시작되도록 해, 스크롤 제스처와 드래그 제스처가 손가락을 대는 즉시
 * 동시에 인식을 시도하며 서로 이벤트를 나눠 받는 문제를 원천적으로 피한다. 지연 시간은
 * Compose 기본 롱프레스 타임아웃(ViewConfiguration.longPressTimeoutMillis, 약 500ms)을
 * 그대로 사용해 iOS의 LongPressGesture(minimumDuration: 0.5)와 동일하게 맞춘다.
 */
@Composable
fun DragReorderHandle(proxy: DragHandleProxy) {
    val accentColor = if (proxy.enabled) Primary else TextSecondary
    // pointerInput(key) { ... } 블록은 key가 바뀌지 않는 한 최초 실행 시점에 캡처한
    // 파라미터를 계속 재사용한다. proxy는 매 리컴포지션마다 새로 만들어지는데, key를
    // proxy.enabled(거의 안 바뀜)로만 주면 이 suspend 블록은 최초의 proxy(그 안의
    // onDragChanged/onDragEnded 클로저, 그리고 그 클로저가 캡처한 draggingId/targetIndex
    // 등 리컴포지션 시점 스냅샷)를 영원히 물고 있게 된다. 그 결과 onDragEnded가 호출하는
    // targetIndex는 항상 드래그 시작 순간(아직 draggingId==null이라 null)의 값으로
    // 고정되어, 손을 떼도 재정렬이 절대 커밋되지 않았다. rememberUpdatedState로 감싸면
    // pointerInput 코루틴 안에서도 항상 최신 proxy를 읽을 수 있다.
    val currentProxy by rememberUpdatedState(proxy)
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 52.dp)
            .semantics { contentDescription = "순서 이동 핸들" }
            .pointerInput(Unit) {
                if (ReorderDragFeatureFlags.useLongPressBeforeDrag) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { },
                        onDrag = { change, dragAmount ->
                            if (!currentProxy.enabled) return@detectDragGesturesAfterLongPress
                            change.consume()
                            if (dragAmount.y != 0f) currentProxy.onDragChanged(dragAmount.y)
                        },
                        onDragEnd = { currentProxy.onDragEnded() },
                        onDragCancel = { currentProxy.onDragEnded() }
                    )
                } else {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (!currentProxy.enabled) return@awaitEachGesture
                        var pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            val dy = change.positionChange().y
                            change.consume()
                            if (dy != 0f) currentProxy.onDragChanged(dy)
                            pointerId = change.id
                        }
                        currentProxy.onDragEnded()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        DragHandleBars(accentColor)
    }
}

@Composable
private fun DragHandleBars(color: Color) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.width(24.dp).height(4.dp).background(color, RoundedCornerShape(2.dp)))
        Box(Modifier.width(24.dp).height(4.dp).background(color, RoundedCornerShape(2.dp)))
        Box(Modifier.width(24.dp).height(4.dp).background(color, RoundedCornerShape(2.dp)))
    }
}

/** 드래그 핸들을 감싸는 컨트롤. 위/아래 스텝 버튼은 자유 드래그 도입으로 더 이상 필요 없어 제외되어 있다. */
@Composable
fun ReorderControls(proxy: DragHandleProxy) {
    DragReorderHandle(proxy)
}
