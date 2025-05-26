# ImportBase64SecretKey.java

## NAME

`ImportBase64SecretKey.java` - Imports a base64-encoded string into a keystore as a secret key.

## SYNOPSIS

```bash
    java ImportBase64SecretKey.java -alias <alias> \
      -keystore <keystore> -storepass <arg> \
      -file <filename> [-keypass <arg>] [-keyalg <arg>] \
      [-help]
```

## DESCRIPTION

Imports a base64-encoded string from a file into the named keystore as a secret key. Sometimes a secret key has to be installed from a non-Java application from a string which is not possible with `keytool`.

## OPTIONS

The options defined here are similar to `keytool` options to communicate intent.

<dl>
  <dt><code>-alias &lt;alias&gt;</code>
  <dd>alias name of the entry to process
  <dt><code>-filename &lt;filename&gt;</code>
  <dd>input file name with a single base64-encoded string
  <dt><code>-help</code>
  <dd>shows a help message and exits
  <dt><code>-keyalg &lt;arg&gt;</code>
  <dd>key algorithm name (default is <em>AES</em>)
  <dt><code>-keypass &lt;arg&gt;</code>
  <dd>key password (default is value of <code>-storepass</code>)
  <dt><code>-keystore &lt;keystore&gt;</code>
  <dd>keystore name
  <dt><code>-storepass &lt;arg&gt;</code>
  <dd>keystore password
  <dt><code>-storetype &lt;arg&gt;</code>
  <dd>keystore type (default is <em>jceks</em>)
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

## SEE ALSO

`keytool`(1)

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.
