#!/usr/bin/env bash

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
 VerticaSchemaToSnowflakeTask --local-scheduler \
 ${RUN_DATE} \
 ${OVERWRITE} \
 --credentials $SNOWFLAKE_CREDENTIALS \
 --warehouse $WAREHOUSE \
 --role $ROLE \
 --database $DATABASE \
 --schema $SCHEMA \
 --vertica-schema-name $VERTICA_SCHEMA_NAME \
 --vertica-credentials $VERTICA_CREDENTIALS \
 --vertica-warehouse-name $VERTICA_WAREHOUSE_NAME \
 ${EXCLUDE}
