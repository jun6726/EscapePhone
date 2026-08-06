import CoreGraphics
import Foundation

enum GameStage: String, Codable, CaseIterable { case notStarted, introCompleted, messengerSolved, flashlightSolved, encryptedNoteSolved, audioRecordSolved, commitGraphSolved, accessLogSolved, gameCompleted }

enum FlashlightControlMode: String, Codable, CaseIterable { case motion, touch }
enum EndingType: String, Codable, CaseIterable { case publicDisclosure, encryptedArchive }
enum PuzzleExitReason: String, Codable, CaseIterable { case backNavigation, appBackgroundOrClosed }
enum AnalyticsConsentStatus: String, Codable, CaseIterable { case notDetermined, granted, denied }

struct PuzzleExitEvent: Codable, Equatable {
    let occurredAt: Date
    let reason: PuzzleExitReason
    let elapsedMsAtExit: Int64
}

struct PuzzleAnalytics: Codable, Equatable {
    let puzzleId: String
    var firstStartedAt: Date?
    var completedAt: Date?
    var elapsedMs: Int64 = 0
    var sessionCount = 0
    var wrongAttemptCount = 0
    var wrongReasonCounts: [String: Int] = [:]
    var hintViewCount = 0
    var exitEvents: [PuzzleExitEvent] = []
}

struct PlayerFeedback: Codable, Equatable {
    let difficultyRating: Int
    let comment: String
    let submittedAt: Date
}

struct PlaytestReport: Codable, Equatable {
    let sessionStartedAt: Date?
    let completedAt: Date?
    let endingType: String?
    let puzzleAnalytics: [String: PuzzleAnalytics]
    let playerFeedback: PlayerFeedback?
}

struct PlaytestUploadEnvelope: Codable, Equatable {
    let schemaVersion: Int
    let sessionId: String
    let sequence: Int
    let platform: String
    let appVersion: String
    let consentVersion: Int
    let isFinal: Bool
    let createdAt: Date
    var themeId: String = "the_last_commit"
    let report: PlaytestReport
}

struct PendingAnalyticsUpload: Codable, Equatable, Identifiable {
    let id: String
    let envelope: PlaytestUploadEnvelope
    var attemptCount: Int = 0
}

struct GameProgress: Codable, Equatable {
    var currentStage: GameStage = .notStarted
    var messengerSolved = false
    var flashlightSolved = false
    var encryptedNoteSolved = false
    var audioRecordSolved = false
    var commitGraphSolved = false
    var accessLogSolved = false
    var endingType: EndingType?
    var collectedEvidenceIds: Set<String> = []
    var discoveredDigits: [Int] = []
    var hintCount = 0
    var usedHints: Set<String> = []
    var startedAt: Date?
    var lastSavedAt: Date?
    var completedAt: Date?
    var controlMode: FlashlightControlMode = .motion
    var puzzleAnalytics: [String: PuzzleAnalytics] = [:]
    var playerFeedback: PlayerFeedback?
    var playtestHistory: [PlaytestReport] = []
    var analyticsConsentStatus: AnalyticsConsentStatus = .notDetermined
    var analyticsConsentVersion = 0
    var anonymousSessionId: String?
    var analyticsUploadSequence = 0
    var pendingAnalyticsUploads: [PendingAnalyticsUpload] = []
    var lastAnalyticsUploadAt: Date?
    var lastAnalyticsUploadError: String?

