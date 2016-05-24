"""
Asks the user for the information needed to trigger a build and then triggers
that build. This is meant to stand in for jenkins/a task runner for
testing or when one is not available.
"""

from collections import namedtuple
import git
import logging
import os
import shutil
import sys
import tempfile
import uuid

from . import trigger_build

logger = logging.getLogger(__name__)  # pylint: disable=invalid-name

Question = namedtuple("Question", ["prompt", "key", "kind"])

QUESTIONS = [
    Question(
        prompt="Code repo URL",
        key="CODE_REPO",
        kind="environ"
    ),
    Question(
        prompt="Code revision",
        key="CODE_REVISION",
        kind="environ"
    ),
    Question(
        prompt="Config repo URL",
        key="CONFIG_REPO",
        kind="environ"
    ),
    Question(
        prompt="Config revision",
        key="CONFIG_REVISION",
        kind="environ"
    ),
    Question(
        prompt="Config subpath",
        key="CONFIG_PATH",
        kind="environ"
    ),
    Question(
        prompt="Build repo local path",
        key="--trigger-repo-path",
        kind="arg"
    ),
]


def fresh_branch_name():
    """
    Generates a unique name for this build
    """
    return "build-%s" % uuid.uuid4()


# pylint: disable=dangerous-default-value
def collect_params(questions=QUESTIONS):
    """
    Asks the user to provide values for a series of variables that can be used
    as input to the trigger_build script

    Returns
        (dict, dict) tuple of (environment variables, command line options)
        that can be sent to the trigger_build script
    """
    environ = {}
    args = []
    for question in questions:
        value = raw_input(question.prompt + ": ")
        if question.kind is "environ":
            environ[question.key] = value
        elif question.kind is "arg":
            args += [question.key, value]

    branch_name = fresh_branch_name()
    logger.info("Using branch: %s", branch_name)
    args += ["--branch-name", branch_name]
    return (args, environ)


def make_build(trigger_repo, overrides={}):
    """
    Checks out the trigger repo then makes a new
    build with remaining arguments supplied by the user

    Arguments:
        trigger_repo (string): A path to a git repo whose commits
            will trigger a new build
        overrides (dict): Seed values for the environment. The user
            will not be queried for keys already in this, allowing
            the caller to provide hard coded answers for certain cases
            such as specifying a particular source code repo
    """
    trigger_path_container = tempfile.mkdtemp()
    # Filter to the questions we're not prepopulating
    questions = [
        question for question in QUESTIONS if
        question.key not in overrides and
        question.key != "--trigger-repo-path"
    ]

    try:
        (args, environ) = collect_params(questions)
        for (key, value) in overrides.items():
            environ[key] = value

        trigger_path = os.path.join(trigger_path_container, "trigger.git")
        git.Repo.clone_from(trigger_repo, to_path=trigger_path)

        args.extend(["--trigger-repo-path", trigger_path])
        trigger_build.run_trigger_build(args, environ)
    finally:
        shutil.rmtree(trigger_path_container)

if __name__ == "__main__":
    logging.basicConfig(
        format='%(asctime)s [%(levelname)s] %(message)s',
        stream=sys.stdout,
        level=logging.INFO
    )
    trigger_build.run_trigger_build(*collect_params(QUESTIONS))
