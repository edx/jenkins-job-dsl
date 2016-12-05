export HOME=$WORKSPACE

pip install -r sailthru/requirements/base.txt
python ${SYNC_COMMAND}
