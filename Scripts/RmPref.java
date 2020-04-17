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

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

class RmPref {
  
  public static void main(String... args) {

    if (args.length == 0) {
      showUsageAndExit(2);
    }

    int argIdx = 0;
    String classname = "";
    String name = "";
    String nodename = "";
    boolean systemRoot = false;

    while (argIdx < args.length) {
      String arg = args[argIdx];
      switch (arg) {
        case "-C":
          classname = nextStringArgValue(arg, ++argIdx, args);
          break;
        case "-h":
          showUsageAndExit(2);
          break;
        case "-N":
          nodename = nextStringArgValue(arg, ++argIdx, args);
          break;
        case "-n":
          name = nextStringArgValue(arg, ++argIdx, args);
          break;
        case "-S":
          systemRoot = true;
          break;
        case "-U":
          systemRoot = false;
          break;
      }
      argIdx++;
    }

    Preferences p = null;
    if (isNotNullAndEmpty(classname)) {
      Class<?> c = null;
      try {
        c = Class.forName(classname);
      } catch (Exception e) {
        System.err.println(e.getMessage());
        System.exit(1);
      }
      p = systemRoot ? Preferences.systemNodeForPackage(c) : 
        Preferences.userNodeForPackage(c);
    } else if (isNotNullAndEmpty(nodename)) {
      p = systemRoot ? Preferences.systemRoot().node(nodename) :
        Preferences.userRoot().node(nodename);
    } else {
      p = systemRoot ? Preferences.systemRoot() : Preferences.userRoot();
    }

    try {
      if (isNotNullAndEmpty(name)) {
        p.remove(name);
      } else {
        p.removeNode();
      }
    } catch (BackingStoreException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  private static String nextStringArgValue(String arg, int argIdx, String[] args) {
    if (argIdx >= args.length) {
      System.err.printf("%s needs an argument!%n", arg);
      showUsageAndExit(1);
    }
    return args[argIdx];
  }

  private static void showUsageAndExit(int status) {
    showUsage();
    System.exit(status);
  }

  private static void showUsage() {
    System.err.printf("Usage: %s [OPTIONS] %n", RmPref.class.getName());
    System.err.println();
    System.err.println("Removes preferences from the named root (default is user)");
    System.err.println();
    System.err.println("Options:");
    System.err.println();
    System.err.println(" -C <name>      Specify the fully qualified class name for the preference node");
    System.err.println(" -h             Show this help and exit");
    System.err.println(" -N             Set the name for this preference node");
    System.err.println(" -n <prefname>  Preference name");
    System.err.println(" -S             Add preferences to the system root");
    System.err.println(" -U             Add preferences to the system root");
    System.err.println();
    System.err.println("NOTES:");
    System.err.println(" - To use the -C option, the specified class must be on the classpath.");
    System.err.println(" - Only one of -C or -N can be set.");
    System.err.println(" - Only one of -S or -U can be set (-U is assumed if no option is set).");
    System.err.println(" - String is assumed if no -t option is set.");
    System.err.println(" - If -N is not set, the root (user or system) is assumed.");
  }

  private static boolean isNotNullAndEmpty(String s) {
    return (s != null && !s.isEmpty());
  }
}
