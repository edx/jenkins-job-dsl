"""
Test cases for make build script
"""
from collections import namedtuple
import git
from unittest import TestCase

from mock import patch
from sys import version_info
if version_info.major == 2:
    import __builtin__ as builtins  # pylint: disable=import-error
else:
    import builtins  # pylint: disable=import-error

from . import utils
from .. import make_build
from .. import trigger_build

Input = namedtuple("Input", ["key", "value"])  # pylint: disable=invalid-name
INPUTS = [
    Input(key="CODE_REPO", value="git://code-repo.git"),
    Input(key="CODE_REVISION", value="code-branch"),
    Input(key="CONFIG_REPO", value="git://config-repo.git"),
    Input(key="CONFIG_REVISION", value="config-branch"),
    Input(key="CONFIG_PATH", value="subpath"),
    Input(key=None, value="../build-repo")
]
VALUES = [entry.value for entry in INPUTS]


class MakeBuildTestCase(TestCase):
    """
    Tests for script that asks user for environment variables
    """

    def test_envs_extracted(self):
        """
        Tests that all the arguments we pass in end up in the environment for
        building. Or are passed as part of the argument list
        """

        with patch.object(builtins, 'raw_input', side_effect=VALUES):
            (args, env) = make_build.collect_params()
        for item in INPUTS:
            if item.key:
                self.assertEqual(env[item.key], item.value)
        self.assertTrue("../build-repo" in args)

    def test_passed_params(self):
        """
        Test that when we pass in explicit parameters to make_build
        they properly end up in the params list
        """

        path = utils.make_test_repo()

        def verify_params(args, environ):
            # pylint: disable=missing-docstring
            self.assertEqual(environ["CODE_REPO"], "fake-code-repo.git")
            index = args.index("--trigger-repo-path")

            repo = git.Repo(args[index + 1])
            self.assertEqual(repo.remotes.origin.url, path)

        entries = [
            item.value for item in INPUTS if
            item.key != 'CODE_REPO' and
            item.key != '--trigger-repo-path'
        ]

        with patch.object(builtins, 'raw_input', side_effect=entries):
            with patch.object(
                trigger_build,
                'run_trigger_build',
                side_effect=verify_params
            ):
                make_build.make_build(path, {
                    "CODE_REPO": "fake-code-repo.git"
                })
