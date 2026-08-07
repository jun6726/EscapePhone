import SwiftUI

enum AppTheme {
    static let background = Color(red: 0.025, green: 0.045, blue: 0.09)
    static let card = Color(red: 0.10, green: 0.14, blue: 0.19)
    static let cardElevated = Color(red: 0.13, green: 0.18, blue: 0.24)
    static let accent = Color(red: 0.22, green: 0.88, blue: 0.72)
    static let warning = Color(red: 1.0, green: 0.55, blue: 0.35)
    static let textPrimary = Color(red: 0.957, green: 0.969, blue: 0.980)
    static let textSecondary = Color(red: 0.667, green: 0.714, blue: 0.761)
    static let hairline = Color.white.opacity(0.08)
}

struct SecondaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .frame(maxWidth: .infinity)
            .padding()
            .background(AppTheme.cardElevated.opacity(configuration.isPressed ? 0.7 : 1))
            .foregroundStyle(AppTheme.textPrimary)
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(AppTheme.hairline))
    }
}

struct GhostButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.subheadline.weight(.medium))
            .foregroundStyle(configuration.isPressed ? AppTheme.textPrimary : AppTheme.textSecondary)
    }
}

struct BackButton: View {
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: "chevron.left")
                Text("테마 선택으로")
            }
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(AppTheme.textPrimary)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(AppTheme.cardElevated)
            .clipShape(Capsule())
            .overlay(Capsule().stroke(AppTheme.hairline))
        }
    }
}

struct PrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label.font(.headline).frame(maxWidth: .infinity).padding().background(AppTheme.accent.opacity(configuration.isPressed ? 0.65 : 1)).foregroundStyle(.black).clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

struct MildDestructiveButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label.font(.subheadline.weight(.medium)).frame(maxWidth: .infinity).padding(.vertical, 12).background(AppTheme.cardElevated.opacity(configuration.isPressed ? 0.7 : 1)).foregroundStyle(AppTheme.warning).clipShape(RoundedRectangle(cornerRadius: 14)).overlay(RoundedRectangle(cornerRadius: 14).stroke(AppTheme.warning.opacity(0.4)))
    }
}

struct StrongDestructiveButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label.font(.subheadline.weight(.semibold)).frame(maxWidth: .infinity).padding(.vertical, 12).background(Color.red.opacity(configuration.isPressed ? 0.65 : 0.85)).foregroundStyle(.white).clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

struct CardModifier: ViewModifier {
    func body(content: Content) -> some View { content.padding().background(AppTheme.card).clipShape(RoundedRectangle(cornerRadius: 18)).overlay(RoundedRectangle(cornerRadius: 18).stroke(.white.opacity(0.08))) }
}
extension View { func gameCard() -> some View { modifier(CardModifier()) } }

/// 문제별 힌트 보기 UI. 팝업(sheet/alert) 대신 현재 화면 안에 인라인으로 힌트를
/// 쌓아 보여주고, "N/최대" 형태로 몇 개를 이미 봤는지 항상 표시한다. 힌트 상태는
/// 이 뷰를 소유한 화면의 `@State`이므로, 뒤로가기나 홈 이동으로 화면이 사라졌다가
/// 다시 생성되면 자동으로 0으로 리셋된다 — 별도 초기화 로직이 필요 없다.
struct HintSection: View {
    let hints: [String]
    let onReveal: (_ level: Int) -> Void
    @State private var level = 0

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button {
                level = min(level + 1, hints.count)
                onReveal(level)
            } label: {
                HStack(spacing: 4) {
                    Image(systemName: "lightbulb.fill")
                    Text(level >= hints.count ? "힌트 모두 확인함 (\(level)/\(hints.count))" : "힌트 보기 (\(level)/\(hints.count))")
                }
                .font(.footnote)
                .foregroundStyle(AppTheme.textSecondary)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .disabled(level >= hints.count)
            ForEach(Array(hints.prefix(level).enumerated()), id: \.offset) { index, hint in
                // 본문 콘텐츠(gameCard)와 겹치지 않도록 아이콘 + 톤 다운 배경 +
                // 점선 테두리로 "이건 부가 정보"라는 인상을 명확히 준다.
                HStack(alignment: .top, spacing: 8) {
                    Image(systemName: "lightbulb").foregroundStyle(AppTheme.accent.opacity(0.8)).font(.footnote)
                    Text("힌트 \(index + 1). \(hint)")
                        .font(.footnote)
                        .foregroundStyle(AppTheme.textSecondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(10)
                .background(AppTheme.background.opacity(0.5))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(AppTheme.hairline, style: StrokeStyle(lineWidth: 1, dash: [4, 3])))
            }
        }
    }
}

