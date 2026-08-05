import XCTest
@testable import EscapePhone

final class FlashlightPuzzleTests: XCTestCase {
    private let first = FlashlightPuzzleEngine.targets[0].normalizedPosition
    func testOutsideTarget() { var engine = FlashlightPuzzleEngine(); XCTAssertNil(engine.update(flashlightPosition: .init(x: 1, y: 1), deltaTime: 1)); XCTAssertTrue(engine.discoveredDigits.isEmpty) }
    func testInsideButUnderDuration() { var engine = FlashlightPuzzleEngine(); XCTAssertNil(engine.update(flashlightPosition: first, deltaTime: 0.1)) }
    func testHoldCompletesTarget() { var engine = FlashlightPuzzleEngine(); var event: FlashlightPuzzleEvent?; for _ in 0..<10 { event = engine.update(flashlightPosition: first, deltaTime: 0.1) ?? event }; XCTAssertEqual(event, .digitDiscovered(4)) }
    func testLeavingResetsTimer() { var engine = FlashlightPuzzleEngine(); for _ in 0..<7 { _ = engine.update(flashlightPosition: first, deltaTime: 0.1) }; _ = engine.update(flashlightPosition: .zero, deltaTime: 0.1); XCTAssertNil(engine.update(flashlightPosition: first, deltaTime: 0.1)); XCTAssertTrue(engine.discoveredDigits.isEmpty) }
    func testWrongTargetIgnored() { var engine = FlashlightPuzzleEngine(); let second = FlashlightPuzzleEngine.targets[1].normalizedPosition; for _ in 0..<12 { _ = engine.update(flashlightPosition: second, deltaTime: 0.1) }; XCTAssertTrue(engine.discoveredDigits.isEmpty) }
    func testFullSequence417() { var engine = FlashlightPuzzleEngine(); var final: FlashlightPuzzleEvent?; for target in FlashlightPuzzleEngine.targets { for _ in 0..<12 { final = engine.update(flashlightPosition: target.normalizedPosition, deltaTime: 0.1) ?? final } }; XCTAssertEqual(engine.discoveredDigits, [4, 1, 7]); XCTAssertEqual(final, .completed) }
    func testCompletedTargetNotDuplicated() { var engine = FlashlightPuzzleEngine(); for _ in 0..<20 { _ = engine.update(flashlightPosition: first, deltaTime: 0.1) }; XCTAssertEqual(engine.discoveredDigits, [4]) }
    func testClamp() { XCTAssertEqual(FlashlightPuzzleEngine.clamp(.init(x: -2, y: 4)), .init(x: 0, y: 1)) }
    func testDistance() { XCTAssertEqual(FlashlightPuzzleEngine.distance(.zero, .init(x: 3, y: 4)), 5, accuracy: 0.0001) }
    func testLargeDeltaIsCapped() { var engine = FlashlightPuzzleEngine(); XCTAssertNil(engine.update(flashlightPosition: first, deltaTime: 100)); XCTAssertTrue(engine.discoveredDigits.isEmpty) }
    func testNegativeDeltaIgnored() { var engine = FlashlightPuzzleEngine(); XCTAssertNil(engine.update(flashlightPosition: first, deltaTime: -1)); XCTAssertTrue(engine.discoveredDigits.isEmpty) }
}
