import XCTest
@testable import EscapePhone

final class ConvenienceStorePuzzleTests: XCTestCase {
    // Receipt
    func testSubmitReceiptAnswer_withCorrectAnomaly_succeeds() {
        var engine = ReceiptPuzzleEngine()
        engine.selectReceiptItem(ConvenienceStoreSpec.receiptAnomalyItemId)
        XCTAssertTrue(engine.submitReceiptAnswer())
    }

    func testSubmitReceiptAnswer_withWrongAnomaly_fails() {
        var engine = ReceiptPuzzleEngine()
        engine.selectReceiptItem("water")
        XCTAssertFalse(engine.submitReceiptAnswer())
    }

    func testCompleteReceiptPuzzle_unlocksBarcodePuzzle() {
        var engine = ReceiptPuzzleEngine()
        XCTAssertTrue(engine.completeReceiptPuzzle())
        XCTAssertTrue(engine.isSolved)
    }

    // Barcode
    func testAssembleBarcodeCode_withCorrectSegments_succeeds() {
        var engine = BarcodePuzzleEngine()
        engine.selectBarcodeSegment("category", value: "99")
        engine.selectBarcodeSegment("shelfPosition", value: "40")
        engine.selectBarcodeSegment("arrivalOrder", value: "01")
        XCTAssertEqual(engine.assembleBarcodeCode(), "994001")
        XCTAssertTrue(engine.submitBarcodeRule())
    }

    func testAssembleBarcodeCode_withWrongSegments_fails() {
        var engine = BarcodePuzzleEngine()
        engine.selectBarcodeSegment("category", value: "12")
        engine.selectBarcodeSegment("shelfPosition", value: "05")
        engine.selectBarcodeSegment("arrivalOrder", value: "03")
        XCTAssertFalse(engine.submitBarcodeRule())
    }

    func testCompleteBarcodePuzzle_unlocksShelfDifferencePuzzle() {
        var engine = BarcodePuzzleEngine()
        XCTAssertTrue(engine.completeBarcodePuzzle())
        XCTAssertTrue(engine.isSolved)
    }

    // Shelf difference
    func testSubmitShelfDifferences_withAllTargets_succeeds() {
        var engine = ShelfDifferencePuzzleEngine()
        for target in ConvenienceStoreSpec.shelfDifferenceTargetIds { engine.selectShelfDifference(target) }
        XCTAssertTrue(engine.submitShelfDifferences())
    }

    func testSubmitShelfDifferences_withMissingTarget_fails() {
        var engine = ShelfDifferencePuzzleEngine()
        for target in ConvenienceStoreSpec.shelfDifferenceTargetIds.dropFirst() { engine.selectShelfDifference(target) }
        XCTAssertFalse(engine.submitShelfDifferences())
    }

    func testTiltObjectPuzzle_reachesDestination() {
        var engine = TiltObjectPuzzleEngine(objectX: 0.5, objectY: 0.5)
        engine.applyTiltInput(deltaX: 0, deltaY: 0.4)
        XCTAssertTrue(engine.isSolved)
    }

    func testTouchObjectPuzzle_reachesDestination() {
        var engine = TiltObjectPuzzleEngine()
        engine.applyTouchInput(x: 0.5, y: 0.9)
        XCTAssertTrue(engine.isSolved)
    }

    // CCTV
    func testSubmitCctvSequence_withCorrectOrder_succeeds() {
        var engine = CctvPuzzleEngine()
        let expected = ConvenienceStoreSpec.cctvRecords.sorted { $0.correctOrder < $1.correctOrder }.map(\.id)
        while engine.recordOrder != expected {
            let misplacedIndex = engine.recordOrder.indices.first { engine.recordOrder[$0] != expected[$0] }!
            let targetIndex = engine.recordOrder.firstIndex(of: expected[misplacedIndex])!
            if targetIndex > misplacedIndex { engine.moveCctvRecordUp(targetIndex) } else { engine.moveCctvRecordDown(targetIndex) }
        }
        XCTAssertTrue(engine.submitCctvSequence())
    }

    func testSubmitCctvSequence_withWrongOrder_fails() {
        var engine = CctvPuzzleEngine()
        XCTAssertFalse(engine.submitCctvSequence())
    }

    func testCompleteCctvPuzzle_unlocksInventoryPuzzle() {
        var engine = CctvPuzzleEngine()
        XCTAssertTrue(engine.completeCctvPuzzle())
        XCTAssertTrue(engine.isSolved)
    }

    // Inventory
    func testCalculateExpectedInventory_returnsCorrectValue() {
        let engine = InventoryPuzzleEngine()
        XCTAssertEqual(engine.calculateExpectedInventory("water"), 3)
        XCTAssertEqual(engine.calculateExpectedInventory("evidence_box"), 3)
    }

    func testSubmitInventoryDiscrepancy_withCorrectItem_succeeds() {
        var engine = InventoryPuzzleEngine()
        engine.selectInventoryItem(ConvenienceStoreSpec.discrepancyItemId)
        XCTAssertTrue(engine.submitInventoryDiscrepancy())
    }

    func testSubmitInventoryDiscrepancy_withWrongItem_fails() {
        var engine = InventoryPuzzleEngine()
        engine.selectInventoryItem("water")
        XCTAssertFalse(engine.submitInventoryDiscrepancy())
    }

    // Customer pattern
    func testDecodePurchasePattern_withCorrectSequence_returnsMessage() {
        let engine = CustomerPatternPuzzleEngine()
        XCTAssertEqual(engine.decodePurchasePattern(), ConvenienceStoreSpec.customerPatternMessage)
    }

    func testSubmitCustomerPattern_withWrongSequence_fails() {
        var engine = CustomerPatternPuzzleEngine()
        XCTAssertFalse(engine.submitCustomerPattern("wrong message"))
    }

    func testCompleteCustomerPatternPuzzle_unlocksTimelinePuzzle() {
        var engine = CustomerPatternPuzzleEngine()
        XCTAssertTrue(engine.completeCustomerPatternPuzzle())
        XCTAssertTrue(engine.isSolved)
    }

    // Timeline
    func testValidateIncidentTimeline_withCorrectOrder_succeeds() {
        var engine = IncidentTimelinePuzzleEngine()
        let expected = ConvenienceStoreSpec.incidentEvents.sorted { $0.correctOrder < $1.correctOrder }.map(\.id)
        while engine.eventOrder != expected {
            let misplacedIndex = engine.eventOrder.indices.first { engine.eventOrder[$0] != expected[$0] }!
            engine.moveIncidentEvent(expected[misplacedIndex], newIndex: misplacedIndex)
        }
        XCTAssertTrue(engine.isOrderCorrect())
    }

    func testValidateIncidentTimeline_withWrongOrder_fails() {
        let engine = IncidentTimelinePuzzleEngine()
        XCTAssertFalse(engine.isOrderCorrect())
    }

    func testValidateIncidentTimeline_withCorrectConclusions_succeeds() {
        var engine = IncidentTimelinePuzzleEngine()
        for event in ConvenienceStoreSpec.incidentEvents { engine.selectIncidentConclusion(event.id) }
        XCTAssertTrue(engine.areConclusionsCorrect())
    }
}
