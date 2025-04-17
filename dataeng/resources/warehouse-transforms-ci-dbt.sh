#!/usr/bin/env bash
set -ex

# This script runs the dbt deps/seed/run/test commands and serve warehouse-transforms-ci.sh script.
# DBT_RDBT_RUN_OPTIONS, DBT_TEST_OPTIONS etc comes from warehouse-transforms-ci.sh

cd $WORKSPACE/warehouse-transforms/projects/$DBT_PROJECT_PATH

source $WORKSPACE/secrets-manager.sh
# Fetch the secrets from AWS
set +x
get_secret_value analytics-secure/warehouse-transforms/profiles DBT_TRANSFORMER_CI_PRIVATE_KEY
set -x
export PRIVATE_KEY=$DBT_TRANSFORMER_CI_PRIVATE_KEY

dbt clean --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt deps --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt seed --full-refresh --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ --profile $DBT_PROFILE --target $DBT_TARGET

if [ "$WITH_SNAPSHOT" == "true" ]
then
    dbt snapshot --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ --profile $DBT_PROFILE --target $DBT_TARGET
fi

dbt run $DBT_RUN_OPTIONS $DBT_RUN_EXCLUDE --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ --profile $DBT_PROFILE --target $DBT_TARGET
# Jenkins jobs are marked as failed when any of command fails so writing the following test command with && true so it will give a chance to
# evaluate its success or failure base on success or failure we can do further re-tries on failed tests
dbt test $DBT_TEST_OPTIONS $DBT_TEST_EXCLUDE --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ --profile $DBT_PROFILE --target $DBT_TARGET && true
if [ $? -eq 1 ]
then
    if [ "$WITH_RETRY" == "true" ]
    then
        pip install -r ../../tools/ci_scripts/requirements.txt
        if [ "$DBT_TEST_EXCLUDE" == "" ]
        then
            python ../../tools/ci_scripts/rerun_flaky_tests.py --project-path . --profiles-dir $WORKSPACE/warehouse-transforms/profiles/  --profile $DBT_PROFILE  \
            --target $DBT_TARGET --count $NO_OF_TRIES
        else
            PREFIX="--exclude "
            TEST_EXCLUSIONS=$(echo "$DBT_TEST_EXCLUDE" | sed -e "s/^$PREFIX//")
            python ../../tools/ci_scripts/rerun_flaky_tests.py --project-path . --profiles-dir $WORKSPACE/warehouse-transforms/profiles/  --profile $DBT_PROFILE  \
            --target $DBT_TARGET --exclusions $TEST_EXCLUSIONS --count $NO_OF_TRIES
        fi
    else
        echo "Tests failed but retry is not enabled"
        exit 1
    fi
fi
