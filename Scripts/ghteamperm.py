'''
NAME
    ghteamperm.py

DESCRIPTION
    Sets permissions for a GitHub team in an organization.

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

           >>> var_state(foo=1, bar='bar').
           ['foo: 1', 'bar: bar']
    '''
    return [f'{k}: {v}' for k, v in kwargs.items()]


def naive_slugify(text):
    ''' Returns the slug of the given text.

        :param text: the text to turn into a slug.
        :return: slug of text
        :rtype: str
    '''
    return text.replace(' ', '-').lower()


def set_permission(gh_org, team_slug, gh_owner, repo, permission={}):
    ''' Sets the permission for the given team on the given repository.

        :param gh_org: GitHub organization
        :param team_slug: team slug
        :param gh_owner: repository owner
        :param repo: repository name
        :param permission: permission dict
    '''
    data = json.dumps(permission, ensure_ascii=False).encode('gbk')
    req = urllib.request.Request(
        url=f'https://api.github.com/orgs/{gh_org}/teams/{team_slug}/repos/{gh_owner}/{repo}',
        headers={
            'Authorization': f'token {os.getenv(GPAT)}', 'Content-Type': f'{GH_CONTENT_TYPE}',
            'Accept': f'{GH_ACCEPT}'},
        data=data,
        method='PUT')

    with urllib.request.urlopen(req) as resp:
        pass


if __name__ == '__main__':

    if not os.getenv(GPAT):
        print(f'Set environment variable {GPAT}', file=sys.stderr)
        sys.exit(1)

    parser = ArgumentParser(description=DESC,
                            formatter_class=RawDescriptionHelpFormatter)
    parser.add_argument('gh_org', nargs=1,
                        metavar='ORG',
                        help='GitHub organization')
    parser.add_argument('gh_repo', nargs=1,
                        metavar='REPO',
                        help='Name of the GitHub repository')
    parser.add_argument('gh_team', nargs=1,
                        metavar='TEAM',
                        help='Name of the GitHub team')
    parser.add_argument('gh_perm', nargs=1,
                        metavar='PERMISSION',
                        choices=['pull', 'push', 'admin',
                                 'maintain', 'triage'],
                        default='pull',
                        help='Permssion to grant')
    parser.add_argument('gh_owner', nargs='?', metavar='OWNER',
                        help='Optional owner of REPO (assumed to be ORG)')
    parser.add_argument('-d', '--debug',
                        action='store_true',
                        help='Turn on debug logging')

    args = parser.parse_args()

    if args.debug:
        logging.basicConfig(format='%(levelname)s: %(message)s',
                            level=logging.DEBUG)

    permission = {'permission': args.gh_perm[0]}
    try:
        gh_owner = args.gh_owner[0]
    except:
        gh_owner = args.gh_org[0]

    gh_org = args.gh_org[0]
    gh_team = naive_slugify(args.gh_team[0])

    gh_repo = args.gh_repo[0]

    err = None
    try:
        set_permission(gh_org, gh_team, gh_owner, gh_repo, permission)
    except urllib.error.HTTPError as e:
        err = e
    finally:
        if err:
            logging.debug(err.headers)
            logging.debug(json.dumps(err.read().decode(
                'utf-8'), indent=2, sort_keys=True))
            print(
                f'HTTPS call to GitHub failed! {err.code} - {err.msg}', file=sys.stderr)
            sys.exit(1)
