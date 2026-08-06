import Foundation
import SwiftUI

enum ConvenienceStoreScreen: Equatable {
    case intro, home, receipt, barcode, shelfDifference, cctv, inventory, customerPattern, incidentTimeline, finalDecision, ending

    var screenId: String {
        switch self {
        case .intro: return "convenience_store_intro"
        case .home: return ConvenienceStoreIds.convenience_store_home
        case .receipt: return ConvenienceStoreIds.receipt_price
        case .barcode: return ConvenienceStoreIds.barcode_rule
        case .shelfDifference: return ConvenienceStoreIds.shelf_difference
        case .cctv: return ConvenienceStoreIds.cctv_sequence
        case .inventory: return ConvenienceStoreIds.inventory_crosscheck
        case .customerPattern: return ConvenienceStoreIds.customer_pattern
        case .incidentTimeline: return ConvenienceStoreIds.incident_timeline
        case .finalDecision: return ConvenienceStoreIds.convenience_store_final_decision
        case .ending: return "convenience_store_ending"
        }
    }

    var puzzleId: String? {
        switch self {
        case .receipt: return ConvenienceStoreIds.receipt_price
        case .barcode: return ConvenienceStoreIds.barcode_rule
        case .shelfDifference: return ConvenienceStoreIds.shelf_difference
        case .cctv: return ConvenienceStoreIds.cctv_sequence
        case .inventory: return ConvenienceStoreIds.inventory_crosscheck
        case .customerPattern: return ConvenienceStoreIds.customer_pattern
        case .incidentTimeline: return ConvenienceStoreIds.incident_timeline
        default: return nil
        }
    }
}

@MainActor
final class ConvenienceStoreContainer: ObservableObject {
    @Published private(set) var progress: ConvenienceStoreProgress
    @Published var currentScreen: ConvenienceStoreScreen
    @Published var hintText: String?
    @Published var notice: String?

    @Published var receiptEngine = ReceiptPuzzleEngine()
    @Published var barcodeEngine = BarcodePuzzleEngine()
    @Published var shelfDifferenceEngine = ShelfDifferencePuzzleEngine()
    @Published var tiltEngine = TiltObjectPuzzleEngine()
    @Published var cctvEngine = CctvPuzzleEngine()
    @Published var inventoryEngine = InventoryPuzzleEngine()
    @Published var customerPatternEngine = CustomerPatternPuzzleEngine()
    @Published var incidentTimelineEngine = IncidentTimelinePuzzleEngine()

    private let themeProgressStore: ThemeProgressStore
    private let dateProvider: DateProviding
    private let analyticsUploader: PlaytestAnalyticsUploader
    private var activePuzzleId: String?
    private var activePuzzleStartedAt: Date?
    private var uploadTask: Task<Void, Never>?

    init(themeProgressStore: ThemeProgressStore, dateProvider: DateProviding = SystemDateProvider(), analyticsUploader: PlaytestAnalyticsUploader = NoOpPlaytestAnalyticsUploader()) {
        self.themeProgressStore = themeProgressStore
        self.dateProvider = dateProvider
        self.analyticsUploader = analyticsUploader
        let theme = themeProgressStore.loadThemeProgress(themeId: .convenienceStoreLoop)
        let loaded: ConvenienceStoreProgress
        if case .convenienceStoreLoop(let state) = theme.themeSpecificState { loaded = state.progress } else { loaded = ConvenienceStoreProgress() }
        progress = loaded
        currentScreen = Self.route(for: loaded)
    }

    private static func route(for progress: ConvenienceStoreProgress) -> ConvenienceStoreScreen {
        switch progress.currentStage {
        case .notStarted: return .intro
        case .introCompleted, .receiptSolved, .barcodeSolved, .shelfDifferenceSolved, .cctvSolved, .inventorySolved, .customerPatternSolved: return .home
        case .timelineSolved: return .finalDecision
        case .gameCompleted: return progress.endingType == nil ? .finalDecision : .ending
        }
    }

    var hasSavedGame: Bool { progress.startedAt != nil }
    var isAnalyticsConsentGranted: Bool { progress.analyticsConsentStatus == .granted }

