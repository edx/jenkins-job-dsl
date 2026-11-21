####################################################################
# Common configuration
# Assumes that ACCEPTANCE_TEST_CONFIG has been defined.

export VENV_ROOT=$WORKSPACE/build/venvs
PIP_INSTALL="pip install"

####################################################################
# Tasks configuration

TASKS_BIN=$VENV_ROOT/analytics-tasks/bin

# Set environment variable used in task acceptance service:
export REMOTE_TASK=$TASKS_BIN/remote-task

####################################################################
# Exporter configuration

EXPORTER_BIN=$VENV_ROOT/analytics-exporter/bin

EXPORTER_CONFIG=default.yaml

# Set environment variable used in test_database_export
export EXPORTER=$EXPORTER_BIN/exporter
export COURSE_EXPORTER=$EXPORTER_BIN/course-exporter

# Exporter configuration destination

ROOT=${WORKSPACE}/analytics-config/analytics-exporter
SECURE_HASH=`GIT_DIR=./analytics-config/.git git rev-parse HEAD`
EXPORTER_CONFIG_BUCKET=$EXPORTER_BUCKET_PATH/$SECURE_HASH
EXPORTER_CONFIG_PATH=${EXPORTER_CONFIG_BUCKET}/${EXPORTER_CONFIG}
GPG_KEYS_PATH=${EXPORTER_CONFIG_BUCKET}/gpg-keys

####################################################################
# Install pre-requisites

env | sort

mkdir -p $VENV_ROOT

[ -f $TASKS_BIN/activate ] || virtualenv -p /usr/bin/python2.7 $VENV_ROOT/analytics-tasks
virtualenv -p /usr/bin/python2.7 $VENV_ROOT/analytics-exporter
# The virtualenv on this version of Jenkins shiningpanda is old, so manually update both pip and setuptools before loading exporter.
$EXPORTER_BIN/$PIP_INSTALL -U 'pip==20.3.4'
$EXPORTER_BIN/$PIP_INSTALL -U 'setuptools<45'
$EXPORTER_BIN/$PIP_INSTALL -U 'wheel'
$EXPORTER_BIN/$PIP_INSTALL -U 'six'
$EXPORTER_BIN/$PIP_INSTALL -U -r $WORKSPACE/analytics-exporter/requirements.txt -r $WORKSPACE/analytics-exporter/github_requirements.txt
$EXPORTER_BIN/$PIP_INSTALL -e $WORKSPACE/analytics-exporter/

####################################################################
# Upload exporter configuration

$EXPORTER_BIN/$PIP_INSTALL awscli

$EXPORTER_BIN/aws s3 cp ${ROOT}/${EXPORTER_CONFIG} ${EXPORTER_CONFIG_PATH}
$EXPORTER_BIN/aws s3 cp --recursive ${ROOT}/gpg-keys ${GPG_KEYS_PATH}

####################################################################
# Run tests

. $TASKS_BIN/activate && make -C analytics-tasks install test-acceptance
