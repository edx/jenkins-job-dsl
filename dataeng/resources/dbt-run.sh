#!/usr/bin/env bash
set -ex

# Setup to run dbt commands
cd $WORKSPACE/warehouse-transforms

# Using --first-parent flag helps to avoids any intermediate commits that developers sometimes leave and merge
# without squashing. It helps us ignoring the noise in commit history.
HEAD_COMMIT=$(git log --first-parent origin/master --format='%H' --max-count=1)

if git log --format=%B -n 1 $HEAD_COMMIT | grep 'Schema Builder automated dbt update at' -q;
then
    IS_SCHEMA_BUILDER_PR="true"
else
    IS_SCHEMA_BUILDER_PR="false"
fi


if [ "$IS_SCHEMA_BUILDER_PR" == "true" ] || [ "$JOB_TYPE" == "manual" ]
then

    # Installing dbt requirements
    pip install -r requirements.txt

    cd $WORKSPACE/warehouse-transforms/projects/$DBT_PROJECT_PATH

    dbt clean --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
    dbt deps --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
    dbt seed --full-refresh --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
    dbt run $DBT_RUN_OPTIONS $DBT_RUN_EXCLUDE --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
    dbt test $DBT_RUN_OPTIONS --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
else
    echo "It is an automated run of job but not a Schema Builder commit. Skipping to run dbt"

fi


