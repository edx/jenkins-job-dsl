#!/usr/bin/env bash

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
 ImportMysqlDatabaseToSnowflakeSchemaTask --local-scheduler \
 --date $(date +%Y-%m-%d -d "$RUN_DATE") \
 --credentials $SNOWFLAKE_CREDENTIALS \
 --warehouse $WAREHOUSE \
 --role $ROLE \
 --sf-database $SNOWFLAKE_DATABASE \
 --schema $SCHEMA \
 --scratch-schema $SCRATCH_SCHEMA \
 --run-id $BUILD_ID \
 --db-credentials $DB_CREDENTIALS \
 --database $DATABASE \
 --overwrite \
 ${EXCLUDE_FIELD} \
 ${EXCLUDE}
