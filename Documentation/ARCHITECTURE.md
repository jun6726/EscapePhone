# 아키텍처

Android는 Compose + `StateFlow` 기반 `GameViewModel`, iOS는 SwiftUI + `ObservableObject` 기반 `AppContainer`를 사용한다. 양쪽 모두 단일 진행 저장소가 `GameProgress`를 관리하고 화면 상태와 진행 상태를 분리한다. 퍼즐 엔진은 UI·센서·저장 API를 모르며 동일한 입력과 규칙으로 테스트된다. 플랫폼 서비스는 `MotionController`, `HapticProvider`, `AdGateway`, `PuzzleDeviceConnector` 뒤에 격리한다.

`Shared/Specifications`는 실행 모듈이 아니라 콘텐츠와 계약의 단일 기준이다. 각 앱 리소스의 `game_spec.json`은 공통 사본과 바이트 단위로 검사한다.
