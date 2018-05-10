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

    final HttpServer httpServer;
    try {
      httpServer = HttpServer.create(new InetSocketAddress(port), 0);
      Runtime.getRuntime().addShutdownHook(new Thread(){
        @Override
        public void run() {
          LOGGER.warning("Stopping HTTP server...");
          httpServer.stop(0);
        }
      });
      for (String path : directoriesToServe.keySet()) {
        httpServer.createContext(path, 
          new LocalDirectoryHttpHandler(directoriesToServe.get(path)));
      }
      httpServer.start();
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

  /**
   * Handles requests for static files from a directory.
   */
  private static class LocalDirectoryHttpHandler implements HttpHandler {

    private static final int HTTP_OK = 200;

    private static final int HTTP_NOT_FOUND = 404;

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
      int status = HTTP_OK;
      byte[] content = new byte[0];
      String contentType = "";
      if (!file.toFile().exists()) {
        status = HTTP_NOT_FOUND;
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
            status = HTTP_INTERNAL_SERVER_ERROR;
            contentType = TYPES.get("html");
            content = getErrorHtml(status, 
              "Internal Server Error", 
              "Error occurred serving " 
              + exchange.getRequestURI().getPath() 
              + " " + e.getMessage()).getBytes();
          }
        }
      }

      doSend(exchange, contentType, content, status);
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
        h.add("Access-Control-Allow-Origin", "*");
        h.add("Access-Control-Allow-Headers", "origin, content-type, accept");
      }
    }
    
    private String getFileExtension(String filename) {
      int lastDot = filename.lastIndexOf('.');
      return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    private String getErrorHtml(int status, String error, String text) {
      return String.format(ERROR_HTML_TEMPLATE, status, error, text);
    }
  }
}
