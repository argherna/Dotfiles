/*bin/mkdir -p /tmp/.java/classes 2> /dev/null

# Compile the program.
#
javac -d /tmp/.java/classes $0

# Run the compiled program only if compilation succeeds.
#
[[ $? -eq 0 ]] && {

  if [[ -f $HOME/Scripts/logging.properties ]]; then
    java -Djava.util.logging.config.file=$HOME/Scripts/logging.properties \
      -cp /tmp/.java/classes $(basename ${0%.*}) "$@"
  else 
    java -cp /tmp/.java/classes $(basename ${0%.*}) "$@"
  fi
}
exit
*/
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

class LocalDirectoryServer {

  private static final int DEFAULT_HTTP_PORT = 8086;

  private static final int DEFAULT_HTTPS_PORT = 4436;
 
  private static final Logger LOGGER = Logger.getLogger(
    LocalDirectoryServer.class.getName());

  private static final String DEFAULT_KEYSTORE = System.getProperty("user.home") + 
    System.getProperty("file.separator") + ".keystore";

  private static final String DEFAULT_KEYSTORE_PASSWORD = "changeit";
  
  private static final String DEFAULT_TRUSTSTORE = System.getProperty("java.home") +
     System.getProperty("file.separator") + "lib" + 
     System.getProperty("file.separator") + "security" + 
     System.getProperty("file.separator") + "cacerts";
  
  private static final String DEFAULT_TRUSTSTORE_PASSWORD = "changeit";

