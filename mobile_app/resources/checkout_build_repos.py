"""
Checks out the various repositories needed to do a build
based on a passed in configuration, switches them to the
specified branch, and points the code at the config by writing
an "edx.properties" file to the code repo.
"""

import git
import json
import logging
import os
import sys

from .path_constants import (
    CODE_CHECKOUT_DIRECTORY,
    CONFIG_CHECKOUT_DIRECTORY,
    CONFIG_FILE,
    PROPERTIES_FILE
)

logger = logging.getLogger(__name__)  # pylint: disable=invalid-name


def checkout_repos(build_environment_path, environ, base_path):
    """
    Checks out the set of repos needed to do a build

    Arguments:
        build_environment_path (string): A path to a
            json file containing the build environment.

        environ (dict): A dictionary to pass additional
            parameters. Typically the os environment.
            Used for the EDX_PROPERTIES_PATH key.

        base_path (string): Path to create files at
    """

    # extract environment
    config = json.load(file(build_environment_path))

    config_repo_url = config["CONFIG_REPO"]
    relative_config_path = config["CONFIG_PATH"]
    config_revision = config["CONFIG_REVISION"]

    code_repo_url = config["CODE_REPO"]
    code_revision = config["CODE_REVISION"]

    edx_properties_path = environ["EDX_PROPERTIES_PATH"]

    code_repo_path = os.path.join(base_path, CODE_CHECKOUT_DIRECTORY)
    config_repo_path = os.path.join(base_path, CONFIG_CHECKOUT_DIRECTORY)

    # checkout repos
    repos = [
        (code_repo_path, code_repo_url, code_revision),
        (config_repo_path, config_repo_url, config_revision)
    ]
    for (repo_path, url, revision) in repos:

        logger.info("cloning repo %s to %s", url, repo_path)
        repo = git.Repo.clone_from(url, to_path=repo_path)
        origin_branch = repo.remotes.origin.refs[revision]
        # need to create a local tracking branch of the remote
        # so we can check it out properly
        repo.create_head(revision, origin_branch)
        repo.refs[revision].checkout()
        logger.info("switching to branch %s", revision)

    _write_properties(
        edx_properties_path,
        relative_config_path,
        code_repo_path,
        config_repo_path
    )


def _write_properties(
        code_repo_properties_path,
        relative_config_path,
        code_repo_path,
        config_repo_path
        ):
    """
    Writes an edx.properties file to the code repo that points at the proper
    location in the config repo.

    Arguments:
        code_repo_properties_path (string): path relative to the root of
            the code root for writing an "edx.properties" file pointing to the
            config.

        relative_config_path (string): path relative to the root of the config
            repo for writing into the "edx.properties" file

        code_repo_path (string): path to the code repo root

        config_repo_path (string): path to the config repo root

    """

    properties_dir_path = os.path.join(
        code_repo_path,
        code_repo_properties_path
    )
    properties_file_path = os.path.join(
        properties_dir_path,
        PROPERTIES_FILE
    )
    absolute_config_path = os.path.join(
        config_repo_path,
        relative_config_path
    )

    # make the parent chain if necessary
    parent_dir = os.path.split(properties_file_path)[0]
    if not os.path.exists(parent_dir):
        os.mkdir(parent_dir)

    logger.info("Writing edx.properties to %s", absolute_config_path)

    with file(properties_file_path, "w") as properties_file:
        properties_file.write(
            "edx.dir = \"{config_path}\"".format(
                config_path=absolute_config_path
            )
        )

if __name__ == "__main__":
    logging.basicConfig(
        format='%(asctime)s [%(levelname)s] %(message)s',
        stream=sys.stdout
    )
    logger.setLevel(logging.INFO)
    checkout_repos(CONFIG_FILE, os.environ, os.path.abspath("."))