/// 드래그 재정렬 관련 실험적 수정을 개별적으로 켜고 끄기 위한 플래그 모음.
/// 특정 기능이 오류의 원인인지 확인하기 위해 하나씩 되돌렸다가 다시 켤 수 있도록
/// 분리해 두었다. 문제가 재발하지 않는다고 확인되면 이 스위치들은 제거해도 된다.
enum ReorderDragFeatureFlags {
    /// 절대 translation 대신 매 프레임 이전 위치와의 상대 delta만 누적해서 반영한다.
    /// false면 DragGesture의 절대 translation을 그대로 사용하는 이전 방식으로 동작한다.
    static let useRelativeDelta = false
    /// 손을 대는 즉시가 아니라 0.5초 롱프레스가 끝난 뒤에만 드래그가 시작되도록 한다.
    /// false면 손을 대는 즉시(최소 이동 거리만 넘으면) 드래그가 시작되는 이전 방식으로 동작한다.
    static let useLongPressBeforeDrag = false
    /// 드래그가 진행 중인 동안 상위 ScrollView의 스크롤을 잠근다.
    /// false면 드래그 중에도 스크롤이 계속 가능한 이전 방식으로 동작한다.
    static let disableScrollWhileDragging = true
}

/// `ReorderableList`가 드래그 진행 여부를 감싸는 스크롤 컨테이너까지 전달하기 위한 PreferenceKey.
/// 드래그 중에는 스크롤과 드래그 제스처가 동시에 이벤트를 받아 서로 간섭하는 것을 막기 위해,
/// 이 값이 true인 동안 상위 ScrollView의 스크롤을 비활성화한다.
struct ReorderDraggingPreferenceKey: PreferenceKey {
    static var defaultValue: Bool = false
    static func reduce(value: inout Bool, nextValue: () -> Bool) { value = value || nextValue() }
}

/// 인스타그램 그리드 스타일의 자유 드래그 재정렬 리스트.
///
/// 드래그 중인 카드는 손가락을 자유롭게 따라 위아래 어디로든 이동하며(범위 제한 없음),
/// 다른 카드들은 드래그 중인 카드가 자신의 자리를 지나칠 때마다 실시간으로 애니메이션되며
/// 자리를 비켜준다. 손을 떼는 순간의 배치가 곧바로 최종 순서로 커밋된다.
///
/// 리스트 전체가 하나의 상태(현재 순서 + 드래그 중인 아이템의 오프셋)를 소유해야 다른
/// 카드들의 실시간 밀림 계산이 가능하므로, 개별 행이 아니라 이 컨테이너가 배열을 직접
/// 들고 있는다. 각 행의 높이는 균일하다고 가정하고 첫 번째 행에서 측정한 높이를 전체
/// 리스트의 기준 높이로 사용한다.
struct ReorderableList<Item: Identifiable, RowContent: View>: View {
    @Binding var items: [Item]
    let enabled: Bool
    @ViewBuilder let rowContent: (Item, DragHandleProxy) -> RowContent

    @State private var rowHeight: CGFloat = 60
    @State private var draggingItemID: Item.ID?
    @State private var dragTranslation: CGFloat = 0
    @State private var dragStartIndex: Int = 0

