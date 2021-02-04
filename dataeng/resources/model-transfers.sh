#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/warehouse-transforms
# To install right version of dbt
pip install -r requirements.txt


cd $WORKSPACE/warehouse-transforms/projects/$DBT_PROJECT


# Each transfer is two elements in this array:
#     - the DBT macro name
#     - the macro arguments to use when calling the DBT macro
DAILY_TRANSFERS=( \
    unload_program_reporting_learner_enrollments_report '{database: PROD, schema: PROGRAMS_REPORTING}' \
    unload_program_reporting_program_cohort_report '{database: PROD, schema: PROGRAMS_REPORTING}' \
)

if [ "$MODELS_TO_TRANSFER" = 'daily' ]
then

    # Call each DBT macro to initiate a transfer.
    num_txfrs=${#DAILY_TRANSFERS[@]}/2

    for (( i=0; i<${num_txfrs}; i++ ));
    do
        dbt run-operation ${DAILY_TRANSFERS[$i*2]} --args "${DAILY_TRANSFERS[$i*2+1]}" --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
    done

fi

# TODO: Adding single_file argument is a short term solution. We have created
# https://openedx.atlassian.net/browse/ENT-4087 to find a better way.
ENTERPRISE_TRANSFERS=( \
    enterprise_copy_to_s3 '{prefix: ENTERPRISE, table: ENT_BASE_ENTERPRISE_USER}' \
    enterprise_copy_to_s3 '{prefix: ENTERPRISE, table: FACT_CUSTOMER_COURSE_DAILY_ROLLUP_ADMIN_DASH}' \
    enterprise_copy_to_s3 '{prefix: ENTERPRISE, table: FACT_ENROLLMENT_ADMIN_DASH}' \
    enterprise_copy_to_s3 '{prefix: ENTERPRISE, table: FACT_ENROLLMENT_ENGAGEMENT_DAY_ADMIN_DASH}' \
    enterprise_copy_to_s3 '{prefix: PEARSON, table: ENT_REPORT_PEARSON_COURSE_METRICS, single_file: true}' \
    enterprise_copy_to_s3 '{prefix: PEARSON, table: ENT_REPORT_PEARSON_BLOCK_COMPLETION, single_file: true}' \
    enterprise_copy_to_s3 '{prefix: PEARSON, table: ENT_REPORT_PEARSON_PERSISTENTSUBSECTIONGRADE, single_file: true}' \
)

if [ "$MODELS_TO_TRANSFER" = 'enterprise' ]
then

    # Call each DBT macro to initiate a transfer.
    num_txfrs=${#ENTERPRISE_TRANSFERS[@]}/2

    for (( i=0; i<${num_txfrs}; i++ ));
    do
        dbt run-operation ${ENTERPRISE_TRANSFERS[$i*2]} --args "${ENTERPRISE_TRANSFERS[$i*2+1]}" --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
    done

fi
