#!/usr/bin/python

"""
A vault token helper to change the default location for vault token.
By default vault writes token to home direcotry of the server. This
creates a race condition when multiple jobs try to access the token
in parallel. In order to avoid it generating a separate token for 
each job in its workspace.
"""

import sys
import os.path


def get(PATH):
    
    if os.path.exists(PATH):
      token = open(PATH,"r")
      token = token.read().strip()
      
    
def store(PATH):
    
    with open(PATH,"wb+") as f:
        for token in sys.stdin:
            f.write(token.strip())
            
    
def erase(PATH):

    if os.path.exists(PATH):
        os.remove(PATH)

if __name__ == "__main__":

    # get jobs worspace path
    workspace = os.environ['WORKSPACE']
    # path to store token   
    PATH=workspace+"/vault-config/vault-token"
    
    args = sys.argv[1:][0]
    
    if args == "get":
        get(PATH)

    elif args == "erase":
        erase(PATH)

    elif args == "store":
        store(PATH)
        
    else:
        print("Unknown method {}".format(args))
        exit(1)


