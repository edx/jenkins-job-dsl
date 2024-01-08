#!/bin/bash
#we don't use u in the pipefail because Jenkins doesn't set variables and we want to check for that
set -exo pipefail

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

HOME=/edx/var/jenkins

env
set -x


cd $WORKSPACE/monitoring-scripts
pip install -r requirements/base.txt
cd saml_ssl_expiration_check

set +x

export SSL=$($SAML_SECRET | sed 's/\\"/"/g' | jq -r ".$SECRET_KEY")

python saml-ssl-expiration-check.py -d $DAYS -v SSL -e $REGION
