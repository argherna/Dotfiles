import plistlib
import sys
import urllib.error
import urllib.request
import json
import http.client

DEFAULT_TIMEOUT = 3

# ANSI codes copied from <https://gist.github.com/rene-d/9e584a7dd2935d0f461904b9f2950007>
END = '\033[0m'
BLUE = "\033[0;34m"
BOLD = "\033[1m"
FAINT = "\033[2m"
GREEN = '\033[0;32m'
RED = '\033[0;31m'
YELLOW = "\033[1;33m"

CHECK = '\u2713'
RIGHT_ARROW = '\u2192'
WARNING = '\u26A0'
X = '\u2716'

BLUE_RIGHT_ARROW = f'{BLUE}{RIGHT_ARROW}{END}'
GREEN_CHECK = f'{GREEN}{CHECK}{END}'
RED_X = f'{RED}{X}{END}'
YELLOW_WARN = f'{YELLOW}{WARNING}{END}'


class NoRedirectHander(urllib.request.HTTPRedirectHandler):

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise urllib.error.HTTPError(
            url, code, 'Blocking auto-follow of redirects', headers, fp)


class GoogleChromeBookmarksIterator:

    def __init__(self, filename):
        with open(filename, 'r') as bookmark_file:
            data = json.load(bookmark_file)

        self.bookmarks = data['roots']

    def bms(self):
        for key, child in self.bookmarks.items():
            if 'children' in child.keys():
                yield from self._process_children(child['children'])

    def _process_children(self, children):
        for child in children:
            if child['type'] == 'url' and child['url'].startswith('http'):
                yield (child['url'], child['name'])
            if 'children' in child.keys():
                yield from self._process_children(child['children'])


class SafariBookmarksIterator:

    def __init__(self, filename):
        with open(filename, 'rb') as bookmark_file:
            plist = plistlib.load(bookmark_file)

        self.bookmarks = plist['Children']

    def bms(self):
        for bookmark in self.bookmarks:
            bookmark_keys = filter(
                lambda k: k == 'Children', bookmark.keys())
            for bookmark_key in bookmark_keys:
                for bkmk_data in bookmark[bookmark_key]:
                    yield (bkmk_data['URLString'],
                           bkmk_data['URIDictionary']['title'])


def connect_to(url, method='HEAD', headers={}, timeout=DEFAULT_TIMEOUT):
    req = urllib.request.Request(url=url, method=method, headers=headers)
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return (resp.status, resp.headers)


def handle_redirect(url, status, headers):
    loc = headers['Location']
    if loc:
        print(f'{YELLOW_WARN} {FAINT}{url}{END} {BLUE_RIGHT_ARROW} {loc}')
    else:
        print(f'{RED_X} {url} has {status} status with no new location')


def handle_success(url, status, headers):
    print(f'{GREEN_CHECK} {url}')


def handle_HTTPError(url, status, headers, err):
    if status in [301, 302, 303, 307, 308]:
        handle_redirect(url, status, headers)
    else:
        print(f'{RED_X} {BOLD}{err}{END} ({status}) {url}')
    return True


def handle_error(url, err):
    print(f'{RED_X} {BOLD}{err}{END} {url}')
    return True


if __name__ == '__main__':

    opener = urllib.request.build_opener(NoRedirectHander())
    opener.addheaders = [('User-agent', 'Mozilla/5.0 (Macintosh; '
                          'Intel Mac OS X 10.15; rv:78.0) '
                          'Gecko/20100101 Firefox/78.0')]
    urllib.request.install_opener(opener)

    try:
        chrome_bookmarks = GoogleChromeBookmarksIterator(
            '/Users/agherna/Library/Application Support/Google/Chrome'
            '/Profile 1/Bookmarks')
        bms = chrome_bookmarks.bms()
        for url, name in bms:

            status = 200
            headers = {}
            handled = False
            try:
                status, headers = connect_to(url)
            except urllib.error.HTTPError as e:
                try:
                    if e.code in [403, 405, 503]:
                        status, headers = connect_to(url, method='GET')
                    else:
                        handled = handle_HTTPError(url, e.code, e.headers, e)
                except urllib.error.HTTPError as e:
                    handled = handle_HTTPError(url, e.code, e.headers, e)
                except Exception as e:
                    handled = handle_error(url, e)
                else:
                    if not handled:
                        handle_success(url, status, headers)
            except Exception as e:
                handled = handle_error(url, e)
            else:
                if not handled:
                    handle_success(url, status, headers)
    except KeyboardInterrupt:
        print('Exiting...')
