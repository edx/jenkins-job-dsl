#!/bin/bash
set -exuo pipefail

export PATH="/edx/bin:$PATH"

cd $WORKSPACE/configuration
pip install -r requirements.txt
. util/jenkins/assume-role.sh

assume-role ${ROLE_ARN}

cd $WORKSPACE/status.edx.org
pip install -r requirements.txt

fab publish