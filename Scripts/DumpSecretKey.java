import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.UnrecoverableKeyException;
import java.util.Base64;
import java.util.Objects;

/**
 * Dumps a secret key a Java keystore to output.
 *
 * <P>
 * <STRONG>NOTE:</STRONG> using this tool will expose a secret key or password.
 * It is strongly recommended to only use this under the supervision of an IT
 * security specialist.
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

      KeyStore.PasswordProtection protection = null;
      try {

        protection = new KeyStore.PasswordProtection(keypass);

        // The secretKeyEntry could be null indicating it wasn't found in the KeyStore
        // so remember to check it after this method is finished!
        secretKeyEntry = (KeyStore.SecretKeyEntry) keystore.getEntry(alias, protection);

      } catch (UnrecoverableKeyException e) {

        // Maybe had the wrong key password? Try again.
        keypass = enterPassword("key");
        protection = new KeyStore.PasswordProtection(keypass);
        secretKeyEntry = (KeyStore.SecretKeyEntry) keystore.getEntry(alias, protection);

      }

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
    return (Objects.isNull(ary) || (Objects.nonNull(ary) && ary.length == 0));
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
   * Command-line arguments match those of the keytool to communicate intent.
   * 
   * <DL>
   * <DT>{@code -alias <arg>}
   * <DD>Alias name of the entry in the keystore to process.
   * <DT>{@code -file <filename>}
   * <DD>Output file name ({@link System#out} if not set).
   * <DT>{@code -help}
   * <DD>Display a help message and exit with status code {@code 2}.
   * <DT>{@code -keypass [:env|:file] <arg>}
   * <DD>Key password. If {@code :env} modifier is specified, retrieve value
   * of the specified environment variable. If {@code :file} modifier specified,
   * read password from the specified file name. Otherwise, use the given argument
   * as the password. If not set, use the same value as {@code -storepass}. If not
   * the same value as {@code -storepass}, user will be prompted for a key
   * password.
   * <DT>{@code -keystore <keystore>}
   * <DD>Keystore file name ({@code $HOME/.keystore} if not set).
   * <DT>{@code -raw}
   * <DD>Bypass base64 encoding of key and dump it raw to the output.
   * <DT>{@code -storepass [:env|:file] <arg>}
   * <DD>Keystore password. If {@code :env} modifier is specified, retrieve value
   * of the specified environment variable. If {@code :file} modifier specified,
   * read password from the specified file name. Otherwise, use the given argument
   * as the password. If not set, user will be prompted for store password.
   * <DT>{@code -storetype <arg>}
   * <DD>Keystore type (result of {@link KeyStore#getDefaultType()} if not set).
   * </DL>
   * 
   * @param args arguments as previously described.
   * 
   * @see <A href="https://dev.java/learn/jvm/tool/security/keytool/">Keytool -
   *      Managing Your Keystore</A>
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
          checkOptionHasArgument(arg, args, argIdx);
          app.setAlias(args[++argIdx]);
          break;
        case "-file":
          checkOptionHasArgument(arg, args, argIdx);
          outfilename = args[++argIdx];
          break;
        case "-help":
          showUsageAndExit(2);
          break;
        case "-keypass":
        case "-keypass:env":
        case "-keypass:file":
          checkOptionHasArgument(arg, args, argIdx);
          app.setKeypass(readPassword(arg, args[++argIdx]));
          break;
        case "-keystore":
          checkOptionHasArgument(arg, args, argIdx);
          app.setKeystoreName(args[++argIdx]);
          break;
        case "-raw":
          raw = true;
          break;
        case "-storepass":
        case "-storepass:env":
        case "-storepass:file":
          checkOptionHasArgument(arg, args, argIdx);
          app.setStorepass(readPassword(arg, args[++argIdx]));
          break;
        case "-storetype":
          checkOptionHasArgument(arg, args, argIdx);
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

      var secretKeyBytes = new byte[0];
      if (Objects.nonNull(secretkey)) {
        secretKeyBytes = secretkey.getSecretKey().getEncoded();
      } else {
        System.err.printf("Alias \"%s\" not found in %s!%n", app.getAlias(), app.getKeystoreName());
        System.exit(1);
      }

      // Send it to output.
      var key = raw ? new String(secretKeyBytes) : new String(Base64.getEncoder().encode(secretKeyBytes));
      out.print(key);

    } catch (Exception e) {
      System.err.printf("Failure! %s%n", e.getMessage());
      System.exit(1);
    } finally {
      if (Objects.nonNull(outstream)) {
        try {
          outstream.close();
        } catch (IOException e) {
          System.err.println("Failed to close output file!");
        }
      }
    }
  }

  /**
   * Attempts to read a password. If the given password option ends with
   * {@code :env}, then the password is retrieved from the environment from the
   * named value at {@code passwdData}. If the password option ends
   * with{@code :file}, then the password will be read in from the file named at
   * {@code passwdData}. Otherwise, the argument given is the password.
   * 
   * @param passwdOpt  password option set on command line.
   * @param passwdData command line arguments array.
   * @return password as a char array.
   */
  private static char[] readPassword(String passwdOpt, String passwdData) {
    if (passwdOpt.endsWith(":env")) {
      return System.getenv(passwdData).toCharArray();
    } else if (passwdOpt.endsWith(":file")) {
      return readRawPasswordFromFile(new File(passwdData));
    } else {
      return passwdData.toCharArray();
    }
  }

  /**
   * 
   * @param file the File to read the raw password from.
   * @return contents of the file as a char array.
   */
  private static char[] readRawPasswordFromFile(File file) {
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      var pword = br.readLine();
      return pword.toCharArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * If the option does not have an argument, show an error message, show usage,
   * and exit with code 1.
   * 
   * @param arg    argument name.
   * @param args   argument array.
   * @param argIdx current index of argument array.
   */
  private static void checkOptionHasArgument(String arg, String[] args, int argIdx) {
    if (args.length < argIdx + 1) {
      showOptionArgumentError(arg);
      showUsageAndExit(1);
    }
  }

  /**
   * Prints an error message stating the given option needs an argument to
   * {@link System#err}.
   * 
   * @param opt option that needs an argument.
   */
  private static void showOptionArgumentError(String opt) {
    System.err.printf("Command option %s needs an argument.", opt);
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
    System.err.println(" -keypass[:env|:file] <arg>");
    System.err.println("                       key password");
    System.err.println(" -keystore <keystore>  keystore name");
    System.err.println(" -raw                  dump the raw value (useful for passwords)");
    System.err.println(" -storepass[:env|:file] <arg>");
    System.err.println("                       keystore password");
    System.err.println(" -storetype <arg>      keystore type");
  }

  /**
   * Checks the given String if it is {@code null} or empty.
   * 
   * @param s the String.
   * @return {@code true} if the String is {@code null} or empty.
   */
  private static boolean isNullOrEmpty(String s) {
    return (Objects.isNull(s) || (Objects.nonNull(s) && s.isEmpty()));
  }
}
