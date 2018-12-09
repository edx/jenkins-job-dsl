#!/usr/bin/env bash
set -e

echo "POSTing the request for GitHub to create a new status."
echo "Response from GitHub:"
curl \
--connect-timeout 3.05 \
--max-time 120 \
--silent \
--show-error \
-H "Authorization: token $GITHUB_OAUTH_TOKEN" \
-d '{"state": "'"$BUILD_STATUS"'", "target_url": "'"$TARGET_URL"'", "description": "'"$DESCRIPTION"'", "context": "'"$CONTEXT"'"}' \
https://api.github.com/repos/$GITHUB_ORG/$GITHUB_REPO/statuses/$GIT_SHA
