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

    -n, --nologin
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
from ConfigParser import ConfigParser
from email import Encoders
from email.MIMEBase import MIMEBase
from email.MIMEMultipart import MIMEMultipart
import os
import smtplib
import socket
import sys

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

if __name__ == '__main__':

    arg_parser = ArgumentParser(description=DESC)
    arg_parser.add_argument(
        '-H', '--host', metavar='hostname', default=HOST, nargs=1, help='Name of the host running the SMTP server')
    arg_parser.add_argument('-p', '--port', metavar='n', default=PORT, nargs=1, type=int,
                            help='Port number the SMTP server is listening on')
    arg_parser.add_argument(
        '-u', '--username', metavar='username', default=USERNAME, nargs=1, help='Username with privileges to send email from the SMTP server')
    arg_parser.add_argument(
        '-W', '--password', metavar='password', default=PASSWORD, nargs=1, help='Password for the user')
    arg_parser.add_argument('-s', '--subject', metavar='subject_text', default=SUBJECT, nargs=1,
                            help='Subject text to use for the email message')
    arg_parser.add_argument('-f', '--from', dest='from_addr', metavar='from_addr',
                            default=FROM_ADDR, nargs=1, help='Email address of the sender')
    arg_parser.add_argument('-t', '--to', dest='to_addr', metavar='to_addr',
                            default=TO_ADDR, nargs=1, help='Email address of the receiver')
    arg_parser.add_argument('-m', '--message', metavar='message_text',
                            default=MSG_TXT, nargs=1, help='The message to send')
    arg_parser.add_argument('-c', '--config', metavar='filename',
                            default=CONF_FILENAME, nargs=1, help='Read configuration from a file')
    arg_parser.add_argument('-n', '--nologin', dest='no_login',
                            action='store_true', help='Suppress login to SMTP server')
    arg_parser.add_argument('filename', metavar='FILENAME',
                            help='Name of the file to attach to the email sent by this program')

    cli_args = arg_parser.parse_args()

    host = cli_args.host
    port = cli_args.port
    username = cli_args.username
    password = cli_args.password
    subject = cli_args.subject
    from_addr = cli_args.from_addr
    to_addr = cli_args.to_addr
    message = cli_args.message
    config_file = cli_args.config
    no_login = cli_args.no_login
    filename = cli_args.filename

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

            if not no_login and config.has_option('smtp', 'suppress_login'):
                no_login = config.getboolean('smtp', 'suppress_login')

        if config.has_section('message'):

            if (subject == SUBJECT) and config.has_option('message', 'subject'):
                subject = config.get('message', 'subject')

            if (from_addr == FROM_ADDR) and config.has_option('message', 'from'):
                from_addr = config.get('message', 'from')

            if (to_addr == TO_ADDR) and config.has_option('message', 'to'):
                to_addr = config.get('message', 'to')

            if (message == MSG_TXT) and config.has_option('message', 'message'):
                message = config.get('message', 'message')

    try:
        msg = MIMEMultipart()
        msg['Subject'] = subject
        msg['From'] = from_addr
        msg['To'] = to_addr
        msg.preamble = message

        part = MIMEBase('application', 'octet-stream')
        part.set_payload(open(filename, 'rb').read())
        Encoders.encode_base64(part)
        part.add_header('Content-Disposition', 'attachment; filename="%s"' %
                        os.path.basename(filename))
        msg.attach(part)

        smtp = smtplib.SMTP(host, port)
        if not no_login:
            smtp.login(username, password)
        smtp.sendmail(from_addr, [to_addr], msg.as_string())
        smtp.quit()
    except socket.gaierror:
        print "Can't find SMTP host %s" % HOST
        sys.exit(1)
    except IOError:
        print "Can't find file %s" % filename
        sys.exit(1)
