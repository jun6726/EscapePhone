package com.example.escapephone.core.game

import com.example.escapephone.core.model.FlashlightTarget
import com.example.escapephone.core.model.PuzzleMessage
import com.example.escapephone.core.model.AudioFragment
import com.example.escapephone.core.model.CommitNode
import kotlin.math.hypot

class MessengerPuzzleEngine(initialMessages: List<PuzzleMessage> = messages.shuffled()) {
    var messages: List<PuzzleMessage> = initialMessages.distinctBy { it.id }; private set
    var isSolved: Boolean = false; private set
    fun moveMessageUp(index: Int) { if (!isSolved && index in messages.indices && index > 0) messages = messages.toMutableList().also { val item = it[index]; it[index] = it[index - 1]; it[index - 1] = item } }
    fun moveMessageDown(index: Int) { if (!isSolved && index in messages.indices && index < messages.lastIndex) messages = messages.toMutableList().also { val item = it[index]; it[index] = it[index + 1]; it[index + 1] = item } }
    fun submitMessengerOrder(): Boolean { if (!isSolved) isSolved = messages.map { it.correctOrder } == messages.indices.toList(); return isSolved }
    companion object {
        val messages = listOf(
            PuzzleMessage("message_1", "도윤", "사진에 남긴 번호부터 확인해. 조작된 커밋을 찾을 수 있어.", "00:42", 0),
            PuzzleMessage("message_2", "민서", "Trace의 수집 기능은 승인 기록이 없어.", "00:56", 1),
            PuzzleMessage("message_3", "도윤", "ROOT-M 로그인과 출입 기록을 반드시 대조해.", "01:18", 2),
            PuzzleMessage("message_4", "민서", "증거 삭제 예약 전에 복구 절차를 끝내야 해.", "01:24", 3)
        )
    }
}

class LowPassFilter(val alpha: Double = 0.16, initialValue: Double = 0.0) {
    val safeAlpha = alpha.takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) ?: 0.16
    var filteredValue = initialValue.takeIf { it.isFinite() } ?: 0.0; private set
    fun update(newValue: Double): Double { if (newValue.isFinite()) filteredValue += safeAlpha * (newValue - filteredValue); return filteredValue }
    fun reset(value: Double = 0.0) { filteredValue = value.takeIf { it.isFinite() } ?: 0.0 }
}

sealed interface FlashlightPuzzleEvent { data class DigitDiscovered(val digit: Int) : FlashlightPuzzleEvent; data object Completed : FlashlightPuzzleEvent }

class FlashlightPuzzleEngine(
    val targets: List<FlashlightTarget> = defaultTargets,
    val detectionRadius: Float = 0.18f,
    discoveredDigits: List<Int> = emptyList()
) {
    var discoveredDigits: List<Int> = discoveredDigits; private set
    var isSolved = discoveredDigits.size >= targets.size; private set
    private var heldDuration = 0.0
    fun updateFlashlightPosition(x: Float, y: Float, deltaTime: Double): FlashlightPuzzleEvent? = updateTargetProgress(clamp(x), clamp(y), deltaTime)
    fun updateTargetProgress(x: Float, y: Float, deltaTime: Double): FlashlightPuzzleEvent? {
        if (isSolved || discoveredDigits.size >= targets.size) return null
        val target = targets[discoveredDigits.size]
        if (distance(x, y, target.x, target.y) > detectionRadius) { heldDuration = 0.0; return null }
        heldDuration += (deltaTime.takeIf { it.isFinite() } ?: 0.0).coerceIn(0.0, 0.1)
        if (heldDuration + 0.000_001 < target.requiredHoldDuration) return null
        heldDuration = 0.0
        discoveredDigits = discoveredDigits + target.digit
        if (discoveredDigits.size == targets.size) { isSolved = true; return FlashlightPuzzleEvent.Completed }
        return FlashlightPuzzleEvent.DigitDiscovered(target.digit)
    }
    fun completeFlashlightPuzzle(): List<Int> { discoveredDigits = targets.map { it.digit }; isSolved = true; return discoveredDigits }
    companion object {
        val defaultTargets = listOf(FlashlightTarget("target_4", 4, .2f, .25f, 1.0), FlashlightTarget("target_1", 1, .78f, .38f, 1.0), FlashlightTarget("target_7", 7, .42f, .78f, 1.0))
        fun clamp(value: Float) = value.coerceIn(0f, 1f)
        fun distance(x1: Float, y1: Float, x2: Float, y2: Float) = hypot(x1 - x2, y1 - y2)
    }
}

enum class ServerCodeResult { incomplete, incorrect, success }
class ServerCodeEngine {
    val serverCode = "417121"
    var serverCodeInput = ""; private set
    val isServerCodeValid get() = serverCodeInput == serverCode
    fun appendServerCodeDigit(digit: Int) { if (digit in 0..9 && serverCodeInput.length < 6) serverCodeInput += digit }
    fun removeServerCodeDigit() { if (serverCodeInput.isNotEmpty()) serverCodeInput = serverCodeInput.dropLast(1) }
    fun clearServerCode() { serverCodeInput = "" }
    fun submitServerCode(): ServerCodeResult = if (serverCodeInput.length < 6) ServerCodeResult.incomplete else if (isServerCodeValid) ServerCodeResult.success else ServerCodeResult.incorrect
}

