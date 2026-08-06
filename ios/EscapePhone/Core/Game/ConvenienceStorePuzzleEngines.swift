import CoreGraphics
import Foundation

struct ReceiptPuzzleEngine {
    private(set) var selectedItemId: String?
    private(set) var isSolved = false
    mutating func selectReceiptItem(_ itemId: String) { guard !isSolved else { return }; selectedItemId = itemId }
    func compareReceiptPrice(_ itemId: String) -> Bool { ConvenienceStoreSpec.receiptItems.contains { $0.id == itemId && $0.code == ConvenienceStoreSpec.receiptAnomalyCode } }
    mutating func submitReceiptAnswer() -> Bool {
        guard !isSolved else { return true }
        isSolved = selectedItemId == ConvenienceStoreSpec.receiptAnomalyItemId
        return isSolved
    }
    mutating func completeReceiptPuzzle() -> Bool { selectedItemId = ConvenienceStoreSpec.receiptAnomalyItemId; isSolved = true; return true }
}

struct BarcodePuzzleEngine {
    private(set) var selectedSegments: [String: String] = [:]
    private(set) var isSolved = false
    mutating func selectBarcodeSegment(_ segment: String, value: String) { guard !isSolved else { return }; selectedSegments[segment] = value }
    func assembleBarcodeCode() -> String { ConvenienceStoreSpec.barcodeSegmentOrder.map { selectedSegments[$0] ?? "" }.joined() }
    mutating func submitBarcodeRule() -> Bool {
        guard !isSolved else { return true }
        isSolved = selectedSegments["category"] == "99" && selectedSegments["shelfPosition"] == "40" && selectedSegments["arrivalOrder"] == "01"
        return isSolved
    }
    mutating func completeBarcodePuzzle() -> Bool { selectedSegments = ["category": "99", "shelfPosition": "40", "arrivalOrder": "01"]; isSolved = true; return true }
}

struct TiltObjectPuzzleEngine {
    private(set) var objectX: CGFloat
    private(set) var objectY: CGFloat
    private let destinationX: CGFloat
    private let destinationY: CGFloat
    private let destinationRadius: CGFloat
    private(set) var isSolved = false

    init(objectX: CGFloat = 0.5, objectY: CGFloat = 0.5, destinationX: CGFloat = 0.5, destinationY: CGFloat = 0.9, destinationRadius: CGFloat = 0.12) {
        self.objectX = objectX; self.objectY = objectY; self.destinationX = destinationX; self.destinationY = destinationY; self.destinationRadius = destinationRadius
    }
    mutating func applyTiltInput(deltaX: CGFloat, deltaY: CGFloat) { updateObjectPosition(x: objectX + deltaX, y: objectY + deltaY) }
    mutating func applyTouchInput(x: CGFloat, y: CGFloat) { updateObjectPosition(x: x, y: y) }
    mutating func updateObjectPosition(x: CGFloat, y: CGFloat) {
        objectX = min(max(x, 0), 1); objectY = min(max(y, 0), 1)
        let distance = hypot(objectX - destinationX, objectY - destinationY)
        if distance <= destinationRadius { isSolved = true }
    }
}

struct ShelfDifferencePuzzleEngine {
    private(set) var selectedDifferenceIds: [String] = []
    private(set) var isSolved = false
    mutating func selectShelfDifference(_ differenceId: String) { guard !isSolved, !selectedDifferenceIds.contains(differenceId) else { return }; selectedDifferenceIds.append(differenceId) }
    mutating func submitShelfDifferences() -> Bool {
        guard !isSolved else { return true }
        isSolved = ConvenienceStoreSpec.shelfDifferenceTargetIds.allSatisfy { selectedDifferenceIds.contains($0) }
        return isSolved
    }
    mutating func completeShelfDifferencePuzzle() -> Bool { selectedDifferenceIds = ConvenienceStoreSpec.shelfDifferenceTargetIds; isSolved = true; return true }
}

