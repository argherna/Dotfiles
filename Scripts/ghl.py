#!/usr/bin/env python

'''\
NAME
----
    ghl.py

SYNOPSIS
--------
    ghl.py <command> args

DESCRIPTION
-----------
    Manage GitHub repository labels
'''

from argparse import ArgumentParser
from argparse import FileType
from argparse import RawDescriptionHelpFormatter
import getpass
import json
import logging
import requests
import sys
import urllib


DESC = 'Maintain labels in a GitHub repository'
EPILOG = '''\
If you use 2-factor authentication, generate a Personal access token at
https://github.com/settings/tokens and use it for your password.
'''


class GitHubUnsuccessError(Exception):
    '''
    Maybe GitHub didn't fail. Maybe YOU did. Maybe this script did. Either
    way, the call was unsuccessful.
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


def read_labels(infile):
    '''Returns the list of labels from the given filename'''
    labels = None
    labels = json.loads(infile.read())
    return labels


def label_request_url(repo_owner, repo_name, label_name):
    '''Returns a URL suitable for reading, updating, and deleting a single label.'''
    return 'https://api.github.com/repos/%s/%s/labels/%s' % (repo_owner, repo_name, urllib.quote(label_name))


def labels_request_url(repo_owner, repo_name):
    '''Returns a URL suitable for creating labels or reading all labels.'''
    return 'https://api.github.com/repos/%s/%s/labels' % (repo_owner, repo_name)


def label_exists(label_name, repo_owner, repo_name, auth):
    '''Returns True if the Label exists.'''
    req_url = label_request_url(repo_owner, repo_name, label_name)
    r = requests.head(req_url, auth=auth)
    return r.status_code == 200


def write_output(json_dat, outfile):
    '''Writes data to the specified output file.'''
    print >>outfile, json.dumps(json_dat, sort_keys=True, indent=2)


def delete_label(label, repo_owner, repo_name, auth):
    '''Calls the GitHub API to delete the given label.'''
    req_url = label_request_url(repo_owner, repo_name, label['name'])
    r = requests.delete(req_url, auth=auth)
    if r.status_code != 204:
        raise GitHubUnsuccessError('Failed to delete label %s' % (label['name']))


def save_label(label, repo_owner, repo_name, auth):
    '''Calls the GitHub API to save the given label.'''
    label_json = json.dumps(label)
    req_url = labels_request_url(repo_owner, repo_name)
    r = requests.post(req_url, auth=auth, data=label_json)
    if r.status_code != 201:
        raise GitHubUnsuccessError('Failed to update label %s (%d)' % (label['name'], r.status_code))


def update_label(label, repo_owner, repo_name, auth):
    '''Calls the GitHub API to update a single label.'''
    label_json = json.dumps(label)
    req_url = label_request_url(repo_owner, repo_name, label['name'])
    r = requests.patch(req_url, auth=auth, data=label_json)
    if r.status_code != 200:
        raise GitHubUnsuccessError('Failed to update label %s' % (label['name']))


def delete_main(cli_args):
    '''Deletes the list of GitHub labels.'''
    labels =  read_labels(cli_args.infile)
    auth = (get_auth(cli_args.auth[0]))
    for label in labels:
        if label_exists(label['name'],
                        cli_args.repository_owner[0],
                        cli_args.repository_name[0],
                        auth):
            delete_label(label,
                         cli_args.repository_owner[0],
                         cli_args.repository_name[0],
                         auth)


def list_main(cli_args):
    '''Lists the GitHub labels.'''
    req_url = labels_request_url(cli_args.repository_owner[0],
                                 cli_args.repository_name[0])

    r = requests.get(req_url, auth=(get_auth(cli_args.auth[0])))
    if r.status_code != 200:
        raise GitHubUnsuccessError('Failed to get labels!')

    labels = r.json()
    if cli_args.prune_urls:
        for label in labels:
            label.pop('url', None)

    write_output(labels, cli_args.outfile)


def upload_main(cli_args):
    '''Uploads the list of labels from input.'''
    labels = read_labels(cli_args.infile)

    auth = (get_auth(cli_args.auth[0]))
    for label in labels:
        if label_exists(label['name'],
                        cli_args.repository_owner[0],
                        cli_args.repository_name[0],
                        auth):
            update_label(label,
                         cli_args.repository_owner[0],
                         cli_args.repository_name[0],
                         auth)
        else:
            save_label(label,
                       cli_args.repository_owner[0],
                       cli_args.repository_name[0],
                       auth)


if __name__ == '__main__':
    parser = ArgumentParser(description=DESC, epilog=EPILOG,
                            formatter_class=RawDescriptionHelpFormatter)
    parser.add_argument('-a', '--auth', nargs=1, type=str,
                          metavar='USER[:PASS]',
                          help='Basic authentication credentials')
    parser.add_argument('-l', '--log', nargs=1, type=str,
                          metavar='DEBUG|INFO|WARNING|ERROR|CRITICAL',
                          default='WARNING',
                          help='Set logging level (default is WARNING)')

    subparsers = parser.add_subparsers(dest='command_to_execute',
                                       description='GitHub label commands',
                                       help='Commands help')

    parser_d = subparsers.add_parser('delete',
                                     help='Delete GitHub labels from specified repository')
    parser_d.add_argument('repository_owner', nargs=1,
                          metavar='OWNER',
                          help='Owner of the GitHub repository to upload the labels to')
    parser_d.add_argument('repository_name', nargs=1,
                          metavar='REPO',
                          help='Name of the GitHub repository to upload the labels to')
    parser_d.add_argument('infile', nargs='?', type=FileType('r'),
                          metavar='FILE',
                          default=sys.stdin,
                          help='Json file containing a list of GitHub label definitions (default is stdin)')

    parser_l = subparsers.add_parser('list',
                                     help='Lists all GitHub labels in the specified repository')
    parser_l.add_argument('-p', '--prune-urls',
                          action='store_true',
                          help='Prune url fields from output')
    parser_l.add_argument('repository_owner', nargs=1,
                          metavar='OWNER',
                          help='Owner of the GitHub repository to upload the labels to')
    parser_l.add_argument('repository_name', nargs=1,
                          metavar='REPO',
                          help='Name of the GitHub repository to upload the labels to')
    parser_l.add_argument('outfile', nargs='?', type=FileType('w'),
                          metavar='FILE',
                          default=sys.stdout,
                          help='Output file to store labels Json')

    parser_u = subparsers.add_parser('upload',
                                     help='Upload GitHub labels to the specified repository')
    parser_u.add_argument('repository_owner', nargs=1,
                          metavar='OWNER',
                          help='Owner of the GitHub repository to upload the labels to')
    parser_u.add_argument('repository_name', nargs=1,
                          metavar='REPO',
                          help='Name of the GitHub repository to upload the labels to')
    parser_u.add_argument('infile', nargs='?', type=FileType('r'),
                          metavar='FILE',
                          default=sys.stdin,
                          help='Json file containing a list of GitHub label definitions (default is stdin)')

    cli_args = parser.parse_args()

    numeric_level = getattr(logging, cli_args.log[0].upper(), None)
    logging.basicConfig(format='%(levelname)s: %(message)s', level=numeric_level)

    try:
        if cli_args.command_to_execute == 'list':
            list_main(cli_args)
        elif cli_args.command_to_execute == 'upload':
            upload_main(cli_args)
        elif cli_args.command_to_execute == 'delete':
            delete_main(cli_args)
        else:
            sys.stderr.write('Unknown function!')
            sys.exit(1)
    except GitHubUnsuccessError, (message):
        sys.stderr.write('%s Exiting!\n' % (message))
        sys.exit(1)
