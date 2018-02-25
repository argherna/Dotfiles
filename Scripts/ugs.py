#!/usr/bin/env python


'''\
NAME
----
  ugs.py

SYNOPSIS
--------
  ugs.py args

DESCRIPTION
-----------
  Uploads Google Spreadsheet json.
'''


from argparse import ArgumentParser
from argparse import FileType
from argparse import RawDescriptionHelpFormatter
from ConfigParser import ConfigParser
import httplib2
import json
import logging
import os
import sys
import time

from apiclient import discovery
from apiclient import errors
import oauth2client
from oauth2client import client
from oauth2client import tools


DESC = 'Upload spreadsheet json to Google Sheets'
EPILOG = ''

APPLICATION_NAME = ['gh2gs-dev']
CLIENT_SECRET_FILE = ['~/.client_secret.json']
CONF_FILENAME = ['~/.ugs.cfg']
CREDENTIAL_STORAGE_FILE = ['sheets.googleapis.com.json']
SCOPES = ['https://www.googleapis.com/auth/spreadsheets']


def get_credentials(scope, client_secret_file, credential_storage_file, application_name, flags=None):
    '''Gets valid user credentials from storage.

    If nothing has been stored, or if the stored credentials are invalid,
    the OAuth2 flow is completed to obtain the new credentials.

    Returns:
        Credentials, the obtained credential.
    '''
    home_dir = os.path.expanduser('~')
    credential_dir = os.path.join(home_dir, '.credentials')
    if not os.path.exists(credential_dir):
        os.makedirs(credential_dir)

    credential_path = os.path.join(credential_dir, credential_storage_file)
    store = oauth2client.file.Storage(credential_path)
    credentials = store.get()
    if not credentials or credentials.invalid:
        flow = client.flow_from_clientsecrets(client_secret_file, scope)
        flow.user_agent = application_name
        credentials = tools.run_flow(flow, store, flags)
        logging.info('Storing credentials to %s')

    return credentials


def spreadsheet_title():
    '''Returns a title for a new spreadsheet using the name of this script and
       the current timestamp in milliseconds.'''
    return '%s-%d' % ('ugs', int(round(time.time() * 1000)))


def create_spreadsheet(service, sheet_ttl, title=None):
    '''Creates a new Google Spreadsheet object. Returns the spreadsheetId.'''
    if not title:
        title = spreadsheet_title()

    ss_body = {'properties': {'title': title}}
    ss_body['sheets'] = [{'properties': {'title': sheet_ttl}}]
    new_ss = service.spreadsheets().create(body=ss_body).execute()
    logging.info('Created new Google Sheet %s', new_ss['spreadsheetId'])
    return new_ss['spreadsheetId']


def upload_to_sheets_main(credentials, spreadsheetId, infile):
    '''Converts the input GitHub issues json to Google Sheets.'''
    http = credentials.authorize(httplib2.Http())
    discoveryUrl = ('https://sheets.googleapis.com/$discovery/rest?'
                    'version=v4')
    service = discovery.build('sheets', 'v4', http=http,
                              discoveryServiceUrl=discoveryUrl)

    body = json.loads(infile.read())

    if not spreadsheetId:
        sheet_ttl = body['range'][:body['range'].index('!')]
        spreadsheetId = create_spreadsheet(service, sheet_ttl)
    result = service.spreadsheets().values().update(
        spreadsheetId=spreadsheetId, range=body['range'], body=body,
        valueInputOption='USER_ENTERED').execute()


if __name__ == '__main__':

    parser = ArgumentParser(parents=[tools.argparser], description=DESC, epilog=EPILOG,
                            formatter_class=RawDescriptionHelpFormatter)
    parser.add_argument('-a', '--application-name', nargs=1, type=str,
                        metavar='OAUTH2 APPLICATION NAME',
                        default=APPLICATION_NAME,
                        help='Set OAuth2 application name')
    parser.add_argument('-c', '--config', nargs=1, type=str,
                        metavar='FILENAME',
                        default=CONF_FILENAME,
                        help='Read configuration from a file')
    parser.add_argument('-f', '--client-secret-file', nargs=1, type=str,
                        metavar='FILENAME',
                        default=CLIENT_SECRET_FILE,
                        help='Google Apps client secret file')
    parser.add_argument('-i', '--spredsheetId', nargs=1, type=str,
                        metavar='ID',
                        help='Google Sheets spreadsheet Id')
    parser.add_argument('-o', '--scopes', nargs=1, type=str,
                        metavar='OAUTH2 SCOPES',
                        default=SCOPES,
                        help='Set OAuth2 scopes')
    parser.add_argument('-s', '--credential-storage-file', nargs=1, type=str,
                        metavar='FILENAME',
                        default=CREDENTIAL_STORAGE_FILE,
                        help='Filename under ~/.credentials to store Google credentails')
    parser.add_argument('infile', nargs='?', type=FileType('r'),
                        metavar='INFILE',
                        default=sys.stdin,
                        help='Json file containing a list of GitHub issues (default is stdin)')

    cli_args = parser.parse_args()

    numeric_level = getattr(logging, cli_args.logging_level.upper(), None)
    logging.basicConfig(format='%(levelname)s: %(message)s',
                        level=numeric_level)

    client_secret_file = cli_args.client_secret_file[0]
    if client_secret_file.startswith('~'):
        client_secret_file = os.path.expanduser(client_secret_file)

    credential_storage_file = cli_args.credential_storage_file[0]
    application_name = cli_args.application_name[0]
    scopes = cli_args.scopes[0]
    spreadsheetId = None
    if getattr(cli_args, 'spreadsheetId', None):
        spreadsheetId = cli_args.spreadsheetId[0]

    # Configure if available. Command-line args override everything unless
    # they are the default values.
    config_file = None
    if cli_args.config and cli_args.config[0].startswith('~'):
        config_file = os.path.expanduser(cli_args.config[0])

    if os.path.exists(config_file):
        config = ConfigParser()
        config.readfp(open(config_file))

        if config.has_section('google_auth'):

            if application_name == APPLICATION_NAME and config.has_option('google_auth', 'application_name'):
                application_name = config.get(
                    'google_auth', 'application_name')

            if client_secret_file == CLIENT_SECRET_FILE and config.has_option('google_auth', 'client_secret_file'):
                client_secret_file = config.get(
                    'google_auth', 'client_secret_file')

            if credential_storage_file == CREDENTIAL_STORAGE_FILE and config.has_option('google_auth', 'credential_storage_file'):
                credential_storage_file = config.get(
                    'google_auth', 'credential_storage_file')

            if scopes == SCOPES and config.has_option('google_auth', 'scopes'):
                scopes = config.get('google_auth', 'scopes')

        if config.has_section('google_sheets'):

            if not spreadsheetId and config.has_option('google_sheets', 'spreadsheetId'):
                spreadsheetId = config.get('google_sheets', 'spreadsheetId')

    # Have to pass cli_args as the 'flags' parameter.
    credentials = get_credentials(
        scopes, client_secret_file, credential_storage_file, application_name, cli_args)
    upload_to_sheets_main(credentials, spreadsheetId, cli_args.infile)
