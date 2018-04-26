# AddPrefTool.java

## NAME

`AddPrefTool.java` - Adds a Java Preference to the local preferences store.

## SYNOPSIS

```bash
    bash AddPrefTool.java [OPTIONS]
```

## DESCRIPTION

Adds a Java Preference to the local preferences store. Preferences follow the Java standard (see the [Preferences API](https://docs.oracle.com/javase/10/docs/api/java/util/prefs/Preferences.html) for background).

Single key-value pairs can be added to the **user root** (the default) or **system root**. If a **node name** is specified, then the key-value pair is added to that node.

Nearly all Preference types supported by the Preferences API are allowed except for byte-arrays at this time.

## ARGUMENTS

<dl>
<dt>`-C <name>`</dt>
<dt>`--class <name>`</dt>
<dd>Specify the fully qualified class name for the preference node.</dd>
</dl>

## OPTIONS

## PREFERENCES

## SYSTEM PROPERTIES

## ENVIRONMENT

## NOTES

* When specifying the `-C`|`--class` option, the class has to be on the classpath when `java` is invoked.

## AUTHOR

## ISSUES
