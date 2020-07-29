#!/usr/bin/env bash

env

# Only invoke this script on Mondays, or when forced to run via the FORCE
# parameter, otherwise skip it.  Since this job is triggered by an upstream for
# which we can't change the cron schedule (due to vertica not yet being
# deprecated), we must skip it manually in this shell script.

DAY_OF_WEEK=$(date +%u)
MONDAY=1
if [[ ${DAY_OF_WEEK} -eq ${MONDAY} -o ${FORCE} = "true" ]]; then
    # Prepare the downstream properties file:
    printf '' > "${WORKSPACE}/downstream.properties"  # Truncate the file first.
    echo "APP_NAME=${APP_NAME}" >> "${WORKSPACE}/downstream.properties"
    echo "SQOOP_START_TIME=${SQOOP_START_TIME}" >> "${WORKSPACE}/downstream.properties"

    ${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
     ImportMysqlDatabaseFromS3ToSnowflakeSchemaTask --local-scheduler \
     --date $(date +%Y-%m-%d -d "$RUN_DATE") \
     --credentials $SNOWFLAKE_CREDENTIALS \
     --warehouse $WAREHOUSE \
     --role $ROLE \
     --sf-database $SNOWFLAKE_DATABASE \
     --schema $SCHEMA \
     --scratch-schema $SCRATCH_SCHEMA \
     --run-id $BUILD_ID \
     --database $DATABASE \
     --overwrite \
     ${INCLUDE} \
     ${EXCLUDE} \
     ${EXTRA_ARGS}
else
    touch "${WORKSPACE}/build_skipped"
fi
