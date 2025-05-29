# ImportBase64SecretKey.java

## NAME

`ImportBase64SecretKey.java` - Imports a base64-encoded string into a keystore as a secret key.

## SYNOPSIS

```bash
    java ImportBase64SecretKey.java -alias <alias> \
      [-keystore <keystore>] [-storepass [:env|:file] <arg>] \
      [-file <filename>] [-keypass [:env|:file] <arg>] [-keyalg <arg>] \
      [-help]
```

## DESCRIPTION

Imports a base64-encoded string from a file into the named keystore as a secret key. Sometimes a secret key has to be installed from a non-Java application from a string which is not possible with `keytool`.

## OPTIONS

The options defined here are similar to `keytool` options to communicate intent.

<dl>
  <dt><code>-alias &lt;alias&gt;</code>
  <dd>Alias name of the entry in the keystore to process.
  <dt><code>-filename &lt;filename&gt;</code>
  <dd>Input file name with a single base64-encoded string (<code>System.in</code> if not set).
  <dt><code>-help</code>
  <dd>Display a help message and exit with status code <code>2</code>.
  <dt><code>-keyalg &lt;arg&gt;</code>
  <dd>Key algorithm to use (<code>AES</code> if not set).
  <dt><code>-keypass [:env|:file] &lt;arg&gt;</code>
  <dd>Key password. If <code>:env</code> modifier is specified, retrieve value of the specified environment variable. If <code>:file</code> modifier specified, read password from the specified file name. Otherwise, use the given argument as the password. If not set, use the same value as <code>-storepass</code>. If not the same value as <code>-storepass</code>, user will be prompted for a key password.
  <dt><code>-keystore &lt;keystore&gt;</code>
  <dd>Keystore file name (<code>$HOME/.keystore</code> if not set).
  <dt><code>-storepass [:env|:file] &lt;arg&gt;</code>
  <dd>Keystore password. If <code>:env</code> modifier is specified, retrieve value of the specified environment variable. If <code>:file</code> modifier specified, read password from the specified file name. Otherwise, use the given argument as the password. If not set, user will be prompted for store password.
  <dt><code>-storetype &lt;arg&gt;</code>
  <dd>Keystore type (result of <code>KeyStore.getDefaultType()</code> if not set).
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
  <dd>Either no options were set or the <code>-help</code> option was set. 
</dl>

## SEE ALSO

* [`keytool`(1)](https://docs.oracle.com/en/java/javase/11/tools/keytool.html)
* [Keytool - Managing Your Keystore](https://dev.java/learn/jvm/tool/security/keytool/)

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.
