#!/usr/bin/env bash
set -ex


# Setup to run dbt commands
cd $WORKSPACE/prefect-flows

COMMIT_ID=$(git rev-parse --short HEAD)
# Only run deployment on merge commits, otherwise exit
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

git diff $PREV_MERGE_COMMIT_ID --name-only # Printing for debug purpose

# Search for .py and .toml extension files in the git diff
FULLFILE=$(git diff $PREV_MERGE_COMMIT_ID --name-only | grep -E ".*\.(py|toml)")

# Remove downstream.properties file to write new input
rm -f ${WORKSPACE}/downstream.properties
# Output 'FLOW_NAME=' in file. This will be used as name of parameter in downstream jobs
echo -n "FLOW_NAME=" > "${WORKSPACE}/downstream.properties"

# Loop will extract the immediate parent directory name of all the .toml or .py files that has changed
files=($FULLFILE)
for file in "${files[@]}"
do
  # Taking the immediate parent name e.g.if flows/load_google_sheets_to_snowflake/load_google_sheets_to_snowflake.py is the modified file it will extract the immediate
  # parent directory name which is load_google_sheets_to_snowflake
  parentname="$(basename "$(dirname "$file")")"
  temp="$(echo -n "${parentname} ")"
  list=$list$temp
done

# If both .toml and .py file of a flow has changed those immediate parent directory entries will be add twice so removing the duplicates
duplicatesRemoved=$(echo "$list" | awk '{for (i=1;i<=NF;i++) if (!a[$i]++) printf("%s%s",$i,FS)}{printf("\n")}')

echo $duplicatesRemoved # Priting for debugging purpose

# Loop will parse all files and append it with 'FLOW_NAME=' to pass this parameter in downstream job
files=($duplicatesRemoved)
for file in "${files[@]}"
do
  # Writing all the changed flows in downstream.properties file which is passed as a parameter to downstream job
  echo -n "prefect-flows-deployment-${file}, " >> "${WORKSPACE}/downstream.properties"
done

sed -i 's/[ \t]*$//' ${WORKSPACE}/downstream.properties # Removing trailing space character
sed -i 's/,$//' ${WORKSPACE}/downstream.properties     	# Removing trailing comma
