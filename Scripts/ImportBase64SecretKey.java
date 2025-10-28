import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
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
      buf.close();
      bis.close();
      if (instream instanceof FileInputStream) {
        instream.close();
      }

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
   * @param s the String.
   * @return {@code true} if the String is {@code null} or empty.
   */
  private boolean isNullOrEmpty(String s) {
    return (Objects.isNull(s) || (Objects.nonNull(s) && s.isEmpty()));
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
   * Main function.
   * 
   * <P>
   * Command-line arguments match those of the keytool to communicate intent.
   * 
   * <DL>
   * <DT>{@code -alias <arg>}
   * <DD>Alias name of the entry in the keystore to process.
   * <DT>{@code -file <filename>}
   * <DD>Input file name ({@link System#in} if not set).
   * <DT>{@code -help}
   * <DD>Display a help message and exit with status code {@code 2}.
   * <DT>{@code -keyalg <alg>}
   * <DD>Key algorithm to use ({@code AES} if not set).
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
   * <P>
   * <STRONG>NOTE:</STRONG> if you're on MacOS and you're using base64 to encode
   * input into this program, you must run it like this to ensure no newline
   * characters are appended to the encoded data:
   * 
   * <PRE>{@code
   * echo -n 'yourdata' | base64 | tr -d \\n \
   *   | java ImportBase64SecretKey.java [options]
   * }</PRE>
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
          app.setKeypass(readPassword(args, ++argIdx));
          break;
        case "-keystore":
        case "-keypass:env":
        case "-keypass:file":
          app.setKeystoreName(args[++argIdx]);
          break;
        case "-storepass":
        case "-storepass:env":
        case "-storepass:file":
          app.setStorepass(readPassword(args, ++argIdx));
          break;
        case "-storetype":
          app.setStoretype(args[++argIdx].toUpperCase());
          break;
        default:
          System.err.printf("Unknown option: %s%n", arg);
          showUsageAndExit(1);
          break;
      }
      argIdx++;
    }

    app.run();
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
    System.err.println(" -keyalg <alg>         key algorithm name");
    System.err.println(" -keypass[:env|:file] <arg>");
    System.err.println("                       key password");
    System.err.println(" -keystore <keystore>  keystore name");
    System.err.println(" -storepass[:env|:file] <arg>");
    System.err.println("                       keystore password");
    System.err.println(" -storetype <arg>      keystore type");
  }
}
