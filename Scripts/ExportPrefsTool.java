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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.prefs.Preferences;

class ExportPrefsTool {

  public static void main(String... args) {

    int argIdx = 0;
    String classname = "";
    boolean exportTree = false;
    String nodename = "";
    String filename = "";
    boolean systemRoot = false;

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
        case "-S":
        case "--system-root":
          systemRoot = true;
          break;
        case "-t":
        case "--export-tree":
          exportTree = true;
          break;
        case "-U":
        case "--user-root":
          systemRoot = false;
          break;
        default:
          filename = arg;
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

    OutputStream out = System.out;
    if (isNotNullAndEmpty(filename)) {
      try {
        out = new FileOutputStream(filename);
      } catch (IOException e) {
        System.err.println(e.getMessage());
        System.exit(1);
      }
    }

    try {
      if (exportTree) {
        p.exportSubtree(out);
      } else {
        p.exportNode(out);
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
    System.err.printf("Usage: %s [OPTIONS] <filename>%n", ExportPrefsTool.class.getName());
    System.err.println();
    System.err.println("Exports preferences to XML.");
    System.err.println();
    System.err.println("Arguments:");
    System.err.println();
    System.err.println("  <filename>   Optional file name to write the preferences to.");
    System.err.println("Options:");
    System.err.println();
    System.err.println(" -C, --class <name>");
    System.err.println("               Specify the fully qualified class name for the preference node");
    System.err.println(" -t, --export-tree");
    System.err.println("               Export whole subtree");
    System.err.println(" -h, --help    Show this help and exit");
    System.err.println(" -N, --node-name");
    System.err.println("               Set the name for this preference node");
    System.err.println(" -S, --system-root");
    System.err.println("               Add preferences to the system root");
    System.err.println(" -U, --user-root");
    System.err.println("               Add preferences to the system root");
    System.err.println();
    System.err.println("NOTES:");
    System.err.println(" - If <filename> is not specified, preferences XML are written to System.out.");
    System.err.println(" - Only one of -C or -N can be set.");
    System.err.println(" - Only one of -S or -U can be set (-U is assumed if no option is set).");
    System.err.println(" - If -N is not set, the root (user or system) is assumed.");
   
  }

  private static boolean isNotNullAndEmpty(String s) {
    return (s != null && !s.isEmpty());
  }
}