class EncryptedNotePuzzleEngine {
    val noteWords = listOf("기록", "삭제", "수집", "완료", "중", "외부")
    var selectedWords: List<String> = emptyList(); private set
    var isUnlocked = false; private set
    var isSolved = false; private set
    fun unlock(code: String): Boolean { isUnlocked = code == "417"; return isUnlocked }
    fun selectNoteWord(word: String) { if (isUnlocked && !isSolved && word in noteWords && word !in selectedWords) selectedWords = selectedWords + word }
    fun removeNoteWord() { if (!isSolved && selectedWords.isNotEmpty()) selectedWords = selectedWords.dropLast(1) }
    fun submitEncryptedNote(): Boolean { isSolved = isUnlocked && selectedWords == listOf("기록", "수집", "중"); return isSolved }
    fun completeEncryptedNotePuzzle(): Boolean { isUnlocked = true; selectedWords = listOf("기록", "수집", "중"); isSolved = true; return true }
}

class AudioRecordPuzzleEngine(initialFragments: List<AudioFragment> = fragments.shuffled()) {
    var fragments: List<AudioFragment> = initialFragments.distinctBy { it.id }; private set
    var playingFragmentId: String? = null; private set
    var isSolved = false; private set
    fun playAudioFragment(fragmentId: String) { if (fragments.any { it.id == fragmentId }) playingFragmentId = fragmentId }
    fun moveAudioFragmentUp(index: Int) { if (!isSolved && index in fragments.indices && index > 0) fragments = fragments.toMutableList().also { values -> val current = values[index]; values[index] = values[index - 1]; values[index - 1] = current } }
    fun moveAudioFragmentDown(index: Int) { if (!isSolved && index in fragments.indices && index < fragments.lastIndex) fragments = fragments.toMutableList().also { values -> val current = values[index]; values[index] = values[index + 1]; values[index + 1] = current } }
    fun submitAudioOrder(): Boolean { if (!isSolved) isSolved = fragments.map { it.correctOrder } == fragments.indices.toList(); return isSolved }
    fun completeAudioRecordPuzzle(): Boolean { fragments = Companion.fragments; isSolved = true; return true }
    companion object {
        val fragments = listOf(
            AudioFragment("audio_1", "이 기능은 승인된 적이 없습니다.", 0),
            AudioFragment("audio_2", "이미 위에서 결정됐어.", 1),
            AudioFragment("audio_3", "기록을 남기면 너도 책임을 피할 수 없어.", 2),
            AudioFragment("audio_4", "출시 전까지 원래대로 되돌려 놔.", 3)
        )
    }
}

class CommitGraphPuzzleEngine {
    val branches = listOf("main", "release", "analytics", "hotfix")
    val nodes = defaultNodes
    var nodeOrder: List<String> = nodes.map { it.id }.reversed(); private set
    var placements: Map<String, String> = emptyMap(); private set
    var connections: Set<String> = emptySet(); private set
    var isSolved = false; private set
    fun moveCommitNode(nodeId: String, branch: String) { if (!isSolved && nodes.any { it.id == nodeId } && branch in branches) placements = placements + (nodeId to branch) }
    fun moveCommitNode(nodeId: String, newIndex: Int) { if (!isSolved && nodeId in nodeOrder && newIndex in nodeOrder.indices) nodeOrder = nodeOrder.toMutableList().also { val oldIndex = it.indexOf(nodeId); it.removeAt(oldIndex); it.add(newIndex, nodeId) } }
    fun connectCommitNode(fromId: String, toId: String) { if (!isSolved && nodes.any { it.id == fromId } && nodes.any { it.id == toId }) connections = connections + "$fromId->$toId" }
    fun validateCommitGraph(): Boolean = nodes.all { placements[it.id] == it.correctBranch } && nodeOrder == nodes.sortedBy { it.correctOrder }.map { it.id } && "417->418" in connections && "418->420" in connections
    fun submitCommitGraph(): Boolean { isSolved = validateCommitGraph(); return isSolved }
    fun completeCommitGraphPuzzle(): Boolean { placements = nodes.associate { it.id to it.correctBranch }; nodeOrder = nodes.sortedBy { it.correctOrder }.map { it.id }; connections = setOf("417->418", "418->420"); isSolved = true; return true }
    companion object {
        val defaultNodes = listOf(
            CommitNode("412", 412, "로그인 오류 수정", "main", 0),
            CommitNode("414", 414, "출시 후보 빌드", "release", 1),
            CommitNode("416", 416, "센서 분석 실험", "analytics", 2),
            CommitNode("417", 417, "백그라운드 수집 활성화", "analytics", 3),
            CommitNode("418", 418, "작성자 정보 수정", "analytics", 4),
            CommitNode("420", 420, "출시 빌드 병합", "main", 5)
        )
    }
}

class AccessLogPuzzleEngine {
    val correctAnswers = listOf("없었다", "예약 메시지다", "관리자 공용 계정이다")
    var selectedEvidenceIds: Set<String> = emptySet(); private set
    var answers: Map<Int, String> = emptyMap(); private set
    var isSolved = false; private set
    fun selectLogEvidence(evidenceId: String) { selectedEvidenceIds = selectedEvidenceIds + evidenceId }
    fun answerLogQuestion(questionIndex: Int, answer: String) { if (!isSolved && questionIndex in correctAnswers.indices) answers = answers + (questionIndex to answer) }
    fun validateAccessLogAnswers(): Boolean = correctAnswers.indices.all { answers[it] == correctAnswers[it] }
    fun completeAccessLogPuzzle(): Boolean { isSolved = validateAccessLogAnswers(); return isSolved }
}
