#!/usr/bin/env bash

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
 LoadVerticaSchemaFromS3ToSnowflakeTask --local-scheduler \
 --date $(date +%Y-%m-%d -d "$RUN_DATE") \
 ${OVERWRITE} \
 --credentials $SNOWFLAKE_CREDENTIALS \
 --warehouse $WAREHOUSE \
 --role $ROLE \
 --sf-database $DATABASE \
 --schema $SCHEMA \
 --vertica-schema-name $VERTICA_SCHEMA_NAME \
 --vertica-credentials $VERTICA_CREDENTIALS \
 --vertica-warehouse-name $VERTICA_WAREHOUSE_NAME \
 ${EXCLUDE}
