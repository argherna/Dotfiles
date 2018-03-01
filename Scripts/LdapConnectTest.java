/*bin/mkdir -p /tmp/.java/classes 2> /dev/null

# Compile the program.
#
javac -d /tmp/.java/classes $0

# Run the compiled program only if compilation succeeds.
#
[[ $? -eq 0 ]] && java -cp /tmp/.java/classes $(basename ${0%.*}) "$@"
exit
*/
import java.io.Console;
import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;

/**
 * Connects to an LDAP server using command-line arguments and display 
 * information if successful.
 * 
 * <p>
 * Command line arguments are similar to ldapsearch in intent.
 */
class LdapConnectTest {

  /**
   * Main method
   */
  public static void main(String... args) {

    if (args.length == 0) {  
      showUsageAndExit(2);
    }

    int argIdx = 0;
    String bindDn = null;
    String ldapUrl = null;
    String password = null;
    boolean promptForPassword = false;
    boolean useStartTls = false;

    while (argIdx < args.length) {
      String arg = args[argIdx];
      switch (arg) {
        case "-D":
          bindDn = args[++argIdx];
          break;
        case "-h":
        case "--help":
          showUsageAndExit(2);
          break;
        case "-H":
          ldapUrl = args[++argIdx];
          break;
        case "-w":
          password = args[++argIdx];
          break;
        case "-W":
          promptForPassword = true;
          break;
        case "-Z":
          useStartTls = true;
          break;
        default:
          System.err.printf("Unknown option %s%n", arg);
          showUsageAndExit(1);
          break;
      }
      argIdx++;
    }

    if (isNullOrEmpty(bindDn)) {
      System.err.println("-D not set!");
      showUsageAndExit(1);
    }

    if (isNullOrEmpty(ldapUrl)) {
      System.err.println("-H not set!");
      showUsageAndExit(1);
    }

    if (promptForPassword && isNullOrEmpty(password)) {
      password = new String(getPassword("bind"));
    }

    if (isNullOrEmpty(password)) {
      System.err.println("Password not set!");
      showUsageAndExit(1);
    }

    Hashtable<String, String> env = new Hashtable<>();
    env.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
    env.put("java.naming.provider.url", ldapUrl);

    Exception ex = null;
    try {
      LdapContext ldapContext = new InitialLdapContext(env, null);
      if (useStartTls) {
        StartTlsResponse tls =
          (StartTlsResponse) ldapContext.extendedOperation(new StartTlsRequest());
        tls.negotiate();
        addToContextEnvironment(bindDn, password, ldapContext);
        ldapContext.getAttributes("");
        tls.close();
      } else {
        addToContextEnvironment(bindDn, password, ldapContext);
        ldapContext.getAttributes("");
      }
    } catch (Exception e) {
      ex = e;
    } finally {
      if (ex != null) {
        System.err.printf("Error! %s%n", ex.getMessage());
        System.exit(1);
      } else {
        System.out.println("OK");
      }
    }
  }

  private static void addToContextEnvironment(String bindDn, String password, 
    LdapContext ldapContext) throws NamingException {
    ldapContext.addToEnvironment("java.naming.security.authentication", 
      "simple");
    ldapContext.addToEnvironment("java.naming.security.principal", bindDn);
    ldapContext.addToEnvironment("java.naming.security.credentials", password);
  }

  private static void showUsageAndExit(int status) {
    showUsage();
    System.exit(status);
  }

  private static void showUsage() {
    System.err.printf("Usage: %s [OPTIONS]%n", LdapConnectTest.class.getName());
    System.err.println();
    System.err.println("Tests LDAP connectivity for a given set of credentials.");
    System.err.println();
    System.err.println("Options:");
    System.err.println();
    System.err.println(" -D binddn        bind DN");
    System.err.println(" -h, --help       Show this help and exit");
    System.err.println(" -H URL           LDAP URL");
    System.err.println(" -w <password>    Bind password");
    System.err.println(" -W               Prompt for password");
    System.err.println(" -Z               Use StartTLS");
  }

  private static char[] getPassword(String passwordType) {
    // Set a default password just in case there's no Console available.
    char[] password = "abcd1234".toCharArray();
    Console c = System.console();
    if (c == null) {
      System.err.println("System console not available!");
    } else {
      password = c.readPassword("Enter %s password:  ", passwordType);
    }
    return password;
  }

  private static boolean isNullOrEmpty(String value) {
    return (value == null || (value != null && value.isEmpty()));
  }
}