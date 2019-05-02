#!/bin/bash -xe

cd $WORKSPACE/tubular
pip install -r requirements.txt

python scripts/message_prs_in_range.py --org "edx" --repo "app-permissions" --base_sha ${GIT_PREVIOUS_COMMIT_1} --head_sha ${GIT_COMMIT_1} --release ${ENVIRONMENT} --extra_text "Job run successfully on ${ENVIRONMENT}-${DEPLOYMENT}"
