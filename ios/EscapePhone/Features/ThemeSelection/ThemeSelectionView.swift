import SwiftUI

struct ThemeSelectionState {
    var selectedThemeId: ThemeId?
    var themeMetadataList: [ThemeMetadata] = ThemeRegistry.themes
    var themeProgressList: [ThemeProgress] = []
    var selectedThemeProgress: ThemeProgress?
    var isResetConfirmationVisible = false
}

private func themeAccentColor(for themeId: ThemeId) -> Color {
    switch themeId {
    case .theLastCommit: return AppTheme.accent
    case .convenienceStoreLoop: return Color(red: 0.70, green: 0.38, blue: 1.0)
    }
}

struct ThemeSelectionScreen: View {
    let themeMetadataList: [ThemeMetadata]
    let progressFor: (ThemeId) -> ThemeProgress
    let onStartTheme: (ThemeId) -> Void
    let onContinueTheme: (ThemeId) -> Void
    let onOpenSettings: () -> Void
    let onResetAll: () -> Void

    @State private var confirmResetAll = false

    var body: some View {
        ZStack {
            AppTheme.background.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .center, spacing: 16) {
                    Text("EscapePhone").font(.system(.largeTitle, design: .monospaced)).bold().foregroundStyle(AppTheme.textPrimary)
                    Text("모바일 미스터리 방탈출").foregroundStyle(AppTheme.textSecondary).padding(.bottom, 12)
                    ForEach(themeMetadataList) { metadata in
                        ThemeCard(metadata: metadata, progress: progressFor(metadata.themeId), accent: themeAccentColor(for: metadata.themeId), onStart: onStartTheme, onContinue: onContinueTheme)
                    }
                    HStack(spacing: 0) {
                        Button("설정", action: onOpenSettings)
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(AppTheme.textPrimary)
                            .frame(maxWidth: .infinity)
                        Divider().frame(height: 20).overlay(AppTheme.hairline)
                        Button("전체 데이터 초기화") { confirmResetAll = true }
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(AppTheme.warning)
                            .frame(maxWidth: .infinity)
                    }
                    .padding(.vertical, 14)
                    .background(AppTheme.cardElevated)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .padding(.top, 8)
                }.padding(24)
            }
        }
        .alert("전체 데이터 초기화", isPresented: $confirmResetAll) {
            Button("초기화", role: .destructive) { onResetAll() }
            Button("취소", role: .cancel) {}
        } message: { Text("모든 테마의 진행 정보가 삭제됩니다. 계속할까요?") }
    }
}

private struct ThemeCard: View {
    let metadata: ThemeMetadata
    let progress: ThemeProgress
    let accent: Color
    let onStart: (ThemeId) -> Void
    let onContinue: (ThemeId) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(metadata.title).font(.title2.bold()).foregroundStyle(AppTheme.textPrimary)
                Spacer()
                statusBadge
            }
            Text(metadata.subtitle).font(.system(.body, design: .monospaced)).foregroundStyle(accent)
            Text(metadata.description).foregroundStyle(AppTheme.textSecondary)
            Text("예상 플레이 시간 \(metadata.estimatedPlayTimeMinutes)분 · 난이도 \(metadata.difficulty)").font(.caption).foregroundStyle(AppTheme.textSecondary)
            Button(progress.themeStatus == .notStarted ? "시작하기" : "이어하기") {
                if progress.themeStatus == .notStarted { onStart(metadata.themeId) } else { onContinue(metadata.themeId) }
            }
            .buttonStyle(PrimaryButtonStyle())
            .disabled(!metadata.isAvailable)
            .opacity(metadata.isAvailable ? 1 : 0.5)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(LinearGradient(colors: [accent.opacity(0.18), .clear], startPoint: .top, endPoint: .bottom))
        .background(AppTheme.card)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(AppTheme.hairline))
    }

    @ViewBuilder private var statusBadge: some View {
        switch progress.themeStatus {
        case .notStarted: EmptyView()
        case .inProgress: Text("진행 중").foregroundStyle(AppTheme.accent).font(.caption.bold())
        case .completed: Text("완료").foregroundStyle(Color.green).font(.caption.bold())
        }
    }
}
