# DumpSecretKey.java

## NAME
`DumpSecretKey.java` - Dumps secret keys from Java keystores.

## SYNOPSIS

```bash
    bash DumpSecretKey.java -keystore <keystore> -alias <alias> 
      -storepass <arg> [-file <filename>] [-storetype <arg>]
      [-raw] [-help]
```

## DESCRIPTION

Dumps a secret key from a Java keystore, which is not possible from `keytool`. Output is a base-64 encoded string to *System.out*. The `-raw` option will not base-64 encode the output. The `-file` option will save the output to the given filename.

## OPTIONS

The options defined here are similar to `keytool` options to communicate intent.

<dl>
  <dt><code>-alias &lt;alias&gt;</code>
  <dd>alias name of the entry in the keystore to process.
  <dt><code>-file &lt;filename&gt;</code>
  <dd>output file name.
  <dt><code>-help</code>
  <dd>shows a help message and exits.
  <dt><code>-keypass &lt;arg&gt;</code>
  <dd>key password
  <dt><code>-keystore &lt;keystore&gt;</code>
  <dd>keystore name
  <dt><code>-raw</code>
  <dd>dumps the raw key value
  <dt><code>-storepass &lt;arg&gt;</code>
  <dd>keystore password
  <dt><code>-storetype &lt;arg&gt;</code>
  <dd>keystore type
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
      <li>An exception was thrown.
    </ul>
  <dt><code>2</code>
  <dd>Either no options were set or the <code>-help</code> option was set. 
</dl>

## SEE ALSO

`keytool`(1)

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.

