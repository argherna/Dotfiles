/*bin/mkdir -p /tmp/.java/classes 2> /dev/null

# Compile the program.
#
javac -d /tmp/.java/classes $0

# Run the compiled program only if compilation succeeds.
#
[[ $? -eq 0 ]] && java -cp /tmp/.java/classes $(basename ${0%.*}) "$@"
exit
*/
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Removes a preference key or node.
 * 
 * <p>
 * Set the system property {@code rmpref.showtraces} to {@code true} to show stack traces from
 * exceptions.
 */
class RmPref implements Runnable {

  private static final Boolean SHOWTRACES = Boolean.getBoolean("rmpref.showtraces");

  private boolean nodeIsClass = false;

  private boolean systemRoot = false;

  private String nodename;

  private String key;

  public static void main(String... args) {
    try {
      var program = args.length > 0 ? parseArgs(new RmPref(), args) : null;
      if (isNull(program)) {
        showErrorAndUsage(new RmPref(), "Required arguments missing");
        System.exit(1);
      }
      program.run();
    } catch (Exception e) {
      System.err.printf("error %s: %s%n", RmPref.class.getName(), e.getMessage());
      if (SHOWTRACES) {
        e.printStackTrace();
      }
      System.exit(1);
    }
  }

  @Override
  public void run() {

    Preferences p = null;
    if (nonNull(getNodename()) && nodeIsClass()) {
      Class<?> c = null;
      try {
        c = Class.forName(getNodename());
      } catch (ClassNotFoundException e) {
        System.err.printf("error %s: Class %s not found (Is it on the classpath?).%n",
            getClass().getName(), getNodename());
        if (SHOWTRACES) {
          e.printStackTrace();
        }
        System.exit(1);
      }
      p = isSystemRoot() ? Preferences.systemNodeForPackage(c) : Preferences.userNodeForPackage(c);
    } else if (nonNull(getNodename())) {
      p = isSystemRoot() ? Preferences.systemRoot().node(getNodename())
          : Preferences.userRoot().node(getNodename());
    } else {
      p = isSystemRoot() ? Preferences.systemRoot() : Preferences.userRoot();
    }

    var errmsg = "";
    Throwable t = null;
    try {
      if (nonNull(getKey())) {
        p.remove(getKey());
      } else {
        p.removeNode();
      }
    } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException
        | BackingStoreException e) {
      errmsg = e.getMessage();
      t = e;
    } catch (NullPointerException e) {
      errmsg = "Key or value is null";
      t = e;
    } finally {
      if (nonNull(t)) {
        System.err.printf("error %s: %s.", getClass().getName(), errmsg);
        if (SHOWTRACES) {
          t.printStackTrace();
        }
        System.exit(1);
      }
    }
  }

  boolean nodeIsClass() {
    return nodeIsClass;
  }

  void setNodeIsClass(boolean nodeIsClass) {
    this.nodeIsClass = nodeIsClass;
  }

  boolean isSystemRoot() {
    return systemRoot;
  }

  void setSystemRoot(boolean systemRoot) {
    this.systemRoot = systemRoot;
  }

  String getNodename() {
    return nodename;
  }

  void setNodename(String nodename) {
    this.nodename = nodename;
  }

  String getKey() {
    return key;
  }

  void setKey(String key) {
    this.key = key;
  }

  private static RmPref parseArgs(RmPref instance, String... args) {
    var allOptsProcessed = false;
    var firstArgProcessed = false;
    for (int i = 0; i < args.length; i++) {
      var arg = args[i];
      if (arg.startsWith("-") && !allOptsProcessed) {
        if (arg.length() > 2) {
          var opts = arg.substring(1).toCharArray();
          for (int j = 0; j < opts.length; j++) {
            if (opts[j] == 'h') {
              showUsage(instance);
              showOptions();
              System.exit(2);
            } else if (j != opts.length - 1 || i >= args.length - 1) {
              showErrorAndUsage(instance, String.format("-%c missing required argument!", opts[j]));
              System.exit(1);
            } else {
              setFieldOn(instance, opts[j]);
            }
          }
        } else {
          var opt = arg.charAt(1);
          if (opt == 'h') {
            showUsage(instance);
            showOptions();
            System.exit(2);
          } else if (i >= args.length - 1) {
            showErrorAndUsage(instance, String.format("-%c missing required argument!", opt));
            System.exit(1);
          } else {
            setFieldOn(instance, opt);
          }
        }
      } else {
        allOptsProcessed = true;
        if (!firstArgProcessed) {
          instance.setNodename(arg);
          firstArgProcessed = true;
        } else {
          instance.setKey(arg);
        }
      }
    }
    return instance;
  }

  private static void setFieldOn(RmPref instance, char opt) {
    switch (opt) {
      case 'C':
        instance.setNodeIsClass(true);
        break;
      case 'S':
        instance.setSystemRoot(true);
        break;
      default:
        showErrorAndUsage(instance, String.format("Unrecognized option \"-%c\".", opt));
        System.exit(1);
        break;
    }
  }

  private static void showErrorAndUsage(RmPref instance, String error) {
    System.err.printf("error %s: %s%n", instance.getClass().getName(), error);
    showUsage(instance);
  }

  private static void showUsage(RmPref instance) {
    System.err.printf("usage: %s [-ChS] node-or-class-name [key]%n",
        instance.getClass().getName());
  }

  private static void showOptions() {
    System.err.println();
    System.err.println("Removes a Preference for a class/node from the preference store.");
    System.err.println();
    System.err.println("Options:");
    System.err.println("  -C                    Treat node name as a fully-qualified class name.");
    System.err.println("  -h                    Shows help message and exits.");
    System.err.println("  -S                    Remove preference from the system root (default is add");
    System.err.println("                          to the user root).");
    System.err.println();
    System.err.println("Arguments:");
    System.err.println("  node-or-class-name    Name of the node or class.");
    System.err.println("  key                   Key (name) of the preference. If not set, the entire");
    System.err.println("                          node is deleted.");
  }
}