    func startNewGame() {
        activePuzzleId = nil; activePuzzleStartedAt = nil
        let consent = progress.analyticsConsentStatus
        let history = isAnalyticsConsentGranted ? archivedHistory() : []
        let pending = progress.pendingAnalyticsUploads
        progress = ConvenienceStoreProgress(controlMode: progress.controlMode, playtestHistory: history, analyticsConsentStatus: consent, analyticsConsentVersion: progress.analyticsConsentVersion, anonymousSessionId: isAnalyticsConsentGranted ? UUID().uuidString : nil, pendingAnalyticsUploads: pending)
        progress.startedAt = dateProvider.now
        save()
        currentScreen = .intro
        flushAnalyticsUploads(delayNanoseconds: 1_500_000_000)
    }

    func continueGame() { navigate(Self.route(for: progress)) }

    func completeIntro() {
        if progress.currentStage == .notStarted { progress.currentStage = .introCompleted; if progress.startedAt == nil { progress.startedAt = dateProvider.now } }
        save(); navigate(.home)
    }

    func completeReceiptPuzzle() { guard !progress.receiptSolved else { return }; completePuzzleSession(ConvenienceStoreIds.receipt_price); progress.currentStage = .receiptSolved; progress.receiptSolved = true; progress.discoveredCodes.append("0217"); save() }
    func completeBarcodePuzzle() { guard !progress.barcodeSolved else { return }; completePuzzleSession(ConvenienceStoreIds.barcode_rule); progress.currentStage = .barcodeSolved; progress.barcodeSolved = true; progress.discoveredCodes.append("냉장고 4번 하단"); save() }
    func completeShelfDifferencePuzzle() { guard !progress.shelfDifferenceSolved else { return }; completePuzzleSession(ConvenienceStoreIds.shelf_difference); progress.currentStage = .shelfDifferenceSolved; progress.shelfDifferenceSolved = true; progress.collectedEvidenceIds.insert("shelf_pouch_message"); save() }
    func completeCctvPuzzle() { guard !progress.cctvSolved else { return }; completePuzzleSession(ConvenienceStoreIds.cctv_sequence); progress.currentStage = .cctvSolved; progress.cctvSolved = true; progress.collectedEvidenceIds.insert("central7_trace"); save() }
    func completeInventoryPuzzle() { guard !progress.inventorySolved else { return }; completePuzzleSession(ConvenienceStoreIds.inventory_crosscheck); progress.currentStage = .inventorySolved; progress.inventorySolved = true; progress.collectedEvidenceIds.insert("evidence_box_location"); save() }
    func completeCustomerPatternPuzzle() { guard !progress.customerPatternSolved else { return }; completePuzzleSession(ConvenienceStoreIds.customer_pattern); progress.currentStage = .customerPatternSolved; progress.customerPatternSolved = true; progress.collectedEvidenceIds.insert("customer_pattern_message"); save() }
    func completeIncidentTimelinePuzzle() { guard !progress.timelineSolved else { return }; completePuzzleSession(ConvenienceStoreIds.incident_timeline); progress.currentStage = .timelineSolved; progress.timelineSolved = true; save(); navigate(.finalDecision) }

    func selectPublicDisclosure() { selectFinalDecision(.publicDisclosure) }
    func selectEncryptedArchive() { selectFinalDecision(.encryptedArchive) }
    private func selectFinalDecision(_ endingType: ConvenienceStoreEndingType) {
        progress.currentStage = .gameCompleted; progress.endingType = endingType; if progress.completedAt == nil { progress.completedAt = dateProvider.now }
        save(); navigate(.ending)
    }

    func requestHint(_ hintId: String, text: String) {
        let puzzleId = String(hintId.split(separator: ".").first ?? "")
        if isAnalyticsConsentGranted { updatePuzzleAnalytics(puzzleId) { $0.hintViewCount += 1 } }
        if progress.seenHintIds.insert(hintId).inserted {}
        save(); hintText = text
    }
    func clearHint() { hintText = nil }

    func setControlMode(_ mode: TiltControlMode) { progress.controlMode = mode; save() }

