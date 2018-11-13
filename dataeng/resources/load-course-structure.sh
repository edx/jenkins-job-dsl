#!/usr/bin/env bash

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
 LoadCourseBlockRecordToVertica --local-scheduler \
 --date $(date +%Y-%m-%d -d "$TO_DATE") \
 --schema $SCHEMA \
 --credentials $CREDENTIALS
