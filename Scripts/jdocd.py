#!/usr/bin/env python

'''NAME
----
    jdocd.py

SYNOPSIS
--------
    jdocd.py [OPTIONS]

OPTIONS
-------
    -p n, --port n
        Start http server on port n (default 8080).

DESCRIPTION
-----------
    Serves javadoc directly from javadoc jar files in local Maven repository.

ACKNOWLEDGEMENTS
----------------
    This tool was heavily inspired (and copied) by `this gist
    <https://gist.github.com/mgodave/5406999>`.

'''
from argparse import ArgumentParser
from argparse import RawDescriptionHelpFormatter
from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
from mimetypes import types_map
from string import join
from zipfile import ZipFile
import sys
import traceback
import os
import re

DESC = 'Serve javadoc over HTTP from the local Maven repository'
EPILOG = '''\
If you don't see javadoc for an artifact you expect to see, run these
commands from the base directory of your Maven project:

    mvn dependency:sources
    mvn dependency:resolve -Dclassifier=javadoc
'''
PORT = 8080
OTHER_TYPES = {'.svg':'image/svg+xml',
               '.woff': 'application/x-font-woff',
               '.eot': 'application/vnd.ms-fontobject',
               '.ttf': 'application/octet-stream',
               '.otf': 'application/octet-stream'}
REPO_TYPES = ['jdk', 'm2']

class MavenRepositoryArtifactPath(object):
    '''Abstraction of path to an artifact in the local Maven repository.'''

    def __init__(self, coordinates, basedir='~/.m2/repository'):
        if basedir.startswith('~'):
            self.basedir = os.path.expanduser(basedir)
        else:
            self.basedir = basedir
        self.group_path = coordinates['group'].replace('.', '/')
        self.artifact_id = coordinates['artifact']
        self.version = coordinates['version']
        self.artifact_name = '-'.join((coordinates['artifact'],
                                       coordinates['version'],
                                       'javadoc.jar'))

    def __call__(self):
        return '/'.join((self.basedir, self.group_path,
                         self.artifact_id,
                         self.version,
                         self.artifact_name))


class ZippedJavadocContent(object):
    '''Reads content from the jar (zip) file'''

    def __init__(self, path_to_jar):
        self.jar_file = ZipFile(path_to_jar)

    def __enter__(self):
        return self

    def __call__(self, path_to_file):
        return self.jar_file.read(path_to_file)

    def __exit__(self, exctype, excinst, exctb):
        if excinst is not None:
            raise excinst
        self.jar_file.close()


class IndexPageWriter(object):
    '''
    An index page if no group/artifact/version is specified in the path.
    '''

    def __init__(self, repo_dir='~/.m2/repository'):
        if repo_dir.startswith('~'):
            self.repo_dir = os.path.expanduser(repo_dir)
        else:
            self.repo_dir = repo_dir

    def __call__(self):
        ''' Returns the index page as a string. '''

        jdk_api = '<li><a href="jdoc/jdk/docs/api/index.html">Java API</a></li>'
        links = list()
        for artifact in self.__artifacts():
            parts = artifact.split('/')
            version = parts.pop()
            artifact = parts.pop()
            parts.pop(0)
            group = '.'.join(parts)
            links.append('<li><a href=\"jdoc/m2/%s/%s/%s/index.html\">%s:%s:%s</a></li>' %
                         (group, artifact, version, group, artifact, version))

        return '''<html>
    <head>
        <title>Available Javadoc</title>
    </head>
    <body>
        <h1>Available Local Javadoc</h1>
        <h2>JDK API</h2>
        <p><ul>
        %s</ul></p>
        <h2>Maven Repository</h2>
        <p><ul>
        %s</ul></p>
    </body>
</html>''' % (jdk_api, '\n'.join(links))

    def __artifacts(self):
        '''
        Returns a generator of all javadoc jar artifacts in the local Maven
        repository.
        '''

        for (path, dirs, files) in os.walk(self.repo_dir):
            files = [f for f in files if re.match('.*-javadoc.jar$', f)]
            if len(files) > 0:
                yield path.replace(self.repo_dir, '')


