import SwiftUI

struct DeviceAnalyticsView: View {
    @EnvironmentObject private var app: AppContainer

    private var progress: GameProgress { app.gameProgress }
    private var statusText: String {
        switch progress.analyticsConsentStatus {
        case .notDetermined: return "동의 선택 전"
        case .denied: return "수집 안 함"
        case .granted where !progress.pendingAnalyticsUploads.isEmpty: return "기기에 저장됨 · 전송 대기"
        case .granted where progress.lastAnalyticsUploadAt != nil: return "서버 전송 완료"
        case .granted: return "동의됨 · 로컬 기록 중"
        }
    }

    var body: some View {
        Form {
            Section("수집·전송 진단") {
                LabeledContent("현재 상태", value: statusText)
                LabeledContent("퍼즐 기록", value: "\(progress.puzzleAnalytics.count)개")
                LabeledContent("전송 대기", value: "\(progress.pendingAnalyticsUploads.count)건")
                LabeledContent("생성한 전송", value: "\(progress.analyticsUploadSequence)건")
                LabeledContent("마지막 성공", value: progress.lastAnalyticsUploadAt?.formatted(date: .abbreviated, time: .standard) ?? "없음")
                if let error = progress.lastAnalyticsUploadError { LabeledContent("마지막 실패", value: error).foregroundStyle(.red) }
                if let id = progress.anonymousSessionId { Text("세션 ID\n\(id)").font(.caption.monospaced()).textSelection(.enabled) }
                Button("현재 데이터 즉시 전송") { app.retryAnalyticsUploadNow() }
                    .disabled(!app.isAnalyticsConsentGranted)
            }

            Section("판단 방법") {
                Text("‘전송 대기’가 1건 이상이면 데이터는 기기에 있으나 서버가 아직 받지 못한 상태입니다.")
                Text("대기 0건과 ‘마지막 성공’ 시각이 함께 보이면 서버가 정상 응답한 상태입니다.")
                Text("퍼즐 기록이 0개면 동의 후 아직 퍼즐 화면에 들어가지 않았거나, 동의를 철회해 기록이 삭제된 상태입니다.")
            }

            Section("퍼즐별 기기 기록") {
                if progress.puzzleAnalytics.isEmpty { Text("아직 저장된 퍼즐 기록이 없습니다.").foregroundStyle(.secondary) }
                ForEach(progress.puzzleAnalytics.keys.sorted(), id: \.self) { id in
                    if let analytics = progress.puzzleAnalytics[id] {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(id).font(.headline.monospaced())
                            Text("시간 \(formatDuration(analytics.elapsedMs)) · 세션 \(analytics.sessionCount)회")
                            Text("오답 \(analytics.wrongAttemptCount)회 · 힌트 \(analytics.hintViewCount)회 · 이탈 \(analytics.exitEvents.count)회")
                            Text(analytics.completedAt == nil ? "진행 중" : "해결 완료").foregroundStyle(analytics.completedAt == nil ? .orange : .green)
                        }
                    }
                }
            }

            Section("전송 대기 상세") {
                if progress.pendingAnalyticsUploads.isEmpty { Text("대기 중인 JSON이 없습니다.").foregroundStyle(.secondary) }
                ForEach(progress.pendingAnalyticsUploads) { pending in
                    LabeledContent("#\(pending.envelope.sequence)", value: "시도 \(pending.attemptCount)회 · \(pending.envelope.isFinal ? "최종" : "중간")")
                }
            }

            Section("기기 저장 JSON") {
                ShareLink(item: app.exportPlaytestReport()) { Label("JSON 공유", systemImage: "square.and.arrow.up") }
                Text(app.exportPlaytestReport()).font(.caption.monospaced()).textSelection(.enabled)
            }
        }
        .navigationTitle("기기 분석 데이터")
    }

    private func formatDuration(_ milliseconds: Int64) -> String {
        let seconds = milliseconds / 1_000
        return "\(seconds / 60)분 \(seconds % 60)초"
    }
}
