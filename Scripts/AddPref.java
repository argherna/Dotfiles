import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * Add a preference for class or node.
 * 
 * <p>
 * Set the system property {@code addpref.showtraces} to {@code true} to show stack traces from
 * exceptions.
 */
class AddPref implements Runnable {

  private static final Boolean SHOWTRACES = Boolean.getBoolean("addpref.showtraces");

  private boolean nodeIsClass = false;

  private boolean systemRoot = false;

  private PrefType preftype = PrefType.STRING;

  private String nodename;

  private String key;

  private String value;

  public static void main(String... args) {
    try {
      var program = args.length > 0 ? parseArgs(new AddPref(), args) : null;
      if (isNull(program)) {
        showErrorAndUsage(new AddPref(), "Required arguments missing");
        System.exit(1);
      }
      program.run();
    } catch (Exception e) {
      System.err.printf("error %s: %s%n", AddPref.class.getName(), e.getMessage());
      if (SHOWTRACES) {
        e.printStackTrace();
      }
      System.exit(1);
    }
  }

  static enum PrefType {
    BOOLEAN, DOUBLE, FLOAT, INT, LONG, STRING;
  }

  @Override
  public void run() {
    Class<?> c = null;
    String nodenm = null;
    if (nodeIsClass()) {
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
    } else {
      nodenm = getNodename();
    }

    Preferences p = null;
    if (nonNull(c)) {
      p = isSystemRoot() ? Preferences.systemNodeForPackage(c) : Preferences.userNodeForPackage(c);
    } else {
      p = isSystemRoot() ? Preferences.systemRoot().node(nodenm)
          : Preferences.userRoot().node(nodenm);
    }

    var errmsg = "";
    Throwable t = null;
    try {
      switch (getPreftype()) {
        case BOOLEAN:
          p.putBoolean(getKey(), Boolean.valueOf(getValue()));
          break;
        case DOUBLE:
          p.putDouble(getKey(), Double.valueOf(getValue()));
          break;
        case FLOAT:
          p.putFloat(getKey(), Float.valueOf(getValue()));
          break;
        case INT:
          p.putInt(getKey(), Integer.valueOf(getValue()));
          break;
        case LONG:
          p.putLong(getKey(), Long.valueOf(getValue()));
          break;
        case STRING:
          p.put(getKey(), getValue());
          break;
      }

    } catch (NullPointerException e) {
      errmsg = "Key or value is null";
      t = e;
    } catch (NumberFormatException e) {
      errmsg = String.format("\"%s\" is not a valid %s", getValue(), getPreftype().name());
      t = e;
    } catch (IllegalArgumentException | IllegalStateException e) {
      errmsg = e.getMessage();
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

  boolean isSystemRoot() {
    return systemRoot;
  }

  void setSystemRoot(boolean systemRoot) {
    this.systemRoot = systemRoot;
  }

  PrefType getPreftype() {
    return preftype;
  }

  void setPreftype(PrefType preftype) {
    this.preftype = preftype;
  }

  boolean nodeIsClass() {
    return nodeIsClass;
  }

  void setNodeIsClass(boolean nodeIsClass) {
    this.nodeIsClass = nodeIsClass;
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

  String getValue() {
    return value;
  }

  void setValue(String value) {
    this.value = value;
  }

  private static AddPref parseArgs(AddPref instance, String... args) {
    var requireArgs = Set.of('T');
    var allOptsProcessed = false;
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
            } else if (!requireArgs.contains(opts[j])) {
              setFieldOn(instance, opts[j]);
            } else if (j != opts.length - 1 || i >= args.length - 1) {
              showErrorAndUsage(instance, String.format("-%c missing required argument!", opts[j]));
              System.exit(1);
            } else {
              setFieldOn(instance, opts[j], args[++i]);
            }
          }
        } else {
          var opt = arg.charAt(1);
          if (opt == 'h') {
            showUsage(instance);
            showOptions();
            System.exit(2);
          } else if (!requireArgs.contains(opt)) {
            setFieldOn(instance, opt);
          } else if (i >= args.length - 1) {
            showErrorAndUsage(instance, String.format("-%c missing required argument!", opt));
            System.exit(1);
          } else {
            setFieldOn(instance, opt, args[++i]);
          }
        }
      } else {
        allOptsProcessed = true;
        if (i < args.length - 2) {
          instance.setNodename(arg);
        } else if (i < args.length - 1) {
          instance.setKey(arg);
        } else if (i < args.length) {
          instance.setValue(arg);
        }
      }
    }
    return instance;
  }

  private static void setFieldOn(AddPref instance, char opt, String arg) {
    switch (opt) {
      case 'T':
        instance.setPreftype(PrefType.valueOf(arg));
        break;
      default:
        showErrorAndUsage(instance, String.format("Unrecognized option \"-%c\".", opt));
        System.exit(1);
        break;
    }
  }

  private static void setFieldOn(AddPref instance, char opt) {
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

  private static void showErrorAndUsage(AddPref instance, String error) {
    System.err.printf("error %s: %s%n", instance.getClass().getName(), error);
    showUsage(instance);
  }

  private static void showUsage(AddPref instance) {
    System.err.printf("usage: %s [-ChS] [-T type] node-or-class-name key value%n",
        instance.getClass().getName());
  }

  private static void showOptions() {
    System.err.println();
    System.err.println("Adds a Preference for a class/node to the preference store.");
    System.err.println();
    System.err.println("Options:");
    System.err.println("  -C                    Treat node name as a fully-qualified class name.");
    System.err.println("  -h                    Shows help message and exits.");
    System.err.println("  -S                    Add preference to the system root (default is add");
    System.err.println("                          to the user root).");
    System.err
        .println("  -T type               Type the value represents, one of STRING (default),");
    System.err.println("                           BOOLEAN, DOUBLE, FLOAT, INT, or LONG.");
    System.err.println();
    System.err.println("Arguments:");
    System.err.println("  node-or-class-name    Name of the node or class.");
    System.err.println("  key                   Key (name) of the preference.");
    System.err.println("  value                 Value for the preference.");
  }
}
