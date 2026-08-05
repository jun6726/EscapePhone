import SwiftUI

struct MainMenuView: View {
    @EnvironmentObject private var app: AppContainer
    @State private var confirmNewGame = false
    @State private var confirmReset = false
    @State private var showAbout = false
#if DEBUG
    @State private var titleTaps = 0
#endif
    var body: some View {
        ZStack {
            LinearGradient(colors: [AppTheme.background, .black], startPoint: .top, endPoint: .bottom).ignoresSafeArea()
            ScrollView {
                VStack(spacing: 18) {
                    Image(systemName: "iphone.gen3.radiowaves.left.and.right").font(.system(size: 66)).foregroundStyle(AppTheme.accent).accessibilityHidden(true)
                    Text("THE LAST COMMIT").font(.system(.largeTitle, design: .monospaced, weight: .bold)).multilineTextAlignment(.center)
                    Text("사라진 개발자의 휴대폰").foregroundStyle(.secondary)
                    Text("SYSTEM // RECOVERY MODE").font(.caption.monospaced()).foregroundStyle(AppTheme.accent)
                        .onTapGesture {
#if DEBUG
                            titleTaps += 1
                            if titleTaps >= 5 { titleTaps = 0; app.navigate(.developerMenu) }
#endif
                        }
                    VStack(spacing: 12) {
                        Button("새 게임") { app.hasSavedGame ? (confirmNewGame = true) : app.startNewGame() }.buttonStyle(PrimaryButtonStyle())
                        Button("이어하기") { app.continueGame() }.buttonStyle(PrimaryButtonStyle()).disabled(!app.hasSavedGame).opacity(app.hasSavedGame ? 1 : 0.4)
                        Button("설정") { app.navigate(.settings) }.frame(maxWidth: .infinity).padding().background(AppTheme.card).clipShape(RoundedRectangle(cornerRadius: 14))
                        Button("게임 소개") { showAbout = true }.frame(maxWidth: .infinity).padding()
                        if app.hasSavedGame { Button("게임 초기화", role: .destructive) { confirmReset = true }.padding() }
                    }.padding(.top, 28)
                    Text("25–35분 · 익명 분석 선택 · 센서 또는 터치").font(.footnote).foregroundStyle(.secondary)
                }.padding(28).frame(maxWidth: 520)
            }
        }
        .confirmationDialog("저장된 진행을 지우고 새 게임을 시작할까요?", isPresented: $confirmNewGame, titleVisibility: .visible) { Button("새 게임 시작", role: .destructive) { app.startNewGame() }; Button("취소", role: .cancel) {} }
        .confirmationDialog("모든 진행 정보를 삭제할까요?", isPresented: $confirmReset, titleVisibility: .visible) { Button("완전히 초기화", role: .destructive) { app.resetGame() }; Button("취소", role: .cancel) {} }
        .sheet(isPresented: $showAbout) { NavigationStack { ScrollView { Text("출시를 하루 앞두고 사라진 개발자의 휴대폰에서 손상된 기록을 복원하는 모바일 방탈출 데모입니다. 동의한 경우에만 익명 플레이 분석 JSON을 화면 로딩을 막지 않는 방식으로 전송합니다.").padding() }.navigationTitle("게임 소개").toolbar { Button("닫기") { showAbout = false } } } }
    }
}

#Preview { MainMenuView().environmentObject(AppContainer.preview()) }
