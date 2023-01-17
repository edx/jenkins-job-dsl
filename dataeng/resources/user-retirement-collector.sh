#!/usr/bin/env bash

set -ex

# Display the environment variables again, this time within the context of a
# new subshell inside of the job. N.B. this should not print plain credentials
# because the only credentialsBindings we currently use is of type "file" which
# just stores a filename in the environment (rather than the content).
env

# Create and activate a virtualenv.  In case we ever change the concurrency
# setting on the jenkins worker, it would be safest to keep the builds from
# clobbering each other's virtualenvs.
VENV="venv-${BUILD_NUMBER}"
virtualenv --python=python3.8 --clear "${VENV}"
source "${VENV}/bin/activate"

# Make sure that when we try to write unicode to the console, it
# correctly encodes to UTF-8 rather than exiting with a UnicodeEncode
# error.
export PYTHONIOENCODING=UTF-8
export LC_CTYPE=en_US.UTF-8


# prepare tubular
cd $WORKSPACE/tubular
# snapshot the current latest versions of pip and setuptools.
pip install 'pip==21.0.1' 'setuptools==53.0.0'
pip install -r requirements.txt

# Create the directory where we will populate properties files, one per
# downstream build.
rm -rf $LEARNERS_TO_RETIRE_PROPERTIES_DIR
mkdir $LEARNERS_TO_RETIRE_PROPERTIES_DIR

# Call the script to collect the list of learners that are to be retired.
python scripts/get_learners_to_retire.py \
    --config_file=$WORKSPACE/user-retirement-secure/$ENVIRONMENT.yml \
    --output_dir=$LEARNERS_TO_RETIRE_PROPERTIES_DIR \
    --cool_off_days=$COOL_OFF_DAYS \
    --user_count_error_threshold=$USER_COUNT_ERROR_THRESHOLD \
    --max_user_batch_size=$MAX_USER_BATCH_SIZE