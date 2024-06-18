# PrefTool.java

## NAME

`PrefTool.java` - Management tool to view and modify java preferences data.

## SYNOPSIS

### Start GUI

```bash
    java PrefTool.java
```

### Command Line

```bash
    java PrefTool.java -h|-add|-export|-import|-remove [OPTION] ARGUMENTS
```

## DESCRIPTION

Manages preferences data from the command line or GUI. The command line presents a short interface for viewing, importing, and managing preferences data. The GUI's visual layout is similar to other tools used to visualize a hierarchical file system.

## COMMANDS

### -add

#### DESCRIPTION

Adds a Preferences node and optionally a key and value to the backing store.

#### ARGUMENTS

<dl>
  <dt><code>NODE-PATH</code>
  <dd>Required node path to be added. Parents are created if needed per Preferences API.
  <dt><code>KEY</code>
  <dd>Optional key name to add to the node. If <code>KEY</code> is specified, <code>VALUE</code> must be specified too. The key can be no more than 80 characters long.
  <dt><code>VALUE</code>
  <dd>Value to add to the key. Can only be specified if <code>KEY</code> is specified. The value can be no more than 8192 characters long.
</dl>

#### OPTIONS

<dl>
  <dt><code>-S</code>
  <dd>Add the preference node, key and/or value to the system root (default is to add to the user root).
</dl>

### -export

#### DESCRIPTION

Exports a preferences node and optionally its subtree to an xml file.

#### ARGUMENTS

<dl>
  <dt><code>NODE-PATH</code>
  <dd>Full node path. Use '/' to separate levels.
  <dt><code>FILENAME</code>
  <dd>Optional file name to write xml to (default writes to System.out).
</dl>

#### OPTIONS

<dl>
  <dt><code>-S</code>
  <dd>Export node from the system root (default is to export from user root).
  <dt><code>-t</code>
  <dd>Export node and subtree (default is to export node only).
</dl>

### -import

#### DESCRIPTION

Imports preferences xml into the backing store.

#### ARGUMENTS

<dl>
  <dt><code>FILENAME</code>
  <dd>Optional filename to read Preferences xml from (default is System.in).
</dl>

### -remove

#### DESCRIPTION

Removes a preferences node or key (if specified). If a key is specified, the node will not be removed. If a key is not specified the node and all its keys, values, and children are removed.

#### ARGUMENTS

<dl>
  <dt><code>NODE-PATH</code>
  <dd>Full node path. Use '/' to separate levels.
  <dt><code>KEY</code>
  <dd>Optional preference key to remove.
</dl>

#### OPTIONS

<dl>
  <dt><code>-S</code>
  <dd>Remove Preferences node/key from the system root (default is to remove the node/key from the user root).
</dl>

## OPTIONS

<dl>
  <dt><code>-h</code>
  <dd>Displays a help message and exits with a status code of <code>2</code>.
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
  <dt><code>2</code>
  <dd>Either no options were set or the <code>-h</code> option was set. 
</dl>

## FILES

<dl>
  <dt><code>preftool_resources.properties</code>
  <dd>Resource messages for the command line and GUI.
</dl>

## SEE ALSO

* [Preferences API documentation](https://docs.oracle.com/en/java/javase/21/docs/api/java.prefs/java/util/prefs/Preferences.html)

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.

### Known Issues

Preferences nodes cannot be deleted from the GUI. This functionality is deliberately left out and cannot be selected.

When refreshing the preferences tree from the View menu, the address label does not clear itself out.

When using the command line with the GUI open, you cannot reliably get the node tree or key view to update using the refresh options without restarting the GUI.
