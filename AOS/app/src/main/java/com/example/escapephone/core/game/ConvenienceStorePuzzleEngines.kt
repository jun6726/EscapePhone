package com.example.escapephone.core.game

import com.example.escapephone.core.model.ConvenienceStoreSpec
import com.example.escapephone.core.model.IncidentEvent

class ReceiptPuzzleEngine {
    var selectedItemId: String? = null; private set
    var isSolved = false; private set
    fun selectReceiptItem(itemId: String) { if (!isSolved) selectedItemId = itemId }
    fun compareReceiptPrice(itemId: String): Boolean = ConvenienceStoreSpec.receiptItems.any { it.id == itemId && it.code == ConvenienceStoreSpec.receiptAnomalyCode }
    fun submitReceiptAnswer(): Boolean {
        if (isSolved) return true
        isSolved = selectedItemId == ConvenienceStoreSpec.receiptAnomalyItemId
        return isSolved
    }
    fun completeReceiptPuzzle(): Boolean { selectedItemId = ConvenienceStoreSpec.receiptAnomalyItemId; isSolved = true; return true }
}

class BarcodePuzzleEngine {
    var selectedSegments: Map<String, String> = emptyMap(); private set
    var isSolved = false; private set
    fun selectBarcodeSegment(segment: String, value: String) { if (!isSolved) selectedSegments = selectedSegments + (segment to value) }
    fun assembleBarcodeCode(): String = ConvenienceStoreSpec.barcodeSegmentOrder.joinToString("") { selectedSegments[it].orEmpty() }
    fun submitBarcodeRule(): Boolean {
        if (isSolved) return true
        isSolved = selectedSegments["category"] == "99" && selectedSegments["shelfPosition"] == "40" && selectedSegments["arrivalOrder"] == "01"
        return isSolved
    }
    fun completeBarcodePuzzle(): Boolean { selectedSegments = mapOf("category" to "99", "shelfPosition" to "40", "arrivalOrder" to "01"); isSolved = true; return true }
}

class TiltObjectPuzzleEngine(
    var objectX: Float = 0.5f,
    var objectY: Float = 0.5f,
    private val destinationX: Float = 0.5f,
    private val destinationY: Float = 0.9f,
    private val destinationRadius: Float = 0.12f
) {
    var isSolved = false; private set
    fun applyTiltInput(deltaX: Float, deltaY: Float) { updateObjectPosition(objectX + deltaX, objectY + deltaY) }
    fun applyTouchInput(x: Float, y: Float) { updateObjectPosition(x, y) }
    fun updateObjectPosition(x: Float, y: Float) {
        objectX = x.coerceIn(0f, 1f); objectY = y.coerceIn(0f, 1f)
        val distance = kotlin.math.hypot((objectX - destinationX).toDouble(), (objectY - destinationY).toDouble())
        if (distance <= destinationRadius) isSolved = true
    }
}

class ShelfDifferencePuzzleEngine {
    var selectedDifferenceIds: List<String> = emptyList(); private set
    var isSolved = false; private set
    fun selectShelfDifference(differenceId: String) { if (!isSolved && differenceId !in selectedDifferenceIds) selectedDifferenceIds = selectedDifferenceIds + differenceId }
    fun submitShelfDifferences(): Boolean {
        if (isSolved) return true
        isSolved = ConvenienceStoreSpec.shelfDifferenceTargetIds.all { it in selectedDifferenceIds }
        return isSolved
    }
    fun completeShelfDifferencePuzzle(): Boolean { selectedDifferenceIds = ConvenienceStoreSpec.shelfDifferenceTargetIds; isSolved = true; return true }
}

