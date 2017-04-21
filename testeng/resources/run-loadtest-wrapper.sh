#!/usr/bin/env bash

JOB_OVERRIDES_FILE="${WORKSPACE}/job_param_overrides.yml"

# Construct the list of overrides files which the loadtest entry point expects.
OVERRIDES_FILES="$SECRET_SETTINGS_FILE"
if [ -e $JOB_OVERRIDES_FILE ]; then
    # The user uploaded an overrides file, so we append it to the list:
    OVERRIDES_FILES="${OVERRIDES_FILES} ${JOB_OVERRIDES_FILE}"
fi
export OVERRIDES_FILES

# Invoke loadtest entry point from the correct directory.
cd edx-load-tests && util/run-loadtest.sh
