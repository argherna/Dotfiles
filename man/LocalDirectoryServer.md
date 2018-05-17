# LocalDirectoryServer.java

## NAME

`LocalDirectoryServer.java` - serves files from specified directories on specified URL paths.

## SYNOPSIS

```bash
    bash LocalDirectoryServer.java [-H <http-port>] \
      [-S <https-port>] [@<filename> ...] \
      <directory-to-serve:server-path[:secure]> ...
```

## DESCRIPTION

Gives access to files through a web browser. Intended for some of the following uses:

* Testing static websites.
* Serve json files to develop ajax calls from other pages.

The server supports HTTP and HTTPS. Port numbers can be set at startup but *8086* is the default for HTTP and *4436* is the default for HTTPS.

The server can have settings stored for CORS. See `LocalDirectoryServer-config` for details.

## ARGUMENTS

<dl>
  <dt><code>@&lt;filename&gt;</code>
  <dd>File containing an argument on each line. Each argument can be of the form below or can specify an option with its value. If the filename is not an absolute path, then the current user directory is searched for the file.
  <dt><code>directory-to-serve:server-path[:secure]</code>
  <dd>Directory to serve files from and server path separated by a ':'. Directories should be specified as absolute paths and the server path should begin with '/'. If <code>:secure</code> is at the end of the argument, serve this directory under HTTPS.
</dl>

## OPTIONS

<dl>
  <dt><code>-H &lt;http-port&gt;</code>
  <dd>Port to serve directories not marked <code>:secure</code> under HTTP (default is <em>8086</em>).
  <dt><code>-h</code>
  <dd>shows a help message and exits
  <dt><code>-S &lt;https-port&gt;</code>
  <dd>Port to serve directories marked <code>:secure</code> under HTTPS (default is <em>4436</em>).
</dl>

## EXIT STATUS

<dl>
  <dt><code>0</code>
  <dd>Successful run.
  <dt><code>1</code>
  <dd>An unrecoverable error occurred during service.
  <dt><code>2</code>
  <dd>No arguments were set or <code>-h</code> was set. 
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

An example of a small argument file named `lds-args.txt` in the directory `/home/andy` would be:

```
/home/andy/var/www/sites/site1:/site1
/home/andy/var/www/sites/site2:/site2:secure
```

Then the command line would be:

```bash
  bash LocalDirectoryServer.java @/home/andy/lds-args.txt
```

The server would serve `/site1` on HTTP port 8086 and `/site2` on HTTPS port 4436.

## SEE ALSO

`LocalDirectoryServer-config`

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.

### Known Issues

* Directories are not displayed (only files).
