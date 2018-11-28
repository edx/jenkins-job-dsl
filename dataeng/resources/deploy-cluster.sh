VENV_ROOT=$WORKSPACE/venvs
mkdir -p $VENV_ROOT
rm -rf $WORKSPACE/logs
if [ ! -d "$VENV_ROOT/analytics-configuration" ]
then
    virtualenv $VENV_ROOT/analytics-configuration
fi
. $VENV_ROOT/analytics-configuration/bin/activate
make -C analytics-configuration provision.emr
