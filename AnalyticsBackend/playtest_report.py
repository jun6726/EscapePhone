import json
import os
import sqlite3
import statistics


SERVER_DIR = os.path.dirname(os.path.abspath(__file__))
DEFAULT_CARROT_SERVER_DIR = os.path.abspath(os.path.join(SERVER_DIR, '..', '..', '..', 'carrot_scanner'))
DB_PATH = os.environ.get(
    'ESCAPEPHONE_PLAYTEST_DB_PATH',
    os.path.join(DEFAULT_CARROT_SERVER_DIR, 'escapephone_playtest.db'),
)

THEME_NAMES = {
    'the_last_commit': 'The Last Commit',
    'convenience_store_loop': '02:17',
}


def median(values):
    return round(statistics.median(values), 1) if values else 0


def mean(values):
    return round(statistics.mean(values), 2) if values else 0


def print_theme_report(theme_id, envelopes):
    reports = [envelope['report'] for envelope in envelopes]
    print(f'\n=== {THEME_NAMES.get(theme_id, theme_id)} ({theme_id}) ===')
    print(f'세션 수: {len(reports)}')
    puzzle_ids = sorted({puzzle_id for report in reports for puzzle_id in report.get('puzzleAnalytics', {})})
    print('\n퍼즐별 지표')
    for puzzle_id in puzzle_ids:
        metrics = [report['puzzleAnalytics'][puzzle_id] for report in reports if puzzle_id in report.get('puzzleAnalytics', {})]
        elapsed = [item.get('elapsedMs', 0) / 1000 for item in metrics]
        wrong = [item.get('wrongAttemptCount', 0) for item in metrics]
        hints = [item.get('hintViewCount', 0) for item in metrics]
        exits = [len(item.get('exitEvents', [])) for item in metrics]
        reasons = {}
        for item in metrics:
            for reason, count in item.get('wrongReasonCounts', {}).items(): reasons[reason] = reasons.get(reason, 0) + count
        print(f'- {puzzle_id}: 표본 {len(metrics)}, 중앙 {median(elapsed)}초, 평균 오답 {mean(wrong)}, 힌트 {mean(hints)}, 이탈 {mean(exits)}')
        if reasons: print('  오답 원인:', ', '.join(f'{key}={value}' for key, value in sorted(reasons.items(), key=lambda pair: -pair[1])))
    feedback = [report['playerFeedback'] for report in reports if report.get('playerFeedback')]
    ratings = [item['difficultyRating'] for item in feedback]
    print(f'\n난이도 응답: {len(ratings)}개, 평균 {mean(ratings)} / 5')
    comments = [item.get('comment', '').strip() for item in feedback if item.get('comment', '').strip()]
    if comments:
        print('최근 의견')
        for comment in comments[:20]: print(f'- {comment}')


def main():
    if not os.path.exists(DB_PATH):
        print('아직 플레이테스트 데이터가 없습니다.')
        return
    conn = sqlite3.connect(DB_PATH)
    rows = conn.execute('''SELECT payload FROM playtest_events e
                           WHERE sequence=(SELECT MAX(sequence) FROM playtest_events
                                           WHERE session_id=e.session_id)
                           ORDER BY received_at DESC''').fetchall()
    conn.close()
    envelopes = [json.loads(row[0]) for row in rows]
    print(f'전체 세션 수: {len(envelopes)}')

    envelopes_by_theme = {}
    for envelope in envelopes:
        theme_id = envelope.get('themeId', 'the_last_commit')
        envelopes_by_theme.setdefault(theme_id, []).append(envelope)

    for theme_id in sorted(envelopes_by_theme, key=lambda key: (key != 'the_last_commit', key)):
        print_theme_report(theme_id, envelopes_by_theme[theme_id])


if __name__ == '__main__':
    main()
