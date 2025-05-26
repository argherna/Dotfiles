import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Base64;
import java.util.Objects;

/**
 * Dumps a secret key as a raw string from a Java keystore.
 */
class DumpSecretKey implements Runnable {

  private String alias;

  private String keystoreName = String.format("%s%s.keystore", System.getProperty("user.home"),
      System.getProperty("file.separator"));

  private char[] keypass;

  private char[] storepass;

  private String storetype;

  private KeyStore.SecretKeyEntry secretKeyEntry;

  /**
   * Retrieve the SecretKeyEntry from a KeyStore.
   * 
   * @throws IllegalStateException if the alias is not set.
   * @throws RuntimeException      if an exception is thrown during processing.
   */
  @Override
  public void run() {

    if (isNullOrEmpty(alias)) {
      throw new IllegalStateException("Alias not specified!");
    }

    if (charArrayNullOrEmpty(storepass)) {
      storepass = enterPassword("keystore");
    }

    if (charArrayNullOrEmpty(keypass)) {
      keypass = storepass;
    }

    if (isNullOrEmpty(storetype)) {
      storetype = KeyStore.getDefaultType();
    }

    try (FileInputStream keystorein = new FileInputStream(new File(keystoreName))) {

      var keystore = KeyStore.getInstance(storetype);
      keystore.load(keystorein, storepass);

      var protection = new KeyStore.PasswordProtection(keypass);

      // The secretKeyEntry could be null indicating it wasn't found in the KeyStore
      // so remember to check it after this method is finished!
      secretKeyEntry = (KeyStore.SecretKeyEntry) keystore.getEntry(alias, protection);

    } catch (IOException | GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the KeyStore alias to be retrieved.
   */
  String getAlias() {
    return alias;
  }

  /**
   * @param alias the alias to set.
   */
  void setAlias(String alias) {
    this.alias = alias;
  }

  /**
   * @return the keystore name.
   */
  String getKeystoreName() {
    return keystoreName;
  }

  /**
   * @param keystoreName the keystoreName to set.
   */
  void setKeystoreName(String keystoreName) {
    this.keystoreName = keystoreName;
  }

  /**
   * @param keypass the keypass to set.
   */
  void setKeypass(char[] keypass) {
    this.keypass = keypass;
  }

  /**
   * @param storepass the storepass to set.
   */
  void setStorepass(char[] storepass) {
    this.storepass = storepass;
  }

  /**
   * @param storetype the storetype to set.
   */
  void setStoreType(String storetype) {
    this.storetype = storetype;
  }

  /**
   * Returns the {@link KeyStore.SecretKeyEntry} from the {@link KeyStore} for
   * further processing.
   * 
   * <P>
   * <STRONG>NOTE:</STRONG> this method should only be called
   * <STRONG>AFTER</STRONG> the {@link #run()} method has been called. This method
   * may return a {@code null} value indicating the secret key with the given
   * alias wasn't found in the keystore.
   * 
   * @return the KeyStore.SecretKeyEntry to process.
   */
  KeyStore.SecretKeyEntry getSecretKeyEntry() {
    return secretKeyEntry;
  }

  /**
   * Checks the given char array if it is {@code null} or empty.
   * 
   * @param ary the char array.
   * @return {@code true} if the array is {@code null} or empty.
   */
  private boolean charArrayNullOrEmpty(char[] ary) {
    return (ary == null || (ary != null && ary.length == 0));
  }

  /**
   * Prompts for a password from the console.
   * 
   * @param passwdType the password type (keystore or key).
   * @return password as a char array.
   * @throws IllegalStateException if the {@link System} doesn't return a
   *                               {@link Console}.
   */
  private char[] enterPassword(String passwdType) {
    var console = System.console();
    if (Objects.isNull(console)) {
      throw new IllegalStateException("Console not available for password entry!");
    }
    return console.readPassword(String.format("Enter %s password:  ", passwdType));
  }

  /**
   * The main entry point.
   * 
   * <P>
   * Command-line arguments match those of the keytool to communicate intent. If
   * the {@code -keystore} option isn't set, the default is to use
   * {@code $HOME/.keystore}. If the {@code -keypass} isn't specified, the default
   * is to use the value for {@code -storepass} whether specified on the command
   * line or via prompt.
   * 
   * @param args arguments the same as keytool functions.
   * 
   * @see <A href=
   *      "https://docs.oracle.com/en/java/javase/11/tools/keytool.html">keytool
   *      reference</A>
   */
  public static void main(String... args) {

    if (args.length == 0) {
      showUsageAndExit(2);
    }

    var app = new DumpSecretKey();
    var argIdx = 0;
    String outfilename = null;
    var raw = false;

    // Loop through the command-line arguments.
    while (argIdx < args.length) {
      var arg = args[argIdx];
      switch (arg) {
        case "-alias":
          app.setAlias(args[++argIdx]);
          break;
        case "-file":
          outfilename = args[++argIdx];
          break;
        case "-help":
          showUsageAndExit(2);
          break;
        case "-keypass":
          app.setKeypass(args[++argIdx].toCharArray());
          break;
        case "-keystore":
          app.setKeystoreName(args[++argIdx]);
          break;
        case "-raw":
          raw = true;
          break;
        case "-storepass":
          app.setStorepass(args[++argIdx].toCharArray());
          break;
        case "-storetype":
          app.setStoreType(args[++argIdx].toUpperCase());
          break;
        default:
          System.err.printf("Unknown option: %s%n", arg);
          showUsageAndExit(1);
          break;
      }
      argIdx++;
    }

    FileOutputStream outstream = null;
    try {

      // Set up the output.
      var out = System.out;
      if (!isNullOrEmpty(outfilename)) {
        outstream = new FileOutputStream(outfilename);
        out = new PrintStream(outstream);
      }

      // Retrieve the SecretKeyEntry and make sure it was retrieved.
      app.run();
      var secretkey = app.getSecretKeyEntry();

      var encoded = new byte[0];
      if (Objects.nonNull(secretkey)) {
        encoded = secretkey.getSecretKey().getEncoded();
      } else {
        System.err.printf("Alias \"%s\" not found in %s!%n", app.getAlias(), app.getKeystoreName());
        System.exit(1);
      }

      // Send it to output.
      var key = raw ? new String(encoded) : new String(Base64.getEncoder().encode(encoded));
      out.print(key);

    } catch (Exception e) {
      System.err.printf("Failure! %s%n", e.getMessage());
      System.exit(1);
    } finally {
      if (outstream != null) {
        try {
          outstream.close();
        } catch (IOException e) {
          System.err.println("Failed to close output file!");
        }
      }
    }
  }

  /**
   * Prints a usage message to {@link System#err} and exits with the given code.
   * 
   * @param code the exit code.
   */
  private static void showUsageAndExit(int code) {
    showUsage();
    System.exit(code);
  }

  /**
   * Prints a usage message to {@link System#err}.
   */
  private static void showUsage() {
    System.err.printf("Usage: %s [OPTION]%n", DumpSecretKey.class.getName());
    System.err.println();
    System.err.println("Dumps base64 encoded (or raw if specified) secret key "
        + "from a keystore; useful");
    System.err.println("for dumping passwords saved in a keystore added by the keytool command");
    System.err.println("-importpass");
    System.err.println();
    System.err.println("Options:");
    System.err.println();
    System.err.println(" -alias <alias>        alias name of the entry to process");
    System.err.println(" -file <filename>      output file name (default is write to stdout)");
    System.err.println(" -help                 show this message and exit");
    System.err.println(" -keypass <arg>        key password");
    System.err.println(" -keystore <keystore>  keystore name");
    System.err.println(" -raw                  dump the raw value (useful for passwords)");
    System.err.println(" -storepass <arg>      keystore password");
    System.err.println(" -storetype <arg>      keystore type");
  }

  /**
   * Checks the given String if it is {@code null} or empty.
   * 
   * @param value the String.
   * @return {@code true} if the String is {@code null} or empty.
   */
  private static boolean isNullOrEmpty(String value) {
    return (value == null || (value != null && value.isEmpty()));
  }
}
