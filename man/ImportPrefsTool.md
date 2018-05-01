# ImportPrefsTool.java

## NAME

`ImportPrefsTool.java` - Imports Preferences XML into the local preferences store.

## SYNOPSIS

```bash
    bash ImportPrefsTool.java [<filename>]
```

## DESCRIPTION

Imports Preferences XML into the local preferences store. See the [Preferences API](https://docs.oracle.com/javase/10/docs/api/java/util/prefs/Preferences.html) for information about the DTD.

Preferences XML can be exported using `ExportPrefsTool.java` which would give a good starting XML template without having to manually start one.

## ARGUMENTS

<dl>
  <dt><code>&lt;filename&gt;</code>
  <dd>Optional filename of preferences XML (default is to read from <em>System.in</em>).
</dl>

## OPTIONS

<dl>
  <dt><code>-h</code>
  <dt><code>shows a help message and exits</code>
  <dd>
</dl>

## EXIT STATUS

<dl>
  <dt><code>0</code>
  <dd>Successful run.
  <dt><code>1</code>
  <dd>One of the following occurred:
    <ul>
      <li>The given filename couldn't be found or read.
      <li>An exception was thrown/raised.
    </ul>
  <dt><code>2</code>
  <dd>Either no options were set or the <code>-h</code> option was set. 
</dl>

## SEE ALSO

`ExportPrefsTool.java`

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.
