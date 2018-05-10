# log2csv.py

## NAME

`log2csv.py` - convert an Apache access log into a tab-delimited CSV file.

## SYNOPSIS

```bash
    python3 log2csv.py [-h] [-s] [-d <delimiter>] [INFILE] [OUTPUT-DIR]
```

## ARGUMENTS

<dl>
  <dt><code>INFILE</code>
  <dd>Name of an optional input file (default is <code>stdin</code>).
  <dt><code>OUTPUT-DIR</code>
  <dd>Name of an output directory. The name of the output file will be the same as the input file, but with the <code>csv</code> suffix. If none is specified, then the output is written to <code>stdout</code>.
</dl>

## OPTIONS

<dl>
  <dt><code>-d &lt;DELIMITER&gt;</code>
  <dd>Set a delimiter to use in the output csv file (default is <code>\t</code>).
  <dt><code>-h</code>
  <dd>Print a help message and exit.
  <dt><code>-s</code>
  <dd>Suppress printing the header row in the output file.
</dl>

## EXIT STATUS

<dl>
  <dt><code>0</code>
  <dd>Successful run.
  <dt><code>1</code>
  <dd>An error occurred reading the input or writing the output.
</dl>

## NOTES

Input should be in the Apache access log format. The regular expression used to match the lines in the log file was borrowed from the loghetti project.

## SEE ALSO

* [Apache Access Log format](http://httpd.apache.org/docs/2.0/logs.html#accesslog)
* [loghetti](https://github.com/bkjones/loghetti)

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.

