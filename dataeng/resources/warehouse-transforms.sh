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
  dbt seed --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
fi

# Source testing *before* model-building can be enabled/disabled with this envvar.
if [ "$TEST_SOURCES_FIRST" = 'true' ] && [ "$SKIP_TESTS" != 'true' ]
then
    # Run the source tests, sadly not just the ones upstream from this tag
    dbt test --models source:* --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
fi

# By default, exclude no models from testing.
exclude_models=""
exclude_param=""

if [ "$MODEL_TAG" = 'daily' ]
then
    # For 'daily' models, exclude these models from being run or tested
    exclude_models="$exclude_models finrep_map_organization_course_courserun finrep_royalty_order_dimension tag:daily-exclude"
fi

if [ "$exclude_models" != "" ]
then
    # If models were excluded, add the leading exclude parameter.
    exclude_param="--exclude $exclude_models"
fi

# Compile/build all models with this tag.
dbt run --models tag:$MODEL_TAG $exclude_param --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/

if [ "$SKIP_TESTS" != 'true' ]
then
    if [ "$TEST_SOURCES_FIRST" = 'true' ]
    then
        # Exclude the sources from testing, since they were already tested pre-'dbt run'.
        exclude_models="$exclude_models source:*"
    fi

    # This duplicates the code above to make sure we get any changes to exclude_models for the source tests.
    if [ "$exclude_models" != "" ]
    then
        # If models were excluded, add the leading exclude parameter.
        exclude_param="--exclude $exclude_models"
    fi

    # Run all tests which haven't been excluded.
    dbt test --models tag:$MODEL_TAG $exclude_param --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
fi

# Each transfer is two elements in this array:
#     - the DBT macro name
#     - the macro arguments to use when calling the DBT macro
PROGRAMS_REPORTING_TRANSFERS=( \
    unload_program_reporting_learner_enrollments_report '{database:PROD,schema:PROGRAMS_REPORTING}' \
    unload_program_reporting_program_cohort_report '{database:PROD,schema:PROGRAMS_REPORTING}' \
)

if [ "$PROGRAMS_REPORTING_TRANSFER_TO_S3" = 'true' ]
then

    # Call each DBT macro to initiate a transfer.
    num_txfrs=${#PROGRAMS_REPORTING_TRANSFERS[@]}/2

    for (( i=0; i<${num_txfrs}; i++ ));
    do
        dbt run-operation ${PROGRAMS_REPORTING_TRANSFERS[$i*2]} --args \'${PROGRAMS_REPORTING_TRANSFERS[$i*2+1]}\' --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
    done

fi

ENTERPRISE_TRANSFERS=( \
    enterprise_copy_to_s3 '{prefix:ENTERPRISE,table:ENT_BASE_ENTERPRISE_USER}' \
    enterprise_copy_to_s3 '{prefix:ENTERPRISE,table:FACT_CUSTOMER_COURSE_DAILY_ROLLUP_ADMIN_DASH}' \
    enterprise_copy_to_s3 '{prefix:ENTERPRISE,table:FACT_ENROLLMENT_ADMIN_DASH}' \
    enterprise_copy_to_s3 '{prefix:ENTERPRISE,table:FACT_ENROLLMENT_ENGAGEMENT_DAY_ADMIN_DASH}' \
    enterprise_copy_to_s3 '{prefix:PEARSON,table:ENT_REPORT_PEARSON_COURSE_METRICS}' \
    enterprise_copy_to_s3 '{prefix:PEARSON,table:ENT_REPORT_PEARSON_BLOCK_COMPLETION}' \
    enterprise_copy_to_s3 '{prefix:PEARSON,table:ENT_REPORT_PEARSON_PERSISTENTSUBSECTIONGRADE}' \
)

if [ "$ENTERPRISE_TRANSFER_TO_S3" = 'true' ]
then

    # Call each DBT macro to initiate a transfer.
    num_txfrs=${#ENTERPRISE_TRANSFERS[@]}/2

    for (( i=0; i<${num_txfrs}; i++ ));
    do
        dbt run-operation ${ENTERPRISE_TRANSFERS[$i*2]} --args \'${ENTERPRISE_TRANSFERS[$i*2+1]}\' --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
    done

fi
