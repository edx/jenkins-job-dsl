#!/usr/bin/env bash
set -x

# Setup
cd $WORKSPACE/warehouse-transforms
# To install right version of dbt
pip install -r requirements.txt

cd $WORKSPACE/warehouse-transforms/projects/$DBT_PROJECT

# Fails the job if a dbt command fails and uploads the dbt artifacts to Snowflake if the job is configured for it
function postCommandChecks {
  echo operation $1 and result code $2;
  if [ "$1" = 'seed' || "$1" = 'source_test' || "$1" = 'run' || "$1" = 'test' ]
  then
    if [ "$PUSH_ARTIFACTS_TO_SNOWFLAKE" = 'true' ]
    then
      # Errors from this operation are eaten as they are just telemetry data and not worth failing jobs over
      dbt run-operation upload_dbt_run_artifacts --args '{operation: '$1'}'  --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ || true
    fi
  fi

  if [ 0 != $2 ];
  then
    echo "dbt command failed, exiting with " $2
    exit $2;
  fi
}

dbt clean --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET ; ret=$?;
postCommandChecks "clean" $ret ;

dbt deps --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET ; ret=$?;
postCommandChecks "deps" $ret ;

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
dbt run --models tag:$MODEL_TAG $exclude_param --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ ; ret=$?;
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
