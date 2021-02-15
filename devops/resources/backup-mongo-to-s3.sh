#!/bin/bash
set -exuo pipefail

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

cd $WORKSPACE/configuration
pip install -r requirements.txt
. util/jenkins/assume-role.sh

assume-role ${ROLE_ARN} 7200

cd $WORKSPACE/sysadmin

bash util/clone-mongo/clone-mongo.sh  \
  --collections modulestore,modulestore.active_versions,modulestore.definitions,modulestore.location_map,modulestore.structures,fs.chunks,fs.files \
  --from-db-hosts ${IP_ADDRESSES}  \
  --from-user ${MONGO_DB_USER}  \
  --from-pass ${MONGO_DB_PASSWORD}   \
  --from-db prod    \
  --from-port 27017   \
  --dump-dest $WORKSPACE/${JOB_NAME}/${BUILD_ID} \
  --cleanup no
bash util/clone-mongo/clone-mongo.sh  \
  --collections activities,contents,delayed_backend_mongoid_jobs,subscriptions,system.indexes,system.users,users \
  --from-db-hosts ${IP_ADDRESSES}  \
  --from-user ${MONGO_DB_USER}  \
  --from-pass ${MONGO_DB_PASSWORD}   \
  --from-db prod-forum    \
  --from-port 27017   \
  --dump-dest $WORKSPACE/${JOB_NAME}/${BUILD_ID} \
  --cleanup no

aws s3 cp --recursive $WORKSPACE/${JOB_NAME}/${BUILD_ID} s3://edx-mongohq/jenkins-backups/${ENVIRONMENT}-${DEPLOYMENT}/${BUILD_ID}/

rm -rf $WORKSPACE/${JOB_NAME}/${BUILD_ID}/*
rmdir $WORKSPACE/${JOB_NAME}/${BUILD_ID}
