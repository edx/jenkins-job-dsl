#!/bin/bash
set -euox pipefail

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

cd $WORKSPACE/configuration
pip install -r requirements.txt

env
. util/jenkins/assume-role.sh
assume-role ${ROLE_ARN}

CLUSTER_FILE=$"${WORKSPACE}/configuration-internal/tools-edx-jenkins/cluster-monitoring-triples.yml"

python util/cluster_instance_monitoring.py --file ${CLUSTER_FILE} --region ${REGION}
