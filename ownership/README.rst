Ownership maintenance jobs
==========================

Jobs for maintaining code ownership records.

``sync_repos.py`` can be called either with arguments or environment
variables. Since the latter is used in Travis, that's the format given
in this example (after running ``make requirements`` in a
virtualenv)::

  SYNC_REPOS_GITHUB_USERNAME=example \
  SYNC_REPOS_GITHUB_TOKEN_FILE=<(pass github-creds) \
  SYNC_REPOS_GOOGLE_CREDS_FILE=<(pass google-creds) \
  SYNC_REPOS_SPREADSHEET_URL="https://docs.google.com/spreadsheets/d/1str...bKo/edit" \
  SYNC_REPOS_SPREADSHEET_WORKSHEET_NAME="Individual Repo Ownership" \
  python3 sync_repos.py

The script will read repositories from the Github orgs and the
specified spreadsheet and print out a list of recommended actions to
take to bring the lists into sync.

The Github creds should be a personal access token for a service
account user that has read access to the required orgs. The Google
creds should be a JSON API creds file for a service account, and the
service account will need to be added as a viewer on the spreadsheet.
