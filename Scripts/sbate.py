#!/usr/bin/env python

'''NAME
----
    sbate.py

SYNOPSIS
--------
    sbate.py [OPTIONS] FILENAME

OPTIONS
-------
    -c CONFIG_FILE, --config CONFIG_FILE
        Read configuration from a file (default is `~/.sbate.conf`)

    -H HOST, --host HOST
        Name of the host running the SMTP server (default localhost).

    -p PORT, --port PORT
        Port number the SMTP server is listening on (default 8025).

    -u USERNAME, --username USERNAME
        Username with privileges to send email from the SMTP server
        (default dummy).

    -W PASSWORD, --password PASSWORD
        Password for the user (default dummy). Use with caution.

    -S, --suppress-login
        Suppresses login to the SMTP server (useful during testing).

    -s 'SUBJECT_TEXT', --subject 'SUBJECT_TEXT'
        Subject text to use for the email message (default 'message 
        subject').

    -f FROMADDR, --from FROMADDR
        Email address of the sender (default sender@example.com).

    -t TOADDR, --to TOADDR
        Email address of the receiver (default receiver@example.com).

    -m 'MESSAGE_TEXT', --message 'MESSAGE_TEXT'
        The message to send (default 'Hello from sbate.py!').

PARAMETERS
----------
    FILENAME
        Required filename to attach to the email sent by this program.

DESCRIPTION
-----------
    Sends a Binary file ATtachment in an Email (sbate).

    The setting of the options described above have precedence. If they are
    supplied on the command line, they will be used.

    Users can put a file named `.sbate.conf` in their home directory to supply
    values that are used often. The program will use the values in this file if
    they are supplied.

    The `.sbate.conf` file may written as follows::

        #
        # Sample ~/.sbate.conf
        #
        # Include values that have overrides you use all the time. Any equivalent
        # command line argument will override these settings.
        #
        
        # SMTP server settings
        #
        [smtp]
        host = localhost
        port = 8025
        username = null
        password = null
        suppress_login = False
        
        # Message settings
        #
        [message]
        subject = test subject
        from = sender@example.com
        to = receiver@example.com
        message = Example! Message body goes here.

    Finally, if no values are supplied then the following defaults mentioned in
    the options section are used.

EXIT STATUS
-----------
    0
        attachement was emailed successfully.

    1
        emailing attachment failed (error message printed).

    2
        filename argument wasn't specified.

'''

from argparse import ArgumentParser
from argparse import FileType
from configparser import ConfigParser
from email.mime.base import MIMEBase
from email.mime.multipart import MIMEMultipart
import email
import logging
import os
import smtplib
import socket
import sys
import traceback

DESC = 'Send Binary ATtachment in an Email (sbate)'

CONF_FILENAME = '~/.sbate.conf'
HOST = 'localhost'
PORT = 8025
USERNAME = 'dummy'
PASSWORD = 'dummy'

SUBJECT = 'message subject'
FROM_ADDR = 'sender@example.com'
TO_ADDR = 'receiver@example.com'
MSG_TXT = 'Hello from sbate.py!'


def var_state(**kwargs):
    ''' Returns a list of variable names and values.

        Sample usage::

           >>> var_state(foo=1, bar='bar')
           ['foo: 1', 'bar: bar']
    '''
    return [f'{k}: {v}' for k, v in kwargs.items()]


