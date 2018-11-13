#!/usr/bin/env bash

####################################################################
# Exporter configuration

ROOT=${WORKSPACE}/analytics-secure/analytics-exporter
SECURE_HASH=`GIT_DIR=./analytics-secure/.git git rev-parse HEAD`
EXPORTER_CONFIG_BUCKET=s3://edx-analytics-scratch/exporter/config/$SECURE_HASH
EXPORTER_CONFIG_PATH=${EXPORTER_CONFIG_BUCKET}/${EXPORTER_CONFIG}
GPG_KEYS_PATH=${EXPORTER_CONFIG_BUCKET}/gpg-keys

####################################################################
# Upload configuration files to s3

pip install awscli

echo ${WORKSPACE}/data-czar-keys/${EXPORTER_CONFIG}

aws s3 cp ${WORKSPACE}/data-czar-keys/${EXPORTER_CONFIG} ${EXPORTER_CONFIG_PATH}
aws s3 cp --recursive ${WORKSPACE}/data-czar-keys/ ${GPG_KEYS_PATH}

####################################################################
# Run exporter task

if [ -z "$NUM_REDUCE_TASKS" ]; then
    NUM_REDUCE_TASKS=$(( $NUM_TASK_CAPACITY + 1 ))
fi

env | sort

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
 EventExportTask --local-scheduler \
 --interval $(date +%Y-%m-%d -d "$FROM_DATE")-$(date +%Y-%m-%d -d "$TO_DATE") \
 --output-root ${OUTPUT_ROOT} \
 --config ${EXPORTER_CONFIG_PATH} \
 --gpg-key-dir ${GPG_KEYS_PATH} \
 --environment ${ENVIRONMENT} \
 ${ONLY_ORGS} \
 --n-reduce-tasks ${NUM_REDUCE_TASKS}
