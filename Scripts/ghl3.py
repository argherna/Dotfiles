'''
NAME
    ghl3.py

DESCRIPTION
    Manage GitHub labels for a repository via the HTTP API.

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
import urllib.error
import urllib.parse
import urllib.request


DESC = 'Maintain labels in a GitHub repository'
GH_ACCEPT = 'application/vnd.github.v3+json'
GH_CONTENT_TYPE = 'application/json'
GPAT = 'GITHUB_PERSONAL_ACCESS_TOKEN'


def dump(**kwargs):
    for k, v in kwargs.items():
        print(f'{k}: {v}')


def dump_and_exit99(**kwargs):
    dump(**kwargs)
    sys.exit(99)


def read_labels(gh_owner=None, gh_repo=None):
    ''' Returns repository labels.

        :param gh_owner: owner or organization name for the repository.
        :param gh_repo: repository name.
        :return: repository labels.
        :rtype: dict
    '''
    req = urllib.request.Request(
        url=f'https://api.github.com/repos/{gh_owner}/{gh_repo}/labels',
        headers={'Authorization': f'token {os.getenv(GPAT)}',
                 'Accept': f'{GH_ACCEPT}'})

    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


def label_exists(gh_owner=None, gh_repo=None, label_name=None):
    ''' Returns True if the given label name is in the GitHub repository.

        :param gh_owner: owner or organization name for the repository.
        :param gh_repo: repository name.
        :param label_name: the label name.
        :return: True if the label exists.
    '''
    req = urllib.request.Request(
        url=f'https://api.github.com/repos/{gh_owner}/{gh_repo}/labels/{urllib.parse.quote(label_name)}',
        headers={'Authorization': f'token {os.getenv(GPAT)}'},
        method='HEAD')

    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status == 200
    except urllib.error.HTTPError:
        return False


def delete_label(gh_owner=None, gh_repo=None, label_name=None):
    ''' Deletes the named label.

        :param gh_owner: owner or organization name for the repository.
        :param gh_repo: repository name.
        :param label_name: the label name.
    '''
    req = urllib.request.Request(
        url=f'https://api.github.com/repos/{gh_owner}/{gh_repo}/labels/{urllib.parse.quote(label_name)}',
        headers={'Authorization': f'token {os.getenv(GPAT)}',
                 'Accept': f'{GH_ACCEPT}'},
        method='DELETE')
    with urllib.request.urlopen(req):
        pass


def save_label(gh_owner=None, gh_repo=None, label={}):
    ''' Returns the full label that was saved with id and url.

        :param gh_owner: owner or organization name for the repository.
        :param gh_repo: repository name.
        :param label_name: the label name.
        :return: True if the label exists.
    '''
    data = json.dumps(label, ensure_ascii=False).encode('gbk')
    req = urllib.request.Request(
        url=f'https://api.github.com/repos/{gh_owner}/{gh_repo}/labels',
        headers={
            'Authorization': f'token {os.getenv(GPAT)}', 'Content-Type': f'{GH_CONTENT_TYPE}'},
        data=data,
        method='POST')

    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


def update_label(gh_owner=None, gh_repo=None, label={}):
    ''' Returns the full label that was updated.

        :param gh_owner: owner or organization name for the repository.
        :param gh_repo: repository name.
        :param label: the label.
        :return: updated repository label dict.
        :rtype: dict
    '''

    data = json.dumps(label, ensure_ascii=False).encode('gbk')
    req = urllib.request.Request(
        url=f'https://api.github.com/repos/{gh_owner}/{gh_repo}/labels/{urllib.parse.quote(label["name"])}',
        headers={
            'Authorization': f'token {os.getenv(GPAT)}', 'Content-Type': f'{GH_CONTENT_TYPE}'},
        data=data,
        method='PATCH')

    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


def update_or_save_label(gh_owner=None, gh_repo=None, label={}):
    ''' Saves a new label or updates an existing label.

        :param gh_owner: owner or organization name for the repository.
        :param gh_repo: repository name.
        :param label: the label to save or update.
        :return: updated repository label dict.
        :rtype: dict
    '''
    if label_exists(gh_owner, gh_repo, label['name']):
        return update_label(gh_owner, gh_repo, label)
    else:
        return save_label(gh_owner, gh_repo, label)


def upload_labels(gh_owner=None, gh_repo=None, labels=[]):
    ''' Uploads one or many labels.

        :param gh_owner: owner or organization name for the repository.
        :param gh_repo: repository name.
        :param labels: the labels to upload.
    '''
    if isinstance(labels, list):
        for label in labels:
            update_or_save_label(gh_owner, gh_repo, label)
    elif isinstance(labels, dict):
        update_or_save_label(gh_owner, gh_repo, labels)


def delete_labels(gh_owner=None, gh_repo=None, labels=[]):
    ''' Deletes one or many labels.

        :param gh_owner: owner or organization name for the repository.
        :param gh_repo: repository name.
        :param labels: the label(s) to delete.
    '''
    if isinstance(labels, list):
        for label in labels:
            delete_label(gh_owner, gh_repo, label['name'])
    elif isinstance(labels, dict):
        delete_label(gh_owner, gh_repo, labels['name'])


if __name__ == '__main__':

    if not os.getenv(GPAT):
        print(f'Set environment variable {GPAT}', file=sys.stderr)
        sys.exit(1)

    parser = ArgumentParser(description=DESC,
                            formatter_class=RawDescriptionHelpFormatter)
    parser.add_argument('-d', '--debug',
                        action='store_true',
                        help='Turn on debug logging.')
    parser.add_argument('gh_owner', nargs=1,
                        metavar='OWNER',
                        help='Owner of the GitHub repository')
    parser.add_argument('gh_repo', nargs=1,
                        metavar='REPO',
                        help='Name of the GitHub repository')

    subparsers = parser.add_subparsers(dest='command_to_execute',
                                       description='GitHub label commands',
                                       help='Commands help')

    parser_d = subparsers.add_parser('delete',
                                     help='Delete GitHub labels from specified repository')
    parser_d.add_argument('infile', nargs='?', type=FileType('r'),
                          metavar='FILE',
                          default=sys.stdin,
                          help='Json file containing a list of GitHub label definitions (default is stdin)')

    parser_l = subparsers.add_parser('list',
                                     help='Lists all GitHub labels in the specified repository')
    parser_l.add_argument('outfile', nargs='?', type=FileType('w'),
                          metavar='FILE',
                          default=sys.stdout,
                          help='Output file to store labels Json')
    parser_l.add_argument('-I', '--prune-ids',
                          action='store_true',
                          help='Prune id fields values from output')
    parser_l.add_argument('-n', '--prune-null-values',
                          action='store_true',
                          help='Prune fields with null values from output')
    parser_l.add_argument('-u', '--prune-urls',
                          action='store_true',
                          help='Prune url fields from output')

    parser_r = subparsers.add_parser(
        'rename', help='Rename an existing label (not implemented)')
    parser_r.add_argument('old_name', nargs=1, metavar='OLD_NAME',
                          help='Current name of existing label')
    parser_r.add_argument('new_name', nargs=1,
                          metavar='NEW_NAME', help='New name for label')

    parser_u = subparsers.add_parser('upload',
                                     help='Upload or update list of GitHub labels to the specified repository')
    parser_u.add_argument('infile', nargs='?', type=FileType('r'),
                          metavar='FILE',
                          default=sys.stdin,
                          help='Json file containing a list of GitHub label definitions (default is stdin)')

    args = parser.parse_args()

    if args.debug:
        logging.basicConfig(format='%(levelname)s: %(message)s',
                            level=logging.DEBUG)

    err = None
    try:
        if args.command_to_execute == 'list':
            labels = read_labels(
                gh_owner=args.gh_owner[0], gh_repo=args.gh_repo[0])
            if args.prune_urls:
                labels = [{k: v for k, v in label.items() if k != 'url'}
                          for label in labels]
            if args.prune_null_values:
                labels = [{k: v for k, v in label.items() if v}
                          for label in labels]
            if args.prune_ids:
                labels = [{k: v for k, v in label.items() if k != 'id' and k != 'node_id'}
                          for label in labels]
            print(json.dumps(labels, sort_keys=True, indent=2), file=args.outfile)
        elif args.command_to_execute == 'upload':
            labels = json.loads(args.infile.read())
            upload_labels(
                args.gh_owner[0], args.gh_repo[0], labels)
        elif args.command_to_execute == 'delete':
            labels = json.loads(args.infile.read())
            delete_labels(args.gh_owner[0], args.gh_repo[0], labels)
        elif args.command_to_execute == 'rename':
            print('ERROR - rename not implemented!', file=sys.stderr)
            sys.exit(1)
    except (urllib.error.HTTPError, ValueError) as e:
        err = e
    finally:
        if err:
            if isinstance(err, urllib.error.HTTPError):
                if args.debug:
                    logging.debug(err.headers)
                print(
                    f'HTTPS call to GitHub failed! {err.code} - {err.msg}', file=sys.stderr)
            elif isinstance(err, ValueError):
                print(err, file=sys.stderr)
            sys.exit(1)
