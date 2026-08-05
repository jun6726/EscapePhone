import SwiftUI

struct MessengerPuzzleView: View {
    @EnvironmentObject private var app: AppContainer
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var engine = MessengerPuzzleEngine()
    @State private var shake: CGFloat = 0
    @State private var hintLevel = 0
    @State private var showHint = false
    var body: some View {
        ZStack {
            AppTheme.background.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 16) {
                    Text("손상된 대화를 시간과 내용의 흐름에 맞게 정렬하세요.").foregroundStyle(.secondary)
                    VStack(spacing: 12) {
                        ForEach(Array(engine.messages.enumerated()), id: \.element.id) { index, message in
                            HStack(alignment: .center) {
                                VStack(alignment: .leading, spacing: 6) { HStack { Text(message.sender).font(.headline); Spacer(); Text(message.displayedTime).font(.caption.monospaced()).foregroundStyle(AppTheme.accent) }; Text(message.body).frame(maxWidth: .infinity, alignment: .leading) }
                                DragReorderHandle(itemId: message.id, currentIndex: index, itemCount: engine.messages.count, enabled: !engine.isSolved) { direction in move(message.id, direction: direction) }
                            }.gameCard()
                        }
                    }.modifier(ShakeEffect(animatableData: shake))
                    if engine.isSolved {
                        Label("기록 복원 완료 · 사진첩 잠금 해제", systemImage: "checkmark.seal.fill").foregroundStyle(AppTheme.accent).gameCard()
                        Button("휴대폰 홈으로") { app.path = [.phoneHome] }.buttonStyle(PrimaryButtonStyle())
                    } else {
                        Button("순서 확인") { submit() }.buttonStyle(PrimaryButtonStyle())
                        Button("힌트 보기") { hintLevel = min(hintLevel + 1, 2); app.requestHint("messenger_order.\(hintLevel)"); showHint = true }.padding()
                    }
                }.padding()
            }
        }.navigationTitle("메신저 복구").sheet(isPresented: $showHint) { HintSheet(title: "메신저 힌트 \(hintLevel)/2", text: hintLevel == 1 ? "메시지에 적힌 시간과 행동의 순서를 비교해 보세요." : "사진 확인은 서버 백업과 마지막 커밋보다 먼저입니다.") }
    }
    private func move(_ messageId: String, direction: Int) { guard let index = engine.messages.firstIndex(where: { $0.id == messageId }) else { return }; app.haptics.play(.selection); if direction < 0 { engine.moveUp(at: index) } else { engine.moveDown(at: index) } }
    private func submit() { if engine.submit() { app.haptics.play(.success); app.completeMessengerPuzzle() } else { app.recordWrongAttempt("messenger_order", reason: "messageOrderIncorrect"); app.haptics.play(.error); withAnimation(reduceMotion ? nil : .linear(duration: 0.35)) { shake += 1 } } }
}

struct HintSheet: View {
    @Environment(\.dismiss) private var dismiss
    let title: String; let text: String
    var body: some View { NavigationStack { VStack(spacing: 24) { Image(systemName: "lightbulb.fill").font(.largeTitle).foregroundStyle(AppTheme.accent); Text(text).font(.title3); Spacer() }.padding().navigationTitle(title).toolbar { Button("닫기") { dismiss() } } } }
}

#Preview { NavigationStack { MessengerPuzzleView().environmentObject(AppContainer.preview(gameProgress: GameProgress(currentStage: .introCompleted))) } }
#Preview("힌트") { HintSheet(title: "힌트 1/2", text: "시간과 행동의 순서를 비교해 보세요.") }
