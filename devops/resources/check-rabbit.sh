#!/bin/bash
set -exuo pipefail

cd $WORKSPACE/configuration
pip install -r requirements.txt
env
export EC2_CACHE_PATH="ec2-cache"
. util/jenkins/assume-role.sh

# Assume the role that will allow running ec2.py for getting a dynamic inventory
assume-role ${ROLE_ARN}


cd $WORKSPACE/configuration/playbooks

ansible tag_Name_${ENVIRONMENT}-${DEPLOYMENT}-${CLUSTER_NAME} -i ./ec2.py -u $SSH_USER \
  -m shell -a "sudo -u ${SUDO_USER} /usr/sbin/service rabbitmq-server status"

cluster_status="$(ansible tag_Name_${ENVIRONMENT}-${DEPLOYMENT}-${CLUSTER_NAME} -i ./ec2.py -u $SSH_USER \
  -m shell -a 'sudo -u root /usr/sbin/rabbitmqctl cluster_status')"
ret=$?
echo "$cluster_status"
[ $ret -gt 0 ] && exit $ret || grep partitions <<<"$cluster_status" | \
  while read item; do [ "$( echo "$item" | sed 's/\(]\|,\)$//')" = '{partitions,[]}' ] || exit 1; done
