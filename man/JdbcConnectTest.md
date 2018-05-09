# JdbcConnectTest.java

## NAME

`JdbcConnectTest.java` - connects to a database with user-specified settings and prints a message.

## SYNOPSIS

```bash
    bash JdbcConnectTest.java [-h] [-q] [-U <database-url>] \
      [-u <username>] [-w <password>]
```

## DESCRIPTION

JdbcConnectTest relies on a JRE with JDBC4. The jar file containing the JDBC4 drivers will need to be added to the classpath on a run.

Depending on the specified options, by default the message that is printed will contain the database url, the username used to connect to the database, and the driver name and version. If the `-q` option is set, the output is `OK` if a successful connection is made.

## OPTIONS

<dl>
  <dt><code>-h</code>
  <dd>Print a help message and exits.
  <dt><code>-q</code>
  <dd>Prints OK if set (that is, run quietly).
  <dt><code>-U &lt;database-url&gt;</code>
  <dd>Database Url.
  <dt><code>-u &lt;username&gt;</code>
  <dd>Username to connect to the database with.
  <dt><code>-w &lt;password&gt;</code>
  <dd>Password for the given username. If not set, you will be prompted for the password.
</dl>

## EXIT STATUS

<dl>
  <dt><code>0</code>
  <dd>Successful run.
  <dt><code>1</code>
  <dd>One of the following occurred:
    <ul>
      <li>Could not connect to the database due to either incorrect username and password, network issues, or incorrect database connection specifications.
      <li>An option with a required argument was missing its argument.
      <li>An exception was thrown/raised.
    </ul>
  <dt><code>2</code>
  <dd>Either no options were set or the <code>-h</code> option was set. 
</dl>

## EXAMPLES

Assuming you're using the shell directive in the source version, you could test connectivity to an HSQLDB database as follows:

```bash
    bash JdbcConnectTest.java /path/to/hsqldb.jar \
      -U jdbc:hsqldb:hsql://example.com:9001  \
      -u dbuser
```

With the password not set in the options, you would be prompted for it before the program finishes its run.

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.
