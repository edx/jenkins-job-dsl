#!/usr/bin/env bash
env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
 LoadGoogleSpreadsheetsToSnowflakeWorkflow --local-scheduler \
 --date $(date +%Y-%m-%d -d "$TO_DATE") \
 --sf-credentials $SNOWFLAKE_CREDENTIALS \
 --sf-warehouse $SNOWFLAKE_WAREHOUSE \
 --sf-role $SNOWFLAKE_ROLE \
 --sf-database $SNOWFLAKE_DATABASE \
 --sf-run-id $BUILD_ID \
 --google-credentials $GOOGLE_CREDENTIALS \
 --overwrite
