#!/usr/bin/env bash
set -ex


# Setup to run dbt commands
cd $WORKSPACE/prefect-flows



#### After testing start using master branch  ####

<< 'MULTILINE-COMMENT'
COMMIT_ID=$(git rev-parse --short HEAD)
# Only run CI on merge commits, otherwise exit
HEAD_COMMIT=$(git rev-parse HEAD)
LAST_MERGE_COMMIT=$(git log --merges origin/master --format='%H' --max-count=1)
if [ $HEAD_COMMIT == $LAST_MERGE_COMMIT ]
then
    echo "This is one of merge commit, Run CI"
else
    echo "Exiting because not a merge commit"
    exit 0
fi


# Get second last merge commit id and compares it with the HEAD to find (git diff) files changed.
PREV_MERGE_COMMIT_ID=$(git log --merges origin/master --format='%H' --max-count=2 | sed -n 2p)

git diff $PREV_MERGE_COMMIT_ID --name-only

MULTILINE-COMMENT
#### After testing start using master branch ###







BRANCH_POINT=$(git merge-base origin/master ${PREFECT_FLOWS_BRANCH})
git diff $BRANCH_POINT --name-only # Printing for debug purpose


FULLFILE=$(git diff $BRANCH_POINT --name-only | grep ".*\.py$")
FILENAMEEXT=$(basename -- "$FULLFILE")
FILENAME="${FILENAMEEXT%.*}"

flow_name=$FILENAME

echo "FLOW_NAME=${FILENAME}" > "${WORKSPACE}/downstream.properties"