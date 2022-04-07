#!/usr/bin/env bash


# make a directory to store vault token helper and config file
mkdir ${WORKSPACE}/vault-config
# get token helper from repo
cp ${WORKSPACE}/jenkins-job-dsl/dataeng/resources/token-helper.py ${WORKSPACE}/vault-config/token-helper.py
# remove the repo after getting token helper
rm -rf ${WORKSPACE}/jenkins-job-dsl
# create path for helper
path_to_helper = ${WORKSPACE}"/vault-config/token-helper.py"
# set up config for config file
token_config = "$(echo token_helper = \"$path_to_helper\" )"
# create the config file
echo $token_config >> ${WORKSPACE}/vault-config/vault_config