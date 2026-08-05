import SwiftUI

private struct VirtualApp: Identifiable { let id: String; let title: String; let symbol: String; let route: AppRoute?; let unlocked: Bool; let reason: String }

struct PhoneHomeView: View {
    @EnvironmentObject private var app: AppContainer
    private var apps: [VirtualApp] { [
        .init(id: "message", title: "메신저", symbol: "message.fill", route: .messengerPuzzle, unlocked: true, reason: ""),
        .init(id: "photo", title: "손전등", symbol: "flashlight.on.fill", route: .flashlightPuzzle, unlocked: app.gameProgress.messengerSolved, reason: "메신저 기록을 먼저 복원하세요."),
        .init(id: "note", title: "암호 메모", symbol: "note.text", route: .encryptedNote, unlocked: app.gameProgress.flashlightSolved, reason: "조작 커밋 번호를 먼저 찾으세요."),
        .init(id: "audio", title: "음성 기록", symbol: "waveform", route: .audioRecordPuzzle, unlocked: app.gameProgress.encryptedNoteSolved, reason: "암호 메모를 먼저 해독하세요."),
        .init(id: "graph", title: "커밋 그래프", symbol: "point.3.connected.trianglepath.dotted", route: .commitGraphPuzzle, unlocked: app.gameProgress.audioRecordSolved, reason: "음성 기록을 먼저 복원하세요."),
        .init(id: "logs", title: "접근 로그", symbol: "list.bullet.rectangle", route: .accessLogPuzzle, unlocked: app.gameProgress.commitGraphSolved, reason: "커밋 그래프를 먼저 복구하세요."),
        .init(id: "server", title: "서버", symbol: "terminal.fill", route: .serverConsole, unlocked: app.gameProgress.accessLogSolved, reason: "접근 로그를 먼저 대조하세요.")
    ] }
    var body: some View {
        ZStack {
            LinearGradient(colors: [AppTheme.background, Color(red: 0.04, green: 0.15, blue: 0.16)], startPoint: .topLeading, endPoint: .bottomTrailing).ignoresSafeArea()
            VStack(spacing: 26) {
                HStack { VStack(alignment: .leading) { Text("GHOST OS").font(.headline.monospaced()); Text(app.gameProgress.currentStage.rawValue).font(.caption).foregroundStyle(.secondary) }; Spacer(); Image(systemName: "battery.50percent") }
                LazyVGrid(columns: [.init(.flexible()), .init(.flexible())], spacing: 24) {
                    ForEach(apps) { item in
                        Button {
                            guard item.unlocked else { app.userMessage = item.reason; return }
                            if let route = item.route { app.navigate(route) }
                        } label: {
                            VStack(spacing: 10) {
                                ZStack(alignment: .topTrailing) { RoundedRectangle(cornerRadius: 22).fill(AppTheme.card).frame(width: 82, height: 82).overlay(Image(systemName: item.symbol).font(.system(size: 34)).foregroundStyle(item.unlocked ? AppTheme.accent : .gray)); if !item.unlocked { Image(systemName: "lock.fill").padding(6).background(.black).clipShape(Circle()) } }
                                Text(item.title).foregroundStyle(.white)
                                if !item.unlocked { Text("잠김").font(.caption2).foregroundStyle(.secondary) }
                            }.frame(maxWidth: .infinity)
                        }.accessibilityLabel(item.unlocked ? item.title : "\(item.title), 잠김, \(item.reason)")
                    }
                }
                Spacer()
                Text("증거 수집 \(app.gameProgress.collectedEvidenceIds.count)개").font(.footnote.monospaced()).foregroundStyle(.secondary)
            }.padding(24)
        }.navigationTitle("휴대폰")
    }
}

#Preview("잠긴 앱") { NavigationStack { PhoneHomeView().environmentObject(AppContainer.preview(gameProgress: GameProgress(currentStage: .introCompleted, startedAt: Date()))) } }