class Handler(BaseHTTPRequestHandler):
    ''' Serves the javadoc html and css files '''

    def do_GET(self):
        ''' Handles HTTP GET requests by serving content from the `/jdoc` path.'''
        self.protocol_version = 'HTTP/1.1'
        path_elements = self.path.split('/')
        try:
            if len(path_elements) == 2:
                index = IndexPageWriter()
                doc = index()
            else:
                (repo_type, elements) = path_elements[2], path_elements[3:]
                archive_path = None
                doc_archive = None
                doc_path = None

                if repo_type not in REPO_TYPES:
                    raise IOError

                if len(elements) == 0:
                    index = IndexPageWriter()
                    doc = index()
                else:
                    if repo_type == 'm2':
                        coordinates = dict(zip(('group',
                                                'artifact',
                                                'version'), elements))
                        mvn_artifact = MavenRepositoryArtifactPath(coordinates)
                        doc_archive = mvn_artifact()
                        doc_path = '/'.join(elements[3:])
                    elif repo_type == 'jdk':
                        doc_archive = '/usr/lib/jvm/java-8-openjdk-amd64/jdk-8u77-docs-all.zip'
                        doc_path = '/'.join(elements)
                    else:
                        raise IOError

                    with ZippedJavadocContent(doc_archive) as javadoc:
                        doc = javadoc(doc_path)
        except IOError as io_error:
            traceback.print_exc(file=sys.stdout)
            self.__respond_with_404(elements[4:])
        except KeyError as key_error:
            traceback.print_exc(file=sys.stdout)
            if key_error.message.endswith('archive'):
                self.__respond_with_404(elements[4:])
            else:
                self.__respond_with_400(elements)
        except:
            traceback.print_exc(file=sys.stdout)
            self.__respond_with_500()
        else:
            fname, ext = os.path.splitext(self.path)
            if ext is '':
                ext = '.html'
            self.send_response(200)
            if ext in types_map:
                self.send_header('Content-Type', types_map[ext])
            elif ext in OTHER_TYPES:
                self.send_header('Content-Type', OTHER_TYPES[ext])
            else:
                print 'Warning! %s not a MIME type' % (ext)
            self.send_header('Content-Length', len(doc))
            self.end_headers()
            self.wfile.write(doc)

    def __respond_with_400(self, bad_path):
        doc = '''<html>
    <head>
        <title>Bad Request</title>
    </head>
    <body>
        <h1>Error - %s path is incomplete</h1>
        <p>URL paths must consist of <code>group/artifact/version</code>.</p>
    </body>
</html>''' % ('/'.join(bad_path))
        self.send_response(400)
        self.send_header('Content-Type', 'text/html')
        self.send_header('Content-Length', len(doc))
        self.end_headers()
        self.wfile.write(doc)

    def __respond_with_404(self, bad_path):
        doc = '''<html>
    <head>
        <title>Not Found</title>
    </head>
    <body>
        <h1>Error - %s not found</h1>
    </body>
</html>''' % ('/'.join(bad_path))
        self.send_response(404)
        self.send_header('Content-Type', 'text/html')
        self.send_header('Content-Length', len(doc))
        self.end_headers()
        self.wfile.write(doc)

    def __respond_with_500(self):
        doc = '''<html>
    <head>
        <title>Internal Server Error</title>
    </head>
    <body>
        <h1>Error</h1>
        <p>An internal server error occurred.</p>
    </body>
</html>'''
        self.send_response(500)
        self.send_header('Content-Type', 'text/html')
        self.send_header('Content-Length', len(doc))
        self.end_headers()
        self.wfile.write(doc)

if __name__ == '__main__':

    arg_parser = ArgumentParser(
        description=DESC, epilog=EPILOG, formatter_class=RawDescriptionHelpFormatter)
    arg_parser.add_argument('-p', '--port', metavar='n', default=PORT, nargs=1, type=int,
                            help='Port number the HTTP server is listening on (default is %d)' % (PORT))

    cli_args = arg_parser.parse_args()

    if isinstance(cli_args.port, list):
        port = cli_args.port[0]
    else:
        port = cli_args.port

    cmd = arg_parser.prog
    httpd = HTTPServer(('localhost', port), Handler)
    try:
        print '%s server ready at http://localhost:%d/jdoc' % (cmd, port)
        httpd.serve_forever()
    except KeyboardInterrupt:
        httpd.shutdown()
        print '%s server stopped' % (cmd)
