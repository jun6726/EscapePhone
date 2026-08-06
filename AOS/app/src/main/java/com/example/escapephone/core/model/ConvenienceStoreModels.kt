package com.example.escapephone.core.model

data class ReceiptItem(val id: String, val name: String, val price: Int, val code: String)
data class ProductPrice(val name: String, val price: Int)
data class ReceiptAnomaly(val itemId: String, val reason: String)

data class BarcodeItem(val id: String, val category: String, val shelfPosition: String, val arrivalOrder: String)
data class BarcodeSegment(val itemId: String, val segment: String)
data class BarcodeRule(val category: String, val shelfPosition: String, val arrivalOrder: String)

data class CctvRecord(val id: String, val description: String, val correctOrder: Int)
data class CctvClue(val recordId: String, val clue: String)

data class InventoryItem(val id: String, val name: String, val previousStock: Int, val arrived: Int, val sold: Int, val actualStock: Int)
data class InventoryDiscrepancy(val itemId: String, val missingCount: Int, val lastKnownLocation: String)

data class CustomerPurchase(val cycle: Int, val items: List<String>)
data class PatternSymbol(val cycle: Int, val symbol: String)

data class IncidentEvent(val id: String, val description: String, val correctOrder: Int)
data class EvidenceLink(val eventId: String, val evidenceId: String)

object ConvenienceStoreSpec {
    val receiptItems = listOf(
        ReceiptItem("triangle_kimbap", "삼각김밥", 1500, "1001"),
        ReceiptItem("water", "생수", 1000, "1002"),
        ReceiptItem("umbrella", "우산", 6500, "1003"),
        ReceiptItem("battery", "건전지", 4000, "1004"),
        ReceiptItem("energy_drink", "에너지음료", 2200, "1005"),
        ReceiptItem("unknown_item", "알 수 없는 상품", 4170, "0217")
    )
    const val receiptAnomalyItemId = "unknown_item"
    const val receiptAnomalyCode = "0217"

    val barcodeItems = listOf(
        BarcodeItem("barcode_1", category = "12", shelfPosition = "05", arrivalOrder = "03"),
        BarcodeItem("barcode_2", category = "12", shelfPosition = "40", arrivalOrder = "07"),
        BarcodeItem("barcode_3", category = "99", shelfPosition = "40", arrivalOrder = "01")
    )
    const val hiddenLocation = "냉장고 4번 하단"
    val barcodeSegmentOrder = listOf("category", "shelfPosition", "arrivalOrder")

    val shelfDifferenceTargetIds = listOf("drink_position", "umbrella_count", "price_tag_direction", "empty_slot", "fridge_pouch")
    const val finalShelfDifferenceId = "fridge_pouch"
    const val pouchMessage = "17분 동안만 기록이 남는다."

    val cctvRecords = listOf(
        CctvRecord("cctv_1", "전자레인지 00:12 작동, 손님 우산 없음, 출입문 닫힘", 0),
        CctvRecord("cctv_2", "점장 카운터 위치, POS 영수증 출력 없음", 1),
        CctvRecord("cctv_3", "손님이 우산을 들고 계산대로 이동, 매장 시계 02:05", 2),
        CctvRecord("cctv_4", "플레이어가 하지 않은 행동이 기록됨 (재현 오류)", 3),
        CctvRecord("cctv_5", "POS 영수증 출력됨, 출입문 열림, 02:17", 4)
    )

    val inventoryItems = listOf(
        InventoryItem("water", "생수", previousStock = 10, arrived = 5, sold = 12, actualStock = 3),
        InventoryItem("umbrella", "우산", previousStock = 6, arrived = 0, sold = 2, actualStock = 3),
        InventoryItem("battery", "건전지", previousStock = 8, arrived = 4, sold = 5, actualStock = 6),
        InventoryItem("evidence_box", "증거 보관 상자(포장 위장)", previousStock = 4, arrived = 0, sold = 1, actualStock = 2)
    )
    const val discrepancyItemId = "evidence_box"
    const val discrepancyMissingCount = 1
    const val discrepancyLocation = "냉장고 하단"

    val customerPurchases = listOf(
        CustomerPurchase(1, listOf("water", "battery", "umbrella")),
        CustomerPurchase(2, listOf("ramen", "umbrella", "water")),
        CustomerPurchase(3, listOf("battery", "water", "ramen"))
    )
    const val customerPatternMessage = "기록은 점장이 지우지 않았다"

    val incidentEvents = listOf(
        IncidentEvent("event_discovery", "윤서진이 불법 물류 거래를 발견했다.", 0),
        IncidentEvent("event_escape", "박재현이 윤서진을 후문으로 내보냈다.", 1),
        IncidentEvent("event_hide_evidence", "박재현이 증거 저장 장치를 상품 포장에 숨겼다.", 2),
        IncidentEvent("event_central7", "CENTRAL-7이 CCTV와 POS 시간을 조작했다.", 3),
        IncidentEvent("event_blame", "본사는 박재현에게 책임을 넘기려 했다.", 4),
        IncidentEvent("event_0217", "02:17은 사건 발생 시각이 아니라 원격 기록 조작이 시작된 시각이다.", 5),
        IncidentEvent("event_loop", "반복은 시간 현상이 아니라 복원 시스템이 손상된 17분의 데이터를 반복 재생하는 현상이다.", 6)
    )
}
