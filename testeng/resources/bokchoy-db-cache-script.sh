#!/bin/bash
set -e

cd edx-platform
source scripts/jenkins-common.sh

paver update_local_bokchoy_db_from_s3 --rewrite_fingerprint

cd ../testeng-ci
pip install -r requirements/base.txt
python -m jenkins.bokchoy_db_pull_request --sha $GIT_COMMIT --repo_root "../edx-platform"
