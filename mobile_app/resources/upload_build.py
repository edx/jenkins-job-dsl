"""
Uploads an app build. Presently it only uploads to hockey app"
"""

import logging
import json
import os
import requests
import sys

from .path_constants import CONFIG_FILE

from .exceptions import (
    MissingEnvironmentVariable,
    UploadFailure
)

logger = logging.getLogger(__name__)  # pylint: disable=invalid-name

UPLOAD_URL = "https://rink.hockeyapp.net/api/2/apps/upload"


class HockeyTokenAuth(object):  # pylint: disable=too-few-public-methods
    """
    Authorization method for requests library supporting HockeyApp tokens
    """
    def __init__(self, token):
        self.token = token

    def __call__(self, request):
        request.headers["X-HockeyAppToken"] = self.token
        return request


def run_upload_build(config, environ):
    """
    Uploads a build to Hockey App

    Arguments
        config (dict): Settings from the build's CONFIGURATION file
        environ (dict): Settings from the surrounding OS environment
    """

    try:
        token = environ["HOCKEY_APP_TOKEN"]
        commit_sha = environ["CODE_SHA"]
        binary_path = environ["BINARY_PATH"]
    except KeyError as error:
        raise MissingEnvironmentVariable(error.args[0])

    with open(binary_path, "rb") as binary_file:
        binary_data = binary_file.read()
    auth = HockeyTokenAuth(token)
    params = {
        "commit_sha": commit_sha,
        "status": 2,  # publish build
    }

    notes = config.get("BUILD_NOTES", None)
    if notes:
        params["notes"] = notes

    files = {
        "ipa": (os.path.split(binary_path)[1], binary_data)
    }

    response = requests.post(UPLOAD_URL, data=params, auth=auth, files=files)
    if response.status_code != 201:
        raise UploadFailure(response)
    logger.info("Uploaded build")

if __name__ == "__main__":
    logging.basicConfig(
        format='%(asctime)s [%(levelname)s] %(message)s',
        stream=sys.stdout
    )
    logger.setLevel(logging.INFO)
    with open(CONFIG_FILE) as config_file:
        config = json.load(config_file)
    run_upload_build(config, os.environ)
