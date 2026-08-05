import SwiftUI

struct IntroView: View {
    @EnvironmentObject private var app: AppContainer
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var shown = 0
    private let lines = ["NOVACODE의 앱 Trace 출시를 앞두고 개발자 한도윤이 사라졌다.", "그가 남긴 휴대폰에는 동의 없는 행동 데이터 수집의 흔적이 있다.", "서버가 증거를 지우기 전에 조작된 커밋을 추적해야 한다."]
    var body: some View {
        ZStack {
            AppTheme.background.ignoresSafeArea()
            VStack(alignment: .leading, spacing: 24) {
                Text("RECOVERING LOG…").font(.caption.monospaced()).foregroundStyle(AppTheme.accent)
                ForEach(Array(lines.enumerated()), id: \.offset) { index, line in Text(line).font(.title2).opacity(index < shown ? 1 : 0) }
                Spacer()
                Button(shown == lines.count ? "휴대폰 열기" : "건너뛰기") { app.completeIntro() }.buttonStyle(PrimaryButtonStyle())
            }.padding(28)
        }
        .navigationBarBackButtonHidden()
        .task {
            guard !reduceMotion else { shown = lines.count; return }
            for count in 1...lines.count { try? await Task.sleep(for: .milliseconds(700)); withAnimation(.easeOut(duration: 0.35)) { shown = count } }
        }
    }
}
