import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var app: AppContainer
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        Form {
            Section("손전등 조작") {
                Picker("조작 방식", selection: Binding(get: { app.gameProgress.controlMode }, set: { app.setControlMode($0) })) { Text("기울기").tag(FlashlightControlMode.motion); Text("터치").tag(FlashlightControlMode.touch) }
                if !app.motion.isAvailable { Label("현재 환경에서는 기울기 센서를 사용할 수 없습니다. 터치 모드가 자동 적용됩니다.", systemImage: "exclamationmark.triangle") }
            }
            Section("익명 플레이 분석") {
                Text("퍼즐별 시간·오답 원인·힌트·이탈과 선택 입력한 피드백만 JSON으로 수집합니다. 계정, 위치, 사진, 연락처, 마이크와 광고 식별자는 포함하지 않습니다.")
                LabeledContent("현재 상태", value: app.isAnalyticsConsentGranted ? "동의함" : "동의하지 않음")
                LabeledContent("전송 대기", value: "\(app.gameProgress.pendingAnalyticsUploads.count)건")
                Button("기기 분석 데이터 보기") { app.navigate(.deviceAnalytics) }
                if app.isAnalyticsConsentGranted { Button("동의 철회 및 기기 내 분석 데이터 삭제", role: .destructive) { app.denyAnalyticsConsent() } }
                else { Button("익명 분석 수집 동의") { app.grantAnalyticsConsent() } }
            }
            Section("버전") { LabeledContent("The Last Commit", value: "1.2") }
        }
        .navigationTitle("설정")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button { dismiss() } label: {
                    Image(systemName: "chevron.left")
                }
            }
        }
    }
}
