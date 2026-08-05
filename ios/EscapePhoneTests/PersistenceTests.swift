import XCTest
@testable import EscapePhone

final class PersistenceTests: XCTestCase {
    func testSaveLoadResetAndCodableRoundTrip() throws {
        let suite = "store.\(UUID().uuidString)"; let defaults = UserDefaults(suiteName: suite) ?? .standard
        let store = PlatformGameProgressStore(defaults: defaults, key: "p")
        let start = Date(timeIntervalSince1970: 123); let end = Date(timeIntervalSince1970: 456)
        let gameProgress = GameProgress(currentStage: .gameCompleted, messengerSolved: true, flashlightSolved: true, discoveredDigits: [4, 1, 7], hintCount: 2, usedHints: ["a"], startedAt: start, lastSavedAt: end, completedAt: end, controlMode: .touch)
        try store.save(gameProgress); XCTAssertEqual(try store.load(), gameProgress); XCTAssertEqual(try store.load()?.startedAt, start); XCTAssertEqual(try store.load()?.controlMode, .touch)
        try store.reset(); XCTAssertNil(try store.load()); defaults.removePersistentDomain(forName: suite)
    }
}
