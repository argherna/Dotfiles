/*bin/mkdir -p /tmp/.java/classes 2> /dev/null
if [[ "$#" -lt 2 ]]; then
  cat <<ENDOFHELP
Tests JDBC4 Connectivity with a database by printing database information.

Usage: bash $(basename $0) <path-to-jdbc-jar> <options>

<path-to-jdbc-jar>  Fully-qualified path to JDBC4 driver jar.
<options>           Program options (run with -h flag for details).  
ENDOFHELP
  exit 1
fi

# Compile the program.
#
javac -d /tmp/.java/classes $0

# Run the compiled program only if compilation succeeds.
#
if [[ $? -eq 0 ]]; then
  JDBC_JAR=$1
  shift
  java -cp $JDBC_JAR:/tmp/.java/classes $(basename ${0%.*}) "$@"
fi
exit
*/
import java.io.Console;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;

/**
 * Connects to a database using command-line arguments and display information
 * if successful.
 */
class JdbcConnectTest {

  /**
   * Main method.
   */
  public static void main(String... args) {

    if (args.length == 0) {
      showUsageAndExit(2);
    }

    int argIdx = 0;
    String username = null;
    String password = null;
    String databaseUrl = null;
    boolean quiet = false;

    while (argIdx < args.length) {
      String arg = args[argIdx];
      switch (arg) {
        case "-h":
          showUsageAndExit(2);
          break;
        case "-q":
          quiet = true;
          break;
        case "-U":
          databaseUrl = args[++argIdx];
          break;
        case "-u":
          username = args[++argIdx];
          break;
        case "-w":
          password = args[++argIdx];
          break;
        default:
          System.err.printf("Unknown option %s%n", arg);
          showUsageAndExit(1);
          break;
      }
      argIdx++;
    }

    if (isNullOrEmpty(databaseUrl)) {
      System.err.printf("No -U option set!%n");
      showUsageAndExit(1);
    }

    if (isNullOrEmpty(username)) {
      System.err.printf("No -u option set!%n");
      showUsageAndExit(1);
    }

    if (isNullOrEmpty(password)) {
      password = new String(getPassword("user's"));
      if (isNullOrEmpty(password)) {
        System.err.println("No password given!");
        showUsageAndExit(1);
      }
    }

    try {
      try (Connection conn = DriverManager.getConnection(databaseUrl, username, 
             password)) {
        DatabaseMetaData metadata = conn.getMetaData();
        if (quiet) {
          System.out.println("OK");
        } else {
          System.out.printf("Connected to %s %s%n", metadata.getDatabaseProductName(), 
            metadata.getDatabaseProductVersion());
          System.out.println();
          System.out.printf("               URL: %s%n", databaseUrl);
          System.out.printf("          Username: %s%n", username);
          System.out.printf(" JDBC Driver class: %s %s%n", 
            metadata.getDriverName(), metadata.getDriverVersion());
        }
      }
    } catch (Exception e) {
      System.err.printf("Error, exiting! %s%n", e.getMessage());
      System.exit(1);
    }
  }

  private static void showUsageAndExit(int status) {
    showUsage();
    System.exit(status);
  }

  private static void showUsage() {
    System.err.printf("Usage: %s [OPTIONS]%n", JdbcConnectTest.class.getName());
    System.err.println();
    System.err.println("Tests JDBC4 connectivity to a database for a given set of credentials.");
    System.err.println();
    System.err.println("Options:");
    System.err.println();
    System.err.println(" -h                Show this help and exit");
    System.err.println(" -q                Prints OK if set, otherwise print database info");
    System.err.println(" -U <database-url> Database Url");
    System.err.println(" -u <username>     Username for database");
    System.err.println(" -w <password>     Password for database (prompted if not set)");
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
