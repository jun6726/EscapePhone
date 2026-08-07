import SwiftUI

struct ConvenienceStoreApp: View {
    @ObservedObject var container: ConvenienceStoreContainer
    let onExitToThemeMenu: () -> Void

    var body: some View {
        ZStack {
            AppTheme.background.ignoresSafeArea()
            switch container.currentScreen {
            case .intro: ConvenienceStoreIntroScreen(container: container).onAppear { container.startPuzzleSession(container.currentScreen.puzzleId) }
            case .home: ConvenienceStoreHomeScreen(container: container, onExitToThemeMenu: onExitToThemeMenu)
            case .receipt: ReceiptPuzzleScreen(container: container)
            case .barcode: BarcodePuzzleScreen(container: container)
            case .shelfDifference: ShelfDifferencePuzzleScreen(container: container)
            case .cctv: CctvPuzzleScreen(container: container)
            case .inventory: InventoryPuzzleScreen(container: container)
            case .customerPattern: CustomerPatternPuzzleScreen(container: container)
            case .incidentTimeline: IncidentTimelinePuzzleScreen(container: container)
            case .finalDecision: ConvenienceStoreFinalDecisionScreen(container: container)
            case .ending: ConvenienceStoreEndingScreen(container: container, onExitToThemeMenu: onExitToThemeMenu)
            }
        }
    }
}

struct StoreScreenColumn<Content: View>: View {
    let title: String
    let onBack: () -> Void
    @ViewBuilder let content: Content

    @State private var isReorderDragging = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Button("←") { onBack() }.foregroundStyle(StoreTheme.accent).font(.title3)
                    Text(title).font(.title3.bold()).foregroundStyle(AppTheme.textPrimary)
                }
                content
            }
            .padding(20)
        }
        .scrollDisabled(ReorderDragFeatureFlags.disableScrollWhileDragging && isReorderDragging)
        .onPreferenceChange(ReorderDraggingPreferenceKey.self) { isReorderDragging = $0 }
    }
}
struct ConvenienceStoreIntroScreen: View {
    @ObservedObject var container: ConvenienceStoreContainer
    private let lines = [
        "지인의 부탁으로 새벽 편의점 근무를 대신 맡았다.",
        "오전 2시 17분, 매장 전원이 꺼지고 다시 켜지자 시계는 오전 2시로 돌아가 있었다.",
        "같은 17분이 반복된다. 하지만 매번 조금씩 달라진다."
    ]
    @State private var shown = 0
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("02:17").font(.system(.largeTitle, design: .monospaced, weight: .bold)).foregroundStyle(StoreTheme.accent)
            ForEach(0..<shown, id: \.self) { index in Text(lines[index]).font(.title3).foregroundStyle(AppTheme.textPrimary) }
            Spacer()
            Button(shown == lines.count ? "관리 태블릿 열기" : "건너뛰기") { container.completeIntro() }.buttonStyle(PrimaryButtonStyle())
        }
        .padding(24)
        .onAppear {
            for index in lines.indices {
                DispatchQueue.main.asyncAfter(deadline: .now() + Double(index) * 0.65) { shown = index + 1 }
            }
        }
    }
}

private struct StoreApp: Identifiable {
    let id: String; let title: String; let icon: String; let unlocked: Bool; let reason: String; let screen: ConvenienceStoreScreen
}

