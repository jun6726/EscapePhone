# EscapePhone — 모바일 미스터리 방탈출

여러 방탈출 테마를 제공하는 모바일 앱을 Android와 iOS에서 각각 네이티브로 구현한 모노레포입니다. Android는 Kotlin·Jetpack Compose, iOS는 Swift·SwiftUI를 사용하며, 콘텐츠와 공개 이름은 `Shared/Specifications` 계약으로 맞춥니다. Kotlin Multiplatform은 사용하지 않습니다.

앱 실행 시 테마 선택 화면(`ThemeSelectionScreen`)이 먼저 표시되고, 테마를 고르면 해당 테마 전용 메인 메뉴(`ThemeMainMenuScreen`)로 이동합니다. 각 테마는 진행 상태·힌트 수·수집 증거·엔딩을 독립적으로 저장하며, 한 테마를 초기화해도 다른 테마의 기록은 유지됩니다.

## 제공 테마

### The Last Commit — 사라진 개발자의 휴대폰 (`the_last_commit`)

NOVACODE는 출시 예정 앱 Trace로 사용자 행동 데이터를 동의 없이 수집하려 합니다. 개발자 한도윤이 불법 수집 코드를 발견하지만, analytics 브랜치의 수집 커밋 `417` 뒤 커밋 `418`에서 작성자가 한도윤으로 조작됩니다. 조작은 CTO 회의실 관리자 단말기와 `ROOT-M` 공용 관리자 계정에서 이루어졌고, 한도윤은 증거 삭제를 막기 위해 실종을 계획한 뒤 복구 절차를 휴대폰에 남겼습니다.

예상 플레이 시간: 25–35분

게임 흐름: `ThemeMainMenuScreen → IntroScreen → PhoneHomeScreen → MessengerPuzzleScreen → FlashlightPuzzleScreen → EncryptedNoteScreen → AudioRecordPuzzleScreen → CommitGraphPuzzleScreen → AccessLogPuzzleScreen → ServerConsoleScreen → FinalDecisionScreen → EndingScreen`

퍼즐: 메신저 기록 시간순 복구 → 손전등(센서/터치)으로 조작 커밋 `417` 발견 → 암호 메모 `기록 수집 중` 완성 → 음성 조각 4개 순서 정렬 → 커밋 그래프 `417→418→420` 복구 → 접근 로그 대조 → 서버 코드 `417121` 입력 → 외부 공개/암호화 보관 중 엔딩 선택.

### 02:17 — 반복되는 편의점의 밤 (`convenience_store_loop`)

지인의 부탁으로 새벽 편의점 근무를 대신 맡은 플레이어는 오전 2시 17분마다 매장이 리셋되는 현상을 겪습니다. 처음엔 시스템 오류처럼 보이지만, 실제로는 과거 실종 사건(손님 윤서진, 점장 박재현) 당시의 센서·POS·CCTV 기록을 반복 재생하는 야간 관리 시스템의 오작동입니다. 본사 공용 계정 `CENTRAL-7`이 기록을 조작한 진짜 배후로 드러납니다.

예상 플레이 시간: 25–35분

게임 흐름: `ThemeMainMenuScreen → ConvenienceStoreIntroScreen → ConvenienceStoreHomeScreen → ReceiptPuzzleScreen → BarcodePuzzleScreen → ShelfDifferencePuzzleScreen → CctvPuzzleScreen → InventoryPuzzleScreen → CustomerPatternPuzzleScreen → IncidentTimelinePuzzleScreen → ConvenienceStoreFinalDecisionScreen → ConvenienceStoreEndingScreen`

퍼즐 7종:

1. **영수증과 가격표 비교**(`receipt_price`) — 잘못된 상품 코드 `0217` 발견
2. **바코드 규칙**(`barcode_rule`) — 분류·위치·입고순서 조합으로 숨겨진 위치 발견
3. **진열대 차이 + 기울기 조작**(`shelf_difference`) — 냉장고 하단 봉투까지 5가지 차이 탐색. 기울기 센서 또는 터치 드래그로 물체를 목적지까지 이동(`TiltObjectPuzzleEngine`)
4. **CCTV 순서 복원**(`cctv_sequence`) — 5개 기록을 시간순 정렬
5. **재고 교차검증**(`inventory_crosscheck`) — 이전 재고 + 입고 − 판매 계산으로 불일치 상품 발견
6. **구매 패턴 해독**(`customer_pattern`) — 반복별 구매 목록으로 메시지 복원
7. **사건 타임라인**(`incident_timeline`) — 7개 사건을 시간순 배치하고 결론 검증

