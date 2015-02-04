#!/usr/bin/env python

"""
log2csv.py

Converts an Apache access log into a tab-delimited CSV file. The output file
will have the same name as the input file with the '.csv' extension in place of
the original.

Note: the regular expression used to match the lines in the log file was borrowed
from the loghetti project <URL:https://github.com/bkjones/loghetti>.  The Apache 
access log format is documented at 

<URL:http://httpd.apache.org/docs/2.0/logs.html#accesslog>
"""

from optparse import OptionParser

import csv
import fileinput
import os.path as path
import re
import sys

__usage__ = "Usage: %prog [options] [access_log_file [output_path]]"
__desc__ = """Converts Apache-style access logs to CSV file.

See <URL:http://httpd.apache.org/docs/2.0/logs.html#accesslog> for the log format.
"""
__epilog__ = """Note: the access_log_file is an absolute path to a log file.
output_path is only read if access_log_file is specified and should be a
writeable directory.

If access_log_file is not specified or is '-', sys.stdin is read.
If output_path is not specified, output is sent to sys.stdout.

"""

parser = OptionParser(usage=__usage__,
                      description=__desc__,
                      epilog=__epilog__)
parser.add_option("-s", 
                  "--suppress-header-row",
                  action="store_false",
                  dest="write_header_row",
                  help="If set, do not print the header row to the CSV file.",
                  default=True)
parser.add_option("-d", 
                  "--delimiter",
                  dest="delimiter",
                  metavar="DELIMITER",
                  help="DELIMITER used to delimit the columns of the csv file (default is TAB).",
                  default="\t")
(options, args) = parser.parse_args()

logfilename = "-"
outfile = sys.stdout

if len(args) > 0:

    logfilename = args[0]
    
    if not path.exists(logfilename):
        sys.stderr.write("Logfile '%s' not found, exiting.\n" % (logfilename))
        sys.exit(1)

    if len(args) > 1:
        output_path = args[1]
        outfilename = ''.join([path.join(output_path, path.splitext(path.basename(logfilename))[0]), ".csv"])
        try:
            outfile = open(outfilename, 'wb')
        except IOError, (errno, message):
            sys.stderr.write("%s: '%s', exiting.\n" % (message, outfilename))
            sys.exit(2)

f_in = fileinput.input(logfilename)
logWriter = csv.writer(outfile, delimiter=options.delimiter)

if options.write_header_row:
    logWriter.writerow(["ip_address", 
                        "identd", 
                        "HTTP_user", 
                        "Request_time", 
                        "HTTP_Method", 
                        "Request_URL", 
                        "HTTP_Version", 
                        "HTTP_ResponseCode", 
                        "HTTP_Response_Size", 
                        "HTTP_Referrer", 
                        "User_Agent"])

lineRegex = re.compile(r"""(\d+\.\d+\.\d+\.\d+)\s # the ip address 
                           ([^ ]*)\s              # identd
                           ([^ ]*)\s              # HTTP user
                           \[([^\]]*)\]\s         # time of the request
                           "([^"]*)"\s            # request line
                           (\d+)\s                # HTTP response code
                           ([^ ]*)\s              # HTTP response size
                           "([^"]*)"\s            # HTTP Referrer
                           "([^"]*)"              # User-Agent""",
                       re.VERBOSE)
try:
    for line in f_in:
        m = lineRegex.match(line)
        if m:
            ip_address, \
            identd, \
            http_user, \
            request_time, \
            request_line, \
            http_response_code, \
            http_response_size, \
            referrer, \
            user_agent = m.groups()
    
            try:
                http_method, request_url, http_vers = request_line.split()
                logWriter.writerow([ip_address, 
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
                sys.stderr.write("Skipping request line %s\n" % (request_line))

except IOError:
    sys.stderr.write("Could not read from file %s, exiting.\n" % (logfilename))
    sys.exit(1)

f_in.close()
sys.exit(0)