struct ConvenienceStoreHomeScreen: View {
    @ObservedObject var container: ConvenienceStoreContainer
    let onExitToThemeMenu: () -> Void
    private var apps: [StoreApp] {
        let p = container.progress
        return [
            StoreApp(id: "pos", title: "POS", icon: "🧾", unlocked: true, reason: "", screen: .receipt),
            StoreApp(id: "message", title: "점장 메시지", icon: "💬", unlocked: true, reason: "", screen: .home),
            StoreApp(id: "product", title: "상품 조회", icon: "📦", unlocked: p.receiptSolved, reason: "영수증 이상 항목을 먼저 찾으세요.", screen: .barcode),
            StoreApp(id: "shelf", title: "진열대 기록", icon: "🗄️", unlocked: p.barcodeSolved, reason: "바코드 규칙을 먼저 복원하세요.", screen: .shelfDifference),
            StoreApp(id: "cctv", title: "CCTV", icon: "🎥", unlocked: p.shelfDifferenceSolved, reason: "진열대 차이를 먼저 확인하세요.", screen: .cctv),
            StoreApp(id: "inventory", title: "재고 관리", icon: "📊", unlocked: p.cctvSolved, reason: "CCTV 순서를 먼저 복원하세요.", screen: .inventory),
            StoreApp(id: "customer", title: "고객 기록", icon: "🧍", unlocked: p.inventorySolved, reason: "재고 불일치를 먼저 찾으세요.", screen: .customerPattern),
            StoreApp(id: "incident", title: "사건 기록", icon: "🗂️", unlocked: p.customerPatternSolved, reason: "구매 패턴을 먼저 해독하세요.", screen: .incidentTimeline)
        ]
    }
    var body: some View {
        StoreScreenColumn(title: "야간 관리 태블릿", onBack: onExitToThemeMenu) {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(apps) { app in
                    Button {
                        if app.unlocked { container.navigate(app.screen) }
                    } label: {
                        VStack(spacing: 6) {
                            Text(app.icon).font(.system(size: 30))
                            Text(app.title).foregroundStyle(app.unlocked ? AppTheme.textPrimary : AppTheme.textSecondary)
                            if !app.unlocked { Text("🔒 \(app.reason)").font(.caption).foregroundStyle(AppTheme.textSecondary) }
                        }.frame(maxWidth: .infinity, minHeight: 120).background(AppTheme.card).clipShape(RoundedRectangle(cornerRadius: 14))
                    }.disabled(!app.unlocked)
                }
            }
            Text("발견한 증거 \(container.progress.collectedEvidenceIds.count)개 · 코드 \(container.progress.discoveredCodes.count)개").foregroundStyle(AppTheme.textSecondary)
        }
    }
}

