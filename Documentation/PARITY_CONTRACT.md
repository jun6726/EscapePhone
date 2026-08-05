# Android–iOS 동일성 계약

두 앱은 런타임 코드를 공유하지 않는 네이티브 구현이다. Android Kotlin/Compose와 iOS Swift/SwiftUI는 다음 사용자 흐름과 공개 이름을 함께 유지한다.

`mainMenu → intro → phoneHome → messengerPuzzle → flashlightPuzzle → encryptedNote → audioRecordPuzzle → commitGraphPuzzle → accessLogPuzzle → serverConsole → finalDecision → ending`

공개 모델, enum, 함수, 변수, 퍼즐 ID와 화면 ID는 `Shared/Specifications/naming_contract.json`을 기준으로 한다. 저장 키는 기존 `escape_phone_game_progress_v1`을 유지하며 새 필드가 없는 레거시 JSON은 기본값으로 복원한다. Android의 epoch millisecond와 iOS의 `Date`는 같은 시간 의미를 갖는다.

| 테스트 시나리오 | Android | iOS |
|---|---:|---:|
| startNewGame_returnsInitialProgress | ✓ | ✓ |
| completeMessengerPuzzle_unlocksPhotoApp | ✓ | ✓ |
| completeFlashlightPuzzle_unlocksEncryptedNote | ✓ | ✓ |
| completeEncryptedNotePuzzle_unlocksAudioRecord | ✓ | ✓ |
| submitAudioOrder_withCorrectOrder_succeeds | ✓ | ✓ |
| submitAudioOrder_withWrongOrder_fails | ✓ | ✓ |
| validateCommitGraph_withCorrectGraph_succeeds | ✓ | ✓ |
| validateCommitGraph_withWrongBranch_fails | ✓ | ✓ |
| validateAccessLogAnswers_withCorrectAnswers_succeeds | ✓ | ✓ |
| submitServerCode_with417121_completesGame | ✓ | ✓ |
| selectPublicDisclosure_savesEndingType | ✓ | ✓ |
| selectEncryptedArchive_savesEndingType | ✓ | ✓ |
| loadLegacyProgress_appliesNewDefaultValues | ✓ | ✓ |
| saveAndLoad_restoresGameProgress | ✓ | ✓ |
| reset_clearsGameProgress | ✓ | ✓ |
| submitMessengerOrder_withCorrectOrder_succeeds | ✓ | ✓ |
| submitMessengerOrder_withWrongOrder_fails | ✓ | ✓ |
| moveMessageUp_atFirstPosition_doesNothing | ✓ | ✓ |
| moveMessageDown_atLastPosition_doesNothing | ✓ | ✓ |
| updateFlashlightPosition_outsideTarget_doesNotDiscoverDigit | ✓ | ✓ |
| updateFlashlightPosition_insideTarget_beforeDuration_doesNotDiscoverDigit | ✓ | ✓ |
| updateFlashlightPosition_insideTarget_afterDuration_discoversDigit | ✓ | ✓ |
| updateFlashlightPosition_withinExpandedRecognitionRange_discoversDigit | ✓ | ✓ |
| updateFlashlightPosition_afterLeavingTarget_resetsProgress | ✓ | ✓ |
| completeFlashlightPuzzle_discovers417 | ✓ | ✓ |
| submitServerCode_with417121_succeeds | ✓ | ✓ |
| submitServerCode_withWrongCode_fails | ✓ | ✓ |
| appendServerCodeDigit_overLimit_isIgnored | ✓ | ✓ |
| clearServerCode_removesAllDigits | ✓ | ✓ |
| completePuzzleSession_savesElapsedTime | ✓ | ✓ |
| recordWrongAttempt_savesReason | ✓ | ✓ |
| requestHint_incrementsPuzzleHintViews | ✓ | ✓ |
| recordPuzzleExit_savesBackNavigation | ✓ | ✓ |
| submitPlayerFeedback_savesDifficultyAndComment | ✓ | ✓ |
| denyAnalyticsConsent_doesNotRecordPuzzleAnalytics | ✓ | ✓ |
| submitPlayerFeedback_enqueuesFinalJson | ✓ | ✓ |

퍼즐 분석은 명시적으로 동의한 경우에만 임의의 게임별 익명 세션 ID로 기록한다. 퍼즐 완료·이탈 때 활성 구간을 누적하고, 오답 원인은 검증 상태로 자동 분류한다. JSON은 로컬 재시도 큐를 거쳐 비동기 전송하며 새 게임을 시작하면 직전 세션을 `playtestHistory`에 보관한다.
