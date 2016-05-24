"""
Tests for the checkout build repos script
"""

import git
import json
import os
import shutil
import tempfile

from .. import checkout_build_repos
from ..test import utils
from ..path_constants import (
    CODE_CHECKOUT_DIRECTORY,
    CONFIG_CHECKOUT_DIRECTORY,
    PROPERTIES_FILE
)

from unittest import TestCase


class CheckoutBuildReposTestCase(TestCase):
    """
    Tests for checkout build repos scripts
    """

    def setUp(self):
        self.code_repo_path = utils.make_test_repo()
        self.config_repo_path = utils.make_test_repo()
        self.environment_path = self._make_environment()
        self.checkout_path = tempfile.mkdtemp()
        self.addCleanup(self._clear_files)

        # Make a branch on each repo
        repos = [
            (self.code_repo_path, "code"),
            (self.config_repo_path, "config")
        ]
        for (repo_path, name) in repos:
            repo = git.Repo(repo_path)
            branch_name = "test-branch-%s" % name
            repo.create_head(branch_name)

            # Add some data to make sure our branch is different from master
            repo.refs[branch_name].checkout()
            stub_file_path = os.path.join(repo_path, "on-branch")
            with open(stub_file_path, "w") as stub:
                stub.write("test data")
            repo.index.add([stub_file_path])
            repo.index.commit("Commit stub file")

            # Now add something to master so they've properly diverged
            repo.refs.master.checkout()
            stub_file_path = os.path.join(repo_path, "on-master")
            with open(stub_file_path, "w") as stub:
                stub.write("test data")
            repo.index.add([stub_file_path])
            repo.index.commit("Commit stub file")

    def test_repos_checked_out(self):
        """
        Tests that we properly cloned the code and config repos
        and set them them to the correct branch
        """
        checkout_build_repos.checkout_repos(
            self.environment_path,
            {"EDX_PROPERTIES_PATH": ""},
            self.checkout_path
        )

        # Check repos exist
        code_checkout = os.path.join(
            self.checkout_path,
            CODE_CHECKOUT_DIRECTORY
        )
        config_checkout = os.path.join(
            self.checkout_path,
            CONFIG_CHECKOUT_DIRECTORY
        )
        code_repo = git.Repo(code_checkout)
        config_repo = git.Repo(config_checkout)

        # Check that we're on the right branch
        self.assertEqual(
            code_repo.active_branch.name,
            "test-branch-code"
        )
        self.assertEqual(
            config_repo.active_branch.name,
            "test-branch-config"
        )

        # And that branch is at the tip
        self.assertEqual(
            config_repo.active_branch.commit,
            config_repo.remotes.origin.refs["test-branch-config"].commit
        )
        self.assertEqual(
            code_repo.active_branch.commit,
            code_repo.remotes.origin.refs["test-branch-code"].commit
        )

    def test_properties_content(self):
        """
        Verify that the edx.properties file created by the script
        exists and points at the write file.
        """
        checkout_build_repos.checkout_repos(
            self.environment_path,
            {"EDX_PROPERTIES_PATH": ""},
            self.checkout_path
        )

        config_checkout_path = os.path.join(
            self.checkout_path,
            CONFIG_CHECKOUT_DIRECTORY
        )
        config_path = os.path.join(config_checkout_path, "config_path")

        code_path = os.path.join(
            self.checkout_path,
            CODE_CHECKOUT_DIRECTORY
        )
        properties_path = os.path.join(code_path, PROPERTIES_FILE)
        with file(properties_path) as properties_file:
            data = properties_file.read()
            self.assertEqual(data, "edx.dir = \"%s\"" % config_path)

    def test_properties_nested(self):
        """
        Test that the script respects nesting the edx.properties file
        inside the code repository.
        """
        checkout_build_repos.checkout_repos(
            self.environment_path,
            {"EDX_PROPERTIES_PATH": "NestedPath"},
            self.checkout_path
        )

        code_path = os.path.join(
            self.checkout_path,
            CODE_CHECKOUT_DIRECTORY
        )
        properties_container = os.path.join(code_path, "NestedPath")
        properties_path = os.path.join(properties_container, PROPERTIES_FILE)
        with file(properties_path) as properties_file:
            data = properties_file.read()
            self.assertTrue(data, "edx.dir")

    def _clear_files(self):
        """
        Removes all temporary test files
        """
        shutil.rmtree(self.checkout_path)
        shutil.rmtree(self.code_repo_path)
        shutil.rmtree(self.config_repo_path)
        os.remove(self.environment_path)

    def _make_environment(self):
        """
        Creates an on disk representation of a test environment
        for use with the build scripts
        """
        (environment_fd, path) = tempfile.mkstemp()
        os.close(environment_fd)

        env = {
            'CONFIG_REPO': 'file://%s' % self.config_repo_path,
            'CONFIG_PATH': 'config_path',
            'CONFIG_REVISION': 'test-branch-config',
            'CODE_REPO': 'file://%s' % self.code_repo_path,
            'CODE_REVISION': 'test-branch-code'
        }
        environment_file = open(path, 'w')
        json.dump(env, environment_file, indent=4, sort_keys=True)
        return path
