#!/usr/bin/env python

'''log2csv.py

Converts an Apache access log into a tab-delimited CSV file. The output file
will have the same name as the input file with the '.csv' extension in place of
the original.

Note: the regular expression used to match the lines in the log file was
borrowed from the loghetti project <URL:https://github.com/bkjones/loghetti>.
The Apache access log format is documented at

<URL:http://httpd.apache.org/docs/2.0/logs.html#accesslog>

'''

from optparse import OptionParser

import csv
import fileinput
import os.path as path
import re
import sys

__usage__ = 'Usage: %prog [options] [access_log_file [output_path]]'
__desc__ = '''Converts Apache-style access logs to CSV file.

See <URL:http://httpd.apache.org/docs/2.0/logs.html#accesslog> for the log
format.
'''
__epilog__ = '''Note: the access_log_file is an absolute path to a log file.
output_path is only read if access_log_file is specified and should be a
writeable directory.

If access_log_file is not specified or is '-', sys.stdin is read.
If output_path is not specified, output is sent to sys.stdout.

'''


class InputThingError(Exception):
    pass


class InputThing(object):
    ''' Wraps a context manager around a fileinput. '''

    def __init__(self, fname):
        self.fname = fname
        self.old_fileinput = fileinput.input(self.fname)

    def __getitem__(self, index):
        return self.old_fileinput[index]

    def __enter__(self):
        if self.fname != '-' and not path.exists(self.fname):
            raise InputThingError('Logfile %s not found!' % (self.fname))
        return self

    def __exit__(self, exctype, excinst, exctb):
        if exctype == IOError:
            raise InputThingError('Could not read from file %s!' %
                                  (self.fname))
        self.old_fileinput.close()

    def __repr__(self):
        return '%s(%r)' % (self.__class__.__name__, self.fname)


class OutputThingError(Exception):
    pass


class OutputThing(object):
    '''Wraps a context manager around a csv writer.'''

    def __init__(self, **kwargs):
        self.filename = kwargs['filename']
        self.delimiter = kwargs['delimiter']
        self.out_file = None
        self.csv_writer = None

    def __enter__(self):
        if self.filename is None:
            self.out_file = sys.stdout
        else:
            self.out_file = open(self.filename, 'wb')

        if self.delimiter is None:
            self.delimiter = '\t'

        self.csv_writer = csv.writer(self.out_file, delimiter=self.delimiter)
        return self

    def __exit__(self, exctype, exinst, exctb):
        if exctype == IOError:
            raise OutputThingError('%s: \'%s\'' % (message, outfilename))
        else:
            self.out_file.close()

    def write_line(self, token_list):
        if self.csv_writer is None:
            raise OutputThingError('OutputThing is not initialized!')
        self.csv_writer.writerow(token_list)


def main():
    parser = OptionParser(usage=__usage__,
                          description=__desc__,
                          epilog=__epilog__)
    parser.add_option('-s',
                      '--suppress-header-row',
                      action='store_false',
                      dest='write_header_row',
                      help='Do not print the header row to the CSV file.',
                      default=True)
    parser.add_option('-d',
                      '--delimiter',
                      dest='delimiter',
                      metavar='DELIMITER',
                      help='DELIMITER delimits the columns of the csv file '
                           '(default is TAB).',
                      default='\t')
    (opts, args) = parser.parse_args()

    logfilename = '-'
    outfilename = None

    if len(args) > 0:
        logfilename = args[0]

    if len(args) > 1:
        output_path = args[1]
        outfilename = ''.join([path.join(output_path,
                                         path.splitext(
                                             path.basename(logfilename))[0]),
                               '.csv'])

    try:
        with OutputThing(filename=outfilename,
                         delimiter=opts.delimiter) as csv_out, \
             InputThing(logfilename) as log_in:
            if opts.write_header_row:
                csv_out.write_line(['ip_address',
                                    'identd',
                                    'HTTP_user',
                                    'Request_time',
                                    'HTTP_Method',
                                    'Request_URL',
                                    'HTTP_Version',
                                    'HTTP_ResponseCode',
                                    'HTTP_Response_Size',
                                    'HTTP_Referrer',
                                    'User_Agent'])

            lineRegex = re.compile(r'''(\d+\.\d+\.\d+\.\d+)\s # the ip address
                                   ([^ ]*)\s              # identd
                                   ([^ ]*)\s              # HTTP user
                                   \[([^\]]*)\]\s         # time of the request
                                   "([^"]*)"\s            # request line
                                   (\d+)\s                # HTTP response code
                                   ([^ ]*)\s              # HTTP response size
                                   "([^"]*)"\s            # HTTP Referrer
                                   "([^"]*)"              # User-Agent''',
                                   re.VERBOSE)

            for line in log_in:
                m = lineRegex.match(line)
                if m:
                    (ip_address, identd, http_user, request_time,
                     request_line, http_response_code, http_response_size,
                     referrer, user_agent) = m.groups()
                    try:
                        (http_method, request_url,
                         http_vers) = request_line.split()
                        csv_out.write_line([ip_address,
                                            identd,
                                            http_user,
                                            request_time,
                                            http_method,
                                            request_url,
                                            http_vers,
                                            http_response_code,
                                            http_response_size,
                                            referrer,
                                            user_agent])
                    except ValueError:
                        sys.stderr.write("Skipping request line %s\n" %
                                         (request_line))
    except InputThingError, (message):
        sys.stderr.write("%s Exiting!\n" % (message))
        return 1
    except OutputThingError, (message):
        sys.stderr.write("%s Exiting!\n" % (message))
        return 1
    except KeyboardInterrupt:
        pass

    return 0

if __name__ == '__main__':
    sys.exit(main())
