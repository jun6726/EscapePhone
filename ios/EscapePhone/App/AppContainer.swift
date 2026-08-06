import Combine
import Foundation
import SwiftUI

enum AppLaunchRoute: Equatable { case themeSelection, themeMainMenu(ThemeId), theLastCommit, convenienceStorePlaying }

@MainActor
final class AppContainer: ObservableObject {
    @Published private(set) var gameProgress: GameProgress
    @Published var path: [AppRoute] = []
    @Published var userMessage: String?
    @Published var launchRoute: AppLaunchRoute = .themeSelection
    @Published var themeProgressVersion: Int = 0
    let store: GameProgressStore
    let themeProgressStore: ThemeProgressStore
    let motion: MotionController
    let haptics: HapticProvider
    let ads: AdGateway
    let device: PuzzleDeviceConnector
    private(set) lazy var convenienceStoreContainer: ConvenienceStoreContainer = ConvenienceStoreContainer(themeProgressStore: themeProgressStore, dateProvider: dateProvider, analyticsUploader: analyticsUploader)
    private let dateProvider: DateProviding
    private let analyticsUploader: PlaytestAnalyticsUploader
    private var activePuzzleId: String?
    private var activePuzzleStartedAt: Date?
    private var uploadTask: Task<Void, Never>?

    init(store: GameProgressStore, themeProgressStore: ThemeProgressStore = InMemoryThemeProgressStore(), motion: MotionController, haptics: HapticProvider, ads: AdGateway, device: PuzzleDeviceConnector, dateProvider: DateProviding = SystemDateProvider(), analyticsUploader: PlaytestAnalyticsUploader = NoOpPlaytestAnalyticsUploader()) {
        self.store = store; self.themeProgressStore = themeProgressStore; self.motion = motion; self.haptics = haptics; self.ads = ads; self.device = device; self.dateProvider = dateProvider; self.analyticsUploader = analyticsUploader
        do { gameProgress = try store.load() ?? GameProgress() }
        catch { gameProgress = GameProgress(); userMessage = "저장된 진행 정보가 손상되었습니다. 안전한 초기 상태로 시작합니다." }
        if !motion.isAvailable, gameProgress.controlMode == .motion { gameProgress.controlMode = .touch; save() }
        themeProgressStore.migrateLegacyProgressIfNeeded()
    }
    static func live() -> AppContainer { AppContainer(store: PlatformGameProgressStore(), themeProgressStore: PlatformThemeProgressStore(), motion: IOSMotionController(), haptics: PlatformHapticProvider(), ads: FakeAdGateway(), device: NoOpPuzzleDeviceConnector(), analyticsUploader: HTTPPlaytestAnalyticsUploader()) }
    static func preview(gameProgress: GameProgress = GameProgress()) -> AppContainer { AppContainer(store: InMemoryGameProgressStore(gameProgress: gameProgress), motion: MockMotionController(), haptics: NoOpHapticProvider(), ads: NoOpAdGateway(), device: NoOpPuzzleDeviceConnector()) }

