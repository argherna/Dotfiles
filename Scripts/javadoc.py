#!/usr/bin/env python

'''
javadoc.py

Serves javadoc directly from javadoc jar files in local Maven repository.

This tool was heavily inspired (and copied) by the code at
<https://gist.github.com/mgodave/5406999>.
'''
from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
from mimetypes import types_map
from string import join
from zipfile import ZipFile
import sys
import traceback
import os
import re


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

        links = list()
        for artifact in self.__artifacts():
            parts = artifact.split('/')
            version = parts.pop()
            artifact = parts.pop()
            parts.pop(0)
            group = '.'.join(parts)
            links.append('<li><a href=\"m2/%s/%s/%s/index.html\">%s:%s:%s</a></li>' %
                         (group, artifact, version, group, artifact, version))

        return '''<html>
    <head>
        <title>Available Javadoc</title>
    </head>
    <body>
        <h1>Available Local Javadoc</h1>
        <p><ul>
        %s</ul></p>
    </body>
</html>''' % ('\n'.join(links))

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
        self.protocol_version = 'HTTP/1.1'
        path_elements = self.path.split('/')
        (repo_type, elements) = path_elements[1], path_elements[2:]
        archive_path = None
        if repo_type == 'm2':
            try:
                if len(elements) == 0:
                    index = IndexPageWriter()
                    doc = index()
                else:
                    coordinates = dict(zip(('group',
                                            'artifact',
                                            'version'), elements))
                    mvn_artifact = MavenRepositoryArtifactPath(coordinates)
                    with ZippedJavadocContent(mvn_artifact()) \
                            as javadoc:
                        doc = javadoc('/'.join(elements[3:]))
            except IOError as io_error:
                traceback.print_exc(file=sys.stdout)
                self._respond_with_404(elements[3:])
            except KeyError as key_error:
                traceback.print_exc(file=sys.stdout)
                if key_error.message.endswith('archive'):
                    self.__respond_with_404(elements[3:])
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
                self.send_header('Content-Type', types_map[ext])
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

    import getopt
    try:
        cmd = os.path.basename(sys.argv[0])
        port = 8080
        opts, args = getopt.getopt(sys.argv[1:], 'p:')
        for opt, val in opts:
            if opt == '-p':
                port = int(val)

        httpd = HTTPServer(('localhost', port), Handler)
        try:
            print '%s server ready at http://localhost:%d/m2' % (cmd, port)
            httpd.serve_forever()
        except KeyboardInterrupt:
            print '%s server stopped' % (cmd)
            httpd.shutdown()

    except (getopt.error, ValueError):
        print '''%s [OPTION]

Serve javadoc from the local Maven repository

If you don't see javadoc for an artifact you expect to see, run these
commands from the base directory of your Maven project:

    mvn dependency:sources
    mvn dependency:resolve -Dclassifier=javadoc

Options:

 -p <port>  Start the HTTP server on this port. The default is 8080.
''' % (cmd)
