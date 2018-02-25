#!/usr/bin/env python


'''\
NAME
----

    ghif.py

SYNOPSIS
--------

    ghif.py <options> <args>

DESCRIPTION
-----------

    Convert json output from ghi.py into a report-style format. Output
    will be converted to Google Spreadsheet json by default. By setting
    an option, output can be formatted to a small screen table.

    Typical usage scenario would be as follows:

        python ghi.py -a USER REPO_OWNER REPO_NAME | python ghif.py | python ugs.py

    That is, pipe intput into this program from ghi.py specifying no
    options. Then pipe the output into ugs.py for uploading into Google
    Sheets.
'''


from argparse import ArgumentParser
from argparse import FileType
from argparse import RawDescriptionHelpFormatter
import json
import logging
import sys


DESC = 'Formats GitHub Issues json to report-style output'


def gh_issues_to_report_issues(gh_issues):
    '''Convert GitHub Issues json to something more flat with the data we
       care about.'''
    rpt_issues = []
    for issue in gh_issues:
        rpt_issues.append({'number': issue['number'],
                           'title': issue['title'],
                           'user_login': issue['user']['login'],
                           'assignee_login': issue['assignee']['login'],
                           'created_at': issue['created_at'],
                           'updated_at': issue['updated_at'],
                           'closed_at': issue['closed_at'],
                           'state': issue['state'],
                           'html_url': issue['html_url'],
                           'labels': issue['labels'],
                           'repo_url': issue['repository_url']})

    return rpt_issues


def check_assignee(rpt_issue):
    '''Sets the assignee field to UNASSIGNED if no one is assigned to this
       issue.'''
    assignee = rpt_issue['assignee_login']
    if not assignee:
        assignee = 'UNASSIGNED'

    return assignee


def format_to_google_sheets(rpt_issues, outfile):

    repo_name = rpt_issues[0]['repo_url'].rsplit('/', 1)[-1]

    # Final row is 1 empty for the 'spacer' row, 1 for the repository name, and
    # 1 for the header row.
    range = '%s!B2:I%d' % (repo_name, len(rpt_issues) + 3)
    ss = {'range': range,
          'majorDimension': 'ROWS',
          'values': []}
    values = ss['values']

    values.append([repo_name])
    values.append(['Number', 'User', 'Assignee', 'Title', 'State',
                   'Created At', 'Updated At', 'Closed At'])
    for rpt_issue in rpt_issues:
        assignee = check_assignee(rpt_issue)
        values.append(['=HYPERLINK("%s", "%s")' % (rpt_issue['html_url'],
                                                   rpt_issue['number']),
                       rpt_issue['user_login'],
                       assignee,
                       rpt_issue['title'],
                       rpt_issue['state'],
                       rpt_issue['created_at'],
                       rpt_issue['updated_at'],
                       rpt_issue['closed_at']])

    print >>outfile, json.dumps(ss, sort_keys=True, indent=2)


def format_to_screen(rpt_issues, outfile):
    '''Prints a table-style report of the issues.'''
    headers = ['Number', 'User', 'Assignee', 'Title', 'State', 'Created At',
               'Updated At', 'Closed At']

    headers_fmt = '{:<8}{:<10}{:<10}{:<15} {:<5}{:>21}{:>21}'
    print headers_fmt.format(*headers)
    row_fmt = '{:>6}  {:<10}{:<10}{:<15} {:<5}{:>21}{:>21}'

    for rpt_issue in rpt_issues:

        # Make sure unassigned issues are called out in the report.
        assignee = check_assignee(rpt_issue)

        # Shorten rpt_issue titles to something that will fit on the screen.
        title = rpt_issue['title']
        if len(title) > 15:
            title = title[:12] + '...'

        print >>outfile, row_fmt.format(rpt_issue['number'], rpt_issue['user_login'], assignee,
                                        title, rpt_issue['state'], rpt_issue[
                                            'created_at'],
                                        rpt_issue['updated_at'])


if __name__ == '__main__':
    parser = ArgumentParser(description=DESC,
                            formatter_class=RawDescriptionHelpFormatter)
    parser.add_argument('--logging_level', nargs=1, type=str,
                        choices=['DEBUG', 'INFO',
                                 'WARNING', 'ERROR', 'CRITICAL'],
                        default='ERROR',
                        help='Set logging level (default is WARNING)')
    parser.add_argument('-s', '--screen',
                        action='store_true',
                        help='Format GitHub issues json to screen output only.')
    parser.add_argument('infile', nargs='?', type=FileType('r'),
                        metavar='FILE',
                        default=sys.stdin,
                        help='Json file containing a list of GitHub issues (default is stdin)')
    parser.add_argument('outfile', nargs='?', type=FileType('w'),
                        metavar='FILE',
                        default=sys.stdout,
                        help='Output file to store formatted issues (default is stdout)')

    cli_args = parser.parse_args()

    numeric_level = getattr(logging, cli_args.logging_level[0].upper(), None)
    logging.basicConfig(format='%(levelname)s: %(message)s',
                        level=numeric_level)

    if cli_args.screen:
        format_to_screen(gh_issues_to_report_issues(
            json.loads(cli_args.infile.read())), cli_args.outfile)
    else:
        format_to_google_sheets(gh_issues_to_report_issues(
            json.loads(cli_args.infile.read())), cli_args.outfile)
