import XCTest
@testable import EscapePhone

final class ThemeProgressTests: XCTestCase {
    private struct FixedDateProvider: DateProviding { let now: Date }

    func testMigrateLegacyProgress_createsTheLastCommitProgress() {
        var legacy = GameProgress(currentStage: .messengerSolved, hintCount: 2)
        legacy.startedAt = Date(timeIntervalSince1970: 100)
        let store = InMemoryThemeProgressStore(legacyGameProgress: legacy, dateProvider: FixedDateProvider(now: Date(timeIntervalSince1970: 500)))
        store.migrateLegacyProgressIfNeeded()
        let migrated = store.loadThemeProgress(themeId: .theLastCommit)
        XCTAssertEqual(migrated.themeId, .theLastCommit)
        XCTAssertEqual(migrated.themeStatus, .inProgress)
        XCTAssertEqual(migrated.hintCount, 2)
        XCTAssertEqual(migrated.startedAt, Date(timeIntervalSince1970: 100))
    }

    func testMigrateLegacyProgress_preservesCompletedStage() {
        var legacy = GameProgress(currentStage: .gameCompleted)
        legacy.completedAt = Date(timeIntervalSince1970: 900)
        legacy.endingType = .publicDisclosure
        legacy.collectedEvidenceIds = ["root_m"]
        let store = InMemoryThemeProgressStore(legacyGameProgress: legacy, dateProvider: FixedDateProvider(now: Date(timeIntervalSince1970: 1000)))
        store.migrateLegacyProgressIfNeeded()
        let migrated = store.loadThemeProgress(themeId: .theLastCommit)
        XCTAssertEqual(migrated.themeStatus, .completed)
        XCTAssertEqual(migrated.completedAt, Date(timeIntervalSince1970: 900))
        XCTAssertEqual(migrated.endingId, "publicDisclosure")
        XCTAssertTrue(migrated.collectedEvidenceIds.contains("root_m"))
    }

    func testMigrateLegacyProgress_doesNotRunTwice() {
        let legacy = GameProgress(currentStage: .messengerSolved)
        let store = InMemoryThemeProgressStore(legacyGameProgress: legacy)
        store.migrateLegacyProgressIfNeeded()
        store.resetThemeProgress(themeId: .theLastCommit)
        store.migrateLegacyProgressIfNeeded()
        let progress = store.loadThemeProgress(themeId: .theLastCommit)
        XCTAssertEqual(progress.themeStatus, .notStarted)
        XCTAssertNil(progress.startedAt)
    }

    func testResetThemeProgress_doesNotResetOtherTheme() {
        let store = InMemoryThemeProgressStore()
        var lastCommit = store.loadThemeProgress(themeId: .theLastCommit); lastCommit.hintCount = 3
        store.saveThemeProgress(lastCommit)
        var convenience = store.loadThemeProgress(themeId: .convenienceStoreLoop); convenience.hintCount = 5
        store.saveThemeProgress(convenience)
        store.resetThemeProgress(themeId: .theLastCommit)
        XCTAssertEqual(store.loadThemeProgress(themeId: .theLastCommit).hintCount, 0)
        XCTAssertEqual(store.loadThemeProgress(themeId: .convenienceStoreLoop).hintCount, 5)
    }

    func testResetAllThemeProgress_clearsEveryTheme() {
        let store = InMemoryThemeProgressStore()
        var lastCommit = store.loadThemeProgress(themeId: .theLastCommit); lastCommit.hintCount = 3
        store.saveThemeProgress(lastCommit)
        var convenience = store.loadThemeProgress(themeId: .convenienceStoreLoop); convenience.hintCount = 5
        store.saveThemeProgress(convenience)
        store.resetAllThemeProgress()
        XCTAssertEqual(store.loadThemeProgress(themeId: .theLastCommit).hintCount, 0)
        XCTAssertEqual(store.loadThemeProgress(themeId: .convenienceStoreLoop).hintCount, 0)
    }

    func testThemeSelection_returnsAvailableThemes() {
        let themes = ThemeRegistry.themes
        XCTAssertEqual(themes.count, 2)
        XCTAssertTrue(themes.contains { $0.themeId == .theLastCommit && $0.isAvailable })
    }

    func testThemeProgress_isStoredSeparately() {
        let store = InMemoryThemeProgressStore()
        var lastCommit = store.loadThemeProgress(themeId: .theLastCommit); lastCommit.hintCount = 1
        store.saveThemeProgress(lastCommit)
        XCTAssertNotEqual(store.loadThemeProgress(themeId: .convenienceStoreLoop).hintCount, 1)
    }
}
