#!/bin/bash
set -e

IFS=',' read -ra SCRIPTSTORUN <<<"$SCRIPTS"

PACKAGESTOINSTALL=$(echo $PACKAGES | tr , " ")

IFS=',' read -ra REPOS <<<"$REPO_NAMES"

failed_repos=()

for repo in "${REPOS[@]}"; do

  rm -rf "${repo}"

  if git clone https://github.com/edx/"${repo}".git; then true; else failed_repos+=("${repo}") && continue; fi

  rm -rf "${repo}"-code_cleanup_venv
  virtualenv --python=python"$PYTHON_VERSION" "${repo}"-code_cleanup_venv -q
  source "${repo}"-code_cleanup_venv/bin/activate

  echo "Upgrading pip..."
  if pip install pip==20.0.2; then true; else failed_repos+=("${repo}") && continue; fi

  echo "Getting current sha..."
  cd "${repo}"
  if git rev-parse HEAD &> /dev/null; then true; else failed_repos+=("${repo}") && continue; fi
  export CURRENT_SHA=$(git rev-parse HEAD)

  echo "Running cleanup..."

  if pip install $PACKAGESTOINSTALL; then true; else failed_repos+=("${repo}") && continue; fi

  for element in "${SCRIPTSTORUN[@]}"; do
    # shellcheck disable=SC2091
    if $(echo "$element"); then true; else failed_repos+=("${repo}") && continue; fi
  done

  echo "Running script to create PR..."
  cd ../testeng-ci
  if pip install -r requirements/base.txt; then true; else failed_repos+=("${repo}") && continue ; fi
  if python -m jenkins.cleanup_python_code --sha=$CURRENT_SHA --repo_root="../${repo}" --user_reviewers=$PR_USER_REVIEWERS --team_reviewers=$PR_TEAM_REVIEWERS --packages="$PACKAGES" --scripts="$SCRIPTS"
  then
    true
  else
    deactivate
    cd ..
    rm -rf "${repo}"-code_cleanup_venv
    failed_repos+=("${repo}") && continue
  fi

  deactivate
  cd ..
  rm -rf "${repo}"-code_cleanup_venv

done

if [ ${#failed_repos[@]} -ne 0 ]; then
  echo "Following repositories failed during execution of cleanup scripts causing the job to fail"
  echo "${failed_repos[@]}"
  exit 1
fi
