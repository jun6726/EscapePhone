import SwiftUI

struct EndingView: View {
    @EnvironmentObject private var app: AppContainer
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var appeared = false
    @State private var difficulty = 0
    @State private var feedback = ""
    @State private var feedbackSaved = false
    var duration: String {
        guard let start = app.gameProgress.startedAt, let end = app.gameProgress.completedAt else { return "기록 없음" }
        let seconds = max(0, Int(end.timeIntervalSince(start))); return "\(seconds / 60)분 \(seconds % 60)초"
    }
    var body: some View {
        ZStack {
            LinearGradient(colors: [.black, AppTheme.background], startPoint: .top, endPoint: .bottom).ignoresSafeArea()
            ScrollView {
                VStack(spacing: 22) {
                    Image(systemName: "checkmark.shield.fill").font(.system(size: 70)).foregroundStyle(AppTheme.accent)
                    Text("서버 복구 성공").font(.largeTitle.bold())
                    Text("한도윤의 증거 복구 절차가 완료되었습니다.").font(.title3).multilineTextAlignment(.center).gameCard()
                    VStack(spacing: 10) { info("최종 선택", app.gameProgress.endingType == .publicDisclosure ? "외부 감사 서버 공개" : "암호화 보관"); info("수집 증거", "\(app.gameProgress.collectedEvidenceIds.count)개"); info("총 플레이 시간", duration); info("힌트 사용", "\(app.gameProgress.hintCount)회"); info("손전등 조작", app.gameProgress.controlMode == .motion ? "기울기" : "터치") }.gameCard()
                    VStack(alignment: .leading, spacing: 14) {
                        Text("플레이 경험을 알려주세요").font(.headline)
                        if !app.isAnalyticsConsentGranted { Text("설정에서 익명 분석 수집에 동의하면 난이도와 의견을 보낼 수 있습니다.").foregroundStyle(.secondary) }
                        Text("난이도 · 1 매우 쉬움 / 5 매우 어려움").foregroundStyle(.secondary)
                        HStack { ForEach(1...5, id: \.self) { value in Button("\(value)") { difficulty = value; feedbackSaved = false }.buttonStyle(.borderedProminent).tint(difficulty == value ? AppTheme.accent : .gray).disabled(!app.isAnalyticsConsentGranted) } }
                        TextField("불편했던 점이나 개선 의견", text: $feedback, axis: .vertical).lineLimit(3...6).textFieldStyle(.roundedBorder).disabled(!app.isAnalyticsConsentGranted).onChange(of: feedback) { _, value in if value.count > 1000 { feedback = String(value.prefix(1000)) }; feedbackSaved = false }
                        Text("\(feedback.count)/1000").font(.caption).foregroundStyle(.secondary).frame(maxWidth: .infinity, alignment: .trailing)
                        Button(feedbackSaved ? "피드백 저장 완료" : "피드백 저장") { feedbackSaved = app.submitPlayerFeedback(difficulty, comment: feedback) }.buttonStyle(PrimaryButtonStyle()).disabled(!app.isAnalyticsConsentGranted || difficulty == 0 || feedbackSaved)
                        Text("분석 JSON은 화면 로딩을 막지 않고 비동기로 전송되며, 실패 시 다음 실행에서 재시도합니다.").font(.caption).foregroundStyle(.secondary)
                        if app.isAnalyticsConsentGranted { ShareLink(item: app.exportPlaytestReport(), subject: Text("The Last Commit 플레이테스트")) { Label("익명 분석 보고서 공유", systemImage: "square.and.arrow.up").frame(maxWidth: .infinity) }.buttonStyle(.bordered) }
                    }.gameCard()
                    Button("다시 플레이") { app.startNewGame() }.buttonStyle(PrimaryButtonStyle())
                    Button("첫 화면으로 이동") { app.path = [] }.padding()
                }.padding().opacity(appeared ? 1 : 0).offset(y: appeared ? 0 : 16)
            }
        }.navigationBarBackButtonHidden().onAppear { difficulty = app.gameProgress.playerFeedback?.difficultyRating ?? 0; feedback = app.gameProgress.playerFeedback?.comment ?? ""; feedbackSaved = app.gameProgress.playerFeedback != nil; withAnimation(reduceMotion ? nil : .easeOut(duration: 0.6)) { appeared = true } }
    }
    private func info(_ title: String, _ value: String) -> some View { HStack { Text(title).foregroundStyle(.secondary); Spacer(); Text(value).font(.body.monospaced()) } }
}

#Preview { EndingView().environmentObject(AppContainer.preview(gameProgress: GameProgress(currentStage: .gameCompleted, messengerSolved: true, flashlightSolved: true, discoveredDigits: [4, 1, 7], hintCount: 2, startedAt: Date(timeIntervalSinceNow: -480), completedAt: Date(), controlMode: .touch))) }
