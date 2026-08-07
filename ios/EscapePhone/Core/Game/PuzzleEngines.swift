import CoreGraphics
import Foundation

struct MessengerPuzzleEngine {
    static let messages: [PuzzleMessage] = [
        .init(id: "message_1", sender: "도윤", body: "사진에 남긴 번호부터 확인해. 조작된 커밋을 찾을 수 있어.", displayedTime: "00:42", correctOrder: 0),
        .init(id: "message_2", sender: "민서", body: "Trace의 수집 기능은 승인 기록이 없어.", displayedTime: "00:56", correctOrder: 1),
        .init(id: "message_3", sender: "도윤", body: "ROOT-M 로그인과 출입 기록을 반드시 대조해.", displayedTime: "01:18", correctOrder: 2),
        .init(id: "message_4", sender: "민서", body: "증거 삭제 예약 전에 복구 절차를 끝내야 해.", displayedTime: "01:24", correctOrder: 3)
    ]

    private(set) var messages: [PuzzleMessage]
    private(set) var isSolved = false
    init(messages: [PuzzleMessage] = Self.messages.shuffled()) { self.messages = Self.unique(messages) }
    static func unique(_ values: [PuzzleMessage]) -> [PuzzleMessage] {
        var seen = Set<String>(); return values.filter { seen.insert($0.id).inserted }
    }
    mutating func moveUp(at index: Int) { guard !isSolved, messages.indices.contains(index), index > 0 else { return }; messages.swapAt(index, index - 1) }
    mutating func moveDown(at index: Int) { guard !isSolved, messages.indices.contains(index), index < messages.count - 1 else { return }; messages.swapAt(index, index + 1) }
    mutating func moveMessageUp(index: Int) { moveUp(at: index) }
    mutating func moveMessageDown(index: Int) { moveDown(at: index) }
    mutating func submit() -> Bool {
        guard !isSolved else { return true }
        isSolved = messages.map(\.correctOrder) == Array(0..<messages.count)
        return isSolved
    }
    mutating func submitMessengerOrder() -> Bool { submit() }
}

struct LowPassFilter {
    let alpha: Double
    private(set) var value: Double
    init(alpha: Double = 0.16, initialValue: Double = 0) {
        self.alpha = min(max(alpha.isFinite ? alpha : 0.16, 0), 1)
        value = initialValue.isFinite ? initialValue : 0
    }
    mutating func update(with newValue: Double) -> Double {
        guard newValue.isFinite else { return value }
        value += alpha * (newValue - value)
        return value
    }
    mutating func update(newValue: Double) -> Double { update(with: newValue) }
    mutating func reset(to newValue: Double = 0) { value = newValue.isFinite ? newValue : 0 }
}

struct FlashlightPuzzleEngine {
    static let targets = [
        FlashlightTarget(id: "target_4", digit: 4, normalizedPosition: CGPoint(x: 0.2, y: 0.25), requiredHoldDuration: 1.0),
        FlashlightTarget(id: "target_1", digit: 1, normalizedPosition: CGPoint(x: 0.78, y: 0.38), requiredHoldDuration: 1.0),
        FlashlightTarget(id: "target_7", digit: 7, normalizedPosition: CGPoint(x: 0.42, y: 0.78), requiredHoldDuration: 1.0)
    ]
    let targets: [FlashlightTarget]
    let detectionRadius: CGFloat
    private(set) var discoveredDigits: [Int]
    private var heldDuration: TimeInterval = 0
    private(set) var isCompleted = false

