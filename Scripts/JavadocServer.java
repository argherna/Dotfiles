
/*bin/mkdir -p /tmp/.java/classes 2> /dev/null

# Compile the program.
#
javac -d /tmp/.java/classes $0

# Run the compiled program only if compilation succeeds.
#
[[ $? -eq 0 ]] && java \
  -cp /tmp/.java/classes $(basename ${0%.*}) "$@"
exit
*/
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * HTTP Server that serves Javadoc found on local host.
 * 
 * <p>
 * This server requires at least Java 1.8 to run.
 * 
 * <p>
 * The server assumes that a properly installed JDK and Maven are available 
 * locally. It looks up the Javadoc zip file from locations specified in the 
 * user preferences. If you are running this for the first time, navigate to 
 * {@link http://localhost:8085/} for getting started instructions.
 * 
 */
class JavadocServer {
    
  // → → Hack! ← ← I hate embedding whole HTML documents. To see what these 
  // say, copy the strings to their own files and run 
  // base64 -D -i <filename> | gunzip -c on each one.
  
  static final String GETTING_STARTED_HTML_GZ_B64 = "H4sICKWH31oAA2dldHRpbmctc"
    + "3RhcnRlZC5odG1sAJ1WTXvaOBC+76/Q+tA9dLFwl2ZDa+iTbSAlIR8bSEh6ySPbAgtkyZFk"
    + "E+fX70gGGrIku80FrBnNO98zCn89PP86vr3oodRkvPtLWP8hFKaUJPYDPjkTC5QqOu14qTG"
    + "5/oSxNiRe5MSkfiSl0UaRPE6EH8sMbwi45Qd+E8da/6D5GYNbWntIUd7xtKk41SmlxkNMGD"
    + "pTzFRATskf+61Ge3b9dyu57S+Xk9F8cPhdDHsT8fB1TkeTfp4es+XRxY0K5kkySM/pjBVBf"
    + "3n+8f7yqOzfnCfH31seipXUWio2Y6LjESFFlclCeyu/DDOcdg9KwjiJOEVDGROOjklJEhmH"
    + "uGbbWOB1MMJIJtVKOmElijnRuuPFUhjCBFUrZBu+oHtEjWFihkaGKEMTQAk27Hz9hdBfdCo"
    + "VRYW2d03KNDJS8t9RJQu0ZJwjQWkCNKSpQTlkgSoqYqp9dLt1gwh0czpEUwaemJQYxKVcaK"
    + "RZBu4pC2DBP230hoDVDWOZgI/vuPn85SHjqKRKMwnBgsx5CPTIBMzqeFfjfmPfQ5B2kRAuB"
    + "e14Qnpf3s3MZye8KaMnBqLR7WjcO0WuaKBm5hBYXxd1lSQmwU+dgbO3QXsK0rsZ9y7PDoZ3"
    + "4Nzdde9yNDg/q81z160nVkRBhSFT5WBYoSERW8wMinGLIMBrJEgGt1fpHlFVPhXbKbpDfJ4"
    + "sGiCud0pu0dZ0Koyq0IJCnUNES8ILgMFDFimiKmzNcT/XTJmC8FMSp1BZGoOiwN/3m3fBXu"
    + "DDAX+FogMsjb/JjFp2Y78AnrOmQTj3H1nu4f8yof1zJjTaftNvvaDf8e5A//9XHzR/Un/Qf"
    + "EF50HxFs9WKnyfEEW0qX8tv9qGhaC7flt+ETknBzQ8Xr6A0NbYFatGxn33ADp0Zqaq32fwC"
    + "xfbDpp+edtoWXIhd/4f2wmY24XzXmBqnAIBimDIRRUSjjIgK1eNj3QMrNJhEyYqzjt6aY2P"
    + "DoKlB3o03IoxvkW24XMCBBVqIipixtYBgNL6gJCqg31MQI3lOYb4xYY/I7iSHhOTUEa4uh9"
    + "rB2AMAFJmtm1rr60Yqel8waw2MUkq0QcHGzLV/qwSvJC0ojG83kjOyoBuD9JYxAiYVEzEvE"
    + "lrPe1cd/uvht7PeBj+WeeUCTBPmAuCGPolkSW0ImAFdS+G2gI8mKbWk36wvJKnqnWJRVCFQ"
    + "SJ7t9BkzaRG56UzULLU1ig+lsVCwwbmMoBq1oQqPYsVy6L1BlktlLgBEj2Fl+XbAe91d1BC"
    + "Trt1AzPE2ZsPKl9Ym5ZYusn2xteH+HZJ69WpnANIqftOLZP78QTLXu54fBZ2exvG8f3wwKP"
    + "cO3qvJ+2Hr4Nu03T4pHw7nk1ERPLavB/ujk7Pr7LGlF39GxYnBe2X7ZLD38V5kLz8/Qlx7U"
    + "D8u6jcFPA/s0+sfUc3YzZEJAAA=";
 
  static final String INTERNAL_SERVER_ERROR_GZ_B64 = "H4sICLKm31oAAzUwMC5odG1s"
    + "AJ2SQW/iMBCF7/sr3FyrxkQNbFMFJASkDdDSblak7c04JnZI7NTjhNJf36CkrLRiL3vy03j"
    + "8nuYb+xfT1eT369MMcVPkox9+eyDkc0aSo2hkLuQOcc22Q4sbU8ItxmAI3ZXEcHujlAGjSU"
    + "kTaVNV4FMBu7Zj9zAF+FOzC9F0AVhIs3xogTnkDDhjxkJCGpZqYQ5NmZPrG/fKS9fPbvIa7"
    + "PdxlIXTN7mcxfJjkrEoDko+F/u7pxftZEkS8hVLReUE+1X//dddHbyskvmbayGqFYDSIhVy"
    + "aBGp5KFQFVjdXEaYnI3CJldLkqOI6ZppNNNaaR+3l0cS+BuFv1HJoXubiBrRnAAMLaqkIUI"
    + "y3fke4Tmjfq+HrtBZ84vG0jn1lqMJZ3SHDGcI2q5cpWD7uOyycBPWSaBalAaBpv+1i+zvVW"
    + "RwDnzFtg+UZsF8HNaD8aWOL5fu+H7reYv6Y5rFUeV8euvwJlo8rotPF3Y/N9XC4EHtLcJB/"
    + "10W/wbv43aCFmzLs6Fx/HRfH2dQG4sCAAA=";

  static final String METHOD_NOT_ALLOWED_GZ_B64 = "H4sICF+m31oAAzQwNS5odG1sAJ2"
    + "SUU/bMBDH3/cpTF4RMdHSjkxppYoSlhYoW1EDvKW2WztN7NR3SSifnlTJQGLby558uvvf6e"
    + "73d3gyXVw+PN1fEYlFPv4Sdg8hoRQpPwZtmCu9I9KKzciRiCV8pxQwZbsyRemujUFAm5aMa"
    + "5eZgr4nqO967jllAB85t1CtCsAhVuQjB/CQC5BCoEOURrG1Cg9tWqZfL/yzYLv66fOnqGmS"
    + "ZRZPn/XNVaJfLjOxTKJSzlRzff9ovYzzWC7EVlVe1CwG+1/XdfS44LNn3yHMGgBj1VbpkZN"
    + "qow+FqcDp70KFuRjfCpSGkzuDZJLnphE8pF3liIH+5hCuDT/0jVzVhOUpwMhhRmOqtLD90C"
    + "M5b+yfD8gZ+XPySTvPexeW4wcpWhD7SgCSolMrILptSLsGYjTBVgTC1sK6IS37FWi7Qx8Cs"
    + "6pEApb9lz/ZZ3sy+JsZldjcMpZFs0lcDyenNjm98Sc/NkEwr1+mWbKsvNdgFV8s53er4tWH"
    + "3bd1NUc6rIN5PBzsdfFvM0LaXdDx7jC3nI4f8Q0JXyWinwIAAA==";

  static final String NOT_FOUND_PAGE_GZ_B64 = "H4sICHsj3loAAzQwNC5odG1sAJ1SwU7"
    + "jMBC971eYXBExEaFLVmmlihJIC5Tdoga4pbZbO03s1DNJKF+/qZLtSoi97MlPb2be6L1xeD"
    + "KZXz+/Pt0QiUU++hZ2DyGhFCk/gBbmSm+JtGI9dCRiCT8oBUzZtkxRuitjENCmJePaZaagR"
    + "4L6rueeUwbwl3ML1XYBOMSKfOgA7nMBUgh0iNIoNlbhvqVlenHlnwWb5U+fv0ZNkyyyePKm"
    + "728S/X6diUUSlXKqmtunF+tlnMdyLjaq8qJmfrn7dVtHL3M+ffMdwqwBMFZtlB46qTZ6X5g"
    + "KnN4XKszF6NEgiUyleUg74uCe/rEfrgzf9/1c1YTlKcDQYUZjqrSwvdYhMG/kn/vkjBwFT1"
    + "oZ71gvR89StLZ3lQAUnHDDqkJoJE0KRLcz636m7NfRdl8PgVlVIgHL/usE2ecLZPBV3pVYP"
    + "zCWRdNxXA/GpzY5vffHd+sgmNXvkyxZVN5HsIyvFrPHZfHhw/b7qpohHdTBLB5c7nTx77xD"
    + "2jnosu0ibcM5/LXfkhABJoICAAA=";

  static final String BAD_REQUEST_GZ_B64 = "H4sICFcj3loAAzQwMC5odG1sAJ2SQXObMB"
    + "CF7/0VCtdMkJkSN3SwZ9w4pLbTOokzJskNS7IlDBLWLhDy64sDTTud9tKT3jytdud9q/Bku"
    + "rx8eLq9IhLzbPwh7A5CQikSfhStzJTeE2nFduRIxAI+UwqYsH2RoHQ3xiCgTQrGtctMTt8N"
    + "6rueO6AM4Jfn5qqtAnCIFdnIAWwyAVIIdIjSKHZWYdPaMvl44Z8Fu/Wdz5+iuo5X6Wz6rG+"
    + "uYv1ymYpVHBVyrurr20frpZzP5FLsVOlF9fL8cH9dRY9LPn/2HcKsATBW7ZQeOYk2uslNCU"
    + "6fCxVmYvwl4eReHEoBGNLOOuanPwGEG8Ob/gVXFWFZAjBymNGYKC1s3+2IzBv7gwE5I7+1P"
    + "Gkbee8VxfhBijb62xWpEyDaICk1FxbQGE42DcG2AoSthHVDWvSDaTu5l8CsKpCAZf+1jvTP"
    + "baTwN/al2H5jLI3mk1k1nJza+PTGn3zdBsGiepmm8ar0XoP17GK1+L7OX33Yf9qUC6TDKlj"
    + "MhucHnf+bfUi7BB3lDm4L6fjvfgAHI4IajgIAAA==";

  static final int HTTP_OK = 200;

  static final int HTTP_BAD_REQUEST = 400;

  static final int HTTP_NOT_FOUND = 404;

  static final int HTTP_METHOD_NOT_ALLOWED = 405;

  static final int HTTP_INTERNAL_SERVER_ERROR = 500;

  private static final int DEFAULT_HTTP_PORT = 8084;

  private static final Logger LOGGER = 
    Logger.getLogger(JavadocServer.class.getName());

  private static final String SYSPROP_FILE_SEP = 
    System.getProperty("file.separator");

  private static final Map<String, String> TYPES;
  
  static {
    Map<String, String> types = new HashMap<>();
    types.put("css", "text/css");
    types.put("gif", "image/gif");
    types.put("html", "text/html");
    types.put("jpg", "image/jpeg");
    types.put("js", "application/javascript");
    types.put("png", "image/png");
    types.put("svg", "image/svg+xml");
    types.put("woff", "application/x-font-woff");
    types.put("eot", "application/vnd.ms-fontobject");
    types.put("ttf", "application/octet-stream");
    types.put("otf", "application/octet-stream");
    types.put("zip", "application/zip");
    TYPES = Collections.unmodifiableMap(types);
  }

  /**
   * The main takes 1 command line argument that signifies the port to run the 
   * server on.
   */
  public static void main(String... args) {
    
    int argIdx = 0;
    int port = DEFAULT_HTTP_PORT;
    
    while (argIdx < args.length) {
      String arg = args[argIdx];
      switch (arg) {
        case "-h":
        case "--help":
          showUsageAndExit(2);
          break;
        default:
          try {
            port = Integer.valueOf(arg);
          } catch (NumberFormatException e) {
            LOGGER.config(
              String.format("Invalid port number %s, defaulting to %d%n", 
              arg, DEFAULT_HTTP_PORT));
            port = DEFAULT_HTTP_PORT;
          }
          break;
      }
      argIdx++;
    }

    final HttpServer server;
    
    try {
      server = HttpServer.create(new InetSocketAddress(port), 0);
    
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          LOGGER.warning("Stopping HTTP server...");
          server.stop(0);
        }
      });

      Filter getRequestsOnlyFilter = new AllowGetRequestsOnlyFilter();
      Filter internalServerErrorFilter = new InternalServerErrorFilter();
      Filter serverHeaderFilter = new ServerHeaderFilter();
  
      HttpHandler indexHandler = new IndexHandler();
      HttpHandler jdkDocsHandler = new JdkDocsHandler();
      HttpHandler m2Handler = new M2Handler();
    
      HttpContext ctx0 = server.createContext("/", indexHandler);
      List<Filter> f0 = ctx0.getFilters();
      f0.add(getRequestsOnlyFilter);
      f0.add(internalServerErrorFilter);
      f0.add(serverHeaderFilter);
      
      HttpContext ctx1 = server.createContext("/jdk", jdkDocsHandler);
      List<Filter> f1 = ctx1.getFilters();
      f1.add(getRequestsOnlyFilter);
      f1.add(internalServerErrorFilter);
      f1.add(serverHeaderFilter);
  
      HttpContext ctx2 = server.createContext("/m2", m2Handler);
      List<Filter> f2 = ctx2.getFilters();
      f2.add(getRequestsOnlyFilter);
      f2.add(internalServerErrorFilter);
      f2.add(serverHeaderFilter);
  
      LOGGER.fine("Starting HTTP server...");
      server.start();
    } catch (Exception e) {
      System.err.printf("%s", e.getMessage());
      System.exit(1);
    }
  }

  private static void showUsageAndExit(int status) {
    showUsage();
    System.exit(status);
  }

  private static void showUsage() {
    System.err.printf("Usage: %s [port]%n", JavadocServer.class.getName());
    System.err.println();
    System.err.println("Serves content from javadoc jars stored locally in zip"
      +"/jar files.");
    System.err.println("Documentation paths and types are read from user "
      + "preferences.");
    System.err.println("Start the server and open http://localhost:port for " 
      + "how to get started.");
    System.err.println();
    System.err.println("Arguments:");
    System.err.println();
    System.err.println(" port          port the server will listen on "
     + "(default is " + DEFAULT_HTTP_PORT + ")");
    System.err.println();
    System.err.println("Options:");
    System.err.println();
    System.err.println(" -h, --help    show this help and exit");
  }
 
  /**
   * Decodes a base64-encoded String to a byte array, then decompresses it
   * and returns the resulting byte array.
   * 
   * @param encodedCompressed base64-encoded String of GZipped compressed
   *   data
   * @return a byte array of the decompressed data.
   * @throws IOException if an IOException occurs while decompressing the
   *   decoded String.
   */
  static byte[] decodeDecompress(String encodedCompressed) throws IOException {
    byte[] decodedCompressed = Base64.getDecoder().decode(encodedCompressed);
    ByteArrayInputStream bis = new ByteArrayInputStream(decodedCompressed);
    try (GZIPInputStream gzin = new GZIPInputStream(bis)) {
      return gzin.readAllBytes();
    }
  }
 
  /**
   * Catches RuntimeExceptions to log them and return a 500 status.
   */
  private static class InternalServerErrorFilter extends Filter {

    @Override
    public String description() {
      return "Handles runtime exceptions by logging an error and setting the HTTP status to 500";
    }

    @Override
    public void doFilter(HttpExchange exchange, Filter.Chain chain) 
      throws IOException {
      try {
        chain.doFilter(exchange);
      } catch (RuntimeException e) {

        // Get the root cause for logging.
        Throwable cause = e.getCause();
        Throwable c0 = cause;
        while (c0 != null) {
          c0 = cause.getCause();
          if (c0 != null) {
            cause = c0;
          }
        }
        LOGGER.log(Level.SEVERE, 
          "Unhandled exception! Returning Internal Server Error", cause);
        int status = HTTP_INTERNAL_SERVER_ERROR;
        byte[] content = decodeDecompress(INTERNAL_SERVER_ERROR_GZ_B64);
        exchange.sendResponseHeaders(status, content.length);
        try (OutputStream out = exchange.getResponseBody()) {
          out.write(content);
          out.flush();
        }
        exchange.close();
      }
    }
  }

  /**
   * Sets the {@code "Server"} HTTP header.
   */
  private static class ServerHeaderFilter extends Filter {

    @Override
    public String description() {
      return "Sets the Server response header";
    }

    @Override
    public void doFilter(HttpExchange exchange, Filter.Chain chain) 
      throws IOException {
      Headers responseHeaders = exchange.getResponseHeaders();
      responseHeaders.put("Server", List.of(JavadocServer.class.getName()));
      chain.doFilter(exchange);
    }
  }

  /**
   * Responds with a 405 status for any method besides {@code GET} or 
   * {@code HEAD}.
   */
  private static class AllowGetRequestsOnlyFilter extends Filter {

    @Override
    public String description() {
      return "Allows GET requests only.";
    }

    @Override
    public void doFilter(HttpExchange exchange, Filter.Chain chain) 
      throws IOException {
      if (!exchange.getRequestMethod().equals("GET")) {
        int status = HTTP_METHOD_NOT_ALLOWED;
        String contentType = TYPES.get("html");
        byte[] content = decodeDecompress(METHOD_NOT_ALLOWED_GZ_B64);
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.put("Allow", List.of("GET"));
        responseHeaders.put("Content-Type", List.of(contentType));
        exchange.sendResponseHeaders(status, content.length);
        try (OutputStream out = exchange.getResponseBody()) {
          out.write(content);
          out.flush();
        }
        exchange.close();
      } else {
        chain.doFilter(exchange);
      }
    }
  }

  /**
   * Base HttpHandler implementation for rendering javadoc pages from an 
   * archive file.
   */
  private static abstract class JavadocHandler implements HttpHandler {

    private static final int BUF_SIZE = 0x1000;

    String getFileExtension(String filename) {
      int lastDot = filename.lastIndexOf('.');
      return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    byte[] getContentFromZip(String zipFilename, String filename) 
      throws IOException {
      byte[] content = new byte[0];
      try (ZipFile zf = new ZipFile(zipFilename)) {
        Enumeration<? extends ZipEntry> entries = zf.entries();
        while (entries.hasMoreElements()) {
          ZipEntry zipEntry = (ZipEntry) entries.nextElement();
          if (zipEntry.getName().equals(filename)) {
            content = toByteArray(zf.getInputStream(zipEntry));
            break;
          }
        }
      }
      return content;
    }

    /**
     * Convert the InputStream to a byte array and returns it.
     * 
     * @param from InputStream to convert.
     * 
     * <p>
     * Shamelessly copied and adapted from com.google.common.io.ByteStreams.copy() and 
     * com.google.common.io.ByteStreams.toByteArray().
     */
    private byte[] toByteArray(InputStream from) throws IOException {
      ByteArrayOutputStream to = new ByteArrayOutputStream();
      byte[] buf = new byte[BUF_SIZE];
      while (true) {
        int read = from.read(buf);
        if (read == -1) {
          break;
        }
        to.write(buf, 0, read);
      }
      return to.toByteArray();
    }

    void doSend(HttpExchange exchange, String contentType, byte[] content, 
      int status) throws IOException {
      Headers h = exchange.getResponseHeaders();
      h.add("Content-Type", contentType);
      
      int contentLength = (content.length == 0) ? -1 : content.length;
      LOGGER.fine(
        String.format("[status = %d, contentType = %s, contentLength = %d]",
          status, contentType, contentLength));
      exchange.sendResponseHeaders(status, contentLength);
      if (contentLength > 0) {
        try (OutputStream out = exchange.getResponseBody()) {
          out.write(content);
          out.flush();
        }
      }
      exchange.close();
    }
  }

  /**
   * Renders the index page or a page of documentation explaining how to get 
   * started.
   */
  private static class IndexHandler extends JavadocHandler {

    private static final String HTML_LIST_ITEM_TEMPLATE = 
      "<li><a href=\"/m2/%1$s/%2$s/%3$s/index.html\">%1$s:%2$s:%3$s</a></li>";

    private static final String INDEX_HTML_TEMPLATE = "<!DOCTYPE html><html><h"
      + "ead><link href=\"https://stackpath.bootstrapcdn.com/bootstrap/4.1.0/c"
      + "ss/bootstrap.min.css\" rel=\"stylesheet\" integrity=\"sha384-9gVQ4dYF"
      + "wwWSjIDZnLEWnxCjeSWFphJiwGPXr1jddIhOegiu1FwO5qRGvFXOdJZ4\" crossorigi"
      + "n=\"anonymous\"><title>Available Local Javadoc</title></head><body><d"
      + "iv class=\"container\"><h1>Avaliable Local Javadoc</h1><h2>JDK API</h"
      + "2><ul>%s</ul><h2>Maven Repository</h2><ul>%s</ul></div><script src=\""
      + "https://stackpath.bootstrapcdn.com/bootstrap/4.1.0/js/bootstrap.min.j"
      + "s\" integrity=\"sha384-uefMccjFJAIv6A+rW+L4AHf99KvxDjWSu1z9VI8SKNVmz4"
      + "sk7buKt/6v9KI65qnm\" crossorigin=\"anonymous\"></script></body></html"
      + ">";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      byte[] content = new byte[0];
      int status = HTTP_OK;
      String contentType = TYPES.get("html");
      Preferences javadocServer = Preferences.userRoot().node("JavadocServer");
      try {
        if (javadocServer.childrenNames().length == 0) {
          content = decodeDecompress(GETTING_STARTED_HTML_GZ_B64);
        } else if (exchange.getRequestURI().getPath().equals("/favicon.ico")) {
          status = HTTP_NOT_FOUND;
          content = decodeDecompress(NOT_FOUND_PAGE_GZ_B64);
        } else {
          Preferences m2Repos = javadocServer.node("m2-repos");
          String m2RepoPath = m2Repos.get("default", "");
          Collection<String> artifactDirs = getMavenJavadocArtifactDirectoryNames(
            m2RepoPath);
          LOGGER.fine(artifactDirs.toString());
          content = String.format(INDEX_HTML_TEMPLATE, 
            renderJdkDocsListItems(javadocServer.node("jdk-docs").keys()),
              toHtmlListElements(artifactDirs)).getBytes();
        }
        doSend(exchange, contentType, content, status);
      } catch (BackingStoreException e) {
        throw new RuntimeException(e);
      }
    }

    private String renderJdkDocsListItems(String[] keys) 
      throws BackingStoreException {
      StringBuilder sb = new StringBuilder();
      for (String key : keys) {
        sb.append("<li><a href=\"/jdk/").append(key)
          .append("/docs/api/index.html\">Java ").append(key)
          .append("</a></li>");
      }
      return sb.toString();
    }

    private Collection<String> getMavenJavadocArtifactDirectoryNames(
      String repoDir) throws IOException {
      Path mavenRepoDir = Paths.get(repoDir);
      Collection<String> dirnames = new ArrayList<>();
      if (mavenRepoDir.toFile().exists()) {
        JavadocArchiveFinder finder = new JavadocArchiveFinder(mavenRepoDir);
        Files.walkFileTree(mavenRepoDir, finder);
        dirnames.addAll(finder.getJavadocArtifactDirectoryNames());
      }
      return dirnames;
    }

    private String toHtmlListElements(Collection<String> javadocArchiveNames) {
      StringJoiner listJoiner = new StringJoiner("");
      for (String javadocArchivename : javadocArchiveNames) {
        List<String> parts = 
          Arrays.asList(javadocArchivename.split(SYSPROP_FILE_SEP));
        StringJoiner dotJoiner = new StringJoiner(".");
        parts.subList(0, parts.size() - 2).stream()
          .forEach(part -> dotJoiner.add(part));
        String listElement = String.format(HTML_LIST_ITEM_TEMPLATE, 
          dotJoiner.toString(), parts.get(parts.size() - 2), 
          parts.get(parts.size() - 1));
        listJoiner.add(listElement);
      }
      return listJoiner.toString();
    }
  }

  /**
   * Renders Javadoc pages from JDK documentation archives.
   */
  private static class JdkDocsHandler extends JavadocHandler {

    private static final int AFTER_PATH_AND_KEY_IDX = 2;

    private static final int MIN_PATH_ELEMENTS = 3; 

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      int status = HTTP_OK;
      byte[] content = new byte[0];
      String requestPath = exchange.getRequestURI().getPath();
      List<String> pathElements = Arrays.asList(
        requestPath.split(SYSPROP_FILE_SEP)).stream()
        .filter(pe -> pe != null && !pe.isEmpty())
        .collect(Collectors.toList());
      String jdkDocsKey = pathElements.get(1);
      String contentType = TYPES.get("html");
      Preferences jdkDocs = Preferences.userRoot().node("JavadocServer/jdk-docs");
      String javadocArchiveName = jdkDocs.get(jdkDocsKey, "");
      if (javadocArchiveName.isEmpty()) {
        status = HTTP_NOT_FOUND;
        content = decodeDecompress(NOT_FOUND_PAGE_GZ_B64);
      } else if (pathElements.size() < MIN_PATH_ELEMENTS) {
        status = HTTP_BAD_REQUEST;
        content = decodeDecompress(BAD_REQUEST_GZ_B64);
      } else {
        StringJoiner sj = new StringJoiner(SYSPROP_FILE_SEP);
        pathElements.subList(AFTER_PATH_AND_KEY_IDX, pathElements.size())
          .stream().forEach(p -> sj.add(p));
        String filename = sj.toString();
        contentType = TYPES.get(getFileExtension(filename));
        content = getContentFromZip(javadocArchiveName, filename);
      }
      doSend(exchange, contentType, content, status);
    }
  }

  /**
   * Renders Javadoc pages from archives in the maven repositories.
   */
  private static class M2Handler extends JavadocHandler {

    private static final int MIN_PATH_ELEMENTS = 4;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      byte[] content = new byte[0];
      int status = HTTP_OK;
      String contentType = TYPES.get("html");
      Preferences m2Repos = Preferences.userRoot()
        .node("JavadocServer/m2-repos");
      String m2RepoPath = m2Repos.get("default", "");
      String requestPath = exchange.getRequestURI().getPath();
      List<String> pathElements = 
        Arrays.asList(requestPath.split(SYSPROP_FILE_SEP))
          .stream().filter(pe -> pe != null && !pe.isEmpty())
          .collect(Collectors.toList());

      if (m2RepoPath.isEmpty()) {
        content = decodeDecompress(GETTING_STARTED_HTML_GZ_B64);
      } else if (pathElements.size() < MIN_PATH_ELEMENTS) {
        status = HTTP_BAD_REQUEST;
        content = decodeDecompress(BAD_REQUEST_GZ_B64);
      } else {
        String filename = toMavenCoordinates(m2RepoPath,
          pathElements.subList(1, pathElements.size()));
        StringJoiner fpJoiner = new StringJoiner(SYSPROP_FILE_SEP);
        pathElements.subList(MIN_PATH_ELEMENTS, pathElements.size()).stream()
          .forEach(pe -> fpJoiner.add(pe));
        String docPath = fpJoiner.toString();
        LOGGER.fine(() -> String.format("[filename = %s, docPath = %s]",
          filename, docPath));
        content = getContentFromZip(filename, docPath);
        contentType = TYPES.get(getFileExtension(docPath));
      }
      doSend(exchange, contentType, content, status);
    }

    private String toMavenCoordinates(String baseDirname, 
      List<String> components) {
      String group = components.get(0).replace(".", SYSPROP_FILE_SEP);
      String artifactId = components.get(1);
      String version = components.get(2);

      StringJoiner dashJoiner = new StringJoiner("-");
      dashJoiner.add(artifactId).add(version).add("javadoc.jar");
      String artifactName = dashJoiner.toString();

      StringJoiner fpJoiner = new StringJoiner(SYSPROP_FILE_SEP);
      fpJoiner.add(baseDirname).add(group).add(artifactId).add(version)
        .add(artifactName);
      return fpJoiner.toString();
    }
  }

  /**
   * FileVisitor that looks for directories containing jar files with a name 
   * that matches {@code *-javadoc.jar}.
   */
  private static class JavadocArchiveFinder extends SimpleFileVisitor<Path> {

    private final List<String> javadocArtifactDirectoryNames = new ArrayList<>();

    private final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("regex:.*-javadoc.jar$");

    private final Path basePath;

    private JavadocArchiveFinder(Path basePath) {
      this.basePath = basePath;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
      LOGGER.finest(String.format("Scanning %s", dir.toString()));
      return super.preVisitDirectory(dir, attrs);
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      Path name = file.getFileName();
      if (name != null && matcher.matches(name)) {
        Path subpath = file.subpath(basePath.getNameCount(), file.getNameCount() - 1);
        LOGGER.finest(String.format("Adding %s", subpath.toString()));
        javadocArtifactDirectoryNames.add(subpath.toString());
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
      LOGGER.log(Level.WARNING, String.format("Problem with %s", file.toString()), exc);
      return super.visitFileFailed(file, exc);
    }

    Collection<String> getJavadocArtifactDirectoryNames() {
      return javadocArtifactDirectoryNames;
    }
  }
}