  /**
   * HTTP server that serves files out of a directory.
   * 
   * <p>
   * Specify each folder on the command line using 
   * {@code directory-to-serve:server-path} for each directory on the command 
   * line. For example, to serve a directory {@code /home/user/json} with the 
   * server path {@code json}, you would set {@code /home/user/json:/json}.
   * 
   * <p>
   * You can set the port. The default is {@value #DEFAULT_HTTP_PORT}.
   */
  public static void main (String... args) {
    if (args.length == 0) {
      System.err.printf(
        "Usage: %s [port] <directory-to-serve:server-path> ...%n",
        LocalDirectoryServer.class.getName());
      System.exit(2);
    }

    int httpPort = DEFAULT_HTTP_PORT;
    int httpsPort = DEFAULT_HTTPS_PORT;
    
    Map<String, Path> httpDirsToServe = new HashMap<>();
    Map<String, Path> httpsDirsToServe = new HashMap<>();
    
    try {
      for (int i = 0; i < args.length; i++) {
        String arg = args[i];
        if (arg.equals("-H")) {
          httpPort = Integer.valueOf(args[++i]);
        } else if (arg.equals("-S")) {
          httpsPort = Integer.valueOf(args[++i]);
        } else if (arg.startsWith("@")) {
          processFileArgs(httpDirsToServe, httpsDirsToServe, httpPort, httpsPort, arg.substring(1));
        } else {
          processDirectoryToServeArgument(arg, httpDirsToServe, httpsDirsToServe);
        }
      }
      if (!httpsDirsToServe.isEmpty()) {
        InetSocketAddress address = new InetSocketAddress(httpsPort);
        String keystoreName = 
          System.getProperty("javax.net.ssl.keyStore") == null ? 
            DEFAULT_KEYSTORE : System.getProperty("javax.net.ssl.keyStore");
        char[] keystorePassword = 
          System.getProperty("javax.net.ssl.keyStorePassword") == null ? 
            DEFAULT_KEYSTORE_PASSWORD.toCharArray() : 
              System.getProperty("javax.net.ssl.keyStorePassword").toCharArray();
        String keystoreType = 
          System.getProperty("javax.net.ssl.keyStoreType") == null ? 
            KeyStore.getDefaultType() : 
              System.getProperty("javax.net.ssl.keyStoreType");
        String truststoreName = 
          System.getProperty("javax.net.ssl.trustStore") == null ? 
            DEFAULT_TRUSTSTORE : System.getProperty("javax.net.ssl.trustStore");;
        char[] truststorePassword = 
          System.getProperty("javax.net.ssl.trustStorePassword") == null ? 
            DEFAULT_TRUSTSTORE_PASSWORD.toCharArray() : 
              System.getProperty("javax.net.ssl.trustStorePassword").toCharArray();
        String truststoreType = 
          System.getProperty("javax.net.ssl.trustStoreType") == null ? 
            KeyStore.getDefaultType() : 
              System.getProperty("javax.net.ssl.trustStoreType");
    
        final KeyStore keystore = KeyStore.getInstance(keystoreType);
        keystore.load(new FileInputStream(new File(keystoreName)), keystorePassword);
        final KeyManagerFactory kmf = 
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keystore, keystorePassword);

        final KeyStore truststore = KeyStore.getInstance(truststoreType);
        truststore.load(new FileInputStream(new File(truststoreName)), truststorePassword);
        final TrustManagerFactory tmf = 
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(truststore);

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        HttpsServer httpsServer = HttpsServer.create(address, 0);
        setExecutor(httpsServer);
        addHandlersForPaths(httpsServer, httpsDirsToServe);
        addShutdownHook(httpsServer);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
          @Override
          public void configure(final HttpsParameters params) {
            final SSLContext ssl = getSSLContext();
            final SSLParameters sslParams = ssl.getDefaultSSLParameters();
            params.setSSLParameters(sslParams);
          }
        });
        httpsServer.start();
      }
      
      if (!httpDirsToServe.isEmpty()) {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(httpPort),
          0);
        setExecutor(httpServer);
        addHandlersForPaths(httpServer, httpDirsToServe);
        addShutdownHook(httpServer);
        httpServer.start();
      }
    } catch (Exception e) {
      System.err.printf("%s%n", e.getMessage());
      if (e.getCause() != null) {
        e.getCause().printStackTrace(System.err);
      } else {
        e.printStackTrace(System.err);
      }
      System.exit(1);
    }
  }

  private static void processFileArgs(Map<String, Path> httpDirsToServe, 
    Map<String, Path> httpsDirsToServe, int httpPort, int httpsPort, 
    String name) throws IOException {
    String filename = name;
    // Prepend the current directory if the path isn't absolute.
    if (!filename.startsWith(System.getProperty("file.separator"))) {
      filename = System.getProperty("user.dir") + 
        System.getProperty("file.separator") + name;
    }
    File f = new File(filename);
    try (FileReader fr = new FileReader(f); 
      BufferedReader br = new BufferedReader(fr)) {
      String line = null;
      while ((line = br.readLine()) != null) {
        if (line.startsWith("-H")) {
          httpPort = Integer.valueOf(line.split(" ")[1]);
        } else if (line.startsWith("-S")) {
          httpsPort = Integer.valueOf(line.split(" ")[1]);
        } else {
          processDirectoryToServeArgument(line, httpDirsToServe, httpsDirsToServe);
        }
      }
    }
  }

  private static void processDirectoryToServeArgument(String arg, 
    Map<String, Path> httpDirsToServe, Map<String, Path> httpsDirsToServe) {
    String[] service = arg.split(":");
    if (service.length == 2) {
      putIfPathExists(httpDirsToServe, service[0], service[1]);
    } else if (service.length == 3) {
      if (service[2].equalsIgnoreCase("secure")) {
        putIfPathExists(httpsDirsToServe, service[0], service[1]);
      } else {
        putIfPathExists(httpDirsToServe, service[0], service[1]);
      }
    } else {
      throw new IllegalArgumentException("Can't parse argument " + arg 
        + "! Use <dir-to-serve:path[:secure]> when specifying " 
        + "directories to serve.");
    }
  }

  private static void putIfPathExists(Map<String, Path> dirs, 
    String localDirectory, String serverPath) {
    Path p = Paths.get(localDirectory);
    if (p.toFile().isDirectory()) {
      dirs.put(serverPath, p);
    } else {
      LOGGER.fine(localDirectory + 
        " is either not a directory or doesn't exist!");
    }
  }

  private static void setExecutor(HttpServer httpServer) {
    httpServer.setExecutor(Executors.newCachedThreadPool());
  }

  private static void addShutdownHook(final HttpServer httpServer) {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        LOGGER.warning("Stopping HTTP server...");
        httpServer.stop(0);
      }
    });
  }

  private static void addHandlersForPaths(HttpServer httpServer, 
    Map<String, Path> directoriesToServe) {
    for (String path : directoriesToServe.keySet()) {
      httpServer.createContext(path, 
        new LocalDirectoryHttpHandler(directoriesToServe.get(path)));
    }
  }

  /**
   * Handles requests for static files from a directory.
   */
  private static class LocalDirectoryHttpHandler implements HttpHandler {

    private static final int HTTP_OK = 200;

    private static final int HTTP_NOT_FOUND = 404;

    private static final String ERROR_HTML_TEMPLATE = 
      "<html><head><title>%1$d %2$s</title></head><body><h1>%2$s</h1>"
      + "<p>%3$s</p></body></html>";

    private static final Map<String, String> TYPES;

    static {
      Map<String, String> types = new HashMap<>();
      types.put("css", "text/css");
      types.put("gif", "image/gif");
      types.put("html", "text/html");
      types.put("jpg", "image/jpeg");
      types.put("js", "application/javascript");
      types.put("json", "application/json");
      types.put("png", "image/png");
      types.put("sh", "text/x-shellscript; charset=us-ascii");
      types.put("svg", "image/svg+xml");
      types.put("txt", "text/plain");
      types.put("default", "application/octet-stream");
      TYPES = Collections.unmodifiableMap(types);
    }

    private final Path path;

    private LocalDirectoryHttpHandler(Path path) {
      this.path = path;
      LOGGER.config(String.format("Serving directory %s", path.toFile().getAbsolutePath()));
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String filename = exchange.getRequestURI().getPath()
        .replace(exchange.getHttpContext().getPath() + "/", "");
      Path file = path.resolve(filename);
      LOGGER.fine(String.format("Serving %s", file.toFile().getAbsolutePath()));
      int status = HTTP_OK;
      byte[] content = new byte[0];
      String contentType = "";
      if (!file.toFile().exists()) {
        status = HTTP_NOT_FOUND;
        contentType = TYPES.get("html");
        content = getErrorHtml(status, "Not Found", 
          exchange.getRequestURI().getPath() + " not found on the server.")
          .getBytes();
      } 

      if (content.length > 0) {
        doSend(exchange, contentType, content, status);
      } else {
        doSend(exchange, file, status);
      }
    }

    private void doSend(HttpExchange exchange, Path file, int status)
      throws IOException {
      int contentLength = (int) file.toFile().length();
      String contentType = Files.probeContentType(file);
      LOGGER.fine("file length = " + contentLength + ", contentType = " + contentType);

      Headers h = exchange.getResponseHeaders();
      h.add("Content-Type", contentType);
      doMinimalCORS(contentType, h);
      exchange.sendResponseHeaders(status, contentLength);
      if (contentLength > 0) {
        Files.copy(file, exchange.getResponseBody());
      }
      exchange.close();
    }

    private void doSend(HttpExchange exchange, String contentType, 
      byte[] content, int status) throws IOException {
      Headers h = exchange.getResponseHeaders();
      h.add("Content-Type", contentType);
      doMinimalCORS(contentType, h);

      int contentLength = (content != null && content.length > 0) ? 
        content.length : -1;
      exchange.sendResponseHeaders(status, contentLength);
      if (contentLength > 0) {
        try (OutputStream out = exchange.getResponseBody()) {
          out.write(content);
          out.flush();
        }
      }
      exchange.close();
    }

    private void doMinimalCORS(String contentType, Headers responseHeaders) {
      if (contentType.equals(TYPES.get("json")) || 
        contentType.equals(TYPES.get("js"))) {
        responseHeaders.add("Access-Control-Allow-Origin", "*");
        responseHeaders.add("Access-Control-Allow-Headers", "origin, content-type, accept");
      }
    }

    private String getErrorHtml(int status, String error, String text) {
      return String.format(ERROR_HTML_TEMPLATE, status, error, text);
    }
  }
}