    init(targets: [FlashlightTarget] = Self.targets, detectionRadius: CGFloat = 0.18, discoveredDigits: [Int] = []) {
        self.targets = targets; self.detectionRadius = detectionRadius; self.discoveredDigits = discoveredDigits
        self.isCompleted = discoveredDigits.count >= targets.count
    }
    static func clamp(_ point: CGPoint) -> CGPoint { CGPoint(x: min(max(point.x, 0), 1), y: min(max(point.y, 0), 1)) }
    static func distance(_ a: CGPoint, _ b: CGPoint) -> CGFloat { hypot(a.x - b.x, a.y - b.y) }
    mutating func update(flashlightPosition: CGPoint, deltaTime: TimeInterval) -> FlashlightPuzzleEvent? {
        guard !isCompleted, discoveredDigits.count < targets.count else { return nil }
        let target = targets[discoveredDigits.count]
        guard Self.distance(Self.clamp(flashlightPosition), target.normalizedPosition) <= detectionRadius else { heldDuration = 0; return nil }
        heldDuration += min(max(deltaTime.isFinite ? deltaTime : 0, 0), 0.1)
        guard heldDuration + 0.000_001 >= target.requiredHoldDuration else { return nil }
        heldDuration = 0; discoveredDigits.append(target.digit)
        if discoveredDigits.count == targets.count { isCompleted = true; return .completed }
        return .digitDiscovered(target.digit)
    }
    mutating func updateFlashlightPosition(x: CGFloat, y: CGFloat, deltaTime: TimeInterval) -> FlashlightPuzzleEvent? { update(flashlightPosition: CGPoint(x: x, y: y), deltaTime: deltaTime) }
    mutating func updateTargetProgress(x: CGFloat, y: CGFloat, deltaTime: TimeInterval) -> FlashlightPuzzleEvent? { updateFlashlightPosition(x: x, y: y, deltaTime: deltaTime) }
    mutating func completeFlashlightPuzzle() -> [Int] { discoveredDigits = targets.map(\.digit); isCompleted = true; return discoveredDigits }
}

struct EncryptedNotePuzzleEngine {
    let noteWords = ["기록", "삭제", "수집", "완료", "중", "외부"]
    private(set) var selectedWords: [String] = []
    private(set) var isUnlocked = false
    private(set) var isSolved = false
    mutating func unlock(code: String) -> Bool { isUnlocked = code == "417"; return isUnlocked }
    mutating func selectNoteWord(_ word: String) { guard isUnlocked, !isSolved, noteWords.contains(word), !selectedWords.contains(word) else { return }; selectedWords.append(word) }
    mutating func removeNoteWord() { guard !isSolved, !selectedWords.isEmpty else { return }; selectedWords.removeLast() }
    mutating func removeAllNoteWords() { guard !isSolved else { return }; selectedWords.removeAll() }
    mutating func submitEncryptedNote() -> Bool { isSolved = isUnlocked && selectedWords == ["기록", "수집", "중"]; return isSolved }
    mutating func completeEncryptedNotePuzzle() -> Bool { isUnlocked = true; selectedWords = ["기록", "수집", "중"]; isSolved = true; return true }
}

struct AudioRecordPuzzleEngine {
    static let fragments = [
        AudioFragment(id: "audio_1", subtitle: "이 기능은 승인된 적이 없습니다.", correctOrder: 0),
        AudioFragment(id: "audio_2", subtitle: "이미 위에서 결정됐어.", correctOrder: 1),
        AudioFragment(id: "audio_3", subtitle: "기록을 남기면 너도 책임을 피할 수 없어.", correctOrder: 2),
        AudioFragment(id: "audio_4", subtitle: "출시 전까지 원래대로 되돌려 놔.", correctOrder: 3)
    ]
    private(set) var fragments: [AudioFragment]
    private(set) var playingFragmentId: String?
    private(set) var isSolved = false
    init(fragments: [AudioFragment] = Self.fragments.shuffled()) { self.fragments = MessengerPuzzleEngine.unique(fragments.map { PuzzleMessage(id: $0.id, sender: "", body: $0.subtitle, displayedTime: "", correctOrder: $0.correctOrder) }).map { AudioFragment(id: $0.id, subtitle: $0.body, correctOrder: $0.correctOrder) } }
    mutating func playAudioFragment(_ fragmentId: String) { if fragments.contains(where: { $0.id == fragmentId }) { playingFragmentId = fragmentId } }
    mutating func moveAudioFragmentUp(_ index: Int) { guard !isSolved, fragments.indices.contains(index), index > 0 else { return }; fragments.swapAt(index, index - 1) }
    mutating func moveAudioFragmentDown(_ index: Int) { guard !isSolved, fragments.indices.contains(index), index < fragments.count - 1 else { return }; fragments.swapAt(index, index + 1) }
    mutating func submitAudioOrder() -> Bool { if !isSolved { isSolved = fragments.map(\.correctOrder) == Array(fragments.indices) }; return isSolved }
    mutating func completeAudioRecordPuzzle() -> Bool { fragments = Self.fragments; isSolved = true; return true }
}

