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

SAML_SSL_CERT_FILE=$WORKSPACE/configuration_secure/${SAML_CERT_FILE}

cd $WORKSPACE/sysadmin
pip install -r requirements.txt
cd jenkins

python saml-ssl-expiration-check.py --region $REGION -d $DAYS -i $SAML_SSL_CERT_FILE
