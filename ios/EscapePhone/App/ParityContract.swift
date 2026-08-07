import SwiftUI

typealias MainMenuScreen = MainMenuView
typealias IntroScreen = IntroView
typealias PhoneHomeScreen = PhoneHomeView
typealias MessengerPuzzleScreen = MessengerPuzzleView
typealias FlashlightPuzzleScreen = FlashlightPuzzleView
typealias EncryptedNoteScreen = EncryptedNoteView
typealias AudioRecordPuzzleScreen = AudioRecordPuzzleView
typealias CommitGraphPuzzleScreen = CommitGraphPuzzleView
typealias AccessLogPuzzleScreen = AccessLogPuzzleView
typealias ServerConsoleScreen = ServerConsoleView
typealias FinalDecisionScreen = FinalDecisionView
typealias EndingScreen = EndingView
typealias SettingsScreen = SettingsView
typealias DeviceAnalyticsScreen = DeviceAnalyticsView
#if DEBUG
typealias DeveloperMenuScreen = DeveloperMenuView
#else
struct DeveloperMenuScreen {}
#endif

extension AppContainer {
    func submitServerCode(_ serverCode: String) -> Bool {
        guard serverCode == "417121", gameProgress.accessLogSolved else { if serverCode.count == 6 { recordWrongAttempt("server_code", reason: "serverCodeIncorrect") }; haptics.play(.error); return false }
        completePuzzleSession("server_code"); completeGame(); haptics.play(.success); return true
    }
    func showHint(_ text: String) { userMessage = text }
}

extension FlashlightViewModel {
    func updateFlashlightPosition(to point: CGPoint, in size: CGSize) { move(to: point, in: size) }
}

// Kept in this always-compiled file so older Xcode project caches also see them.
struct EncryptedNoteView: View {
    @EnvironmentObject var app: AppContainer; @State var engine = EncryptedNotePuzzleEngine(); @State var code = ""; @State var error = false
    @FocusState private var isCodeFieldFocused: Bool
    var body: some View { PuzzleContainer("암호화 메모") {
        TextField("3자리 커밋 번호", text: $code).keyboardType(.numberPad).textFieldStyle(.roundedBorder).focused($isCodeFieldFocused).onChange(of: code) { _, value in code = String(value.filter(\.isNumber).prefix(3)) }
        if !engine.isUnlocked {
            Text("손전등에서 발견한 조작 커밋 번호로 메모를 여세요.").foregroundStyle(.secondary)
            Button("메모 열기") { error = !engine.unlock(code: code); if error { app.recordWrongAttempt("encrypted_note", reason: "unlockCodeIncorrect") } else { isCodeFieldFocused = false } }.buttonStyle(PrimaryButtonStyle()).disabled(code.count != 3)
        } else {
            VStack(alignment: .leading, spacing: 8) { Text("복호화된 메모 조각").bold().foregroundStyle(AppTheme.accent); Text("Trace 출시 빌드의 백그라운드 상태 표시 문자열이 손상됐다."); Text("남은 로그: 대상은 증거 기록 / 동작은 데이터 수집 / 상태는 아직 진행 중").foregroundStyle(.secondary); Text("형식: [대상] [동작] [진행 상태]").font(.body.monospaced()).foregroundStyle(AppTheme.accent) }.gameCard()
            Text("로그의 뜻에 맞는 단어를 골라 아래 세 칸을 완성하세요.")
            HStack { ForEach(0..<3, id: \.self) { index in Text(engine.selectedWords.indices.contains(index) ? engine.selectedWords[index] : "___").font(.title3.monospaced()).foregroundStyle(engine.selectedWords.indices.contains(index) ? AppTheme.accent : .secondary).frame(maxWidth: .infinity, minHeight: 54).background(AppTheme.card).clipShape(RoundedRectangle(cornerRadius: 12)) } }
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 3)) { ForEach(engine.noteWords, id: \.self) { word in Button(word) { engine.selectNoteWord(word) }.buttonStyle(.bordered) } }
            HStack(spacing: 8) { Button("마지막 단어 제거") { engine.removeNoteWord() }.buttonStyle(MildDestructiveButtonStyle()); Button("모든 단어 제거") { engine.removeAllNoteWords() }.buttonStyle(StrongDestructiveButtonStyle()) }
            Button("문장 확인") { if engine.submitEncryptedNote() { app.completeEncryptedNotePuzzle() } else { error = true; app.recordWrongAttempt("encrypted_note", reason: engine.selectedWords.count < 3 ? "noteWordsIncomplete" : "noteWordOrderIncorrect") } }.buttonStyle(PrimaryButtonStyle())
            if app.gameProgress.encryptedNoteSolved { Button("음성 기록 열기") { app.navigate(.audioRecordPuzzle) }.buttonStyle(PrimaryButtonStyle()) }
        }
        Text("입력과 단어 순서를 확인하세요.").foregroundStyle(.red).opacity(error ? 1 : 0)
        HintSection(hints: ["메모의 형식은 [대상] [동작] [진행 상태]로, 빈칸은 총 세 칸입니다.", "대상=기록, 동작=수집, 진행 상태=중입니다. 따라서 ‘기록 수집 중’입니다."]) { level in app.requestHint("encrypted_note.\(level)") }
    } }
}

