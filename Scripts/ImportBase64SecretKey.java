/*bin/mkdir -p /tmp/.java/classes 2> /dev/null
javac -d /tmp/.java/classes $0
java -cp /tmp/.java/classes $(basename ${0%.*}) "$@"
exit
*/
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Base64;
import java.util.stream.Stream;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Imports a Base64-encoded string into a Java JCEKS keystore as a secret key.
 */
public class ImportBase64SecretKey {

  /**
   * Main function.
   * 
   * <p>
   * Command-line arguments match those of the keytool to communicate intent.
   */
  public static void main(String[] args) {
    String alias = null;
    String filename = null;
    char[] keypass = null;
    String keystorename = null;
    char[] storepass = null;
    int argIdx = 0;
    while (argIdx < args.length) {
      String arg = args[argIdx];
      switch (arg) {
        case "-alias":
          alias = args[++argIdx];
          break;
        case "-file":
          filename = args[++argIdx];
          break;
        case "-help":
          showUsageAndExit(2);
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
        default:
          break;
      }
      argIdx++;
    }

    if (isNullOrEmpty(alias)) {
      System.err.println("Must specify a key alias");
      showUsageAndExit(1);
    }

    if (isNullOrEmpty(keystorename)) {
      System.err.println("Must specify a keystore");
      showUsageAndExit(1);
    }

    if (storepass == null || (storepass != null && storepass.length == 0)) {
      System.err.println("Must specify a store password");
      showUsageAndExit(1);
    }

    if (keypass == null || (keypass != null && keypass.length == 0)) {
      keypass = storepass;
    }

    File keystoreFile = new File(keystorename);
    KeyStore keystore = null;

    try (FileInputStream keystoreIn = new FileInputStream(keystoreFile)) {

      keystore = KeyStore.getInstance("JCEKS");
      keystore.load(keystoreIn, storepass);

    } catch (IOException | GeneralSecurityException e) {
      System.err.printf("Failure! %s%n", e.getMessage());
      System.exit(1);
    }

    SecretKey key = null;
    try {

      Path keypath = FileSystems.getDefault().getPath(filename);
      String keystring = Files.lines(keypath).findFirst().get();
      
      Base64.Decoder decoder = Base64.getDecoder();
      byte[] bytes = decoder.decode(keystring);
      key = new SecretKeySpec(bytes, 0, bytes.length, "AES");

      KeyStore.SecretKeyEntry ske = new KeyStore.SecretKeyEntry(key);
      KeyStore.PasswordProtection prot = new KeyStore.PasswordProtection(keypass);
      keystore.setEntry(alias, ske, prot);

    } catch (IOException | GeneralSecurityException e) {
      System.err.printf("Failure! %s%n", e.getMessage());
      System.exit(1);
    }

    try (FileOutputStream keyStoreOut = new FileOutputStream(keystoreFile)) {
      keystore.store(keyStoreOut, storepass);
    } catch (IOException | GeneralSecurityException e) {
      System.err.printf("Failure! %s%n", e.getMessage());
      System.exit(1);
    }
  }

  private static void showUsageAndExit(int code) {
    showUsage();
    System.exit(code);
  }

  private static void showUsage() {
    System.err.printf("%s [OPTION]%n", ImportBase64SecretKey.class.getName());
    System.err.println("");
    System.err.println("Imports base64 encoded secret key to an existing keystore");
    System.err.println("");
    System.err.println("Options:");
    System.err.println("");
    System.err.println(" -alias <alias>        alias name of the entry to process");
    System.err.println(" -file <filename>      output file name (default is write to stdout)");
    System.err.println(" -help                 show this message and exit");
    System.err.println(" -keypass <arg>        key password");
    System.err.println(" -keystore <keystore>  keystore name");
    System.err.println(" -storepass <arg>      keystore password");
  }

  private static boolean isNullOrEmpty(String value) {
    return (value == null || (value != null && value.isEmpty()));
  }
}
