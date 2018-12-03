#!/usr/bin/env bash

DIR_NAME=sql-scripts

# Note that the scriptname has "single_" prepended to it.   That way it will not collide with the names used for marking the tables in the "regular" run.
${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
  --extra-repo "$SCRIPTS_REPO?dir_name=$DIR_NAME&branch=$SCRIPTS_BRANCH" \
  RunVerticaSqlScriptTask --local-scheduler \
    --source-script ../$DIR_NAME/$SOURCE_SCRIPT \
    --script-name single_$SCRIPT_NAME \
    --schema $SCHEMA \
    --credentials $CREDENTIALS \
    --read-timeout 3600 \
    $EXTRA_ARGS
