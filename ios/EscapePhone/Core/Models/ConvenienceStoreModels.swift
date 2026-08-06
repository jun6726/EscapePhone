import Foundation

struct ReceiptItem: Identifiable, Equatable { let id: String; let name: String; let price: Int; let code: String }
struct ProductPrice: Equatable { let name: String; let price: Int }
struct ReceiptAnomaly: Equatable { let itemId: String; let reason: String }

struct BarcodeItem: Identifiable, Equatable { let id: String; let category: String; let shelfPosition: String; let arrivalOrder: String }
struct BarcodeSegment: Equatable { let itemId: String; let segment: String }
struct BarcodeRule: Equatable { let category: String; let shelfPosition: String; let arrivalOrder: String }

struct CctvRecord: Identifiable, Equatable { let id: String; let description: String; let correctOrder: Int }
struct CctvClue: Equatable { let recordId: String; let clue: String }

struct InventoryItem: Identifiable, Equatable { let id: String; let name: String; let previousStock: Int; let arrived: Int; let sold: Int; let actualStock: Int }
struct InventoryDiscrepancy: Equatable { let itemId: String; let missingCount: Int; let lastKnownLocation: String }

struct CustomerPurchase: Equatable { let cycle: Int; let items: [String] }
struct PatternSymbol: Equatable { let cycle: Int; let symbol: String }

struct IncidentEvent: Identifiable, Equatable { let id: String; let description: String; let correctOrder: Int }
struct EvidenceLink: Equatable { let eventId: String; let evidenceId: String }

enum ConvenienceStoreSpec {
    static let receiptItems: [ReceiptItem] = [
        ReceiptItem(id: "triangle_kimbap", name: "삼각김밥", price: 1500, code: "1001"),
        ReceiptItem(id: "water", name: "생수", price: 1000, code: "1002"),
        ReceiptItem(id: "umbrella", name: "우산", price: 6500, code: "1003"),
        ReceiptItem(id: "battery", name: "건전지", price: 4000, code: "1004"),
        ReceiptItem(id: "energy_drink", name: "에너지음료", price: 2200, code: "1005"),
        ReceiptItem(id: "unknown_item", name: "알 수 없는 상품", price: 4170, code: "0217")
    ]
    static let receiptAnomalyItemId = "unknown_item"
    static let receiptAnomalyCode = "0217"

    static let barcodeItems: [BarcodeItem] = [
        BarcodeItem(id: "barcode_1", category: "12", shelfPosition: "05", arrivalOrder: "03"),
        BarcodeItem(id: "barcode_2", category: "12", shelfPosition: "40", arrivalOrder: "07"),
        BarcodeItem(id: "barcode_3", category: "99", shelfPosition: "40", arrivalOrder: "01")
    ]
    static let hiddenLocation = "냉장고 4번 하단"
    static let barcodeSegmentOrder = ["category", "shelfPosition", "arrivalOrder"]

    static let shelfDifferenceTargetIds = ["drink_position", "umbrella_count", "price_tag_direction", "empty_slot", "fridge_pouch"]
    static let finalShelfDifferenceId = "fridge_pouch"
    static let pouchMessage = "17분 동안만 기록이 남는다."

    static let cctvRecords: [CctvRecord] = [
        CctvRecord(id: "cctv_1", description: "전자레인지 00:12 작동, 손님 우산 없음, 출입문 닫힘", correctOrder: 0),
        CctvRecord(id: "cctv_2", description: "점장 카운터 위치, POS 영수증 출력 없음", correctOrder: 1),
        CctvRecord(id: "cctv_3", description: "손님이 우산을 들고 계산대로 이동, 매장 시계 02:05", correctOrder: 2),
        CctvRecord(id: "cctv_4", description: "플레이어가 하지 않은 행동이 기록됨 (재현 오류)", correctOrder: 3),
        CctvRecord(id: "cctv_5", description: "POS 영수증 출력됨, 출입문 열림, 02:17", correctOrder: 4)
    ]

    static let inventoryItems: [InventoryItem] = [
        InventoryItem(id: "water", name: "생수", previousStock: 10, arrived: 5, sold: 12, actualStock: 3),
        InventoryItem(id: "umbrella", name: "우산", previousStock: 6, arrived: 0, sold: 2, actualStock: 3),
        InventoryItem(id: "battery", name: "건전지", previousStock: 8, arrived: 4, sold: 5, actualStock: 6),
        InventoryItem(id: "evidence_box", name: "증거 보관 상자(포장 위장)", previousStock: 4, arrived: 0, sold: 1, actualStock: 2)
    ]
    static let discrepancyItemId = "evidence_box"
    static let discrepancyMissingCount = 1
    static let discrepancyLocation = "냉장고 하단"

    static let customerPurchases: [CustomerPurchase] = [
        CustomerPurchase(cycle: 1, items: ["water", "battery", "umbrella"]),
        CustomerPurchase(cycle: 2, items: ["ramen", "umbrella", "water"]),
        CustomerPurchase(cycle: 3, items: ["battery", "water", "ramen"])
    ]
    static let customerPatternMessage = "기록은 점장이 지우지 않았다"

    static let incidentEvents: [IncidentEvent] = [
        IncidentEvent(id: "event_discovery", description: "윤서진이 불법 물류 거래를 발견했다.", correctOrder: 0),
        IncidentEvent(id: "event_escape", description: "박재현이 윤서진을 후문으로 내보냈다.", correctOrder: 1),
        IncidentEvent(id: "event_hide_evidence", description: "박재현이 증거 저장 장치를 상품 포장에 숨겼다.", correctOrder: 2),
        IncidentEvent(id: "event_central7", description: "CENTRAL-7이 CCTV와 POS 시간을 조작했다.", correctOrder: 3),
        IncidentEvent(id: "event_blame", description: "본사는 박재현에게 책임을 넘기려 했다.", correctOrder: 4),
        IncidentEvent(id: "event_0217", description: "02:17은 사건 발생 시각이 아니라 원격 기록 조작이 시작된 시각이다.", correctOrder: 5),
        IncidentEvent(id: "event_loop", description: "반복은 시간 현상이 아니라 복원 시스템이 손상된 17분의 데이터를 반복 재생하는 현상이다.", correctOrder: 6)
    ]
}
