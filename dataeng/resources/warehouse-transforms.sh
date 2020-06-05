#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/warehouse-transforms
# To install right version of dbt
pip install -r tools/dbt_schema_builder/requirements.txt


cd $WORKSPACE/warehouse-transforms/projects/reporting

dbt clean --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt deps --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

if [ "$SKIP_SEED" != 'true' ]
then
  dbt seed --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
fi

# Source testing *before* model-building can be enabled/disabled with this envvar.
if [ "$TEST_SOURCES_FIRST" = 'true' ] && [ "$SKIP_TESTS" != 'true' ]
then
    # Run the source tests, sadly not just the ones upstream from this tag
    dbt test --models source:* --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
fi

# Compile/build all models with this tag.
dbt run --models tag:$MODEL_TAG --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/

if [ "$SKIP_TESTS" != 'true' ]
then
    # By default, exclude no models from testing.
    exclude_models=""
    exclude_param=""

    if [ "$TEST_SOURCES_FIRST" = 'true' ]
    then
        # Exclude the sources from testing, since they were already tested pre-'dbt run'.
        exclude_models="$exclude_models 'source:*'"
    fi

    if [ "$MODEL_TAG" = 'daily' ]
    then
        # For 'daily' models, exclude these two models from finrep that inexplicably are tested when testing those models.
        exclude_models="$exclude_models finrep_map_organization_course_courserun finrep_report_royalty_dimension"
    fi

    if [ "$exclude_models" != "" ]
    then
        # If models were excluded, add the leading exclude parameter.
        exclude_param="--exclude $exclude_models"
    fi

    # Run all tests which haven't been excluded.
    dbt test --models tag:$MODEL_TAG $exclude_param --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
fi
