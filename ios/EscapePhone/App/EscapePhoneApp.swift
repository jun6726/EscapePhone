import SwiftUI

@main
struct EscapePhoneApp: App {
    @StateObject private var container = AppContainer.live()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup { AppRootView().environmentObject(container) }
            .onChange(of: scenePhase) { _, phase in
                container.handleScenePhase(phase)
                container.convenienceStoreContainer.handleScenePhaseChange(active: phase == .active)
            }
    }
}
