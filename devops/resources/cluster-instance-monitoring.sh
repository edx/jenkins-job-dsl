#!/bin/bash
set -euox pipefail

cd $WORKSPACE/configuration
pip install -r requirements.txt
pip install -r util/cluster_monitoring_requirements.txt

env
. util/jenkins/assume-role.sh
assume-role ${ROLE_ARN}

CLUSTER_FILE=$"${WORKSPACE}/configuration-internal/tools-edx-jenkins/cluster-monitoring-triples.yml"

python util/cluster_instance_monitoring.py --file ${CLUSTER_FILE}