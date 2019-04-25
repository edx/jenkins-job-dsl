#!/bin/bash

set -x 

cd $WORKSPACE/configuration

pip install -r requirements.txt
pip install click

. util/jenkins/assume-role.sh

assume-role ${ROLE_ARN}


python util/asg_event_notifications_util.py create_asg_event_notifications --topic_arn ${SNS_TOPIC_ARN} --confirm
