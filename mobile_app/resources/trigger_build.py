"""
Triggers a new app build by making a branch for it.
"""

import argparse
import git
import logging
import json
import os
import sys

from .exceptions import (
    BranchAlreadyExists,
    MissingEnvironmentVariable
)

from .path_constants import (
    CONFIG_FILE
)

logger = logging.getLogger(__name__)  # pylint: disable=invalid-name

EXPECTED_ENVIRONMENT_VARIABLES = [
    "CONFIG_REPO",
    "CONFIG_PATH",
    "CONFIG_REVISION",
    "CODE_REPO",
    "CODE_REVISION",
]

OPTIONAL_ENVIRONMENT_VARIABLES = [
    "BUILD_NOTES",
    "DISTRIBUTION"
]


def _build_parser():
    """
    Constructs a command line option parser

    Returns:
        ArgumentParser: The option parser
    """
    parser = argparse.ArgumentParser(
        description="""
        Commits a list of environment variables to trigger a new build
        in that environment
        """
    )
    parser.add_argument(
        '--branch-name',
        nargs='?',
        required=True
    )
    parser.add_argument(
        '--trigger-repo-path',
        nargs='?',
        required=True
    )
    return parser


def run_trigger_build(raw_args, environ):
    """
    Triggers a build in the current environment by writing
    the environment to disk and committing it to a new branch.

    Arguments:
        raw_args (list): Command line arguments
    """
    parser = _build_parser()
    args = parser.parse_args(raw_args)
    repo_path = os.path.abspath(args.trigger_repo_path)
    config_path = os.path.join(repo_path, CONFIG_FILE)
    logger.info(
        'Triggering build on repo %s with branch %s',
        repo_path, args.branch_name
    )

    # generate config file
    items = {}
    for variable in EXPECTED_ENVIRONMENT_VARIABLES:
        try:
            items[variable] = environ[variable]
        except KeyError:
            raise MissingEnvironmentVariable(variable)

    for variable in OPTIONAL_ENVIRONMENT_VARIABLES:
        value = environ.get(variable, None)
        if value:
            items[variable] = value

    with file(config_path, "w") as config_file:
        json.dump(items, config_file, indent=4, sort_keys=True)

    # commit and push
    repo = git.Repo(repo_path)
    origin = repo.remote()

    # pull to ensure our branch is up to date
    try:
        origin.pull()
    except git.exc.GitCommandError:
        # If pull fails we're probably on a detached branch like jenkins gives
        # us. That means that pulling won't do anything anyway, so just ignore
        # it
        pass

    try:
        repo.heads[args.branch_name]
        raise BranchAlreadyExists(args.branch_name)
    except IndexError:
        pass

    branch = repo.create_head(args.branch_name)
    branch.checkout()
    repo.index.add([config_path])
    repo.index.commit("Automated commit")
    origin.push(args.branch_name)

if __name__ == "__main__":
    logging.basicConfig(
        format='%(asctime)s [%(levelname)s] %(message)s',
        stream=sys.stdout
    )
    logger.setLevel(logging.INFO)
    run_trigger_build(sys.argv[1:], os.environ)
