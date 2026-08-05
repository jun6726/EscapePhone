# 플레이 분석 백엔드 연결 가이드

## 현재 앱 동작

- 첫 실행에서 익명 분석 수집 동의를 받는다.
- 거부하면 분석 모델과 전송 큐를 만들지 않는다.
- 동의하면 게임마다 새로운 무작위 `sessionId`를 사용한다.
- 퍼즐 완료 2초 후, 최종 피드백 저장 직후, 앱 백그라운드 진입 때 JSON 스냅샷을 큐에 넣는다.
- 네트워크 전송은 UI 스레드를 막지 않는다. 실패한 항목은 기기에 남겨 다음 활성화 시 재시도한다.
- `sessionId + sequence`를 중복 방지 키로 사용한다.

현재 기본 수집 주소는 `https://s-imac.coati-bramble.ts.net/v1/playtest-events`다. Android는 `PLAYTEST_ANALYTICS_ENDPOINT`, iOS는 `HTTPPlaytestAnalyticsUploader.defaultEndpoint`를 사용한다. 빈 주소를 주입한 개발 빌드는 실제 전송 없이 최대 20개 JSON을 로컬 큐에 보관한다. 다른 운영 서버를 사용할 때는 반드시 `https://` 주소를 설정한다.

앱의 **설정 → 기기 분석 데이터**에서는 동의 상태, 익명 세션 ID, 퍼즐별 기록, 대기 큐, 마지막 성공 시각과 실패 상태를 확인할 수 있다. `전송 대기`가 남아 있으면 기기에는 저장됐지만 서버가 아직 받지 못한 상태이며, `현재 데이터 즉시 전송`으로 현재 스냅샷을 생성하거나 대기 큐를 즉시 재시도할 수 있다.

현재 carrot_scanner 서버에서는 다음 명령으로 수집 결과를 바로 요약할 수 있다.

```bash
cd /Users/yeonjunsimac/Documents/carrot_scanner
./venv/bin/python ../woody/EscapePhone/AnalyticsBackend/playtest_report.py
```

웹 리포트는 `https://s-imac.coati-bramble.ts.net/playtest-report`에서 확인한다. 서버의
`PLAYTEST_ADMIN_TOKEN` 값을 관리자 비밀번호로 사용하며, 로그인 성공 시 12시간 동안
유효한 `Secure`, `HttpOnly`, `SameSite=Strict` 세션 쿠키가 만들어진다. 관리자 비밀번호는
URL이나 앱 바이너리에 넣지 않는다. 화면은 전체 완료율, 퍼즐별 중앙 완료 시간·오답·힌트·이탈,
플랫폼 분포, 난이도, 최근 자유 의견과 세션 진행 상태를 보여주며 60초마다 갱신한다.

```bash
# Android
./gradlew assembleRelease -PplaytestAnalyticsEndpoint=https://api.example.com/v1/playtest-events

# iOS
xcodebuild ... PLAYTEST_ANALYTICS_URL=https://api.example.com/v1/playtest-events
```

## 권장 1단계 구성

소규모 테스트에는 **Supabase Edge Function + PostgreSQL** 구성이 빠르다. Edge Function에서 스키마 검증과 속도 제한을 수행하고 원본 JSON은 `jsonb`로 저장한다. 사용량이 커지거나 엣지 단위 제어가 중요하면 **Cloudflare Worker + D1/Queues**로 같은 API 계약을 구현할 수 있다.

앱 안에는 데이터베이스 비밀번호나 서비스 역할 키를 넣지 않는다. 수집 엔드포인트만 공개하고 서버에서 요청 크기, 스키마, 허용 앱 버전, 속도 제한을 검증한다.

## API 계약

`POST /v1/playtest-events`

- `Content-Type: application/json`
- 성공: `202 Accepted` 또는 `200 OK`
- 최대 요청 크기 권장: 128 KB
- 멱등 키: `(session_id, sequence)` unique
- 동일 키 재요청은 성공으로 응답하되 행을 중복 생성하지 않는다.

권장 테이블:

```sql
create table playtest_events (
  session_id uuid not null,
  sequence integer not null,
  platform text not null,
  app_version text not null,
  consent_version integer not null,
  is_final boolean not null,
  client_created_at timestamptz not null,
  received_at timestamptz not null default now(),
  payload jsonb not null,
  primary key (session_id, sequence)
);
```

서버는 `schemaVersion`, 난이도 범위 1~5, 의견 길이 1,000자, 알려진 퍼즐 ID와 오답 원인만 허용해야 한다. 원본 IP와 User-Agent는 분석 테이블에 저장하지 않는 것을 권장한다.

## 운영 개선 순서

1. 개인정보 처리방침에 수집 항목, 목적, 보존 기간, 철회 후 처리 방식을 명시한다.
2. 서버에 30~90일 자동 삭제 정책과 집계 테이블을 둔다.
3. 원본 자유 의견은 접근 권한을 별도로 제한하고 로그에 남기지 않는다.
4. 요청량이 늘면 지수 백오프와 OS 백그라운드 작업(Android WorkManager, iOS BackgroundTasks)을 추가한다.
5. 조작된 보고서가 문제가 되면 iOS App Attest와 Android Play Integrity를 서버 검증 방식으로 추가한다.
6. 대시보드는 퍼즐별 중앙 완료시간, 오답 원인 비율, 힌트 사용률, 이탈률, 난이도 평가의 상관관계를 우선 표시한다.

동의 철회 시 앱은 향후 수집을 중단하고 기기 내 큐를 삭제한다. 이미 서버에 전달된 익명 데이터까지 삭제하려면 별도의 삭제 토큰 또는 개인정보 선택 페이지를 설계해야 한다.
