import CoreMotion
import Foundation

struct DeviceAttitude: Equatable { let pitch: Double; let roll: Double; let yaw: Double }

protocol MotionController: AnyObject {
    var isAvailable: Bool { get }
    var onAttitudeChanged: ((DeviceAttitude) -> Void)? { get set }
    func start()
    func stop()
    func calibrate()
}

final class IOSMotionController: MotionController {
    private let manager = CMMotionManager()
    private let queue: OperationQueue = { let queue = OperationQueue(); queue.name = "EscapePhone.motion"; queue.maxConcurrentOperationCount = 1; return queue }()
    private var baseline: DeviceAttitude?
    private var running = false
    var onAttitudeChanged: ((DeviceAttitude) -> Void)?
    var isAvailable: Bool { manager.isDeviceMotionAvailable }
    func start() {
        guard isAvailable, !running else { return }; running = true
        manager.deviceMotionUpdateInterval = 1.0 / 60.0
        manager.startDeviceMotionUpdates(to: queue) { [weak self] motion, _ in
            guard let self, let attitude = motion?.attitude else { return }
            let raw = DeviceAttitude(pitch: attitude.pitch, roll: attitude.roll, yaw: attitude.yaw)
            if self.baseline == nil { self.baseline = raw }
            let base = self.baseline ?? raw
            let relative = DeviceAttitude(pitch: raw.pitch - base.pitch, roll: raw.roll - base.roll, yaw: raw.yaw - base.yaw)
            DispatchQueue.main.async { [weak self] in self?.onAttitudeChanged?(relative) }
        }
    }
    func stop() { guard running else { return }; manager.stopDeviceMotionUpdates(); running = false }
    func calibrate() { baseline = nil }
    deinit { manager.stopDeviceMotionUpdates() }
}

final class MockMotionController: MotionController {
    var isAvailable: Bool
    var onAttitudeChanged: ((DeviceAttitude) -> Void)?
    init(isAvailable: Bool = false) { self.isAvailable = isAvailable }
    func start() {}
    func stop() {}
    func calibrate() {}
    func send(_ attitude: DeviceAttitude) { onAttitudeChanged?(attitude) }
}
