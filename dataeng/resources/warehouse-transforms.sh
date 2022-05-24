#!/usr/bin/env bash
set -ex

# Creating python 3.8 virtual environment to run dbt warehouse-transform job
PYTHON38_VENV="py38_venv"
virtualenv --python=python3.8 --clear "${PYTHON38_VENV}"
source "${PYTHON38_VENV}/bin/activate"

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
  dbt seed --full-refresh --models "$SEED_SELECTOR" --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ ; ret=$?;
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
dbt $DBT_COMMAND $FULL_REFRESH_ARG --models $MODEL_SELECTOR --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ ; ret=$?;
postCommandChecks "run" $ret ;

# The setup here is being done in order to integrate monte carlo and dbt
# This feature was requested as part of DESUPPORT-1358

# find the path of target folder which has dbt artifacts
# this will be used in our dbt monte carlo integration
# using pwd to find the fully qualified path for target folder.
# because monte carlo cli command needs fully qualified path for artifcats.
curr_dir="$(pwd)" 
TARGET_FOLDER_PATH="$(find ${curr_dir} -type d -name "target")"

# Do not print commands in this function since they may contain secrets.
set +x

# Retrieve a vault token corresponding to the jenkins AppRole.  The token is then stored in the VAULT_TOKEN variable
# which is implicitly used by subsequent vault commands within this script.
# Instructions followed: https://learn.hashicorp.com/tutorials/vault/approle#step-4-login-with-roleid-secretid
export VAULT_TOKEN=$(vault write -field=token auth/approle/login \
      role_id=${ANALYTICS_VAULT_ROLE_ID} \
      secret_id=${ANALYTICS_VAULT_SECRET_ID}
  )

# set monte carlo api keys to integrate with monte carlo
export MCD_DEFAULT_API_ID="$(vault kv get -version=${MONTE_CARLO_KEYS_VAULT_KV_VERSION} \
 -field=MCD_DEFAULT_API_ID ${MONTE_CARLO_KEYS_VAULT_KV_PATH})"
export MCD_DEFAULT_API_TOKEN="$(vault kv get -version=${MONTE_CARLO_KEYS_VAULT_KV_VERSION} \
 -field=MCD_DEFAULT_API_TOKEN ${MONTE_CARLO_KEYS_VAULT_KV_PATH})"

# Re-enable printing of commands.
set -x 

# following commands will upload dbt metadata into monte carlo data catalog      
montecarlo import dbt-manifest ${TARGET_FOLDER_PATH}/manifest.json --project-name $DBT_PROJECT      
montecarlo import dbt-run-results ${TARGET_FOLDER_PATH}/run_results.json --project-name $DBT_PROJECT


if [ "$SKIP_TESTS" != 'true' ]
then
    # By default, exclude no models from testing.
    exclude_param=""
    if [ "$TEST_SOURCES_FIRST" = 'true' ]
    then
        # Exclude the sources from testing, since they were already tested pre-'dbt run'.
        exclude_param="--exclude source:*"
    fi

    # By default, do not specify an indirection selection option. As of dbt 1.0.3 the default is to use "eager" indirect
    # selection, which runs a test if ANY model which touches it has been selected.  Typically this is the desired
    # behavior, but when building only a subset of a DAG it can be prohibitive.
    INDIRECT_SELECTION_PARAM=""
    if [ "$CAUTIOUS_INDIRECT_SELECTION" = 'true' ]
    then
        INDIRECT_SELECTION_PARAM="--indirect-selection=cautious"
    fi

    # Run all tests as specified.
    dbt test --models $MODEL_SELECTOR $exclude_param $INDIRECT_SELECTION_PARAM --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ ; ret=$?;
    postCommandChecks "test" $ret ;
fi