"""
Tests for trigger app build script
"""

import git
import json
from mock import Mock, patch
import os
import shutil
from unittest import TestCase

from .. import trigger_build
from .. import path_constants
from .. import exceptions
from . import utils

TEST_BRANCH = "test-branch"


class Aborted(Exception):
    """
    Helper exception for indicating system exit instead of
    actually exiting the process
    """
    pass


class TriggerBuildTestCase(TestCase):
    """
    Test cases for the trigger build task
    """

    def setUp(self):
        self.repo_path = utils.make_test_repo()
        self.addCleanup(self._clear_repo)

        repo = git.Repo(self.repo_path)
        repo.create_remote('origin', "https://example.com")

    def _clear_repo(self):
        """
        Removes the created repo
        """
        shutil.rmtree(self.repo_path)

    @patch('sys.exit', new=Mock(side_effect=Aborted))
    def test_missing_arguments(self):
        """
        Tests that the command quits when not given
        enough arguments
        """
        with self.assertRaises(Aborted):
            trigger_build.run_trigger_build([], {})

    @patch('git.Remote.pull')
    def test_branch_already_exists(self, _):
        """
        Tests that the task fails if the branch
        already exists
        """
        repo = git.Repo(self.repo_path)
        repo.create_head(TEST_BRANCH, force=False)
        environ = _dummy_environ()
        with self.assertRaises(exceptions.BranchAlreadyExists):
            _trigger_build(self.repo_path, environ)

    def test_missing_env_variable(self):
        """
        Test that a missing expected environment variable causes
        the task to fail.
        """
        with self.assertRaises(exceptions.MissingEnvironmentVariable):
            _trigger_build(self.repo_path, {})

    @patch('git.Remote.pull')
    @patch('git.Remote.push')
    def test_config_file_written(self, _, __):
        """
        Test that the configuration passed in is properly written to
        the expected file and is committed.
        """
        environ = _dummy_environ()
        _trigger_build(self.repo_path, environ)

        config_path = os.path.join(
            self.repo_path,
            path_constants.CONFIG_FILE
        )
        config = json.load(file(config_path))

        for key, value in environ.iteritems():
            self.assertEqual(config[key], value)

    @patch('git.Remote.pull')
    @patch('git.Remote.push')
    def test_branch_setup(self, _, __):
        """
        Verifies that the change has been committed to a new branch
        with the appropriate name
        """
        _trigger_build(self.repo_path, _dummy_environ())

        repo = git.Repo(self.repo_path)
        self.assertEqual(repo.active_branch.name, TEST_BRANCH)

        # verify that the config was committed by adding it again
        # and making sure that didn't actually do anything
        # by checking that there are no diffs
        config_path = os.path.join(
            self.repo_path,
            trigger_build.CONFIG_FILE
        )
        repo.index.add([config_path])
        diffs = repo.head.commit.diff()
        self.assertEqual(len(diffs), 0)


# Helpers


def _trigger_build(repo_path, environ):
    """
    Helper to kick off the trigger build task
    """
    trigger_build.run_trigger_build(
        [
            "--trigger-repo-path", repo_path,
            "--branch-name", TEST_BRANCH
        ],
        environ
    )


def _dummy_environ():
    """
    Generates a test config for all known keys
    """
    result = {}
    for key in trigger_build.EXPECTED_ENVIRONMENT_VARIABLES:
        result[key] = "test-value"
    return result
