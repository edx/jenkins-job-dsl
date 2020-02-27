#!/usr/bin/env bash

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
LoadVerticaSchemaFromS3WithMetadataToSnowflakeTask --local-scheduler \
 --date $(date +%Y-%m-%d -d "$RUN_DATE") \
 ${OVERWRITE} \
 --credentials $SNOWFLAKE_CREDENTIALS \
 --warehouse $WAREHOUSE \
 --role $ROLE \
 --sf-database $DATABASE \
 --schema $SCHEMA \
 --scratch-schema $SCRATCH_SCHEMA \
 --run-id $BUILD_ID \
 --vertica-schema-name $VERTICA_SCHEMA_NAME \
 --vertica-warehouse-name $VERTICA_WAREHOUSE_NAME
