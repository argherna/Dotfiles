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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

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
      showUsageAndExit(2);
    }

    int httpPort = DEFAULT_HTTP_PORT;
    int httpsPort = DEFAULT_HTTPS_PORT;
    
    Map<String, Path> dirsToServeHttp = new HashMap<>();
    Map<String, Path> dirsToServeHttps = new HashMap<>();
    
    try {
      for (int i = 0; i < args.length; i++) {
        String arg = args[i];
        if (arg.equals("-H")) {
          httpPort = Integer.valueOf(args[++i]);
        } else if (arg.equals("-S")) {
          httpsPort = Integer.valueOf(args[++i]);
        } else if (arg.startsWith("@")) {
          processFileArgs(dirsToServeHttp, dirsToServeHttps, httpPort, httpsPort, arg.substring(1));
        } else {
          processDirectoryToServeArgument(arg, dirsToServeHttp, dirsToServeHttps);
        }
      }
      if (!dirsToServeHttps.isEmpty()) {
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
        addHandlersForPaths(httpsServer, dirsToServeHttps);
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
      
      if (!dirsToServeHttp.isEmpty()) {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(httpPort),
          0);
        setExecutor(httpServer);
        addHandlersForPaths(httpServer, dirsToServeHttp);
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

  private static void showUsageAndExit(int status) {
    showUsage();
    System.exit(status);
  }

  private static void showUsage() {
    System.err.printf("Usage: %s [OPTIONS] [ARGUMENTS]%n", 
      LocalDirectoryServer.class.getName());
    System.err.println();
    System.err.println("Serves content from local directories");
    System.err.println("CORS support for directories and MIME types are read "
      + "from user preferences.");
    System.err.println();
    System.err.println("Arguments:");
    System.err.println();
    System.err.println(" directory-to-serve:server-path[:secure]");
    System.err.println("                  Absolute path to serve, the url path ");
    System.err.println("                  it will be served from, and optionally ");
    System.err.println("                  to serve it under HTTPS (keyword is secure).");
    System.err.println(" @<filename>      The name of a file containing the argument ");
    System.err.println("                  described above or the options as described ");
    System.err.println("                  below, one on each line.");
    System.err.println();
    System.err.println("Options:");
    System.err.println();
    System.err.println(" -H <http-port>   Port to serve HTTP from (default is "
      + DEFAULT_HTTP_PORT + ").");
    System.err.println(" -h               Show this help and exit");
    System.err.println(" -S <https-port>  Port to serve HTTPS from (default "
      + "is " +DEFAULT_HTTPS_PORT + ").");
  }

  private static void processFileArgs(Map<String, Path> dirsToServeHttp, 
    Map<String, Path> dirsToServeHttps, int httpPort, int httpsPort, 
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
          processDirectoryToServeArgument(line, dirsToServeHttp , dirsToServeHttps);
        }
      }
    }
  }

  private static void processDirectoryToServeArgument(String arg, 
    Map<String, Path> dirsToServeHttp, Map<String, Path> dirsToServeHttps) {
    String[] service = arg.split(":");
    if (service.length == 2) {
      putIfPathExists(dirsToServeHttp  , service[0], service[1]);
    } else if (service.length == 3) {
      if (service[2].equalsIgnoreCase("secure")) {
        putIfPathExists(dirsToServeHttps, service[0], service[1]);
      } else {
        putIfPathExists(dirsToServeHttp  , service[0], service[1]);
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

    private static final int HTTP_NO_CONTENT = 204;

    private static final int HTTP_FORBIDDEN = 403;

    private static final int HTTP_NOT_FOUND = 404;

    private static final int HTTP_METHOD_NOT_ALLOWED = 405;

    private static final int HTTP_INTERNAL_SERVER_ERROR = 500;

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
      if (exchange.getRequestMethod().equals("GET")) {
        doGet(exchange);
      } else if (exchange.getRequestMethod().equals("OPTIONS")) {
        doOptions(exchange);
      } else {
        doMethodNotAllowed(exchange);
      }
    }

    private void doGet(HttpExchange exchange) throws IOException {
      Path file = path.resolve(exchange.getRequestURI().getPath()
        .replace(exchange.getHttpContext().getPath() + "/", ""));
      
      if (!file.toFile().exists()) {
        doNotFound(exchange);
      } else {
        LOGGER.fine(() -> String.format("Serving %s", file.toString()));
        doSend(exchange, file, HTTP_OK);
      }
    }

    private void doOptions(HttpExchange exchange) throws IOException {
      String node = LocalDirectoryServer.class.getSimpleName() + 
        "/corsSupport" + exchange.getRequestURI().getPath();
      try {
        if (!Preferences.userRoot().nodeExists(node)) {
          doMethodNotAllowed(exchange);
          return;
        }
  
        Preferences corsSupport = Preferences.userRoot().node(node);
        String origin = corsSupport.get("allowedOrigins", "*");
        String directory = corsSupport.get("directory", "");
        
        Headers requestHeaders = exchange.getRequestHeaders();
        
        if (directory.equals(path.toString())) {
          if (origin.equals("*")) {
            addCorsHeaders(exchange, corsSupport, origin);
          } else {
            List<String> origins = List.of(origin.split(","));
            String requestOrigin = requestHeaders.getFirst("Origin");
            if (origins.contains(URI.create(requestOrigin).getHost())) {
              addCorsHeaders(exchange, corsSupport, requestOrigin);
            } else {
              doForbidden(exchange);
              return;
            }
          }
        } else {
          doMethodNotAllowed(exchange);
          return;
        }
        doNoContent(exchange);
      } catch (BackingStoreException e) {
        LOGGER.log(Level.FINE, "Internal error!",  e);
        doInternalServerError(exchange);
        return;
      }
    }

    private void addCorsHeaders(HttpExchange exchange) {
      Preferences corsSupport = Preferences.userRoot()
        .node(LocalDirectoryServer.class.getSimpleName() + "/corsSupport" + 
          exchange.getHttpContext().getPath());
      String origin = exchange.getRequestHeaders().getFirst("Origin");
      if (origin == null) {
        origin = ""; // ¯\_(ツ)_/¯
      }
      addCorsHeaders(exchange, corsSupport, origin);
    }

    private void addCorsHeaders(HttpExchange exchange, Preferences corsSupport, 
      String origin) throws IOException {
      List<String> corsHeaders = List.of(corsSupport.get("headers", "").split(","));
      Headers responseHeaders = exchange.getResponseHeaders();
      for (String corsHeader : corsHeaders) {
        if (corsHeader.equals("Access-Control-Allow-Origin")) {
          responseHeaders.put("Access-Control-Allow-Origin", List.of(origin));
        } else if (corsHeader.equals("Access-Control-Allow-Methods")) {
          responseHeaders.put("Access-Control-Allow-Methods", List.of("GET"));
        }
      }
    }

    private void doNotFound(HttpExchange exchange) {
      doSend(exchange, TYPES.get("html"), getErrorHtml(status, "Not Found", 
      exchange.getRequestURI().getPath() + " not found on the server.")
        .getBytes(), HTTP_NOT_FOUND);
    }

    private void doMethodNotAllowed(HttpExchange exchange) 
      throws IOException {
      doSend(exchange, TYPES.get("html"), getErrorHtml(HTTP_METHOD_NOT_ALLOWED,
        "Method not allowed", exchange.getRequestMethod() + 
        " not allowed on this server!").getBytes(), HTTP_METHOD_NOT_ALLOWED);
    }

    private void doForbidden(HttpExchange exchange) throws IOException {
      doSend(exchange, TYPES.get("html"), getErrorHtml(HTTP_FORBIDDEN, 
        "Method not allowed", exchange.getRequestMethod() + 
        " not allowed on this server!").getBytes(), HTTP_FORBIDDEN);
    }

    private void doInternalServerError(HttpExchange exchange) 
      throws IOException {
      doSend(exchange, TYPES.get("html"), 
        getErrorHtml(HTTP_INTERNAL_SERVER_ERROR, "Internal Server Error", 
        "An internal server error occurred, check the logs.").getBytes(), 
        HTTP_INTERNAL_SERVER_ERROR);
    }

    private void doSend(HttpExchange exchange, Path file, int status)
      throws IOException {
      String probed = Files.probeContentType(file);
      String contentType = probed == null ? probed : TYPES.get("default");
      addContentType(exchange.getResponseHeaders(), contentType);
      if (shouldAddCorsHeaders(contentType, exchange.getHttpContext().getPath())) {
        addCorsHeaders(exchange);
      } 
      int contentLength = (int) file.toFile().length();
      LOGGER.fine("file length = " + contentLength + ", contentType = " + 
        contentType);
      exchange.sendResponseHeaders(status, contentLength);
      if (contentLength > 0) {
        Files.copy(file, exchange.getResponseBody());
      }
      exchange.close();
    }

    private void doNoContent(HttpExchange exchange) throws IOException {
      doSend(exchange, null, null, HTTP_NO_CONTENT);
    }

    private void doSend(HttpExchange exchange, String contentType, 
      byte[] content, int status) throws IOException {
      addContentType(exchange.getResponseHeaders(), contentType);
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

    private boolean shouldAddCorsHeaders(String contentType, String requestPath) {
      Preferences corsSupport = Preferences.userRoot()
        .node(LocalDirectoryServer.class.getSimpleName() + "/corsSupport" + 
          requestPath);
      return List.of(corsSupport.get("MIMETypes", "").split(","))
        .contains(contentType);
    }

    private void addContentType(Headers responseHeaders, String contentType) {
      if (contentType != null && !contentType.isEmpty()) {
        responseHeaders.add("Content-Type", contentType);
      }
    }

    private String getErrorHtml(int status, String error, String text) {
      return String.format(ERROR_HTML_TEMPLATE, status, error, text);
    }
  }
}
