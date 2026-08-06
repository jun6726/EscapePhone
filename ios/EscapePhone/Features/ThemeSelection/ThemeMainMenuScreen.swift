import SwiftUI

struct ThemeMainMenuScreen: View {
    let themeId: ThemeId
    let themeMetadata: ThemeMetadata
    let themeProgress: ThemeProgress
    let onBackToThemeSelection: () -> Void
    var convenienceStoreContainer: ConvenienceStoreContainer?
    var onResetTheme: (() -> Void)?
    var onEnterConvenienceStore: (() -> Void)?

    var body: some View {
        switch themeId {
        case .theLastCommit:
            MainMenuView(onBackToThemeSelection: onBackToThemeSelection)
        case .convenienceStoreLoop:
            if let container = convenienceStoreContainer {
                ConvenienceStoreMainMenu(themeMetadata: themeMetadata, container: container, onBackToThemeSelection: onBackToThemeSelection, onResetTheme: onResetTheme, onEnterConvenienceStore: onEnterConvenienceStore)
            }
        }
    }
}

private struct ConvenienceStoreMainMenu: View {
    let themeMetadata: ThemeMetadata
    @ObservedObject var container: ConvenienceStoreContainer
    let onBackToThemeSelection: () -> Void
    var onResetTheme: (() -> Void)?
    var onEnterConvenienceStore: (() -> Void)?

    @State private var confirmNewGame = false
    @State private var confirmReset = false
    @State private var showAbout = false

    var body: some View {
        ZStack(alignment: .topLeading) {
            AppTheme.background.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 16) {
                    Text("⏱").font(.system(size: 60)).foregroundStyle(StoreTheme.accent)
                    Text(themeMetadata.title).font(.system(.largeTitle, design: .monospaced, weight: .bold)).foregroundStyle(AppTheme.textPrimary)
                    Text(themeMetadata.subtitle).foregroundStyle(AppTheme.textSecondary)
                    Button("새 게임") { if container.hasSavedGame { confirmNewGame = true } else { container.startNewGame(); onEnterConvenienceStore?() } }.buttonStyle(PrimaryButtonStyle())
                    Button("이어하기") { container.continueGame(); onEnterConvenienceStore?() }.buttonStyle(PrimaryButtonStyle()).disabled(!container.hasSavedGame).opacity(container.hasSavedGame ? 1 : 0.4)
                    Button("테마 소개") { showAbout = true }.buttonStyle(SecondaryButtonStyle())
                    if container.hasSavedGame { Button("테마 진행 초기화") { confirmReset = true }.buttonStyle(GhostButtonStyle()).foregroundStyle(AppTheme.warning) }
                    Text("\(themeMetadata.estimatedPlayTimeMinutes)분 · 센서 또는 터치").font(.footnote).foregroundStyle(AppTheme.textSecondary)
                }.padding(28).padding(.top, 36)
            }
            BackButton { onBackToThemeSelection() }.padding(20)
        }
        .confirmationDialog("저장된 진행을 지우고 새 게임을 시작할까요?", isPresented: $confirmNewGame, titleVisibility: .visible) { Button("새 게임 시작", role: .destructive) { container.startNewGame(); onEnterConvenienceStore?() }; Button("취소", role: .cancel) {} }
        .confirmationDialog("02:17 테마의 진행 정보만 삭제됩니다.", isPresented: $confirmReset, titleVisibility: .visible) { Button("초기화", role: .destructive) { container.reset(); onResetTheme?() }; Button("취소", role: .cancel) {} }
        .sheet(isPresented: $showAbout) { NavigationStack { ScrollView { Text(themeMetadata.description).padding() }.navigationTitle("게임 소개").toolbar { Button("닫기") { showAbout = false } } } }
    }
}
