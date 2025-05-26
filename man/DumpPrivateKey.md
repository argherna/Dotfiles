# DumpPrivateKey.java

## NAME
`DumpPrivateKey.java` - Dumps the private key from Java keystores in RFC format.

## SYNOPSIS

```bash
    java DumpPrivateKey.java [-keystore <keystore>] [-alias <alias>] 
       [-file <filename>] [-storepass <arg>] [-keypass <arg>]
       [-storetype <arg>]
       
```

## DESCRIPTION

Dumps the private key from a Java keystore. This tool has very obvious security implications, but I sometimes find it necessary to use it during development for diagnostic purposes. Please use good judgment when using this tool for yourself.

## OPTIONS

The options defined here are similar to `keytool` options to communicate intent.

<dl>
<dt><code>-alias &lt;alias&gt;</code>
<dd>alias name of the entry to process (default is <em>mykey</em>).
<dt><code>-file &lt;filename&gt;</code>
<dd>output file name (default is to print to <em>System.out</em>).
<dt><code>-keypass &lt;arg&gt;</code>
<dd>key password (default is the value of <code>-storepass</code>).
<dt><code>-keystore &lt;keystore&gt;</code>
<dd>keystore name (default is <em>$HOME/.keystore</em>).
<dt><code>-storepass &lt;arg&gt;</code>
<dd>keystore password.
<dt><code>-storetype &lt;arg&gt;</code>
<dd>keystore type (default is <em>jceks</em>).
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

* `keytool`(1)
* [RFC 1421 Certificate Encoding format](https://docs.oracle.com/javase/7/docs/technotes/tools/solaris/keytool.html#EncodeCertificate)

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.
