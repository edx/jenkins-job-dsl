#!/usr/bin/env bash

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
 LoadWarehouseSnowflakeTask --local-scheduler \
 --date $(date +%Y-%m-%d -d "$TO_DATE") \
 --warehouse $WAREHOUSE \
 --role $ROLE \
 --database $DATABASE \
 --schema $SCHEMA \
 --credentials $CREDENTIALS \
 --overwrite
