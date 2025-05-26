import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.Objects;

/**
 * Dumps the private key from a keystore to {@linkplain System#out System.out}.
 *
 *
 * <P>
 * Keystores must be created as follows to work with this tool:
 *
 * <PRE>
 * {@code keytool -genkeypair \
 *   -keyalg rsa \
 *   -keysize 2048 \
 *   -validity 9999 \
 *   -v \}
 * </PRE>
 *
 * <P>
 * <STRONG>NOTE:</STRONG> using this tool will expose your private key. It is
 * strongly recommended to only use this under the supervision of an IT
 * security specialist.
 */
class DumpPrivateKey implements Runnable {

  // Default alias for the private key.
  private String alias = "mykey";

  private char[] keypass;

  private KeyStore keystore;

  private String keystoreName = String.format("%s%s.keystore", System.getProperty("user.home"),
      System.getProperty("file.separator"));

  private File ksFile;

  private char[] storepass;

  private String storetype;

  private KeyStore.PrivateKeyEntry privateKeyEntry;

  /**
   * Retrieve the PrivateKeyEntry from a KeyStore.
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

    ksFile = new File(keystoreName);

    try (FileInputStream keystorein = new FileInputStream(ksFile)) {

      keystore = KeyStore.getInstance(storetype);
      keystore.load(keystorein, storepass);

      var protection = new KeyStore.PasswordProtection(keypass);

      // The privateKeyEntry could be null indicating it wasn't found in the KeyStore
      // so remember to check it after this method is finished!
      privateKeyEntry = (KeyStore.PrivateKeyEntry) keystore.getEntry(alias, protection);

    } catch (IOException | GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the alias.
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
   * @return the keystoreName.
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
  void setStoretype(String storetype) {
    this.storetype = storetype;
  }

  /**
   * @return the KeyStore.
   */
  KeyStore getKeyStore() {
    return keystore;
  }

  /**
   * @return the keystore file.
   */
  File getKeystoreFile() {
    return ksFile;
  }

  /**
   * @return the privateKeyEntry.
   */
  KeyStore.PrivateKeyEntry getPrivateKeyEntry() {
    return privateKeyEntry;
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
   * Checks the given String if it is {@code null} or empty.
   * 
   * @param value the String.
   * @return {@code true} if the String is {@code null} or empty.
   */
  private boolean charArrayNullOrEmpty(char[] value) {
    return (value == null || (value != null && value.length == 0));
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
  public static void main(String[] args) {

    var app = new DumpPrivateKey();
    int argIdx = 0;
    String outfilename = null;

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
        case "-storepass":
          app.setStorepass(args[++argIdx].toCharArray());
          break;
        case "-storetype":
          app.setStoretype(args[++argIdx]);
          break;
        default:
          System.err.printf("Unknown option %s%n", arg);
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

      // Retrieve the PrivateKeyEntry and make sure it was retrieved.
      app.run();
      var pke = app.getPrivateKeyEntry();
      if (Objects.isNull(pke)) {
        System.err.printf("Private key \"%s\" not found in %s!%n", app.getAlias(), app.getKeystoreName());
        System.exit(1);
      }

      writeAsRFC(out, app.getKeyStore(), app.getKeystoreFile(), pke.getPrivateKey());

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
   * Writes the private key to the given PrintStream in RFC format.
   * 
   * <P>
   * The format for the private key is as follows:
   *
   * <PRE>
   * {@code -----BEGIN PRIVATE KEY-----
   * Key-Format: XXXXXX"
   * Keystore-File: <keystore filename>
   * Keystore-Type: ZZZZZZ
   * 
   * MII ...
   * -----END PRIVATE KEY----- }
   * </PRE>
   * 
   * @param out          PrintStream to write output to.
   * @param ks           the KeyStore.
   * @param keystoreFile the KeyStore File.
   * @param pk           the PrivateKey.
   */
  private static void writeAsRFC(PrintStream out, KeyStore ks, File keystoreFile, PrivateKey pk) {

    Base64.Encoder encoder = Base64.getEncoder();
    String b64 = encoder.encodeToString(pk.getEncoded());
    out.printf("-----BEGIN %s PRIVATE KEY-----%n", pk.getAlgorithm());
    out.printf("Key-Format: %s%n", pk.getFormat());
    out.printf("Keystore-File: %s%n", keystoreFile.getName());
    out.printf("Keystore-Type: %s%n", ks.getType());
    out.println();
    int index = 0;
    int linelength = 64;
    while (index < b64.length()) {
      if (index < (b64.length() - linelength)) {
        out.println(b64.substring(index, (linelength + index)));
      } else {
        out.println(b64.substring(index));
      }

      index = index + linelength;
    }
    out.printf("-----END %s PRIVATE KEY-----%n", pk.getAlgorithm());
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
    System.err
        .printf("Usage: %s [OPTION]...%n", DumpPrivateKey.class.getSimpleName());
    System.err.println();
    System.err.println("Dumps the private key from a keystore");
    System.err.println();
    System.err.println("Options:");
    System.err.println();
    System.err.println(" -alias <alias>        alias name of the entry to process");
    System.err.println(" -file <filename>      output file name");
    System.err.println(" -help                 show this message and exit");
    System.err.println(" -keypass <arg>        key password (optional, defaults to storepass)");
    System.err.println(" -keystore <keystore>  keystore name");
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
