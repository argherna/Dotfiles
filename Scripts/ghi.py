#!/usr/bin/env python


'''\
NAME
----
  ghi.py

SYNOPSIS
--------
  ghi.py <command> args

DESCRIPTION
-----------
  Maintain issues in a GitHub repository.

  *Note*: Output of this program is raw json. This program should pass
  through a formatter program to make it more readable.
'''


from argparse import ArgumentParser
from argparse import FileType
from argparse import RawDescriptionHelpFormatter
import getpass
import json
import logging
import requests
import sys


DESC = 'Maintain issues in a GitHub repository'
EPILOG = '''\
If you use 2-factor authentication, generate a Personal access token at
https://github.com/settings/tokens and use it for your password.
'''


class GitHubUnsuccessError(Exception):
    '''
    Maybe GitHub didn't fail. Maybe YOU did. Maybe this script did. Either
    way, this error means the call to the GitHub API was not successful. We
    can usually blame the caller since this error is raised only when the API
    returns a non-successful call.
    See https://developer.github.com/v3/
    '''
    pass


def get_auth(auth):
    '''Separate username and password. Prompts for password if none present.'''
    user = None
    password = None

    if auth is not None:
        try:
            [user, password] = auth.split(':')
        except ValueError:
            user = cli_args.auth[0]
            password = getpass.getpass()

    return user, password


def issues_request_url(repo_owner, repo_name):
    '''Returns a URL suitable for reading all issues.'''
    return 'https://api.github.com/repos/%s/%s/issues' % (repo_owner, repo_name)


def list_main(args):
    '''Lists the GitHub issues.

       Output will be to stdout by default.

       :param: args - command-line arguments for the list function
    '''
    req_url = issues_request_url(args.repository_owner[0],
                                 args.repository_name[0])

    r = requests.get(req_url, auth=(get_auth(args.auth[0])))
    if r.status_code != 200:
        print(r.status_code)
        raise GitHubUnsuccessError('Failed to get issues!')

    issues = r.json()

    print >>args.outfile, json.dumps(issues, sort_keys=True, indent=2)


if __name__ == '__main__':
    parser = ArgumentParser(description=DESC, epilog=EPILOG,
                            formatter_class=RawDescriptionHelpFormatter)
    parser.add_argument('-a', '--auth', nargs=1, type=str,
                        metavar='USER[:PASS]',
                        help='Basic authentication credentials')
    parser.add_argument('-L', '--logging_level', nargs=1, type=str,
                        choices=['DEBUG', 'INFO',
                                 'WARNING', 'ERROR', 'CRITICAL'],
                        default='ERROR',
                        help='Set logging level (default is WARNING)')

    subparsers = parser.add_subparsers(dest='command_to_execute',
                                       description='GitHub issues commands',
                                       help='Commands help')

    parser_l = subparsers.add_parser('list',
                                     help='Lists all open GitHub issues in the specified repository')
    parser_l.add_argument('repository_owner', nargs=1,
                          metavar='OWNER',
                          help='Owner of the GitHub repository to list issues from')
    parser_l.add_argument('repository_name', nargs=1,
                          metavar='REPO',
                          help='Name of the GitHub repository to list issues from')
    parser_l.add_argument('outfile', nargs='?', type=FileType('w'),
                          metavar='FILE',
                          default=sys.stdout,
                          help='Output file to store labels Json')

    cli_args = parser.parse_args()

    numeric_level = getattr(logging, cli_args.logging_level[0].upper(), None)
    logging.basicConfig(format='%(levelname)s: %(message)s',
                        level=numeric_level)

    try:
        if cli_args.command_to_execute == 'list':
            list_main(cli_args)
        else:
            sys.stderr.write('Unknown function!')
            sys.exit(1)
    except GitHubUnsuccessError as message:
        sys.stderr.write('%s Exiting!\n' % (message))
        sys.exit(1)
