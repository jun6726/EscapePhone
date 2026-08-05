import SwiftUI

@main
struct EscapePhoneApp: App {
    @StateObject private var container = AppContainer.live()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup { RootView().environmentObject(container) }
            .onChange(of: scenePhase) { _, phase in
                container.handleScenePhase(phase)
            }
    }
}
