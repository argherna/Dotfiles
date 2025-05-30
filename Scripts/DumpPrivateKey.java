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
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.util.Base64;
import java.util.Objects;

/**
 * Dumps the private key from a keystore to output.
 *
 * <P>
 * A keystore has to be created in order to use this tool. An example command
 * for creating a keystore is as follows:
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

      KeyStore.PasswordProtection protection = null;
      try {

        protection = new KeyStore.PasswordProtection(keypass);

        // The privateKeyEntry could be null indicating it wasn't found in the KeyStore
        // so remember to check it after this method is finished!
        privateKeyEntry = (KeyStore.PrivateKeyEntry) keystore.getEntry(alias, protection);

      } catch (UnrecoverableKeyException e) {

        // Maybe had the wrong key password? Try again.
        keypass = enterPassword("key");
        protection = new KeyStore.PasswordProtection(keypass);
        privateKeyEntry = (KeyStore.PrivateKeyEntry) keystore.getEntry(alias, protection);

      }

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
   * Returns the {@link KeyStore.PrivateKeyEntry} form the {@link KeyStore} for
   * further processing.
   * 
   * <P>
   * <STRONG>NOTE:</STRONG> this method should only be called
   * <STRONG>AFTER</STRONG> the {@link #run()} method has been called. This method
   * may return a {@code null} value indicating the private key with the given
   * alias wasn't found in the keystore.
   * 
   * @return the privateKeyEntry.
   */
  KeyStore.PrivateKeyEntry getPrivateKeyEntry() {
    return privateKeyEntry;
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
          app.setKeypass(readPassword(args, ++argIdx));
          // Increment argIdx if value of the current argument is ":env" or ":file".
          if (args[argIdx].startsWith(":")) {
            argIdx++;
          }
          break;
        case "-keystore":
          app.setKeystoreName(args[++argIdx]);
          break;
        case "-storepass":
          app.setStorepass(readPassword(args, ++argIdx));
          // Increment argIdx if value of the current argument is ":env" or ":file".
          if (args[argIdx].startsWith(":")) {
            argIdx++;
          }
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

      printAsRFC(out, app.getKeyStore(), app.getKeystoreFile(), pke.getPrivateKey());

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
   * Attempts to read a password. This method will first read the password from
   * the {@code args} array at {@code idx}. If the value is {@code :env}, then the
   * password is retrieved from the environment from the named value at
   * {@code args[idx + 1]}. If the value is {@code :file}, then the password will
   * be
   * read in from the file named at {@code args[idx + 1]}. Otherwise, the argument
   * given is the password.
   * 
   * @param args command line arguments array.
   * @param idx  index of where to start looking in {@code args}.
   * @return password as a char array.
   */
  private static char[] readPassword(String[] args, int idx) {
    var arg = args[idx];
    if (arg.equals(":env")) {
      return System.getenv(args[idx + 1]).toCharArray();
    } else if (arg.equals(":file")) {
      return readRawPasswordFromFile(new File(args[idx + 1]));
    } else {
      return arg.toCharArray();
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
   * Prints the private key to the given PrintStream in RFC 1421 format.
   * 
   * <P>
   * The format for the private key is as follows:
   *
   * <PRE>
   * {@code -----BEGIN <algorithm> PRIVATE KEY-----
   * Key-Format: XXXXXX
   * Keystore-File: <keystore filename>
   * Keystore-Type: ZZZZZZ
   * 
   * MII ...
   * -----END <algorithm> PRIVATE KEY----- }
   * </PRE>
   * 
   * @param out          PrintStream to write output to.
   * @param ks           the KeyStore.
   * @param keystoreFile the KeyStore File.
   * @param pk           the PrivateKey.
   * 
   * @see <A href="https://www.rfc-editor.org/rfc/rfc1421">Privacy Enhancement for
   *      Internet Electronic Mail:
   *      Part I: Message Encryption and Authentication Procedures</A>
   * @see <A href=
   *      "https://docs.oracle.com/javase/7/docs/technotes/tools/solaris/keytool.html#EncodeCertificate">RFC
   *      1421 Certificate Encoding format</A>
   */
  private static void printAsRFC(PrintStream out, KeyStore ks, File keystoreFile, PrivateKey pk) {

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
    out.flush();
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
    System.err.println(" -file <filename>      output file name (default is write to stdout)");
    System.err.println(" -help                 show this message and exit");
    System.err.println(" -keypass [:env|:file] <arg>");
    System.err.println("                       key password");
    System.err.println(" -keystore <keystore>  keystore name");
    System.err.println(" -storepass [:env|:file] <arg>");
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
