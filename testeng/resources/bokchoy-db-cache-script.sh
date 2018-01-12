#!/bin/bash
set -e

cd edx-platform
source scripts/jenkins-common.sh

paver update_local_bokchoy_db_from_s3 --rewrite_fingerprint

cd ../testeng-ci
pip install -r requirements.txt
cd jenkins
python bokchoy_db_pull_request.py --sha $GIT_COMMIT --repo_root "../../edx-platform"
