#!/usr/bin/env bash

# This script will create a vault_config file.
# This config file is used to change the defualt location where vault token is stored.
# By default vault token is stored in home directory of server, this config file will change this behaviour 
# When token was stored at home directory of the server that was creating race condition when
# multiple jobs access the token in parallel, which was causing login failure for several chlid jobs
# Now creating separate token for each job in its workspace so that each job can use its own token

# making sure there is no vault config for new job 
rm -rf ${WORKSPACE}/vault-config
# make a directory to store vault token helper and config file
mkdir ${WORKSPACE}/vault-config
# create path for helper
path_to_helper = ${WORKSPACE}"/jenkins-job-dsl/dataeng/resources/token-helper.py"
# set up config for config file
token_config = "$(echo token_helper = \"$path_to_helper\" )"
# create the config file
echo $token_config >> ${WORKSPACE}/vault-config/vault_config