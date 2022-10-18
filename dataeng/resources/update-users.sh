VENV_ROOT=$WORKSPACE/venvs
mkdir -p $VENV_ROOT

if [ ! -d "$VENV_ROOT/analytics-configuration" ]
then
    virtualenv --python=python3.8 $VENV_ROOT/analytics-configuration
fi
. $VENV_ROOT/analytics-configuration/bin/activate
make -C analytics-configuration users.update