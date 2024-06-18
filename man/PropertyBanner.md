# PropertyBanner.java

## NAME

`PropertyBanner.java` - prints a banner used as a header in properties files.

## SYNOPSIS

```bash
    java PropertyBanner.java BANNER-TEXT
```

## DESCRIPTION

Prints a banner for use in Java Properties files to separate sections. The banner is printed to `System.out`. The banner is nothing more than a commented section that can be used to denote properties that follow. A sample of output is below:

```
# ------------------------------------------------------------------------------
#
#                                Sample Banner
#
# ------------------------------------------------------------------------------
```

Text must be no longer than 78 characters to account for the comment character and spacing.

## ARGUMENTS

<dl>
  <dt><code>BANNER-TEXT</code>
  <dd>Required text for the banner. Surround the argument with double-quotes to ensure the argument is treated as a single argument. If no argument is provided, that's an error.
</dl>


## EXIT STATUS

<dl>
  <dt><code>0</code>
  <dd>Successful run.
  <dt><code>1</code>
  <dd>Required banner text argument was not set.
</dl>

## EXAMPLES

Sample run used to create the banner above:

```java
    java PropertyBanner.java "Sample Banner"
```

## NOTES

Output is sent to `System.out` which means to capture the output it will have to be either redirected to a file or piped into a program that can process text. For example, on macOS you can pipe the output to [pbcopy](https://ss64.com/mac/pbcopy.html) to have the data sent to the pasteboard and then pasted into an editor.

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.
