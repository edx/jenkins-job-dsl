#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/warehouse-transforms
# To install right version of dbt
pip install -r requirements.txt


cd $WORKSPACE/warehouse-transforms/projects/$DBT_PROJECT

# Choose the marts from which to transfer DBT models based on Jenkins job parameter.
if [ "$MODELS_TO_TRANSFER" = 'daily' ]
then
    MART_NAME=programs_reporting
elif [ "$MODELS_TO_TRANSFER" = 'enterprise' ]
    MART_NAME=enterprise
fi

ARGS="{mart: ${MART_NAME} }"

# Currently, only the programs_reporting mart's transfers are performed in the daily transfer.
#
# Call DBT to query the list of macros to call and the macro parameters to perform all transfers.
dbt run-operation get_all_s3_transfers --args "${ARGS}" > dbt_get_data_output_tmp.txt || exit

# Parse the output to extract the list of macros/parameters from the JSON and convert to DBT commands.
# The expected input format from the file looks like this:
#
# Running with dbt=0.19.1-b2
# [["unload_program_reporting_learner_enrollments_report", "{database: PROD, schema: PROGRAMS_REPORTING}"], ["unload_program_reporting_program_cohort_report", "{database: PROD, schema: PROGRAMS_REPORTING}"]]
#
# The output format looks like this:
#
# [ "dbt run-operation unload_program_reporting_learner_enrollments_report --args \"{database: PROD, schema: PROGRAMS_REPORTING}\"" ]
# [ "dbt run-operation unload_program_reporting_program_cohort_report --args \"{database: PROD, schema: PROGRAMS_REPORTING}\"" ]
commands=$( sed -n '/^\[/p' dbt_get_data_output_tmp.txt | jq '.[] | ["dbt run-operation " + .[0] + " --args " + "\"" + .[1] + "\""]' )

# Enable extended regex.
shopt -s extglob
# For each line in the list, strip whitespace, escape quotes, and eval the DBT command.
while IFS= read -r line; do
    if [[ $line == !(\]|\[) ]]; then
        stripped_line=$( echo $line | sed 's/^[ "]*//' | sed 's/["]$//' | sed 's/\\"/"/g' )
        echo "${stripped_line[@]}"
        eval "${stripped_line}"
    fi
done <<< "$commands"
