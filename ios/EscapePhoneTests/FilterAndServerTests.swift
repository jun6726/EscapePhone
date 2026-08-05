import XCTest
@testable import EscapePhone

final class FilterAndServerTests: XCTestCase {
    func testFilterInitialValue() { XCTAssertEqual(LowPassFilter(initialValue: 2).value, 2) }
    func testFilterUpdates() { var filter = LowPassFilter(alpha: 0.2); XCTAssertEqual(filter.update(with: 10), 2, accuracy: 0.0001) }
    func testFilterAlphaClamped() { XCTAssertEqual(LowPassFilter(alpha: 2).alpha, 1); XCTAssertEqual(LowPassFilter(alpha: -1).alpha, 0) }
    func testFilterContinuousUpdates() { var filter = LowPassFilter(alpha: 0.5); _ = filter.update(with: 10); XCTAssertEqual(filter.update(with: 10), 7.5, accuracy: 0.0001) }
    func testFilterRejectsNonFinite() { var filter = LowPassFilter(initialValue: 3); XCTAssertEqual(filter.update(with: .infinity), 3) }
    func testServerSuccess() { var engine = ServerCodeEngine(); [4, 1, 7, 1, 2, 1].forEach { engine.append($0) }; XCTAssertEqual(engine.submit(), .success) }
    func testServerWrongCode() { var engine = ServerCodeEngine(); [1, 2, 3, 4, 5, 6].forEach { engine.append($0) }; XCTAssertEqual(engine.submit(), .incorrect) }
    func testServerIncomplete() { var engine = ServerCodeEngine(); engine.append(4); XCTAssertEqual(engine.submit(), .incomplete) }
    func testServerMaximumLength() { var engine = ServerCodeEngine(); [4, 1, 7, 1, 2, 1, 9].forEach { engine.append($0) }; XCTAssertEqual(engine.input, "417121") }
    func testServerDelete() { var engine = ServerCodeEngine(); engine.append(4); engine.append(1); engine.deleteLast(); XCTAssertEqual(engine.input, "4") }
    func testServerClear() { var engine = ServerCodeEngine(); engine.append(4); engine.clear(); XCTAssertTrue(engine.input.isEmpty) }
}
