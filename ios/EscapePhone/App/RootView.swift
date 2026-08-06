import SwiftUI

struct AppRootView: View {
    @EnvironmentObject private var app: AppContainer
    var body: some View {
        switch app.launchRoute {
        case .themeSelection:
            ThemeSelectionScreen(
                themeMetadataList: app.themeMetadataList(),
                progressFor: { app.themeProgress(for: $0) },
                onStartTheme: { app.startTheme($0) },
                onContinueTheme: { app.continueTheme($0) },
                onOpenSettings: { app.openThemeSettings() },
                onResetAll: { app.resetAllThemeProgress() }
            )
            .id(app.themeProgressVersion)
        case .themeMainMenu(let themeId):
            ThemeMainMenuScreen(
                themeId: themeId,
                themeMetadata: ThemeRegistry.metadata(for: themeId),
                themeProgress: app.themeProgress(for: themeId),
                onBackToThemeSelection: { app.returnToThemeSelection() },
                convenienceStoreContainer: app.convenienceStoreContainer,
                onResetTheme: { app.themeProgressVersion += 1 },
                onEnterConvenienceStore: { app.enterConvenienceStore() }
            )
        case .convenienceStorePlaying:
            ConvenienceStoreApp(container: app.convenienceStoreContainer, onExitToThemeMenu: { app.themeProgressVersion += 1; app.launchRoute = .themeMainMenu(.convenienceStoreLoop) })
        case .theLastCommit:
            RootView()
        }
    }
}

struct RootView: View {
    @EnvironmentObject private var app: AppContainer
    var body: some View {
        NavigationStack(path: $app.path) {
            MainMenuView()
                .navigationDestination(for: AppRoute.self) { route in
                    switch route {
                    case .intro: IntroView()
                    case .phoneHome: PhoneHomeView()
                    case .messengerPuzzle: MessengerPuzzleView()
                    case .flashlightPuzzle: FlashlightPuzzleView()
                    case .encryptedNote: EncryptedNoteView()
                    case .audioRecordPuzzle: AudioRecordPuzzleView()
                    case .commitGraphPuzzle: CommitGraphPuzzleView()
                    case .accessLogPuzzle: AccessLogPuzzleView()
                    case .serverConsole: ServerConsoleView()
                    case .finalDecision: FinalDecisionView()
                    case .ending: EndingView()
                    case .settings: SettingsView()
                    case .deviceAnalytics: DeviceAnalyticsView()
#if DEBUG
                    case .developerMenu: DeveloperMenuView()
#endif
                    }
                }
        }
        .tint(AppTheme.accent)
        .preferredColorScheme(.dark)
        .onChange(of: app.path) { oldPath, newPath in app.handleRouteChange(from: oldPath.last, to: newPath.last) }
        .fullScreenCover(isPresented: Binding(get: { app.gameProgress.analyticsConsentStatus == .notDetermined }, set: { _ in })) { AnalyticsConsentView().environmentObject(app).interactiveDismissDisabled() }
        .alert("알림", isPresented: Binding(get: { app.userMessage != nil }, set: { if !$0 { app.userMessage = nil } })) { Button("확인") { app.userMessage = nil } } message: { Text(app.userMessage ?? "") }
    }
}

private struct AnalyticsConsentView: View {
    @EnvironmentObject var app: AppContainer
    var body: some View {
        ZStack {
            AppTheme.background.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Image(systemName: "chart.xyaxis.line").font(.system(size: 54)).foregroundStyle(AppTheme.accent)
                    Text("익명 플레이 분석에 참여할까요?").font(.largeTitle.bold())
                    Text("게임 개선을 위해 다음 정보를 익명 세션 ID와 함께 JSON으로 수집합니다.").font(.title3)
                    Group { Label("퍼즐별 소요 시간과 재진입", systemImage: "timer"); Label("오답 횟수와 자동 분류된 원인", systemImage: "xmark.circle"); Label("힌트 조회와 뒤로가기·백그라운드 이탈", systemImage: "lightbulb"); Label("선택 입력한 난이도와 자유 의견", systemImage: "text.bubble") }
                    Text("계정, 위치, 사진, 연락처, 마이크와 광고 식별자는 수집하지 않습니다. 화면 로딩을 막지 않는 비동기 방식으로 전송하며, 설정에서 언제든 철회하고 분석 데이터를 삭제할 수 있습니다.").foregroundStyle(.secondary)
                    Button("동의하고 계속") { app.grantAnalyticsConsent() }.buttonStyle(PrimaryButtonStyle())
                    Button("동의하지 않음") { app.denyAnalyticsConsent() }.frame(maxWidth: .infinity).padding()
                }.padding(28)
            }
        }
    }
}