    enum CodingKeys: String, CodingKey { case currentStage, messengerSolved, flashlightSolved, encryptedNoteSolved, audioRecordSolved, commitGraphSolved, accessLogSolved, endingType, collectedEvidenceIds, discoveredDigits, hintCount, usedHints, startedAt, lastSavedAt, completedAt, controlMode, puzzleAnalytics, playerFeedback, playtestHistory, analyticsConsentStatus, analyticsConsentVersion, anonymousSessionId, analyticsUploadSequence, pendingAnalyticsUploads, lastAnalyticsUploadAt, lastAnalyticsUploadError }
    init(currentStage: GameStage = .notStarted, messengerSolved: Bool = false, flashlightSolved: Bool = false, encryptedNoteSolved: Bool = false, audioRecordSolved: Bool = false, commitGraphSolved: Bool = false, accessLogSolved: Bool = false, endingType: EndingType? = nil, collectedEvidenceIds: Set<String> = [], discoveredDigits: [Int] = [], hintCount: Int = 0, usedHints: Set<String> = [], startedAt: Date? = nil, lastSavedAt: Date? = nil, completedAt: Date? = nil, controlMode: FlashlightControlMode = .motion, puzzleAnalytics: [String: PuzzleAnalytics] = [:], playerFeedback: PlayerFeedback? = nil, playtestHistory: [PlaytestReport] = [], analyticsConsentStatus: AnalyticsConsentStatus = .notDetermined, analyticsConsentVersion: Int = 0, anonymousSessionId: String? = nil, analyticsUploadSequence: Int = 0, pendingAnalyticsUploads: [PendingAnalyticsUpload] = [], lastAnalyticsUploadAt: Date? = nil, lastAnalyticsUploadError: String? = nil) {
        self.currentStage = currentStage; self.messengerSolved = messengerSolved; self.flashlightSolved = flashlightSolved; self.encryptedNoteSolved = encryptedNoteSolved; self.audioRecordSolved = audioRecordSolved; self.commitGraphSolved = commitGraphSolved; self.accessLogSolved = accessLogSolved; self.endingType = endingType; self.collectedEvidenceIds = collectedEvidenceIds; self.discoveredDigits = discoveredDigits; self.hintCount = hintCount; self.usedHints = usedHints; self.startedAt = startedAt; self.lastSavedAt = lastSavedAt; self.completedAt = completedAt; self.controlMode = controlMode; self.puzzleAnalytics = puzzleAnalytics; self.playerFeedback = playerFeedback; self.playtestHistory = playtestHistory; self.analyticsConsentStatus = analyticsConsentStatus; self.analyticsConsentVersion = analyticsConsentVersion; self.anonymousSessionId = anonymousSessionId; self.analyticsUploadSequence = analyticsUploadSequence; self.pendingAnalyticsUploads = pendingAnalyticsUploads; self.lastAnalyticsUploadAt = lastAnalyticsUploadAt; self.lastAnalyticsUploadError = lastAnalyticsUploadError
    }
    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        currentStage = try values.decodeIfPresent(GameStage.self, forKey: .currentStage) ?? .notStarted
        messengerSolved = try values.decodeIfPresent(Bool.self, forKey: .messengerSolved) ?? false
        flashlightSolved = try values.decodeIfPresent(Bool.self, forKey: .flashlightSolved) ?? false
        encryptedNoteSolved = try values.decodeIfPresent(Bool.self, forKey: .encryptedNoteSolved) ?? false
        audioRecordSolved = try values.decodeIfPresent(Bool.self, forKey: .audioRecordSolved) ?? false
        commitGraphSolved = try values.decodeIfPresent(Bool.self, forKey: .commitGraphSolved) ?? false
        accessLogSolved = try values.decodeIfPresent(Bool.self, forKey: .accessLogSolved) ?? false
        endingType = try values.decodeIfPresent(EndingType.self, forKey: .endingType)
        collectedEvidenceIds = try values.decodeIfPresent(Set<String>.self, forKey: .collectedEvidenceIds) ?? []
        discoveredDigits = try values.decodeIfPresent([Int].self, forKey: .discoveredDigits) ?? []
        hintCount = try values.decodeIfPresent(Int.self, forKey: .hintCount) ?? 0
        usedHints = try values.decodeIfPresent(Set<String>.self, forKey: .usedHints) ?? []
        startedAt = try values.decodeIfPresent(Date.self, forKey: .startedAt)
        lastSavedAt = try values.decodeIfPresent(Date.self, forKey: .lastSavedAt)
        completedAt = try values.decodeIfPresent(Date.self, forKey: .completedAt)
        controlMode = try values.decodeIfPresent(FlashlightControlMode.self, forKey: .controlMode) ?? .motion
        puzzleAnalytics = try values.decodeIfPresent([String: PuzzleAnalytics].self, forKey: .puzzleAnalytics) ?? [:]
        playerFeedback = try values.decodeIfPresent(PlayerFeedback.self, forKey: .playerFeedback)
        playtestHistory = try values.decodeIfPresent([PlaytestReport].self, forKey: .playtestHistory) ?? []
        analyticsConsentStatus = try values.decodeIfPresent(AnalyticsConsentStatus.self, forKey: .analyticsConsentStatus) ?? .notDetermined
        analyticsConsentVersion = try values.decodeIfPresent(Int.self, forKey: .analyticsConsentVersion) ?? 0
        anonymousSessionId = try values.decodeIfPresent(String.self, forKey: .anonymousSessionId)
        analyticsUploadSequence = try values.decodeIfPresent(Int.self, forKey: .analyticsUploadSequence) ?? 0
        pendingAnalyticsUploads = try values.decodeIfPresent([PendingAnalyticsUpload].self, forKey: .pendingAnalyticsUploads) ?? []
        lastAnalyticsUploadAt = try values.decodeIfPresent(Date.self, forKey: .lastAnalyticsUploadAt)
        lastAnalyticsUploadError = try values.decodeIfPresent(String.self, forKey: .lastAnalyticsUploadError)
    }
}

