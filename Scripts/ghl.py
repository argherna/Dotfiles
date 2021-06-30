'''
GitHub label utility.

Run ``python3 ghl.py -h`` for how to use.
'''


from argparse import ArgumentParser
from argparse import FileType
from argparse import RawDescriptionHelpFormatter
import json
import logging
import os
import sys
import traceback
import urllib.error
import urllib.parse
import urllib.request


DESC = 'Maintain labels in a GitHub repository'
GH_ACCEPT = 'application/vnd.github.v3+json'
GH_CONTENT_TYPE = 'application/json'
GH_PAT = 'GITHUB_PERSONAL_ACCESS_TOKEN'


def var_state(**kwargs):
    ''' Returns a list of variable names and values.

        Sample usage::

           >>> var_state(foo=1, bar='bar')
           ['foo: 1', 'bar: bar']
    '''
    return [f'{k}: {v}' for k, v in kwargs.items()]


def read_labels(owner=None, repo=None):
    ''' Returns repository labels.

        :param owner: owner or organization name for the repository.
        :param repo: repository name.
        :return: repository labels.
        :rtype: dict
    '''
    req = urllib.request.Request(
        url=f'https://api.github.com/repos/{owner}/{repo}/labels',
        headers={'Authorization': f'token {os.getenv(GH_PAT)}',
                 'Accept': f'{GH_ACCEPT}'})

    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


def get_label(owner=None, repo=None, label_name=None):
    ''' Returns True if the given label name is in the GitHub repository.

        :param owner: owner or organization name for the repository.
        :param repo: repository name.
        :param label_name: the label name.
        :return: True if the label exists.
    '''
    req = urllib.request.Request(
        url=f'https://api.github.com/repos/{owner}/{repo}/labels/{urllib.parse.quote(label_name)}',
        headers={'Authorization': f'token {os.getenv(GH_PAT)}',
                 'Accept': f'{GH_ACCEPT}'},
        method='HEAD')

    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


def delete_label(owner=None, repo=None, label_name=None):
    ''' Deletes the named label.

        :param owner: owner or organization name for the repository.
        :param repo: repository name.
        :param label_name: the label name.
    '''
    req = urllib.request.Request(
        url=f'https://api.github.com/repos/{owner}/{repo}/labels/{urllib.parse.quote(label_name)}',
        headers={'Authorization': f'token {os.getenv(GH_PAT)}',
                 'Accept': f'{GH_ACCEPT}'},
        method='DELETE')
    with urllib.request.urlopen(req):
        pass


def save_label(owner=None, repo=None, label={}):
    ''' Returns the full label that was saved with id and url.

        :param owner: owner or organization name for the repository.
        :param repo: repository name.
        :param label_name: the label name.
        :return: True if the label exists.
    '''
    data = json.dumps(label, ensure_ascii=False).encode('gbk')
    req = urllib.request.Request(
        url=f'https://api.github.com/repos/{owner}/{repo}/labels',
        headers={
            'Authorization': f'token {os.getenv(GH_PAT)}',
            'Accept': f'{GH_ACCEPT}',
            'Content-Type': f'{GH_CONTENT_TYPE}'},
        data=data,
        method='POST')

    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


def update_label(owner=None, repo=None, label={}):
    ''' Returns the full label that was updated.

        :param owner: owner or organization name for the repository.
        :param repo: repository name.
        :param label: the label.
        :return: updated repository label dict.
        :rtype: dict
    '''

    data = json.dumps(label, ensure_ascii=False).encode('gbk')
    req = urllib.request.Request(
        url=f'https://api.github.com/repos/{owner}/{repo}/labels/{urllib.parse.quote(label["name"])}',
        headers={
            'Authorization': f'token {os.getenv(GH_PAT)}',
            'Accept': f'{GH_ACCEPT}',
            'Content-Type': f'{GH_CONTENT_TYPE}'},
        data=data,
        method='PATCH')

    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


