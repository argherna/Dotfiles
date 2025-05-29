# DumpPrivateKey.java

## NAME
`DumpPrivateKey.java` - Dumps the private key from Java keystores in RFC format.

## SYNOPSIS

```bash
    java DumpPrivateKey.java [-keystore <keystore>] [-alias <alias>] 
       [-file <filename>] [-storepass [:env|:file] <arg>] 
       [-keypass [:env|:file] <arg>] [-storetype <arg>]
       
```

## DESCRIPTION

Dumps the private key from a Java keystore, which is not possible from `keytool`. The private key is output in RFC 1421 form. This tool has very obvious security implications, but I sometimes find it necessary to use it during development for diagnostic purposes. Please use good judgment when using this tool for yourself.

## OPTIONS

The options defined here are similar to `keytool` options to communicate intent.

<dl>
  <dt><code>-alias &lt;alias&gt;</code>
  <dd>Alias name of the entry in the keystore to process (default is <code>mykey</code>).
  <dt><code>-file &lt;filename&gt;</code>
  <dd>Output file name (<code>System.out</code> if not set).
  <dt><code>-help</code>
  <dd>Display a help message and exit with status code 2.
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
<li>An option with a required argument was unacceptable.
<li>An exception was thrown.
</ul>
<dt><code>2</code>
<dd>Either no options were set or the <code>-help</code> option was set. 
</dl>

## NOTES

* If a `-keystore` is not set, the default is *$HOME/.keystore*.
* If `-file` is not set, the default is to write the output to *System.out*.
* The default `-storetype` is *jceks*.
* The default `-alias` is *mykey*.

## SEE ALSO

* [`keytool`(1)](https://docs.oracle.com/en/java/javase/11/tools/keytool.html)
* [Keytool - Managing Your Keystore](https://dev.java/learn/jvm/tool/security/keytool/)
* [RFC 1421 Certificate Encoding format](https://docs.oracle.com/javase/7/docs/technotes/tools/solaris/keytool.html#EncodeCertificate)

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.
