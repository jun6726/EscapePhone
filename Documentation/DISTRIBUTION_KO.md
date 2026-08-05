# EscapePhone 외부 테스트 배포

## Android

`Distribution/EscapePhone-Android-1.2-test.apk`를 전달하면 테스트 기기에 직접 설치할 수 있다.
기기에서 해당 파일을 연 뒤, 요청되는 경우 파일을 연 앱에 대해 **알 수 없는 앱 설치**를 허용한다.

이 APK는 개발용 키로 서명한 테스트 빌드다. Google Play에 출시할 때는 별도의 비공개 출시 키 또는 Play App Signing을 사용해 Release AAB를 생성해야 한다.

## iPhone

외부 테스트에는 TestFlight를 사용한다.

1. Apple Developer Program이 연결된 팀을 Xcode의 Signing & Capabilities에서 선택한다.
2. `com.example.escapephone`을 실제 소유한 고유 Bundle ID로 변경한다.
3. 같은 Bundle ID로 App Store Connect에 앱 레코드를 만든다.
4. Xcode에서 Product > Archive를 실행한다.
5. Organizer에서 Distribute App > App Store Connect > Upload를 선택한다.
6. App Store Connect의 TestFlight에서 외부 테스터 그룹을 만들고 빌드를 제출한다.
7. 베타 앱 심사가 승인되면 이메일 초대 또는 공개 링크를 공유한다.

현재 생성된 `Distribution/EscapePhone-iOS-1.1.xcarchive`는 Apple Development 인증서로 서명된 검증용 아카이브다. 외부 배포 전 Apple Distribution 서명과 App Store Connect 앱 레코드가 필요하며, 이 아카이브 자체는 설치 파일이 아니다.
