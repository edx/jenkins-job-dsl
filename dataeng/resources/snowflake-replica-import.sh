#!/usr/bin/env bash

set -ex

env

# First delete the downstream properties file so that we do not trigger
# downstream jobs if not necessary.
DOWNSTREAM_PROPERTIES_FILE="${WORKSPACE}/downstream.properties"
rm "${DOWNSTREAM_PROPERTIES_FILE}" || true

# Only invoke this script on Mondays, or when forced to run via the FORCE
# parameter, otherwise skip it.  Since this job is triggered by an upstream for
# which we can't change the cron schedule (due to vertica not yet being
# deprecated), we must skip it manually in this shell script.
DAY_OF_WEEK=$(date +%u)
MONDAY=1
if [[ ${DAY_OF_WEEK} -eq ${MONDAY} || ${FORCE} = "true" ]]; then

    # Actually run the Luigi tasks:
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

    # All went well (we know that because of set -e), so we should generate a
    # downstream properties file which signals this job to invoke the downstream
    # job.
    echo "APP_NAME=${APP_NAME}" >> "${DOWNSTREAM_PROPERTIES_FILE}"
    echo "SQOOP_START_TIME=${SQOOP_START_TIME}" >> "${DOWNSTREAM_PROPERTIES_FILE}"

else
    echo "SKIPPING build because today is not Monday."
fi