    func themeMetadataList() -> [ThemeMetadata] { ThemeRegistry.themes }
    func themeProgress(for themeId: ThemeId) -> ThemeProgress { themeProgressStore.loadThemeProgress(themeId: themeId) }
    func openThemeDetails(_ themeId: ThemeId) -> AppLaunchRoute {
        switch themeId {
        case .theLastCommit: return .theLastCommit
        case .convenienceStoreLoop: return .themeMainMenu(themeId)
        }
    }
    func selectTheme(_ themeId: ThemeId) {
        switch themeId {
        case .theLastCommit: if hasSavedGame { continueGame() } else { path = [] }
        case .convenienceStoreLoop: break
        }
        launchRoute = openThemeDetails(themeId)
    }
    func startTheme(_ themeId: ThemeId) {
        switch themeId {
        case .theLastCommit: startNewGame()
        case .convenienceStoreLoop: break
        }
        launchRoute = openThemeDetails(themeId)
    }
    func continueTheme(_ themeId: ThemeId) {
        switch themeId {
        case .theLastCommit:
            path = []
            launchRoute = openThemeDetails(themeId)
            if hasSavedGame {
                // NavigationStack이 빈 path로 먼저 루트 레이아웃을 안정시킨 뒤 목적지로
                // 이동해야 large title과 목적지 화면의 부제목이 같은 최초 렌더 패스에서
                // 동시에 계산되며 겹쳐 보이는 현상을 피할 수 있다.
                DispatchQueue.main.async { [weak self] in self?.continueGame() }
            }
            return
        case .convenienceStoreLoop: break
        }
        launchRoute = openThemeDetails(themeId)
    }
    func enterConvenienceStore() { launchRoute = .convenienceStorePlaying }
    func openThemeSettings() { path = [.settings]; launchRoute = .theLastCommit }
    func resetSelectedTheme(_ themeId: ThemeId) {
        themeProgressStore.resetThemeProgress(themeId: themeId)
        switch themeId {
        case .theLastCommit: resetGame()
        case .convenienceStoreLoop: convenienceStoreContainer.reset()
        }
        themeProgressVersion += 1
    }
    func resetAllThemeProgress() {
        themeProgressStore.resetAllThemeProgress()
        resetGame()
        convenienceStoreContainer.reset()
        themeProgressVersion += 1
    }
    func returnToThemeSelection() {
        syncTheLastCommitThemeProgress()
        themeProgressVersion += 1
        launchRoute = .themeSelection
    }
    private func syncTheLastCommitThemeProgress() {
        let status: ThemeStatus
        switch gameProgress.currentStage {
        case .gameCompleted: status = .completed
        case .notStarted: status = .notStarted
        default: status = .inProgress
        }
        themeProgressStore.saveThemeProgress(ThemeProgress(
            themeId: .theLastCommit,
            themeStatus: status,
            currentStageId: gameProgress.currentStage.rawValue,
            startedAt: gameProgress.startedAt,
            lastSavedAt: gameProgress.lastSavedAt,
            completedAt: gameProgress.completedAt,
            hintCount: gameProgress.hintCount,
            collectedEvidenceIds: gameProgress.collectedEvidenceIds,
            endingId: gameProgress.endingType?.rawValue,
            themeSpecificState: .theLastCommit(.init())
        ))
    }

