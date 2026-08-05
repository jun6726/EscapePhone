import Foundation

enum AppRoute: Hashable {
    case intro, phoneHome, messengerPuzzle, flashlightPuzzle, encryptedNote, audioRecordPuzzle, commitGraphPuzzle, accessLogPuzzle, serverConsole, finalDecision, ending, settings, deviceAnalytics
#if DEBUG
    case developerMenu
#endif
}

extension AppRoute {
    var puzzleId: String? {
        switch self {
        case .messengerPuzzle: return "messenger_order"
        case .flashlightPuzzle: return "flashlight_search"
        case .encryptedNote: return "encrypted_note"
        case .audioRecordPuzzle: return "audio_record"
        case .commitGraphPuzzle: return "commit_graph"
        case .accessLogPuzzle: return "access_log"
        case .serverConsole: return "server_code"
        default: return nil
        }
    }
}