struct PuzzleMessage: Identifiable, Codable, Equatable {
    let id: String
    let sender: String
    let body: String
    let displayedTime: String
    let correctOrder: Int
}
struct AudioFragment: Identifiable, Codable, Equatable { let id: String; let subtitle: String; let correctOrder: Int }
struct CommitNode: Identifiable, Codable, Equatable { let id: String; let number: Int; let title: String; let correctBranch: String; let correctOrder: Int }

struct FlashlightTarget: Identifiable, Equatable {
    let id: String
    let digit: Int
    let normalizedPosition: CGPoint
    let requiredHoldDuration: TimeInterval
}

enum FlashlightPuzzleEvent: Equatable { case digitDiscovered(Int), completed }

enum ServerCodeResult: Equatable { case incomplete, incorrect, success }

struct ServerCodeEngine {
    let serverCode = "417121"
    private(set) var input = ""
    mutating func append(_ digit: Int) { guard (0...9).contains(digit), input.count < 6 else { return }; input.append(String(digit)) }
    mutating func deleteLast() { if !input.isEmpty { input.removeLast() } }
    mutating func clear() { input = "" }
    var serverCodeInput: String { input }
    var isServerCodeValid: Bool { input == serverCode }
    mutating func appendServerCodeDigit(_ digit: Int) { append(digit) }
    mutating func removeServerCodeDigit() { deleteLast() }
    mutating func clearServerCode() { clear() }
    func submitServerCode() -> ServerCodeResult { submit() }
    func submit() -> ServerCodeResult { input.count < 6 ? .incomplete : (isServerCodeValid ? .success : .incorrect) }
}

enum ConvenienceStoreStage: String, Codable, CaseIterable {
    case notStarted = "not_started"
    case introCompleted = "intro_completed"
    case receiptSolved = "receipt_solved"
    case barcodeSolved = "barcode_solved"
    case shelfDifferenceSolved = "shelf_difference_solved"
    case cctvSolved = "cctv_solved"
    case inventorySolved = "inventory_solved"
    case customerPatternSolved = "customer_pattern_solved"
    case timelineSolved = "timeline_solved"
    case gameCompleted = "game_completed"
}

enum ConvenienceStoreEndingType: String, Codable, CaseIterable { case publicDisclosure, encryptedArchive }
enum TiltControlMode: String, Codable, CaseIterable { case motion, touch }

struct ConvenienceStoreProgress: Codable, Equatable {
    var currentStage: ConvenienceStoreStage = .notStarted
    var receiptSolved = false
    var barcodeSolved = false
    var shelfDifferenceSolved = false
    var cctvSolved = false
    var inventorySolved = false
    var customerPatternSolved = false
    var timelineSolved = false
    var discoveredCodes: [String] = []
    var selectedAnomalyIds: Set<String> = []
    var collectedEvidenceIds: Set<String> = []
    var hintLevels: [String: Int] = [:]
    var controlMode: TiltControlMode = .motion
    var endingType: ConvenienceStoreEndingType?
    var startedAt: Date?
    var completedAt: Date?
    var seenHintIds: Set<String> = []
    var puzzleAnalytics: [String: PuzzleAnalytics] = [:]
    var playerFeedback: PlayerFeedback?
    var playtestHistory: [PlaytestReport] = []
    var analyticsConsentStatus: AnalyticsConsentStatus = .notDetermined
    var analyticsConsentVersion = 0
    var anonymousSessionId: String?
    var analyticsUploadSequence = 0
    var pendingAnalyticsUploads: [PendingAnalyticsUpload] = []
    var lastAnalyticsUploadAt: Date?
    var lastAnalyticsUploadError: String?
}

enum ConvenienceStoreIds {
    static let convenience_store_home = "convenience_store_home"
    static let receipt_price = "receipt_price"
    static let barcode_rule = "barcode_rule"
    static let shelf_difference = "shelf_difference"
    static let cctv_sequence = "cctv_sequence"
    static let inventory_crosscheck = "inventory_crosscheck"
    static let customer_pattern = "customer_pattern"
    static let incident_timeline = "incident_timeline"
    static let convenience_store_final_decision = "convenience_store_final_decision"
}

enum GameIds {
    static let messenger_order = "messenger_order", flashlight_search = "flashlight_search", encrypted_note = "encrypted_note", audio_record = "audio_record", commit_graph = "commit_graph", access_log = "access_log", server_code = "server_code"
    static let main_menu = "main_menu", intro = "intro", phone_home = "phone_home", messenger_puzzle = "messenger_puzzle", flashlight_puzzle = "flashlight_puzzle", encrypted_note_screen = "encrypted_note_screen", audio_record_puzzle = "audio_record_puzzle", commit_graph_puzzle = "commit_graph_puzzle", access_log_puzzle = "access_log_puzzle", server_console = "server_console", final_decision = "final_decision", ending = "ending", settings = "settings", device_analytics = "device_analytics", developer_menu = "developer_menu"
}
