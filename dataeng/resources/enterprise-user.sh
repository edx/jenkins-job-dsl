#!/usr/bin/env bash

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
  ImportEnterpriseUsersIntoMysql --local-scheduler \
  --date $(date +%Y-%m-%d -d "$REPORT_DATE") \
  --overwrite-mysql \
  --overwrite-hive \
  $EXTRA_ARGS