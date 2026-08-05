import os
import sys
import tempfile
import unittest
import uuid

TESTS_DIR = os.path.dirname(os.path.abspath(__file__))
DEFAULT_CARROT_SERVER_DIR = os.path.abspath(
    os.path.join(TESTS_DIR, '..', '..', '..', '..', 'carrot_scanner')
)
CARROT_SERVER_DIR = os.environ.get('CARROT_SCANNER_SERVER_DIR', DEFAULT_CARROT_SERVER_DIR)
if CARROT_SERVER_DIR not in sys.path:
    sys.path.insert(0, CARROT_SERVER_DIR)

import app as server


class PlaytestApiTests(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        server.PLAYTEST_DB_PATH = os.path.join(self.temp_dir.name, 'playtest.db')
        self.previous_admin_token = os.environ.get('PLAYTEST_ADMIN_TOKEN')
        os.environ['PLAYTEST_ADMIN_TOKEN'] = 'test-admin-token'
        server.app.config['TESTING'] = True
        self.client = server.app.test_client()

    def tearDown(self):
        if self.previous_admin_token is None:
            os.environ.pop('PLAYTEST_ADMIN_TOKEN', None)
        else:
            os.environ['PLAYTEST_ADMIN_TOKEN'] = self.previous_admin_token
        self.temp_dir.cleanup()

    def payload(self):
        return {
            'schemaVersion': 1,
            'sessionId': str(uuid.uuid4()),
            'sequence': 1,
            'platform': 'android',
            'appVersion': '1.2',
            'consentVersion': 1,
            'isFinal': False,
            'createdAt': 1_700_000_000_000,
            'report': {
                'puzzleAnalytics': {
                    'encrypted_note': {
                        'puzzleId': 'encrypted_note',
                        'elapsedMs': 1200,
                        'wrongAttemptCount': 1,
                        'wrongReasonCounts': {'noteWordOrderIncorrect': 1},
                        'hintViewCount': 1,
                        'exitEvents': []
                    }
                },
                'playerFeedback': None
            }
        }

    def test_receive_playtest_event_accepts_valid_json(self):
        response = self.client.post('/v1/playtest-events', json=self.payload())
        self.assertEqual(response.status_code, 202)
        self.assertFalse(response.get_json()['duplicate'])

    def test_receive_playtest_event_is_idempotent(self):
        payload = self.payload()
        self.client.post('/v1/playtest-events', json=payload)
        response = self.client.post('/v1/playtest-events', json=payload)
        self.assertEqual(response.status_code, 202)
        self.assertTrue(response.get_json()['duplicate'])

    def test_receive_playtest_event_rejects_unknown_puzzle(self):
        payload = self.payload()
        payload['report']['puzzleAnalytics'] = {'unknown': {}}
        response = self.client.post('/v1/playtest-events', json=payload)
        self.assertEqual(response.status_code, 400)

    def test_playtest_health_reports_event_count(self):
        self.client.post('/v1/playtest-events', json=self.payload())
        response = self.client.get('/v1/playtest-events/health')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.get_json()['eventCount'], 1)

    def test_playtest_report_data_requires_admin(self):
        response = self.client.get('/v1/playtest-report/data')
        self.assertEqual(response.status_code, 404)

    def test_playtest_report_data_aggregates_latest_session(self):
        payload = self.payload()
        self.client.post('/v1/playtest-events', json=payload)
        payload['sequence'] = 2
        payload['isFinal'] = True
        payload['report']['puzzleAnalytics']['encrypted_note']['completedAt'] = 1_700_000_001_200
        payload['report']['playerFeedback'] = {
            'difficultyRating': 4,
            'comment': '단서가 조금 어려웠어요.',
            'submittedAt': 1_700_000_002_000,
        }
        self.client.post('/v1/playtest-events', json=payload)

        response = self.client.get(
            '/v1/playtest-report/data',
            headers={'X-Admin-Token': 'test-admin-token'},
        )
        self.assertEqual(response.status_code, 200)
        report = response.get_json()
        self.assertEqual(report['totalSessions'], 1)
        self.assertEqual(report['finalReports'], 1)
        self.assertEqual(report['feedback']['averageDifficulty'], 4)
        encrypted_note = next(item for item in report['puzzles'] if item['puzzleId'] == 'encrypted_note')
        self.assertEqual(encrypted_note['startedCount'], 1)
        self.assertEqual(encrypted_note['completedCount'], 1)

    def test_playtest_report_login_sets_secure_session_cookie(self):
        response = self.client.post(
            '/playtest-report',
            data={'admin_token': 'test-admin-token'},
        )
        self.assertEqual(response.status_code, 302)
        cookie = response.headers['Set-Cookie']
        self.assertIn('escapephone_report_session=', cookie)
        self.assertIn('Secure', cookie)
        self.assertIn('HttpOnly', cookie)
        self.assertIn('SameSite=Strict', cookie)


if __name__ == '__main__':
    unittest.main()
