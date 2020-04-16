# Jttp.java

## NAME

`Jttp.java` - Send an HTTP(S) request to a web server and process the response.

## SYNOPSIS

### Java 13 and Newer:

```bash
java [jvm args] Jttp.java [-dhNORvV] [-A user[:passwd]] [-M mime_type] [-o filename] 
    [-P format_option] [--pre-process-script script_name] 
    [--pre-process-script-arg script_arg [--pre-process-script-arg script_arg] ...] 
    [--post-process-script script_name] 
    [--post-process-script-arg script_arg [--post-process-script-arg script_arg] ...] 
    [-p what] [-S session_name] [method] url [request_item ...]
```

## DESCRIPTION

Command line HTTP(S) client, similar to [httpie](https://httpie.org/). Offers numerous input and output methods and many authentication options. You should be using Java 13 or newer to execute Jttp directly from the source file. Input for forms or json can be set as arguments from the command line or as redirected input files. Simple json is generated from command line arguments. For more complex formats, use input redirection.

Jttp may be extended or enhanced by using scripts that execute before and after the request is made. These scripts may be written in languages supporting `javax.script` such as Groovy and Beanshell (others too). 

## ARGUMENTS

<dl>
  <dt><code>method</code>
  <dd>Optional HTTP method to set. Can be one of <code>DELETE</code>, <code>GET</code> (default), <code>HEAD</code>, <code>OPTIONS</code>, <code>PATCH</code>, <code>POST</code>, <code>PUT</code>, and <code>TRACE</code>.
  <dt><code>url</code>
  <dd>Request url. Scheme defaults to <code>http://</code> if the URL doesn't include one. You can also use a shorthand for localhost
  <pre><code>
      $ java Jttp.java :8080     # => http://localhost:8080
      $ java Jttp.java /foo      # => http://localhost/foo
  </code></pre>
  <dt><code>request_item</code>
  <dd>Optional key-value pairs to be included in the request. The separator used determins the type:
  <dl>
    <dt><code>':'</code> HTTP headers:
    <dd><code>Referer:http://example.com Cookie:foo=bar User-Agent:Crawler/1.0</code>
    <dt><code>'=='</code> URL parameters to be appended to the URI (query string):
    <dd><code>el==toro marco==polo</code>
    <dt><code>'='</code> Data fields to be serialized into a JSON object (with <code>-M JSON</code>) or form data (with <code>-M FORM</code>):
    <dd><code>fizz=buzz bar=baz foo=fu</code>
    <dt><code>'@'</code> Form file fields:
    <dd><code>filename@Documents/report.docx</code>
  </dl>
</dl>

## OPTIONS

<dl>
  <dt><code>-A,--auth user[:passwd]</code>
  <dd>If only the username is provided, (e.g. <code>-A user</code>), Jttp will prompt for the password.
  <dt><code>-d,--download</code>
  <dd>Do not print the response body to stdout. Rather, download it and store it in a file. The filename is guessed unless specified with <code>-o filename</code>. If the value of <code>-o</code> is not an absolute path, the output is saved in a relative directory to the current directory. If nothing is specified and no preference for download directory is set (see PREFERENCES), then the file is saved to 

      ${user.home}/.jttp/downloads/<GUESSED-FILENAME>
  
  <dt><code>-h,--help</code>
  <dd>Shows a detailed help message and exits.
  <dt><code>-M,--mime-type mime_type</code>
  <dd>Set the request MIME type. Should be one of FORM, JSON or MULTIPART.
  <dt><code>-N,--no-verify</code>
  <dd>Turn off certificate and host name checking. The internal logger will emit a WARNING message when this option is set.
  <dt><code>-O,--offline</code>
  <dd>Build the request and print it, but don't actually send it.
  <dt><code>-o,--output filename</code>
  <dd>Save output to <code>filename</code> instead of stdout. If <code>-d</code> is also set, then only the response body is saved to <code>filename</code>ß.
  <dt><code>-P,--pretty format_option</code>
  <dd>Controls output processing. The value can be <code>NONE</code> to not prettify the output, <code>ALL</code> to apply both colors and indenting (default when printing to <code>System.out</code>), <code>COLOR</code>, or <code>INDENT</code>.
  <dt><code>--post-process-script script_name</code>
  <dd>Script to run after the request has fetched data but before final output is handled by Jttp.
  <dt><code>--post-process-script-arg script_arg</code>
  <dd>Argument to pass to the <code>--post-process-script</code>. This can be specified as many times as you need for your script. The args will be passed to the script as an array of Strings to the script in a variable bound to the name <code>args</code>.
  <dt><code>--pre-process-script</code>
  <dd>Script to run before the request has fetched data but after initial setup has been performed by Jttp.
  <dt><code>--pre-process-script-arg</code>
  <dd>Argument to pass to the <code>--pre-process-script</code>. This can be specified as many times as you need for your script. The args will be passed to the script as an array of Strings to the script in a variable bound to the name <code>args</code>.
  <dt><code>-p,--print what</code>
  <dd>String specifying what the output should contain:
  <dl>
    <dt><code>"H"</code>
    <dd>Request headers
    <dt><code>"B"</code>
    <dd>Request body
    <dt><code>"h"</code>
    <dd>Resposne headers
    <dt><code>"b"</code>
    <dd>Response body
  </dl>
  The default behavior is "hb" (the response headers and body is printed).
  <dt><code>-R,--read-only-session</code>
  <dd>Load the named session, but don't change it when processing the response. Ignored if <code>-s session_name</code> isn't specified.
  <dt><code>-s,--session session_name</code>
  <dd>Create or reuse and update a session. Within a session, headers and cookies set are persisted between requests.

  By default, session files are stored in:

      ${user.home}/.jttp/sessions/<HOST>/<SESSION_NAME>.zip
  
  The zip file contains 2 files: <code>headers.xml</code> and <code>cookies.xml</code>.
  <dt><code>-v,--verbose</code>
  <dd>Print request and response headers and body. Shortcut for <code>-p HBhb</code>.
  <dt><code>-V,--version</code>
  <dd>Shows the version information and exits.
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
  <dd>Either no options were set or the <code>--help</code> or <code>--version</code> option was set. 
</dl>

## FILES

<dl>
  <dt><code>messages_jttp.properties</code>
  <dd>Contains all application messages. Useful for i18n messages for this application. This file needs to be in the same directory as <code>Jttp.java</code>.
  <dt><code>${user.home}/.jttp/downloads</code>
  <dd>Default directory for storing downloaded responses.
  <dt><code>${user.home}/.jttp/scripts</code>
  <dd>Default directory for storing scripts.
  <dt><code>${user.home}/.jttp/sessions</code>
  <dd>Default directory for storing session data.
</dl>

## PREFERENCES

Jttp does not add preferences when it runs. You should use `AddPrefTool.java` to add these preferences under the user root (`-U` which is the default) with the node name `Jttp/<subnode>`. The last qualifier in the preferences documented is the preference name.

<dl>
  <dt><code>user/Jttp/directories/base</code>
  <dd>Base directory for most save information (default is <code>${user.home}/.jttp</code>).
  <dt><code>user/Jttp/directories/downloads</code>
  <dd>Absolute pathname for storing downloads (from using the <code>--download</code> option with no <code>--output</code> option set; default is <code>${base}/downloads</code> where <code>${base}</code> is the value of the base preference above).
  <dt><code>user/Jttp/directories/scripts</code>
  <dd>Directory (under <code>${base}</code>) where scripts are stored (default is <code>${base}/scripts</code>).
  <dt><code>user/Jttp/directories/sessions</code>
  <dd>Directory (under <code>${base}</code>) where session files are stored (default is <code>${base}/sessions</code>).
</dl>

## SYSTEM PROPERTIES

Jttp is implemented with the HttpURLConnection. This means all system properties that apply to it are applicable to Jttp with all desired side-effects.

The Jttp-specific system properties that can be set are:

<dl>
  <dt><code>jttp.indent</code>
  <dd>Number of spaces to indent output when formatting (default is <code>2</code>).
  <dt><code>jttp.keep.tempfiles</code>
  <dd>When <code>true</code>, don't delete any temporary files produced by the run (default is <code>false</code>).
</dl>

Other system properties are also used by internal subsystems:

<dl>
  <dt><code>content.types.user.table</code>
  <dd>Path to a custom filename map (mimetable), used by the <code>java.net.FileNameMap</code> implementation to determine file mimetype by looking at the file extension.
  <dt><code>http.auth.preference</code>
  <dd>One of <code>SPNEGO</code>, <code>digest</code>, <code>NTLM</code>, or <code>basic</code> (which is the default). See the Http Authentication reference below for more information.
  <dt><code>java.security.auth.login.config</code>
  <dd>Path to the JAAS configuration file.
  <dt><code>java.security.krb5.conf</code>
  <dd>Path to the Kerberos configuration file. Ignored unless <code>SPNEGO</code> authentication is specified.
  <dt><code>java.util.logging.config.file</code>
  <dd>Standard Java System Property that points to a logging configuration file.
  <dt><code>javax.net.debug</code>
  <dd>Write SSL debug information to <code>System.err</code>. See the references below related to debugging SSL.
  <dt><code>javax.net.ssl.keyStore</code>
  <dd>Standard Java System Property that points to the keystore to use for client authentication.
  <dt><code>javax.net.ssl.keyStorePassword</code>
  <dd>Standard Java System Property that is the password for the keystore specified by <code>javax.net.ssl.keyStore</code>.
  <dt><code>javax.net.ssl.keyStoreType</code>
  <dd>Standard Java System Property that is the type for the keystore specified by <code>javax.net.ssl.keyStore</code> (default is <code>JKS</code>).
  <dt><code>javax.net.ssl.trustStore</code>
  <dd>Standard Java System Property that points to the truststore to use for server authentication (default is the JRE's <code>cacerts</code> file).
  <dt><code>javax.net.ssl.trustStorePassword</code>
  <dd>Standard Java System Property that is the password for the truststore specified by <code>javax.net.ssl.trustStore</code> (default is changeit).
  <dt><code>javax.net.ssl.trustStoreType</code>
  <dd>Standard Java System Property that is the type for the truststore specified by <code>javax.net.ssl.trustStore</code> (default is <code>JKS</code>).
  <dt><code>javax.security.auth.useSubjectCredsOnly</code>
  <dd>Set to <code>false</code> to make the underlying Kerberos mechanism obtain Kerberos credentials. Jttp does not support a setting of <code>true</code>.
  <dt><code>sun.security.spnego.debug</code>
  <dd>Writes SPNEGO debug messages to <code>System.err</code>. Useful for troubleshooting SPNEGO authentication issues.
  <dt><code>sun.security.krb5.debug</code>
  <dd>Writes Kerberos debug messages to <code>System.err</code>. Useful for troubleshooting SPNEGO authentication issues.
</dl>

## EXAMPLES

Examples are formatted using unix multiline style.

### Common Uses

Generate the request but don't send it.

```bash
java Jttp.java -O http://www.example.com
```

Save session information (useful to keep a cookie-based session going after login).

```bash
java Jttp.java \
  -S my_session \
  https://www.example.com/secure
```

Send a form with a file to upload.

```bash
java Jttp.java \
  -M FORM POST \
  https://www.example.com/expenses \
  user=myUserId \
  statement@Documents/expenses.xlsx
```

Set a custom header.

```bash
java Jttp.java \
  http://www.example.com/ua-sniffer \
  User-Agent:my-custom-ua/1.2.3.4
```

### Using System Properties

Note that you can set system properties in a text file (for example call it `argsfile`), one per line defined as `-Dname=value` as usual and then pass the filename to the `java` command with `@argsfile`. This is a useful shorthand for making repeated requests using the same settings. For illustrative purposes, these examples will specify the system properties on the command line.

Use SPNEGO authentication. Assume a `krb5.conf` file and JAAS configuation file `my-jaas.conf` exist at the specified locations.

```bash
java -Dhttp.auth.preference=SPNEGO \
  -Djava.security.auth.login.config=/secure/my-jaas.conf \
  -Djava.security.krb5.conf=/secure/krb5.conf \
  -Djavax.security.auth.useSubjectCredsOnly=false \
  Jttp.java https://www.example.com/SPNEGO/doc
```

Use your keystore to authenticate your client (note the Url for cryptomix.com is a free service to test client authentication).

```bash
java -Djavax.net.ssl.keyStore=/home/user/.keystore \
  -Djavax.net.ssl.keyStorePassword=changeit \
  Jttp.java https://server.cryptomix.com/secure/
```

### Scripts

You can specify scripts to run before and after the actual HTTP request is run. You have to add the scripting language jars to the classpath. If you specify the same script name for both `--pre-process-script` and `--post-process-script`, they are executed separately using separate instances of the underlying Java scripting objects. This example shows how to tell Jttp to run a script called `pre-auth.groovy` before running the HTTP request. You can specify scripts written with different languages for both scripts.

```bash
java -cp [path to scripting language jar(s)] \
  Jttp.java \
  --pre-process-script pre-auth.groovy \
  https://authed.example.com/secure
```

You can specify options to pass to the `--pre` or `--post` processing scripts.

```bash
java -cp [path to scripting language jar(s)] \
  Jttp.java \
  --pre-process-script pre-auth.groovy \
  --pre-process-script-arg "--userid bob" \
  --pre-process-script-arg "--keystore /home/bob/.keystore" \
  https://authed.example.com/secure
```

And you can specify scripts to run both before and after the HTTP request has been made.

```bash
java -cp [path to scripting language jar(s)] \
  Jttp.java \
  --pre-process-script pre-auth.groovy \
  --pre-process-script-arg "--userid bob" \
  --pre-process-script-arg "--keystore /home/bob/.keystore" \
  --post-process-script post-dbload.groovy \
  --post-process-script-arg "--jdbcurl jdbc:hsqldb:mem:mydb" \
  --post-process-script-arg "--jdbcuser sa" \
  --post-process-script-arg an_arg \
  https://authed.example.com/secure
```

## NOTES

Jttp was inspired by and has several features and options copied from [httpie](https://httpie.org). The motivation for Jttp was to implement a client using the venerable [HttpURLConnection](https://docs.oracle.com/en/java/javase/13/docs/api/java.base/java/net/HttpURLConnection.html) to demonstrate how to use it effectively. The HttpURLConnection is versatile and takes advantage of many built-in features to the Java Runtime through system properties including SPNEGO authentication, client certificate authentication, basic authentication, and custom MIME-type databases. Since the intended use for Jttp is as a command line program, system properties are well-suited for this purpose. StackOverflow provides an [excellent essay](https://stackoverflow.com/a/2793153/37776) on how to use HttpURLConnection the way it was intended. This shows that the API does not necessarily have to mirror the actual conversation that takes place between clients and servers but rather acts as a way to send and receive data to process.

Jttp will download all responses to the `java.io.tmpdir` location (usually `$TMPDIR`). It will then either read from the file locally to produce output that can be formatted for indentation and color or copy the file to the `downloads` directory. The temporary files are deleted at the end of the run unless the `jttp.keep.tempfiles` system property is specified with a value of `true`. Keeping the temporary files is useful for debugging certain issues that can arise during execution. Most of the time, they should just be thrown away.

Jttp binds an object to the scripting engine called `jttpScriptObject` that gives scripts access to:

* The HttpURLConnection (through the `getHttpURLConnection` method) in the curret state it's in depending on when the script was executed. If it is executed before the HTTP request is run, you can set headers, etc. You don't have to set the method since Jttp does that for you. If it is executed after the HTTP request is run, you have access to the response headers. It's best practice to use scripting to set headers before the HTTP request is run. It's best practice to not modify any responses or response data in the HttpURLConnection after the HTTP request is run.
* The temporary response file (through the `getResponseFile` method), but only in scripts run after the HTTP request is run. It's best practice to use the temporary file to read and process but not modify.
* A logger (through the `log` method) that will write log messages to the logger used by Jttp at `INFO` level.

## SEE ALSO

* [Http Authentication](https://docs.oracle.com/javase/8/docs/technotes/guides/net/http-auth.html)
* [(Kerberos) Troubleshooting](https://docs.oracle.com/javase/8/docs/technotes/guides/security/jgss/tutorials/Troubleshooting.html)
* [Java Logging Configuration](https://docs.oracle.com/javase/8/docs/technotes/guides/logging/overview.html)
* [Debugging SSL/TLS Connections](https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/ReadDebug.html)
* [(SSL) Debugging Utilities](https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#Debug)
* [How to use java.net.URLConnection to fire and handle HTTP requests?](https://stackoverflow.com/a/2793153/37776)
* [Package javax.script](https://docs.oracle.com/en/java/javase/13/docs/api/java.scripting/javax/script/package-summary.html)

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.

### Known Issues

* Support for the <code>PATCH</code> method is server dependent.
* HTTP/2 is not supported.
* Asynchronous requests are not supported.
* Rendering issues will occur when <code>--pretty-print INDENT</code> or <code>--pretty-print ALL</code> or the default settings are set depending on the document received. This is due to the formatting done by Jttp and can be adjusted (you have the code in front of you and I love reviewing pull requests).
