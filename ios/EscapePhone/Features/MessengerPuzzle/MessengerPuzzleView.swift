import SwiftUI

struct MessengerPuzzleView: View {
    @EnvironmentObject private var app: AppContainer
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var engine = MessengerPuzzleEngine()
    @State private var orderedMessages: [PuzzleMessage] = []
    @State private var shake: CGFloat = 0
    @State private var isReorderDragging = false
    @State private var showError = false
    var body: some View {
        ZStack {
            AppTheme.background.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 16) {
                    Text("손상된 대화를 시간과 내용의 흐름에 맞게 정렬하세요.").foregroundStyle(.secondary)
                    ReorderableList(items: $orderedMessages, enabled: !engine.isSolved) { message, proxy in
                        HStack(alignment: .center) {
                            VStack(alignment: .leading, spacing: 6) { HStack { Text(message.sender).font(.headline); Spacer(); Text(message.displayedTime).font(.caption.monospaced()).foregroundStyle(AppTheme.accent) }; Text(message.body).frame(maxWidth: .infinity, alignment: .leading) }
                            ReorderControls(enabled: !engine.isSolved, proxy: proxy)
                        }.gameCard()
                    }
                    .modifier(ShakeEffect(animatableData: shake))
                    .onChange(of: orderedMessages.map(\.id)) { _, _ in syncEngineOrder() }
                    if engine.isSolved {
                        Label("기록 복원 완료 · 사진첩 잠금 해제", systemImage: "checkmark.seal.fill").foregroundStyle(AppTheme.accent).gameCard()
                        Button("휴대폰 홈으로") { app.path = [.phoneHome] }.buttonStyle(PrimaryButtonStyle())
                    } else {
                        Button("순서 확인") { submit() }.buttonStyle(PrimaryButtonStyle())
                        Text("순서가 맞지 않습니다.").foregroundStyle(.red).opacity(showError ? 1 : 0)
                        HintSection(hints: ["메시지에 적힌 시간과 행동의 순서를 비교해 보세요.", "사진 확인은 서버 백업과 마지막 커밋보다 먼저입니다."]) { level in app.requestHint("messenger_order.\(level)") }
                    }
                }
                .padding()
            }
            .scrollDisabled(ReorderDragFeatureFlags.disableScrollWhileDragging && isReorderDragging)
            .onPreferenceChange(ReorderDraggingPreferenceKey.self) { isReorderDragging = $0 }
        }
        .navigationTitle("메신저 복구")
        .onAppear { orderedMessages = engine.messages }
    }
    /// 사용자가 손을 뗀 순간의 시각적 배치를 엔진의 실제 순서에 반영한다.
    /// 인접 교환(버블 정렬)으로 목표 순서에 수렴시키며, 항목 수의 제곱만큼 상한을 둬
    /// 데이터 불일치 등 예외 상황에서도 무한 반복되지 않도록 한다.
    private func syncEngineOrder() {
        let targetOrder = orderedMessages.map(\.id)
        guard targetOrder != engine.messages.map(\.id) else { return }
        app.haptics.play(.selection)
        let maxIterations = targetOrder.count * targetOrder.count
        var iterations = 0
        while engine.messages.map(\.id) != targetOrder, iterations < maxIterations {
            iterations += 1
            guard let mismatchIndex = Array(zip(engine.messages.map(\.id), targetOrder)).firstIndex(where: { $0 != $1 }) else { break }
            let targetID = targetOrder[mismatchIndex]
            guard let currentIndex = engine.messages.firstIndex(where: { $0.id == targetID }) else { break }
            if currentIndex > mismatchIndex { engine.moveUp(at: currentIndex) } else { engine.moveDown(at: currentIndex) }
        }
    }
    private func submit() {
        if engine.submit() {
            showError = false
            app.haptics.play(.success)
            app.completeMessengerPuzzle()
        } else {
            app.recordWrongAttempt("messenger_order", reason: "messageOrderIncorrect")
            app.haptics.play(.error)
            showError = true
            withAnimation(reduceMotion ? nil : .linear(duration: 0.35)) { shake += 1 }
        }
    }
}

#Preview { NavigationStack { MessengerPuzzleView().environmentObject(AppContainer.preview(gameProgress: GameProgress(currentStage: .introCompleted))) } }
