#!/bin/bash

# unofficial bash strict mode
set -euo pipefail

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python${CI_PYTHON_VERSION} --clear
. "$venvpath/bin/activate"
set -u

function singleline ()
{
    COUNT=0;
    while read LINE; do
        if [[ "$COUNT" -gt "0" ]]; then
            echo -n "$1";
        fi;
        let COUNT=$COUNT+1;
        echo -n "$LINE";
    done
}

# install configuration requirements 
pip install -r configuration/requirements.txt

# set TRAVIS_BUILD_DIR variable to point to root of configuration repository; needed by parsefiles.py
export TRAVIS_BUILD_DIR=${WORKSPACE}/configuration

# find the intersection of the applications described in the job configuration file and the applications determined
# as necessary to be built via the parsefiles.py script and redirect it into temp_props file to be read in as an
# environment variable TO_BUILD
cd configuration
echo "TO_BUILD=$(comm -12 <(echo ${APPS} | tr " " "\n" | sort) <(git diff --name-only ${GIT_PREVIOUS_COMMIT}...${GIT_COMMIT} | python util/parsefiles.py | tr " " "\n" | sort) | while read play; do echo -n "image-builders/$play-image-builder, "; done)" > ../temp_props
