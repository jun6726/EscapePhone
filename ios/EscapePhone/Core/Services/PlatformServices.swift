import Foundation
import UIKit

enum HapticEvent { case success, error, selection, digitDiscovered }
protocol HapticProvider { func play(_ event: HapticEvent) }
struct PlatformHapticProvider: HapticProvider {
    func play(_ event: HapticEvent) {
        switch event {
        case .success: UINotificationFeedbackGenerator().notificationOccurred(.success)
        case .error: UINotificationFeedbackGenerator().notificationOccurred(.error)
        case .selection: UISelectionFeedbackGenerator().selectionChanged()
        case .digitDiscovered: UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        }
    }
}
struct NoOpHapticProvider: HapticProvider { func play(_ event: HapticEvent) {} }

enum AdResult: Equatable { case rewarded, skipped, unavailable, failed(String) }
protocol AdGateway { func showRewardedAd() async -> AdResult }
struct NoOpAdGateway: AdGateway { func showRewardedAd() async -> AdResult { .unavailable } }
final class FakeAdGateway: AdGateway {
    var result: AdResult = .rewarded
    init(result: AdResult = .rewarded) { self.result = result }
    func showRewardedAd() async -> AdResult { try? await Task.sleep(for: .milliseconds(350)); return result }
}

enum DeviceCommand: Equatable { case ping, arm, unlock, reset }
enum PuzzleDeviceState: Equatable { case disconnected, connecting, connected, error(String) }
protocol PuzzleDeviceConnector: AnyObject {
    var state: PuzzleDeviceState { get }
    func connect(sessionId: String) async
    func send(_ command: DeviceCommand) async throws
    func disconnect() async
}
final class NoOpPuzzleDeviceConnector: PuzzleDeviceConnector {
    private(set) var state: PuzzleDeviceState = .disconnected
    func connect(sessionId: String) async {}
    func send(_ command: DeviceCommand) async throws {}
    func disconnect() async {}
}
final class FakePuzzleDeviceConnector: PuzzleDeviceConnector {
    var state: PuzzleDeviceState = .disconnected
    func connect(sessionId: String) async { state = .connecting; state = .connected }
    func send(_ command: DeviceCommand) async throws {}
    func disconnect() async { state = .disconnected }
}
protocol PuzzleOutput { func puzzleDidComplete(id: String) async }
struct NoOpPuzzleOutput: PuzzleOutput { func puzzleDidComplete(id: String) async {} }
