# ghi.py

## NAME

`ghi.py` - GitHub issues tool.

## SYNOPSIS

```bash
     python ghi.py [OPTIONS] <command> <ARGUMENTS>
```

## DESCRIPTION

Interact with issues in a GitHub repository. The output of this program is raw json.

## COMMANDS

### list

#### DESCRIPTION

Retrieve issues json from the GitHub repository displaying it in raw json.

#### ARGUMENTS

<dl>
  <dt><code>&lt;OWNER&gt;</code>
  <dd>Owner of the GitHub repository to list issues from.
  <dt><code>&lt;NAME&gt;</code>
  <dd>Name of the GitHub repository to list issues from.
  <dt><code>&lt;FILE&gt;</code>
  <dd>Optional name of an output file to write issues json to (default is <em>sys.stdout</em>).
</dl>

## OPTIONS

<dl>
  <dt><code>-a &lt;USER[:PASS]&gt;</code>
  <dd>Basic authentication credentials. If the password is not supplied, program will prompt for it.
  <dt><code>-L &lt;LEVEL&gt;</code>
  <dd>Set the logging level to one of <em>DEBUG</em>, <em>INFO</em>, <em>WARNING</em>, <em>ERROR</em>, or <em>CRITICAL</em> (default is <em>ERROR</em>).
  <dt><code>-h</code>
  <dd>Shows a help message and exits.
</dl>

## EXIT STATUS

<dl>
  <dt><code>0</code>
  <dd>Successful run.
  <dt><code>1</code>
  <dd>One of the following occurred:
    <ul>
      <li>A required option was not set.
      <li>An unkown function was specified.
      <li>A problem occurred communicating with GitHub.
    </ul>
</dl>

## EXAMPLES

List issues from the *foo* repository owned by *bar* writing the results to *foo-issues.json*:

```bash
python3 ghi.py list foo bar foo-issues.json
```

## NOTES

Requires python3 and requests to execute. Communicates with the GitHub API version 3.

## SEE ALSO

* [Python3 documentation](https://docs.python.org/3/)
* [Requests](http://docs.python-requests.org/en/master/)
* [GitHub v3 API](https://developer.github.com/v3/)

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.

