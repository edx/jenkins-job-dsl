#!/bin/bash
set -eu -o pipefail

# Jenkins won't set an environment variable if it's empty.
# Set this to an empty string if it's unset.

curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash

source ~/.bashrc
source ~/.nvm/nvm.sh

nvm install 16
nvm use 16

node --version

PACKAGES="${PACKAGES:-}"

IFS=', ' read -ra REPOS <<<"$REPO_NAMES"

failed_repos=()

all_repos="$WORKSPACE/edx-repos"
mkdir -p "$all_repos"

all_venvs="$WORKSPACE/edx-venvs"
mkdir -p "$all_venvs"

commit_msg="$COMMIT_MESSAGE"
base_pr_body="${PR_BODY:-}"
draft="$DRAFT"
branch_name="${BRANCH_NAME:-cleanup-python-code}"
force_delete_old_prs="$FORCE_DELETE_OLD_PRS"

do_one_repo () {
  local repo="$1"
  local repo_dir="$all_repos/$repo"

  rm -rf "$repo_dir"
  git clone "https://github.com/$ORG/$repo.git" "$repo_dir"

  local venv_dir="$all_venvs/$repo"
  virtualenv --python=python"$PYTHON_VERSION" "$venv_dir" --clear -q
  source "$venv_dir/bin/activate"

  echo "Upgrading pip..."
  pip install "pip<22"

  local PKG_SPECS
  IFS=", " read -ra PKG_SPECS <<<"$PACKAGES"
  if [[ "${#PKG_SPECS[@]}" -gt 0 ]]; then
    echo "Installing extra packages..."
    pip install "${PKG_SPECS[@]}"
  fi

  echo "Running cleanup scripts..."
  cd "$repo_dir"
  # Run in a subshell for isolation, and enable same error/exit
  # handling (-eu -o pipefail) so that a failing command in a sequence
  # still marks the iteration as a failure. Turn on command echoing
  # (-x) for better debugging.
  bash -c "set -eu -o pipefail -x; $SCRIPTS" || {
      error_code="$?"
      echo "Script exited with non-zero exit code $error_code."
      echo "Check if this is a real failure or a non-zero exit that should be suppressed."
      return "$error_code"
  }
  echo "Script ran successfully."

  echo "Running script to create PR..."
  cd "$WORKSPACE/testeng-ci"
  pip install -r requirements/base.txt
  

    # if present and non-empty, get additional information for PR body from .git/cleanup-python-code-description
  if [ -s "$repo_dir/.git/cleanup-python-code-description" ]
  then
    local pr_description_from_file="$(cat "$repo_dir/.git/cleanup-python-code-description")"
  else
    local pr_description_from_file=''
  fi

  local description="$(cat <<EOF
$base_pr_body

<details>
<summary>Additional information from script execution</summary>

$pr_description_from_file
</details>

Python code cleanup by the cleanup-python-code Jenkins job.

<details>
<summary>This pull request was generated by the cleanup-python-code Jenkins job, which ran</summary>

\`\`\`
$SCRIPTS
\`\`\`

</details>

The following packages were installed:
\`$PACKAGES\`
EOF
)"

  if ${draft}
  then
     draftflag="--draft"
  else
     draftflag=""
  fi

  if ${force_delete_old_prs}
  then
     force_delete_old_prs_flag="--force-delete-old-prs"
  else
     force_delete_old_prs_flag="--no-force-delete-old-prs"
  fi

  python -m jenkins.pull_request_creator --repo-root="$repo_dir" \
         --base-branch-name="$branch_name" --commit-message="$commit_msg" \
         --pr-title="$commit_msg" --pr-body="$description" \
         --user-reviewers="$PR_USER_REVIEWERS" --team-reviewers="$PR_TEAM_REVIEWERS" \
         ${force_delete_old_prs_flag} ${draftflag} --untracked-files-required=true


  rm -rf "$repo_dir"
  deactivate
  rm -rf "$venv_dir"
}


for repo in "${REPOS[@]}"; do
  # This &/wait hack is because if we used a pipeline like
  # `do_one_repo || ...` then the set -e would be suppressed inside
  # the function. Backgrounding the call and then putting the wait
  # inside the conditional is apparently the usual workaround.
  #
  # Running in a subshell means we don't have to deactivate the venv.
  (do_one_repo "$repo") &
  wait $! || failed_repos+=("$repo")
done

if [ ${#failed_repos[@]} -ne 0 ]; then
  echo "Following repositories failed during execution of cleanup scripts causing the job to fail"
  echo "${failed_repos[@]}"
  exit 1
fi
