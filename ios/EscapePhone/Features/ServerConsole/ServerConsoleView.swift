import SwiftUI

struct ServerConsoleView: View {
    @EnvironmentObject private var app: AppContainer
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var engine = ServerCodeEngine()
    @State private var shake: CGFloat = 0
    private let columns = Array(repeating: GridItem(.flexible(), spacing: 10), count: 3)
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 18) {
                    Text("root@recovery:~$ authorize --code").font(.callout.monospaced()).foregroundStyle(AppTheme.accent).frame(maxWidth: .infinity, alignment: .leading)
                    HStack(spacing: 5) { ForEach(0..<6, id: \.self) { index in Text(character(at: index)).font(.system(size: 28, weight: .bold, design: .monospaced)).frame(width: 48, height: 64).background(AppTheme.card).clipShape(RoundedRectangle(cornerRadius: 10)) } }.modifier(ShakeEffect(animatableData: shake)).accessibilityLabel("입력 코드 \(engine.input.isEmpty ? "비어 있음" : engine.input)")
                    LazyVGrid(columns: columns, spacing: 10) {
                        ForEach(1...9, id: \.self) { digit in key(String(digit)) { engine.append(digit); app.haptics.play(.selection) } }
                        key("전체 삭제") { engine.clear() }
                        key("0") { engine.append(0); app.haptics.play(.selection) }
                        key("지우기") { engine.deleteLast() }
                    }
                    Button("코드 제출") { submit() }.buttonStyle(PrimaryButtonStyle()).disabled(engine.input.count < 6)
                    Text("조작 커밋 417 + 증거 삭제 예약 01:21의 분 단위 121").font(.footnote).foregroundStyle(.secondary)
                }.padding()
            }
        }.navigationTitle("서버 콘솔")
    }
    private func character(at index: Int) -> String { let chars = Array(engine.input); return chars.indices.contains(index) ? String(chars[index]) : "—" }
    private func key(_ title: String, action: @escaping () -> Void) -> some View { Button(action: action) { Text(title).font(title.count == 1 ? .title.monospaced() : .caption).frame(maxWidth: .infinity, minHeight: 56).background(AppTheme.card).clipShape(RoundedRectangle(cornerRadius: 12)) }.accessibilityLabel(title) }
    private func submit() { switch engine.submitServerCode() { case .success: if app.submitServerCode(engine.serverCodeInput) { app.navigate(.finalDecision) }; case .incorrect: app.recordWrongAttempt("server_code", reason: "serverCodeIncorrect"); app.haptics.play(.error); withAnimation(reduceMotion ? nil : .linear(duration: 0.35)) { shake += 1 }; case .incomplete: break } }
}

#Preview { NavigationStack { ServerConsoleView().environmentObject(AppContainer.preview(gameProgress: GameProgress(currentStage: .flashlightSolved, messengerSolved: true, flashlightSolved: true, discoveredDigits: [4, 1, 7]))) } }
