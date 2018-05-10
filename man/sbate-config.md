# sbate-config

## NAME

`sbate-config` - sbate configuration file.

## DESCRIPTION

sbate will get configuration data in the following order:

1. command-line options
1. user's configuration file

The configuration file can be changed from the default by using the `-C` option in sbate. The configuration file format is a typical python configuration file that can be read by `ConfigParser`.

### SECTIONS

#### [smtp]

<dl>
  <dt><code>host</code>
  <dd>SMTP server host.
  <dt><code>port</code>
  <dd>SMTP server port.
  <dt><code>username</code>
  <dd>User login name to the SMTP server.
  <dt><code>password</code>
  <dd>User login password to the SMTP server.
  <dt><code>suppress_login</code>
  <dd>If <code>True</code>, do not attempt to log in to the server.
</dl>

#### [message]

<dl>
  <dt><code>subject</code>
  <dd>Email subject.
  <dt><code>from</code>
  <dd>Email sender address.
  <dt><code>to</code>
  <dd>Email receiver address.
  <dt><code>message</code>
  <dd>The email message.
</dl>

## FILES

<dl>
  <dt><code>~/.sbate.conf</code>
  <dd>Default location of sbate settings. This can be overridden by the <code>-C</code> option in sbate.
</dl>

## SEE ALSO

* `sbate`
* [Python ConfigParser](https://docs.python.org/3/library/configparser.html)

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>
