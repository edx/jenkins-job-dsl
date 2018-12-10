#!/usr/bin/env bash

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
 LoadAllVideoToVertica --local-scheduler \
 --date $(date +%Y-%m-%d -d "$TO_DATE") \
 --schema $SCHEMA \
 --credentials $CREDENTIALS \
 --marker-schema $MARKER_SCHEMA \
 --overwrite
