import XCTest
@testable import EscapePhone

@MainActor
final class ConvenienceStoreThemeTests: XCTestCase {
    func testSelectTheme_withTheLastCommit_opensCorrectMenu() {
        let store = InMemoryThemeProgressStore()
        var progress = store.loadThemeProgress(themeId: .theLastCommit)
        progress.themeStatus = .inProgress
        store.saveThemeProgress(progress)
        XCTAssertEqual(store.loadThemeProgress(themeId: .theLastCommit).themeId, .theLastCommit)
    }

    func testSelectTheme_withConvenienceStoreLoop_opensCorrectMenu() {
        let store = InMemoryThemeProgressStore()
        let container = ConvenienceStoreContainer(themeProgressStore: store)
        container.startNewGame()
        XCTAssertTrue(container.hasSavedGame)
        XCTAssertEqual(store.loadThemeProgress(themeId: .convenienceStoreLoop).themeId, .convenienceStoreLoop)
    }

    func testSaveAndLoad_convenienceStoreProgress_restoresState() {
        let store = InMemoryThemeProgressStore()
        let container = ConvenienceStoreContainer(themeProgressStore: store)
        container.startNewGame()
        container.completeIntro()
        container.completeReceiptPuzzle()
        let reloaded = ConvenienceStoreContainer(themeProgressStore: store)
        XCTAssertTrue(reloaded.progress.receiptSolved)
    }

    func testResetConvenienceStoreProgress_keepsTheLastCommitProgress() {
        let store = InMemoryThemeProgressStore()
        var lastCommit = store.loadThemeProgress(themeId: .theLastCommit); lastCommit.hintCount = 4
        store.saveThemeProgress(lastCommit)
        let container = ConvenienceStoreContainer(themeProgressStore: store)
        container.startNewGame()
        container.reset()
        XCTAssertEqual(store.loadThemeProgress(themeId: .theLastCommit).hintCount, 4)
    }

    func testSelectPublicDisclosure_savesConvenienceStoreEnding() {
        let store = InMemoryThemeProgressStore()
        let container = ConvenienceStoreContainer(themeProgressStore: store)
        container.startNewGame()
        container.selectPublicDisclosure()
        XCTAssertEqual(container.progress.endingType, .publicDisclosure)
    }

    func testSelectEncryptedArchive_savesConvenienceStoreEnding() {
        let store = InMemoryThemeProgressStore()
        let container = ConvenienceStoreContainer(themeProgressStore: store)
        container.startNewGame()
        container.selectEncryptedArchive()
        XCTAssertEqual(container.progress.endingType, .encryptedArchive)
    }

    func testCompleteConvenienceStoreTheme_marksThemeCompleted() {
        let store = InMemoryThemeProgressStore()
        let container = ConvenienceStoreContainer(themeProgressStore: store)
        container.startNewGame()
        container.selectPublicDisclosure()
        let theme = store.loadThemeProgress(themeId: .convenienceStoreLoop)
        XCTAssertEqual(theme.themeStatus, .completed)
        if case .convenienceStoreLoop = theme.themeSpecificState {} else { XCTFail("expected convenienceStoreLoop state") }
    }
}
