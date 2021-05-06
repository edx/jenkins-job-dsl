import os
import json
import sys
import urllib2

import requests
import jenkins

payload = os.getenv('payload')
target = os.getenv('target')

# a map from payload actions to github event header values
github_event_map = {
        'opened': 'pull_request', # action for newly created pull requests
        'synchronized': 'pull_request', # action for updated pull requests
        'created': 'issue_comment' # action for new comments on a pull request
}

target_instance = jenkins.Jenkins(target)
json_data = json.loads(payload)

# determine the correct rest endpoint and github event header to use for this payload
if 'action' in json_data.keys():
    if json_data['action'] not in github_event_map.keys():
        sys.stdout.write('This webhook is not used by the GHPRB and will not be ')
        sys.stdout.write('forwarded\n')
        with open('custom_description', 'w') as fi:
            fi.write("Gracefully skipping this one...")
        sys.exit(0)
    sys.stdout.write('This is a pull request, sending to {}/ghprbhook/'.format(target))
    target_endpoint = "{}/ghprbhook/".format(target)
    github_event = github_event_map[json_data['action']]
elif json_data['ref'] == 'refs/heads/master':
    sys.stdout.write('This is a merge, sending to {}/github-webhook/'.format(target))
    target_endpoint = "{}/github-webhook/".format(target)
    github_event = 'push'
else:
    sys.stdout.write('This webhook is not used by the us and will not be ')
    sys.stdout.write('forwarded\n')
    with open('custom_description', 'w') as fi:
        fi.write("Gracefully skipping this one...")
    sys.exit(0)

# fail if the target jenkins is inaccessible or not accepting new jobs
try:
    assert not target_instance.get_info()['quietingDown']
except jenkins.JenkinsException:
    sys.stdout.write('The target Jenkins is not accessible\n')
    sys.exit(1)
except AssertionError:
    sys.stdout.write('The target Jenkins is shutting down\n')
    sys.exit(1)

# only the user agent and github event headers are required by the github pull
# request builder to be considered a valid webhook
headers = { 'User-Agent': 'GitHub-Hookshot/79cd007', 'X-Github-Event': github_event}

req = requests.post(target_endpoint, headers=headers, data={ 'payload': payload } )

# write a file containing the description of this build to be consumed via the
# `set build description` plugin
with open('custom_description', 'w') as fi:
    fi.write("Forwarded {} to {}".format(github_event, target_endpoint))
