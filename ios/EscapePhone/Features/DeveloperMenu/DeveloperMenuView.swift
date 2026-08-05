#if DEBUG
import SwiftUI

struct DeveloperMenuView: View {
    @EnvironmentObject private var app: AppContainer
    @State private var pitch = 0.0
    @State private var roll = 0.0
    @State private var yaw = 0.0
    @State private var adChoice = "rewarded"
    @State private var adStatus = "미실행"
    var body: some View {
        Form {
            Section("현재 상태") { LabeledContent("단계", value: app.gameProgress.currentStage.rawValue); Text(String(describing: app.gameProgress)); LabeledContent("센서 지원", value: app.motion.isAvailable ? "예" : "아니오") }
            Section("즉시 이동") { Button("휴대폰 홈") { app.path = [.phoneHome] }; Button("메신저 강제 성공") { app.completeMessengerPuzzle() }; Button("손전등 강제 성공") { app.completeMessengerPuzzle(); app.completeFlashlightPuzzle() }; Button("최종 코드 화면") { unlockAll(); app.path = [.serverConsole] }; Button("엔딩") { unlockAll(); app.completeGame(); app.selectEncryptedArchive(); app.path = [.ending] } }
            Section("Mock Motion / 원시·필터 확인") { LabeledContent("원시 pitch", value: pitch.formatted(.number.precision(.fractionLength(2)))); LabeledContent("필터 예상 pitch", value: (pitch * 0.16).formatted(.number.precision(.fractionLength(2)))); Slider(value: $pitch, in: -0.8...0.8); LabeledContent("원시 roll", value: roll.formatted(.number.precision(.fractionLength(2)))); LabeledContent("필터 예상 roll", value: (roll * 0.16).formatted(.number.precision(.fractionLength(2)))); Slider(value: $roll, in: -0.8...0.8); LabeledContent("yaw", value: yaw.formatted(.number.precision(.fractionLength(2)))); Slider(value: $yaw, in: -1...1); Button("Mock 값 전송") { (app.motion as? MockMotionController)?.send(.init(pitch: pitch, roll: roll, yaw: yaw)) } }
            Section("조작 및 확장") {
                Picker("조작", selection: Binding(get: { app.gameProgress.controlMode }, set: { app.setControlMode($0) })) { Text("motion").tag(FlashlightControlMode.motion); Text("touch").tag(FlashlightControlMode.touch) }
                Picker("가짜 광고 결과", selection: $adChoice) { Text("rewarded").tag("rewarded"); Text("skipped").tag("skipped"); Text("failed").tag("failed") }.onChange(of: adChoice) { _, value in (app.ads as? FakeAdGateway)?.result = value == "rewarded" ? .rewarded : (value == "skipped" ? .skipped : .failed("Debug 실패")) }
                Button("가짜 광고 실행") { Task { adStatus = String(describing: await app.ads.showRewardedAd()) } }; LabeledContent("광고 상태", value: adStatus)
                LabeledContent("가짜 장치", value: String(describing: app.device.state)); Button("장치 연결") { Task { await app.device.connect(sessionId: "debug"); app.objectWillChange.send() } }; Button("장치 연결 해제") { Task { await app.device.disconnect(); app.objectWillChange.send() } }
            }
            Section("저장") { Button("저장 데이터 출력") { print(app.gameProgress) }; Button("저장 데이터 삭제", role: .destructive) { try? app.store.reset() }; Button("전체 게임 초기화", role: .destructive) { app.resetGame() } }
        }.navigationTitle("개발 메뉴")
    }
    private func unlockAll() { app.completeMessengerPuzzle(); app.completeFlashlightPuzzle(); app.completeEncryptedNotePuzzle(); app.completeAudioRecordPuzzle(); app.completeCommitGraphPuzzle(); app.completeAccessLogPuzzle() }
}
#endif