    /// 드래그 중인 아이템이 현재 이동한 목표 인덱스(원래 인덱스 + 이동한 슬롯 수).
    /// 슬롯 경계에서 손가락이 미세하게 흔들려도 자리바꿈이 반복 트리거되지 않도록,
    /// 반올림 대신 한 슬롯을 완전히 넘어야 다음 슬롯으로 넘어가는 히스테리시스를 둔다.
    private var targetIndex: Int? {
        guard draggingItemID != nil else { return nil }
        let slots: Int
        if dragTranslation >= 0 {
            slots = Int(dragTranslation / rowHeight)
        } else {
            slots = -Int((-dragTranslation) / rowHeight)
        }
        return (dragStartIndex + slots).clamped(to: 0...(items.count - 1))
    }

    var body: some View {
        VStack(spacing: 12) {
            ForEach(items) { item in
                let isDragging = item.id == draggingItemID
                let displayOffset = offset(for: item)
                rowContent(item, DragHandleProxy(
                    isDragging: isDragging,
                    onDragChanged: { delta in
                        guard enabled else { return }
                        if draggingItemID == nil {
                            draggingItemID = item.id
                            dragStartIndex = items.firstIndex(where: { $0.id == item.id }) ?? 0
                            dragTranslation = 0
                        }
                        guard draggingItemID == item.id else { return }
                        dragTranslation += delta
                    },
                    onDragEnded: {
                        guard draggingItemID == item.id else { return }
                        if let target = targetIndex, let currentIndex = items.firstIndex(where: { $0.id == item.id }), target != currentIndex {
                            withAnimation(.interactiveSpring(response: 0.35, dampingFraction: 0.86)) {
                                let moved = items.remove(at: currentIndex)
                                items.insert(moved, at: target)
                            }
                        }
                        withAnimation(.easeOut(duration: 0.2)) {
                            draggingItemID = nil
                            dragTranslation = 0
                        }
                    }
                ))
                .background(
                    GeometryReader { proxy in
                        Color.clear
                            .onAppear { if rowHeight == 60 { rowHeight = proxy.size.height + 12 } }
                    }
                )
                .offset(y: displayOffset)
                .scaleEffect(isDragging ? 1.04 : 1.0)
                .shadow(color: .black.opacity(isDragging ? 0.4 : 0), radius: isDragging ? 14 : 0, y: isDragging ? 6 : 0)
                .zIndex(isDragging ? 1 : 0)
                .animation(isDragging ? nil : .interactiveSpring(response: 0.35, dampingFraction: 0.8), value: displayOffset)
            }
        }
        .preference(key: ReorderDraggingPreferenceKey.self, value: draggingItemID != nil)
    }

    /// 드래그 중인 카드는 손가락 이동량을 그대로 따라가고(자유도),
    /// 그 외 카드는 드래그 카드가 자신을 지나쳤을 때만 한 칸만큼 밀린다.
    private func offset(for item: Item) -> CGFloat {
        guard let draggingItemID, let target = targetIndex else { return 0 }
        if item.id == draggingItemID { return dragTranslation }
        guard let itemIndex = items.firstIndex(where: { $0.id == item.id }) else { return 0 }
        let low = min(dragStartIndex, target)
        let high = max(dragStartIndex, target)
        guard (low...high).contains(itemIndex), itemIndex != dragStartIndex else { return 0 }
        return target > dragStartIndex ? -rowHeight : rowHeight
    }
}

/// 각 행에 전달되는, 드래그 핸들이 리스트 상태와 통신하기 위한 콜백 묶음.
struct DragHandleProxy {
    let isDragging: Bool
    let onDragChanged: (CGFloat) -> Void
    let onDragEnded: () -> Void
}

/**
 * 인스타그램 그리드 스타일 드래그 핸들 UI. 롱프레스(0.5초, Android의
 * ViewConfiguration.longPressTimeoutMillis 기본값과 동일)로 드래그가 강조 표시된
 * 뒤에만 실제 재정렬이 시작되도록 해, 스크롤 제스처와 드래그 제스처가 손가락을
 * 대는 즉시 동시에 인식을 시도하며 서로 이벤트를 나눠 받는 문제를 원천적으로 피한다.
 */
struct DragReorderHandle: View {
    let enabled: Bool
    let proxy: DragHandleProxy
    @State private var lastLocationY: CGFloat?

