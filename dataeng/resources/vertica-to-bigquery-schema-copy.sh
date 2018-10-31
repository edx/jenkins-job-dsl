#!/usr/bin/env bash

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
 VerticaSchemaToBigQueryTask --local-scheduler \
 --vertica-schema-name $SCHEMA \
 --vertica-credentials $VERTICA_CREDENTIALS \
 --gcp-credentials $GCP_CREDENTIALS \
 ${RUN_DATE} \
 ${OVERWRITE} \
 ${EXCLUDE}
