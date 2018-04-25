#! /usr/bin/env python3

"""
Command-line script to retrieve list of learners that have requested to be retired.
The script calls the appropriate LMS endpoint to get this list of learners.
"""
from __future__ import absolute_import
from __future__ import unicode_literals

import io
import sys
import logging
import click
import requests
import yaml


logging.basicConfig(stream=sys.stdout, level=logging.DEBUG)
LOG = logging.getLogger(__name__)


@click.command()
@click.option(
    '--client_id',
    help='ID of OAuth client used in svr-to-svr client credentials grant.'
)
@click.option(
    '--client_secret',
    help='Secret associated with OAuth client used in svr-to-svr client credentials grant.'
)
@click.option(
    '--lms_hostname',
    help='Hostname of LMS from which to retrieve learner list.',
    default='http://localhost'
)
@click.option(
    '--lms_port',
    help='Port number of LMS from which to retrieve learner list.'
)
@click.option(
    '--basic_auth_username',
    help='HTTP basic auth username, if used on LMS.'
)
@click.option(
    '--basic_auth_password',
    help='HTTP basic auth password, if used on LMS.'
)
@click.option(
    '--output_file',
    help="File in which to write the script's YAML output",
    default='learners_to_retire.yml'
)
def get_learners_to_retire(client_id,
                           client_secret,
                           lms_hostname,
                           lms_port,
                           basic_auth_username,
                           basic_auth_password,
                           output_file):
    """
    Retrieves a JWT token as the retirement service user, then calls the LMS
    endpoint to retrieve the list of learners awaiting retirement.
    """
    if lms_port:
        lms_hostname += ':' + str(lms_port)

    # Retrieve an OAuth access token to use for LMS requests.
    token_url = '{hostname}/oauth2/access_token'.format(
        hostname=lms_hostname
    )
    basic_auth = None
    if basic_auth_username and basic_auth_password:
        basic_auth = requests.auth.HTTPBasicAuth(
            username=basic_auth_username,
            password=basic_auth_password
        )
    params = {
        'client_id': client_id,
        'client_secret': client_secret,
        'grant_type': 'client_credentials',
        'token_type': 'jwt'
    }
    response = requests.post(token_url, data=params)
    if response.status_code != 200:
        raise Exception("Unable to retrieve access token: {}".format(response.content))
    r = response.json()
    jwt = r['access_token']
    headers = {
        'Authorization': 'JWT {}'.format(jwt)
    }

    # Request the learners to retire.
    learners_url = '{hostname}/api/user/v1/accounts/retirement_queue/'.format(
        hostname=lms_hostname
    )
    params = {
        'cool_off_days': 7,
        'states': ['PENDING', 'COMPLETE']
    }
    kwargs = {
        'params': params,
        'headers': headers
    }
    if basic_auth:
        kwargs['auth'] = basic_auth
    response = requests.get(url=learners_url, **kwargs)
    if response.status_code != 200:
        raise Exception("Unable to retrieve learners to retire: {}".format(response.content))

    output_yaml = {
        'learners_to_retire': response.json(),
    }

    with io.open(output_file, 'w') as stream:
        yaml.safe_dump(
            output_yaml,
            stream,
            default_flow_style=False,
            explicit_start=True
        )


if __name__ == "__main__":
    get_learners_to_retire(auto_envvar_prefix='RETIREMENT')
