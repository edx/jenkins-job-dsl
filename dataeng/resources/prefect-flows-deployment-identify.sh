#!/usr/bin/env bash
set -ex


# Setup to run dbt commands
cd $WORKSPACE/prefect-flows
echo $ONE
ONE="prefect-flows-deployment-intermediate"
#Only run deployment on merge commits, otherwise exit
HEAD_COMMIT=$(git rev-parse HEAD)
LAST_MERGE_COMMIT=$(git log --merges origin/master --format='%H' --max-count=1)
if [ $HEAD_COMMIT == $LAST_MERGE_COMMIT ]
then
    echo "This is one of merge commit, Run CD"
else
    echo "Exiting because not a merge commit"
    exit 0
fi


#Get second last merge commit id and compares it with the HEAD to find (git diff) files changed.
PREV_MERGE_COMMIT_ID=$(git log --merges origin/master --format='%H' --max-count=2 | sed -n 2p)

git diff $PREV_MERGE_COMMIT_ID --name-only # Printing for debug purpose


# Remove downstream.properties file to write new input
rm -f ${WORKSPACE}/downstream.properties
# Output 'FLOW_NAME=' in file. This will be used as name of parameter in downstream jobs
echo -n "FLOW_NAME=" > "${WORKSPACE}/downstream.properties"

# Extract the immediate parent directory name of all the .toml or .py files those are changed and removing the duplicates that
# may occur if both .toml and .py files of a flow has changed.
FLOWS_TO_DEPLOY=$(git diff $PREV_MERGE_COMMIT_ID --name-only | grep -E ".*\.(py|toml)" |cut -d'/' -f2 |sort -u)

# Loop will parse list and append it with 'FLOW_NAME=' to pass this parameter in downstream job
list=($FLOWS_TO_DEPLOY)
for item in "${list[@]}"
do
  # Writing all the changed flows in downstream.properties file which is passed as a parameter to downstream job
  echo -n "prefect-flows-deployment-${item}, " >> "${WORKSPACE}/downstream.properties"
done

sed -i 's/[ \t]*$//' ${WORKSPACE}/downstream.properties # Removing trailing space character
sed -i 's/,$//' ${WORKSPACE}/downstream.properties     	# Removing trailing comma
