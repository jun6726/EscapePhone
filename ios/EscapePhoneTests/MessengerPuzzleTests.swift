import XCTest
@testable import EscapePhone

final class MessengerPuzzleTests: XCTestCase {
    let correct = MessengerPuzzleEngine.messages
    func testCorrectOrder() { var engine = MessengerPuzzleEngine(messages: correct); XCTAssertTrue(engine.submit()) }
    func testIncorrectOrder() { var engine = MessengerPuzzleEngine(messages: correct.reversed()); XCTAssertFalse(engine.submit()) }
    func testMoveUp() { var engine = MessengerPuzzleEngine(messages: correct); let id = engine.messages[1].id; engine.moveUp(at: 1); XCTAssertEqual(engine.messages[0].id, id) }
    func testMoveDown() { var engine = MessengerPuzzleEngine(messages: correct); let id = engine.messages[0].id; engine.moveDown(at: 0); XCTAssertEqual(engine.messages[1].id, id) }
    func testFirstDoesNotMoveUp() { var engine = MessengerPuzzleEngine(messages: correct); engine.moveUp(at: 0); XCTAssertEqual(engine.messages, correct) }
    func testLastDoesNotMoveDown() { var engine = MessengerPuzzleEngine(messages: correct); engine.moveDown(at: 3); XCTAssertEqual(engine.messages, correct) }
    func testDuplicatesRemoved() { let engine = MessengerPuzzleEngine(messages: correct + [correct[0]]); XCTAssertEqual(engine.messages.count, 4) }
    func testSolvedStateDoesNotMutate() { var engine = MessengerPuzzleEngine(messages: correct); XCTAssertTrue(engine.submit()); engine.moveDown(at: 0); XCTAssertEqual(engine.messages, correct); XCTAssertTrue(engine.submit()) }
}
