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
        case "-T [":
          try {
            type = PrefType.valueOf(nextStringArgValue(arg, ++argIdx, args).toUpperCase());
          } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
          }
          break;
        case "-U":
          systemRoot = false;
          break;
        case "-V":
          verbose = true;
          break;
        case "-v":
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
    System.err.println("Adds preferences to the named root (default is user)");
    System.err.println();
    System.err.println("Options:");
    System.err.println();
    System.err.println(" -C <classname> Specify the fully qualified class name for the preference node");
    System.err.println(" -h             Show this help and exit");
    System.err.println(" -N <nodename>  Set the name for this preference node");
    System.err.println(" -n <prefname>  Preference name");
    System.err.println(" -S             Add preferences to the system root");
    System.err.println(" -T <typename>  Preference type (boolean, double, float, int, long, string)");
    System.err.println(" -U             Add preferences to the system root");
    System.err.println(" -V             Verbose output");
    System.err.println(" -v <prefvalue> Preference value");
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
