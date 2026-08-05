# The Last Commit — 사라진 개발자의 휴대폰

25–35분 분량의 모바일 방탈출을 Android와 iOS에서 각각 네이티브로 구현한 모노레포입니다. Android는 Kotlin·Jetpack Compose, iOS는 Swift·SwiftUI를 사용하며 콘텐츠와 공개 이름은 `Shared/Specifications` 계약으로 맞춥니다.

## 스토리와 게임 흐름

NOVACODE는 출시 예정 앱 Trace로 사용자 행동 데이터를 동의 없이 수집하려 합니다. 개발자 한도윤이 불법 수집 코드를 발견하지만, analytics 브랜치의 수집 커밋 `417` 뒤 커밋 `418`에서 작성자가 한도윤으로 조작됩니다. 범인처럼 보이던 팀장 강민석은 실제로 원상 복구를 지시했습니다. 조작은 CTO 회의실 관리자 단말기와 `ROOT-M` 공용 관리자 계정에서 이루어졌고, 한도윤은 01:21 증거 삭제를 막기 위해 실종을 계획한 뒤 복구 절차를 휴대폰에 남겼습니다.

게임 흐름:

`MainMenuScreen → IntroScreen → PhoneHomeScreen → MessengerPuzzleScreen → FlashlightPuzzleScreen → EncryptedNoteScreen → AudioRecordPuzzleScreen → CommitGraphPuzzleScreen → AccessLogPuzzleScreen → ServerConsoleScreen → FinalDecisionScreen → EndingScreen`

퍼즐 목록:

- 메신저 기록 시간순 복구
- 센서 또는 터치 손전등으로 조작 커밋 `417` 발견
- `417`로 암호 메모를 열고 `기록 수집 중` 완성
- 파형과 자막으로 손상된 음성 조각 4개 정렬(마이크 권한 없음)
- `main`, `release`, `analytics`, `hotfix` 커밋 그래프와 `417 → 418 → 420` 복구
- 출입·서버·메신저 로그를 대조해 CTO 회의실 단말기 확인
- 조작 커밋 `417`과 삭제 예약 01:21의 `121`을 합친 서버 코드 `417121` 입력
- 외부 감사 공개 또는 암호화 보관 중 정상 엔딩 선택

각 신규 퍼즐에는 2단계 힌트가 있고, 진행·힌트·수집 증거·엔딩 선택은 로컬에 저장됩니다. 기존 저장 키 `escape_phone_game_progress_v1`을 유지하며 새 필드가 없는 저장 데이터도 복원합니다.

## 프로젝트 구조

```text
IOS/                    Swift·SwiftUI 앱, Xcode 프로젝트와 iOS 테스트
AOS/                    Kotlin·Jetpack Compose 앱, Gradle과 Android 테스트
Shared/Specifications/  게임 규칙·공개 이름·한국어 문구·디자인 기준
Shared/Assets/           양쪽 플랫폼에서 사용하는 원본 이미지
AnalyticsBackend/       분석 웹 화면·리포트 도구·서버 API 테스트
Documentation/          아키텍처·배포·패리티·분석 문서
Tools/                  공유 JSON 및 Android–iOS 패리티 검사
Distribution/           APK와 iOS Archive 등 생성된 배포 산출물
```

## 빌드와 테스트

EscapePhone 전용 플레이 분석 웹 화면과 조회·테스트 도구는 [`AnalyticsBackend/`](AnalyticsBackend/)에서
모바일 프로젝트와 함께 관리한다. 실제 Flask 프로세스와 분석 SQLite 데이터는 기존
`carrot_scanner` 서버에서 계속 실행·보관한다.

Android(JDK 17, SDK 35):

```bash
cd AOS
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

iOS(iOS 17+, signing 없는 Simulator):

```bash
xcodebuild -project IOS/EscapePhone.xcodeproj -scheme EscapePhone \
  -sdk iphonesimulator -destination 'platform=iOS Simulator,id=YOUR_SIMULATOR_UDID' \
  CODE_SIGNING_ALLOWED=NO build
xcodebuild -project IOS/EscapePhone.xcodeproj -scheme EscapePhone \
  -destination 'platform=iOS Simulator,id=YOUR_SIMULATOR_UDID' \
  CODE_SIGNING_ALLOWED=NO test
```

공유 계약 검사:

```bash
python3 Tools/validate_shared_spec.py
python3 Tools/check_parity.py
```

센서가 없는 Android 에뮬레이터와 iOS Simulator에서는 터치 손전등 모드로 완료할 수 있습니다. 앱은 첫 실행에서 동의를 받은 경우에만 퍼즐별 소요 시간, 오답 횟수와 자동 분류된 오답 원인, 힌트 조회, 뒤로가기·백그라운드 이탈 시점, 엔딩 후 난이도와 자유 의견을 익명 JSON으로 기록합니다. 계정, 위치, 사진, 연락처, 마이크 또는 광고 식별자는 수집하지 않습니다.

분석 데이터는 퍼즐별 `elapsedMs`, `sessionCount`, `wrongAttemptCount`, `wrongReasonCounts`, `hintViewCount`, `exitEvents`로 구성됩니다. 엔딩 피드백은 1~5 난이도와 최대 1,000자의 의견을 저장합니다. 새 게임을 시작하면 직전 결과를 보관하며 최근 20개 플레이 세션을 유지합니다.
JSON은 퍼즐 완료 후 유휴 구간, 최종 피드백 저장 직후, 앱 백그라운드 진입 때 UI를 막지 않고 비동기로 전송합니다. 실패하면 로컬 큐에 보관하고 다음 활성화 때 재시도합니다. 서버 주소가 비어 있는 개발 빌드는 실제 전송 없이 로컬 큐와 수동 공유만 사용합니다. 백엔드 연결 방법은 `Documentation/ANALYTICS_BACKEND_KO.md`를 참고하세요.

기기에서 수집 상태를 확인하려면 **설정 → 기기 분석 데이터 보기**로 이동합니다. 퍼즐별 로컬 기록, 전송 대기 JSON, 시도 횟수, 마지막 성공 시각과 실패 상태를 확인하고 즉시 재전송하거나 현재 JSON을 공유할 수 있습니다. `전송 대기 0건`과 `마지막 성공` 시각이 함께 표시되면 수집 서버가 정상 응답한 상태입니다.
