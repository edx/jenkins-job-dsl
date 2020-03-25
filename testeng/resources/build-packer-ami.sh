#!/usr/bin/env bash

# This script will run packer to create an AMI for use as a Jenkins worker

if [ -z "${JENKINS_WORKER_AMI}" ]; then
export JENKINS_WORKER_AMI=$(aws ec2 describe-images \
    --region us-east-1 \
    --owners 099720109477 \
    --filters 'Name=name,Values=ubuntu/images/hvm-ssd/ubuntu-xenial-16.04-amd64*' \
    --query 'sort_by(Images,&CreationDate)[-1].ImageId' \
    --output text)
fi

export TEST_PLATFORM_VERSION=${PLATFORM_VERSION}

# Variable is used for tagging the AMI
export DELETE_OR_KEEP_AMI=${DELETE_OR_KEEP}

# Variable is used for installing and setting NewRelic agent on workers
export NEW_RELIC_KEY=${NEW_RELIC_KEY}

# Activate the Python virtualenv
. $HOME/edx-venv/bin/activate

cd util/packer
echo $PWD
packer build $PACKER_JSON
