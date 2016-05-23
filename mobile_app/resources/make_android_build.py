"""
Invoke this script to kick off a new android build. The user will provide
any further necessary information at the command line.
"""

import logging
import sys

from .make_build import make_build

logger = logging.getLogger(__name__)  # pylint: disable=invalid-name


def make_android_build():
    """
    Kicks off a new android app build, asking the user a few
    additional questions
    """
    make_build(
        "git@github.com:edx/edx-app-build-android.git",
        {"CODE_REPO": "git@github.com:edx/edx-app-android.git"}
    )

if __name__ == "__main__":
    logging.basicConfig(
        format='%(asctime)s [%(levelname)s] %(message)s',
        stream=sys.stdout,
        level=logging.INFO
    )
    make_android_build()
