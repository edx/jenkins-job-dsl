#!/bin/bash

set -x 

export AWS_DEFAULT_REGION='us-east-1'

cd $WORKSPACE/configuration

pip install -r requirements3.txt
pip install click

. util/jenkins/assume-role.sh

assume-role ${ROLE_ARN}


python util/asg_event_notifications_util.py create-asg-event-notifications --topic_arn ${SNS_TOPIC_ARN} --confirm
