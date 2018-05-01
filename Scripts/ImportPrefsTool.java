/*bin/mkdir -p /tmp/.java/classes 2> /dev/null

# Compile the program.
#
javac -d /tmp/.java/classes $0

# Run the compiled program only if compilation succeeds.
#
[[ $? -eq 0 ]] && java \
  -cp /tmp/.java/classes $(basename ${0%.*}) "$@"
exit
*/

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.prefs.Preferences;

class ImportPrefsTool {

  public static void main(String... args) {

    String filename = "";
    if (args.length > 0) {
      if (args[0].equals("-h")) {
        showUsageAndExit(2);
      } else {
        filename = args[0];
      }
    }

    InputStream in = System.in;
    if (isNotNullAndEmpty(filename)) {
      try {
        in = new FileInputStream(filename);
      } catch (IOException e) {
        System.err.println(e.getMessage());
        System.exit(1);
      }
    }

    try {
      Preferences.importPreferences(in);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  private static void showUsageAndExit(int status) {
    showUsage();
    System.exit(status);
  }

  private static void showUsage() {
    System.err.printf("Usage: %s <filename>%n", ImportPrefsTool.class.getName());
    System.err.println();
    System.err.println("Imports preferences from XML.");
    System.err.println();
    System.err.println("Arguments:");
    System.err.println();
    System.err.println("  <filename>   Optional file name to read the preferences from.");
    System.err.println();
    System.err.println("NOTES:");
    System.err.println(" - If <filename> is not specified, preferences XML are read from System.in.");
   
  }

  private static boolean isNotNullAndEmpty(String s) {
    return (s != null && !s.isEmpty());
  }
}
