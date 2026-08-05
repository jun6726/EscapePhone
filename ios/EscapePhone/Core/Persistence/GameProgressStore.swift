import Foundation

protocol GameProgressStore {
    func load() throws -> GameProgress?
    func save(_ gameProgress: GameProgress) throws
    func reset() throws
}

enum ProgressStoreError: LocalizedError { case corruptedData
    var errorDescription: String? { "저장된 진행 정보를 읽을 수 없습니다." }
}

final class PlatformGameProgressStore: GameProgressStore {
    private let defaults: UserDefaults
    private let key: String
    init(defaults: UserDefaults = .standard, key: String = "escape_phone_game_progress_v1") { self.defaults = defaults; self.key = key }
    func load() throws -> GameProgress? {
        guard let data = defaults.data(forKey: key) else { return nil }
        do { return try JSONDecoder().decode(GameProgress.self, from: data) }
        catch { throw ProgressStoreError.corruptedData }
    }
    func save(_ gameProgress: GameProgress) throws { defaults.set(try JSONEncoder().encode(gameProgress), forKey: key) }
    func reset() throws { defaults.removeObject(forKey: key) }
}

final class InMemoryGameProgressStore: GameProgressStore {
    var gameProgress: GameProgress?
    init(gameProgress: GameProgress? = nil) { self.gameProgress = gameProgress }
    func load() throws -> GameProgress? { gameProgress }
    func save(_ gameProgress: GameProgress) throws { self.gameProgress = gameProgress }
    func reset() throws { gameProgress = nil }
}

protocol DateProviding { var now: Date { get } }
struct SystemDateProvider: DateProviding { var now: Date { Date() } }
struct FixedDateProvider: DateProviding { let now: Date }