    var hasSavedGame: Bool { gameProgress.startedAt != nil || gameProgress.currentStage != .notStarted }
    func startNewGame() { activePuzzleId = nil; activePuzzleStartedAt = nil; let consent = gameProgress.analyticsConsentStatus; let history = isAnalyticsConsentGranted ? archivedHistory() : []; let pending = gameProgress.pendingAnalyticsUploads; gameProgress = GameProgress(controlMode: motion.isAvailable ? .motion : .touch, playtestHistory: history, analyticsConsentStatus: consent, analyticsConsentVersion: gameProgress.analyticsConsentVersion, anonymousSessionId: isAnalyticsConsentGranted ? UUID().uuidString : nil, pendingAnalyticsUploads: pending); gameProgress.startedAt = dateProvider.now; save(); path = [.intro]; flushAnalyticsUploads(delayNanoseconds: 1_500_000_000) }
    func completeIntro() { guard gameProgress.currentStage == .notStarted else { continueGame() ; return }; gameProgress.currentStage = .introCompleted; if gameProgress.startedAt == nil { gameProgress.startedAt = dateProvider.now }; save(); path = [.phoneHome] }
    func completeMessengerPuzzle() { guard !gameProgress.messengerSolved else { return }; completePuzzleSession("messenger_order"); gameProgress.messengerSolved = true; gameProgress.currentStage = .messengerSolved; save() }
    func recordDigit(_ digit: Int) { guard [4, 1, 7].contains(digit), !gameProgress.discoveredDigits.contains(digit) else { return }; gameProgress.discoveredDigits.append(digit); save() }
    func completeFlashlightPuzzle() { guard !gameProgress.flashlightSolved else { return }; completePuzzleSession("flashlight_search"); gameProgress.flashlightSolved = true; gameProgress.currentStage = .flashlightSolved; gameProgress.discoveredDigits = [4, 1, 7]; save() }
    func completeEncryptedNotePuzzle() { guard !gameProgress.encryptedNoteSolved else { return }; completePuzzleSession("encrypted_note"); gameProgress.encryptedNoteSolved = true; gameProgress.currentStage = .encryptedNoteSolved; gameProgress.collectedEvidenceIds.insert("note_trace_collection"); save() }
    func completeAudioRecordPuzzle() { guard !gameProgress.audioRecordSolved else { return }; completePuzzleSession("audio_record"); gameProgress.audioRecordSolved = true; gameProgress.currentStage = .audioRecordSolved; gameProgress.collectedEvidenceIds.insert("audio_restore_order"); save() }
    func completeCommitGraphPuzzle() { guard !gameProgress.commitGraphSolved else { return }; completePuzzleSession("commit_graph"); gameProgress.commitGraphSolved = true; gameProgress.currentStage = .commitGraphSolved; gameProgress.collectedEvidenceIds.formUnion(["root_m", "message_0114"]); save() }
    func completeAccessLogPuzzle(evidenceIds: Set<String> = []) { guard !gameProgress.accessLogSolved else { return }; completePuzzleSession("access_log"); gameProgress.accessLogSolved = true; gameProgress.currentStage = .accessLogSolved; gameProgress.collectedEvidenceIds.formUnion(evidenceIds); gameProgress.collectedEvidenceIds.insert("cto_meeting_terminal"); save() }
    func completeGame() { guard gameProgress.currentStage != .gameCompleted else { return }; gameProgress.currentStage = .gameCompleted; gameProgress.completedAt = dateProvider.now; save() }
    func selectPublicDisclosure() { selectEnding(.publicDisclosure) }
    func selectEncryptedArchive() { selectEnding(.encryptedArchive) }
    private func selectEnding(_ endingType: EndingType) { gameProgress.endingType = endingType; if gameProgress.currentStage == .gameCompleted { save() } else { completeGame() }; path.append(.ending) }
    func requestHint(_ key: String) { let puzzleId = String(key.split(separator: ".").first ?? ""); if isAnalyticsConsentGranted { updatePuzzleAnalytics(puzzleId) { $0.hintViewCount += 1 } }; if gameProgress.usedHints.insert(key).inserted { gameProgress.hintCount += 1 }; save() }
    func startPuzzleSession(_ puzzleId: String?) {
        guard isAnalyticsConsentGranted, let puzzleId, activePuzzleId != puzzleId, gameProgress.puzzleAnalytics[puzzleId]?.completedAt == nil else { return }
        if activePuzzleId != nil { recordPuzzleExit(.backNavigation) }
        let now = dateProvider.now; activePuzzleId = puzzleId; activePuzzleStartedAt = now
        updatePuzzleAnalytics(puzzleId) { analytics in analytics.firstStartedAt = analytics.firstStartedAt ?? now; analytics.sessionCount += 1 }; save()
    }
    func completePuzzleSession(_ puzzleId: String) {
        guard isAnalyticsConsentGranted else { return }
        let now = dateProvider.now; let added = activePuzzleId == puzzleId ? max(0, Int64(now.timeIntervalSince(activePuzzleStartedAt ?? now) * 1000)) : 0
        updatePuzzleAnalytics(puzzleId) { analytics in analytics.completedAt = analytics.completedAt ?? now; analytics.elapsedMs += added }
        if activePuzzleId == puzzleId { activePuzzleId = nil; activePuzzleStartedAt = nil }
        enqueueAnalyticsUpload(isFinal: false, delayNanoseconds: 2_000_000_000)
    }
    func recordWrongAttempt(_ puzzleId: String, reason: String) { guard isAnalyticsConsentGranted else { return }; updatePuzzleAnalytics(puzzleId) { analytics in analytics.wrongAttemptCount += 1; analytics.wrongReasonCounts[reason, default: 0] += 1 }; save() }
    func recordPuzzleExit(_ reason: PuzzleExitReason) {
        guard let puzzleId = activePuzzleId else { return }; let now = dateProvider.now; let added = max(0, Int64(now.timeIntervalSince(activePuzzleStartedAt ?? now) * 1000))
        updatePuzzleAnalytics(puzzleId) { analytics in analytics.elapsedMs += added; analytics.exitEvents.append(PuzzleExitEvent(occurredAt: now, reason: reason, elapsedMsAtExit: analytics.elapsedMs)) }
        activePuzzleId = nil; activePuzzleStartedAt = nil; save()
    }
    @discardableResult func submitPlayerFeedback(_ difficultyRating: Int, comment: String) -> Bool { guard isAnalyticsConsentGranted, (1...5).contains(difficultyRating) else { return false }; gameProgress.playerFeedback = PlayerFeedback(difficultyRating: difficultyRating, comment: String(comment.trimmingCharacters(in: .whitespacesAndNewlines).prefix(1000)), submittedAt: dateProvider.now); enqueueAnalyticsUpload(isFinal: true, delayNanoseconds: 0); save(); return true }
    func exportPlaytestReport() -> String { let report = PlaytestReport(sessionStartedAt: gameProgress.startedAt, completedAt: gameProgress.completedAt, endingType: gameProgress.endingType?.rawValue, puzzleAnalytics: gameProgress.puzzleAnalytics, playerFeedback: gameProgress.playerFeedback); let encoder = JSONEncoder(); encoder.outputFormatting = [.prettyPrinted, .sortedKeys]; return (try? encoder.encode(report)).flatMap { String(data: $0, encoding: .utf8) } ?? "{}" }
    func setControlMode(_ mode: FlashlightControlMode) { gameProgress.controlMode = mode == .motion && !motion.isAvailable ? .touch : mode; save() }
    func resetGame() { activePuzzleId = nil; activePuzzleStartedAt = nil; let consent = gameProgress.analyticsConsentStatus; let version = gameProgress.analyticsConsentVersion; motion.stop(); try? store.reset(); gameProgress = GameProgress(controlMode: motion.isAvailable ? .motion : .touch, analyticsConsentStatus: consent, analyticsConsentVersion: version); path = [] }
    func continueGame() { path = [routeForProgress()] }
    func routeForProgress() -> AppRoute {
        switch gameProgress.currentStage { case .notStarted: return .intro; case .introCompleted, .messengerSolved: return .phoneHome; case .flashlightSolved: return .encryptedNote; case .encryptedNoteSolved: return .audioRecordPuzzle; case .audioRecordSolved: return .commitGraphPuzzle; case .commitGraphSolved: return .accessLogPuzzle; case .accessLogSolved: return .serverConsole; case .gameCompleted: return gameProgress.endingType == nil ? .finalDecision : .ending }
    }
    func canOpen(_ route: AppRoute) -> Bool {
        switch route { case .flashlightPuzzle: return gameProgress.messengerSolved; case .encryptedNote: return gameProgress.flashlightSolved; case .audioRecordPuzzle: return gameProgress.encryptedNoteSolved; case .commitGraphPuzzle: return gameProgress.audioRecordSolved; case .accessLogPuzzle: return gameProgress.commitGraphSolved; case .serverConsole: return gameProgress.accessLogSolved; case .finalDecision: return gameProgress.currentStage == .gameCompleted; case .ending: return gameProgress.currentStage == .gameCompleted && gameProgress.endingType != nil; default: return true }
    }
    func navigate(_ route: AppRoute) { guard canOpen(route) else { userMessage = "아직 잠겨 있습니다. 앞선 기록부터 복구하세요."; return }; path.append(route) }
    func handleRouteChange(from oldRoute: AppRoute?, to newRoute: AppRoute?) { if oldRoute?.puzzleId != newRoute?.puzzleId, activePuzzleId != nil { recordPuzzleExit(.backNavigation) }; startPuzzleSession(newRoute?.puzzleId) }
    func save() { gameProgress.lastSavedAt = dateProvider.now; do { try store.save(gameProgress) } catch { userMessage = "진행 정보를 저장하지 못했습니다." } }
    func handleScenePhase(_ phase: ScenePhase) { if phase == .active { startPuzzleSession(path.last?.puzzleId); retryPendingAnalyticsUploads() } else { recordPuzzleExit(.appBackgroundOrClosed); if isAnalyticsConsentGranted { enqueueAnalyticsUpload(isFinal: false, delayNanoseconds: 0) }; motion.stop(); save() } }
    var isAnalyticsConsentGranted: Bool { gameProgress.analyticsConsentStatus == .granted }
    func grantAnalyticsConsent() { gameProgress.analyticsConsentStatus = .granted; gameProgress.analyticsConsentVersion = 1; gameProgress.anonymousSessionId = gameProgress.anonymousSessionId ?? UUID().uuidString; save(); startPuzzleSession(path.last?.puzzleId); enqueueAnalyticsUpload(isFinal: false, delayNanoseconds: 1_500_000_000) }
    func denyAnalyticsConsent() { activePuzzleId = nil; activePuzzleStartedAt = nil; uploadTask?.cancel(); gameProgress.analyticsConsentStatus = .denied; gameProgress.analyticsConsentVersion = 1; gameProgress.anonymousSessionId = nil; gameProgress.puzzleAnalytics = [:]; gameProgress.playerFeedback = nil; gameProgress.playtestHistory = []; gameProgress.pendingAnalyticsUploads = []; gameProgress.analyticsUploadSequence = 0; gameProgress.lastAnalyticsUploadAt = nil; gameProgress.lastAnalyticsUploadError = nil; save() }
    func retryPendingAnalyticsUploads() { flushAnalyticsUploads(delayNanoseconds: 1_500_000_000) }
    func retryAnalyticsUploadNow() { if gameProgress.pendingAnalyticsUploads.isEmpty { enqueueAnalyticsUpload(isFinal: gameProgress.playerFeedback != nil, delayNanoseconds: 0) } else { flushAnalyticsUploads(delayNanoseconds: 0) } }
    private func updatePuzzleAnalytics(_ puzzleId: String, _ change: (inout PuzzleAnalytics) -> Void) { var analytics = gameProgress.puzzleAnalytics[puzzleId] ?? PuzzleAnalytics(puzzleId: puzzleId); change(&analytics); gameProgress.puzzleAnalytics[puzzleId] = analytics }
    private func archivedHistory() -> [PlaytestReport] { var history = gameProgress.playtestHistory; if gameProgress.startedAt != nil, !gameProgress.puzzleAnalytics.isEmpty { history.append(PlaytestReport(sessionStartedAt: gameProgress.startedAt, completedAt: gameProgress.completedAt, endingType: gameProgress.endingType?.rawValue, puzzleAnalytics: gameProgress.puzzleAnalytics, playerFeedback: gameProgress.playerFeedback)) }; return Array(history.suffix(20)) }
    private func enqueueAnalyticsUpload(isFinal: Bool, delayNanoseconds: UInt64) {
        guard isAnalyticsConsentGranted else { return }
        let sessionId = gameProgress.anonymousSessionId ?? UUID().uuidString; let sequence = gameProgress.analyticsUploadSequence + 1
        let report = PlaytestReport(sessionStartedAt: gameProgress.startedAt, completedAt: gameProgress.completedAt, endingType: gameProgress.endingType?.rawValue, puzzleAnalytics: gameProgress.puzzleAnalytics, playerFeedback: gameProgress.playerFeedback)
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.2"
        let envelope = PlaytestUploadEnvelope(schemaVersion: 1, sessionId: sessionId, sequence: sequence, platform: "ios", appVersion: version, consentVersion: gameProgress.analyticsConsentVersion, isFinal: isFinal, createdAt: dateProvider.now, report: report)
        gameProgress.anonymousSessionId = sessionId; gameProgress.analyticsUploadSequence = sequence; gameProgress.pendingAnalyticsUploads = Array((gameProgress.pendingAnalyticsUploads + [PendingAnalyticsUpload(id: "\(sessionId)-\(sequence)", envelope: envelope)]).suffix(20)); save(); flushAnalyticsUploads(delayNanoseconds: delayNanoseconds)
    }
    private func flushAnalyticsUploads(delayNanoseconds: UInt64) {
        guard isAnalyticsConsentGranted, analyticsUploader.isConfigured, uploadTask == nil else { return }
        uploadTask = Task { [weak self] in
            if delayNanoseconds > 0 { try? await Task.sleep(nanoseconds: delayNanoseconds) }
            guard let self else { return }
            let encoder = JSONEncoder(); encoder.dateEncodingStrategy = .millisecondsSince1970
            while self.isAnalyticsConsentGranted, let pending = self.gameProgress.pendingAnalyticsUploads.first {
                guard let data = try? encoder.encode(pending.envelope) else { self.gameProgress.pendingAnalyticsUploads.removeFirst(); self.save(); continue }
                let success = await self.analyticsUploader.upload(data)
                if success { self.gameProgress.pendingAnalyticsUploads.removeFirst(); self.gameProgress.lastAnalyticsUploadAt = self.dateProvider.now; self.gameProgress.lastAnalyticsUploadError = nil; self.save() }
                else { self.gameProgress.pendingAnalyticsUploads[0].attemptCount += 1; self.gameProgress.lastAnalyticsUploadError = "서버 연결 또는 응답 실패"; self.save(); break }
            }
            self.uploadTask = nil
        }
    }
}
