"""
Synchronize ownership information between repositories and spreadsheet.

Finds mismatches between the repositories listed in the ownership
tracking spreadsheet and the actual list of repositories in a
collection of Github organizations.
"""

from itertools import groupby
import re

import click
import github3
from github3.exceptions import NotFoundError
import gspread


class KnownError(Exception):
    """Known exception cases where we won't need a stack trace."""
    def __init__(self, message):
        super().__init__(message)
        self.message = message

@click.command()
@click.option(
    '--github-username',
    help="Username corresponding to the Github access token"
)
@click.option(
    '--github-token-file',
    help=("File containing Github token with repo scope on applicable orgs.\n"
          "(Recommend using a Bash process substitution.)")
)
@click.option(
    '--google-creds-file',
    help="JSON file containing Google API credentials"
)
@click.option(
    '--spreadsheet-url',
    help="Ownership spreadsheet URL"
)
@click.option(
    '--spreadsheet-worksheet-name',
    help="The name of the worksheet (tab) to use on the ownership spreadsheet"
)
def main(github_username, github_token_file, google_creds_file, spreadsheet_url, spreadsheet_worksheet_name):
    """CLI entry point."""
    orgs = ['edx', 'edx-solutions', 'edx-ops']

    # Authenticate to Github
    with open(github_token_file, 'r') as tf:
        token = tf.readline().rstrip('\n')
    hub = github3.login(github_username, token)

    # Authenticate to Google
    worksheet = gspread.service_account(filename=google_creds_file) \
                       .open_by_url(spreadsheet_url) \
                       .worksheet(spreadsheet_worksheet_name)

    try:
        repos_gh = list(fetch_github(hub, orgs))
        repos_ss = list(fetch_spreadsheet(worksheet, orgs))

        actions = run_compare(hub, orgs, repos_gh, repos_ss)

        # For now, just make recommendations, don't make changes.
        report_actions(actions)
    except KnownError as e:
        print(e.message)


def run_compare(hub, orgs, repos_gh, repos_ss):
    """
    Compare repository lists from Github and spreadsheet and return a list of
    recommended actions for reconciling the lists.

    Each action is a dictionary with at least the following keys, and
    possibly others depending on the action:

    - action: The type of action, such as 'add_row' or 'move_repo_unsupported'
    - repo: The full name of the repo on Github ("org/name") related to this
      action
    - why: Description of why this action is recommended

    Actions relating to an existing row on the spreadsheet have a row_id key.

    """
    # List of actions to recommend at end of run.
    #
    # This list will *grow* as the comparison progresses.
    actions = []

    # For any archived repos, we don't want them in the spreadsheet
    # anyhow, and should recommend moving them to the edx-unsupported
    # org.
    archived = [r for r in repos_gh if r['archived']]
    for r in archived:
        actions.append({
            'action': 'move_repo_unsupported',
            'repo': r['name'],
            'why': ("Repo has been archived but is still in a main org "
                    "rather than edx-unsupported")
        })
        repos_gh.remove(r)

    names_all_gh = {r['name'] for r in repos_gh}
    names_all_ss = {r['name'] for r in repos_ss}

    # Bail out early on any duplicates, which complicate analysis
    names_ss_dup = names_all_ss.copy() # a list of duplicated names
    for uname in set(names_ss_dup):
        names_ss_dup.remove(uname)
    if names_ss_dup:
        error_lines = [
            ("Manual intervention required: The following repositories are "
             "listed multiple times in the spreadsheet:")]
        for uniq_dup_name in set(names_ss_dup):
            row_ids = [str(r['row_id']) for r in repos_ss if r['name'] == uniq_dup_name]
            error_lines.append("%s on rows %s" % (uniq_dup_name, ', '.join(row_ids)))
        raise KnownError(error_lines)

    # Mismatched names in both directions.
    #
    # These lists will *shrink* as the comparison progresses and they are
    # converted into action items.
    names_only_gh = names_all_gh - names_all_ss
    names_only_ss = names_all_ss - names_all_gh

    def find_row_by_name(repo_name):
        return next(filter(lambda r: r['name'] == repo_name, repos_ss))

    def recommend_delete_row(name, why, more_data=None):
        """Add an action recommending a row delete."""
        actions.append({
            'action': 'delete_row',
            'repo': name,
            'row_id': find_row_by_name(name)['row_id'],
            'why': why,
            **(more_data or {})
        })
        names_only_ss.remove(name)

    # First, if a repo is only in the spreadsheet, check if it was
    # moved or archived
    for ss_name in names_only_ss.copy():
        try:
            (ss_repo_org, ss_repo_shortname) = ss_name.split('/', 2)
            found_repo = hub.repository(ss_repo_org, ss_repo_shortname)
        except NotFoundError:
            found_repo = None
        if not found_repo:
            recommend_delete_row(ss_name, "In spreadsheet but not Github, and not moved")
        elif str(found_repo.owner) not in orgs:
            recommend_delete_row(
                ss_name, "Moved to a Github org not in the scan list",
                {'new_location': str(found_repo)})
        elif found_repo.archived:
            recommend_delete_row(ss_name, "Repo has been archived")
        else:
            new_name = str(found_repo).lower()
            if [r for r in repos_ss if r['name'] == new_name]:
                raise KnownError(
                    "Manual intervention required: A repo that has moved "
                    "exists in the spreadsheet under both its old name "
                    "and its new name. %s -> %s"
                     % (ss_name, new_name))
            actions.append({
                'action': 'rename_row',
                'repo': ss_name,
                'new_name': new_name,
                'row_id': find_row_by_name(ss_name)['row_id'],
                'why': ("Repo has been renamed, and is not already in the "
                        "spreadsheet under that name")
            })
            names_only_ss.remove(ss_name)
            names_only_gh.remove(new_name)

    # Now check ones that are only in Github. We've already filtered
    # out archived repos and removed any that are pending a rename on
    # the spreadsheet.
    for gh_name in names_only_gh.copy():
        actions.append({
            'action': 'add_row',
            'repo': gh_name,
            'why': "Repo present in Github but not spreadsheet"
        })
        names_only_gh.remove(gh_name)

    # Make sure nothing is unaccounted for before presenting recommendations.
    if names_only_gh or names_only_ss:
        raise KnownError(
            "Failed to convert all mismatches between Github and spreadsheet "
            "into actions! Github remaining = %s; spreadsheet remaining = %s"
            % (names_only_gh, names_only_ss))

    # Ensure actions don't have any internal conflicts. It might be OK
    # if the same *name* comes up twice; for example, we might want to
    # move an archived repo to the unspported org *and* remove it from
    # the spreadsheet. However, two actions for the same row ID is a
    # problem.
    seen_row_ids = set()
    for action in actions:
        row_id = action.get('row_id')
        if not row_id:
            continue
        if row_id in seen_row_ids:
            raise KnownError(
                "Possible bug in script: Was going to suggest multiple "
                "actions for row %s: %s"
                % (row_id, [r for r in actions if r.get('row_id') == row_id]))
        seen_row_ids.add(row_id)

    return actions


