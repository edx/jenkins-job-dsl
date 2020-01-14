#!/usr/bin/env bash
env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
 LoadGoogleSpreadsheetsToVerticaWorkflow --local-scheduler \
 --date $(date +%Y-%m-%d -d "$TO_DATE") \
 --google-credentials $GOOGLE_CREDENTIALS \
 --overwrite