최종 선택: 외부 감사 기관에 기록 전송(`publicDisclosure`) 또는 암호화 보관(`encryptedArchive`) — 둘 다 정상 엔딩입니다. 암호화 보관 엔딩은 향후 Arduino·ESP32 등 실물 하드웨어 연동 테마로 이어질 여지를 남깁니다.

## 테마 시스템

- `ThemeId`, `ThemeMetadata`, `ThemeProgress`, `ThemeStatus`, `ThemeRegistry`, `ThemeProgressStore`가 Android/iOS 양쪽에 동일한 이름으로 존재합니다.
- 저장 키는 테마별로 분리됩니다(`theme_progress_the_last_commit`, `theme_progress_convenience_store_loop`). 기존 The Last Commit 저장 데이터(`escape_phone_game_progress_v1`)는 앱 최초 실행 시 자동으로 `the_last_commit` 테마 진행 데이터로 마이그레이션되며, 손상된 데이터는 크래시 없이 초기값으로 복구됩니다.
- **테마별 초기화**(`resetSelectedTheme`)는 선택한 테마의 진행만 지웁니다. **전체 초기화**(`resetAllThemeProgress`)는 모든 테마의 진행을 지웁니다. 테마 선택 화면에서 두 초기화 모두 접근할 수 있습니다.

## 프로젝트 구조

```text
ios/                    Swift·SwiftUI 앱, Xcode 프로젝트와 iOS 테스트
AOS/                    Kotlin·Jetpack Compose 앱, Gradle과 Android 테스트
Shared/Specifications/  게임 규칙·공개 이름·한국어 문구·디자인 기준 (테마별 game_spec.json 포함)
Shared/Assets/          양쪽 플랫폼에서 사용하는 원본 이미지
AnalyticsBackend/       분석 웹 화면·리포트 도구·서버 API 테스트
Documentation/          아키텍처·배포·패리티·분석 문서
tools/                  공유 JSON 검증 및 Android–iOS 패리티 검사
Distribution/           APK와 iOS Archive 등 생성된 배포 산출물
```

## 빌드와 테스트

Android(JDK 17, SDK 35):

```bash
cd AOS
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

iOS(iOS 17+, signing 없는 Simulator):

```bash
cd ios
xcrun simctl list devices available
xcodebuild -project EscapePhone.xcodeproj -scheme EscapePhone \
  -sdk iphonesimulator CODE_SIGNING_ALLOWED=NO build
xcodebuild -project EscapePhone.xcodeproj -scheme EscapePhone \
  -destination 'platform=iOS Simulator,id=YOUR_SIMULATOR_UDID' \
  CODE_SIGNING_ALLOWED=NO test
```

공유 계약 검사:

```bash
python3 tools/validate_shared_spec.py
python3 tools/check_parity.py
```

## 센서와 터치 대체 방식

- The Last Commit의 손전등 퍼즐과 02:17의 진열대 차이 퍼즐은 기기 기울기 센서를 사용합니다.
- 센서가 없는 Android 에뮬레이터·iOS Simulator에서는 터치 드래그 모드로 동일하게 완료할 수 있습니다(`FlashlightControlMode` / `TiltControlMode`가 `motion`↔`touch` 전환).

## 플레이 분석

앱은 첫 실행에서 동의를 받은 경우에만 퍼즐별 소요 시간, 오답 횟수와 자동 분류된 오답 원인, 힌트 조회, 뒤로가기·백그라운드 이탈 시점, 엔딩 후 난이도와 자유 의견을 익명 JSON으로 기록합니다. 계정, 위치, 사진, 연락처, 마이크 또는 광고 식별자는 수집하지 않습니다. 분석 백엔드는 [`AnalyticsBackend/`](AnalyticsBackend/)에서 관리합니다.

## 향후 하드웨어 연동 방향

이번 작업에서는 실제 Arduino, ESP32, Raspberry Pi, BLE 장치 연동을 구현하지 않았습니다. `PuzzleDeviceConnector`/`PuzzleOutput` 인터페이스와 `puzzleDidComplete(themeId, puzzleId)` 콜백은 NoOp/Fake 구현만 사용하며, 향후 편의점 테마의 암호화 보관 엔딩과 연결해 바코드 리더·RFID 태그·냉장고 문 센서·무게 센서 등을 붙일 수 있는 확장 지점으로 남겨두었습니다.
