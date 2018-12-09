# The environment needs a time to start. TODO: Make this wait more intelligent.
sleep ${SLEEP_TIME}

xvfb-run --server-args="-screen 0 1600x1200x24" ./scripts/jenkins-acceptance-tests.sh
