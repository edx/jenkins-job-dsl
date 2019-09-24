#!/usr/bin/env bash

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
 LoadPayPalCaseReportToVertica --local-scheduler \
 --run-date $(date +%Y-%m-%d -d "$RUN_DATE") \
 --schema ${SCHEMA} \
 $EXTRA_ARGS