struct CctvPuzzleEngine {
    private(set) var recordOrder: [String] = Array(ConvenienceStoreSpec.cctvRecords.map(\.id).reversed())
    private(set) var playingRecordId: String?
    private(set) var isSolved = false
    mutating func playCctvRecord(_ recordId: String) { if ConvenienceStoreSpec.cctvRecords.contains(where: { $0.id == recordId }) { playingRecordId = recordId } }
    mutating func moveCctvRecordUp(_ index: Int) { guard !isSolved, recordOrder.indices.contains(index), index > 0 else { return }; recordOrder.swapAt(index, index - 1) }
    mutating func moveCctvRecordDown(_ index: Int) { guard !isSolved, recordOrder.indices.contains(index), index < recordOrder.count - 1 else { return }; recordOrder.swapAt(index, index + 1) }
    mutating func submitCctvSequence() -> Bool {
        guard !isSolved else { return true }
        let expected = ConvenienceStoreSpec.cctvRecords.sorted { $0.correctOrder < $1.correctOrder }.map(\.id)
        isSolved = recordOrder == expected
        return isSolved
    }
    mutating func completeCctvPuzzle() -> Bool { recordOrder = ConvenienceStoreSpec.cctvRecords.sorted { $0.correctOrder < $1.correctOrder }.map(\.id); isSolved = true; return true }
}

struct InventoryPuzzleEngine {
    private(set) var selectedItemId: String?
    private(set) var isSolved = false
    mutating func selectInventoryItem(_ itemId: String) { guard !isSolved else { return }; selectedItemId = itemId }
    func calculateExpectedInventory(_ itemId: String) -> Int {
        guard let item = ConvenienceStoreSpec.inventoryItems.first(where: { $0.id == itemId }) else { return 0 }
        return item.previousStock + item.arrived - item.sold
    }
    mutating func submitInventoryDiscrepancy() -> Bool {
        guard !isSolved else { return true }
        isSolved = selectedItemId == ConvenienceStoreSpec.discrepancyItemId
        return isSolved
    }
    mutating func completeInventoryPuzzle() -> Bool { selectedItemId = ConvenienceStoreSpec.discrepancyItemId; isSolved = true; return true }
}

struct CustomerPatternPuzzleEngine {
    private(set) var selectedPurchases: [(cycle: Int, item: String)] = []
    private(set) var decodedMessage: String?
    private(set) var isSolved = false
    mutating func selectCustomerPurchase(_ cycle: Int, item: String) { guard !isSolved else { return }; selectedPurchases.append((cycle, item)) }
    func decodePurchasePattern() -> String { ConvenienceStoreSpec.customerPatternMessage }
    mutating func submitCustomerPattern(_ candidate: String) -> Bool {
        guard !isSolved else { return true }
        isSolved = candidate == ConvenienceStoreSpec.customerPatternMessage
        if isSolved { decodedMessage = candidate }
        return isSolved
    }
    mutating func completeCustomerPatternPuzzle() -> Bool { decodedMessage = ConvenienceStoreSpec.customerPatternMessage; isSolved = true; return true }
}

struct IncidentTimelinePuzzleEngine {
    private(set) var eventOrder: [String] = Array(ConvenienceStoreSpec.incidentEvents.map(\.id).reversed())
    private(set) var selectedConclusionIds: Set<String> = []
    private(set) var evidenceLinks: Set<String> = []
    private(set) var isSolved = false
    mutating func moveIncidentEvent(_ eventId: String, newIndex: Int) { guard !isSolved, let oldIndex = eventOrder.firstIndex(of: eventId), eventOrder.indices.contains(newIndex) else { return }; let value = eventOrder.remove(at: oldIndex); eventOrder.insert(value, at: newIndex) }
    mutating func connectEvidence(_ eventId: String, evidenceId: String) { guard !isSolved else { return }; evidenceLinks.insert("\(eventId)->\(evidenceId)") }
    mutating func selectIncidentConclusion(_ conclusionId: String) { guard !isSolved else { return }; selectedConclusionIds.insert(conclusionId) }
    func isOrderCorrect() -> Bool { eventOrder == ConvenienceStoreSpec.incidentEvents.sorted { $0.correctOrder < $1.correctOrder }.map(\.id) }
    func areConclusionsCorrect() -> Bool { ConvenienceStoreSpec.incidentEvents.map(\.id).allSatisfy { selectedConclusionIds.contains($0) } }
    func validateIncidentTimeline() -> Bool { isOrderCorrect() && areConclusionsCorrect() }
    mutating func completeIncidentTimelinePuzzle() -> Bool {
        eventOrder = ConvenienceStoreSpec.incidentEvents.sorted { $0.correctOrder < $1.correctOrder }.map(\.id)
        selectedConclusionIds = Set(ConvenienceStoreSpec.incidentEvents.map(\.id))
        isSolved = true
        return true
    }
}
