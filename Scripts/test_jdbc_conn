/*usr/bin/env jrunscript \
-cp /path/to/jdbc/driver.jar \
-f "$0" $*
exit
*/

/**
 * NAME
 *
 *     test_pgsql_connection
 *
 * SYNOPSIS
 *
 *     jrunscript -cp /path/to/postgresql.jar -f test_pgsql_connection [arg]
 *
 * PARAMETERS
 *
 *     argument
 *       If set, path to a properties file containing connection properties.
 *
 * DESCRIPTION
 * 
 *     test_pgsql_connection attempts to connect to a PostgreSQL database using
 *     the properties specified in the given properties file. If no properties
 *     file is specifed, the default is 'conf/postgresql.properties'.
 *
 *     The property names in the properties file must be:
 *
 *       jdbc.url
 *       jdbc.username
 *       jdbc.password
 *
 *     The PostgreSQL driver must also be on the classpath as described in the
 *     SYNOPSIS. Failure to include it will cause the script to fail.
 *
 *     When a connection succeeds, the script will print out the database
 *     information. When a connection fails, the script will print an error
 *     message.
 *
 * EXIT STATUS
 *
 *     0 
 *         connection established successfully.
 *
 *     1 
 *         connection failed.
 *
 *     2
 *         properties file not found.
 *
 * SEE ALSO
 *
 *     This script is based on the documentation at 
 *     <URL:http://jdbc.postgresql.org/documentation/91/connect.html>
 */ 
importPackage(java.io);
importPackage(java.lang);
importPackage(java.sql);
importPackage(java.util);
importPackage(org.postgresql);

var jdbc_props_filename = "jdbc_test.properties";
if (arguments[0] != null) {
    jdbc_props_filename = arguments[0];
} 

var c = null;
var exitCode = 1;
var except = null;
var inputStream = null;
try {

    inputStream = new FileInputStream(jdbc_props_filename);
    var jdbc_props = new Properties();
    jdbc_props.load(inputStream);
    c = DriverManager.getConnection(
                jdbc_props.getProperty("jdbc.url"), 
                jdbc_props.getProperty("jdbc.username"),
                jdbc_props.getProperty("jdbc.password"));

    var meta = c.getMetaData();
    System.out.print("Connected to: ");
    System.out.println(meta.getDatabaseProductName() + " " +
                meta.getDatabaseProductVersion());
    System.out.print("         URL: ");
    System.out.println(meta.getURL());
    System.out.print("    Username: ");
    System.out.println(meta.getUserName());
    System.out.print(" JDBC Driver: ");
    System.out.println(meta.getDriverName() + " " + meta.getDriverVersion());

} catch (e) {
    
    except = e.javaException;

} finally {

    
    if (except != null) {

        if (except instanceof IOException) {

            println(except.getMessage());
            exitCode = 2;

        } else if (except instanceof SQLException) {

            for (var iter = except.iterator(); iter.hasNext();) {
                var e = iter.next();
                System.err.println(e.getMessage());
                if (e instanceof SQLException) 
                    System.err.println("SQLSTATE: " + except.getSQLState());
            }
            
            exitCode = 1;
        }

    } else if (c != null) {

        c.close();
        exitCode = 0;

    }

}
System.exit(exitCode);
