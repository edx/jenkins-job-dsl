#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/warehouse-transforms
# To install right version of dbt
pip install -r requirements.txt


cd $WORKSPACE/warehouse-transforms/projects/$DBT_PROJECT

dbt clean --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt deps --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

if [ "$SKIP_SEED" != 'true' ]
then
  dbt seed --full-refresh --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
  if [ "$PUSH_ARTIFACTS_TO_SNOWFLAKE" = 'true' ]
  then
    # Errors from this operation are eaten as they are just telemetry data and not worth failing jobs over
    dbt run-operation upload_dbt_run_artifacts --args '{operation: seed}'  --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ || true
  fi
fi

# Source testing *before* model-building can be enabled/disabled with this envvar.
if [ "$TEST_SOURCES_FIRST" = 'true' ] && [ "$SKIP_TESTS" != 'true' ]
then
    # Run the source tests, sadly not just the ones upstream from this tag
    dbt test --models source:* --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
    if [ "$PUSH_ARTIFACTS_TO_SNOWFLAKE" = 'true' ]
    then
      # Errors from this operation are eaten as they are just telemetry data and not worth failing jobs over
      dbt run-operation upload_dbt_run_artifacts --args '{operation: source_test}'  --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ || true
    fi
fi

# Compile/build all models with this tag.
dbt run --models tag:$MODEL_TAG $exclude_param --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
if [ "$PUSH_ARTIFACTS_TO_SNOWFLAKE" = 'true' ]
then
  # Errors from this operation are eaten as they are just telemetry data and not worth failing jobs over
  dbt run-operation upload_dbt_run_artifacts --args '{operation: run}'  --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ || true
fi

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
    dbt test --models tag:$MODEL_TAG $exclude_param --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
    if [ "$PUSH_ARTIFACTS_TO_SNOWFLAKE" = 'true' ]
    then
      # Errors from this operation are eaten as they are just telemetry data and not worth failing jobs over
      dbt run-operation upload_dbt_run_artifacts --args '{operation: test}'  --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ || true
    fi
fi
