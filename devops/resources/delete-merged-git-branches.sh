#!/usr/bin/env bash

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u
#
# Script to delete merged branches in edx-platform repository
# Options
#    -r dry run
# Example
#   ./delete-merged-git-branches.sh
#   ./delete-merged-git-branches.sh -r

dry_run=0

while getopts 'r' opt; do
    case "$opt" in
        r) dry_run=1 ;;
        *) echo 'error in command line parsing' >&2
           exit 1
    esac
done

IGNORE_BRANCHES="open-release|origin/release$|olive"

cd edx-platform/

# this will list branches which are merged into origin/master but not deleted
# loop into all branches except  HEAD, origin/master and branches in IGNORE_BRANCHES
for branch in $(git branch -r --merged origin/master | grep -v HEAD | grep -v origin/master | grep -vE "${IGNORE_BRANCHES}"); do
    # check if merged branch is older than 1 week
    if [ -z "$(git log -1 --since='1 week ago' -s $branch)" ]; then
        echo -e "$(git show -s --format="%ai %ar by %an" $branch) $branch"
    fi
done | sort -r > branches.txt

for branch in $(awk '{print $NF}' branches.txt | sed 's|origin/||'); do
    if [ "$dry_run" -eq 1 ]; then
        echo Would have deleted branch "${branch}"
    else
        echo Deleting "${branch}"
        git push origin --delete "${branch}"
    fi
done