if __name__ == '__main__':

    parser = ArgumentParser(description=DESC)
    parser.add_argument('-d', '--debug', action='store_true',
                        help='Turn on debug logging')
    parser.add_argument('-H', '--host', nargs='?',
                        metavar='HOSTNAME', default=HOST,
                        help='Name of the host running the SMTP server')
    parser.add_argument('-p', '--port', nargs='?',
                        metavar='N', default=PORT, type=int,
                        help='Port number the SMTP server is listening on')
    parser.add_argument('-u', '--username', nargs='?',
                        metavar='username', default=USERNAME,
                        help='Username with privileges to send email from the SMTP server')
    parser.add_argument('-W', '--password', nargs='?',
                        metavar='PASSWORD', default=PASSWORD,
                        help='Password for the user')
    parser.add_argument('-s', '--subject', nargs='?',
                        metavar='SUBJECT_TEXT', default=SUBJECT,
                        help='Subject text to use for the email message')
    parser.add_argument('-f', '--from', nargs='?', metavar='FROM_ADDR',
                        dest='from_addr', default=FROM_ADDR,
                        help='Email address of the sender')
    parser.add_argument('-t', '--to', nargs='?', metavar='TO_ADDR',
                        dest='to_addr', default=TO_ADDR,
                        help='Email address of the receiver')
    parser.add_argument('-m', '--message', nargs='?',
                        metavar='MESSAGE_TEXT', default=MSG_TXT,
                        help='The message to send')
    parser.add_argument('-c', '--config', nargs=1,
                        metavar='FILENAME', default=CONF_FILENAME,
                        help='Read configuration from a file')
    parser.add_argument('-S', '--suppress-login', action='store_true',
                        help='Suppress login to SMTP server')
    parser.add_argument('filename', metavar='FILENAME',
                        help='Name of the file to attach to the email sent by this program')

    args = parser.parse_args()

    try:
        if args.debug:
            logging.basicConfig(format='%(levelname)s: %(message)s',
                                level=logging.DEBUG)
    except AttributeError:
        parser.print_usage()
        sys.exit(1)

    host = args.host
    port = args.port
    username = args.username
    password = args.password
    subject = args.subject
    from_addr = args.from_addr
    to_addr = args.to_addr
    message = args.message
    config_file = args.config
    suppress_login = args.suppress_login
    filename = args.filename

    try:
        if args.debug:
            logging.basicConfig(format='%(levelname)s: %(message)s',
                                level=logging.DEBUG)
    except AttributeError:
        parser.print_usage()
        sys.exit(1)

    if (config_file != CONF_FILENAME) and isinstance(config_file, list):
        config_file = config_file[0]

    if config_file.startswith('~'):
        config_file = os.path.expanduser(config_file)

    if os.path.exists(config_file):
        config = ConfigParser()
        config.readfp(open(config_file))

        if config.has_section('smtp'):

            if (host == HOST) and config.has_option('smtp', 'host'):
                host = config.get('smtp', 'host')

            if (port == PORT) and config.has_option('smtp', 'port'):
                port = config.getint('smtp', 'port')

            if (username == USERNAME) and config.has_option('smtp', 'username'):
                username = config.get('smtp', 'username')

            if (password == PASSWORD) and config.has_option('smtp', 'password'):
                password = config.get('smtp', 'password')

            if not suppress_login and config.has_option('smtp', 'suppress_login'):
                suppress_login = config.getboolean('smtp', 'suppress_login')

        if config.has_section('message'):

            if (subject == SUBJECT) and config.has_option('message', 'subject'):
                subject = config.get('message', 'subject')

            if (from_addr == FROM_ADDR) and config.has_option('message', 'from'):
                from_addr = config.get('message', 'from')

            if (to_addr == TO_ADDR) and config.has_option('message', 'to'):
                to_addr = config.get('message', 'to')

            if (message == MSG_TXT) and config.has_option('message', 'message'):
                message = config.get('message', 'message')

    logging.debug(var_state(host=host, port=port, username=username,
                            password='** REDACTED **', subject=subject,
                            from_addr=from_addr, to_addr=to_addr,
                            message=message,
                            suppress_login=suppress_login, filename=filename))
    err = None
    try:
        msg = MIMEMultipart()
        msg['Subject'] = subject
        msg['From'] = from_addr
        msg['To'] = to_addr
        msg.preamble = message

        part = MIMEBase('application', 'octet-stream')
        part.set_payload(open(filename, 'rb').read())
        email.encoders.encode_base64(part)
        part.add_header('Content-Disposition', 'attachment; filename="%s"' %
                        os.path.basename(filename))
        msg.attach(part)

        smtp = smtplib.SMTP(host, port)
        if args.debug:
            smtp.set_debuglevel(1)
        if not suppress_login:
            smtp.login(username, password)
        smtp.sendmail(from_addr, [to_addr], msg.as_string())
        smtp.quit()
    except socket.gaierror as e:
        print(f'Can\'t find SMTP host {HOST}')
        err = e
    except IOError as e:
        print(f'Can\'t find file {filename}')
        err = e
    finally:
        if err:
            if args.debug:
                traceback.print_exception(type(err), err, err.__traceback__)
            sys.exit(1)
