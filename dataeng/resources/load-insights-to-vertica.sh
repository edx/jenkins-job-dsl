#!/usr/bin/env bash

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
 LoadInsightsTableToVertica --local-scheduler \
 --schema $SCHEMA \
 --credentials $CREDENTIALS \
 --marker-schema $MARKER_SCHEMA \
 --overwrite \
 $EXTRA_ARGS
