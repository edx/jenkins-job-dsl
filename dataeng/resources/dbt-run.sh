#!/usr/bin/env bash
set -ex

# Creating python 3.8 virtual environment to run dbt warehouse-transform job
PYTHON38_VENV="py38_venv"
virtualenv --python=python3.8 --clear "${PYTHON38_VENV}"
source "${PYTHON38_VENV}/bin/activate"

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


DBT_PROFILE_ARGS="--profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET"

if ! [ -z "$DBT_MODEL_INCLUDE" ]
then
    DBT_MODEL_INCLUDE="--select $DBT_MODEL_INCLUDE"
else
    DBT_MODEL_INCLUDE=""
fi

if ! [ -z "$DBT_MODEL_EXCLUDE" ]
then
    DBT_MODEL_EXCLUDE="--exclude $DBT_MODEL_EXCLUDE"
else
    DBT_MODEL_EXCLUDE=""
fi

if [ "$FULL_REFRESH" = "true" ]
then
    FULL_REFRESH="--full-refresh"
else
    FULL_REFRESH=""
fi


if [ "$IS_SCHEMA_BUILDER_PR" == "true" ] || [ "$JOB_TYPE" == "manual" ]
then

    # Installing dbt requirements
    pip install -r requirements.txt

    cd $WORKSPACE/warehouse-transforms/projects/$DBT_PROJECT_PATH

    dbt clean $DBT_PROFILE_ARGS
    dbt deps $DBT_PROFILE_ARGS
    # ALWAYS pass --full-refreh for seeds, since there's no reason not to.  Not passing --full-refreh will more often
    # lead to job failures due to seed column changes.
    dbt seed --full-refresh $DBT_PROFILE_ARGS
    dbt run  $FULL_REFRESH $DBT_MODEL_INCLUDE $DBT_MODEL_EXCLUDE $DBT_RUN_ARGS $DBT_PROFILE_ARGS
    if [ "$SKIP_TESTS" = "true" ]
    then
        echo "Skipping dbt tests"
    else
        dbt test $FULL_REFRESH $DBT_MODEL_INCLUDE $DBT_MODEL_EXCLUDE $DBT_TEST_ARGS $DBT_PROFILE_ARGS
    fi

else
    echo "It is an automated run of job but not a Schema Builder commit. Skipping to run dbt"

fi
