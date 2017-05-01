#!/usr/bin/env bash

LOADTEST_OVERRIDES_PATH="${WORKSPACE}/job_param_overrides.yml"

# Create a file containing loadtest overrides.  The user may not have specified
# any overrides, in which case this line will create an empty file.
echo "$LOADTEST_OVERRIDES" > "$LOADTEST_OVERRIDES_PATH"

# Construct the list of overrides files which the loadtest entry point expects.
OVERRIDES_FILES="$SECRET_SETTINGS_FILE"
if [ -n "$LOADTEST_OVERRIDES" ]; then
    # The user specified loadtest overrides, so we append it to the list:
    OVERRIDES_FILES="${OVERRIDES_FILES} ${LOADTEST_OVERRIDES_PATH}"
fi
export OVERRIDES_FILES

# Invoke loadtest entry point from the correct directory.
cd edx-load-tests && util/run-loadtest.sh
