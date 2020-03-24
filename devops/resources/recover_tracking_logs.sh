#!/bin/bash
set -e

export AWS_DEFAULT_REGION=us-east-1
CLUSTER=edxapp

cd $WORKSPACE/configuration
pip install -r requirements.txt
. util/jenkins/assume-role.sh

# DA means days ago
DA0="$(date -I)"
DA1=$(date -I -d "${DA0} - 1 day")
DA2=$(date -I -d "${DA1} - 1 day")
DA3=$(date -I -d "${DA2} - 1 day")
DA4=$(date -I -d "${DA3} - 1 day")
DA5=$(date -I -d "${DA4} - 1 day")
DA6=$(date -I -d "${DA5} - 1 day")
DA7=$(date -I -d "${DA6} - 1 day")

assume-role ${ROLE_ARN}

cd $WORKSPACE/private-configuration

echo "Getting all snapshots for ${ENV_NAME} edxapp"
SNAPSHOTS=$(aws ec2 describe-snapshots --filters Name=tag-key,Values="cluster" Name=tag-value,Values="edxapp" Name=tag-key,Values="environment" Name=tag-value,Values="${ENV_NAME}" --query 'Snapshots[*].{A:SnapshotId,B:StartTime,C:Tags[?Key==`instance-id`].Value,D:Tags[?Key==`hostname`].Value}' --output text | tr '\n' '\t' | sed 's/snap-/\nsnap-/g' | sed 's/[CD]\t//g' | sed 's/\t$//' | tr '\t' ',' | grep -E ",($DA0|$DA1|$DA2|$DA3|$DA4|$DA5|$DA6|$DA7)")

for SNAPSHOT in ${SNAPSHOTS}; do
    # Assume role every time so 1 hour token doesn't expire if the job runs for a long time
    unassume-role
    assume-role ${ROLE_ARN}

    SNAP_ID=$(echo "${SNAPSHOT}" | cut -d , -f 1)
    DATE=$(echo "${SNAPSHOT}" | cut -d , -f 2)
    INSTANCE_ID=$(echo "${SNAPSHOT}" | cut -d , -f 3)
    IP=$(echo "${SNAPSHOT}" | cut -d , -f 4 | sed 's/ip-//' | sed 's/-/./g')

    if echo "${IP}" | cut -d . -f 3 | grep -q '^16$'; then
        SG="${SG_STUDIO}"
    elif echo "${IP}" | cut -d . -f 3 | grep -q '^7[0-9]$'; then
        SG="${SG_WORKERS}"
    elif echo "${IP}" | cut -d . -f 3 | grep -q '^1[0-9]$'; then
        SG="${SG_FRONTEND}"
    else
        echo "Can't determine SG from IP:${IP}"
        exit 1
    fi

    echo "Checking Snapshot-ID:${SNAP_ID} Instance:${INSTANCE_ID} IP:${IP} Date:${DATE}"
    # Redirect to /dev/null because grep -q causes aws cli to complain about a broken pipe when grep stops early
    if aws s3 ls s3://${BUCKET}/logs/tracking/${SG}/${INSTANCE_ID}-${IP}/tracking.log | grep ' 0 tracking.log' > /dev/null; then
        echo "Already Synced Snapshot-ID:${SNAP_ID} Instance:${INSTANCE_ID} IP:${IP} Date:${DATE}"
    else
        echo "Not Synced Snapshot-ID:${SNAP_ID} Instance:${INSTANCE_ID} IP:${IP} Date:${DATE}"
        echo "Recovering tracking logs from Snapshot ID:${SNAP_ID} Instance:${INSTANCE_ID} IP:${IP} From:${DATE}"
        ansible-playbook -u ubuntu -vvv sync_tracking_logs.yml -e "{\"snapshots\": [{\"id\": \"${SNAP_ID}\", \"s3_path\": \"s3://edx-prod-edx/logs/tracking/${SG}/${INSTANCE_ID}-${IP}/\"}]}" -e "security_group_ids=${SG_ID}" -e "subnet_id=${SUBNET_ID}" -e "iam_profile=${IAM_PROFILE}" -clocal
    fi
    echo
done