if __name__ == '__main__':

    httpclient_logger = logging.getLogger("http.client")

    def do_http_client_logging_setup(level=logging.DEBUG):
        ''' Turn on debugging of the HTTP client used by urllib.

            See `Python3 urllib.request debug (request and response details)<https://gist.github.com/maczniak/db34452555f33805302d2c5557167164>`_ and
            `this StackOverflow.com <https://stackoverflow.com/a/16337639/37776>`_ answer for details.

            :param level: logging level to use (DEBUG is default)
        '''
        def httpclient_log(*args):
            ''' Masks the print() built-in in the http.client module
                to use logging instead.
            '''
            httpclient_logger.log(level, " ".join(args))

        import http.client
        http.client.print = httpclient_log
        http_handler = urllib.request.HTTPHandler(debuglevel=1)
        try:
            import ssl
            https_handler = urllib.request.HTTPSHandler(debuglevel=1)
            opener = urllib.request.build_opener(http_handler, https_handler)
        except ImportError:
            opener = urllib.request.build_opener(http_handler)
        urllib.request.install_opener(opener)

    def label_exists(owner=None, repo=None, label_name=None):
        ''' Returns True if the given label name is in the GitHub repository.

            :param owner: owner or organization name for the repository.
            :param repo: repository name.
            :param label_name: the label name.
            :return: True if the label exists.
        '''
        try:
            just_checking = get_label(
                owner=owner, repo=repo, label_name=label_name)
            return True
        except urllib.error.HTTPError:
            return False

    def update_or_save_label(owner=None, repo=None, label={}):
        ''' Saves a new label or updates an existing label.

            :param owner: owner or organization name for the repository.
            :param repo: repository name.
            :param label: the label to save or update.
            :return: updated repository label dict.
            :rtype: dict
        '''
        if label_exists(owner, repo, label['name']):
            return update_label(owner, repo, label)
        else:
            return save_label(owner, repo, label)

    def do_delete_label(args):
        ''' Command line exclusive delete_labels. Executes
            the delete_label function.

            :param args: Namespace containing the parsed
                         command line arguments.
        '''
        labels = json.loads(args.infile.read())
        if isinstance(labels, list):
            for label in labels:
                delete_label(args.gh_owner[0], args.gh_repo[0], label['name'])
        elif isinstance(labels, dict):
            delete_label(args.gh_owner[0], args.gh_repo[0], labels['name'])
        return {}

    def do_read_labels(args):
        ''' Command line exclusive read_labels. Executes
            the read_labels function.

            :param args: Namespace containing the parsed
                         command line arguments.
        '''
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
        return {}

    def do_upload_labels(args):
        ''' Command line exclusive function for uploading labels.
            Executes functions that update or save label data to
            GitHub.

            :param args: Namespace containing the parsed
                         command line arguments.
        '''
        labels = json.loads(args.infile.read())
        if args.debug:
            logging.debug(' '.join(
                ['send:', json.dumps(labels, indent=2, sort_keys=True)]))

        if isinstance(labels, list):
            for label in labels:
                update_or_save_label(args.gh_owner[0], args.gh_repo[0], label)
        elif isinstance(labels, dict):
            update_or_save_label(args.gh_owner[0], args.gh_repo[0], labels)

    if not os.getenv(GH_PAT):
        print(f'Set environment variable {GH_PAT}', file=sys.stderr)
        sys.exit(1)

    common_group = ArgumentParser(
        formatter_class=RawDescriptionHelpFormatter, add_help=False)
    common_group.add_argument('-d', '--debug', action='store_true',
                              help='Turn on debug logging')
    common_group.add_argument('gh_owner', nargs=1,
                              metavar='OWNER',
                              help='Owner of the GitHub repository')
    common_group.add_argument('gh_repo', nargs=1,
                              metavar='REPO',
                              help='Name of the GitHub repository')
    infile_group = ArgumentParser(
        formatter_class=RawDescriptionHelpFormatter, add_help=False)
    infile_group.add_argument('infile', nargs='?', type=FileType('r'),
                              metavar='FILE',
                              default=sys.stdin,
                              help='Json file containing a list of GitHub label definitions (default is stdin)')

    parser = ArgumentParser(description=DESC,
                            formatter_class=RawDescriptionHelpFormatter)

    subparsers = parser.add_subparsers(metavar='Command',
                                       description='GitHub label commands',
                                       help='Description')

    parser_d = subparsers.add_parser('delete',
                                     help='Delete GitHub labels from specified repository',
                                     parents=[common_group, infile_group])
    parser_d.set_defaults(func=do_delete_label)

    parser_l = subparsers.add_parser('list',
                                     help='Lists all GitHub labels in the specified repository',
                                     parents=[common_group])
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
    parser_l.set_defaults(func=do_read_labels)

    parser_u = subparsers.add_parser('upload',
                                     help='Upload or update list of GitHub labels to the specified repository',
                                     parents=[common_group, infile_group])
    parser_u.set_defaults(func=do_upload_labels)

    args = parser.parse_args()

    try:
        if args.debug:
            logging.basicConfig(format='%(levelname)s: %(message)s',
                                level=logging.DEBUG)
            do_http_client_logging_setup()
    except AttributeError:
        parser.print_usage()
        sys.exit(1)

    err = None
    try:
        logging.debug(var_state(**vars(args)))
        resp = args.func(args)
        if resp:
            logging.debug(json.dumps(resp, sort_keys=True, indent=2))
    except AttributeError as e:
        print('Need to specify a command', file=sys.stderr)
        err = e
    except urllib.error.HTTPError as e:
        print(f'{e.code} - {e.msg}', file=sys.stderr)
        print(
            f'{json.dumps(e.read().decode("utf-8"), indent=2, sort_keys=True, ensure_ascii=False)}',
            file=sys.stderr)
        err = e
    except ValueError as e:
        print(e, file=sys.stderr)
        err = e
    finally:
        if err:
            if args.debug:
                traceback.print_exception(type(err), err, err.__traceback__)
            sys.exit(1)
