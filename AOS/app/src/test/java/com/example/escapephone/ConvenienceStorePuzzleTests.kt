package com.example.escapephone

import com.example.escapephone.core.game.BarcodePuzzleEngine
import com.example.escapephone.core.game.CctvPuzzleEngine
import com.example.escapephone.core.game.CustomerPatternPuzzleEngine
import com.example.escapephone.core.game.IncidentTimelinePuzzleEngine
import com.example.escapephone.core.game.InventoryPuzzleEngine
import com.example.escapephone.core.game.ReceiptPuzzleEngine
import com.example.escapephone.core.game.ShelfDifferencePuzzleEngine
import com.example.escapephone.core.game.TiltObjectPuzzleEngine
import com.example.escapephone.core.model.ConvenienceStoreSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConvenienceStorePuzzleTests {
    // Receipt
    @Test
    fun submitReceiptAnswer_withCorrectAnomaly_succeeds() {
        val engine = ReceiptPuzzleEngine()
        engine.selectReceiptItem(ConvenienceStoreSpec.receiptAnomalyItemId)
        assertTrue(engine.submitReceiptAnswer())
    }

    @Test
    fun submitReceiptAnswer_withWrongAnomaly_fails() {
        val engine = ReceiptPuzzleEngine()
        engine.selectReceiptItem("water")
        assertFalse(engine.submitReceiptAnswer())
    }

    @Test
    fun completeReceiptPuzzle_unlocksBarcodePuzzle() {
        val engine = ReceiptPuzzleEngine()
        assertTrue(engine.completeReceiptPuzzle())
        assertTrue(engine.isSolved)
    }

    // Barcode
    @Test
    fun assembleBarcodeCode_withCorrectSegments_succeeds() {
        val engine = BarcodePuzzleEngine()
        engine.selectBarcodeSegment("category", "99")
        engine.selectBarcodeSegment("shelfPosition", "40")
        engine.selectBarcodeSegment("arrivalOrder", "01")
        assertEquals("994001", engine.assembleBarcodeCode())
        assertTrue(engine.submitBarcodeRule())
    }

    @Test
    fun assembleBarcodeCode_withWrongSegments_fails() {
        val engine = BarcodePuzzleEngine()
        engine.selectBarcodeSegment("category", "12")
        engine.selectBarcodeSegment("shelfPosition", "05")
        engine.selectBarcodeSegment("arrivalOrder", "03")
        assertFalse(engine.submitBarcodeRule())
    }

    @Test
    fun completeBarcodePuzzle_unlocksShelfDifferencePuzzle() {
        val engine = BarcodePuzzleEngine()
        assertTrue(engine.completeBarcodePuzzle())
        assertTrue(engine.isSolved)
    }

    // Shelf difference
    @Test
    fun submitShelfDifferences_withAllTargets_succeeds() {
        val engine = ShelfDifferencePuzzleEngine()
        ConvenienceStoreSpec.shelfDifferenceTargetIds.forEach { engine.selectShelfDifference(it) }
        assertTrue(engine.submitShelfDifferences())
    }

    @Test
    fun submitShelfDifferences_withMissingTarget_fails() {
        val engine = ShelfDifferencePuzzleEngine()
        ConvenienceStoreSpec.shelfDifferenceTargetIds.drop(1).forEach { engine.selectShelfDifference(it) }
        assertFalse(engine.submitShelfDifferences())
    }

    @Test
    fun tiltObjectPuzzle_reachesDestination() {
        val engine = TiltObjectPuzzleEngine(objectX = 0.5f, objectY = 0.5f)
        engine.applyTiltInput(0f, 0.4f)
        assertTrue(engine.isSolved)
    }

    @Test
    fun touchObjectPuzzle_reachesDestination() {
        val engine = TiltObjectPuzzleEngine()
        engine.applyTouchInput(0.5f, 0.9f)
        assertTrue(engine.isSolved)
    }

    // CCTV
    @Test
    fun submitCctvSequence_withCorrectOrder_succeeds() {
        val engine = CctvPuzzleEngine()
        val expected = ConvenienceStoreSpec.cctvRecords.sortedBy { it.correctOrder }.map { it.id }
        while (engine.recordOrder != expected) {
            val misplacedIndex = engine.recordOrder.indices.first { engine.recordOrder[it] != expected[it] }
            val targetIndex = engine.recordOrder.indexOf(expected[misplacedIndex])
            if (targetIndex > misplacedIndex) engine.moveCctvRecordUp(targetIndex) else engine.moveCctvRecordDown(targetIndex)
        }
        assertTrue(engine.submitCctvSequence())
    }

    @Test
    fun submitCctvSequence_withWrongOrder_fails() {
        val engine = CctvPuzzleEngine()
        assertFalse(engine.submitCctvSequence())
    }

    @Test
    fun completeCctvPuzzle_unlocksInventoryPuzzle() {
        val engine = CctvPuzzleEngine()
        assertTrue(engine.completeCctvPuzzle())
        assertTrue(engine.isSolved)
    }

    // Inventory
    @Test
    fun calculateExpectedInventory_returnsCorrectValue() {
        val engine = InventoryPuzzleEngine()
        assertEquals(3, engine.calculateExpectedInventory("water"))
        assertEquals(3, engine.calculateExpectedInventory("evidence_box"))
    }

    @Test
    fun submitInventoryDiscrepancy_withCorrectItem_succeeds() {
        val engine = InventoryPuzzleEngine()
        engine.selectInventoryItem(ConvenienceStoreSpec.discrepancyItemId)
        assertTrue(engine.submitInventoryDiscrepancy())
    }

    @Test
    fun submitInventoryDiscrepancy_withWrongItem_fails() {
        val engine = InventoryPuzzleEngine()
        engine.selectInventoryItem("water")
        assertFalse(engine.submitInventoryDiscrepancy())
    }

    // Customer pattern
    @Test
    fun decodePurchasePattern_withCorrectSequence_returnsMessage() {
        val engine = CustomerPatternPuzzleEngine()
        assertEquals(ConvenienceStoreSpec.customerPatternMessage, engine.decodePurchasePattern())
    }

    @Test
    fun submitCustomerPattern_withWrongSequence_fails() {
        val engine = CustomerPatternPuzzleEngine()
        assertFalse(engine.submitCustomerPattern("wrong message"))
    }

    @Test
    fun completeCustomerPatternPuzzle_unlocksTimelinePuzzle() {
        val engine = CustomerPatternPuzzleEngine()
        assertTrue(engine.completeCustomerPatternPuzzle())
        assertTrue(engine.isSolved)
    }

    // Timeline
    @Test
    fun validateIncidentTimeline_withCorrectOrder_succeeds() {
        val engine = IncidentTimelinePuzzleEngine()
        val expected = ConvenienceStoreSpec.incidentEvents.sortedBy { it.correctOrder }.map { it.id }
        while (engine.eventOrder != expected) {
            val misplacedIndex = engine.eventOrder.indices.first { engine.eventOrder[it] != expected[it] }
            engine.moveIncidentEvent(expected[misplacedIndex], misplacedIndex)
        }
        assertTrue(engine.isOrderCorrect())
    }

    @Test
    fun validateIncidentTimeline_withWrongOrder_fails() {
        val engine = IncidentTimelinePuzzleEngine()
        assertFalse(engine.isOrderCorrect())
    }

    @Test
    fun validateIncidentTimeline_withCorrectConclusions_succeeds() {
        val engine = IncidentTimelinePuzzleEngine()
        ConvenienceStoreSpec.incidentEvents.forEach { engine.selectIncidentConclusion(it.id) }
        assertTrue(engine.areConclusionsCorrect())
    }
}
