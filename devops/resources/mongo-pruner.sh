#!/bin/bash
set -exuo pipefail

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

cd $WORKSPACE/tubular
pip install -r requirements.txt
pip install awscli

set +x

SESSIONID=$(date +"%s")

RESULT=(`aws sts assume-role --role-arn $ROLE_ARN \
            --role-session-name $SESSIONID \
            --query '[Credentials.AccessKeyId,Credentials.SecretAccessKey,Credentials.SessionToken]' \
            --output text`)

export AWS_ACCESS_KEY_ID=${RESULT[0]}
export AWS_SECRET_ACCESS_KEY=${RESULT[1]}
export AWS_SECURITY_TOKEN=${RESULT[2]}
export AWS_SESSION_TOKEN=${AWS_SECURITY_TOKEN}

set -x

NAME_TAG="${ENVIRONMENT}-${DEPLOYMENT}-mongo"
IP_ADDRESSES=`aws ec2 describe-instances\
               --filter Name=tag:Name,Values=$NAME_TAG\
               --output text --query 'Reservations[*].Instances[*].PrivateIpAddress'\
               --region us-east-1`

MONGO_IPS=`echo $IP_ADDRESSES | sed 's/ /,/g'`

python scripts/structures.py\
        --database-name ${DATABASE_NAME}\
        --connection "mongodb://${MONGO_USER}:${MONGO_PASSWORD}@${MONGO_IPS}/${DATABASE_NAME}" make_plan --retain 10 plan.json

python scripts/structures.py\
        --database-name ${DATABASE_NAME}\
        --connection "mongodb://${MONGO_USER}:${MONGO_PASSWORD}@${MONGO_IPS}/${DATABASE_NAME}" prune --delay 5000 plan.json
