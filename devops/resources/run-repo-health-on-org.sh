#!/bin/bash
set -e -v

# Click requires this to work cause it interfaces weirdly with python 3 ASCII default
export LC_ALL=C.UTF-8
export LANG=C.UTF-8

ORG_NAMES=("edx" "openedx")
GITHUB_USER_EMAIL="admin+edxstatusbot@edx.org"
# If the REPORT_DATE variable is set and not an empty string parse the date to standardize it.
if [[ ! -z $REPORT_DATE ]]; then REPORT_DATE=$(date '+%Y-%m-%d' -d "$REPORT_DATE"); fi

# for unknown reasons, repo-health-data does not come with correct refs
# git checkout master is necessary to get the refs/heads/master
cd "${WORKSPACE}/repo-health-data"
git show-ref
git checkout master
git show-ref

###############################
# Get list of repos in edx org.
###############################

cd "${WORKSPACE}"
virtualenv --python=/usr/bin/python3.8 venv -q --clear
source venv/bin/activate

cd "${WORKSPACE}/repo_tools"
make install
pip install -e .
REPOSIORIES_URLS_FILE="${WORKSPACE}/repositories.txt"
cat /dev/null > $REPOSIORIES_URLS_FILE
for ORG_NAME in ${ORG_NAMES[@]}; do
  get_org_repo_urls "${ORG_NAME}" --url_type https --forks --add_archived --output_file "${REPOSIORIES_URLS_FILE}" --username "${GITHUB_USER_EMAIL}" --token "${GITHUB_TOKEN}" --ignore-repo "clamps-ghsa-c4rq-qwgr-pj5h"
done
unset ORG_NAME

deactivate

############################
# Run checks on repositories
############################

# Recreate new virtualenv just for testeng-ci script

cd ${WORKSPACE}
# delete previous venv to make room for new one
rm -rf venv
virtualenv --python=/usr/bin/python3.8 venv -q
source venv/bin/activate

# Install checks and dashboarding script, this should also install pytest-repo-health
cd edx-repo-health
pip install -r requirements/base.txt
pip install -e .
cd ${WORKSPACE}

# data destination folder setup

METADATA_FILE_DIST="${WORKSPACE}/repo-health-data/docs/checks_metadata.yaml"

failed_repos=()

OUTPUT_FILE_POSTFIX="_repo_health.yaml"

