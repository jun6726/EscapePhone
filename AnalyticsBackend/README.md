# EscapePhone 플레이 분석 서버 연동 파일

EscapePhone 모노레포와 함께 관리하는 플레이 분석 웹 화면, 조회 도구, 서버 API 테스트다.
실제 Flask 프로세스와 SQLite 데이터베이스는 기존 `carrot_scanner`에서 계속 실행·보관한다.

## 구성

- `templates/playtest_report.html`: `/playtest-report` 웹 리포트 화면의 원본
- `playtest_report.py`: SQLite 분석 결과를 터미널에서 요약하는 도구
- `tests/test_playtest_api.py`: 수집·인증·집계 API 테스트

`carrot_scanner/templates/playtest_report.html`은 이 폴더의 HTML을 가리키는 호환 심볼릭
링크다. 따라서 화면 원본은 EscapePhone에서 한 번만 관리하면서 기존 Flask 라우팅은 그대로
유지한다.

## 확인 명령

```bash
cd /Users/yeonjunsimac/Documents/woody/EscapePhone
python3 AnalyticsBackend/playtest_report.py

cd /Users/yeonjunsimac/Documents/carrot_scanner
./venv/bin/python ../woody/EscapePhone/AnalyticsBackend/tests/test_playtest_api.py
```

기본 서버 또는 DB 위치가 달라지면 다음 환경 변수를 사용할 수 있다.

- `CARROT_SCANNER_SERVER_DIR`: 테스트가 `app.py`를 불러올 서버 폴더
- `ESCAPEPHONE_PLAYTEST_DB_PATH`: 리포트 도구가 조회할 SQLite 파일

운영 서버 파일인 `carrot_scanner/app.py`, `escapephone_playtest.db`, LaunchAgent 설정은
서비스 중단을 피하기 위해 이동하지 않는다.