struct AudioRecordPuzzleView: View {
    @EnvironmentObject var app: AppContainer; @State var engine = AudioRecordPuzzleEngine(); @State var error = false
    @State private var orderedFragments: [AudioFragment] = []
    var body: some View { PuzzleContainer("손상된 음성 기록") {
        Text("마이크를 사용하지 않습니다. 파형과 자막을 정렬하세요.").foregroundStyle(.secondary)
        ReorderableList(items: $orderedFragments, enabled: !engine.isSolved) { fragment, proxy in
            HStack(spacing: 8) { Button("▶") { engine.playAudioFragment(fragment.id) }.frame(width: 38); Image(systemName: "waveform").foregroundStyle(AppTheme.accent); Text(fragment.subtitle).font(.callout).lineLimit(2).frame(maxWidth: .infinity, alignment: .leading); ReorderControls(enabled: !engine.isSolved, proxy: proxy) }.padding(.horizontal, 12).frame(minHeight: 62).background(AppTheme.card).clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .onChange(of: orderedFragments.map(\.id)) { _, _ in syncEngineFragmentOrder() }
        Button("음성 순서 확인") { if engine.submitAudioOrder() { error = false; app.completeAudioRecordPuzzle() } else { error = true; app.recordWrongAttempt("audio_record", reason: "audioOrderIncorrect") } }.buttonStyle(PrimaryButtonStyle()); if error { Text("대화 순서를 다시 확인하세요.").foregroundStyle(.red) }; if app.gameProgress.audioRecordSolved { Button("커밋 그래프 열기") { app.navigate(.commitGraphPuzzle) }.buttonStyle(PrimaryButtonStyle()) }; HintSection(hints: ["승인 여부를 묻는 말이 시작입니다.", "결정 → 경고 → 원상 복구 지시 순서입니다."]) { level in app.requestHint("audio_record.\(level)") }
    }.onAppear { orderedFragments = engine.fragments } }
    private func syncEngineFragmentOrder() {
        let targetOrder = orderedFragments.map(\.id)
        guard targetOrder != engine.fragments.map(\.id) else { return }
        app.haptics.play(.selection)
        let maxIterations = targetOrder.count * targetOrder.count
        var iterations = 0
        while engine.fragments.map(\.id) != targetOrder, iterations < maxIterations {
            iterations += 1
            guard let mismatchIndex = Array(zip(engine.fragments.map(\.id), targetOrder)).firstIndex(where: { $0 != $1 }) else { break }
            let targetID = targetOrder[mismatchIndex]
            guard let currentIndex = engine.fragments.firstIndex(where: { $0.id == targetID }) else { break }
            if currentIndex > mismatchIndex { engine.moveAudioFragmentUp(currentIndex) } else { engine.moveAudioFragmentDown(currentIndex) }
        }
    }
}

struct CommitGraphPuzzleView: View {
    @EnvironmentObject var app: AppContainer; @State var engine = CommitGraphPuzzleEngine(); @State var error = false
    @State private var orderedNodes: [CommitNode] = []
    var body: some View { PuzzleContainer("커밋 그래프 복구") {
        ReorderableList(items: $orderedNodes, enabled: !engine.isSolved) { node, proxy in
            VStack(alignment: .leading) { HStack { Text("\(node.number) \(node.title)").bold(); Spacer(); ReorderControls(enabled: !engine.isSolved, proxy: proxy) }; ScrollView(.horizontal) { HStack { ForEach(engine.branches, id: \.self) { branch in Button(branch) { engine.moveCommitNode(node.id, branch: branch) }.buttonStyle(.bordered).tint(engine.placements[node.id] == branch ? AppTheme.accent : .gray) } } } }.gameCard()
        }
        .onChange(of: orderedNodes.map(\.id)) { _, _ in syncEngineNodeOrder() }
        HStack { Button("417 → 418") { engine.connectCommitNode("417", toId: "418") }; Button("418 → 420") { engine.connectCommitNode("418", toId: "420") } }; Button("그래프 검증") { if engine.submitCommitGraph() { app.completeCommitGraphPuzzle() } else { error = true; let reason = engine.nodes.contains { engine.placements[$0.id] != $0.correctBranch } ? "commitBranchIncorrect" : engine.nodeOrder != engine.nodes.sorted { $0.correctOrder < $1.correctOrder }.map(\.id) ? "commitOrderIncorrect" : "commitConnectionMissing"; app.recordWrongAttempt("commit_graph", reason: reason) } }.buttonStyle(PrimaryButtonStyle()); Text("브랜치·순서·연결을 확인하세요.").foregroundStyle(.red).opacity(error ? 1 : 0); if app.gameProgress.commitGraphSolved { Button("접근 로그 열기") { app.navigate(.accessLogPuzzle) }.buttonStyle(PrimaryButtonStyle()) }; HintSection(hints: ["실험과 수집 코드는 analytics입니다.", "417 → 418 → 420 순서입니다."]) { level in app.requestHint("commit_graph.\(level)") }
    }.onAppear { orderedNodes = engine.nodeOrder.compactMap { id in engine.nodes.first { $0.id == id } } } }
    private func syncEngineNodeOrder() {
        let targetOrder = orderedNodes.map(\.id)
        guard targetOrder != engine.nodeOrder else { return }
        app.haptics.play(.selection)
        let maxIterations = targetOrder.count * targetOrder.count
        var iterations = 0
        while engine.nodeOrder != targetOrder, iterations < maxIterations {
            iterations += 1
            guard let mismatchIndex = Array(zip(engine.nodeOrder, targetOrder)).firstIndex(where: { $0 != $1 }) else { break }
            let targetID = targetOrder[mismatchIndex]
            guard let currentIndex = engine.nodeOrder.firstIndex(of: targetID) else { break }
            let direction = currentIndex > mismatchIndex ? -1 : 1
            engine.moveCommitNode(targetID, newIndex: min(max(currentIndex + direction, 0), engine.nodeOrder.count - 1))
        }
    }
}

struct AccessLogPuzzleView: View {
    @EnvironmentObject var app: AppContainer; @State var engine = AccessLogPuzzleEngine(); @State var error = false
    let questions = [("한도윤이 사무실에 있었는가?", ["있었다", "없었다"]), ("01:14 메시지 유형은?", ["실시간 메시지다", "예약 메시지다"]), ("ROOT-M 계정 유형은?", ["개인 계정이다", "관리자 공용 계정이다"])]
    var body: some View { PuzzleContainer("접근 로그 교차 검증") { ForEach(["한도윤 00:38 퇴실 / 재입실 없음", "01:12 ROOT-M / CTO 회의실 단말기", "01:14 예약 전송 / 삭제 예약 01:21"], id: \.self) { Text($0).font(.callout.monospaced()).gameCard() }; ForEach(Array(questions.enumerated()), id: \.offset) { index, question in VStack(alignment: .leading) { Text(question.0).bold(); HStack { ForEach(question.1, id: \.self) { answer in Button(answer) { engine.answerLogQuestion(index, answer: answer) }.buttonStyle(.bordered).tint(engine.answers[index] == answer ? AppTheme.accent : .gray) } } } }; Button("답변 검증") { if engine.validateAccessLogAnswers(), engine.completeAccessLogPuzzle() { app.completeAccessLogPuzzle(evidenceIds: ["entry_log", "server_log", "scheduled_message"] ) } else { error = true; app.recordWrongAttempt("access_log", reason: engine.answers.count < 3 ? "logAnswersIncomplete" : "logAnswerIncorrect") } }.buttonStyle(PrimaryButtonStyle()); Text("기록을 다시 대조하세요.").foregroundStyle(.red).opacity(error ? 1 : 0); if app.gameProgress.accessLogSolved { Text("조작 단말기: CTO 회의실 관리용 단말기").foregroundStyle(AppTheme.accent); Button("서버 콘솔 열기") { app.navigate(.serverConsole) }.buttonStyle(PrimaryButtonStyle()) }; HintSection(hints: ["퇴실과 로그인 시각을 비교하세요.", "예약 플래그와 ROOT-M 유형을 확인하세요."]) { level in app.requestHint("access_log.\(level)") } } }
}

struct FinalDecisionView: View {
    @EnvironmentObject var app: AppContainer
    var body: some View { PuzzleContainer("최종 결정") { Text("한도윤이 남긴 증거 복구 절차가 완료되었습니다."); Button("증거를 외부 감사 서버에 공개한다") { app.selectPublicDisclosure() }.buttonStyle(PrimaryButtonStyle()); Button("증거를 암호화해 보관한다") { app.selectEncryptedArchive() }.buttonStyle(.bordered).frame(maxWidth: .infinity) } }
}

private struct PuzzleContainer<Content: View>: View {
    let title: String
    let content: Content
    init(_ title: String, @ViewBuilder content: () -> Content) { self.title = title; self.content = content() }

    @State private var isReorderDragging = false

    var body: some View {
        ZStack {
            AppTheme.background.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 14) { content }.padding()
            }
            // 드래그 재정렬이 진행 중일 때만 스크롤을 잠가 드래그 제스처와의 동시 인식
            // 경쟁을 막는다.
            .scrollDisabled(ReorderDragFeatureFlags.disableScrollWhileDragging && isReorderDragging)
            .onPreferenceChange(ReorderDraggingPreferenceKey.self) { isReorderDragging = $0 }
        }
        .navigationTitle(title)
    }
}
