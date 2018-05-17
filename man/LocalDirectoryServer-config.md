# LocalDirectoryServer-config

## NAME

`LocalDirectoryServer-config` - LocalDirectoryServer configuration.

## DESCRIPTION

LocalDirectoryServer uses different, non-overriding methods for configuration:

* Command-line arguments (see `LocalDirectoryServer.java`).
* System properties for JRE and SSL settings.
* Preferences for data that can be changed but should be persisted between runs.

## FILES

These files are the default values and are used only if HTTPS is needed. See the SYSTEM PROPERTIES section for overriding these defaults.

<dl>
  <dt><code>${user.home}/.keystore</code>
  <dd>Default keystore file containing the server key under the alias <code>&lt;mykey&gt;</code>.
  <dt><code>${java.home}/lib/security/cacerts</code>
  <dd>Default trust store used by the server.
</dl>

## PREFERENCES

CORS support is enabled through using preferences. See the Preferences API below for more details, and `ImportPrefsTool.java`.

Each preference below is the name of a node. The key-value pairs are listed after. These values are persisted between runs. Sometimes a run will have no use for these preferences. All preferences are read-only in `LocalDirectoryServer.java`.

<dl>
  <dt><code>user/LocalDirectoryServer/corsSupport/&lt;SERVER-PATH&gt;</code>
  <dd>Only 1 <code>&lt;SERVER-PATH&gt;</code> can be saved at a time. You can save as many different <code>&lt;SERVER-PATH&gt;</code> nodes as you like.<table>
    <tr>
      <th>Key</th>
      <th>Value</th>
    </tr>
    <tr>
      <td>directory</td>
      <td>The directory being served (specified on the   command line)</td>
    </tr>
    <tr>
      <td>headers</td>
      <td>CORS headers returned for this <code>&lt;SERVER-PATH&gt;</code> served out of this directory. Multiple headers are separated by a comma and no space.</td> 
    </tr>
    <tr>
      <td>allowedOrigins</td>
      <td>Optional allowed origin sites. Default is <code>*</code>.</td> 
    </tr>
    <tr>
      <td>MIMETypes</td>
      <td>MIME types to have CORS applied to it.</td>
    </tr>
  </table>
</dl>

## SYSTEM PROPERTIES

<dl>
  <dt><code>java.util.logging.config.file</code>
  <dd>Standard Java System Property that points to a logging configuration file.
  <dt><code>javax.net.ssl.keyStore</code>
  <dd>Overrides the default keystore file containing the server key under the alias <code>&lt;mykey&gt;</code>.
  <dt><code>javax.net.ssl.keyStorePassword</code>
  <dd>Overrides the default keystore password of <code>changeit</code>.
  <dt><code>javax.net.ssl.keyStoreType</code>
  <dd>Overrides the default keystore type. The default keystore type is <code><a href="https://docs.oracle.com/javase/10/docs/api/java/security/KeyStore.html#getDefaultType()">Keystore.getDefaultType()</a></code>.
  <dt><code>javax.net.ssl.trustStore</code>
  <dd>Overrides the default trust store used by the server.
  <dt><code>javax.net.ssl.trustStorePassword</code>
  <dd><dd>Overrides the default trust store password of <code>changeit</code>.
  <dt><code>javax.net.ssl.trustStoreType</code>
  <dd>Overrides the default trust store type. The default keystore type is <code><a href="https://docs.oracle.com/javase/10/docs/api/java/security/KeyStore.html#getDefaultType()">Keystore.getDefaultType()</a></code>.
</dl>

## NOTES

These loggers can be configured in a logging configuration file:

- `LocalDirectoryServer`
- `com.sun.net.httpserver`

Logging typically happens at `FINE` or higher.

## SEE ALSO

* [CORS](https://www.w3.org/TR/cors/)
* `ExportPrefsTool.java`
* `ImportPrefsTool.java`
* [Java Logging Configuration](https://docs.oracle.com/javase/7/docs/technotes/guides/logging/overview.html)
* [Java Preferences API](https://docs.oracle.com/javase/10/docs/api/java/util/prefs/Preferences.html)
* `LocalDirectoryServer.java`
* [Keystore](https://docs.oracle.com/javase/10/docs/api/java/security/KeyStore.html) API documentation

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>
