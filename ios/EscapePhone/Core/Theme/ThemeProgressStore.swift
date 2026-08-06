import Foundation

protocol ThemeProgressStore {
    func loadThemeProgress(themeId: ThemeId) -> ThemeProgress
    func saveThemeProgress(_ themeProgress: ThemeProgress)
    func resetThemeProgress(themeId: ThemeId)
    func resetAllThemeProgress()
    func migrateLegacyProgressIfNeeded()
}

func migrateLegacyGameProgress(_ legacy: GameProgress, now: Date) -> ThemeProgress {
    let status: ThemeStatus
    switch legacy.currentStage {
    case .gameCompleted: status = .completed
    case .notStarted: status = .notStarted
    default: status = .inProgress
    }
    return ThemeProgress(
        themeId: .theLastCommit,
        themeStatus: status,
        currentStageId: legacy.currentStage.rawValue,
        startedAt: legacy.startedAt,
        lastSavedAt: legacy.lastSavedAt ?? now,
        completedAt: legacy.completedAt,
        hintCount: legacy.hintCount,
        collectedEvidenceIds: legacy.collectedEvidenceIds,
        endingId: legacy.endingType?.rawValue,
        themeSpecificState: .theLastCommit(.init())
    )
}

final class PlatformThemeProgressStore: ThemeProgressStore {
    private let defaults: UserDefaults
    private let legacyKey: String
    private let dateProvider: DateProviding
    private let migrationFlagKey = "legacy_migration_done"

    init(defaults: UserDefaults = .standard, legacyKey: String = "escape_phone_game_progress_v1", dateProvider: DateProviding = SystemDateProvider()) {
        self.defaults = defaults; self.legacyKey = legacyKey; self.dateProvider = dateProvider
    }

    func loadThemeProgress(themeId: ThemeId) -> ThemeProgress {
        guard let data = defaults.data(forKey: key(for: themeId)), let decoded = try? JSONDecoder.themeDecoder().decode(ThemeProgress.self, from: data) else {
            return ThemeRegistry.defaultProgress(for: themeId)
        }
        return decoded
    }

    func saveThemeProgress(_ themeProgress: ThemeProgress) {
        guard let data = try? JSONEncoder.themeEncoder().encode(themeProgress) else { return }
        defaults.set(data, forKey: key(for: themeProgress.themeId))
    }

    func resetThemeProgress(themeId: ThemeId) { defaults.removeObject(forKey: key(for: themeId)) }

    func resetAllThemeProgress() {
        for theme in ThemeId.allCases { defaults.removeObject(forKey: key(for: theme)) }
        defaults.set(true, forKey: migrationFlagKey)
    }

    func migrateLegacyProgressIfNeeded() {
        guard !defaults.bool(forKey: migrationFlagKey) else { return }
        defaults.set(true, forKey: migrationFlagKey)
        guard defaults.data(forKey: key(for: .theLastCommit)) == nil else { return }
        guard let legacyData = defaults.data(forKey: legacyKey), let legacy = try? JSONDecoder().decode(GameProgress.self, from: legacyData) else { return }
        saveThemeProgress(migrateLegacyGameProgress(legacy, now: dateProvider.now))
    }

    private func key(for themeId: ThemeId) -> String { "theme_progress_\(themeId.rawValue)" }
}

final class InMemoryThemeProgressStore: ThemeProgressStore {
    private var progressByTheme: [ThemeId: ThemeProgress]
    private var legacyGameProgress: GameProgress?
    private let dateProvider: DateProviding
    private var migrationDone = false

    init(progressByTheme: [ThemeId: ThemeProgress] = [:], legacyGameProgress: GameProgress? = nil, dateProvider: DateProviding = SystemDateProvider()) {
        self.progressByTheme = progressByTheme; self.legacyGameProgress = legacyGameProgress; self.dateProvider = dateProvider
    }

    func loadThemeProgress(themeId: ThemeId) -> ThemeProgress { progressByTheme[themeId] ?? ThemeRegistry.defaultProgress(for: themeId) }
    func saveThemeProgress(_ themeProgress: ThemeProgress) { progressByTheme[themeProgress.themeId] = themeProgress }
    func resetThemeProgress(themeId: ThemeId) { progressByTheme.removeValue(forKey: themeId) }
    func resetAllThemeProgress() { progressByTheme.removeAll(); migrationDone = true }
    func migrateLegacyProgressIfNeeded() {
        guard !migrationDone else { return }
        migrationDone = true
        guard progressByTheme[.theLastCommit] == nil else { return }
        guard let legacy = legacyGameProgress else { return }
        progressByTheme[.theLastCommit] = migrateLegacyGameProgress(legacy, now: dateProvider.now)
    }

    func setLegacyGameProgress(_ gameProgress: GameProgress?) { legacyGameProgress = gameProgress }
}

extension JSONEncoder {
    static func themeEncoder() -> JSONEncoder { let e = JSONEncoder(); e.dateEncodingStrategy = .millisecondsSince1970; return e }
}
extension JSONDecoder {
    static func themeDecoder() -> JSONDecoder { let d = JSONDecoder(); d.dateDecodingStrategy = .millisecondsSince1970; return d }
}
