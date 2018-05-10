# LdapConnectTest.java

## NAME

`LdapConnectTest.java` - connects to an LDAP server with a given set of credentials.

## SYNOPSIS

```bash
    bash LdapConnectTest.java [-D <bindDN>] [-h] [-H <ldapUrl>] \
      [-w <password>|-W] [-Z] 
```

## DESCRIPTION

LdapConnectTest can be used to test a connection to an LDAP server by performing a simple bind with a given set of credentials. If the connection is successful, "OK" is printed on the command line and the program exits with a status of 0. Otherwise, an error message is displayed and the program exits with a status of 1.

## OPTIONS

<dl>
  <dt><code>-D &lt;bindDN&gt;</code>
  <dd>Bind DN to use to connect to LDAP.
  <dt><code>-h</code>
  <dd>Displays a help message and exits.
  <dt><code>-H &lt;URL&gt;</code>
  <dd>The LDAP URL to the server.
  <dt><code>-w &lt;password&gt;</code>
  <dd>Password for the bind DN specified by <code>-D</code>.
  <dt><code>-W</code>
  <dd>Prompt for a password for the bind DN specified by <code>-D</code>.
  <dt><code>-Z</code>
  <dd>Use StartTLS.
</dl>

## EXIT STATUS

<dl>
  <dt><code>0</code>
  <dd>Successful run.
  <dt><code>1</code>
  <dd>One of the following occurred:
    <ul>
      <li>A required option was not set.
      <li>An option with a required argument was missing its argument.
      <li>Connection to the LDAP server was unsuccessful.
    </ul>
  <dt><code>2</code>
  <dd>Either no options were set or the <code>-h</code> option was set. 
</dl>

## NOTES

The command line arguments are similar to `ldapsearch`.

You can only set one of `-w` or `-W`. If `-w` is set with a password, `-W` is ignored. If neither is set that is an error.

## SEE ALSO

`ldapsearch(1)`

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.

### Known Issues

Only simple binds are supported at this time.
