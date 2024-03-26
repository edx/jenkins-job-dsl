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

source $WORKSPACE/secrets-manager.sh
# Fetch the secrets from AWS
set +x
get_secret_value warehouse-transforms/profiles/profiles DBT_PASSWORD
set -x
export DBT_PASSWORD

# Fails the job if a dbt command fails and uploads the dbt artifacts to Snowflake if the job is configured for it
# First argument is the dbt operation name, second is the result code from the dbt command
function postCommandChecks {
  echo operation $1 and result code $2;

  if [ "$PUSH_ARTIFACTS_TO_SNOWFLAKE" = 'true' ]
  then
    # Errors from this operation are eaten as they are just telemetry data and not worth failing jobs over
    dbt run-operation upload_dbt_run_artifacts --args '{operation: '$1'}'  --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ || true
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
dbt clean --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt deps --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ --profile $DBT_PROFILE --target $DBT_TARGET

# Turn off automatic failure of this script if the command returns non-0 for the rest of these commands
set +e

if [ "$SKIP_SEED" != 'true' ]
then
  dbt seed --full-refresh --models "$SEED_SELECTOR" --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ ; ret=$?;
  postCommandChecks "seed" $ret ;
fi

# Source testing *before* model-building can be enabled/disabled with this envvar.
if [ "$TEST_SOURCES_FIRST" = 'true' ] && [ "$SKIP_TESTS" != 'true' ]
then
    # Run the source tests, sadly not just the ones upstream from this tag
    dbt test --models source:* --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ ; ret=$?;
    postCommandChecks "source_test" $ret ;
fi

# Parent models tests *before* model-building can be enabled/disabled with this envvar.
if [ "$TEST_PARENT_MODELS_FIRST" = 'true' ]
then
    # Copy the value of MODEL_SELECTOR to MODEL_SELECTOR_WITH_PARENTS and EXCLUDE_MODELS.
    MODEL_SELECTOR_WITH_PARENTS="$MODEL_SELECTOR"
    EXCLUDE_MODELS="$MODEL_SELECTOR"

    # Check if MODEL_SELECTOR_WITH_PARENTS doesn't start with '+', then add '+' at the beginning.
    if [ ${MODEL_SELECTOR_WITH_PARENTS:0:1} != "+" ]
    then
      MODEL_SELECTOR_WITH_PARENTS="+$MODEL_SELECTOR_WITH_PARENTS"
    fi

    # Check if EXCLUDE_MODELS starts with '+', then remove the '+' at the beginning
    if [ ${EXCLUDE_MODELS:0:1} = "+" ]
    then
      EXCLUDE_MODELS="${EXCLUDE_MODELS:1}"
    fi

    # This will only runs parents tests without running current models tests.
    dbt test --models $MODEL_SELECTOR_WITH_PARENTS --exclude $EXCLUDE_MODELS --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ ; ret=$?;
    postCommandChecks "parent_models_tests" $ret ;
fi

# Compile/build all models with this tag.
dbt $DBT_COMMAND $FULL_REFRESH_ARG --models $MODEL_SELECTOR --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ ; ret=$?;
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

    # By default, do not specify an indirection selection option. As of dbt 1.0.3 the default is to use "eager" indirect
    # selection, which runs a test if ANY model which touches it has been selected.  Typically this is the desired
    # behavior, but when building only a subset of a DAG it can be prohibitive.
    INDIRECT_SELECTION_PARAM=""
    if [ "$CAUTIOUS_INDIRECT_SELECTION" = 'true' ]
    then
        INDIRECT_SELECTION_PARAM="--indirect-selection=cautious"
    fi

    # Run all tests as specified.
    dbt test --models $MODEL_SELECTOR $exclude_param $INDIRECT_SELECTION_PARAM --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ ; ret=$?;
    postCommandChecks "test" $ret ;
fi
