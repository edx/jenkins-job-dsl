#!/usr/bin/env bash

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

WHITELIST="open-release|origin/release$|olive"

cd edx-platform/

# this will list branches which are merged into origin/master but not deleted
# loop into all branches and match if it is not HEAD, origin/master and whitelist branches
for branch in $(git branch -r --merged origin/master | grep -v HEAD | grep -v origin/master | grep -vE "${WHITELIST}"); do
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