def report_actions(actions):
    """Report the recommended actions without making changes."""
    if actions:
        print("Recommendations:")

        groupkey = lambda a: (a['action'], a['why'])
        for ((action_name, why), group) in groupby(sorted(actions, key=groupkey), groupkey):
            print()
            print("Action: %s -- %s" % (action_name, why))
            for action in group:
                # Extract a dictionary of all the remaining keys that
                # aren't being pulled out explicitly for display, such
                # as row numbers and new repo locations
                extra = {k: v for k, v in action.items() if k not in {'action', 'repo', 'why'}}
                if extra:
                    extra_str = " " + str(extra)
                else:
                    extra_str = ""
                print("- %s%s" % (action['repo'], extra_str))
    else:
        print("No changes required. ✔")


def fetch_github(hub, orgs):
    """
    Generator of repos found in Github orgs, as dicts:

    - name: Lowercased full name of repository
    - archived: Boolean, true if archived
    """
    for org in orgs:
        for repo in hub.organization(org).repositories():
            yield {'name': str(repo).lower(),
                   'archived': repo.archived}

def fetch_spreadsheet(worksheet, orgs):
    """
    Generator for repos found in spreadsheet, as dicts:

    - name: Lowercased full name of repository
    - row_id: Spreadsheet row number

    Raises an exception if any entries are not in provided orgs.
    """
    # Start numbering at row 2; headers are spreadsheet row 1
    for row_id, row in enumerate(worksheet.get_all_records(), start=2):
        repo_short_name = row['repo name'].lower()

        # Make sure the repo is unambiguous
        url = row['repo url']
        match = re.match(r'^https://github\.com/(?P<org>[^/]+)/(?P<name>[^/]+)/?$', url)
        if match:
            url_org = match.groupdict()['org'].lower()
            url_name = match.groupdict()['name'].lower()
            repo_name = url_org + '/' + url_name
        else:
            raise KnownError(
                "Cannot parse Github repo URL on spreadsheet row %s: %s"
                % (row_id, url))

        if repo_short_name != url_name:
            raise KnownError(
                "On row %s, repo name '%s' does not match URL %s"
                % (row_id, repo_short_name, url))

        if url_org not in orgs:
            raise KnownError(
                "Row %s has a repo in an org that isn't being scanned: %s"
                % (row_id, url))

        yield {'name': repo_name,
               'row_id': row_id}

if __name__ == '__main__':
    main(auto_envvar_prefix='SYNC_REPOS')