struct CommitGraphPuzzleEngine {
    static let defaultNodes = [
        CommitNode(id: "412", number: 412, title: "로그인 오류 수정", correctBranch: "main", correctOrder: 0),
        CommitNode(id: "414", number: 414, title: "출시 후보 빌드", correctBranch: "release", correctOrder: 1),
        CommitNode(id: "416", number: 416, title: "센서 분석 실험", correctBranch: "analytics", correctOrder: 2),
        CommitNode(id: "417", number: 417, title: "백그라운드 수집 활성화", correctBranch: "analytics", correctOrder: 3),
        CommitNode(id: "418", number: 418, title: "작성자 정보 수정", correctBranch: "analytics", correctOrder: 4),
        CommitNode(id: "420", number: 420, title: "출시 빌드 병합", correctBranch: "main", correctOrder: 5)
    ]
    let branches = ["main", "release", "analytics", "hotfix"]
    let nodes = Self.defaultNodes
    private(set) var nodeOrder = Array(Self.defaultNodes.map(\.id).reversed())
    private(set) var placements: [String: String] = [:]
    private(set) var connections: Set<String> = []
    private(set) var isSolved = false
    mutating func moveCommitNode(_ nodeId: String, branch: String) { guard !isSolved, nodes.contains(where: { $0.id == nodeId }), branches.contains(branch) else { return }; placements[nodeId] = branch }
    mutating func moveCommitNode(_ nodeId: String, newIndex: Int) { guard !isSolved, let oldIndex = nodeOrder.firstIndex(of: nodeId), nodeOrder.indices.contains(newIndex) else { return }; let value = nodeOrder.remove(at: oldIndex); nodeOrder.insert(value, at: newIndex) }
    mutating func connectCommitNode(_ fromId: String, toId: String) { guard !isSolved, nodes.contains(where: { $0.id == fromId }), nodes.contains(where: { $0.id == toId }) else { return }; connections.insert("\(fromId)->\(toId)") }
    func validateCommitGraph() -> Bool { nodes.allSatisfy { placements[$0.id] == $0.correctBranch } && Array(nodeOrder) == nodes.sorted { $0.correctOrder < $1.correctOrder }.map(\.id) && connections.contains("417->418") && connections.contains("418->420") }
    mutating func submitCommitGraph() -> Bool { isSolved = validateCommitGraph(); return isSolved }
    mutating func completeCommitGraphPuzzle() -> Bool { placements = Dictionary(uniqueKeysWithValues: nodes.map { ($0.id, $0.correctBranch) }); nodeOrder = nodes.sorted { $0.correctOrder < $1.correctOrder }.map(\.id); connections = ["417->418", "418->420"]; isSolved = true; return true }
}

struct AccessLogPuzzleEngine {
    let correctAnswers = ["없었다", "예약 메시지다", "관리자 공용 계정이다"]
    private(set) var selectedEvidenceIds: Set<String> = []
    private(set) var answers: [Int: String] = [:]
    private(set) var isSolved = false
    mutating func selectLogEvidence(_ evidenceId: String) { selectedEvidenceIds.insert(evidenceId) }
    mutating func answerLogQuestion(_ questionIndex: Int, answer: String) { guard !isSolved, correctAnswers.indices.contains(questionIndex) else { return }; answers[questionIndex] = answer }
    func validateAccessLogAnswers() -> Bool { correctAnswers.indices.allSatisfy { answers[$0] == correctAnswers[$0] } }
    mutating func completeAccessLogPuzzle() -> Bool { isSolved = validateAccessLogAnswers(); return isSolved }
}
