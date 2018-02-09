#!/bin/bash

# Temporary fix

# This script will ensure that pull requests that were open before 
# https://github.com/edx/edx-platform/pull/17252 was merged into the platform
# do not report false positives for quality. The thresholds.sh file was introduced
# in this pull request. If it is not present, fail fast.

# Once all prs have been rebased to contain this fix, remove this script

if [[ $TEST_SUITE == 'quality' ]] && [[ ! -e 'scripts/thresholds.sh' ]]; then
    echo 'The quality job has been refactored and requires a fix in the platform.'
    echo 'Please rebase your pr and rerun this test'
    exit 1
fi
