# sbate.py

## NAME

`sbate.py` - Sends a Binary file ATtachment in an Email (sbate).

## SYNOPSIS

```bash
    python3 sbate.py [OPTIONS] FILENAME
```

## DESCRIPTION

The setting of the options described above have precedence. If they are supplied on the command line, they will be used. Users can put a file named `.sbate.conf` in their home directory to supply values that are used often. The program will use the values in this file if they are supplied.

## ARGUMENTS

<dl>
  <dt><code>FILENAME</code>
  <dd>Required absolute path of the binary file to attach to an email.
</dl>

## OPTIONS

<dl>
  <dt><code>-c &lt;CONFIG_FILE&gt;</code>
  <dd>Read configuration from a file (default is <code>~/.sbate.conf</code>)
  <dt><code>-H &lt;HOST&gt;</code>
  <dd>Name of the host running the SMTP server (default <code>localhost</code>).
  <dt><code>-p &lt;PORT&gt;</code>
  <dd>Port number the SMTP server is listening on (default <em>8025</em>).
  <dt><code>-u &lt;USERNAME&gt;</code>
  <dd>Username with privileges to send email from the SMTP server (default <em>dummy</em>).
  <dt><code>-W &lt;PASSWORD&gt;</code>
  <dd>Password for the user (default <em>dummy</em>). Use with caution.
  <dt><code>-n</code>
  <dd>Suppresses login to the SMTP server (useful during testing).
  <dt><code>-s '&lt;SUBJECT_TEXT&gt;'</code>
  <dd>Subject text to use for the email message (default <em>'message subject'</em>).
  <dt><code>-f &lt;FROMADDR&gt;</code>
  <dd>Email address of the sender (default <em>sender@example.com</em>).
  <dt><code>-t &lt;TOADDR&gt;</code>
  <dd>Email address of the receiver (default <em>receiver@example.com</em>).
  <dt><code>-m '&lt;MESSAGE_TEXT&gt;'</code>
  <dd>The message to send (default <em>'Hello from sbate.py!'</em>).
</dl>

## EXIT STATUS

<dl>
  <dt><code>0</code>
  <dd>Successful run.
  <dt><code>1</code>
  <dd>One of the following occurred:
    <ul>
      <li>SMTP host could not be found (check the value of the `-H` or `host`).
      <li>The binary file was not found (check the argument you supplied to the program).
    </ul>
</dl>

## FILES

<dl>
  <dt><code>~/.sbate.conf</code>
  <dd>The sbate configuration file.
</dl>

## SEE ALSO

`sbate-config`

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.
