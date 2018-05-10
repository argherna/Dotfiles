# RmPref.java

## NAME

`RmPref.java` - Removes a Java Preference from the local preferences store.

## SYNOPSIS

```bash
    bash RmPref.java [-C <classname> | -N <nodename>] [-h]
      [-n <prefname>] [-S | -U]
```

## DESCRIPTION

Removes a Java Preference from the local preferences store. Preferences follow the Java standard (see the [Preferences API](https://docs.oracle.com/javase/10/docs/api/java/util/prefs/Preferences.html) for background).

By default, if not otherwise specifed Preferences are removed from the user root. If a node name is not specified, the removal will happen at the root (user or system) node. If a preference name is not specified, the entire node will be removed.

## OPTIONS

Options for all preferences tools are similar to communicate intent.

<dl>
  <dt><code>-C &lt;classname&gt;</code>
  <dd>Specify the fully qualified class name for the preference node.
  <dt><code>-h</code>
  <dd>Show a short help message with options and exit.
  <dt><code>-N &lt;name&gt;</code>
  <dd>Name of the preference node to remove or to remove a preference from (note the whole node is removed if no preference name is set).
  <dt><code>-n &lt;prefname&gt;</code>
  <dd>Preference name to remove.
  <dt><code>-S</code>
  <dd>Remove node or name-value pair from the <em>system root</em>.
  <dt><code>-U</code>
  <dd>Remove node or name-value pair from the <em>user root</em>.
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

## EXAMPLES

The following are some sample invocations.

Remove a preference with name `savedir` from a node named `app-settings` under the **user root**:

        bash RmPref.java -U \
          -N app-settings \
          -n savedir 

Omitting `-U` would have the same affect.

## NOTES

* When specifying the `-C` option, the named class has to be on the classpath when `java` is invoked.
* Only one of `-C` or `-N` can be set.
* If neither `-C` nor `-N` are set, the name-value pair is set just directly under the ***user root*** or ***system root*** depending on whether `-S` or `-U` is set.
* Only one of `-S` or `-U` can be set.
* `-U` is assumed if neither `-S` nor `-U` is set.

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.