struct ReceiptPuzzleScreen: View {
    @ObservedObject var container: ConvenienceStoreContainer
    @State private var selected: String?
    @State private var result: Bool?
    private let hints = ["가격표와 영수증의 금액이 모두 같은지 확인하세요.", "실제 상품 목록에 존재하지 않는 네 자리 코드를 찾아보세요."]
    var body: some View {
        StoreScreenColumn(title: "영수증과 가격표 비교", onBack: { container.navigate(.home) }) {
            Text("POS에 남은 영수증과 진열대 가격표를 비교해 잘못 계산된 상품을 찾으세요.").foregroundStyle(AppTheme.textSecondary)
            ForEach(ConvenienceStoreSpec.receiptItems) { item in
                Button {
                    selected = item.id; container.receiptEngine.selectReceiptItem(item.id)
                } label: {
                    HStack { Text("\(item.name) (\(item.code))").foregroundStyle(AppTheme.textPrimary); Spacer(); Text("\(item.price)원").foregroundStyle(StoreTheme.pos) }
                        .padding().background(selected == item.id ? StoreTheme.accent.opacity(0.25) : AppTheme.card).clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
            if container.progress.receiptSolved || container.receiptEngine.isSolved {
                Text("✓ 이상 코드 0217 발견 · 상품 조회 잠금 해제").foregroundStyle(Color.green)
                Button("홈으로") { container.completeReceiptPuzzle(); container.navigate(.home) }.buttonStyle(PrimaryButtonStyle())
            } else {
                Button("확인") { result = container.receiptEngine.submitReceiptAnswer(); if result == true { container.completeReceiptPuzzle() } else { container.recordWrongAttempt("receipt_price", reason: "receiptAnomalyIncorrect") } }.buttonStyle(PrimaryButtonStyle())
                Text("일치하지 않습니다. 다시 확인하세요.").foregroundStyle(StoreTheme.warning).opacity(result == false ? 1 : 0)
                HintSection(hints: hints) { level in container.requestHint("receipt_price.\(level)", text: hints[level - 1]) }
            }
        }
    }
}

struct BarcodePuzzleScreen: View {
    @ObservedObject var container: ConvenienceStoreContainer
    @State private var category = ""
    @State private var shelfPosition = ""
    @State private var arrivalOrder = ""
    @State private var result: Bool?
    private let hints = ["바코드 숫자를 한 번에 읽지 말고 구간별로 나누어 보세요.", "상품 분류, 진열 위치, 입고 순서가 각각 다른 구간에 들어 있습니다."]
    var body: some View {
        StoreScreenColumn(title: "바코드 규칙 복원", onBack: { container.navigate(.home) }) {
            Text("바코드는 [분류][위치][입고순서] 형식입니다. 세 상품을 비교해 숨겨진 코드를 조합하세요.").foregroundStyle(AppTheme.textSecondary)
            ForEach(ConvenienceStoreSpec.barcodeItems) { item in
                Text("\(item.id): \(item.category)-\(item.shelfPosition)-\(item.arrivalOrder)").font(.system(.body, design: .monospaced)).foregroundStyle(AppTheme.textPrimary)
            }
            HStack {
                Button("분류 = 99") { category = "99"; container.barcodeEngine.selectBarcodeSegment("category", value: "99") }
                Button("위치 = 40") { shelfPosition = "40"; container.barcodeEngine.selectBarcodeSegment("shelfPosition", value: "40") }
                Button("입고순서 = 01") { arrivalOrder = "01"; container.barcodeEngine.selectBarcodeSegment("arrivalOrder", value: "01") }
            }
            Text("조합: \(category)-\(shelfPosition)-\(arrivalOrder)").font(.system(.body, design: .monospaced)).foregroundStyle(StoreTheme.pos)
            if container.barcodeEngine.isSolved {
                Text("✓ 숨겨진 위치 발견: \(ConvenienceStoreSpec.hiddenLocation) · 진열대 기록 잠금 해제").foregroundStyle(Color.green)
                Button("홈으로") { container.completeBarcodePuzzle(); container.navigate(.home) }.buttonStyle(PrimaryButtonStyle())
            } else {
                Button("확인") { result = container.barcodeEngine.submitBarcodeRule(); if result == true { container.completeBarcodePuzzle() } else { container.recordWrongAttempt("barcode_rule", reason: "barcodeSegmentsIncorrect") } }.buttonStyle(PrimaryButtonStyle())
                Text("규칙이 맞지 않습니다.").foregroundStyle(StoreTheme.warning).opacity(result == false ? 1 : 0)
                HintSection(hints: hints) { level in container.requestHint("barcode_rule.\(level)", text: hints[level - 1]) }
            }
        }
    }
}

struct ShelfDifferencePuzzleScreen: View {
    @ObservedObject var container: ConvenienceStoreContainer
    @State private var touchMode = true
    @State private var revealMessage = false
    private let hints = ["상품 개수뿐 아니라 가격표와 빈 공간도 비교하세요.", "냉장고 아래쪽에 이전 기록에는 없던 물체가 있습니다."]
    private let labels: [(String, String)] = [
        ("drink_position", "음료 한 병의 위치"), ("umbrella_count", "우산 개수"), ("price_tag_direction", "가격표 방향"), ("empty_slot", "빈 상품 칸"), ("fridge_pouch", "냉장고 하단의 작은 봉투")
    ]
    var body: some View {
        StoreScreenColumn(title: "반복 전후 진열대 차이", onBack: { container.navigate(.home) }) {
            Text("같은 시각 두 기록을 비교해 차이를 모두 선택하세요.").foregroundStyle(AppTheme.textSecondary)
            ForEach(labels, id: \.0) { id, label in
                let selected = container.shelfDifferenceEngine.selectedDifferenceIds.contains(id)
                Button {
                    container.shelfDifferenceEngine.selectShelfDifference(id)
                    if id == ConvenienceStoreSpec.finalShelfDifferenceId { revealMessage = true }
                } label: {
                    Text(label).frame(maxWidth: .infinity, alignment: .leading).padding().foregroundStyle(AppTheme.textPrimary)
                        .background(selected ? StoreTheme.accent.opacity(0.25) : AppTheme.card).clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
            if revealMessage { Text("봉투 속 문구: \"\(ConvenienceStoreSpec.pouchMessage)\"").font(.system(.body, design: .monospaced)).foregroundStyle(StoreTheme.pos) }
            Toggle("터치 모드", isOn: $touchMode).tint(StoreTheme.accent)
            if container.shelfDifferenceEngine.isSolved {
                Text("✓ 모든 차이 발견 · CCTV 잠금 해제").foregroundStyle(Color.green)
                Button("홈으로") { container.completeShelfDifferencePuzzle(); container.navigate(.home) }.buttonStyle(PrimaryButtonStyle())
            } else {
                Button("확인") { if !container.shelfDifferenceEngine.submitShelfDifferences() { container.recordWrongAttempt("shelf_difference", reason: "shelfDifferencesIncomplete") } }.buttonStyle(PrimaryButtonStyle())
                HintSection(hints: hints) { level in container.requestHint("shelf_difference.\(level)", text: hints[level - 1]) }
            }
        }
    }
}

struct CctvPuzzleScreen: View {
    @ObservedObject var container: ConvenienceStoreContainer
    @State private var result: Bool?
    @State private var orderedRecords: [CctvRecord] = []
    private let hints = ["각 화면의 매장 시계만으로는 순서를 확정할 수 없습니다.", "전자레인지와 POS 영수증 출력 상태를 함께 비교하세요."]
    var body: some View {
        StoreScreenColumn(title: "CCTV 시간 순서 복원", onBack: { container.navigate(.home) }) {
            Text("기록을 올바른 시간 순서로 정렬하세요.").foregroundStyle(AppTheme.textSecondary)
            ReorderableList(items: $orderedRecords, enabled: !container.cctvEngine.isSolved) { record, proxy in
                HStack {
                    Text(record.description).foregroundStyle(AppTheme.textPrimary)
                    Spacer()
                    ReorderControls(enabled: !container.cctvEngine.isSolved, proxy: proxy)
                }.padding().background(AppTheme.card).clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .onAppear { orderedRecords = container.cctvEngine.recordOrder.compactMap { id in ConvenienceStoreSpec.cctvRecords.first { $0.id == id } } }
            .onChange(of: orderedRecords.map(\.id)) { _, _ in syncCctvOrder() }
            if container.cctvEngine.isSolved {
                Text("✓ 순서 복원 완료 · CENTRAL-7 흔적 발견 · 재고 관리 잠금 해제").foregroundStyle(Color.green)
                Button("홈으로") { container.completeCctvPuzzle(); container.navigate(.home) }.buttonStyle(PrimaryButtonStyle())
            } else {
                Button("확인") { result = container.cctvEngine.submitCctvSequence(); if result == true { container.completeCctvPuzzle() } else { container.recordWrongAttempt("cctv_sequence", reason: "cctvOrderIncorrect") } }.buttonStyle(PrimaryButtonStyle())
                Text("순서가 맞지 않습니다.").foregroundStyle(StoreTheme.warning).opacity(result == false ? 1 : 0)
                HintSection(hints: hints) { level in container.requestHint("cctv_sequence.\(level)", text: hints[level - 1]) }
            }
        }
    }
    private func syncCctvOrder() {
        let targetOrder = orderedRecords.map(\.id)
        guard targetOrder != container.cctvEngine.recordOrder else { return }
        let maxIterations = targetOrder.count * targetOrder.count
        var iterations = 0
        while container.cctvEngine.recordOrder != targetOrder, iterations < maxIterations {
            iterations += 1
            guard let mismatchIndex = Array(zip(container.cctvEngine.recordOrder, targetOrder)).firstIndex(where: { $0 != $1 }) else { break }
            let targetID = targetOrder[mismatchIndex]
            guard let currentIndex = container.cctvEngine.recordOrder.firstIndex(of: targetID) else { break }
            if currentIndex > mismatchIndex { container.cctvEngine.moveCctvRecordUp(currentIndex) } else { container.cctvEngine.moveCctvRecordDown(currentIndex) }
        }
    }
}

struct InventoryPuzzleScreen: View {
    @ObservedObject var container: ConvenienceStoreContainer
    @State private var selected: String?
    @State private var result: Bool?
    private let hints = ["현재 재고는 이전 재고에 입고를 더하고 판매를 빼서 계산합니다.", "계산 결과와 실제 수량이 1개 차이 나는 상품을 찾으세요."]
    var body: some View {
        StoreScreenColumn(title: "POS 판매기록과 재고 교차검증", onBack: { container.navigate(.home) }) {
            Text("현재 재고 = 이전 재고 + 입고 - 판매. 계산이 맞지 않는 상품을 찾으세요.").foregroundStyle(AppTheme.textSecondary)
            ForEach(ConvenienceStoreSpec.inventoryItems) { item in
                let expected = container.inventoryEngine.calculateExpectedInventory(item.id)
                Button {
                    selected = item.id; container.inventoryEngine.selectInventoryItem(item.id)
                } label: {
                    VStack(alignment: .leading) {
                        Text(item.name).foregroundStyle(AppTheme.textPrimary).bold()
                        Text("예상 재고 \(expected) · 실제 재고 \(item.actualStock)").foregroundStyle(expected != item.actualStock ? StoreTheme.warning : AppTheme.textSecondary)
                    }.frame(maxWidth: .infinity, alignment: .leading).padding().background(selected == item.id ? StoreTheme.accent.opacity(0.25) : AppTheme.card).clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
            if container.inventoryEngine.isSolved {
                Text("✓ 불일치 상품 발견 · 고객 기록 잠금 해제").foregroundStyle(Color.green)
                Button("홈으로") { container.completeInventoryPuzzle(); container.navigate(.home) }.buttonStyle(PrimaryButtonStyle())
            } else {
                Button("확인") { result = container.inventoryEngine.submitInventoryDiscrepancy(); if result == true { container.completeInventoryPuzzle() } else { container.recordWrongAttempt("inventory_crosscheck", reason: "inventoryItemIncorrect") } }.buttonStyle(PrimaryButtonStyle())
                Text("이 상품은 정상입니다.").foregroundStyle(StoreTheme.warning).opacity(result == false ? 1 : 0)
                HintSection(hints: hints) { level in container.requestHint("inventory_crosscheck.\(level)", text: hints[level - 1]) }
            }
        }
    }
}

struct CustomerPatternPuzzleScreen: View {
    @ObservedObject var container: ConvenienceStoreContainer
    @State private var result: Bool?
    private let hints = ["손님이 산 상품보다 상품이 놓인 위치에 주목하세요.", "각 반복에서 구매한 상품을 진열 순서로 변환하세요."]
    var body: some View {
        StoreScreenColumn(title: "손님의 구매 패턴", onBack: { container.navigate(.home) }) {
            Text("반복마다 동일 손님이 다른 상품을 구매합니다. 패턴을 해독해 메시지를 복원하세요.").foregroundStyle(AppTheme.textSecondary)
            ForEach(ConvenienceStoreSpec.customerPurchases, id: \.cycle) { purchase in
                Text("\(purchase.cycle)회차: \(purchase.items.joined(separator: ", "))").foregroundStyle(AppTheme.textPrimary)
            }
            if container.customerPatternEngine.isSolved {
                Text("✓ 메시지 복원: \"\(container.customerPatternEngine.decodedMessage ?? "")\" · 사건 기록 잠금 해제").foregroundStyle(Color.green)
                Button("홈으로") { container.completeCustomerPatternPuzzle(); container.navigate(.home) }.buttonStyle(PrimaryButtonStyle())
            } else {
                Button("패턴 해독") {
                    let candidate = container.customerPatternEngine.decodePurchasePattern()
                    result = container.customerPatternEngine.submitCustomerPattern(candidate)
                    if result == true { container.completeCustomerPatternPuzzle() } else { container.recordWrongAttempt("customer_pattern", reason: "customerPatternIncorrect") }
                }.buttonStyle(PrimaryButtonStyle())
                HintSection(hints: hints) { level in container.requestHint("customer_pattern.\(level)", text: hints[level - 1]) }
            }
        }
    }
}

struct IncidentTimelinePuzzleScreen: View {
    @ObservedObject var container: ConvenienceStoreContainer
    @State private var orderedEvents: [IncidentEvent] = []
    @State private var result: Bool?
    private let hints = ["기록 시간과 실제 사건 시간이 조작되었을 가능성을 고려하세요.", "CENTRAL-7 접속 이후 변경된 기록을 별도로 분리하세요."]
    var body: some View {
        StoreScreenColumn(title: "실종 당일 타임라인", onBack: { container.navigate(.home) }) {
            Text("사건 카드를 시간순으로 배치하고 결론을 모두 확인하세요.").foregroundStyle(AppTheme.textSecondary)
            ReorderableList(items: $orderedEvents, enabled: !container.incidentTimelineEngine.isSolved) { event, proxy in
                HStack {
                    Text(event.description).foregroundStyle(AppTheme.textPrimary)
                    Spacer()
                    ReorderControls(enabled: !container.incidentTimelineEngine.isSolved, proxy: proxy)
                }.padding().background(AppTheme.card).clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .onAppear { orderedEvents = container.incidentTimelineEngine.eventOrder.compactMap { id in ConvenienceStoreSpec.incidentEvents.first { $0.id == id } } }
            .onChange(of: orderedEvents.map(\.id)) { _, _ in syncIncidentOrder() }
            Button("모든 결론 확인") { for event in ConvenienceStoreSpec.incidentEvents { container.incidentTimelineEngine.selectIncidentConclusion(event.id) } }
            if container.incidentTimelineEngine.isSolved {
                Text("✓ 타임라인 복원 완료 · 최종 선택 잠금 해제").foregroundStyle(Color.green)
                Button("계속") { container.completeIncidentTimelinePuzzle() }.buttonStyle(PrimaryButtonStyle())
            } else {
                Button("확인") { result = container.incidentTimelineEngine.validateIncidentTimeline(); if result == true { container.completeIncidentTimelinePuzzle() } else { container.recordWrongAttempt("incident_timeline", reason: "incidentTimelineIncorrect") } }.buttonStyle(PrimaryButtonStyle())
                Text("사건 순서가 맞지 않습니다.").foregroundStyle(StoreTheme.warning).opacity(result == false ? 1 : 0)
                HintSection(hints: hints) { level in container.requestHint("incident_timeline.\(level)", text: hints[level - 1]) }
            }
        }
    }
    private func syncIncidentOrder() {
        let targetOrder = orderedEvents.map(\.id)
        guard targetOrder != container.incidentTimelineEngine.eventOrder else { return }
        let maxIterations = targetOrder.count * targetOrder.count
        var iterations = 0
        while container.incidentTimelineEngine.eventOrder != targetOrder, iterations < maxIterations {
            iterations += 1
            guard let mismatchIndex = Array(zip(container.incidentTimelineEngine.eventOrder, targetOrder)).firstIndex(where: { $0 != $1 }) else { break }
            let targetID = targetOrder[mismatchIndex]
            guard let currentIndex = container.incidentTimelineEngine.eventOrder.firstIndex(of: targetID) else { break }
            let direction = currentIndex > mismatchIndex ? -1 : 1
            let newIndex = min(max(currentIndex + direction, 0), container.incidentTimelineEngine.eventOrder.count - 1)
            container.incidentTimelineEngine.moveIncidentEvent(targetID, newIndex: newIndex)
        }
    }
}

struct ConvenienceStoreFinalDecisionScreen: View {
    @ObservedObject var container: ConvenienceStoreContainer
    var body: some View {
        VStack(spacing: 20) {
            Text("최종 결정").font(.title.bold()).foregroundStyle(AppTheme.textPrimary)
            Button("복원한 기록을 외부 감사 기관으로 전송한다") { container.selectPublicDisclosure() }.buttonStyle(PrimaryButtonStyle())
            Button("불완전한 증거를 암호화해 보관한다") { container.selectEncryptedArchive() }.frame(maxWidth: .infinity).padding().background(AppTheme.card).clipShape(RoundedRectangle(cornerRadius: 14))
        }.padding(24)
    }
}

struct ConvenienceStoreEndingScreen: View {
    @ObservedObject var container: ConvenienceStoreContainer
    let onExitToThemeMenu: () -> Void
    var body: some View {
        let message = container.progress.endingType == .publicDisclosure ? "반복된 것은 시간이 아니라, 누군가 지우려 했던 17분의 기록이었다." : "마지막 기록은 아직 화면 밖에 남아 있다."
        VStack(spacing: 20) {
            Text("서버 복구 완료").font(.title2.bold()).foregroundStyle(StoreTheme.pos)
            Text(message).foregroundStyle(AppTheme.textPrimary)
            Text("힌트 사용 \(container.progress.seenHintIds.count)회 · 증거 \(container.progress.collectedEvidenceIds.count)개 · 조작 방식 \(container.progress.controlMode.rawValue)").foregroundStyle(AppTheme.textSecondary)
            Button("다시 플레이") { container.startNewGame() }.buttonStyle(PrimaryButtonStyle())
            Button("테마 선택으로 돌아가기") { onExitToThemeMenu() }
        }.padding(24)
    }
}