echo
echo
# Git clone each repo in org and run checks on it
input="${REPOSIORIES_URLS_FILE}"
while IFS= read -r line
do
    cd "$WORKSPACE"

    if [[ "$line" =~ ^(git@github\.com:|https://github\.com/)([a-zA-Z0-9_.-]+?)/([a-zA-Z0-9_.-]+?)\.git$ ]]; then
        ORG_NAME="${BASH_REMATCH[2]}"
        REPO_NAME="${BASH_REMATCH[3]}"
        FULL_NAME="$ORG_NAME/$REPO_NAME"
    else
        echo "Skipping <$line>: Could not recognize as a GitHub URL in order to extract org and repo name."
        continue
    fi

    if [[ "$REPO_NAME" = "edx-repo-health" ]]; then
        echo "Skipping <$line>: edx-repo health"
        continue
    fi

    if [[ -n "$ONLY_CHECK_THIS_REPOSITORY" && "$FULL_NAME" != "$ONLY_CHECK_THIS_REPOSITORY" ]]; then
        echo "Skipping <$line>: ONLY_CHECK_THIS_REPOSITORY was set, and does not match"
        continue
    fi

    echo
    echo
    echo "Processing repo: $FULL_NAME"

    rm -rf target-repo
    git clone -- "${line/https:\/\//https:\/\/$GITHUB_TOKEN@}" target-repo || {
        failed_repos+=("$FULL_NAME")
        continue
    }

    cd target-repo

    # If the REPORT_DATE variable is set and not an empty string.
    if [[ ! -z $REPORT_DATE ]]
      then
        # If a specific date is given for report
        FIRST_COMMIT=$(git log --reverse --format="format:%ci" | sed -n 1p)
        if [[ $REPORT_DATE > ${FIRST_COMMIT:0:10} ]]
          then
            git checkout `git rev-list -n 1 --before="${REPORT_DATE} 00:00" master`
          else
            echo "${REPO_NAME} doesn't have any commits prior to ${REPORT_DATE}"
            failed_repos+=("$FULL_NAME")
            continue
        fi
    fi

    ORG_DATA_DIR="${WORKSPACE}/repo-health-data/individual_repo_data/${ORG_NAME}"
    # make sure destination folder exists
    mkdir -p "$ORG_DATA_DIR"

    OUTPUT_FILE_NAME=${REPO_NAME}${OUTPUT_FILE_POSTFIX}
    cd ${WORKSPACE}
    if pytest --repo-health --repo-health-path edx-repo-health --repo-path target-repo --repo-health-metadata "${METADATA_FILE_DIST}" --output-path "${ORG_DATA_DIR}/${OUTPUT_FILE_NAME}" -o log_cli=true --exitfirst --noconftest -v -c /dev/null
    then
        true
    elif pytest --repo-health --repo-health-path edx-repo-health --repo-path target-repo --repo-health-metadata "${METADATA_FILE_DIST}" --output-path "${ORG_DATA_DIR}/${OUTPUT_FILE_NAME}" -o log_cli=true --exitfirst --noconftest -v -c /dev/null
    # rerun the same command if it fails once
    then
        true
    else
        failed_repos+=("$FULL_NAME") && continue
    fi
done < "$input"

##############################
# Recalculate aggregated data.
##############################

# Go into data repo, recalculate aggregate data, and push a PR
IFS=,
failed_repo_names=`echo "${failed_repos[*]}"`
# Delete existing dashboards to re compile
find "${WORKSPACE}/repo-health-data/dashboards" -type f -iname "\dashboard*.csv" -delete

for ORG_NAME in ${ORG_NAMES[@]}; do
    echo "Pushing data for org $ORG_NAME"
    cd "${WORKSPACE}/repo-health-data/individual_repo_data/${ORG_NAME}"
    ls
    repo_health_dashboard --data-dir . --configuration "${WORKSPACE}/edx-repo-health/repo_health_dashboard/configuration.yaml" --output-csv "${WORKSPACE}/repo-health-data/dashboards/dashboard" --append
done

deactivate
cd ${WORKSPACE}
rm -rf venv

# Only commit the data if running with master and no REPORT_DATE is set.
if [[ ${EDX_REPO_HEALTH_BRANCH} == 'master' && -z ${REPORT_DATE} ]]
then

  ###########################################
  # Commit files and push to repo-health-data
  ###########################################
  echo "Commit new files and push to master..."

  commit_message="Update repo health data"

  cd "${WORKSPACE}/repo-health-data"

  if [[ ! "${failed_repos}" ]]
  then
     commit_message+="\nFollowing repos failed repo health checks\n ${failed_repo_names}"

     for full_name in "${failed_repos[@]}"
     do
          OUTPUT_FILE_NAME="${full_name}${OUTPUT_FILE_POSTFIX}"
          echo "reverting repo health data for ${OUTPUT_FILE_NAME}"
          git checkout -- "individual_repo_data/${OUTPUT_FILE_NAME}"
     done
  fi

  git add --all
  git status
  git diff-index --quiet HEAD || git commit -m ${commit_message}
  git push origin master

fi

if [[ ${#failed_repos[@]} -ne 0 ]]; then
  echo
  echo
  echo "The following repositories failed while executing pytest repo-health scripts, causing the job to fail:"
  echo
  echo "    ${failed_repos[*]}"
  echo
  echo '(Search the console output for "ERRORS", or search for any of the failed repo names above.)'
  echo
  echo "Runbook: <https://2u-internal.atlassian.net/wiki/spaces/AT/pages/16386018/Repo+Health+Debugging+Runbook>"
  echo
  exit 1
fi
