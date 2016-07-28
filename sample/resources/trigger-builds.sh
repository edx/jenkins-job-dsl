#!/bin/bash

# unofficial bash strict mode
set -euo pipefail

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

# pipe the diff of the commit range into the parsefiles.py script, which resolves to a list of files affected by the change
# transform the output into a comma-separated list prefixed by the name of the folder in which the jobs should be created 
# "applications-autobuilds" and redirect into temp_props file
cd configuration
echo "TO_BUILD=$(git diff --name-only ${GIT_PREVIOUS_COMMIT}...${GIT_COMMIT} | sort | python util/parsefiles.py | sed -e 's/ /\,applications-autobuilds\//g' -e 's/^/applications-autobuilds\//')" > ../temp_props
