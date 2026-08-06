import Foundation

enum ThemeId: String, Codable, CaseIterable {
    case theLastCommit = "the_last_commit"
    case convenienceStoreLoop = "convenience_store_loop"
}

enum ThemeStatus: String, Codable, CaseIterable { case notStarted, inProgress, completed }

struct ThemeMetadata: Identifiable, Equatable {
    var id: ThemeId { themeId }
    let themeId: ThemeId
    let title: String
    let subtitle: String
    let description: String
    let estimatedPlayTimeMinutes: Int
    let difficulty: String
    let isAvailable: Bool
    let thumbnailResourceName: String
}

enum ThemeSpecificState: Codable, Equatable {
    case theLastCommit(TheLastCommitState)
    case convenienceStoreLoop(ConvenienceStoreLoopState)

    struct TheLastCommitState: Codable, Equatable { var placeholder: Bool = true }
    struct ConvenienceStoreLoopState: Codable, Equatable { var progress: ConvenienceStoreProgress = ConvenienceStoreProgress() }

    private enum CodingKeys: String, CodingKey { case type, value }
    private enum Kind: String, Codable { case theLastCommit, convenienceStoreLoop }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let kind = try container.decode(Kind.self, forKey: .type)
        switch kind {
        case .theLastCommit: self = .theLastCommit(try container.decode(TheLastCommitState.self, forKey: .value))
        case .convenienceStoreLoop: self = .convenienceStoreLoop(try container.decode(ConvenienceStoreLoopState.self, forKey: .value))
        }
    }
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        switch self {
        case .theLastCommit(let state): try container.encode(Kind.theLastCommit, forKey: .type); try container.encode(state, forKey: .value)
        case .convenienceStoreLoop(let state): try container.encode(Kind.convenienceStoreLoop, forKey: .type); try container.encode(state, forKey: .value)
        }
    }
}

struct ThemeProgress: Codable, Equatable {
    var themeId: ThemeId
    var themeStatus: ThemeStatus = .notStarted
    var currentStageId: String = "not_started"
    var startedAt: Date?
    var lastSavedAt: Date?
    var completedAt: Date?
    var hintCount: Int = 0
    var collectedEvidenceIds: Set<String> = []
    var endingId: String?
    var themeSpecificState: ThemeSpecificState
}

enum ThemeScreenIds {
    static let theme_selection = "theme_selection"
}

enum ThemeRegistry {
    static let themes: [ThemeMetadata] = [
        ThemeMetadata(
            themeId: .theLastCommit,
            title: "The Last Commit",
            subtitle: "사라진 개발자의 휴대폰",
            description: "실종된 개발자의 휴대폰에서 조작된 기록과 사건의 진실을 찾아낸다.",
            estimatedPlayTimeMinutes: 30,
            difficulty: "보통",
            isAvailable: true,
            thumbnailResourceName: "theme_the_last_commit"
        ),
        ThemeMetadata(
            themeId: .convenienceStoreLoop,
            title: "02:17",
            subtitle: "반복되는 편의점의 밤",
            description: "새벽 2시 17분이 반복되는 편의점에서 사라진 손님의 흔적을 복원한다.",
            estimatedPlayTimeMinutes: 30,
            difficulty: "보통",
            isAvailable: true,
            thumbnailResourceName: "theme_convenience_store_loop"
        )
    ]

    static func metadata(for themeId: ThemeId) -> ThemeMetadata { themes.first { $0.themeId == themeId }! }

    static func defaultProgress(for themeId: ThemeId) -> ThemeProgress {
        switch themeId {
        case .theLastCommit:
            return ThemeProgress(themeId: themeId, themeSpecificState: .theLastCommit(.init()))
        case .convenienceStoreLoop:
            return ThemeProgress(themeId: themeId, themeSpecificState: .convenienceStoreLoop(.init()))
        }
    }
}
