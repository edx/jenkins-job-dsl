"""
Utilities for the mobile app script unit tests
"""

import git
import os
import tempfile


def make_test_repo():
    """
    Creates a test repository on disk with an initial commit.
    The caller is expected to clean this up when it is done with it.

    Returns:
        A path to the new repo
    """
    repo_path = tempfile.mkdtemp(prefix="test-repo")
    repo = git.Repo.init(repo_path)
    dummy_file_path = os.path.join(repo_path, "README")

    # Add a stub file. Git works funny with empty repos
    open(dummy_file_path, 'wb').close()
    repo.index.add([dummy_file_path])
    repo.index.commit("initial commit")

    return repo_path
