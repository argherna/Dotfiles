# JavadocServer.java

## NAME

`JavadocServer.java` - display Javadoc pages in zip files from either a local maven repository or from installed JDKs.

## SYNOPSIS

```bash
    bash JavadocServer.java [port]
```

## DESCRIPTION

JavadocServer displays Javadoc pages that are in zip files from either a local Maven repository or from installed JDKs. Start the program and navigate to `http://localhost:port/`. 

The server will display a getting started page when it is run for the first time with a sample of the preferences XML you can use to edit and import to the local preferences store. The preferences are used so that there are no configuration files to edit (very rarely) or screw up. Use the `ImportPrefsTool.java` utility to import the XML file. See the Preferences section below for more details.

## ARGUMENTS

<dl>
  <dt><code>port</code>
  <dd>Port the server will listen on (default is <em>8084</em>)
</dl>

## OPTIONS

<dl>
  <dt><code>-h</code>
  <dd>shows a help message and exits
</dl>

## EXIT STATUS

<dl>
  <dt><code>0</code>
  <dd>Successful run.
  <dt><code>1</code>
  <dd>An exception was thrown/raised.
  <dt><code>2</code>
  <dd>Either no options were set or the <code>-h</code> option was set. 
</dl>

## PREFERENCES

A sample XML document for the preferences you can import using `ImportPrefsTool.java` is as follows:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE preferences SYSTEM "http://java.sun.com/dtd/preferences.dtd">
<preferences EXTERNAL_XML_VERSION="1.0">
  <root type="user">
    <map/>
    <node name="JavadocServer">
      <map/>
      <node name="jdk-docs">
        <map>
          <entry key="8" value="/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jdk-8u161-docs-all.zip"/>
          <entry key="9" value="/Library/Java/JavaVirtualMachines/jdk-9.0.4.jdk/Contents/Home/jdk-9.0.4_doc-all.zip"/>
          <entry key="10" value="/Library/Java/JavaVirtualMachines/jdk-10.jdk/Contents/Home/jdk-10_doc-all.zip"/>
        </map>
      </node>
      <node name="m2-repos">
        <map>
          <entry key="default" value="/Users/username/.m2/repository"/>
        </map>
      </node>
    </node>
  </root>
</preferences>
```

The entry keys for `jdk-docs` are completely arbitrary, but it's recommended to use the version of the JDK the docs are for because it will become part of the URL that the server will use for the documents it serves. For example, to read JDK 8 documents with these preference keys, the URL `http://localhost:8084/jdk/8/docs/api/index.html` would bring up the index page showing all the packages in JDK 8.

<dl>
  <dt><code>user/JavadocServer/jdk-docs</code>
  <dd>Map of JDK javadoc archives. For each entry, the key is used in the URL of the pages served. The value is the fully-qualified path to the zip file with the Javadoc files.
  <dt><code>user/JavadocServer/m2-repos</code>
  <dd>Map of local Maven repositories. Only 1 entry on the map is supported at this time with a key named "default". The value should be the path to the local Maven repository as shown in the example.
</dl>

## SYSTEM PROPERTIES

<dl>
  <dt><code>java.util.logging.config.file</code>
  <dd>Standard Java System Property that points to a logging configuration file.
</dl>

## NOTES

These loggers can be configured in a logging configuration file:

- `JavadocServer`
- `com.sun.net.httpserver`

Logging typically happens at `FINE` or higher.

## SEE ALSO

- `ExportPrefsTool.java`
- `ImportPrefsTool.java`
- [Java Logging Configuration](https://docs.oracle.com/javase/7/docs/technotes/guides/logging/overview.html)

## AUTHOR

Andy Gherna <mailto: argherna@gmail.com>

## ISSUES

### Reporting

Report issues at https://github.com/argherna/Dotfiles/issues.

### Known Issues

- Only 1 local maven repository is supported
- HTTPS is not supported

