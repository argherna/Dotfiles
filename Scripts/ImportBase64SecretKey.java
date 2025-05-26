import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.spec.SecretKeySpec;

/**
 * Imports a Base64-encoded string into a Java keystore as a secret key.
 */
class ImportBase64SecretKey implements Runnable {

  private String alias;

  private String inFilename = "-";

  private String keyalg = "AES";

  private String keystoreName = String.format("%s%s.keystore", System.getProperty("user.home"),
      System.getProperty("file.separator"));

  private char[] keypass;

  private char[] storepass;

  private String storetype;

  /**
   * Read the Base64 string in from {@link System#in} or a file, then store it in
   * the KeyStore with the given {@code alias}.
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

    try {

      // Read input into Base64 encoded string.
      var instream = inFilename.equals("-") ? System.in : new FileInputStream(inFilename);
      var bis = new BufferedInputStream(instream);
      var buf = new ByteArrayOutputStream();
      var result = bis.read();
      while (result != -1) {
        byte b = (byte) result;
        buf.write(b);
        result = bis.read();
      }
      var keystring = buf.toString();

      // Decode Base64 into a secret key.
      var bytes = Base64.getDecoder().decode(keystring);
      var key = new SecretKeySpec(bytes, 0, bytes.length, keyalg);

      // Read keystore file.
      var ksFile = new File(keystoreName);
      var keystorein = new FileInputStream(ksFile);
      var keystore = KeyStore.getInstance(storetype);
      keystore.load(keystorein, storepass);
      keystorein.close();

      // Add the key to the KeyStore, then save it.
      var secretKeyEntry = new KeyStore.SecretKeyEntry(key);
      var protection = new KeyStore.PasswordProtection(keypass);
      keystore.setEntry(alias, secretKeyEntry, protection);

      var keystoreout = new FileOutputStream(ksFile);
      keystore.store(keystoreout, storepass);
      keystoreout.close();

    } catch (IOException | GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param alias the alias to set.
   */
  void setAlias(String alias) {
    this.alias = alias;
  }

  /**
   * @param inFilename the inFilename to set.
   */
  void setInFilename(String inFilename) {
    this.inFilename = inFilename;
  }

  /**
   * @param keyalg the key algorithm to set.
   */
  void setKeyalg(String keyalg) {
    this.keyalg = keyalg;
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
   * Checks the given String if it is {@code null} or empty.
   * 
   * @param value the String.
   * @return {@code true} if the String is {@code null} or empty.
   */
  private boolean isNullOrEmpty(String value) {
    return (value == null || (value != null && value.isEmpty()));
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
   * Main function.
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

    if (args.length == 0) {
      showUsageAndExit(2);
    }

    var app = new ImportBase64SecretKey();
    int argIdx = 0;

    while (argIdx < args.length) {
      String arg = args[argIdx];
      switch (arg) {
        case "-alias":
          app.setAlias(args[++argIdx]);
          break;
        case "-file":
          app.setInFilename(args[++argIdx]);
          break;
        case "-help":
          showUsageAndExit(2);
          break;
        case "-keyalg":
          app.setKeyalg(args[++argIdx].toUpperCase());
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
          app.setStoretype(args[++argIdx].toUpperCase());
          break;
        default:
          System.err.printf("Unknown option: %s%n");
          showUsageAndExit(1);
          break;
      }
      argIdx++;
    }

    app.run();
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
    System.err.printf("Usage: %s [OPTION]%n", ImportBase64SecretKey.class.getName());
    System.err.println();
    System.err.println("Imports base64 encoded secret key to an existing keystore");
    System.err.println();
    System.err.println("Options:");
    System.err.println();
    System.err.println(" -alias <alias>        alias name of the entry to process");
    System.err.println(" -file <filename>      input file name with a single base64-encoded string");
    System.err.println(" -help                 show this message and exit");
    System.err.println(" -keyalg <arg>         key algorithm name");
    System.err.println(" -keypass <arg>        key password");
    System.err.println(" -keystore <keystore>  keystore name");
    System.err.println(" -storepass <arg>      keystore password");
    System.err.println(" -storetype <arg>      keystore type");
  }
}
