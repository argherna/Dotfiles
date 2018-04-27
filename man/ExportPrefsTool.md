# ExportPrefsTool.java

## NAME

`ExportPrefsTool.java` - export Java preferences in XML format from local preferences store.

## SYNOPSIS

```bash
    bash ExportPrefsTool.java [-C <classname> | -N <nodename>]
      [-S | -U] [-t] [<filename>]
```

## DESCRIPTION

Export Java preferences in XML format. If a `<filename>` is set, export the XML to that file, otherwise write it to stdout. Specific preferences can be set using one of either `-C <classname>` or `-N <nodename>` which will export the node but not its subtree (that is, child nodes of the node will not be exported). You can set `-t` to export the entire subtree.

## ARGUMENTS

<dl>
  <dt><code>&lt;filename&gt;</code>
  <dd>File name to write the preferences XML to if set.
</dl>

## OPTIONS

Options for all preferences tools are similar to communicate intent.

<dl>
  <dt><code>-C &lt;classname&gt;</code>
  <dd>Specify the fully qualified class name for the   preference node.
  <dt><code>-t</code>
  <dd>Export whole subtree under the node.
  <dt><code>-h</code>
  <dd>Shows a help message and exits.
  <dt><code>-N &lt;nodename&gt;</code>
  <dd>Set the name for the preference node to export.
  <dt><code>-S</code>
  <dd>Exports preferences from the system root.
  <dt><code>-U</code>
  <dd>Exports preferences from the user root.
</dl>

## EXIT STATUS

<dl>
  <dt><code>0</code>
  <dd>Successful run.
  <dt><code>1</code>
  <dd>One of the following occurred:
    <ul>
      <li>An option with a required argument was missing its argument.
      <li>An exception was thrown.
    </ul>
  <dt><code>2</code>
  <dd>Either no options were set or the `-h` option was set. 
</dl>

## NOTES

* If `<filename>` is not specified, preferences XML is written to ***System.out***.
* Only one of `-C` or `-N` can be set.
* If neither `-C` nor `-N` are set, the name-value pair is set just directly under the ***user root*** or ***system root*** depending on whether `-S` or `-U` is set.
* Only one of `-S` or `-U` can be set.
* `-U` is assumed if neither `-S` nor `-U` is set.

## SEE ALSO

* [Preferences API](https://docs.oracle.com/javase/10/docs/api/java/util/prefs/Preferences.html)

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.

### Known Issues
