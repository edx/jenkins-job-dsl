#!/usr/bin/env python

"""
A vault token helper to change the default location for vault token.
By default vault writes token to home direcotry of the server. This
creates a race condition when multiple jobs try to access the token
in parallel. In order to avoid it generating a separate token for 
each job in its workspace.

Documentation Reference:
https://www.vaultproject.io/docs/commands/token-helper
https://www.hashicorp.com/blog/building-a-vault-token-helper

"""

import os
import os.path
import sys

if os.environ.get('VAULT_TOKEN_PATH') is None:
    raise StandardError('env var VAULT_TOKEN_PATH is not set')
elif len(sys.argv) != 2 or sys.argv[1] not in ['get', 'erase', 'store']:
    raise StandardError('USAGE: get, erase, store')
tokenpath = os.environ['VAULT_TOKEN_PATH']
if sys.argv[1] == 'get' and os.path.isfile(tokenpath):
    with open(tokenpath, 'r') as f:
        sys.stdout.write(f.read().strip())
elif sys.argv[1] == 'erase' and os.path.isfile(tokenpath):
    os.remove(tokenpath)
elif sys.argv[1] == 'store':
    with open(tokenpath, 'wb+') as f:
        f.write(sys.stdin.read())