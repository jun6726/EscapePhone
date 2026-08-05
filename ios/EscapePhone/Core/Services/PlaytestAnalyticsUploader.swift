import Foundation

protocol PlaytestAnalyticsUploader {
    var isConfigured: Bool { get }
    func upload(_ jsonPayload: Data) async -> Bool
}

struct NoOpPlaytestAnalyticsUploader: PlaytestAnalyticsUploader {
    let isConfigured = false
    func upload(_ jsonPayload: Data) async -> Bool { false }
}

struct HTTPPlaytestAnalyticsUploader: PlaytestAnalyticsUploader {
    static let defaultEndpoint = "https://s-imac.coati-bramble.ts.net/v1/playtest-events"
    let endpointURL: URL?
    init(endpoint: String? = nil) {
        let bundled = Bundle.main.object(forInfoDictionaryKey: "PLAYTEST_ANALYTICS_ENDPOINT") as? String
        let configured = endpoint ?? ((bundled?.isEmpty == false) ? bundled : Self.defaultEndpoint)
        endpointURL = configured.flatMap(URL.init(string:))
    }
    var isConfigured: Bool { endpointURL?.scheme?.lowercased() == "https" }

    func upload(_ jsonPayload: Data) async -> Bool {
        guard isConfigured, let endpointURL else { return false }
        var request = URLRequest(url: endpointURL)
        request.httpMethod = "POST"
        request.timeoutInterval = 8
        request.httpBody = jsonPayload
        request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        do {
            let (_, response) = try await URLSession.shared.data(for: request)
            return (response as? HTTPURLResponse).map { (200...299).contains($0.statusCode) } ?? false
        } catch { return false }
    }
}