    /// 손을 대는 즉시 시작되는 DragGesture 단독 버전. `dragChanged(from:)`으로
    /// 상대/절대 delta 전달 방식을 분기한다.
    private var immediateDrag: some Gesture {
        DragGesture(minimumDistance: ReorderDragFeatureFlags.useRelativeDelta ? 4 : 2, coordinateSpace: .local)
            .onChanged { value in
                guard enabled else { return }
                dragChanged(location: value.location.y, translation: value.translation.height)
            }
            .onEnded { _ in
                lastLocationY = nil
                proxy.onDragEnded()
            }
    }

    /// 0.5초 롱프레스가 끝난 뒤에만 드래그를 시작하는 버전.
    private var longPressThenDrag: some Gesture {
        LongPressGesture(minimumDuration: 0.5)
            .sequenced(before: DragGesture(minimumDistance: 0, coordinateSpace: .local))
            .onChanged { value in
                guard enabled else { return }
                switch value {
                case .second(true, let drag):
                    guard let drag else { return }
                    dragChanged(location: drag.location.y, translation: drag.translation.height)
                default: break
                }
            }
            .onEnded { _ in
                lastLocationY = nil
                proxy.onDragEnded()
            }
    }

    /// - Parameters:
    ///   - location: 로컬 좌표계에서의 현재 y 위치(상대 delta 계산용).
    ///   - translation: 제스처 시작점 기준 누적 이동량(절대 방식용).
    private func dragChanged(location: CGFloat, translation: CGFloat) {
        if ReorderDragFeatureFlags.useRelativeDelta {
            if let last = lastLocationY {
                let delta = location - last
                if abs(delta) >= 0.5 { proxy.onDragChanged(delta) }
            } else {
                lastLocationY = location
            }
            lastLocationY = location
        } else {
            proxy.onDragChanged(translation)
        }
    }

    var body: some View {
        VStack(spacing: 3) {
            Capsule().frame(width: 24, height: 4)
            Capsule().frame(width: 24, height: 4)
            Capsule().frame(width: 24, height: 4)
        }
        .foregroundStyle(enabled ? AppTheme.accent : AppTheme.textSecondary)
        .frame(width: 44, height: 52)
        .contentShape(Rectangle())
        .modifier(ConditionalGestureModifier(useLongPress: ReorderDragFeatureFlags.useLongPressBeforeDrag, longPress: longPressThenDrag, immediate: immediateDrag))
        .accessibilityLabel("순서 이동 핸들")
        .accessibilityHint(ReorderDragFeatureFlags.useLongPressBeforeDrag ? "길게 눌러 드래그를 시작한 뒤 위아래로 자유롭게 이동해 순서를 변경합니다" : "눌러서 위아래로 자유롭게 드래그해 순서를 변경합니다")
    }
}

/// 두 종류의 `Gesture`(opaque type)를 런타임 플래그로 분기 적용하기 위한 헬퍼.
/// SwiftUI의 `some Gesture`는 타입이 서로 달라 삼항 연산자로 바로 고를 수 없어
/// ViewModifier로 감싼다.
private struct ConditionalGestureModifier<LongPress: Gesture, Immediate: Gesture>: ViewModifier {
    let useLongPress: Bool
    let longPress: LongPress
    let immediate: Immediate

    func body(content: Content) -> some View {
        if useLongPress {
            content.gesture(longPress)
        } else {
            content.gesture(immediate)
        }
    }
}

/// 드래그 핸들을 감싸는 컨트롤. 위/아래 스텝 버튼은 자유 드래그 도입으로 더 이상 필요 없어 비활성화되어 있다.
struct ReorderControls: View {
    let enabled: Bool
    let proxy: DragHandleProxy

    var body: some View {
        DragReorderHandle(enabled: enabled, proxy: proxy)
    }
}

private extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self { min(max(self, range.lowerBound), range.upperBound) }
}

struct ShakeEffect: GeometryEffect {
    var amount: CGFloat = 8
    var shakesPerUnit = 3
    var animatableData: CGFloat
    func effectValue(size: CGSize) -> ProjectionTransform { ProjectionTransform(CGAffineTransform(translationX: amount * sin(animatableData * .pi * CGFloat(shakesPerUnit)), y: 0)) }
}
