'''
NAME
    ghteamperm.py

DESCRIPTION
    Sets permissions for a GitHub user on an organization repository.

ENVIRONMENT
    GITHUB_PERSONAL_ACCESS_TOKEN
        Your GitHub personal access token for authenticating to the GitHub API.
'''
from argparse import ArgumentParser
from argparse import FileType
from argparse import RawDescriptionHelpFormatter
import json
import logging
import os
import sys
import urllib.request


DESC = 'Create GitHub organizational repositories'
EXCLUDED_KEYS = ('debug', 'generate_repo_json_only')
GH_ACCEPT = 'application/vnd.github.v3+json'
GH_CONTENT_TYPE = 'application/json'
GPAT = 'GITHUB_PERSONAL_ACCESS_TOKEN'


def var_state(**kwargs):
    ''' Returns a list of variable names and values. 

        Sample usage::

           >>> var_state(foo=1, bar='bar')
           ['foo: 1', 'bar: bar']
    '''
    return [f'{k}: {v}' for k, v in kwargs.items()]


def set_permission(gh_owner, gh_user, repo, permission={}):
    ''' Sets the permission for the given user on the given repository.

        :param gh_owner: repository owner
        :param gh_user: GitHub username
        :param repo: repository name
        :param permission: permission dict
        :return: response data if any
        :rtype: dict
    '''
    data = json.dumps(permission, ensure_ascii=False).encode('gbk')
    req = urllib.request.Request(
        url=f'https://api.github.com/repos/{gh_owner}/{repo}/collaborators/{gh_user}',
        headers={
            'Authorization': f'token {os.getenv(GPAT)}', 'Content-Type': f'{GH_CONTENT_TYPE}',
            'Accept': f'{GH_ACCEPT}'},
        data=data,
        method='PUT')

    with urllib.request.urlopen(req) as resp:
        resp_data = resp.read()
        if resp_data:
            return json.loads(resp_data)
        else:
            return {}


if __name__ == '__main__':

    if not os.getenv(GPAT):
        print(f'Set environment variable {GPAT}', file=sys.stderr)
        sys.exit(1)

    parser = ArgumentParser(description=DESC,
                            formatter_class=RawDescriptionHelpFormatter)
    parser.add_argument('gh_owner', nargs=1, metavar='OWNER',
                        help='Owner of REPO')
    parser.add_argument('gh_repo', nargs=1,
                        metavar='REPO',
                        help='Name of the GitHub repository')
    parser.add_argument('gh_user', nargs=1,
                        metavar='USERNAME',
                        help='GitHub username')
    parser.add_argument('gh_perm', nargs=1,
                        metavar='PERMISSION',
                        choices=['pull', 'push', 'admin',
                                 'maintain', 'triage'],
                        default='pull',
                        help='Permssion to grant')
    parser.add_argument('-d', '--debug',
                        action='store_true',
                        help='Turn on debug logging')

    args = parser.parse_args()

    if args.debug:
        logging.basicConfig(format='%(levelname)s: %(message)s',
                            level=logging.DEBUG)

    gh_owner = args.gh_owner[0]
    gh_repo = args.gh_repo[0]
    gh_user = args.gh_user[0]
    permission = {'permission': args.gh_perm[0]}

    err = None
    permission_resp = {}
    try:
        permission_resp = set_permission(
            gh_owner, gh_user, gh_repo, permission)
    except urllib.error.HTTPError as e:
        err = e
    else:
        if permission_resp:
            logging.debug(json.dumps(permission_resp,
                          indent=2, sort_keys=True))
    finally:
        if err:
            logging.debug(err.headers)
            logging.debug(json.dumps(err.read().decode(
                'utf-8'), indent=2, sort_keys=True))
            print(
                f'HTTPS call to GitHub failed! {err.code} - {err.msg}', file=sys.stderr)
            sys.exit(1)
