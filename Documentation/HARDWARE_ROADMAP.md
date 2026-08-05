# 하드웨어 로드맵

현재 구현은 NoOp/Fake 장치 커넥터만 사용하며 실제 BLE로 연결되었다고 표시하지 않는다. 이후 Android BluetoothGatt 및 iOS CoreBluetooth 어댑터가 `PuzzleDeviceConnector`를 구현하고 `ping`, `arm`, `unlock`, `reset` 명령을 ESP32/Arduino 특성에 매핑한다. Raspberry Pi 현장 컨트롤러는 `PuzzleOutput` 구현으로 완료 이벤트를 수신해 LED·서보·버튼·자석 센서 상태와 연계한다. 퍼즐 엔진은 수정하지 않는다.
