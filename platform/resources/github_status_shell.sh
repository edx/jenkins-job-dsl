#!/usr/bin/env bash
set -e

echo "POSTing the request for GitHub to create a new status."
echo "Response from GitHub:"
curl \
-s -S --retry 3 \
-H "Authorization: token $GITHUB_OAUTH_TOKEN" \
-d '{"state": "'"$BUILD_STATUS"'", "target_url": "'"$TARGET_URL"'", "description": "'"$DESCRIPTION"'", "context": "'"$CONTEXT"'"}' \
https://api.github.com/repos/$GITHUB_ORG/$GITHUB_REPO/statuses/$GIT_SHA

# If this is a passing master build of any of the contexts, they will signal via
# the CREATE_DEPLOYMENT variable that we should try to create a new deployment.
# Each of the contexts will attempt this, but the new deployment will be created
# only after ALL of the contexts reports in as passed. (In effect, after the
# last one passes.)
#
# FYI. Here is an example of the syntax if we wanted to pass a payload.
#    -d '{"ref": "'"$GIT_SHA"'", "payload": {"git_sha": "'"$GIT_SHA"'"}, "environment": "ci", "auto_merge": false, "required_contexts": ["jenkins/a11y", "jenkins/bokchoy", "jenkins/js", "jenkins/lettuce", "jenkins/python", "jenkins/quality"]}' \
#
if [ "$CREATE_DEPLOYMENT" == 'true' ]
then
    echo "POSTing the request for GitHub to create a new deployment."
    echo "Response from GitHub:"
    curl \
    -s -S --retry 3 \
    -H "Authorization: token $GITHUB_OAUTH_TOKEN" \
    -d '{"ref": "'"$GIT_SHA"'", "environment": "ci", "auto_merge": false, "required_contexts": ["jenkins/a11y", "jenkins/bokchoy", "jenkins/js", "jenkins/lettuce", "jenkins/python", "jenkins/quality"]}' \
    https://api.github.com/repos/$GITHUB_ORG/$GITHUB_REPO/deployments
fi
