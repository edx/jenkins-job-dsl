#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/warehouse-transforms
# To install right version of dbt
pip install -r requirements.txt

cd $WORKSPACE/warehouse-transforms/projects/$DBT_PROJECT

# Fails the job if a dbt command fails and uploads the dbt artifacts to Snowflake if the job is configured for it
# First argument is the dbt operation name, second is the result code from the dbt command
function postCommandChecks {
  echo operation $1 and result code $2;

  if [ "$PUSH_ARTIFACTS_TO_SNOWFLAKE" = 'true' ]
  then
    # Errors from this operation are eaten as they are just telemetry data and not worth failing jobs over
    dbt run-operation upload_dbt_run_artifacts --args '{operation: '$1'}'  --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ || true
  fi

  if [ 0 != $2 ];
  then
    echo "dbt command failed, exiting with " $2
    exit $2;
  fi
}

# If the FULL_REFRESH_INCREMENTALS checkbox was enabled, switch to a bigger
# warehouse and tell dbt to do a "full refresh".
FULL_REFRESH_ARG=""
if [ "$FULL_REFRESH_INCREMENTALS" = 'true' ]
then
    FULL_REFRESH_ARG="--full-refresh"
    # Only switch to a larger warehouse if the target is prod, since there's no
    # other "large warehouse" target for non-prod environments.
    if [ "$DBT_TARGET" = 'prod' ]
    then
        DBT_TARGET='prod_load_incremental'
    fi
fi

# These commands don't have artifacts to be uploaded so if they fail the job can just fail
dbt clean --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt deps --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

# Turn off automatic failure of this script if the command returns non-0 for the rest of these commands
set +e

if [ "$SKIP_SEED" != 'true' ]
then
  dbt seed --full-refresh --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ ; ret=$?;
  postCommandChecks "seed" $ret ;
fi

# Source testing *before* model-building can be enabled/disabled with this envvar.
if [ "$TEST_SOURCES_FIRST" = 'true' ] && [ "$SKIP_TESTS" != 'true' ]
then
    # Run the source tests, sadly not just the ones upstream from this tag
    dbt test --models source:* --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ ; ret=$?;
    postCommandChecks "source_test" $ret ;
fi

# Compile/build all models with this tag.
dbt run $FULL_REFRESH_ARG --models tag:$MODEL_TAG $exclude_param --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ ; ret=$?;
postCommandChecks "run" $ret ;


if [ "$SKIP_TESTS" != 'true' ]
then
    # By default, exclude no models from testing.
    exclude_param=""

    if [ "$TEST_SOURCES_FIRST" = 'true' ]
    then
        # Exclude the sources from testing, since they were already tested pre-'dbt run'.
        exclude_param="--exclude source:*"
    fi

    # Run all tests which haven't been excluded.
    dbt test --models tag:$MODEL_TAG $exclude_param --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ ; ret=$?;
    postCommandChecks "test" $ret ;
fi
