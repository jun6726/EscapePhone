import XCTest
@testable import EscapePhone

@MainActor
final class GameProgressTests: XCTestCase {
    private let date = Date(timeIntervalSince1970: 1_700_000_000)
    private func makeApp(gameProgress: GameProgress? = nil) -> (AppContainer, InMemoryGameProgressStore) {
        let store = InMemoryGameProgressStore(gameProgress: gameProgress)
        return (AppContainer(store: store, motion: MockMotionController(), haptics: NoOpHapticProvider(), ads: NoOpAdGateway(), device: NoOpPuzzleDeviceConnector(), dateProvider: FixedDateProvider(now: date)), store)
    }
    func testNewGameInitialValues() { let (app, _) = makeApp(); app.startNewGame(); XCTAssertEqual(app.gameProgress.currentStage, .notStarted); XCTAssertEqual(app.gameProgress.startedAt, date); XCTAssertEqual(app.gameProgress.hintCount, 0) }
    func testIntroCompletion() { let (app, _) = makeApp(); app.startNewGame(); app.completeIntro(); XCTAssertEqual(app.gameProgress.currentStage, .introCompleted) }
    func testMessengerUnlocksPhotos() { let (app, _) = makeApp(); app.completeMessengerPuzzle(); XCTAssertTrue(app.gameProgress.messengerSolved); XCTAssertTrue(app.canOpen(.flashlightPuzzle)) }
    func testFlashlightUnlocksEncryptedNote() { let (app, _) = makeApp(); app.completeFlashlightPuzzle(); XCTAssertTrue(app.gameProgress.flashlightSolved); XCTAssertTrue(app.canOpen(.encryptedNote)) }
    func testFinalCodeCompletesGame() { var code = ServerCodeEngine(); [4, 1, 7, 1, 2, 1].forEach { code.append($0) }; let (app, _) = makeApp(); if code.submit() == .success { app.completeGame() }; XCTAssertEqual(app.gameProgress.currentStage, .gameCompleted); XCTAssertEqual(app.gameProgress.completedAt, date) }
    func testCompletedStageIsIdempotent() { let (app, _) = makeApp(); app.completeMessengerPuzzle(); let saved = app.gameProgress; app.completeMessengerPuzzle(); XCTAssertEqual(app.gameProgress, saved) }
    func testReset() { let (app, store) = makeApp(); app.startNewGame(); app.resetGame(); XCTAssertNil(store.gameProgress); XCTAssertFalse(app.hasSavedGame) }
    func testSaveAndRestore() { let (app, store) = makeApp(); app.startNewGame(); app.completeIntro(); let restored = AppContainer(store: store, motion: MockMotionController(), haptics: NoOpHapticProvider(), ads: NoOpAdGateway(), device: NoOpPuzzleDeviceConnector()); XCTAssertEqual(restored.gameProgress.currentStage, .introCompleted) }
    func testHintOnlyCountsOnce() { let (app, _) = makeApp(); app.requestHint("m1"); app.requestHint("m1"); XCTAssertEqual(app.gameProgress.hintCount, 1) }
    func testCorruptedStorageRecoversSafely() {
        let suite = "corrupt.\(UUID().uuidString)"; let defaults = UserDefaults(suiteName: suite) ?? .standard; defaults.set(Data([0xFF]), forKey: "p")
        let app = AppContainer(store: PlatformGameProgressStore(defaults: defaults, key: "p"), motion: MockMotionController(), haptics: NoOpHapticProvider(), ads: NoOpAdGateway(), device: NoOpPuzzleDeviceConnector())
        XCTAssertEqual(app.gameProgress.currentStage, .notStarted); XCTAssertNotNil(app.userMessage); defaults.removePersistentDomain(forName: suite)
    }
}