class CctvPuzzleEngine {
    var recordOrder: List<String> = ConvenienceStoreSpec.cctvRecords.map { it.id }.reversed(); private set
    var playingRecordId: String? = null; private set
    var isSolved = false; private set
    fun playCctvRecord(recordId: String) { if (ConvenienceStoreSpec.cctvRecords.any { it.id == recordId }) playingRecordId = recordId }
    fun moveCctvRecordUp(index: Int) { if (!isSolved && index in recordOrder.indices && index > 0) recordOrder = recordOrder.toMutableList().also { val item = it[index]; it[index] = it[index - 1]; it[index - 1] = item } }
    fun moveCctvRecordDown(index: Int) { if (!isSolved && index in recordOrder.indices && index < recordOrder.lastIndex) recordOrder = recordOrder.toMutableList().also { val item = it[index]; it[index] = it[index + 1]; it[index + 1] = item } }
    fun submitCctvSequence(): Boolean {
        if (isSolved) return true
        val expected = ConvenienceStoreSpec.cctvRecords.sortedBy { it.correctOrder }.map { it.id }
        isSolved = recordOrder == expected
        return isSolved
    }
    fun completeCctvPuzzle(): Boolean { recordOrder = ConvenienceStoreSpec.cctvRecords.sortedBy { it.correctOrder }.map { it.id }; isSolved = true; return true }
}

class InventoryPuzzleEngine {
    var selectedItemId: String? = null; private set
    var isSolved = false; private set
    fun selectInventoryItem(itemId: String) { if (!isSolved) selectedItemId = itemId }
    fun calculateExpectedInventory(itemId: String): Int {
        val item = ConvenienceStoreSpec.inventoryItems.firstOrNull { it.id == itemId } ?: return 0
        return item.previousStock + item.arrived - item.sold
    }
    fun submitInventoryDiscrepancy(): Boolean {
        if (isSolved) return true
        isSolved = selectedItemId == ConvenienceStoreSpec.discrepancyItemId
        return isSolved
    }
    fun completeInventoryPuzzle(): Boolean { selectedItemId = ConvenienceStoreSpec.discrepancyItemId; isSolved = true; return true }
}

class CustomerPatternPuzzleEngine {
    var decodedMessage: String? = null; private set
    var isSolved = false; private set
    var selectedPurchases: List<Pair<Int, String>> = emptyList(); private set
    fun selectCustomerPurchase(cycle: Int, item: String) { if (!isSolved) selectedPurchases = selectedPurchases + (cycle to item) }
    fun decodePurchasePattern(): String = ConvenienceStoreSpec.customerPatternMessage
    fun submitCustomerPattern(candidate: String): Boolean {
        if (isSolved) return true
        isSolved = candidate == ConvenienceStoreSpec.customerPatternMessage
        if (isSolved) decodedMessage = candidate
        return isSolved
    }
    fun completeCustomerPatternPuzzle(): Boolean { decodedMessage = ConvenienceStoreSpec.customerPatternMessage; isSolved = true; return true }
}

class IncidentTimelinePuzzleEngine {
    var eventOrder: List<String> = ConvenienceStoreSpec.incidentEvents.map { it.id }.reversed(); private set
    var selectedConclusionIds: Set<String> = emptySet(); private set
    var isSolved = false; private set
    fun moveIncidentEvent(eventId: String, newIndex: Int) { if (!isSolved && eventId in eventOrder && newIndex in eventOrder.indices) eventOrder = eventOrder.toMutableList().also { val oldIndex = it.indexOf(eventId); it.removeAt(oldIndex); it.add(newIndex, eventId) } }
    var evidenceLinks: Set<Pair<String, String>> = emptySet(); private set
    fun connectEvidence(eventId: String, evidenceId: String) { if (!isSolved) evidenceLinks = evidenceLinks + (eventId to evidenceId) }
    fun selectIncidentConclusion(conclusionId: String) { if (!isSolved) selectedConclusionIds = selectedConclusionIds + conclusionId }
    fun isOrderCorrect(): Boolean = eventOrder == ConvenienceStoreSpec.incidentEvents.sortedBy { it.correctOrder }.map { it.id }
    fun areConclusionsCorrect(): Boolean = ConvenienceStoreSpec.incidentEvents.map { it.id }.all { it in selectedConclusionIds }
    fun validateIncidentTimeline(): Boolean = isOrderCorrect() && areConclusionsCorrect()
    fun completeIncidentTimelinePuzzle(): Boolean {
        eventOrder = ConvenienceStoreSpec.incidentEvents.sortedBy { it.correctOrder }.map { it.id }
        selectedConclusionIds = ConvenienceStoreSpec.incidentEvents.map { it.id }.toSet()
        isSolved = true
        return true
    }
}
