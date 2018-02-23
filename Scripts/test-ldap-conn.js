/*usr/bin/env jrunscript \
-f "$0" $*
exit
*/

/**
 *
 * NAME
 *
 *     test_ldap_conn
 *
 * SYNOPSIS
 *
 *     jrunscript -f test_ldap_conn [props_file]
 *
 * PARAMETERS
 *
 *     props_file
 *         name of file that contains ldap connection properties. If not 
 *         specified, default ./ldap.properties is used.
 *
 * DESCRIPTION
 *
 *     Connects to the named ldap server and reports any errors encountered.
 *
 * EXIT STATUS
 *
 *     0
 *         Connection was successful.
 *
 *     1
 *         Connection failed.
 *
 *     2
 *         Connection properties file not found.
 *
 * SEE ALSO
 *
 *     <URL:http://docs.oracle.com/javase/7/docs/api/javax/naming/Context.html>
 *         Names the properties to set in the connection properties file.
 *
 */

importPackage(java.io);
importPackage(java.lang);
importPackage(java.util);
importPackage(javax.naming);
importPackage(javax.naming.directory);
importPackage(javax.naming.ldap);

var ldap_props_filename = "ldap_test.properties";

if (arguments[0] != null) {
    ldap_props_filename = arguments[0];
}

var ctx = null;
var exitCode = 1;
var except = null;
var inputStream = null;

try {

    inputStream = new FileInputStream(ldap_props_filename);
    var ldap_props = new Properties();
    ldap_props.load(inputStream);

    env = new Hashtable();
    
    env.put("java.naming.factory.initial", ldap_props.get("java.naming.factory.initial"));
    env.put("java.naming.provider.url", ldap_props.get("java.naming.provider.url"));
    env.put("java.naming.security.authentication" , ldap_props.get("java.naming.security.authentication"));
    env.put("java.naming.security.principal", ldap_props.get("java.naming.security.principal"));
    env.put("java.naming.security.credentials", ldap_props.get("java.naming.security.credentials"));

    ctx = new InitialLdapContext(env, null);
    controls = new SearchControls();
    controls.setSearchScope( SearchControls.SUBTREE_SCOPE);
    ctx.search(ldap_props.get("ldap.baseDN"), ldap_props.get("ldap.search.filter"), controls);
    
    println("OK");

} catch (e) {

    except = e.javaException;

} finally {

    if (except != null) {

        if (except instanceof IOException) {

            println(except.getMessage());
            exitCode = 2;

        } else if (except instanceof NamingException) {

            println("Connection failure! " + except.getMessage());
            exitCode = 1;
            
        } else {

            println(except.getMessage());
            exitCode = 1;
        }

    } else if (ctx != null) {

        ctx.close();
        exitCode = 0;

    } else {

	println("Unknown state!");
	exitCode = 2;
	
    }
}
System.exit(exitCode);
