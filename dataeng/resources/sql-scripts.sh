#!/usr/bin/env bash

DIR_NAME=sql-scripts

analytics-configuration/automation/run-automated-task.sh \
  --extra-repo "$SCRIPTS_REPO?dir_name=$DIR_NAME&branch=$SCRIPTS_BRANCH" \
  RunVerticaSqlScriptsTask --local-scheduler \
    --script-configuration=../$DIR_NAME/$SCRIPTS_CONFIG \
    --script-root=../$DIR_NAME \
    --schema $SCHEMA \
    --credentials $CREDENTIALS \
    --read-timeout 3600 \
    $EXTRA_ARGS
