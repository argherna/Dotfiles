# ghl.py

## NAME

`ghl.py` - maintain labels in a GitHub repository.

## SYNOPSIS

```bash
    python3 ghl.py [-a USER[:PASSWORD]] \
      [-l DEBUG|INFO|WARNING|ERROR|CRITICAL] \
      [delete <args>|list [-p] <args>|upload <args>]
```

## DESCRIPTION

Maintain issue labels in a GitHub repository. 

## COMMANDS

One of the following commands has to be set.

### delete

#### DESCRIPTION

Deletes GitHub labels from the specified repository.

#### ARGUMENTS

Arguments have to be specified in this order.

<dl>
  <dt><code>OWNER</code>
  <dd>The repository owner.
  <dt><code>REPO</code>
  <dd>The repository name.
  <dt><code>INFILE</code>
  <dd>Json file containing a list of GitHub label definitions (default is stdin).
</dl>

### list

#### DESCRIPTION

Lists all GitHub labels in the specified repository.

#### ARGUMENTS

Arguments have to be specified in this order.

<dl>
  <dt><code>OWNER</code>
  <dd>The repository owner.
  <dt><code>REPO</code>
  <dd>The repository name.
  <dt><code>OUTFILE</code>
  <dd>Output file to store label definition Json (default is stdout).
</dl>

#### OPTIONS

<dl>
  <dt><code>-p</code>
  <dd>Prunes the URL information from the output. This is useful when copying labels from one repository to another.
</dl>

### upload

#### DESCRIPTION

Upload GitHub labels to the specified repository

#### ARGUMENTS

Arguments have to be specified in this order.

<dl>
  <dt><code>OWNER</code>
  <dd>The repository owner.
  <dt><code>REPO</code>
  <dd>The repository name.
  <dt><code>INFILE</code>
  <dd>Json file containing a list of GitHub label definitions (default is stdin).
</dl>

## OPTIONS

<dl>
  <dt><code>-a &lt;USER[:PASS]&gt;</code>
  <dd>GitHub username and password.
  <dt><code>-l &lt;DEBUG|INFO|WARNING|ERROR|CRITICAL&gt;</code>
  <dd>Logging level to set (default is <code>WARNING</code>)
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
      <li>An exception was thrown/raised.
    </ul>
</dl>

## NOTES

If you use 2-factor authentication, generate a Personal access token at https://github.com/settings/tokens and use it for your password.

This script relies on the [requests](http://docs.python-requests.org/en/master/) library. It should be installed prior to running.

## SEE ALSO

* [Access Token Generator](https://github.com/settings/tokens)
* [GitHub Labels API v3](https://developer.github.com/v3/issues/labels/)
* [Requests: HTTP for Humans](http://docs.python-requests.org/en/master/)

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.
