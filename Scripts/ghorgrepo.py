
'''
NAME
    ghorgrepo.py

DESCRIPTION
    Sets permissions for a GitHub team on an organization repository.

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


def repo_metadata(**kwargs):
    ''' Create and return dict with repository metadata for creating a 
        GitHub repository.

        See `Create an organization repository <https://docs.github.com/en/rest/reference/repos#create-an-organization-repository>` 
        for values of the keyword arguments.
    '''
    md0 = {'name': v[0] for k, v in kwargs.items() if k == 'gh_repo'}
    md1 = {k: v for k, v in kwargs.items() if not isinstance(v, list)
           and k not in EXCLUDED_KEYS and v}
    return {**md0, **md1}


def create_repo(gh_org, repo={}):
    ''' Creates the GitHub repository for the organization.

        :param gh_org: the GitHub organization.
        :param repo: dict with repository metadata.
        :return: created repository metadata.
        :rtype: dict
    '''
    data = json.dumps(repo, ensure_ascii=False).encode('gbk')
    req = urllib.request.Request(
        url=f'https://api.github.com/orgs/{gh_org}/repos',
        headers={
            'Authorization': f'token {os.getenv(GPAT)}', 'Content-Type': f'{GH_CONTENT_TYPE}',
            'Accept': f'{GH_ACCEPT}'},
        data=data,
        method='POST')

    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


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
                        help='Name of the new GitHub repository')
    parser.add_argument('-d', '--debug',
                        action='store_true',
                        help='Turn on debug logging')
    parser.add_argument('-j', '--generate-repo-json-only', action='store_true',
                        help='Generate repository json, but do not create the repository')
    parser.add_argument('-A', '--auto-init', action='store_true',
                        help='Override default auto_init setting (False).')
    parser.add_argument('-B', '--allow-rebase-merge', action='store_false',
                        help='Override default allow_rebase_merge setting (True).')
    parser.add_argument('-C', '--license-template', metavar='LICENSE_KEYWORD',
                        help='Open source license to apply')
    parser.add_argument('-D', '--description',
                        help='A short description of the repository')
    parser.add_argument('-E', '--is-template', action='store_true',
                        help='Override default is_template setting (False).')
    parser.add_argument('-G', '--gitignore-template', metavar='LANG_OR_PLATFORM',
                        help='Desired language or platform .gitignore template to apply')
    parser.add_argument('-H', '--homepage', metavar='URL',
                        help='A URL with more information about the repository')
    parser.add_argument('-I', '--has-issues', action='store_false',
                        help='Override default has_issues setting (True).')
    parser.add_argument('-L', '--delete-branch-on-merge', action='store_false',
                        help='Override default delete_branch_on_merge setting (True).')
    parser.add_argument('-M', '--allow-merge-commit', action='store_false',
                        help='Override default allow_merge_commit setting (True).')
    parser.add_argument('-P', '--private', action='store_false',
                        help='Override default private repository setting (True).')
    parser.add_argument('-R', '--has-projects', action='store_false',
                        help='Override default has_projects setting (True).')
    parser.add_argument('-S', '--allow-squash-merge', action='store_false',
                        help='Override default allow_squash_merge setting (True).')
    # Disabled since the nebula-preview header has to be set, see
    # https://docs.github.com/rest/reference/repos#create-an-organization-repository
    # parser.add_argument('-V', '--visibility', choices=[
    #                     'private', 'internal', 'public'], default='private',
    #                     help='Repository visibility (default is private)')
    parser.add_argument('-W', '--has-wiki', action='store_false',
                        help='Override default has_wiki setting (True).')

    args = parser.parse_args()

    if args.debug:
        logging.basicConfig(format='%(levelname)s: %(message)s',
                            level=logging.DEBUG)

    # Too many flags to pass in as keyword arguments, so use vars(args) as
    # shortcut (causes us to do some filtering, but better than individually
    # specifying keyword args; credit https://stackoverflow.com/a/5710402/37776).
    repo_metadata = repo_metadata(**vars(args))

    if args.generate_repo_json_only:
        print(json.dumps(repo_metadata, indent=2, sort_keys=True))
        sys.exit(0)

    err = None
    new_repo = {}
    try:
        new_repo = create_repo(args.gh_org[0], repo_metadata)
    except urllib.error.HTTPError as e:
        err = e
    else:
        if new_repo:
            logging.debug(json.dumps(new_repo, indent=2, sort_keys=True))
    finally:
        if err:
            logging.debug(err.headers)
            logging.debug(json.dumps(err.read().decode(
                'utf-8'), indent=2, sort_keys=True))
            print(
                f'HTTPS call to GitHub failed! {err.code} - {err.msg}', file=sys.stderr)
            sys.exit(1)
