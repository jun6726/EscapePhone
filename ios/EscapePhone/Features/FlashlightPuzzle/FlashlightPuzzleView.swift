import Combine
import SwiftUI

@MainActor
final class FlashlightViewModel: ObservableObject {
    @Published var position = CGPoint(x: 0.5, y: 0.5)
    @Published private(set) var discovered: [Int]
    private var engine: FlashlightPuzzleEngine
    private var rollFilter = LowPassFilter()
    private var pitchFilter = LowPassFilter()
    private var lastTick: Date?
    init(discovered: [Int] = []) { engine = FlashlightPuzzleEngine(discoveredDigits: discovered); self.discovered = discovered }
    func handle(_ attitude: DeviceAttitude) {
        let roll = rollFilter.update(with: attitude.roll)
        let pitch = pitchFilter.update(with: attitude.pitch)
        position = FlashlightPuzzleEngine.clamp(CGPoint(x: 0.5 + roll / 0.8, y: 0.5 + pitch / 0.8))
    }
    func move(to point: CGPoint, in size: CGSize) { guard size.width > 0, size.height > 0 else { return }; position = FlashlightPuzzleEngine.clamp(CGPoint(x: point.x / size.width, y: point.y / size.height)) }
    func resetCalibration() { rollFilter.reset(); pitchFilter.reset(); position = CGPoint(x: 0.5, y: 0.5) }
    func tick(_ date: Date) -> FlashlightPuzzleEvent? {
        defer { lastTick = date }
        guard let lastTick else { return nil }
        let event = engine.update(flashlightPosition: position, deltaTime: date.timeIntervalSince(lastTick))
        discovered = engine.discoveredDigits
        return event
    }
}

struct FlashlightPuzzleView: View {
    @EnvironmentObject private var app: AppContainer
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var model = FlashlightViewModel()
    @State private var hintLevel = 0
    @State private var showHint = false
    @State private var showInstructions = true
    @State private var didComplete = false
    private let timer = Timer.publish(every: 0.05, on: .main, in: .common).autoconnect()
    var body: some View {
        ZStack {
            AppTheme.background.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 14) {
                    HStack { Label(app.gameProgress.controlMode == .motion ? "기울기 조작" : "터치 조작", systemImage: app.gameProgress.controlMode == .motion ? "gyroscope" : "hand.draw"); Spacer(); Text(model.discovered.map(String.init).joined(separator: " · ")).font(.title2.monospaced()).foregroundStyle(AppTheme.accent) }
                    GeometryReader { geo in
                        ZStack {
                            RoundedRectangle(cornerRadius: 18).fill(Color(red: 0.015, green: 0.02, blue: 0.025))
                            ForEach(FlashlightPuzzleEngine.targets) { target in
                                Text(String(target.digit)).font(.system(size: 58, weight: .black, design: .monospaced)).foregroundStyle(.white.opacity(FlashlightPuzzleEngine.distance(model.position, target.normalizedPosition) < 0.18 ? 0.9 : 0.025)).position(x: target.normalizedPosition.x * geo.size.width, y: target.normalizedPosition.y * geo.size.height).accessibilityHidden(true)
                            }
                            Circle().fill(RadialGradient(colors: [.white.opacity(0.34), AppTheme.accent.opacity(0.08), .clear], center: .center, startRadius: 4, endRadius: 72)).frame(width: 150, height: 150).overlay(Circle().stroke(AppTheme.accent.opacity(0.5))).position(x: model.position.x * geo.size.width, y: model.position.y * geo.size.height)
                            VStack { Spacer(); Text("빛을 목표에 잠시 머무르게 하세요").font(.caption).padding(8).background(.black.opacity(0.7)).clipShape(Capsule()).padding() }
                        }.clipShape(RoundedRectangle(cornerRadius: 18)).contentShape(Rectangle()).gesture(app.gameProgress.controlMode == .touch ? DragGesture(minimumDistance: 0).onChanged { model.move(to: $0.location, in: geo.size) } : nil)
                    }.frame(height: 430).accessibilityLabel("어두운 사진. 발견한 숫자 \(model.discovered.map(String.init).joined(separator: ", "))")
                    if !app.motion.isAvailable { Label("이 환경에서는 센서를 사용할 수 없어 터치 모드로 동작합니다.", systemImage: "hand.tap").font(.footnote).foregroundStyle(.secondary) }
                    HStack {
                        Button("기준 다시 맞추기") { app.motion.calibrate(); model.resetCalibration() }.disabled(app.gameProgress.controlMode == .touch)
                        Spacer()
                        Button(app.gameProgress.controlMode == .motion ? "터치로 전환" : "기울기로 전환") { app.setControlMode(app.gameProgress.controlMode == .motion ? .touch : .motion); configureMotion() }.disabled(!app.motion.isAvailable && app.gameProgress.controlMode == .touch)
                    }.buttonStyle(.bordered)
                    Button("힌트 보기") { hintLevel = min(hintLevel + 1, 2); app.requestHint("flashlight_search.\(app.gameProgress.controlMode.rawValue).\(hintLevel)"); showHint = true }.padding()
                    if didComplete || app.gameProgress.flashlightSolved { Label("조작 커밋 417 발견 · 암호 메모 잠금 해제", systemImage: "checkmark.seal.fill").foregroundStyle(AppTheme.accent).gameCard(); Button("암호 메모 열기") { app.navigate(.encryptedNote) }.buttonStyle(PrimaryButtonStyle()) }
                }.padding()
            }
        }.navigationTitle("어두운 사진").onAppear { showInstructions = !app.gameProgress.flashlightSolved; model.resetCalibration(); configureMotion() }.onDisappear { app.motion.stop() }.onChange(of: scenePhase) { _, phase in if phase == .active { configureMotion() } else { app.motion.stop() } }.onReceive(timer) { date in guard !didComplete else { return }; handle(model.tick(date)) }.sheet(isPresented: $showHint) { HintSheet(title: "손전등 힌트 \(hintLevel)/2", text: hintText) }.alert("손전등 사용 방법", isPresented: $showInstructions) { Button("확인", role: .cancel) {} } message: { Text("스마트폰의 기울기에 따라서 손전등의 표시가 움직입니다. 특정 문자 위에서 1초이상 있으면 우측 상단에 기록됩니다.\n\n위에서 부터 힌트를 찾아주세요.").bold() }
    }
    private var hintText: String { if app.gameProgress.controlMode == .motion { return hintLevel == 1 ? "사진을 밝히는 방법이 화면 터치만은 아닐 수 있습니다." : "휴대폰을 천천히 기울여 사진의 구석을 확인하세요." }; return hintLevel == 1 ? "빛을 사진의 어두운 부분으로 이동해 보세요." : "빛을 천천히 드래그해 사진의 구석을 확인하세요." }
    private func configureMotion() { app.motion.stop(); app.motion.onAttitudeChanged = { [weak model] attitude in model?.handle(attitude) }; if app.gameProgress.controlMode == .motion { app.motion.calibrate(); app.motion.start() } }
    private func handle(_ event: FlashlightPuzzleEvent?) { guard let event else { return }; switch event { case .digitDiscovered(let digit): app.recordDigit(digit); app.haptics.play(.digitDiscovered); case .completed: app.completeFlashlightPuzzle(); app.haptics.play(.success); didComplete = true; app.motion.stop() } }
}

#Preview("터치 모드") { NavigationStack { FlashlightPuzzleView().environmentObject(AppContainer.preview(gameProgress: GameProgress(currentStage: .messengerSolved, messengerSolved: true, controlMode: .touch))) } }
