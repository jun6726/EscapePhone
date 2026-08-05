import XCTest
@testable import EscapePhone

@MainActor
final class GameParityScenarioTests: XCTestCase {
    private let fixedDate = Date(timeIntervalSince1970: 1_700_000_000)
    private func makeApp(store: InMemoryGameProgressStore = InMemoryGameProgressStore()) -> AppContainer { AppContainer(store: store, motion: MockMotionController(), haptics: NoOpHapticProvider(), ads: NoOpAdGateway(), device: NoOpPuzzleDeviceConnector(), dateProvider: FixedDateProvider(now: fixedDate)) }
    func test_startNewGame_returnsInitialProgress() { let app = makeApp(); app.startNewGame(); XCTAssertEqual(app.gameProgress.currentStage, .notStarted); XCTAssertEqual(app.gameProgress.startedAt, fixedDate) }
    func test_completeMessengerPuzzle_unlocksPhotoApp() { let app = makeApp(); app.completeMessengerPuzzle(); XCTAssertTrue(app.gameProgress.messengerSolved); XCTAssertTrue(app.canOpen(.flashlightPuzzle)) }
    func test_completeFlashlightPuzzle_unlocksEncryptedNote() { let app = makeApp(); app.completeFlashlightPuzzle(); XCTAssertTrue(app.canOpen(.encryptedNote)) }
    func test_submitServerCode_with417121_completesGame() { let app = makeApp(); app.completeAccessLogPuzzle(); XCTAssertTrue(app.submitServerCode("417121")); XCTAssertEqual(app.gameProgress.currentStage, .gameCompleted) }
    func test_saveAndLoad_restoresGameProgress() { let store = InMemoryGameProgressStore(); let app = makeApp(store: store); app.startNewGame(); app.completeIntro(); XCTAssertEqual(makeApp(store: store).gameProgress.currentStage, .introCompleted) }
    func test_reset_clearsGameProgress() { let store = InMemoryGameProgressStore(gameProgress: GameProgress(startedAt: fixedDate)); let app = makeApp(store: store); app.resetGame(); XCTAssertNil(store.gameProgress) }
}

final class MessengerParityScenarioTests: XCTestCase {
    func test_submitMessengerOrder_withCorrectOrder_succeeds() { var engine = MessengerPuzzleEngine(messages: MessengerPuzzleEngine.messages); XCTAssertTrue(engine.submit()) }
    func test_submitMessengerOrder_withWrongOrder_fails() { var engine = MessengerPuzzleEngine(messages: MessengerPuzzleEngine.messages.reversed()); XCTAssertFalse(engine.submit()) }
    func test_moveMessageUp_atFirstPosition_doesNothing() { var engine = MessengerPuzzleEngine(messages: MessengerPuzzleEngine.messages); engine.moveUp(at: 0); XCTAssertEqual(engine.messages, MessengerPuzzleEngine.messages) }
    func test_moveMessageDown_atLastPosition_doesNothing() { var engine = MessengerPuzzleEngine(messages: MessengerPuzzleEngine.messages); engine.moveDown(at: 3); XCTAssertEqual(engine.messages, MessengerPuzzleEngine.messages) }
}

final class FlashlightParityScenarioTests: XCTestCase {
    private let first = FlashlightPuzzleEngine.targets[0].normalizedPosition
    func test_updateFlashlightPosition_outsideTarget_doesNotDiscoverDigit() { var engine = FlashlightPuzzleEngine(); _ = engine.update(flashlightPosition: .init(x: 1, y: 1), deltaTime: 1); XCTAssertTrue(engine.discoveredDigits.isEmpty) }
    func test_updateFlashlightPosition_insideTarget_beforeDuration_doesNotDiscoverDigit() { var engine = FlashlightPuzzleEngine(); _ = engine.update(flashlightPosition: first, deltaTime: 0.1); XCTAssertTrue(engine.discoveredDigits.isEmpty) }
    func test_updateFlashlightPosition_insideTarget_afterDuration_discoversDigit() { var engine = FlashlightPuzzleEngine(); for _ in 0..<10 { _ = engine.update(flashlightPosition: first, deltaTime: 0.1) }; XCTAssertEqual(engine.discoveredDigits, [4]) }
    func test_updateFlashlightPosition_withinExpandedRecognitionRange_discoversDigit() { var engine = FlashlightPuzzleEngine(); let nearEdge = CGPoint(x: first.x + 0.17, y: first.y); for _ in 0..<10 { _ = engine.update(flashlightPosition: nearEdge, deltaTime: 0.1) }; XCTAssertEqual(engine.discoveredDigits, [4]) }
    func test_updateFlashlightPosition_afterLeavingTarget_resetsProgress() { var engine = FlashlightPuzzleEngine(); for _ in 0..<7 { _ = engine.update(flashlightPosition: first, deltaTime: 0.1) }; _ = engine.update(flashlightPosition: .zero, deltaTime: 0.1); _ = engine.update(flashlightPosition: first, deltaTime: 0.1); XCTAssertTrue(engine.discoveredDigits.isEmpty) }
    func test_completeFlashlightPuzzle_discovers417() { var engine = FlashlightPuzzleEngine(); for target in FlashlightPuzzleEngine.targets { for _ in 0..<10 { _ = engine.update(flashlightPosition: target.normalizedPosition, deltaTime: 0.1) } }; XCTAssertEqual(engine.discoveredDigits, [4, 1, 7]); XCTAssertTrue(engine.isCompleted) }
}

final class ServerParityScenarioTests: XCTestCase {
    func test_submitServerCode_with417121_succeeds() { var engine = ServerCodeEngine(); [4, 1, 7, 1, 2, 1].forEach { engine.appendServerCodeDigit($0) }; XCTAssertEqual(engine.submitServerCode(), .success) }
    func test_submitServerCode_withWrongCode_fails() { var engine = ServerCodeEngine(); [1, 2, 3, 4, 5, 6].forEach { engine.appendServerCodeDigit($0) }; XCTAssertEqual(engine.submitServerCode(), .incorrect) }
    func test_appendServerCodeDigit_overLimit_isIgnored() { var engine = ServerCodeEngine(); [4, 1, 7, 1, 2, 1, 9].forEach { engine.appendServerCodeDigit($0) }; XCTAssertEqual(engine.serverCodeInput, "417121") }
    func test_clearServerCode_removesAllDigits() { var engine = ServerCodeEngine(); engine.appendServerCodeDigit(4); engine.clearServerCode(); XCTAssertTrue(engine.serverCodeInput.isEmpty) }
}
