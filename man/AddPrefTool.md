# AddPrefTool.java

## NAME

`AddPrefTool.java` - Adds a Java Preference to the local preferences store.

## SYNOPSIS

```bash
    bash AddPrefTool.java [OPTIONS]
```

## DESCRIPTION

Adds a Java Preference to the local preferences store. Preferences follow the Java standard (see the [Preferences API](https://docs.oracle.com/javase/10/docs/api/java/util/prefs/Preferences.html) for background).

Single name-value pairs can be added to the **user root** (the default) or **system root**. If a **node name** is specified, then the name-value pair is added to that node.

Nearly all Preference types supported by the Preferences API are allowed except for byte arrays at this time.

## OPTIONS

<dl>
<dt><code>-C &lt;name&gt;</code>
<dt><code>--class &lt;name&gt;</code>
<dd>Specify the fully qualified class name for the preference node.
<dt><code>-h</code>
<dt><code>--help</code>
<dd>Show a short help message with options and exit.
<dt><code>-N &lt;name&gt;</code>
<dt><code>--node-name &lt;name&gt;</code>
<dd>Set the name for the preference node to store the name-value pair.
<dt><code>-n &lt;prefname&gt;</code>
<dt><code>--name &lt;prefname&gt;</code>
<dd><em>REQUIRED</em> Preference name in the name-value pair.
<dt><code>-S</code>
<dt><code>--system-root</code>
<dd>Add nodes or name-value pairs to the <em>system root</em>.
<dt><code>-t &lt;typename&gt;</code>
<dt><code>--type &lt;typename&gt;</code>
<dd>Preference type (boolean, double, float, int, long, string)
<dt><code>-U</code>
<dt><code>--user-root</code>
<dd>Add nodes or name-value pairs to the <em>user root</em>.
<dt><code>-v &lt;prefvalue&gt;</code>
<dt><code>--value &lt;prefvalue&gt;</code>
<dd><em>REQUIRED</em> Preference value in the name-value pair.
</dl>

## EXIT STATUS

<dl>
<dt><code>0</code>
<dd>Successful run.
<dt><code>1</code>
<dd>One of the following occurred:
<ul>
<li>A required option was not set (either `-n` or `-v`).
<li>An option with a required argument was missing its argument.
<li>An exception was thrown when saving the preference name-value pair.
</ul>
<dt><code>2</code>
<dd>Either no options were set or the `-h` option was set. 
</dl>

## EXAMPLES

The following are some sample invocations.

Add a preference with name `savedir` and value `${user.home}/Documents` to a node named `app-settings` under the **user root**:

        bash AddPrefTool.java -U \
          -N app-settings \
          -n savedir \
          -v ${user.home}/Documents

Omitting `-U` would have the same affect.

## NOTES

Items below refer to the short options, however the notes apply to the companion long options.

* When specifying the `-C` option, the named class has to be on the classpath when `java` is invoked.
* Only one of `-C` or `-N` can be set.
* If neither `-C` nor `-N` are set, the name-value pair is set just directly under the **user root** or **system root** depending on whether `-S` or `-U` is set.
* Only one of `-S` or `-U` can be set.
* `-U` is assumed if neither `-S` nor `-U` is set.
* If `-t`|`--type` isn't set, String is assumed.
* Adding an existing Preference will overwrite its value.

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.

### Known Issues

* Byte arrays are not supported.