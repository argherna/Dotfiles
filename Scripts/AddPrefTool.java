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

import java.util.prefs.NodeChangeEvent;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

class AddPrefTool {
  
  public static void main(String... args) {

    if (args.length == 0) {
      showUsageAndExit(2);
    }

    int argIdx = 0;
    String classname = "";
    String name = "";
    String nodename = "";
    boolean systemRoot = false;
    PrefType type = PrefType.STRING;
    String value = "";
    boolean verbose = false;

    while (argIdx < args.length) {
      String arg = args[argIdx];
      switch (arg) {
        case "-C":
        case "--class":
          classname = nextStringArgValue(arg, ++argIdx, args);
          break;
        case "-h":
        case "--help":
          showUsageAndExit(2);
          break;
        case "-N":
        case "--node-name":
          nodename = nextStringArgValue(arg, ++argIdx, args);
          break;
        case "-n":
        case "--name":
          name = nextStringArgValue(arg, ++argIdx, args);
          break;
        case "-S":
        case "--system-root":
          systemRoot = true;
          break;
        case "-t":
        case "--type":
          try {
            type = PrefType.valueOf(nextStringArgValue(arg, ++argIdx, args).toUpperCase());
          } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
          }
          break;
        case "-U":
        case "--user-root":
          systemRoot = false;
          break;
        case "-V":
        case "--verbose":
          verbose = true;
          break;
        case "-v":
        case "--value":
          value = nextStringArgValue(arg, ++argIdx, args);
          break;
      }
      argIdx++;
    }

    if (isNullOrEmpty(name)) {
      System.err.println("--name not set!");
      showUsageAndExit(1);
    }

    if (isNullOrEmpty(value)) {
      System.err.println("--value not set!");
      showUsageAndExit(1);
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
  
    if (verbose) {
      p.addNodeChangeListener(new NodeChangeListener(){
      
        @Override
        public void childRemoved(NodeChangeEvent evt) {
          // No-op
        }
      
        @Override
        public void childAdded(NodeChangeEvent evt) {
          System.err.println("Added node " + evt.getChild().name());
        }
      });

      p.addPreferenceChangeListener(new PreferenceChangeListener(){
      
        @Override
        public void preferenceChange(PreferenceChangeEvent evt) {
          System.err.println("Add[" + evt.getNode().name() + ": " + 
            evt.getKey() + " = " + evt.getNewValue() + "]");
        }
      });
    }

    try {
      switch (type) {
        case BOOLEAN:
          p.putBoolean(name, Boolean.valueOf(value));
          break;
        case DOUBLE:
          p.putDouble(name, Double.valueOf(value));
          break;
        case FLOAT:
          p.putFloat(name, Float.valueOf(value));
          break;
        case INT:
          p.putInt(name, Integer.valueOf(value));
          break;
        case LONG:
          p.putLong(name, Long.valueOf(value));
          break;
        case STRING:
          p.put(name, value);
          break;
      }
    } catch (Exception e) {
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
    System.err.printf("Usage: %s [OPTIONS] %n", AddPrefTool.class.getName());
    System.err.println();
    System.err.println("Adds preferences to the named store (default is user)");
    System.err.println();
    System.err.println("Options:");
    System.err.println();
    System.err.println(" -C, --class <name>");
    System.err.println("               Specify the fully qualified class name for the preference node");
    System.err.println(" -h, --help    Show this help and exit");
    System.err.println(" -N, --node-name");
    System.err.println("               Set the name for this preference node");
    System.err.println(" -n, --name <prefname>");
    System.err.println("               Preference name");
    System.err.println(" -S, --system-root");
    System.err.println("               Add preferences to the system root");
    System.err.println(" -t, --type <typename>");
    System.err.println("               Preference type (boolean, double, float, int, long, string)");
    System.err.println(" -U, --user-root");
    System.err.println("               Add preferences to the system root");
    System.err.println(" -v, --value <prefvalue>");
    System.err.println("               Preference value");
    System.err.println();
    System.err.println("NOTES:");
    System.err.println(" - To use the --class/-C option, the specified class must be on the classpath.");
    System.err.println(" - Only one of -C or -N can be set.");
    System.err.println(" - Only one of -S or -U can be set (-U is assumed if no option is set).");
    System.err.println(" - String is assumed if no -t option is set.");
    System.err.println(" - If -N is not set, the root (user or system) is assumed.");
  }

  private static boolean isNullOrEmpty(String s) {
    return (s == null || s.isEmpty());
  }

  private static boolean isNotNullAndEmpty(String s) {
    return (s != null && !s.isEmpty());
  }

  private static enum PrefType {
    BOOLEAN, DOUBLE, FLOAT, INT, LONG, STRING;
  }
}