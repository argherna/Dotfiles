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
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

class LocalDirectoryServer {

  private static final Pattern INTEGER_PATTERN = 
    Pattern.compile("(?<![-.])\\b[0-9]+\\b(?!\\.[0-9])");

  private static final int DEFAULT_HTTP_PORT = 8086;
 
  private static final Logger LOGGER = Logger.getLogger(
    LocalDirectoryServer.class.getName());

  private static final String HTTP_DATE_LOG_FORMAT = "[dd/MMM/yyyy HH:mm:ss]";
 
  private final Map<String, Path> directoriesToServe = new HashMap<>();

  private final HttpServer httpServer;

  public static void main (String... args) {

    if (args.length == 0) {
      System.err.printf(
        "Usage: %s [port] <directory-to-serve:server-path> ...%n",
        LocalDirectoryServer.class.getName());
      System.exit(1);
    }

    int port = DEFAULT_HTTP_PORT;
    Map<String, Path> directoriesToServe = new HashMap<>();

    for (String arg : args) {
      if (INTEGER_PATTERN.matcher(arg).matches()) {
        port = Integer.valueOf(arg);
      } else {
        List<String> components = List.of(arg.split(":"));
        Path directory = Paths.get(components.get(0));
        if (directory.toFile().exists()) {
          directoriesToServe.put(components.get(1), directory);
        } else {
          System.err.printf("%s not found, skipping!%n", arg);
        }
      }
    }

    final LocalDirectoryServer server;

    try {
      server = new LocalDirectoryServer(port, directoriesToServe);

      Runtime.getRuntime().addShutdownHook(new Thread(){
        @Override
        public void run() {
          server.shutdown();
        }
      });

      server.serve();
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

  LocalDirectoryServer(int port, Map<String, Path> directoriesToServe) 
    throws IOException {
    httpServer = HttpServer.create(new InetSocketAddress(port), 0);
    this.directoriesToServe.putAll(directoriesToServe);
  }

  void serve() {
    LOGGER.config("Starting HTTP server...");
    for (String path : directoriesToServe.keySet()) {
      HttpHandler localDirectoryHttpHandler = new LocalDirectoryHttpHandler(
        directoriesToServe.get(path));
      HttpContext context = httpServer.createContext(path, localDirectoryHttpHandler);
      LOGGER.config(String.format("Server ready at http://localhost:%1$d%2$s",
        httpServer.getAddress().getPort(), context.getPath()));
    }
    httpServer.start();
  }

  void shutdown() {
    LOGGER.warning("Stopping HTTP server...");
    httpServer.stop(0);
  }

  /**
   * Handles requests for static files from a directory.
   */
  private static class LocalDirectoryHttpHandler implements HttpHandler {

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

      String filename = exchange.getRequestURI().getPath().substring(1);
      Path file = path.resolve(filename);
      LOGGER.fine(String.format("Serving %s", file.toFile().getAbsolutePath()));
      int status = 200;
      byte[] content = new byte[0];
      String contentType = "";
      if (!file.toFile().exists()) {
        status = 404;
        contentType = TYPES.get("html");
        content = getErrorHtml(status, "Not Found", 
          exchange.getRequestURI().getPath() + " not found on the server.")
          .getBytes();
      } else {
        try {
          content = Files.readAllBytes(file);
          String extension = getFileExtension(filename);
          if (TYPES.containsKey(extension)) {
            contentType = TYPES.get(extension);
          } else {
            contentType = TYPES.get("default");
          }
          LOGGER.fine(String.format("content type = %s", contentType));
        } catch (Exception e) {
          if (e instanceof IOException) {
            throw (IOException) e;
          } else {
            status = 500;
            contentType = TYPES.get("html");
            content = getErrorHtml(status, 
              "Internal Server Error", 
              "Error occurred serving " 
              + exchange.getRequestURI().getPath() 
              + " " + e.getMessage()).getBytes();
          }
        }
      }

      try (OutputStream out = exchange.getResponseBody()) {
        Headers h = exchange.getResponseHeaders();
        h.add("Content-Type", contentType);
        if (contentType.equals(TYPES.get("json")) || contentType.equals(TYPES.get("js"))) {
          h.add("Access-Control-Allow-Origin", "*");
          h.add("Access-Control-Allow-Headers", "origin, content-type, accept");
        }

        exchange.sendResponseHeaders(status, content.length);

        out.write(content);
        out.flush();
        // Log the request
        LOGGER.info(
            String.format("%1$s - - %2$s \"%3$s %4$s\" %5$d -", exchange.getRemoteAddress().getAddress().toString(),
                new SimpleDateFormat(HTTP_DATE_LOG_FORMAT).format(new Date()), exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(), status));
        exchange.close();
      }
    }

    private String getFileExtension(String filename) {
      int lastDot = filename.lastIndexOf('.');
      return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    private String getErrorHtml(int status, String error, String text) {
      return String.format(ERROR_HTML_TEMPLATE, status, error, text);
    }

    private static String getLastPathComponent(String uriPath) {
      String[] pathComponents = uriPath.split("/");
      return pathComponents[pathComponents.length - 1];
    }
  }
}