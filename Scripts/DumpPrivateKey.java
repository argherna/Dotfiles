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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.security.Key;
import java.security.KeyStore;
import java.util.Base64;
import java.util.Base64.Encoder;

/**
 * Dumps the private key from a JKS keystore to {@linkplain System#out System.out}.
 *
 *
 * <p>
 * Keystores must be created as follows to work with this tool:
 *
 * <pre>
 * {@code keytool -genkeypair \
 *   -alias KEYALIAS \
 *   -keyalg rsa \
 *   -keysize 2048 \
 *   -validity 9999 \
 *   -v \
 *   -keystore KEYSTORE.jks
 *   -storetype jks}
 * </pre>
 *
 * </p>
 * <p>
 * This tool will export the private key in the format:
 *
 * <pre>
 * {@code -----BEGIN PRIVATE KEY-----
 * MII ...
 * -----END PRIVATE KEY----- }
 * </pre>
 *
 * </p>
 *
 * @author agherna
 *
 */
class DumpPrivateKey {

  public static void main(String[] args) {

    if (args.length == 0) {
      showUsageAndExit(2);
    }

    int argIdx = 0;
    String keystorename = null;
    char[] storepass = null;
    String alias = "mykey";
    PrintStream out = System.out;
    char[] keypass = null;
    String outfilename = null;
    String storetype = "jks";

    while (argIdx < args.length) {

      String arg = args[argIdx];
      switch (arg) {
        case "-alias":
          alias = args[++argIdx];
          break;
        case "-file":
          outfilename = args[++argIdx];
          break;
        case "-help":
          showUsage();
          System.exit(2);
          break;
        case "-keypass":
          keypass = args[++argIdx].toCharArray();
          break;
        case "-keystore":
          keystorename = args[++argIdx];
          break;
        case "-storepass":
          storepass = args[++argIdx].toCharArray();
          break;
        case "-storetype":
          storetype = args[++argIdx];
          break;
        default:
          System.err.printf("Unknown option %s%n", arg);
          showUsageAndExit(1);
          break;
      }
      argIdx++;
    }

    if (keystorename == null) {
      keystorename =
          String.format("%s%s.keystore", System.getProperty("user.home"),
              System.getProperty("file.separator"));
    }

    if (isNullOrEmpty(storepass)) {
      storepass = getPassword("keystore");
    }

    if (isNullOrEmpty(keypass)) {
      keypass = storepass;
    }

    if (outfilename != null) {
      try {
        out = new PrintStream(new FileOutputStream(outfilename));
      } catch (FileNotFoundException e) {
        System.err.println(e.getMessage());
        System.exit(1);
      }
    }

    try {
      KeyStore ks = KeyStore.getInstance(storetype);
      File keystore = new File(keystorename);
      ks.load(new FileInputStream(keystore), storepass);
      Key key = ks.getKey(alias, keypass);
      writeAsRFC(out, ks, keystore, key);
    } catch (FileNotFoundException e) {
      System.err.printf("Keystore file does not exist: %s%n", keystorename);
      System.exit(1);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  private static void writeAsRFC(PrintStream out, KeyStore ks, File keystore, Key key) {
    Base64.Encoder encoder = Base64.getEncoder();
    String b64 = encoder.encodeToString(key.getEncoded());
    out.printf("-----BEGIN %s PRIVATE KEY-----%n", key.getAlgorithm());
    out.printf("Key-Format: %s%n", key.getFormat());
    out.printf("Keystore-File: %s%n", keystore.getName());
    out.printf("Keystore-Type: %s%n", ks.getType());
    out.println();
    int index = 0;
    int linelength = 76;
    while (index < b64.length()) {
      if (index < (b64.length() - linelength)) {
        out.println(b64.substring(index, (linelength + index)));
      } else {
        out.println(b64.substring(index));
      }

      index = index + linelength;
    }
    out.printf("-----END %s PRIVATE KEY-----%n", key.getAlgorithm());
  }

  private static void showUsageAndExit(int code) {
    showUsage();
    System.exit(code);
  }

  private static void showUsage() {
    System.err
        .printf("Usage: %s [OPTION]...%n", DumpPrivateKey.class.getSimpleName());
    System.err.println();
    System.err.println("Dumps the private key from a keystore");
    System.err.println();
    System.err.println("Options:");
    System.err.println();
    System.err.println(" -alias <alias>        alias name of the entry to process");
    System.err.println(" -file <filename>      output file name");
    System.err.println(" -keypass <arg>        key password (optional, defaults to storepass)");
    System.err.println(" -keystore <keystore>  keystore name");
    System.err.println(" -storepass <arg>      keystore password");
    System.err.println(" -storetype <arg>      keystore type");
  }

  private static char[] getPassword(String passwordType) {
    char[] password = "changeme".toCharArray();
    Console c = System.console();
    if (c == null) {
      System.err.println("System console not available!");
    } else {
      password = c.readPassword("Enter %s password:  ", passwordType);
    }
    return password;
  }

  private static boolean isNullOrEmpty(char[] value) {
    return (value == null || (value !=null && value.length == 0));
  }
}
