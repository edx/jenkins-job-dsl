#!/usr/bin/env bash
env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
 LoadGoogleAnalyticsPermissionsWorkflow --local-scheduler \
 --date $(date +%Y-%m-%d -d "$TO_DATE") \
 --schema $SCHEMA \
 --ga-credentials $GA_CREDENTIALS \
 --overwrite \
 $EXTRA_ARGS

