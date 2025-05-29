# DumpSecretKey.java

## NAME
`DumpSecretKey.java` - Dumps secret keys from Java keystores.

## SYNOPSIS

```bash
    java DumpSecretKey.java -alias <alias> [-keystore <keystore>]
      [-storepass [:env|:file] <arg>] [-file <filename>] 
      [-storetype <arg>] [-keypass [:env|:file] <arg>] 
      [-raw] [-help]
```

## DESCRIPTION

Dumps a secret key from a Java keystore, which is not possible from `keytool`. Output is a base-64 encoded string to *System.out*. The `-raw` option will not base-64 encode the output. The `-file` option will save the output to the given filename.

**NOTE**: using this tool will expose a secret key or password. It is strongly recommended to only use this under the supervision of an IT security specialist.

## OPTIONS

The options defined here are similar to `keytool` options to communicate intent.

<dl>
  <dt><code>-alias &lt;arg&gt;</code>
  <dd>Alias name of the entry in the keystore to process.
  <dt><code>-file &lt;filename&gt;</code>
  <dd>Output file name (<code>System.out</code> if not set).
  <dt><code>-help</code>
  <dd>Display a help message and exit with status code 2.
  <dt><code>-keypass [:env|:file] &lt;arg&gt;</code>
  <dd>Key password. If <code>:env</code> modifier is specified, retrieve value of the specified environment variable. If <code>:file</code> modifier specified, read password from the specified file name. Otherwise, use the given argument as the password. If not set, use the same value as <code>-storepass</code>. If not the same value as <code>-storepass</code>, user will be prompted for a key password.
  <dt><code>-keystore &lt;keystore&gt;</code>
  <dd>Keystore file name (<code>$HOME/.keystore</code> if not set).
  <dt><code>-raw</code>
  <dd>Bypass base64 encoding of key and dump it raw to the output.
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
      <li>An exception was thrown.
    </ul>
  <dt><code>2</code>
  <dd>Either no options were set or the <code>-help</code> option was set. 
</dl>

## FILES

<dl>
  <dt><code>$HOME/.keystore</code>
  <dd>Default keystore used if <code>-keystore</code> argument is not specified.
</dl>

## SEE ALSO

* [`keytool`(1)](https://docs.oracle.com/en/java/javase/11/tools/keytool.html)
* [Keytool - Managing Your Keystore](https://dev.java/learn/jvm/tool/security/keytool/)


## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.

