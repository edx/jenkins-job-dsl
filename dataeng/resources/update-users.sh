VENV_ROOT=$WORKSPACE/venvs
mkdir -p $VENV_ROOT

virtualenv --python=python3.8 $VENV_ROOT/analytics-configuration
. $VENV_ROOT/analytics-configuration/bin/activate
make -C analytics-configuration users.update