    func navigate(_ screen: ConvenienceStoreScreen) {
        startPuzzleSession(screen.puzzleId)
        currentScreen = screen; notice = nil
    }
    func dismissNotice() { notice = nil }

    func reset() {
        activePuzzleId = nil; activePuzzleStartedAt = nil
        let consent = progress.analyticsConsentStatus; let version = progress.analyticsConsentVersion
        progress = ConvenienceStoreProgress(controlMode: progress.controlMode, analyticsConsentStatus: consent, analyticsConsentVersion: version)
        save()
        currentScreen = .intro
    }

    func startPuzzleSession(_ puzzleId: String?) {
        guard isAnalyticsConsentGranted, let puzzleId, activePuzzleId != puzzleId, progress.puzzleAnalytics[puzzleId]?.completedAt == nil else { return }
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

    func recordWrongAttempt(_ puzzleId: String, reason: String) {
        guard isAnalyticsConsentGranted else { return }
        updatePuzzleAnalytics(puzzleId) { analytics in analytics.wrongAttemptCount += 1; analytics.wrongReasonCounts[reason, default: 0] += 1 }; save()
    }

    func recordPuzzleExit(_ reason: PuzzleExitReason) {
        guard let puzzleId = activePuzzleId else { return }
        let now = dateProvider.now; let added = max(0, Int64(now.timeIntervalSince(activePuzzleStartedAt ?? now) * 1000))
        updatePuzzleAnalytics(puzzleId) { analytics in analytics.elapsedMs += added; analytics.exitEvents.append(PuzzleExitEvent(occurredAt: now, reason: reason, elapsedMsAtExit: analytics.elapsedMs)) }
        activePuzzleId = nil; activePuzzleStartedAt = nil; save()
    }

    @discardableResult func submitPlayerFeedback(_ difficultyRating: Int, comment: String) -> Bool {
        guard isAnalyticsConsentGranted, (1...5).contains(difficultyRating) else { return false }
        progress.playerFeedback = PlayerFeedback(difficultyRating: difficultyRating, comment: String(comment.trimmingCharacters(in: .whitespacesAndNewlines).prefix(1000)), submittedAt: dateProvider.now)
        enqueueAnalyticsUpload(isFinal: true, delayNanoseconds: 0); save(); return true
    }

    func grantAnalyticsConsent() {
        progress.analyticsConsentStatus = .granted; progress.analyticsConsentVersion = 1; progress.anonymousSessionId = progress.anonymousSessionId ?? UUID().uuidString
        save(); startPuzzleSession(currentScreen.puzzleId); enqueueAnalyticsUpload(isFinal: false, delayNanoseconds: 1_500_000_000)
    }
    func denyAnalyticsConsent() {
        activePuzzleId = nil; activePuzzleStartedAt = nil; uploadTask?.cancel()
        progress.analyticsConsentStatus = .denied; progress.analyticsConsentVersion = 1; progress.anonymousSessionId = nil; progress.puzzleAnalytics = [:]; progress.playerFeedback = nil; progress.playtestHistory = []; progress.pendingAnalyticsUploads = []; progress.analyticsUploadSequence = 0; progress.lastAnalyticsUploadAt = nil; progress.lastAnalyticsUploadError = nil
        save()
    }
    func retryPendingAnalyticsUploads() { flushAnalyticsUploads(delayNanoseconds: 1_500_000_000) }
    func retryAnalyticsUploadNow() { if progress.pendingAnalyticsUploads.isEmpty { enqueueAnalyticsUpload(isFinal: progress.playerFeedback != nil, delayNanoseconds: 0) } else { flushAnalyticsUploads(delayNanoseconds: 0) } }
    func handleScenePhaseChange(active: Bool) {
        if active { startPuzzleSession(currentScreen.puzzleId); retryPendingAnalyticsUploads() }
        else { recordPuzzleExit(.appBackgroundOrClosed); if isAnalyticsConsentGranted { enqueueAnalyticsUpload(isFinal: false, delayNanoseconds: 0) }; save() }
    }

    func save() {
        let status: ThemeStatus
        switch progress.currentStage {
        case .gameCompleted: status = .completed
        case .notStarted: status = .notStarted
        default: status = .inProgress
        }
        themeProgressStore.saveThemeProgress(ThemeProgress(
            themeId: .convenienceStoreLoop,
            themeStatus: status,
            currentStageId: progress.currentStage.rawValue,
            startedAt: progress.startedAt,
            lastSavedAt: dateProvider.now,
            completedAt: progress.completedAt,
            hintCount: progress.seenHintIds.count,
            collectedEvidenceIds: progress.collectedEvidenceIds,
            endingId: progress.endingType?.rawValue,
            themeSpecificState: .convenienceStoreLoop(.init(progress: progress))
        ))
    }

    private func updatePuzzleAnalytics(_ puzzleId: String, _ change: (inout PuzzleAnalytics) -> Void) {
        var analytics = progress.puzzleAnalytics[puzzleId] ?? PuzzleAnalytics(puzzleId: puzzleId)
        change(&analytics)
        progress.puzzleAnalytics[puzzleId] = analytics
    }

    private func archivedHistory() -> [PlaytestReport] {
        var history = progress.playtestHistory
        if progress.startedAt != nil, !progress.puzzleAnalytics.isEmpty {
            history.append(PlaytestReport(sessionStartedAt: progress.startedAt, completedAt: progress.completedAt, endingType: progress.endingType?.rawValue, puzzleAnalytics: progress.puzzleAnalytics, playerFeedback: progress.playerFeedback))
        }
        return Array(history.suffix(20))
    }

    private func enqueueAnalyticsUpload(isFinal: Bool, delayNanoseconds: UInt64) {
        guard isAnalyticsConsentGranted else { return }
        let sessionId = progress.anonymousSessionId ?? UUID().uuidString
        let sequence = progress.analyticsUploadSequence + 1
        let report = PlaytestReport(sessionStartedAt: progress.startedAt, completedAt: progress.completedAt, endingType: progress.endingType?.rawValue, puzzleAnalytics: progress.puzzleAnalytics, playerFeedback: progress.playerFeedback)
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.2"
        let envelope = PlaytestUploadEnvelope(schemaVersion: 1, sessionId: sessionId, sequence: sequence, platform: "ios", appVersion: version, consentVersion: progress.analyticsConsentVersion, isFinal: isFinal, createdAt: dateProvider.now, themeId: ThemeId.convenienceStoreLoop.rawValue, report: report)
        progress.anonymousSessionId = sessionId; progress.analyticsUploadSequence = sequence; progress.pendingAnalyticsUploads = Array((progress.pendingAnalyticsUploads + [PendingAnalyticsUpload(id: "\(sessionId)-\(sequence)", envelope: envelope)]).suffix(20)); save(); flushAnalyticsUploads(delayNanoseconds: delayNanoseconds)
    }

    private func flushAnalyticsUploads(delayNanoseconds: UInt64) {
        guard isAnalyticsConsentGranted, analyticsUploader.isConfigured, uploadTask == nil else { return }
        uploadTask = Task { [weak self] in
            if delayNanoseconds > 0 { try? await Task.sleep(nanoseconds: delayNanoseconds) }
            guard let self else { return }
            let encoder = JSONEncoder(); encoder.dateEncodingStrategy = .millisecondsSince1970
            while self.isAnalyticsConsentGranted, let pending = self.progress.pendingAnalyticsUploads.first {
                guard let data = try? encoder.encode(pending.envelope) else { self.progress.pendingAnalyticsUploads.removeFirst(); self.save(); continue }
                let success = await self.analyticsUploader.upload(data)
                if success { self.progress.pendingAnalyticsUploads.removeFirst(); self.progress.lastAnalyticsUploadAt = self.dateProvider.now; self.progress.lastAnalyticsUploadError = nil; self.save() }
                else { self.progress.pendingAnalyticsUploads[0].attemptCount += 1; self.progress.lastAnalyticsUploadError = "서버 연결 또는 응답 실패"; self.save(); break }
            }
            self.uploadTask = nil
        }
    }
}
