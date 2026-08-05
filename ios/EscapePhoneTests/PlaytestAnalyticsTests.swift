import XCTest
@testable import EscapePhone

@MainActor
final class PlaytestAnalyticsTests: XCTestCase {
    private final class MutableDateProvider: DateProviding { var now: Date; init(_ now: Date) { self.now = now } }
    private func makeApp(store: InMemoryGameProgressStore, time: MutableDateProvider, consent: Bool = true) -> AppContainer { let app = AppContainer(store: store, motion: MockMotionController(), haptics: NoOpHapticProvider(), ads: NoOpAdGateway(), device: NoOpPuzzleDeviceConnector(), dateProvider: time); if consent { app.grantAnalyticsConsent() }; return app }

    func test_completePuzzleSession_savesElapsedTime() {
        let store = InMemoryGameProgressStore(); let time = MutableDateProvider(Date(timeIntervalSince1970: 1)); let app = makeApp(store: store, time: time)
        app.startPuzzleSession("encrypted_note"); time.now = Date(timeIntervalSince1970: 4.5); app.completePuzzleSession("encrypted_note"); app.save()
        XCTAssertEqual(store.gameProgress?.puzzleAnalytics["encrypted_note"]?.elapsedMs, 3_500)
    }

    func test_recordWrongAttempt_savesReason() {
        let store = InMemoryGameProgressStore(); let app = makeApp(store: store, time: MutableDateProvider(Date(timeIntervalSince1970: 1)))
        app.recordWrongAttempt("audio_record", reason: "audioOrderIncorrect")
        XCTAssertEqual(store.gameProgress?.puzzleAnalytics["audio_record"]?.wrongAttemptCount, 1); XCTAssertEqual(store.gameProgress?.puzzleAnalytics["audio_record"]?.wrongReasonCounts["audioOrderIncorrect"], 1)
    }

    func test_requestHint_incrementsPuzzleHintViews() {
        let store = InMemoryGameProgressStore(); let app = makeApp(store: store, time: MutableDateProvider(Date(timeIntervalSince1970: 1)))
        app.requestHint("commit_graph.1"); app.requestHint("commit_graph.1")
        XCTAssertEqual(store.gameProgress?.puzzleAnalytics["commit_graph"]?.hintViewCount, 2); XCTAssertEqual(store.gameProgress?.hintCount, 1)
    }

    func test_recordPuzzleExit_savesBackNavigation() {
        let store = InMemoryGameProgressStore(); let time = MutableDateProvider(Date(timeIntervalSince1970: 1)); let app = makeApp(store: store, time: time)
        app.startPuzzleSession("access_log"); time.now = Date(timeIntervalSince1970: 2.25); app.recordPuzzleExit(.backNavigation)
        XCTAssertEqual(store.gameProgress?.puzzleAnalytics["access_log"]?.exitEvents.first?.reason, .backNavigation); XCTAssertEqual(store.gameProgress?.puzzleAnalytics["access_log"]?.exitEvents.first?.elapsedMsAtExit, 1_250)
    }

    func test_submitPlayerFeedback_savesDifficultyAndComment() {
        let store = InMemoryGameProgressStore(); let app = makeApp(store: store, time: MutableDateProvider(Date(timeIntervalSince1970: 1)))
        XCTAssertTrue(app.submitPlayerFeedback(4, comment: "커밋 그래프가 어려웠어요")); XCTAssertEqual(store.gameProgress?.playerFeedback?.difficultyRating, 4); XCTAssertNotNil(store.gameProgress?.playerFeedback)
    }


    func test_denyAnalyticsConsent_doesNotRecordPuzzleAnalytics() {
        let store = InMemoryGameProgressStore(); let app = makeApp(store: store, time: MutableDateProvider(Date(timeIntervalSince1970: 1)), consent: false)
        app.denyAnalyticsConsent(); app.startPuzzleSession("encrypted_note"); app.recordWrongAttempt("encrypted_note", reason: "noteWordOrderIncorrect")
        XCTAssertTrue(store.gameProgress?.puzzleAnalytics.isEmpty == true)
    }

    func test_submitPlayerFeedback_enqueuesFinalJson() {
        let store = InMemoryGameProgressStore(); let app = makeApp(store: store, time: MutableDateProvider(Date(timeIntervalSince1970: 1)))
        app.submitPlayerFeedback(3, comment: "좋아요")
        XCTAssertEqual(store.gameProgress?.pendingAnalyticsUploads.last?.envelope.isFinal, true)
    }
}
