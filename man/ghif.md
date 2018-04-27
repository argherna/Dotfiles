# ghif.py

## NAME

`ghif.py` - Format output from `ghi.py` to screen or to Google Spreadsheet json.

## SYNOPSIS

```bash
    python3 ghif.py [-L DEBUG|INFO|WARNING|ERROR|CRITICAL]
      [-s] [<infile>] [<outfile>]
```

## DESCRIPTION

Convert json output from ghi.py into a report-style format. Output will be converted to Google Spreadsheet json by default. By setting an option, output can be formatted to a small screen table.

Typical usage scenario would be as follows:

```bash
python3 ghi.py list -a USER REPO_OWNER REPO_NAME | python3 ghif.py
```

That is, pipe intput into this program from ghi.py specifying no options. Then pipe the output into ugs.py for uploading into Google Sheets.

## ARGUMENTS

<dl>
  <dt><code>&lt;infile&gt;</code>
  <dd>Input file with GitHub issues json. Uses <em>sys.stdin</em> if not specified.
  <dt><code>&lt;outfile&gt;</code>
  <dd>Output file with Google Apps Spreadsheet-formatted json. Uses <em>sys.stdout</em> if not specified.
</dl>

## OPTIONS

<dl>
  <dt><code>-L &lt;LEVEL&gt;</code>
  <dd>Set the logging level to one of <em>DEBUG</em>, <em>INFO</em>, <em>WARNING</em>, <em>ERROR</em>, or <em>CRITICAL</em> (default is <em>ERROR</em>).
  <dt><code>-h</code>
  <dd>Shows a help message and exits.
  <dt><code>-s</code>
  <dd>Format output for screen.
</dl>

## SEE ALSO

* [Google Sheets API for Python](https://developers.google.com/sheets/api/quickstart/python)

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.
