"""
Tests for the upload build task
"""

from mock import patch, mock_open, Mock
from unittest import TestCase

# Ensure we're properly handling python2 vs 3
from sys import version_info
if version_info.major == 2:
    import __builtin__ as builtins  # pylint: disable=import-error
else:
    import builtins  # pylint: disable=import-error

from .. import upload_build
from .. import exceptions


class UploadBuildTestCase(TestCase):
    """
    Test cases for the upload build task
    """

    def setUp(self):
        self.code_sha = "1234"
        self.hockey_app_token = "some_token"
        self.binary_path = "a/b/c.apk"

    def test_catches_missing_env(self):
        """
        Tests that if we start the task with missing environment variables
        it fails
        """
        with self.assertRaises(exceptions.MissingEnvironmentVariable):
            upload_build.run_upload_build({}, {})

    @patch('requests.post')
    @patch.object(builtins, 'open', new=mock_open(read_data='abc123'))
    def test_request_fails(self, request_mock):
        """
        Tests that if the upload fails we raise an appropriate exception
        """
        request_mock.status_code = 401
        with self.assertRaises(exceptions.UploadFailure):
            upload_build.run_upload_build({}, {
                "CODE_SHA": self.code_sha,
                "HOCKEY_APP_TOKEN": self.hockey_app_token,
                "BINARY_PATH": self.binary_path
            })

    @patch.object(builtins, 'open', new=mock_open(read_data='abc123'))
    def test_request_formatted(self):
        """
        Tests that the request matches our expectations based on our input
        """

        def verify_request(*_, **kwargs):
            # pylint: disable=missing-docstring
            data = kwargs['data']
            self.assertEqual(data['commit_sha'], self.code_sha)
            self.assertEqual(data['notes'], "Example note")

            mock_request = Mock()
            mock_request.headers = {}
            auth = kwargs['auth']
            auth(mock_request)
            self.assertEqual(
                mock_request.headers["X-HockeyAppToken"],
                self.hockey_app_token
            )

            mock_response = Mock()
            mock_response.status_code = 201
            return mock_response

        with patch('requests.post', side_effect=verify_request):
            upload_build.run_upload_build(
                {"BUILD_NOTES": "Example note"},
                {
                    "CODE_SHA": self.code_sha,
                    "HOCKEY_APP_TOKEN": self.hockey_app_token,
                    "BINARY_PATH": self.binary_path,
                }
            )
