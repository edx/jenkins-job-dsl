"""
Invoke this script to kick off a new ios build. The user will provide
any further necessary information at the command line.
"""

import logging
import sys

from .make_build import make_build
from .utils import is_affirmative

logger = logging.getLogger(__name__)  # pylint: disable=invalid-name


def make_ios_build():
    """
    Kicks off a new ios app build, asking the user a few
    additional questions
    """

    options = {"CODE_REPO": "git@github.com:edx/edx-app-ios.git"}

    # force testflight builds to only be from prod
    use_test_flight = raw_input("Send to Testflight [y/n]: ")
    if is_affirmative(use_test_flight):
        options["CONFIG_REPO"] = "https://github.com/edx/edx-mobile-config"
        options["CONFIG_PATH"] = "prod"
        options["DISTRIBUTION"] = "release"
        logger.info("Using 'edx-mobile-config/prod' for config")
    else:
        options["DISTRIBUTION"] = "enterprise"

    make_build(
        "git@github.com:edx/edx-app-build-ios.git",
        options
    )

if __name__ == "__main__":
    logging.basicConfig(
        format='%(asctime)s [%(levelname)s] %(message)s',
        stream=sys.stdout,
        level=logging.INFO
    )
    make_ios_build()
