# LocalDirectoryServer.java

## NAME

`LocalDirectoryServer.java` - serves files from specified directories on specified URL paths.

## SYNOPSIS

```bash
    bash LocalDirectoryServer.java [port] \
      <directory-to-serve:server-path> ...
```

## DESCRIPTION

Gives access to files through a web browser. Intended for some of the following uses:

* Testing static websites.
* Serve json files to develop ajax calls from other pages.

A port number can be specified but *8086* is the default.

## ARGUMENTS

<dl>
  <dt><code>port</code>
  <dd>A port number to serve the files from (default is <em>8086</em>).
  <dt><code>directory-to-serve:server-path</code>
  <dd>Directory to serve files from and server path separated by a ':'. Directories should be specified as absolute paths and the server path should begin with '/'.
</dl>

## EXIT STATUS

<dl>
  <dt><code>0</code>
  <dd>Successful run.
  <dt><code>1</code>
  <dd>An unrecoverable error occurred during service.
  <dt><code>2</code>
  <dd>No arguments were set. 
</dl>

## EXAMPLES

Serve files in the `site1` directory to be accessed using `http://localhost:port/site1`:

```bash
  bash LocalDirectoryServer.java /home/user/site1:/site1
```

Serve files in the `site1` and `site2` directory to be accessed using `http://localhost:port/site1` and `http://localhost:port/site2`:

```bash
  bash LocalDirectoryServer.java /home/user/site1:/site1 \
    /home/user/site2:/site2
```

## NOTES

For serving json to support ajax calls, a crude implementation of CORS is used.

## SEE ALSO

* [CORS](https://www.w3.org/TR/cors/)

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.

### Known Issues

* HTTPS is not supported.
* Directories are not displayed (only files).
