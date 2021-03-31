#!/usr/bin/env bash
set -ex

# This script runs the dbt deps/seed/run/test commands and serve warehouse-transforms CI jobs.
# DBT_RDBT_RUN_OPTIONS, DBT_TEST_OPTIONS etc comes from warehouse-transforms-ci.sh
# Difference between this script and warehouse-transforms-c-dbt.sh is it offers Retries mechanism beforing
# marking a job as failed job

cd $WORKSPACE/warehouse-transforms/projects/$DBT_PROJECT_PATH

dbt clean --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt deps --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt seed --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

# NO_OF_TRIES is currently set to '2' which will execute dbt test '2' times and can be increase as per the need.
NO_OF_TRIES=3
i=0
failed=true
while [[ $i -lt $NO_OF_TRIES ]]
do
    dbt run $DBT_RUN_OPTIONS $DBT_RUN_EXCLUDE --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
    dbt test $DBT_TEST_OPTIONS $DBT_TEST_EXCLUDE --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET && true
    if [ $? -eq 0 ]
    then
        $failed=false
        break
    fi
    ((i=i+1))
done
if [ $failed ]
then
    echo "Failed even after $NO_OF_TRIES number of tries, Marking it as a failure"
    exit 1
fi
