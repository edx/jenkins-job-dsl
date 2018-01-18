#!/usr/bin/env bash

# This script will run packer to create an AMI for use as a Jenkins worker

export TEST_PLATFORM_VERSION=${PLATFORM_VERSION}

# Variable is used for tagging the AMI
export DELETE_OR_KEEP_AMI=${DELETE_OR_KEEP}

# Variable is used for installing and setting NewRelic agent on workers
export NEW_RELIC_KEY=${NEW_RELIC_KEY}

# Variables for boto to connect with the testeng db cache bucket
export DB_CACHE_ACCESS_KEY_ID=${DB_CACHE_ACCESS_KEY_ID}
export DB_CACHE_SECRET_ACCESS_KEY=${DB_CACHE_SECRET_ACCESS_KEY}

# Activate the Python virtualenv
. $HOME/edx-venv/bin/activate

cd util/packer
echo $PWD
packer build $PACKER_JSON
