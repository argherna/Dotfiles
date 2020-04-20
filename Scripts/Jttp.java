import static java.lang.String.format;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.TRACE;
import static java.lang.System.Logger.Level.WARNING;
import static java.net.URLEncoder.encode;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.Authenticator;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.prefs.Preferences;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

@Jttp.Version(name = "Jttp", major = "1")
class Jttp implements Runnable {

    private static final Integer BUF_SZ = 0x1000;

    private static final Integer CHUNK_SZ = 0x10000;

    private static final ResourceBundle RB = ResourceBundle.getBundle("messages_jttp");

    private static final String BOUNDARY = Long.toHexString(System.currentTimeMillis());

    private static final String CRLF = "\r\n";

    private static final String FILE_SEP = AccessController
            .doPrivileged((PrivilegedAction<String>) () -> System.getProperty("file.separator"));

    private static final String LOCAL_SAVE_DIRECTORY = format("%s%s.%s",
            AccessController
                    .doPrivileged((PrivilegedAction<String>) () -> System.getProperty("user.home")),
            FILE_SEP, Jttp.class.getSimpleName().toLowerCase());

    private static final String SYS_PROP_INDENT = "jttp.indent";

    private static final Integer DEFAULT_INDENT = 2;

    private static final Integer INDENT = AccessController.doPrivileged(
            (PrivilegedAction<Integer>) () -> Integer.getInteger(SYS_PROP_INDENT, DEFAULT_INDENT));

    private static final String SYS_PROP_KEEP_TEMP_FILES = "jttp.keep.tempfiles";

    private static final System.Logger LOGGER = System.getLogger(Jttp.class.getName(), RB);

    private final PrintStream ps;

    private File tempResponse;

    private HttpURLConnection conn;

    private InputStream inStream;

    private Map<String, String> requestDataMap;

    private Map<String, List<String>> requestHeaders;

    private Map<String, File> uploadFiles;

    @Option(shortName = "A", longName = "auth", argRequired = true, argName = "user[:passwd]",
            resourceKey = "jttp.opt.auth")
    private String auth;

    @Option(shortName = "d", longName = "download", resourceKey = "jttp.opt.download")
    private boolean download;

    @Option(shortName = "h", longName = "help", resourceKey = "jttp.opt.help", isHelp = true)
    private boolean help;

    @Option(shortName = "M", longName = "mime-type", argRequired = true, argName = "mime_type",
            resourceKey = "jttp.opt.reqmimetype")
    private RequestMimeType requestMimeType = RequestMimeType.JSON;

    private Session session;

    @Option(shortName = "N", longName = "no-verify", resourceKey = "jttp.opt.noverify")
    private boolean noVerify;

    @Option(shortName = "O", longName = "offline", resourceKey = "jttp.opt.offline")
    private boolean offline;

    @Option(shortName = "o", longName = "output", argRequired = true, argName = "filename",
            resourceKey = "jttp.opt.output")
    private File outfile;

    @Option(shortName = "P", longName = "pretty-print", argRequired = true,
            argName = "format_option", resourceKey = "jttp.opt.prettyprint")
    private PrettyPrint prettyPrint = PrettyPrint.ALL;

    @Option(longName = "pre-process-script", argRequired = true, argName = "script_name",
            resourceKey = "jttp.opt.preprocessscript")
    private String preProcessScriptName;

    @Option(longName = "pre-process-script-arg", argRequired = true, argName = "script_arg",
            resourceKey = "jttp.opt.preprocessscriptarg", multipleSpecs = true)
    private String[] preProcessScriptArgs;

    @Option(longName = "post-process-script", argRequired = true, argName = "script_name",
            resourceKey = "jttp.opt.postprocessscript")
    private String postProcessScriptName;

    @Option(longName = "post-process-script-arg", argRequired = true, argName = "script_arg",
            resourceKey = "jttp.opt.postprocessscriptarg", multipleSpecs = true)
    private String[] postProcessScriptArgs;

    @Option(shortName = "p", longName = "print", argRequired = true, argName = "what",
            resourceKey = "jttp.opt.print")
    private String print = "hb";

    @Option(shortName = "R", longName = "read-only-session",
            resourceKey = "jttp.opt.readonlysession")
    private boolean readOnlySession;

    @Option(shortName = "S", longName = "session", argRequired = true, argName = "session_name",
            resourceKey = "jttp.opt.sessionname")
    private String sessionName;

    private String requestData = "";

    @Option(shortName = "v", longName = "verbose", resourceKey = "jttp.opt.verbose")
    private boolean verbose;

    @Option(shortName = "V", longName = "version", resourceKey = "jttp.opt.version",
            isVersion = true)
    private boolean version;

    @Argument(name = "method", index = 0, resourceKey = "jttp.arg.method")
    private RequestMethod method = RequestMethod.GET;

    @Argument(name = "url", index = 1, resourceKey = "jttp.arg.url", required = true)
    private String urlString;

    @Argument(name = "request_item", resourceKey = "jttp.arg.reqitem", multipleSpecs = true)
    private String[] requestItems;

    private URI url;

    /**
     * The main.
     * 
     * @param args command line arguments.
     */
    public static void main(String... args) {

        var saveDir = new File(getBaseSaveDirectory());
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }

        int returncode = CommandLine.run(new Jttp(), RB, args);
        System.exit(returncode);
    }

    /**
     * Constructs a new instance of Jttp.
     */
    Jttp() {
        ps = System.out;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * <strong>Implementation note:</strong> this method is the instance entry point for Jttp. It is
     * executed in 5 phases:
     * <ol>
     * <li>{@link #setup() setup} initializes the state, then initializes the HttpURLConnection by
     * utilizing the state. If a {@code session name} is set on the command line, that session is
     * loaded and the headers and cookies contained in it become part of the request.
     * <li>{@link #preProcess() preProcess} executes the {@code pre-process-script} if set on the
     * command line. If script execution fails, the logger will write the reasons for the failure to
     * the log file and this method will continue to execute.
     * <li>{@link #process() process} executes the Http request. Response bodies are written as
     * temporary files.
     * <li>{@link #postProcess() postProcess} executes the {@code post-process-script} if set on the
     * command line. If script execution fails, the logger will write the reasons for the failure to
     * the log file and this method will continue to execute.
     * <li>{@link #finish() finish} determines how to handle the response (print it, save it to a
     * different location on disk, etc.).
     * </ol>
     * 
     * @throws RuntimeException if an uncaught exception or RuntimeException is thrown internally.
     */
    @Override
    public void run() {
        try {

            setup();
            preProcess();
            process();
            postProcess();
            finish();

        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Set up the components needed for the run.
     */
    void setup() throws IOException, URISyntaxException {
        initializeState();
        conn = (HttpURLConnection) url.toURL().openConnection();
        if (nonNull(sessionName) && !sessionName.isEmpty()) {
            session = new Session(sessionName, conn);
            try {
                session.load();
            } catch (Exception e) {
                LOGGER.log(WARNING, "logger.warning.xmlerror", e.getMessage());
                e.printStackTrace();
            }
        }

        setRequestHeaders();
        setRequestMethod();
        if (method.hasPayload()) {
            var mimeType = requestMimeType;
            if (!requestHeaders.containsKey("Content-Type")) {
                var contentType = mimeType == RequestMimeType.MULTIPART
                        ? format("%s; boundary=%s", mimeType.getContentType(), BOUNDARY)
                        : mimeType.getContentType();
                conn.setRequestProperty("Content-Type", contentType);
            }
            if (uploadFiles.isEmpty() && (!requestDataMap.isEmpty() || nonNull(inStream))) {
                requestData = nonNull(inStream) ? readFromInStream() : renderPayload();
            }
        }
    }

    /**
     * Executes the preprocessing script after {@link Jttp#setup() setup} executes and before the
     * {@link Jttp#process() process} method executes.
     */
    void preProcess() {
        if (nonNull(preProcessScriptName) && !preProcessScriptName.isEmpty()) {
            LOGGER.log(DEBUG, RB, "logger.debug.exec.preprocess");
            var jttpScriptObject = new JttpScriptObject(conn);
            executeScript(preProcessScriptName, jttpScriptObject, preProcessScriptArgs);
        }
    }

    /**
     * Sends request data and get the response.
     * 
     * <p>
     * <strong>Implementation Note:</strong> all response bodies are stored in a temporary file that
     * is deleted after the run.
     */
    void process() throws IOException {
        requireNonNull(conn, RB.getString("error.null.connection"));
        if (!offline() && conn.getDoOutput()
                && ((nonNull(requestData) && !requestData.isEmpty()) || !uploadFiles.isEmpty())) {
            try (var out = conn.getOutputStream()) {
                if (nonNull(requestData) && !requestData.isEmpty()) {
                    sendRequestData(out);
                } else {
                    sendMultipartData(out);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        InputStream responseStream = null;
        try {
            responseStream = offline() ? null : getInputStream();
        } catch (IOException e) {
            responseStream = conn.getErrorStream();
        } finally {
            if (nonNull(responseStream)) {
                tempResponse = File.createTempFile(
                        format("%s-response", getClass().getSimpleName().toLowerCase()),
                        ".download");
                boolean deleteTempFiles =
                        !AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> Boolean
                                .getBoolean(SYS_PROP_KEEP_TEMP_FILES));
                if (deleteTempFiles) {
                    tempResponse.deleteOnExit();
                }
                var xfered = responseStream.transferTo(new FileOutputStream(tempResponse));
                LOGGER.log(TRACE, "logger.trace.bytes.transferred", xfered,
                        tempResponse.toString());
            }
            conn.disconnect();
        }
    }

    /**
     * Executes the postprocessing script after the {@link Jttp#process() process} method executes
     * and before the {@link Jttp#finish() finish} method executes.
     */
    void postProcess() {
        if (nonNull(postProcessScriptName) && !postProcessScriptName.isEmpty()) {
            LOGGER.log(DEBUG, "logger.debug.exec.postprocess");
            var jttpScriptObject = new JttpScriptObject(conn, tempResponse);
            executeScript(postProcessScriptName, jttpScriptObject, postProcessScriptArgs);
        }
    }

    /**
     * Handles output.
     */
    void finish() throws IOException, URISyntaxException {

        var headerRenderer = new HeaderRenderer(conn, colorOutput());

        if (printRequestHeaders()) {
            headerRenderer.requestLine(ps);
            headerRenderer.requestHeaders(ps);
        }

        if (printRequestBody()) {
            createContentRenderer(conn.getRequestProperty("Content-Type"),
                    requestData.toCharArray()).run();
            ps.println();
            ps.println();
        }

        if (!offline()) {

            if (!readOnlySession() && nonNull(session)) {
                try {
                    session.save(requestData, tempResponse);
                } catch (XMLStreamException | TransformerFactoryConfigurationError
                        | TransformerException e) {
                    LOGGER.log(WARNING, RB, "logger.warning.xmlerror", e.getMessage());
                    e.printStackTrace();
                }
            }

            if (printResponseHeaders()) {
                headerRenderer.responseStatusLine(ps);
                headerRenderer.responseHeaders(ps);
                ps.println();
            }

            if (nonNull(tempResponse)) {
                if (isDownload()) {
                    doDownload();
                } else if (printResponseBody()) {
                    createContentRenderer(conn.getContentType(), tempFileToChars()).run();
                    ps.println();
                }
            } else {
                LOGGER.log(INFO, "logger.info.no.response.body.sent");
            }
        }
    }

    /**
     * Prior to setup but after construction, make sure base objects needed for the run are
     * instantiated.
     */
    private void initializeState() {
        initializeUri();
        initializeRequestHeaders();
        initializeAuthenticator();
        initializeNoVerify();
        initializeRequestData();
    }

    /**
     * Initializes the request Uri with query parameters if set from the command line.
     */
    private void initializeUri() {
        var uri = new StringBuilder();

        if (urlString.startsWith(":") || urlString.startsWith("/")) {
            uri.append("http://localhost");
        }
        uri.append(urlString);
        if (nonNull(requestItems)) {
            var qparams = Arrays.stream(requestItems).filter(i -> i.contains("=="))
                    .map(i -> i.replace("==", "=")).collect(joining("&"));

            if (!qparams.isEmpty()) {
                uri.append("?").append(qparams);
            }
        }

        url = URI.create(uri.toString());
    }

    /**
     * Initializes the request headers if set from the command line.
     * 
     * <p>
     * If none set, request headers are an empty Map to avoid NPEs.
     */
    private void initializeRequestHeaders() {
        requestHeaders =
                nonNull(requestItems) ? Arrays.stream(requestItems).filter(i -> i.contains(":"))
                        .map(i -> new Pair<String, String>(i.split(":")[0], i.split(":")[1]))
                        .collect(groupingBy(Pair::getN, mapping(Pair::getV, toList()))) : Map.of();
    }

    /**
     * Initializes an Authenticator object if auth credentials were set on the command line.
     */
    private void initializeAuthenticator() {
        if (nonNull(auth) && !auth.isEmpty() && !auth.isBlank()) {
            Authenticator a;
            if (auth.indexOf(":") != -1) {
                var creds = auth.split(":");
                a = new JttpAuthenticator(creds[0], creds[1].toCharArray());
            } else {
                a = new JttpAuthenticator(auth);
            }
            Authenticator.setDefault(a);
        }
    }

    /**
     * If {@code -N} is set on the command line, set up TrustManager & HostNameVerifier to trust
     * everything.
     * 
     * <p>
     * Emits a log message at warning that this option was selected.
     */
    private void initializeNoVerify() {
        if (noVerify) {
            TrustManager[] trustAllCertificates = new TrustManager[] {new X509TrustManager() {

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }
            }};

            HostnameVerifier trustAllHostnames = new HostnameVerifier() {

                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            try {
                LOGGER.log(WARNING, "logger.warning.noverify", Jttp.class.getSimpleName());
                System.setProperty("jsse.enableSNIExtension", "false");
                var sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCertificates, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(trustAllHostnames);
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Initializes request data and resets the MIME type if needed.
     */
    private void initializeRequestData() {
        requestDataMap =
                nonNull(requestItems)
                        ? Arrays.stream(requestItems)
                                .filter(i -> i.contains("=") && !i.contains("=="))
                                .map(i -> new Pair<String, String>(i.split("=")[0],
                                        i.split("=")[1]))
                                .collect(toMap(Pair::getN, Pair::getV))
                        : Map.of();
        uploadFiles = nonNull(requestItems)
                ? Arrays.stream(requestItems).filter(i -> i.contains("@"))
                        .map(i -> new Pair<String, File>(i.split("@")[0],
                                new File(i.split("@")[1])))
                        .collect(toMap(Pair::getN, Pair::getV))
                : Map.of();

        if (!requestDataMap.isEmpty() && method == RequestMethod.GET) {
            method = RequestMethod.POST;
        }
        if (!uploadFiles.isEmpty()) {
            requestMimeType = RequestMimeType.MULTIPART;
        }
        if (uploadFiles.isEmpty() && requestDataMap.isEmpty() && method.hasPayload()) {
            inStream = System.in;
        }
    }

    /**
     * Set the request headers on the HttpURLConnection.
     */
    private void setRequestHeaders() throws IOException, URISyntaxException {
        for (String headername : requestHeaders.keySet()) {
            var values = requestHeaders.get(headername);
            conn.setRequestProperty(headername, values.get(0));
            for (var value : values.subList(1, values.size())) {
                conn.addRequestProperty(headername, value);
            }
        }
        if (!requestHeaders.containsKey("Accept-Charset")) {
            conn.setRequestProperty("Accept-Charset", defaultCharset().name());
        }
        if (!requestHeaders.containsKey("Accept")) {
            conn.setRequestProperty("Accept", "*/*");
        }
        if (!requestHeaders.containsKey("User-Agent")) {
            conn.setRequestProperty("User-Agent", userAgent(this));
        }
        if (conn instanceof HttpsURLConnection) {
            conn.setRequestProperty("Accept-Encoding", "gzip");
        }
    }

    /**
     * Set the request method on the HttpURLConnection.
     * 
     * @throws IOException if an @link IOException occurs.
     */
    private void setRequestMethod() throws IOException {
        if (method == RequestMethod.PATCH) {
            // per https://stackoverflow.com/a/32503192/37776
            // The server MUST support this to work. Need to look at
            // https://stackoverflow.com/a/40606633/37776 as a more versatile (and invasive)
            // solution. However, in Java 9+, the sun.www.net.protocol.https.HttpsURLConnection
            // class is not exported from the java.base module and therefore creates a compilation
            // error.
            conn.setDoOutput(true);
            conn.setRequestProperty("X-HTTP-Method-Override", method.name());
            conn.setRequestMethod(RequestMethod.POST.name());
        } else {
            conn.setRequestMethod(method.name());
            if (method.hasPayload()) {
                conn.setDoOutput(true);
            }
        }
    }

    private String renderPayload() {
        if (requestMimeType == RequestMimeType.FORM) {
            return renderWwwFormUrlEncodedString();
        }
        return renderSimpleJson();
    }

    private String renderWwwFormUrlEncodedString() {
        return requestDataMap.keySet().stream()
                .map(k -> format("%s=%s", k, encode(requestDataMap.get(k), defaultCharset())))
                .collect(joining("&"));
    }

    private String renderSimpleJson() {
        return format("{%s}",
                requestDataMap.keySet().stream()
                        .map(k -> format("\"%s\":\"%s\"", k, requestDataMap.get(k)))
                        .collect(joining(", ")));
    }

    private String readFromInStream() {
        var sb = new StringBuilder();
        try (var sc = new Scanner(inStream);) {
            while (sc.hasNextLine())
                sb.append(sc.nextLine());
        }
        return sb.toString();
    }

    private byte[] tempFileToBytes() throws IOException {
        try (var fis = new FileInputStream(tempResponse)) {
            return toByteArray(fis);
        }
    }

    private char[] tempFileToChars() throws IOException {
        var fileBytes = tempFileToBytes();
        var chars = new char[fileBytes.length];
        for (int i = 0; i < fileBytes.length; i++) {
            chars[i] = (char) (fileBytes[i] & 0xff);
        }
        return chars;
    }

    private ContentRenderer createContentRenderer(String contentType, char[] content)
            throws IOException {
        ContentRenderer renderer = null;
        if ((!formatOutput() && !colorOutput()) || isNull(contentType) || contentType.isEmpty()) {
            renderer = ContentRenderer.newRawInstance(content, ps);
        } else if (contentType.contains("json")) {
            renderer = new JsonRenderer(content, ps, colorOutput(), formatOutput());
        } else if (contentType.contains("xml")) {
            renderer = new MarkupRenderer(content, ps, colorOutput(), formatOutput());
        } else if (contentType.contains("html")) {
            // Don't format Html.
            renderer = new MarkupRenderer(content, ps, colorOutput(), false);
        } else if (contentType.equals(RequestMimeType.FORM.getContentType())) {
            renderer = new FormDataRenderer(content, ps, colorOutput());
        } else {
            renderer = ContentRenderer.newRawInstance(content, ps);
        }
        return renderer;
    }

    /**
     * Sends encoded request data (read from command line either from arguments or from redirected
     * input) through the OutputStream.
     * 
     * @param output the OutputStream.
     * @throws IOException if an IOException occurs.
     */
    private void sendRequestData(OutputStream output) throws IOException {
        output.write(requestData.getBytes());
    }

    /**
     * Sends request data and files specified from the command line as a multipart form.
     * 
     * @param output the OutputStream.
     * @throws IOException if an IOException.
     */
    private void sendMultipartData(OutputStream output) throws IOException {
        conn.setChunkedStreamingMode(CHUNK_SZ);
        try (var pw = new PrintWriter(output, true, defaultCharset())) {
            var somethingWritten = false;
            for (var entry : requestDataMap.entrySet()) {
                pw.append("--").append(BOUNDARY).append(CRLF)
                        .append("Content-Disposition: form-data; name=\"").append(entry.getKey())
                        .append("\"").append(CRLF).append("Content-Type: text/plain; charset=")
                        .append(defaultCharset().name()).append(CRLF).append(CRLF)
                        .append(entry.getValue()).append(CRLF).flush();
                somethingWritten = true;
            }

            for (var entry : uploadFiles.entrySet()) {
                var contentType =
                        URLConnection.guessContentTypeFromName(entry.getValue().getName());
                pw.append("--").append(BOUNDARY).append(CRLF)
                        .append("Content-Disposition: form-data; name=\"").append(entry.getKey())
                        .append("\"").append(CRLF).append("Content-Type: ");
                if (isNull(contentType) || !contentType.contains("text")) {
                    contentType = "application/octet";
                    pw.append(contentType);
                    pw.append(CRLF).append("Content-Transfer-Encoding: binary").append(CRLF);
                } else {
                    pw.append(contentType).append("; charset=").append(defaultCharset().toString())
                            .append(CRLF);
                }
                pw.append(CRLF).flush();
                Files.copy(entry.getValue().toPath(), output);
                output.flush();
                pw.append(CRLF).flush();
                somethingWritten = true;
            }
            if (somethingWritten) {
                pw.append("--").append(BOUNDARY).append("--").append(CRLF).flush();
            }
        }
    }

    private InputStream getInputStream() throws IOException {
        // Assuming we already tested for offline run.
        if (conn instanceof HttpsURLConnection) {
            if (nonNull(conn.getHeaderField("Content-Encoding"))
                    && conn.getHeaderField("Content-Encoding").equals("gzip")) {
                return new GZIPInputStream(conn.getInputStream());
            } else {
                return conn.getInputStream();
            }
        } else {
            return conn.getInputStream();
        }
    }

    private void doDownload() throws IOException {
        Path download = getDownloadPath();
        if (!Files.exists(download.getParent())) {
            Files.createDirectories(download.getParent());
        }
        Files.move(tempResponse.toPath(), download, ATOMIC_MOVE);
    }

    private Path getDownloadPath() {
        var downloadDir = new File(getDownloadsDirectory());
        Path download = null;
        if (nonNull(outfile)) {
            if (outfile.isAbsolute()) {
                download = outfile.toPath();
            } else {
                download = new File(downloadDir, outfile.toString()).toPath();
            }
        } else {
            var filename = conn.getURL().getFile();
            download = new File(downloadDir, filename).toPath();
        }
        return download;
    }

    private boolean offline() {
        return offline;
    }

    private boolean printRequestHeaders() {
        return verbose || print.contains("H");
    }

    private boolean printRequestBody() {
        return verbose || print.contains("B");
    }

    private boolean printResponseHeaders() {
        return verbose || print.contains("h");
    }

    private boolean printResponseBody() {
        return !download && (verbose || print.contains("b"));
    }

    private boolean readOnlySession() {
        return readOnlySession;
    }

    private boolean isDownload() {
        return download;
    }

    private boolean colorOutput() {
        return !download && (prettyPrint == PrettyPrint.ALL || prettyPrint == PrettyPrint.COLORS);
    }

    private boolean formatOutput() {
        return !download && (prettyPrint == PrettyPrint.ALL || prettyPrint == PrettyPrint.INDENT);
    }

    private void executeScript(String scriptname, JttpScriptObject jttpScriptObject,
            String[] scriptargs) {
        var scriptEngineName = scriptEngineNameFrom(scriptname);
        var manager = new ScriptEngineManager();
        var engine = manager.getEngineByName(scriptEngineName);
        if (isNull(engine)) {
            LOGGER.log(WARNING, "logger.warning.null.scriptengine", scriptEngineName);
            return;
        }
        engine.put("jttpScriptObject", jttpScriptObject);
        if (nonNull(scriptargs) && scriptargs.length > 0) {
            engine.put("args", scriptargs);
        } else {
            engine.put("args", new String[0]);
        }
        var scriptFilename = format("%s%s%s", getScriptsDirectory(), FILE_SEP, scriptname);
        try (var scriptFileReader = new FileReader(scriptFilename)) {
            engine.eval(scriptFileReader);
        } catch (FileNotFoundException e) {
            LOGGER.log(WARNING, () -> MessageFormat
                    .format(RB.getString("logger.warning.script.file.not.found"), scriptFilename),
                    e);
            return;
        } catch (IOException e) {
            LOGGER.log(WARNING, () -> MessageFormat
                    .format(RB.getString("logger.warning.script.file.close"), scriptFilename), e);
            return;
        } catch (ScriptException e) {
            LOGGER.log(
                    WARNING, () -> MessageFormat
                            .format(RB.getString("logger.warning.script.exec.error"), scriptname),
                    e);
            return;
        }
    }

    private String scriptEngineNameFrom(String scriptname) {
        var scriptEngineName = "nashorn";
        if (scriptname.indexOf('.') != -1) {
            var namecomponents = scriptname.split("\\.");
            scriptEngineName = namecomponents[namecomponents.length - 1];
        }
        return scriptEngineName;
    }

    private static String userAgent(Jttp instance) {
        return versionString(instance, false);
    }

    private static String versionString(Object instance, boolean includePlatformData) {
        var cl = instance.getClass();
        var ver = cl.getAnnotation(Version.class);
        if (isNull(ver)) {
            return "";
        }
        var verstr = new StringBuilder(ver.name()).append("/").append(ver.major());
        if (!ver.patch().equals("0")) {
            verstr.append('.').append(ver.minor()).append('.').append(ver.patch());
        } else if (!ver.minor().equals("0")) {
            verstr.append('.').append(ver.minor());
        }
        if (!ver.suffix().isEmpty() && !ver.suffix().isBlank()) {
            verstr.append('-').append(ver.suffix());
        }
        if (includePlatformData) {
            verstr.append(System.getProperty("line.separator")).append("Java version: ")
                    .append(System.getProperty("java.version")).append(", vendor: ")
                    .append(System.getProperty("java.vendor")).append(", runtime: ")
                    .append(System.getProperty("java.home"))
                    .append(System.getProperty("line.separator")).append("Default locale: ")
                    .append(Locale.getDefault()).append(", platform encoding: ")
                    .append(defaultCharset()).append(System.getProperty("line.separator"))
                    .append("OS name: \"").append(System.getProperty("os.name"))
                    .append("\", version: \"").append(System.getProperty("os.version"))
                    .append("\", arch: \"").append(System.getProperty("os.arch")).append("\"");
        }
        return verstr.toString();
    }

    /**
     * @param in an InputStream.
     * @return a byte array with the contents of the InputStream.
     * @throws IOException if an IOException occurs.
     */
    private static byte[] toByteArray(InputStream in) throws IOException {
        var bos = new ByteArrayOutputStream();
        var buf = new byte[BUF_SZ];
        while (true) {
            var read = in.read(buf);
            if (read == -1) {
                break;
            }
            bos.write(buf, 0, read);
        }
        return bos.toByteArray();
    }

    /**
     * @return the root save directory.
     */
    private static String getBaseSaveDirectory() {
        return Preferences.userRoot().node("Jttp").node("directories").get("base",
                LOCAL_SAVE_DIRECTORY);
    }

    /**
     * @return the name of the directory to save session data in.
     */
    private static String getSessionsDirectory() {
        return format("%s%s%s", getBaseSaveDirectory(), FILE_SEP, Preferences.userRoot()
                .node("Jttp").node("directories").get("sessions", "sessions"));
    }

    /**
     * @return the name of the base directory where scripts are located.
     */
    private static String getScriptsDirectory() {
        return format("%s%s%s", getBaseSaveDirectory(), FILE_SEP,
                Preferences.userRoot().node("Jttp").node("directories").get("scripts", "scripts"));
    }

    /**
     * @return absolute path to the preferred downloads directory.
     */
    private static String getDownloadsDirectory() {
        return Preferences.userRoot().node("Jttp").node("directories").get("downloads",
                format("%s%s%s", getBaseSaveDirectory(), FILE_SEP, "downloads"));
    }

    private static String formatMarkup(Source source)
            throws TransformerFactoryConfigurationError, TransformerException {

        var transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount",
                Integer.toString(INDENT));
        var sw = new StringWriter();
        transformer.transform(source, new StreamResult(sw));

        // Note: for some reason, the transformer inserts spaces and newlines in extant xml, so
        // get rid of all that extra by replacing the trailing spaces and newline with a simple
        // newline.
        return sw.toString().replaceAll("\n *\n", "\n").replace("?><", "?>\n<");
    }

    /**
     * Authenticator implementation used for gathering username and password.
     */
    static class JttpAuthenticator extends Authenticator {

        private final String username;

        private final char[] password;

        /**
         * Construct a new instance of JttpAuthenticator.
         * 
         * <p>
         * When invoked with the username, password is prompted. Note that {@link System#console()
         * System.console} must not return {@code null} for this to work.
         * 
         * @param username the username.
         */
        JttpAuthenticator(String username) {
            this(username, System.console().readPassword(RB.getString("jttp.password.prompt")));
        }

        /**
         * Construct a new instance of JttpAuthenticator with username and password pre-populated.
         * 
         * @param username the username.
         * @param password the password.
         */
        JttpAuthenticator(String username, char[] password) {
            this.username = username;
            this.password = password.clone();
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password);
        }

        @Override
        public String toString() {
            return "JttpAuthenticator [username=" + username + "]";
        }
    }

    /**
     * Allows access to certain internal Jttp objects from within pre- and post-process scripts.
     * 
     * <p>
     * There should be separate instances of JttpScriptObject associated with pre-processing and
     * post-processing. That is, each phase gets its own instance. However, the HttpURLConnection
     * will be the same object in both phases but in different states.
     * 
     * <p>
     * The pre-processing phase can modify the HttpURLConnection as normal by setting headers,
     * cookies, etc. The response file will be unavailable in pre-processing.
     * 
     * <p>
     * The post-processing phase can read the HttpURLConnection as normal such as headers and
     * cookies. The response file will be available and its data processed by the script.
     * Post-processing should not modify the contents of this file.
     */
    static class JttpScriptObject {

        private static System.Logger SCRIPT_LOGGER =
                System.getLogger(JttpScriptObject.class.getName());

        private final HttpURLConnection conn;

        private final File response;

        /**
         * Construct a new JttpScriptObject.
         * 
         * @param conn the HttpURLConnection associated with Jttp.
         */
        JttpScriptObject(HttpURLConnection conn) {
            this(conn, null);
        }

        /**
         * 
         * @param conn     the HttpURLConnection associated with Jttp.
         * @param response the response file (a temporary file).
         */
        public JttpScriptObject(HttpURLConnection conn, File response) {
            this.conn = conn;
            this.response = response;
        }

        /**
         * @return the HttpURLConnection associated with Jttp.
         */
        final HttpURLConnection getHttpURLConnection() {
            return conn;
        }

        /**
         * @return the response (temporary) file, possibly {@code null} if in pre-processing phase.
         */
        final File getResponseFile() {
            return response;
        }

        /**
         * Logs a message at INFO.
         * 
         * @param msg message to log.
         */
        final void log(String msg) {
            SCRIPT_LOGGER.log(INFO, msg);
        }
    }

    /**
     * Holder type used for handling request items specified on the command line.
     */
    private static class Pair<N, V> {
        private final N n;
        private final V v;

        Pair(N n, V v) {
            this.n = n;
            this.v = v;
        }

        N getN() {
            return n;
        }

        V getV() {
            return v;
        }
    }

    /**
     * HTTP request methods.
     */
    static enum RequestMethod {
        DELETE(false), GET(false), HEAD(false), OPTIONS(false), PATCH(true), POST(true), PUT(
                true), TRACE(false);

        private final boolean hasPayload;

        private RequestMethod(boolean hasPayload) {
            this.hasPayload = hasPayload;
        }

        /**
         * @return {@code true} if the method can have a payload to send to the server.
         */
        public boolean hasPayload() {
            return hasPayload;
        }
    }

    /**
     * Built-in Mime types for request payloads.
     */
    static enum RequestMimeType {
        /** {@code application/x-www-form-urlencoded} */
        FORM("application/x-www-form-urlencoded"),
        /** {@code application/json} */
        JSON("application/json"),
        /** {@code multipart/form-data} */
        MULTIPART("multipart/form-data");

        private final String contentType;

        private RequestMimeType(String contentType) {
            this.contentType = contentType;
        }

        /**
         * @return the content type string.
         */
        public String getContentType() {
            return contentType;
        }
    }

    /**
     * Describes pretty print options.
     */
    static enum PrettyPrint {
        /** No options applied. */
        NONE,
        /** Color output only. */
        COLORS,
        /** Indented output only. */
        INDENT,
        /** Color and indent. */
        ALL;
    }


    /**
     * ANSI Color enumeration.
     */
    static enum AnsiColor {

        BLACK(0), RED(1), GREEN(2), YELLOW(3), BLUE(4), MAGENTA(5), CYAN(6), WHITE(7), DEFAULT(9);

        private static final char FIRST_ESC_CHAR = '\u001B';
        private static final char SECOND_ESC_CHAR = '[';

        private final int code;

        private String fgCode;

        private AnsiColor(int code) {
            this.code = code;
        }

        /**
         * @return foreground code for this AnsiColor.
         */
        public String fgCode() {
            if (isNull(fgCode)) {
                fgCode = format("%c%c%dm", FIRST_ESC_CHAR, SECOND_ESC_CHAR, code + 30);
            }
            return fgCode;
        }
    }

    /**
     * ColorTheme for renderers.
     */
    static enum ColorTheme {

        DEFAULT(AnsiColor.WHITE, AnsiColor.CYAN, AnsiColor.BLUE, AnsiColor.MAGENTA,
                AnsiColor.YELLOW, AnsiColor.DEFAULT, AnsiColor.GREEN);

        private final AnsiColor punctuationColor;

        private final AnsiColor keyColor;

        private final AnsiColor keywordValueColor;

        private final AnsiColor numericValueColor;

        private final AnsiColor stringValueColor;

        private final AnsiColor defaultColor;

        private final AnsiColor functionColor;

        private ColorTheme(AnsiColor punctuationColor, AnsiColor keyColor,
                AnsiColor keywordValueColor, AnsiColor numericValueColor,
                AnsiColor stringValueColor, AnsiColor defaultColor, AnsiColor functionColor) {
            this.punctuationColor = punctuationColor;
            this.keyColor = keyColor;
            this.keywordValueColor = keywordValueColor;
            this.numericValueColor = numericValueColor;
            this.stringValueColor = stringValueColor;
            this.defaultColor = defaultColor;
            this.functionColor = functionColor;
        }

        public AnsiColor getPunctuationColor() {
            return punctuationColor;
        }

        public AnsiColor getKeyColor() {
            return keyColor;
        }

        public AnsiColor getKeywordValueColor() {
            return keywordValueColor;
        }

        public AnsiColor getNumericValueColor() {
            return numericValueColor;
        }

        public AnsiColor getStringValueColor() {
            return stringValueColor;
        }

        public AnsiColor getDefaultColor() {
            return defaultColor;
        }

        public AnsiColor getFunctionColor() {
            return functionColor;
        }
    }

    /**
     * Base class for all renderers.
     */
    static abstract class Renderer {

        static final char[] EMPTY_BUFFER = new char[0];

        static final Integer FGCODE_LEN = 5;

        private final ColorTheme colorTheme;

        private final boolean colorOutput;

        private int currentPosition;

        private int mark;

        private AnsiColor currColor = AnsiColor.DEFAULT;

        private AnsiColor prevColor = AnsiColor.DEFAULT;

        /**
         * Constructs a Renderer with the default ColorTheme.
         */
        Renderer() {
            this(ColorTheme.DEFAULT);
        }

        /**
         * Constructs a new Renderer.
         * 
         * @param colorOutput  if {@code true} output in color.
         * @param indentOutput if {@code true} indent output.
         */
        Renderer(boolean colorOutput) {
            this(ColorTheme.DEFAULT, colorOutput);
        }

        /**
         * Constructs a Renderer.
         * 
         * @param colorTheme ColorTheme to use if color output is supported.
         */
        Renderer(ColorTheme colorTheme) {
            this(colorTheme, true);
        }

        /**
         * Constructs a Renderer.
         * 
         * @param colorTheme  ColorTheme to use if color output is supported.
         * @param colorOutput if {@code true} output in color.
         */
        Renderer(ColorTheme colorTheme, boolean colorOutput) {
            this.colorTheme = colorTheme;
            this.colorOutput = colorOutput;
        }

        /**
         * Set the AnsiColor to use.
         * 
         * @param color AnsiColor.
         */
        void setColor(AnsiColor color) {
            prevColor = currColor;
            currColor = color;
        }

        /**
         * Resets AnsiColor to signal no changes have been made.
         */
        void resetColor() {
            prevColor = currColor;
        }

        /**
         * @return {@code true} if {@link #setColor(AnsiColor) setColor} was called.
         */
        boolean colorChanged() {
            return prevColor != currColor;
        }

        /**
         * @return the AnsiColor.
         */
        AnsiColor getColor() {
            return currColor;
        }

        /**
         * @return char array of AnsiColor's {@value Jttp#AnsiColor#fgCode() fgCode} value.
         */
        char[] getColorFgCode() {
            return currColor.fgCode().toCharArray();
        }

        /**
         * Mark the current position in a buffer.
         */
        void mark() {
            mark = currentPosition;
        }

        /**
         * Set the current position to the mark.
         */
        void reset() {
            currentPosition = mark;
            mark = 0;
        }

        /**
         * @return the current position.
         */
        final int getCurrentPosition() {
            return currentPosition;
        }

        final void incrementCurrentPosition() {
            currentPosition++;
        }

        final void incrementCurrentPositionBy(int inc) {
            currentPosition = currentPosition + inc;
        }

        final int getAndIncrementCurrentPosition() {
            return currentPosition++;
        }

        final int incrementAndGetCurrentPosition() {
            return ++currentPosition;
        }

        final void decrementCurrentPosition() {
            currentPosition--;
        }

        final int decrementAndGetCurrentPosition() {
            decrementCurrentPosition();
            return getCurrentPosition();
        }

        final void zeroCurrentPosition() {
            currentPosition = 0;
        }

        final ColorTheme getColorTheme() {
            return colorTheme;
        }

        final boolean isColorOutput() {
            return colorOutput;
        }
    }

    /**
     * Renders headers.
     */
    static class HeaderRenderer extends Renderer {

        private static final Set<String> ALWAYS_RENDERED_HEADERS = new LinkedHashSet<>();

        static {
            ALWAYS_RENDERED_HEADERS.add("Host");
            ALWAYS_RENDERED_HEADERS.add("Accept");
            ALWAYS_RENDERED_HEADERS.add("User-Agent");
        }

        private final HttpURLConnection conn;

        HeaderRenderer(HttpURLConnection conn, boolean colorOutput) {
            super(colorOutput);
            this.conn = conn;
        }

        /**
         * Prints the request line to the given PrintStream.
         * 
         * @param ps the PrintStream.
         * @throws URISyntaxException if a URISyntaxException occurs in processing.
         */
        void requestLine(PrintStream ps) throws URISyntaxException {
            var reqline = findRequestLine();

            if (isColorOutput()) {
                try (var scanner0 = new Scanner(reqline)) {
                    var method = scanner0.useDelimiter(" ").next();
                    var path = scanner0.next();
                    var protocol = scanner0.next();
                    try (var scanner1 = new Scanner(protocol)) {
                        var proto = scanner1.useDelimiter("/").next();
                        var vers = scanner1.next();
                        ps.printf("%s%s %s%s %s%s%s/%s%s%n",
                                getColorTheme().getFunctionColor().fgCode(), method,
                                getColorTheme().getKeyColor().fgCode(), path,
                                getColorTheme().getKeywordValueColor().fgCode(), proto,
                                getColorTheme().getDefaultColor().fgCode(),
                                getColorTheme().getKeywordValueColor().fgCode(), vers);
                    }
                }
            } else {
                ps.println(reqline);
            }
        }

        /**
         * Prints the request headers to the given PrintStream.
         * 
         * @param ps the PrintStream.
         * @throws URISyntaxException if a URISyntaxException occurs in processing.
         */
        void requestHeaders(PrintStream ps) throws URISyntaxException {
            if (!conn.getRequestProperties().containsKey("Host")) {
                var uri = conn.getURL().toURI();
                var host = uri.getPort() == -1 ? uri.getHost()
                        : format("%s:%d", uri.getHost(), uri.getPort());
                render(ps, "Host", Arrays.asList(host));
            }

            ALWAYS_RENDERED_HEADERS.stream().filter(h -> conn.getRequestProperties().containsKey(h))
                    .forEach(e -> render(ps, e, conn.getRequestProperties().get(e)));
            conn.getRequestProperties().keySet().stream()
                    .filter(k -> !ALWAYS_RENDERED_HEADERS.contains(k))
                    .filter(k -> nonNull(conn.getRequestProperties().get(k).get(0)))
                    .forEach(k -> render(ps, k, conn.getRequestProperties().get(k)));
            ps.println();
        }

        /**
         * Prints the response line to the given PrintStream.
         * 
         * @param ps the PrintStream.
         */
        void responseStatusLine(PrintStream ps) throws IOException {
            var statusCode = conn.getResponseCode();
            var message = conn.getResponseMessage();
            if (isColorOutput()) {
                var statusColor = AnsiColor.BLUE;
                if (statusCode >= 200 && statusCode < 300) {
                    statusColor = AnsiColor.GREEN;
                } else if (statusCode >= 300 && statusCode < 400) {
                    statusColor = AnsiColor.YELLOW;
                } else if (statusCode >= 400 && statusCode < 500) {
                    statusColor = AnsiColor.MAGENTA;
                } else if (statusCode >= 500) {
                    statusColor = AnsiColor.RED;
                }
                ps.printf("%sHTTP%s/%s1.1 %s%d %s%s%n",
                        getColorTheme().getKeywordValueColor().fgCode(),
                        getColorTheme().getDefaultColor().fgCode(),
                        getColorTheme().getKeywordValueColor().fgCode(), statusColor.fgCode(),
                        statusCode, getColorTheme().getKeyColor().fgCode(),
                        isNull(message) ? "" : message);
            } else {
                ps.println(conn.getHeaderField(0));
            }
        }

        /**
         * Prints the response headers to the given PrintStream.
         * 
         * @param ps the PrintStream.
         */
        void responseHeaders(PrintStream ps) {
            if (isColorOutput()) {
                conn.getHeaderFields().entrySet().stream().filter(e -> nonNull(e.getKey()))
                        .forEach(e -> ps.printf("%s%s%s: %s%n",
                                getColorTheme().getKeyColor().fgCode(),
                                capitalizeHeaderName(e.getKey()),
                                getColorTheme().getDefaultColor().fgCode(),
                                e.getValue().stream().collect(joining(","))));
            } else {
                conn.getHeaderFields().entrySet().stream().filter(e -> nonNull(e.getKey()))
                        .forEach(e -> ps.printf("%s: %s%n", capitalizeHeaderName(e.getKey()),
                                e.getValue().stream().collect(joining(","))));
            }
        }

        /**
         * Converts header names of the form {@code header-name} to {@code Header-Name}.
         * 
         * @param headername the headername.
         * @return capitalized headername.
         */
        private String capitalizeHeaderName(String headername) {
            return Arrays.stream(headername.split("-"))
                    .map(h -> h.substring(0, 1).toUpperCase() + h.substring(1))
                    .collect(joining("-"));
        }

        private String findRequestLine() throws URISyntaxException {
            // → Cheat: the request header with the null value is the request line.
            var reqline = conn.getRequestProperties().entrySet().stream()
                    .filter(e -> isNull(e.getValue().get(0))).map(e -> e.getKey()).findFirst()
                    .orElse("");
            // → Failing getting the request line, make our own from the data provided.
            if (reqline.isEmpty()) {
                var uri = conn.getURL().toURI();
                var path = (nonNull(uri.getQuery()) && !uri.getQuery().isEmpty())
                        ? format("%s?%s", uri.getPath(), uri.getQuery())
                        : uri.getPath();
                reqline = format("%s %s HTTP/1.1", conn.getRequestMethod(), path);
            }
            return reqline;
        }

        /**
         * Convenience for printing the header to the PrintStream.
         * 
         * <p>
         * Implementation note: The value for each header is a List of Strings. This method
         * collapses the logic.
         * 
         * @param ps    the PrintStream.
         * @param name  the header name.
         * @param value the header value(s).
         */
        private void render(PrintStream ps, String name, List<String> value) {
            if (isColorOutput()) {
                value.stream()
                        .forEach(v -> ps.printf("%s%s: %s%s%n",
                                getColorTheme().getKeyColor().fgCode(), name,
                                getColorTheme().getDefaultColor().fgCode(), v));
            } else {
                value.stream().forEach(v -> ps.printf("%s: %s%n", name, v));
            }
        }
    }

    /**
     * Base class for rendering content.
     */
    static abstract class ContentRenderer extends Renderer implements Runnable {

        private static final Integer DEFAULT_INDENT = 2;

        static final Integer INDENT =
                AccessController.doPrivileged((PrivilegedAction<Integer>) () -> Integer
                        .getInteger(SYS_PROP_INDENT, DEFAULT_INDENT));

        private final boolean indentOutput;

        private int indentLevel = 0;

        /**
         * Constructs a new ContentRenderer with indenting turned off.
         * 
         * @param colorOutput if {@code true} output in color.
         */
        ContentRenderer(boolean colorOutput) {
            this(colorOutput, false);
        }

        /**
         * Constructs a new ContentRenderer.
         * 
         * @param colorOutput  if {@code true} output in color.
         * @param indentOutput if {@code true} indent output.
         */
        ContentRenderer(boolean colorOutput, boolean indentOutput) {
            super(colorOutput);
            this.indentOutput = indentOutput;
        }

        /**
         * Returns a ContentRenderer that renders the characters directly to the PrintStream with no
         * coloring or indentation.
         * 
         * @param chars characters to render.
         * @param ps    the PrintStream.
         * @return a raw ContentRenderer.
         */
        static ContentRenderer newRawInstance(char[] chars, PrintStream ps) {
            return new RawContentRenderer(chars, ps);
        }

        final void zeroIndentLevel() {
            indentLevel = 0;
        }

        final boolean isIndentOutput() {
            return indentOutput;
        }

        final int getIndentLevel() {
            return indentLevel;
        }

        final void incrementIndentLevel() {
            indentLevel++;
        }

        final void decrementIndentLevel() {
            if (indentLevel - 1 >= 0) {
                indentLevel--;
            }
        }

        final int incrementAndGetIndentLevel() {
            incrementIndentLevel();
            return getIndentLevel();
        }

        final int decrementAndGetIndentLevel() {
            decrementIndentLevel();
            return getIndentLevel();
        }

        /**
         * ContentRenderer that prints the characters directly to a PrintStream with no formatting
         * or colors.
         */
        private static class RawContentRenderer extends ContentRenderer {

            private final char[] content;

            private final PrintStream ps;

            /**
             * Construct a new RawContentRenderer.
             * 
             * @param content characters to render.
             * @param ps      the PrintStream.
             */
            private RawContentRenderer(char[] content, PrintStream ps) {
                super(false, false);
                this.content = Arrays.copyOf(content, content.length);
                this.ps = ps;
            }

            @Override
            public void run() {
                ps.print(content);
            }
        }
    }

    /**
     * Renders form data.
     */
    static class FormDataRenderer extends ContentRenderer {

        private final char[] formdata;

        private final PrintStream ps;

        /**
         * Constructs a new FormDataRenderer.
         * 
         * @param formdata    characters to render.
         * @param ps          the PrintStream.
         * @param colorOutput when {@code true}, print output in color.
         */
        FormDataRenderer(char[] formdata, PrintStream ps, boolean colorOutput) {
            super(colorOutput);
            this.formdata = Arrays.copyOf(formdata, formdata.length);
            this.ps = ps;
        }

        @Override
        public void run() {

            var buffer = EMPTY_BUFFER;
            var ch = '\0';
            zeroCurrentPosition();

            while (getCurrentPosition() < formdata.length) {
                ch = formdata[getCurrentPosition()];
                if (isConverted(ch)) {
                    setColor(getColorTheme().getNumericValueColor());
                    if (buffer == EMPTY_BUFFER) {
                        buffer = isStartHex(ch) ? fillHex(3, ch) : ArrayUtils.asArray(ch);
                    }
                } else if (isSeparator(ch)) {
                    setColor(getColorTheme().getKeyColor());
                    buffer = ArrayUtils.asArray(ch);
                } else {
                    setColor(getColorTheme().getDefaultColor());
                    buffer = ArrayUtils.asArray(ch);
                }

                if (isColorOutput() && colorChanged()) {
                    ps.print(getColorFgCode());
                    resetColor();
                }

                ps.print(buffer);
                buffer = EMPTY_BUFFER;
                incrementCurrentPosition();
            }
        }

        private boolean isStartHex(char ch) {
            return ch == '%';
        }

        private boolean isConverted(char ch) {
            return isStartHex(ch) || ch == '+';
        }

        private boolean isSeparator(char ch) {
            return ch == '=' || ch == '&';
        }

        private char[] fillHex(int bufsz, char c) {
            var buffer = new char[bufsz];
            var bufferIdx = 0;
            buffer[bufferIdx++] = c;
            while (bufferIdx < bufsz) {
                var ch = formdata[incrementAndGetCurrentPosition()];
                buffer[bufferIdx++] = ch;
            }
            return buffer;
        }
    }

    /**
     * Renders Json.
     */
    static class JsonRenderer extends ContentRenderer {

        private static final BiPredicate<Character, Character> IN_STRING =
                (c0, c1) -> (c0 == '\\' && c1 == '"') || c1 != '"';

        private static final Predicate<Character> CHAR_IS_DIGIT = c -> Character.isDigit(c);

        private static final Predicate<Character> CHAR_IS_LETTER = c -> Character.isLetter(c);

        private static final Predicate<Character> IS_ESCAPE_CHAR =
                c -> c == '\b' || c == '\f' || c == '\n' || c == '\r';

        private final char[] json;

        private final PrintStream ps;

        /**
         * Constructs a new JsonRenderer.
         * 
         * @param json         characters to render.
         * @param ps           the PrintStream.
         * @param colorOutput  if {@code true} output in color.
         * @param indentOutput if {@code true} indent output.
         */
        JsonRenderer(char[] json, PrintStream ps, boolean colorOutput, boolean indentOutput) {
            super(colorOutput, indentOutput);
            this.json = Arrays.copyOf(json, json.length);
            this.ps = ps;
        }

        @Override
        public void run() {

            var buffer = EMPTY_BUFFER;
            var ch = '\0';
            zeroCurrentPosition();

            while (getCurrentPosition() < json.length) {
                ch = json[getCurrentPosition()];

                if (isPunctuation(ch)) {
                    setColor(getColorTheme().getPunctuationColor());
                    if (isOpenContainer(ch)) {
                        var next = json[getCurrentPosition() + 1];
                        if (isCloseContainer(next)) {
                            buffer = ArrayUtils.asArray(ch, next);
                            incrementCurrentPosition();
                        } else {
                            buffer = isIndentOutput()
                                    ? toIndentedCharArray(ch, incrementAndGetIndentLevel())
                                    : ArrayUtils.asArray(ch);
                        }
                    } else if (ch == ',') {
                        buffer = isIndentOutput() ? toIndentedCharArray(ch, getIndentLevel())
                                : ArrayUtils.asArray(ch);
                    } else if (ch == ':') {
                        if (isIndentOutput()) {
                            buffer = new char[2];
                            buffer[0] = ch;
                            buffer[1] = ' ';
                        } else {
                            buffer = ArrayUtils.asArray(ch);
                        }
                    } else if (isCloseContainer(ch)) {
                        buffer = isIndentOutput() ? toUnindentedCharArray(ch)
                                : ArrayUtils.asArray(ch);
                    }
                } else if (ch == '"') {
                    if (buffer == EMPTY_BUFFER) {
                        var bufSz = scanAheadForStringValueSize();
                        buffer = fillStringBuffer(bufSz, ch);
                        // Put the closing quote in the array.
                        buffer[buffer.length - 1] = ch;

                        mark();
                        // Look ahead to the next punctuation character
                        ch = getNextCharAfterWhitespace();
                        if (isPunctuation(ch)) {
                            if (ch == ':') {
                                setColor(getColorTheme().getKeyColor());
                            } else if (ch == ',' || isCloseContainer(ch)) {
                                setColor(getColorTheme().getStringValueColor());
                            }
                        }
                        // Reset the current position to resume processing.
                        reset();
                    }
                } else if (Character.isDigit(ch)) {
                    if (buffer == EMPTY_BUFFER) {
                        var bufSz = scanAheadForBufSz(CHAR_IS_DIGIT) - 1;
                        buffer = fillBuffer(CHAR_IS_DIGIT, bufSz, ch);

                        // Move the pointer back and mark our position since we will miss the ',' on
                        // the next iteration.
                        decrementCurrentPosition();
                        mark();

                        if (!isPunctuation(ch)) {
                            // Look ahead to the next punctuation character
                            ch = getNextCharAfterWhitespace();
                        }
                        if (isPunctuation(ch)) {
                            // Assume this is a bare number since the rules of json suggest so which
                            // means
                            // this punctuation character is a ','.
                            setColor(getColorTheme().getNumericValueColor());
                        }
                        // Reset the current position to resume processing.
                        reset();
                    }
                } else if (ch == 't' || ch == 'f' || ch == 'n') {
                    if (buffer == EMPTY_BUFFER) {
                        var bufSz = scanAheadForBufSz(CHAR_IS_LETTER) - 1;
                        buffer = fillBuffer(CHAR_IS_LETTER, bufSz, ch);

                        // Move the pointer back and mark our position since we will miss the ',' on
                        // the next iteration.
                        decrementCurrentPosition();
                        mark();

                        if (!isPunctuation(ch)) {
                            // Look ahead to the next punctuation character
                            ch = getNextCharAfterWhitespace();
                        }
                        if (isPunctuation(ch)) {
                            // Assume this is a bare number since the rules of json suggest so which
                            // means this punctuation character is a ','.
                            setColor(getColorTheme().getKeywordValueColor());
                        }
                        // Reset the current position to resume processing.
                        reset();
                    }
                }

                if (isColorOutput() && colorChanged()) {
                    ps.print(getColorFgCode());
                    resetColor();
                }

                if (buffer.length > 0) {
                    ps.print(buffer);
                    buffer = EMPTY_BUFFER;
                } else if (!isIndentOutput()) {
                    ps.print(ch);
                }
                incrementCurrentPosition();
            }
        }

        private boolean isPunctuation(char ch) {
            return ch == '{' || ch == '}' || ch == ':' || ch == ',' || ch == '[' || ch == ']';
        }

        private boolean isOpenContainer(char ch) {
            return ch == '{' || ch == '[';
        }

        private boolean isCloseContainer(char ch) {
            return ch == ']' || ch == '}';
        }

        private char[] toIndentedCharArray(char c, int indentLevel) {
            var buffer = new char[(indentLevel * INDENT) + 2];
            var bufferIndex = 0;
            buffer[bufferIndex++] = c;
            buffer[bufferIndex++] = '\n';
            Arrays.fill(buffer, bufferIndex, buffer.length, ' ');
            return buffer;
        }

        private char[] toUnindentedCharArray(char c) {
            decrementIndentLevel();
            var buffer = (getIndentLevel() > -1) ? new char[(getIndentLevel() * INDENT) + 2]
                    : new char[2];
            buffer[0] = '\n';
            if (getIndentLevel() > 0) {
                Arrays.fill(buffer, 1, buffer.length, ' ');
            }
            buffer[buffer.length - 1] = c;
            return buffer;
        }

        /**
         * @param scanCondition Condition that tests for end of scan.
         * @return the number of indexes in the json array scanned + 1 to account for quote strings.
         */
        private int scanAheadForBufSz(Predicate<Character> scanCondition) {
            // Already came across the first character.
            var bufSz = 1;
            mark();
            var ch = json[incrementAndGetCurrentPosition()];
            while (scanCondition.test(ch)) {
                bufSz++;
                ch = json[incrementAndGetCurrentPosition()];
            }
            // Account for the last character. This is the +1 and is needed for quoted text.
            bufSz++;
            reset();
            return bufSz;
        }

        private int scanAheadForStringValueSize() {
            var bufSz = 1;
            mark();
            var prev = json[getCurrentPosition()];
            var ch = json[incrementAndGetCurrentPosition()];
            var escapes = 0;
            do {
                if (IS_ESCAPE_CHAR.test(ch)) {
                    escapes++;
                }
                bufSz++;
                prev = ch;
                ch = json[incrementAndGetCurrentPosition()];
            } while (IN_STRING.test(prev, ch));
            bufSz++;
            reset();
            return bufSz + (escapes * 2);
        }

        private char[] fillBuffer(Predicate<Character> fillCondition, int bufSz, char c) {
            var buffer = new char[bufSz];
            var bufferIndex = 0;
            buffer[bufferIndex++] = c;
            var ch = json[incrementAndGetCurrentPosition()];
            while (fillCondition.test(ch)) {
                buffer[bufferIndex++] = ch;
                ch = json[incrementAndGetCurrentPosition()];
            }
            return buffer;
        }

        private char[] fillStringBuffer(int bufSz, char c) {
            var buffer = new char[bufSz];
            var bufferIndex = 0;
            buffer[bufferIndex++] = c;
            var prev = c;
            var ch = json[incrementAndGetCurrentPosition()];
            do {
                if (IS_ESCAPE_CHAR.test(ch)) {
                    var escaped = toEscapeCharArray(ch);
                    bufferIndex = ArrayUtils.appendTo(buffer, escaped, bufferIndex);
                } else {
                    buffer[bufferIndex++] = ch;
                }
                prev = ch;
                ch = json[incrementAndGetCurrentPosition()];
            } while (IN_STRING.test(prev, ch));
            return buffer;
        }

        /**
         * Escape '\' and non-printable characters.
         * 
         * @param c char to escape.
         * @return the escaped printable characters as an array.
         */
        private char[] toEscapeCharArray(char c) {
            if (c == '\b') {
                return ArrayUtils.asArray('\\', 'b');
            } else if (c == '\f') {
                return ArrayUtils.asArray('\\', 'f');
            } else if (c == '\n') {
                return ArrayUtils.asArray('\\', 'n');
            } else if (c == '\r') {
                return ArrayUtils.asArray('\\', 'r');
            } else if (c == '\t') {
                return ArrayUtils.asArray('\\', 't');
            } else {
                return ArrayUtils.asArray('\\', '\\');
            }
        }

        private char getNextCharAfterWhitespace() {
            char c = json[incrementAndGetCurrentPosition()];
            // Eat the whitespace in the meantime (like if json was "key" : "value" or
            // "value" ,\n "key")
            while (Character.isWhitespace(c)) {
                c = json[incrementAndGetCurrentPosition()];
            }
            return c;
        }
    }

    /**
     * Renders HTML and XML.
     */
    static class MarkupRenderer extends ContentRenderer {

        private static final char[] END_COMMENT_ARY = ArrayUtils.asArray('-', '-', '>');

        private static final Predicate<Character> CHAR_NOT_CLOSE_TAG = c -> c != '>';

        private static final Predicate<Character> CHAR_CLOSE_TAG =
                Predicate.not(CHAR_NOT_CLOSE_TAG);

        private static final Predicate<Character> CHAR_NOT_OPEN_TAG = c -> c != '<';

        private static final Predicate<Character> CHAR_CHANGE_COLOR_IN_TAG =
                c -> c == '=' || c == '"' || c == ' ' || c == '?' || c == '/' || c == '\'';

        private final char[] markup;

        private final PrintStream ps;

        private boolean inComment = false;

        private boolean inDeclarativeStatement = false;

        /**
         * Constructs a new MarkupRenderer.
         * 
         * @param markup       the characters to render,
         * @param ps           the PrintStream.
         * @param colorOutput  if {@code true} output in color.
         * @param indentOutput if {@code true} indent output.
         */
        MarkupRenderer(char[] markup, PrintStream ps, boolean colorOutput, boolean indentOutput) {
            super(colorOutput, indentOutput);
            this.ps = ps;
            this.markup = isIndentOutput() ? indent(markup) : Arrays.copyOf(markup, markup.length);
        }

        /**
         * @param markup char array to indent.
         * @return char array of indented markup.
         */
        private char[] indent(char[] markup) {
            try {
                return formatMarkup(new StreamSource(new CharArrayReader(markup))).toCharArray();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            var buffer = EMPTY_BUFFER;
            var ch = '\0';

            while (getCurrentPosition() < markup.length) {
                ch = markup[getCurrentPosition()];

                if (ch == '<') {
                    var next = markup[incrementAndGetCurrentPosition()];
                    if (next == '?' || next == '/' || Character.isLetterOrDigit(next)) {
                        setColor(getColorTheme().getKeywordValueColor());
                    } else if (next == '!') {
                        setColor(getColorTheme().getNumericValueColor());
                        if (markup[getCurrentPosition() + 1] == '-'
                                && markup[getCurrentPosition() + 2] == '-') {
                            inComment = true;
                        } else {
                            inDeclarativeStatement = true;
                        }
                    }
                    int bufSz = inComment ? scanForCommentBufSz() : scanForTagBufSz();
                    buffer = inComment
                            ? fillCommentBuffer(bufSz, ch, next,
                                    markup[incrementAndGetCurrentPosition()],
                                    markup[incrementAndGetCurrentPosition()])
                            : fillTagBuffer(bufSz, ch, next);
                } else if (Character.isWhitespace(ch)) {
                    int bufSz = scanForWhitespaceBufSz();
                    buffer = fillWhitespaceBuffer(bufSz, ch);
                } else {
                    setColor(getColorTheme().getDefaultColor());
                    int bufSz = scanForTextBufSz();
                    buffer = fillTextBuffer(bufSz, ch);
                }

                if (isColorOutput() && colorChanged()) {
                    ps.print(getColorFgCode());
                    resetColor();
                }

                if (buffer.length > 0) {
                    ps.print(buffer);
                    buffer = EMPTY_BUFFER;
                } else if (!isIndentOutput()) {
                    ps.print(ch);
                }

                inDeclarativeStatement = false;
                incrementCurrentPosition();
            }
        }

        private int scanForTagBufSz() {
            var bufSz = 2;
            mark();
            var ch = markup[incrementAndGetCurrentPosition()];
            var colorChanges = 0;
            var instring = false;
            var lastIndexOfQuote = -1;
            while (CHAR_NOT_CLOSE_TAG.test(ch)) {
                bufSz++;
                if (CHAR_CHANGE_COLOR_IN_TAG.test(ch) && !inDeclarativeStatement && !instring) {
                    colorChanges++;
                }
                if (ch == '"' || ch == '\'') {
                    instring = !instring;
                    if (!instring) {
                        colorChanges++;
                    }
                    lastIndexOfQuote = getCurrentPosition();
                }
                ch = markup[incrementAndGetCurrentPosition()];
            }
            // Account for the closing character and possible color changes if for example there's
            // an unquoted attribute value.
            bufSz++;
            if (lastIndexOfQuote == -1) {
                colorChanges++;
            }
            // Account for anything that could cause the color to change inside a tag if needed.
            if (isColorOutput()) {
                bufSz = bufSz + (FGCODE_LEN * colorChanges);
            }
            reset();
            return bufSz;
        }

        private int scanForCommentBufSz() {
            var bufSz = 4; // start with <!--
            mark();
            incrementCurrentPositionBy(2);
            var ch = markup[incrementAndGetCurrentPosition()];
            var last3chars = ArrayUtils.asArray(markup[getCurrentPosition() - 2],
                    markup[getCurrentPosition() - 1], ch);
            while (!Arrays.equals(END_COMMENT_ARY, last3chars)) {
                bufSz++;
                ch = markup[incrementAndGetCurrentPosition()];
                last3chars = ArrayUtils.asArray(markup[getCurrentPosition() - 2],
                        markup[getCurrentPosition() - 1], ch);
            }
            bufSz++; // Get the close tag character.
            reset();
            return bufSz;
        }

        private int scanForWhitespaceBufSz() {
            var bufSz = 1;
            if (getCurrentPosition() < markup.length - 1) {
                mark();
                var ch = markup[incrementAndGetCurrentPosition()];
                while (Character.isWhitespace(ch)) {
                    bufSz++;
                    if (getCurrentPosition() == markup.length - 1) {
                        break;
                    }
                    ch = markup[incrementAndGetCurrentPosition()];
                }
                reset();
            }
            return bufSz;
        }

        private int scanForTextBufSz() {
            var bufSz = 1;
            if (getCurrentPosition() < markup.length - 1) {
                mark();
                var ch = markup[incrementAndGetCurrentPosition()];
                while (CHAR_NOT_OPEN_TAG.test(ch) && getCurrentPosition() < markup.length) {
                    bufSz++;
                    ch = markup[incrementAndGetCurrentPosition()];
                }
                reset();
            }
            return bufSz;
        }

        private char[] fillTagBuffer(int bufSz, char first, char second) {
            var buffer = new char[bufSz];
            var bufIdx = 0;
            buffer[bufIdx++] = first;
            buffer[bufIdx++] = second;
            var ch = markup[incrementAndGetCurrentPosition()];
            var currColorChars = getColorFgCode();
            while (CHAR_NOT_CLOSE_TAG.test(ch) && bufIdx < bufSz
                    && getCurrentPosition() < markup.length) {
                if (!inDeclarativeStatement && CHAR_CHANGE_COLOR_IN_TAG.test(ch)
                        && CHAR_NOT_CLOSE_TAG.test(markup[getCurrentPosition() + 1])) {
                    if (ch == ' ' && markup[getCurrentPosition() - 1] != ' ') {
                        buffer[bufIdx++] = ch;
                        if (isColorOutput()) {
                            currColorChars = getColorTheme().getKeyColor().fgCode().toCharArray();
                            bufIdx = ArrayUtils.appendTo(buffer, currColorChars, bufIdx);
                        }
                        ch = markup[incrementAndGetCurrentPosition()];
                        while (!CHAR_CHANGE_COLOR_IN_TAG.test(ch) && CHAR_NOT_CLOSE_TAG.test(ch)
                                && bufIdx < bufSz && getCurrentPosition() < markup.length - 1) {
                            buffer[bufIdx++] = ch;
                            ch = markup[incrementAndGetCurrentPosition()];
                        }
                    } else if (ch == '=') {
                        if (isColorOutput()) {
                            currColorChars =
                                    getColorTheme().getPunctuationColor().fgCode().toCharArray();
                            bufIdx = ArrayUtils.appendTo(buffer, currColorChars, bufIdx);
                        }
                        buffer[bufIdx++] = ch;
                        ch = markup[incrementAndGetCurrentPosition()];
                    } else if (ch == '"' || ch == '\'') {
                        var strDelimiter = ch;
                        if (isColorOutput()) {
                            currColorChars =
                                    getColorTheme().getStringValueColor().fgCode().toCharArray();
                            bufIdx = ArrayUtils.appendTo(buffer, currColorChars, bufIdx);
                        }

                        do {
                            buffer[bufIdx++] = ch;
                            ch = markup[incrementAndGetCurrentPosition()];
                        } while (ch != strDelimiter && bufIdx < bufSz);

                        if (ch == strDelimiter) {
                            buffer[bufIdx++] = ch;
                        }
                        ch = markup[incrementAndGetCurrentPosition()];
                    } else if (ch == '?' || ch == '/') {
                        if (isColorOutput()) {
                            // currColorChars = keywordColorChars;
                            currColorChars =
                                    getColorTheme().getKeywordValueColor().fgCode().toCharArray();
                            bufIdx = ArrayUtils.appendTo(buffer, currColorChars, bufIdx);
                        }
                        buffer[bufIdx++] = ch;
                        ch = markup[incrementAndGetCurrentPosition()];
                    } else {
                        buffer[bufIdx++] = ch;
                        ch = markup[incrementAndGetCurrentPosition()];
                    }
                } else {
                    buffer[bufIdx++] = ch;
                    ch = markup[incrementAndGetCurrentPosition()];
                }
            }
            if (CHAR_CLOSE_TAG.test(ch) && !inDeclarativeStatement && !Arrays.equals(currColorChars,
                    getColorTheme().getKeywordValueColor().fgCode().toCharArray())) {
                bufIdx = ArrayUtils.appendTo(buffer,
                        getColorTheme().getKeywordValueColor().fgCode().toCharArray(), bufIdx);
            }
            buffer[bufIdx] = ch;
            return ArrayUtils.trimNulls(buffer);
        }

        private char[] fillCommentBuffer(int bufSz, char first, char second, char third,
                char fourth) {
            var buffer = new char[bufSz];
            var bufIdx = 0;
            buffer[bufIdx++] = first;
            buffer[bufIdx++] = second;
            buffer[bufIdx++] = third;
            buffer[bufIdx++] = fourth;
            var ch = markup[incrementAndGetCurrentPosition()];
            var last3chars = ArrayUtils.asArray(markup[getCurrentPosition() - 2],
                    markup[getCurrentPosition() - 1], ch);
            while (!Arrays.equals(END_COMMENT_ARY, last3chars)) {
                buffer[bufIdx++] = ch;
                ch = markup[incrementAndGetCurrentPosition()];
                last3chars = ArrayUtils.asArray(markup[getCurrentPosition() - 2],
                        markup[getCurrentPosition() - 1], ch);
            }
            buffer[bufIdx] = ch;
            return buffer;
        }

        private char[] fillTextBuffer(int bufSz, char first) {
            var buffer = new char[bufSz];
            var bufIdx = 0;
            buffer[bufIdx++] = first;
            if (getCurrentPosition() < markup.length - 1) {
                var ch = markup[incrementAndGetCurrentPosition()];
                while (CHAR_NOT_OPEN_TAG.test(ch)) {
                    buffer[bufIdx++] = ch;
                    ch = markup[incrementAndGetCurrentPosition()];
                }
                // Back up 1 character becaue the open tag is at the current position.
                decrementCurrentPosition();
            }
            return ArrayUtils.trimNulls(buffer);
        }

        private char[] fillWhitespaceBuffer(int bufSz, char first) {
            var buffer = new char[bufSz];
            var bufIdx = 0;
            buffer[bufIdx++] = first;
            if (getCurrentPosition() < markup.length - 1) {
                var ch = markup[incrementAndGetCurrentPosition()];
                while (Character.isWhitespace(ch)) {
                    buffer[bufIdx++] = ch;
                    if (getCurrentPosition() == markup.length - 1) {
                        break;
                    }
                    ch = markup[incrementAndGetCurrentPosition()];
                }
                decrementCurrentPosition();
            }
            return ArrayUtils.trimNulls(buffer);
        }
    }

    /**
     * Array utilities not in {@link Arrays}.
     */
    static class ArrayUtils {

        private ArrayUtils() {
        }

        /**
         * @param appendee    the array to be appended to
         * @param append      the array to be appended to the appendee
         * @param appendeeIdx offset index in appendee to start appending
         * @return value of appendeeIdx after appending.
         */
        static int appendTo(char[] appendee, char[] append, int appendeeIdx) {
            for (int i = 0; i < append.length; i++) {
                appendee[appendeeIdx++] = append[i];
            }
            return appendeeIdx;
        }

        /**
         * @param c array to have null characters removed from.
         * @return a new copy of c with no null characters.
         */
        static char[] trimNulls(char[] c) {
            var nullchars = 0;
            for (int i = c.length - 1; i >= 0; i--) {
                if (c[i] == '\0') {
                    nullchars++;
                }
            }
            return Arrays.copyOf(c, c.length - nullchars);
        }

        /**
         * @param ignoreWhitespace if {@code true}, ignores leading whitespace in c.
         * @param c                the target array
         * @param cs               characters to test (in specified order) that the array starts
         *                         with.
         * @return {@code true} if c has the characters from its first index up to the number of
         *         test characters in argument order.
         * @throws IllegalArgumentException if the number of test characters exceeds the length of
         *                                  the target array.
         */
        static boolean startsWith(boolean ignoreWhitespace, char[] c, char... cs) {
            if (cs.length > c.length) {
                throw new IllegalArgumentException(RB.getString("error.util.too.many.chars"));
            }
            var startsWith = true;
            for (int i = 0, j = 0; i < cs.length; i++, j++) {
                if (ignoreWhitespace) {
                    while (Character.isWhitespace(c[j])) {
                        j++;
                        if (j == c.length) {
                            return false;
                        }
                    }
                }
                startsWith = startsWith && (cs[i] == c[j]);
            }
            return startsWith;
        }

        /**
         * @param c  the target array
         * @param cs characters to test (in reverse of specified order) that the array ends with.
         * @return {@code true} if c has the characters from its last index down to the number of
         *         tet characters in reverse argument order.
         * @throws IllegalArgumentException if the number of test characters exceeds the length of
         *                                  the target array.
         */
        static boolean endsWith(char[] c, char... cs) {
            if (cs.length > c.length) {
                throw new IllegalArgumentException(RB.getString("error.util.too.many.chars"));
            }
            var endsWith = true;
            for (int i = cs.length - 1, j = c.length - 1; i >= 0; i--, j--) {
                endsWith = endsWith && (cs[i] == c[j]);
            }
            return endsWith;
        }

        /**
         * @param c array to search.
         * @param t target character to find.
         * @return index in the array after the first appearance of the target character.
         */
        static int indexAfter(char[] c, char t) {
            var i = 0;
            while (c[i] != t && i != c.length) {
                i++;
            }
            return i == c.length ? -1 : i + 1;
        }

        /**
         * @param c characters to put into an array.
         * @return a char array whose length is the number of chars passed to it and whose contents
         *         are the chars passed to it in order.
         */
        static char[] asArray(char... c) {
            var buffer = new char[c.length];
            for (int i = 0; i < buffer.length; i++) {
                buffer[i] = c[i];
            }
            return buffer;
        }
    }

    /**
     * Session object that loads and saves session data.
     * 
     * <p>
     * Session data is saved by default to
     * {@code ${user.home}/.jttp/sessions/HOST_PORT/SESSIONNAME.zip}. The zip file consists of 2
     * files:
     * <ul>
     * <li>{@code headers.xml} for request headers.
     * <li>{@code cookies.xml} for cookies.
     * </ul>
     */
    static class Session {

        private static final Boolean DELETE_TEMPFILES = !AccessController.doPrivileged(
                (PrivilegedAction<Boolean>) () -> Boolean.getBoolean(SYS_PROP_KEEP_TEMP_FILES));

        private static final Collection<String> RESTRICTED_HEADERS =
                Set.of("Host", "Connection", "Set-Cookie");

        private static final Integer DEFAULT_INDENT = 2;

        private static final Integer INDENT =
                AccessController.doPrivileged((PrivilegedAction<Integer>) () -> Integer
                        .getInteger(SYS_PROP_INDENT, DEFAULT_INDENT));

        private static final Map<String, String> LOAD_ENV = Map.of();

        private static final Map<String, String> SAVE_ENV = Map.of("create", "true");

        private final URI sessionFsUri;

        private final HttpURLConnection conn;

        // Maybe make this an option later, for now hardcode as on.
        private final boolean formatSessionXml = true;

        private Document history;

        Session(String sessionName, HttpURLConnection conn) throws URISyntaxException {
            this.sessionFsUri = initSessionFsUri(sessionName, conn.getURL().toURI());
            this.conn = conn;
        }

        /**
         * Load and populate session data into the different parts of the request.
         * 
         * <p>
         * If the session file doesn't yet exist, then this method does nothing. If an IOException
         * occurs and the session file exists, then it is thrown.
         * 
         * @throws IOException                  if an IOException occurs.
         * @throws XMLStreamException           if an XMLStreamException occurs.
         * @throws SAXException                 if a SAXException occurs.
         * @throws ParserConfigurationException if a ParserConfigurationException occurs.
         */
        void load()
                throws XMLStreamException, ParserConfigurationException, SAXException, IOException {
            // Load headers into HttpURLConnection request properties
            // Set cookies in default CookieHandler's CookieStore.
            try (var sessionFs = FileSystems.newFileSystem(sessionFsUri, LOAD_ENV)) {
                history = getHistoryXml(sessionFs);
                doLoadCookies(sessionFs);
                doLoadHeaders(sessionFs);
            } catch (FileSystemNotFoundException e) {
                // No filesystem available since this is the first time using this so just create
                // the starter history Xml document.
                history = generateNewHistoryDocument();
            }
        }

        /**
         * Save session data.
         * 
         * @param requestData data sent as part of the request, possibly {@code null}.
         * @param response    response from the server.
         * 
         * @throws IOException                          if an IOException occurs.
         * @throws XMLStreamException                   if an XMLStreamException occurs.
         * @throws TransformerException                 if a TransformerException occurs.
         * @throws TransformerFactoryConfigurationError if a TransformerFactoryConfigurationError
         *                                              occurs.
         */
        void save(String requestData, File response) throws IOException, XMLStreamException,
                TransformerFactoryConfigurationError, TransformerException {
            var timeOfRun = Instant.now();
            // Create the filesystem
            try (var sessionFs = FileSystems.newFileSystem(sessionFsUri, SAVE_ENV)) {
                doSaveCookies(sessionFs);
                doSaveHeaders(sessionFs);
                var entryId = doUpdateAndSaveHistory(sessionFs, requestData, timeOfRun);
                doSaveResponseData(sessionFs, response, timeOfRun, entryId);
            }
        }

        /**
         * Loads a file named {@code cookies.xml} in the session zip file.
         * 
         * <p>
         * Cookies are added to the HttpURLConnection in this Session.
         * 
         * @param sessionFs the pseudo FileSystem of the zip file.
         * @throws IOException        if an IOException occurs.
         * @throws XMLStreamException if an XMLStreamException.
         */
        private void doLoadCookies(FileSystem sessionFs) throws IOException, XMLStreamException {
            var cookiesXml = sessionFs.getPath("/cookies.xml");
            try (var infile = cookiesXml.toUri().toURL().openStream()) {
                var xmlIf = XMLInputFactory.newInstance();
                var xmlEvR = xmlIf.createXMLEventReader(infile);
                var qCNm = new QName("name");
                var currEltNm = "";
                var inCookie = false;
                HttpCookie c = null;
                while (xmlEvR.hasNext()) {
                    var xmlEv = xmlEvR.nextEvent();
                    if (xmlEv.isStartElement()) {
                        var se = xmlEv.asStartElement();
                        currEltNm = se.getName().getLocalPart();
                        if (currEltNm.equals("cookie")) {
                            c = new HttpCookie(se.getAttributeByName(qCNm).getValue(), "");
                            inCookie = true;
                        } else if (inCookie && currEltNm.equals("value")) {
                            xmlEv = xmlEvR.nextEvent();
                            if (xmlEv.isCharacters() && nonNull(c)) {
                                c.setValue(xmlEv.asCharacters().getData().trim());
                            }
                        } else if (inCookie && currEltNm.equals("path")) {
                            xmlEv = xmlEvR.nextEvent();
                            if (xmlEv.isCharacters() && nonNull(c)) {
                                c.setPath(xmlEv.asCharacters().getData().trim());
                            }
                        } else if (inCookie && currEltNm.equals("secure")) {
                            xmlEv = xmlEvR.nextEvent();
                            if (xmlEv.isCharacters() && nonNull(c)) {
                                c.setSecure(Boolean.valueOf(xmlEv.asCharacters().getData().trim()));
                            }
                        } else if (inCookie && currEltNm.equals("comment")) {
                            xmlEv = xmlEvR.nextEvent();
                            if (xmlEv.isCharacters() && nonNull(c)) {
                                c.setComment(xmlEv.asCharacters().getData().trim());
                            }
                        } else if (inCookie && currEltNm.equals("commentUrl")) {
                            xmlEv = xmlEvR.nextEvent();
                            if (xmlEv.isCharacters() && nonNull(c)) {
                                c.setCommentURL(xmlEv.asCharacters().getData().trim());
                            }
                        } else if (inCookie && currEltNm.equals("discard")) {
                            xmlEv = xmlEvR.nextEvent();
                            if (xmlEv.isCharacters() && nonNull(c)) {
                                c.setDiscard(
                                        Boolean.valueOf(xmlEv.asCharacters().getData().trim()));
                            }
                        } else if (inCookie && currEltNm.equals("max_age")) {
                            xmlEv = xmlEvR.nextEvent();
                            if (xmlEv.isCharacters() && nonNull(c)) {
                                c.setMaxAge(Long.valueOf(xmlEv.asCharacters().getData().trim()));
                            }
                        } else if (inCookie && currEltNm.equals("version")) {
                            xmlEv = xmlEvR.nextEvent();
                            if (xmlEv.isCharacters() && nonNull(c)) {
                                c.setVersion(
                                        Integer.valueOf(xmlEv.asCharacters().getData().trim()));
                            }
                        } else if (inCookie && currEltNm.equals("http_only")) {
                            xmlEv = xmlEvR.nextEvent();
                            if (xmlEv.isCharacters() && nonNull(c)) {
                                c.setHttpOnly(
                                        Boolean.valueOf(xmlEv.asCharacters().getData().trim()));
                            }
                        } else if (inCookie && currEltNm.equals("port_list")) {
                            xmlEv = xmlEvR.nextEvent();
                            if (xmlEv.isCharacters() && nonNull(c)) {
                                c.setPortlist(xmlEv.asCharacters().getData().trim());
                            }
                        }
                    } else if (xmlEv.isEndElement()) {
                        var ee = xmlEv.asEndElement();
                        if (ee.getName().getLocalPart().equals("cookie")) {
                            inCookie = false;
                            if (!c.hasExpired()) {
                                conn.addRequestProperty("Cookie", c.toString());
                            }
                            c = null;
                        }
                    }
                }
            }
        }

        /**
         * Loads a file named {@code headers.xml} in the session zip file.
         * 
         * <p>
         * Headers in the XML file are added to the HttpURLConnection in this Session.
         * 
         * @param sessionFs the pseudo FileSystem of the zip file.
         * @throws IOException        if an IOException occurs.
         * @throws XMLStreamException if an XMLStreamException.
         */
        private void doLoadHeaders(FileSystem sessionFs) throws IOException, XMLStreamException {
            var headersXml = sessionFs.getPath("/headers.xml");
            try (var infile = headersXml.toUri().toURL().openStream()) {
                var xmlIf = XMLInputFactory.newInstance();
                var xmlEvR = xmlIf.createXMLEventReader(infile);
                var qNm = new QName("name");
                var currEltNm = "";
                var hdrNm = "";
                var hdrVls = new ArrayList<String>();
                while (xmlEvR.hasNext()) {
                    var xmlEv = xmlEvR.nextEvent();
                    if (xmlEv.isStartElement()) {
                        var se = xmlEv.asStartElement();
                        currEltNm = se.getName().getLocalPart();
                        if (currEltNm.equals("header")) {
                            hdrNm = se.getAttributeByName(qNm).getValue();
                        } else if (currEltNm.equals("value")) {
                            xmlEv = xmlEvR.nextEvent();
                            if (xmlEv.isCharacters()) {
                                hdrVls.add(xmlEv.asCharacters().getData().trim());
                            }
                        }
                    } else if (xmlEv.isEndElement()) {
                        var ee = xmlEv.asEndElement();
                        if (ee.getName().getLocalPart().equals("header")) {
                            conn.setRequestProperty(hdrNm, hdrVls.get(0));
                            for (String hdrVl : hdrVls.subList(1, hdrVls.size())) {
                                conn.addRequestProperty(hdrNm, hdrVl);
                            }
                            hdrVls.clear();
                        }
                    }
                }
            }
        }

        /**
         * Instantiates the history document for this Session.
         * 
         * <p>
         * First, try to read an existing history document on the Session FileSystem. Failing that,
         * generate a new one.
         * 
         * @param sessionFs the Session FileSystem.
         * 
         * @return Document containing history data.
         * 
         * @throws IOException                  if an IOException occurs.
         * @throws MalformedURLException        if a MalformedURLException occurs.
         * @throws ParserConfigurationException if a ParserConfigurationException occurs.
         * @throws SAXException                 if a SAXException occurs.
         */
        private Document getHistoryXml(FileSystem sessionFs) throws ParserConfigurationException,
                MalformedURLException, SAXException, IOException {
            var historyXml = sessionFs.getPath("/history.xml");
            Document histDoc = null;
            var dbf = DocumentBuilderFactory.newInstance();
            var histDb = dbf.newDocumentBuilder();
            if (Files.exists(historyXml)) {
                try (var xmlIn = historyXml.toUri().toURL().openStream()) {
                    histDoc = histDb.parse(xmlIn);
                } catch (Exception e) {
                    // Something's wrong so log it and generate a new history document.
                    LOGGER.log(WARNING, "logger.warning.xml.history.error", e);
                    generateNewHistoryDocument(histDb);
                }
            } else {
                histDoc = generateNewHistoryDocument(histDb);
            }
            return histDoc;
        }

        /**
         * @return Document containing root element for history data.
         */
        private Document generateNewHistoryDocument() throws ParserConfigurationException {
            var dbf = DocumentBuilderFactory.newInstance();
            return generateNewHistoryDocument(dbf.newDocumentBuilder());
        }

        /**
         * @param db a DocumentBuilder.
         * @return Document containing root element for history data.
         */
        private Document generateNewHistoryDocument(DocumentBuilder db) {
            var histDoc = db.newDocument();
            var root = histDoc.createElement("jttp_history");
            histDoc.appendChild(root);
            return histDoc;
        }

        /**
         * Adds an entry with the appropriate ID and saves it to the Session FileSystem.
         * 
         * @param sessionFs   FileSystem to write the history document to.
         * @param requestData data sent by the request, possibly null or empty.
         * @param timeOfRun   Instant the request is being recorded.
         * 
         * @return the ID of the entry appended to the history.
         * 
         * @throws IOException                          if an IOException occurs.
         * @throws TransformerFactoryConfigurationError if a TransformerFactoryConfigurationError
         *                                              occurs.
         * @throws TransformerException                 if a TransformerException occurs.
         */
        private int doUpdateAndSaveHistory(FileSystem sessionFs, String requestData,
                Instant timeOfRun)
                throws IOException, TransformerFactoryConfigurationError, TransformerException {
            var root = history.getDocumentElement(); // jttp_history
            var id = 1;
            var elts = root.getElementsByTagName("entry");
            if (nonNull(elts) && elts.getLength() > 0) {
                for (int i = 0; i < elts.getLength(); i++) {
                    var entry = (Element) elts.item(i);
                    var entryId = Integer.valueOf(entry.getAttribute("id"));
                    id = Math.max(id, entryId);
                }
                id++;
            }
            root.appendChild(createEntry(id, requestData, timeOfRun));
            writeHistory(sessionFs);
            return id;
        }

        /**
         * Creates an Entry in the history document.
         * 
         * @param id          the ID for the entry.
         * @param requestData data sent by the request, possibly null or empty.
         * @param timeOfRun   Instant the request is being recorded.
         * @return the entry Node.
         * 
         * @throws IOException if an IOException occurs.
         */
        private Node createEntry(int id, String requestData, Instant timeOfRun) throws IOException {
            var entry = history.createElement("entry");
            entry.setAttribute("id", Integer.toString(id));
            entry.setAttribute("timestamp", Long.toString(timeOfRun.toEpochMilli()));

            var request = history.createElement("request");

            var method = history.createElement("method");
            var methodText = history.createTextNode(conn.getRequestMethod());
            method.appendChild(methodText);
            request.appendChild(method);

            var uri = history.createElement("uri");
            var uriText = history.createTextNode(conn.getURL().getPath());
            uri.appendChild(uriText);
            request.appendChild(uri);

            if (nonNull(conn.getURL().getQuery())) {
                var query = history.createElement("query");
                var queryString = history.createCDATASection(conn.getURL().getQuery());
                query.appendChild(queryString);
                request.appendChild(query);
            }

            if (nonNull(requestData) && !requestData.isEmpty() && !requestData.isBlank()) {
                var data = history.createElement("data");
                var dataString = history.createCDATASection(requestData);
                data.appendChild(dataString);
                request.appendChild(data);
            }
            entry.appendChild(request);

            var response = history.createElement("response");

            var status = history.createElement("status");
            var statuscode = history.createTextNode(Integer.toString(conn.getResponseCode()));
            status.appendChild(statuscode);
            response.appendChild(status);

            if (nonNull(conn.getHeaderField("Content-Type"))) {
                var contentType = history.createElement("content_type");
                var contentTypeString = history.createTextNode(conn.getHeaderField("Content-Type"));
                contentType.appendChild(contentTypeString);
                response.appendChild(contentType);
            }

            entry.appendChild(response);
            return entry;
        }

        /**
         * Writes the history to the Session FileSystem.
         * 
         * @param sessionFs where to write the history.
         * 
         * @throws IOException                          if an IOException occurs.
         * @throws TransformerFactoryConfigurationError if a TransformerFactoryConfigurationError
         *                                              occurs.
         * @throws TransformerException                 if a TransformerException occurs.
         */
        private void writeHistory(FileSystem sessionFs)
                throws TransformerFactoryConfigurationError, TransformerException, IOException {
            var historyXml = File.createTempFile("history", ".xml");
            if (DELETE_TEMPFILES) {
                historyXml.deleteOnExit();
            }

            var formatted = formatMarkup(new DOMSource(history));
            var historyXmlAsPath = Paths.get(historyXml.toURI());
            Files.writeString(historyXmlAsPath, formatted, CREATE);
            var historyXmlInSession = sessionFs.getPath("/history.xml");
            Files.copy(historyXmlAsPath, historyXmlInSession, REPLACE_EXISTING);
        }

        /**
         * Writes the response data to a file in the Session FileSystem.
         * 
         * <p>
         * File name has the format {@code entry-[ID]-[timeOfRunInMillis]}.
         * 
         * @param sessionFs the Session FileSystem.
         * @param response  the response data File.
         * @param timeOfRun Instant the request is being recorded.
         * @param entryId   Id for the entry.
         * 
         * @throws IOException if an IOException occurs.
         */
        private void doSaveResponseData(FileSystem sessionFs, File response, Instant timeOfRun,
                int entryId) throws IOException {
            var responseAsPath = Paths.get(response.toURI());
            var responseInSession =
                    sessionFs.getPath(format("/entry-%d-%d", entryId, timeOfRun.toEpochMilli()));
            Files.copy(responseAsPath, responseInSession, REPLACE_EXISTING);
        }

        /**
         * Save a file named {@code cookies.xml} in the session zip file.
         * 
         * @param sessionFs the psuedo FileSystem of the zip file.
         * @throws IOException        if an IOException occurs.
         * @throws XMLStreamException if an XMLStreamException.
         */
        private void doSaveCookies(FileSystem sessionFs) throws IOException, XMLStreamException {
            var cookiesXml = File.createTempFile("cookies", ".xml");
            if (DELETE_TEMPFILES) {
                cookiesXml.deleteOnExit();
            }
            // Go through the response headers, filter out the Set-Cookie header(s), create
            // a flat List containing only the values from the Set-Cookie header(s).
            var setCookies = conn.getHeaderFields().entrySet().stream()
                    .filter(e -> "Set-Cookie".equals(e.getKey())).map(e -> e.getValue())
                    .collect(toList()).stream().flatMap(List::stream).collect(toList());
            // Check the request headers as well and add those too.
            setCookies.addAll(conn.getRequestProperties().entrySet().stream()
                    .filter(e -> "Cookie".equals(e.getKey())).map(e -> e.getValue())
                    .collect(toList()).stream().flatMap(List::stream).collect(toList()));

            try (var outfile = new FileWriter(cookiesXml)) {
                var xmlOf = XMLOutputFactory.newInstance();
                var xsw = xmlOf.createXMLStreamWriter(outfile);
                xsw.writeStartDocument("utf-8", "1.0");
                doFormat(xsw, 0);
                if (setCookies.isEmpty()) {
                    xsw.writeEmptyElement("jttp_cookies");
                } else {
                    xsw.writeStartElement("jttp_cookies");
                    var indentLevel = 1;
                    doFormat(xsw, indentLevel);
                    for (int i = 0; i < setCookies.size(); i++) {
                        var setCookie = setCookies.get(i);
                        xsw.writeStartElement("cookies");
                        doFormat(xsw, ++indentLevel);
                        var httpCookies = HttpCookie.parse(setCookie);
                        for (var httpCookie : httpCookies) {
                            xsw.writeStartElement("cookie");
                            xsw.writeAttribute("name", httpCookie.getName());
                            doFormat(xsw, ++indentLevel);
                            if (nonNull(httpCookie.getPath()) && !httpCookie.getPath().isEmpty()) {
                                writeXmlElement(xsw, "path", httpCookie.getPath());
                            }

                            doFormat(xsw, indentLevel);
                            writeXmlElement(xsw, "secure",
                                    Boolean.toString(httpCookie.getSecure()));

                            doFormat(xsw, indentLevel);
                            writeXmlElement(xsw, "value", httpCookie.getValue());

                            if (nonNull(httpCookie.getComment())
                                    && !httpCookie.getComment().isEmpty()) {
                                doFormat(xsw, indentLevel);
                                writeXmlElement(xsw, "comment", httpCookie.getComment());
                            }

                            if (nonNull(httpCookie.getCommentURL())
                                    && !httpCookie.getCommentURL().isEmpty()) {
                                doFormat(xsw, indentLevel);
                                writeXmlElement(xsw, "commentUrl", httpCookie.getCommentURL());
                            }

                            doFormat(xsw, indentLevel);
                            writeXmlElement(xsw, "discard",
                                    Boolean.toString(httpCookie.getDiscard()));

                            doFormat(xsw, indentLevel);
                            writeXmlElement(xsw, "max_age", Long.toString(httpCookie.getMaxAge()));

                            doFormat(xsw, indentLevel);
                            writeXmlElement(xsw, "version",
                                    Integer.toString(httpCookie.getVersion()));

                            doFormat(xsw, indentLevel);
                            writeXmlElement(xsw, "http_only",
                                    Boolean.toString(httpCookie.isHttpOnly()));

                            if (nonNull(httpCookie.getPortlist())
                                    && !httpCookie.getPortlist().isEmpty()) {
                                doFormat(xsw, indentLevel);
                                writeXmlElement(xsw, "port_list", httpCookie.getPortlist());
                            }

                            if (nonNull(httpCookie.getDomain())
                                    && !httpCookie.getDomain().isEmpty()) {
                                doFormat(xsw, indentLevel);
                                writeXmlElement(xsw, "domain", httpCookie.getDomain());
                            }
                            doFormat(xsw, --indentLevel);
                            xsw.writeEndElement(); // cookie
                        }
                        doFormat(xsw, --indentLevel);
                        xsw.writeEndElement();
                        doFormat(xsw, i < setCookies.size() - 1 ? indentLevel : 0);
                    }
                }
                xsw.writeEndDocument();
            }
            var cookiesXmlAsPath = Paths.get(cookiesXml.toURI());
            var cookiesXmlInSession = sessionFs.getPath("/cookies.xml");
            Files.copy(cookiesXmlAsPath, cookiesXmlInSession, REPLACE_EXISTING);
        }

        /**
         * Save a file named {@code headers.xml} in the session zip file.
         * 
         * @param sessionFs the psuedo FileSystem of the zip file.
         * @throws IOException        if an IOException occurs.
         * @throws XMLStreamException if an XMLStreamException occurs.
         */
        private void doSaveHeaders(FileSystem sessionFs) throws IOException, XMLStreamException {
            var headersXml = File.createTempFile("headers", ".xml");
            if (DELETE_TEMPFILES) {
                headersXml.deleteOnExit();
            }
            try (var outfile = new FileWriter(headersXml)) {
                var xmlOf = XMLOutputFactory.newInstance();
                XMLStreamWriter xsw = xmlOf.createXMLStreamWriter(outfile);
                xsw.writeStartDocument("utf-8", "1.0");
                doFormat(xsw, 0);
                if (conn.getRequestProperties().isEmpty()) {
                    xsw.writeEmptyElement("jttp_headers");
                } else {
                    xsw.writeStartElement("jttp_headers");
                    var indentLevel = 1;
                    doFormat(xsw, indentLevel);
                    var reqProps = conn.getRequestProperties().entrySet().stream()
                            .filter(e -> !RESTRICTED_HEADERS.contains(e.getKey())
                                    && nonNull(e.getValue().get(0)))
                            .collect(toMap(e -> e.getKey(), e -> e.getValue()));
                    for (Iterator<String> iter0 = reqProps.keySet().iterator(); iter0.hasNext();) {
                        var header = iter0.next();
                        var values = reqProps.get(header);
                        if (!values.isEmpty()) {
                            xsw.writeStartElement("header");
                            xsw.writeAttribute("name", header);
                            doFormat(xsw, ++indentLevel);
                            xsw.writeStartElement("values");
                            doFormat(xsw, ++indentLevel);
                            for (Iterator<String> iter1 = values.iterator(); iter1.hasNext();) {
                                writeXmlElement(xsw, "value", iter1.next());
                                if (iter1.hasNext()) {
                                    doFormat(xsw, indentLevel);
                                }
                            }
                            doFormat(xsw, --indentLevel);
                            xsw.writeEndElement(); // values
                            doFormat(xsw, --indentLevel);
                            xsw.writeEndElement(); // header
                            if (iter0.hasNext()) {
                                doFormat(xsw, indentLevel);
                            }
                        }
                    }
                    doFormat(xsw, --indentLevel);
                    xsw.writeEndElement();
                }
                xsw.writeEndDocument();
            }
            var headersXmlAsPath = Paths.get(headersXml.toURI());
            var headersXmlInSession = sessionFs.getPath("/headers.xml");
            Files.copy(headersXmlAsPath, headersXmlInSession, REPLACE_EXISTING);
        }

        /**
         * Writes an XML element of the form {@code &lt;tag&gt;text&lt;/tag&gt;} to the
         * XMLStreamWriter.
         * 
         * @param xsw  XMLStreamWriter
         * @param tag  the tag name
         * @param text the text
         * @throws XMLStreamException if an exception occurs writing to the XMLStreamWriter.
         */
        private void writeXmlElement(XMLStreamWriter xsw, String tag, String text)
                throws XMLStreamException {
            xsw.writeStartElement(tag);
            xsw.writeCharacters(text);
            xsw.writeEndElement();
        }

        /**
         * Writes a line separator and character array (filled w/ spaces) to the XMLStreamWriter.
         * 
         * <P>
         * The number of spaces to indent is calculated as {@code INDENT * indentLevel} where
         * {@code INDENT} is set by the {@code jttp.indent} system property on the command line or
         * the default of {@code 2}.
         * 
         * @param xsw         the XMLStreamWriter.
         * @param indentLevel indent level.
         * @throws XMLStreamException if an exception occurs during formatting.
         */
        private void doFormat(XMLStreamWriter xsw, int indentLevel) throws XMLStreamException {
            if (formatSessionXml) {
                xsw.writeCharacters(AccessController.doPrivileged(
                        (PrivilegedAction<String>) () -> System.getProperty("line.separator")));

                if (indentLevel > 0) {
                    var spaces = new char[INDENT * indentLevel];
                    Arrays.fill(spaces, ' ');
                    xsw.writeCharacters(spaces, 0, spaces.length);
                }
            }
        }

        /**
         * @param sessionName name for the session zip file.
         * @param url         the URI for the Jttp request.
         * 
         * @return a URI for the zip file to use as the basis for a pseudo FileSystem object.
         */
        private URI initSessionFsUri(String sessionName, URI url) {
            var hostDir = url.getPort() != -1 ? format("%s_%d", url.getHost(), url.getPort())
                    : url.getHost();
            var saveDir = Paths.get(getSessionsDirectory(), hostDir);
            if (!saveDir.toFile().exists()) {
                saveDir.toFile().mkdirs();
            }
            return URI.create(format("jar:file:%s/%s.zip", saveDir.toString(), sessionName));
        }
    }

    /**
     * Parses the command line and instantiate an Object from it.
     * 
     * <p>
     * Typical usage scenario:
     * 
     * <pre>
     * {
     *     &#64;code
     *     class MyProgram implements Runnable {
     * 
     *         private static final ResourceBundle RB =
     *                 ResourceBundle.getBundle("messages_myprogram");
     * 
     *         public static void main(String... args) {
     *             CommandLine.run(new MyProgram(), RB, args);
     *         }
     * 
     *     }
     * }
     * </pre>
     * 
     * This will parse the arguments and run the program.
     * 
     * <p>
     * A {@link ResourceBundle} is used for the messages output by the CommandLine. The keys and
     * required arguments are:
     * 
     * <table>
     * <th>
     * <td>Key</td>
     * <td>Description</td>
     * <td>Arguments</td></th>
     * <tr>
     * <td>{@code cmdline.help.footer}</td>
     * <td>Footer message to pass to the {@link Help} instance.</td>
     * <td>N/A</td>
     * </tr>
     * <tr>
     * <td>{@code cmdline.help.header}</td>
     * <td>Header message to pass to the {@link Help} instance.</td>
     * <td>N/A</td>
     * </tr>
     * <tr>
     * <td>{@code cmdline.error.missing.required.arg}</td>
     * <td>Message indicating a required {@link Argument} was not set on the command line.</td>
     * <td>
     * <ul>
     * <li>{0} - Command/Class name for this program.
     * <li>{1} - Name of required {@link Argument}.
     * </ul>
     * </td>
     * </tr>
     * <tr>
     * <td>{@code cmdline.error.unknown.option}</td>
     * <td>Message indicating an unknown {@link Option} was set on the command line.</td>
     * <td>
     * <ul>
     * <li>{0} - Command/Class name for this program.
     * <li>{1} - {@link Option} found on the command line.
     * </ul>
     * </td>
     * </tr>
     * <tr>
     * <td>{@code cmdline.error.runtime.exception.msg}</td>
     * <td>Message for a RuntimeException.</td>
     * <td>
     * <ul>
     * <li>{0} - Command/Class name for this program.
     * <li>{1} - The exception message (without a stacktrace).
     * </ul>
     * </td>
     * </tr>
     * <tr>
     * <td>{@code cmdline.error.opt.missing.arg}</td>
     * <td>Message indicating an option (usually in an option cluster) requires a missing
     * argument.</td>
     * <td>
     * <ul>
     * <li>{0} - Command/Class name for this program.
     * <li>{1} - Option (short) name.
     * <li>{2} - Argument name.
     * </ul>
     * </td>
     * </tr>
     * </table>
     * 
     * <p>
     * When an unsupported option, missing required argument, or RuntimeException is encountered,
     * CommandLine will print an error message, if applicable print a usage message, and exit with a
     * code of {@code 1}. When a help or version request is made, CommandLine will print the
     * requested message type and exit with a code of {@code 2}.
     */
    static class CommandLine {

        private static final Predicate<String> IS_SHORT_OPTION =
                s -> s.startsWith("-") && !s.startsWith("--");

        private static final Predicate<String> IS_LONG_OPTION = s -> s.startsWith("--");

        private static final Predicate<Field> IS_OPTION_FIELD =
                f -> nonNull(f) && nonNull(f.getAnnotation(Option.class));

        private static final Function<String, String> OPT_NAME =
                o -> o.startsWith("--") ? o.substring(2) : o.substring(1);

        private final Object instance;

        private final Help help;

        private final ResourceBundle rb;

        private List<Field> argFields = new ArrayList<>();

        private List<Argument> supportedArgsList = new ArrayList<>();

        private List<Option> supportedOptsList = new ArrayList<>();

        private Map<String, Field> supportedOptionsByShortName = new HashMap<>();

        private Map<String, Field> supportedOptionsByLongName = new HashMap<>();

        /**
         * Construct a new CommandLine instance.
         */
        CommandLine(Object instance, ResourceBundle rb) {
            this.instance = instance;
            initializeSupportedArgumentsAndOptions();
            this.help =
                    new Help(instance.getClass().getName(), supportedArgsList, supportedOptsList);
            this.rb = rb;
        }

        private void initializeSupportedArgumentsAndOptions() {
            var c = instance.getClass();
            var fields = c.getDeclaredFields();

            for (Field field : fields) {
                var argument = field.getAnnotation(Argument.class);
                var option = field.getAnnotation(Option.class);
                if (nonNull(argument)) {
                    supportedArgsList.add(argument);
                    if (argument.index() != -1) {
                        argFields.add(argument.index(), field);
                    } else {
                        argFields.add(field);
                    }
                }
                if (nonNull(option)) {
                    supportedOptsList.add(option);
                    if (!option.shortName().isEmpty()) {
                        supportedOptionsByShortName.put(option.shortName(), field);
                    }
                    if (!option.longName().isEmpty()) {
                        supportedOptionsByLongName.put(option.longName(), field);
                    }
                }
            }
        }

        private void parseArgs(String[] args) throws IllegalAccessException, InstantiationException,
                InvocationTargetException, NoSuchMethodException {

            var allOptsProcessed = false;
            var currentArg = "";
            Field curFld = null;
            Option curOpt = null;
            Map<Field, List<String>> optArrays = new HashMap<>();

            Object argArray = null;
            int argsProcessed = 0;
            int argArrayLen = 0;
            int argArrayIdx = 0;
            Argument curArg = null;

            for (int i = 0; i < args.length; i++) {
                currentArg = args[i];

                if (!allOptsProcessed && isNull(curFld)
                        && IS_SHORT_OPTION.or(IS_LONG_OPTION).test(currentArg)) {
                    curFld = IS_SHORT_OPTION.test(currentArg)
                            ? supportedOptionsByShortName.get(OPT_NAME.apply(currentArg))
                            : supportedOptionsByLongName.get(OPT_NAME.apply(currentArg));
                    if (isNull(curFld)) {
                        if (IS_SHORT_OPTION.test(currentArg)) {
                            var optCluster = OPT_NAME.apply(currentArg);
                            for (int j = 0; j < optCluster.length(); j++) {
                                var shortname = optCluster.subSequence(j, j + 1);
                                curFld = supportedOptionsByShortName.get(shortname);
                                curOpt = curFld.getAnnotation(Option.class);
                                if (curOpt.isHelp()) {
                                    help.printHelp(rb.getString("cmdline.help.header"),
                                            rb.getString("cmdline.help.footer"));
                                    System.exit(2);
                                }
                                if (curOpt.isVersion()) {
                                    help.printMessage(versionString(instance, true));
                                    System.exit(2);
                                }
                                if (!curOpt.argRequired()) {
                                    setFieldOnInstance(curFld);
                                    curFld = null;
                                    curOpt = null;
                                } else {
                                    help.printMessageAndSyntax(MessageFormat.format(
                                            rb.getString("cmdline.error.opt.missing.arg"),
                                            instance.getClass().getName(), shortname,
                                            curOpt.argName()));
                                    System.exit(1);
                                }
                            }
                            continue;
                        } else {
                            help.printMessageAndSyntax(MessageFormat.format(
                                    rb.getString("cmdline.error.unknown.option"),
                                    instance.getClass().getName(), currentArg));
                            System.exit(1);
                        }
                    }
                    curOpt = curFld.getAnnotation(Option.class);
                    if (curOpt.isHelp()) {
                        help.printHelp(rb.getString("cmdline.help.header"),
                                rb.getString("cmdline.help.footer"));
                        System.exit(2);
                    }
                    if (curOpt.isVersion()) {
                        help.printMessage(versionString(instance, true));
                        System.exit(2);
                    }
                    if (!curOpt.argRequired()) {
                        setFieldOnInstance(curFld);
                        curFld = null;
                        curOpt = null;
                    }
                } else if (!allOptsProcessed && nonNull(curFld) && IS_OPTION_FIELD.test(curFld)
                        && nonNull(curOpt) && curOpt.argRequired()) {
                    if (curFld.getType().isArray()) {
                        if (!optArrays.containsKey(curFld)) {
                            optArrays.put(curFld, new ArrayList<String>());
                        }
                        var values = optArrays.get(curFld);
                        values.add(currentArg);
                    } else if (curFld.getType().isEnum()) {
                        try {
                            if (isValidEnumValue(curFld, currentArg)) {
                                setEnumFieldOnInstance(curFld, currentArg);
                            } else if (isValueSet(curFld)) {
                                // Ignore it and move on.
                            }
                        } catch (IllegalArgumentException e) {
                            help.printMessageAndSyntax(MessageFormat.format(
                                    rb.getString("cmdline.error.runtime.exception.msg"),
                                    instance.getClass().getName(), e.getMessage()));
                            System.exit(1);
                        }
                    } else {
                        setFieldOnInstance(curFld, currentArg);
                    }
                    curFld = null;
                    curOpt = null;
                } else {
                    // Must be an @Argument
                    allOptsProcessed = true;

                    if (isNull(curFld)) {
                        curFld = argFields.remove(argsProcessed);
                    }
                    curArg = curFld.getAnnotation(Argument.class);
                    if (curFld.getType().isArray()) {
                        if (isNull(argArray)) {
                            argArrayLen = args.length - i;
                            argArray = Array.newInstance(curFld.getType().getComponentType(),
                                    argArrayLen);
                        }
                        if (argArrayIdx < argArrayLen) {
                            Object inst = currentArg;
                            if (!curFld.getType().getComponentType().equals(String.class)) {
                                var ctor = curFld.getType().getComponentType()
                                        .getConstructor(currentArg.getClass());
                                inst = ctor.newInstance(currentArg);
                            }
                            Array.set(argArray, argArrayIdx, inst);
                            argArrayIdx++;
                        } else {
                            setFieldValueOn(curFld, argArray);
                            argArray = null;
                            curFld = null;
                            argArrayLen = argArrayIdx = 0;
                        }
                    } else if (curFld.getType().isEnum()) {
                        try {
                            if (isValidEnumValue(curFld, currentArg)) {
                                setEnumFieldOnInstance(curFld, currentArg);
                            } else if (isValueSet(curFld) && !curArg.required()) {
                                // Not a required argument and it has a default set so move the args
                                // pointer back 1.
                                i--;
                                curFld = null;
                            }
                        } catch (IllegalArgumentException e) {
                            help.printMessageAndSyntax(MessageFormat.format(
                                    rb.getString("cmdline.error.runtime.exception.msg"),
                                    instance.getClass().getName(), e.getMessage()));
                            System.exit(1);
                        }
                    } else {
                        setFieldOnInstance(curFld, currentArg);
                        curFld = null;
                    }
                }
            }

            // Make sure no lingering @Argument array types need to be set.
            if (nonNull(argArray)) {
                setFieldValueOn(curFld, argArray);
            }

            // Make sure all required args are set.
            if (!argFields.isEmpty()) {
                for (Field f : argFields) {
                    var argument = f.getAnnotation(Argument.class);
                    if (argument.required()) {
                        help.printMessageAndSyntax(MessageFormat.format(
                                rb.getString("cmdline.error.missing.required.arg"),
                                instance.getClass().getName(), argument.name()));
                        System.exit(1);
                    }
                }
            }

            // Set any values for array @Options
            for (Map.Entry<Field, List<String>> optAry : optArrays.entrySet()) {
                var fld = optAry.getKey();
                var type = fld.getType().getComponentType();
                var vals = optAry.getValue();
                var optArray = Array.newInstance(type, vals.size());
                var optArrayIdx = 0;
                for (String val : vals) {
                    Object inst = val;
                    if (!type.equals(String.class)) {
                        var ctor = type.getConstructor(val.getClass());
                        inst = ctor.newInstance(val);
                    }
                    Array.set(optArray, optArrayIdx, inst);
                    optArrayIdx++;
                }
                setFieldValueOn(fld, optArray);
            }
        }

        private void setFieldOnInstance(Field f) throws IllegalAccessException {
            setFieldValueOn(f, true);
        }

        private void setFieldOnInstance(Field f, String value) throws NoSuchMethodException,
                IllegalAccessException, InstantiationException, InvocationTargetException {
            var fieldClass = f.getType();
            var constructor = fieldClass.getConstructor(value.getClass());
            var fieldValue = constructor.newInstance(value);
            setFieldValueOn(f, fieldValue);
        }

        private boolean isValidEnumValue(Field f, String value) {
            try {
                @SuppressWarnings({"unchecked", "unused", "rawtypes"})
                var unused = Enum.valueOf((Class<Enum>) f.getType(), value.toUpperCase());
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        private boolean isValueSet(Field f)
                throws IllegalArgumentException, IllegalAccessException {
            return nonNull(f.get(instance));
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private void setEnumFieldOnInstance(Field f, String value) throws IllegalAccessException {
            setFieldValueOn(f, Enum.valueOf((Class<Enum>) f.getType(), value.toUpperCase()));
        }

        private void setFieldValueOn(Field f, Object value) throws IllegalAccessException {
            var resetAccessible = false;
            if (!f.canAccess(instance)) {
                resetAccessible = true;
                f.setAccessible(true);
            }
            f.set(instance, value);
            if (resetAccessible) {
                f.setAccessible(false);
            }
        }

        /**
         * Parse the arguments and run the program.
         * 
         * @param instance the program (Runnable) to run.
         * @param rb       the ResourceBundle.
         * @param args     command line arguments.
         */
        static int run(Runnable instance, ResourceBundle rb, String[] args) {
            try {
                var cmdLine = new CommandLine(instance, rb);
                cmdLine.parseArgs(args);
                instance.run();
                return 0;
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException
                    | NoSuchMethodException | RuntimeException e) {
                LOGGER.log(ERROR, "cmdline.error.runtime.exception.msg",
                        instance.getClass().getName(), e.getMessage());
                System.err.println(
                        MessageFormat.format(rb.getString("cmdline.error.runtime.exception.msg"),
                                instance.getClass().getName(), e.getMessage()));
                e.printStackTrace();
                return 1;
            }
        }
    }

    /**
     * Prints help messages.
     */
    static class Help {

        private static final char[] NO_PADDING = new char[0];

        private static final String HANGING_INDENT_PADDING = "  ";

        // SOL = start of line
        private static final String SOL_PADDING = "  ";

        private static final String OPT_DESC_PADDING = "    ";

        private static final String SYN_PADDING = "    ";

        private static final String LS = System.getProperty("line.separator");

        private final int width = 80;

        private final List<Argument> supportedArgs;

        private final List<Option> supportedOptions;

        private final String command;

        private final int margin;

        private char[] hangingIndent;

        private int maxDefLen = 40;

        private PrintWriter pw;

        Help(String command, List<Argument> arguments, List<Option> options) {
            this.command = command;
            this.supportedArgs = arguments;
            this.supportedOptions = options;
            this.margin = (int) (width - (.1 * width));
            pw = new PrintWriter(System.out, true);
        }

        void printMessage(String message) {
            pw.println(message);
        }

        void printSyntax() {
            var syn = new StringBuilder("usage: ").append(command).append(" ");
            var clusteredOpts = supportedOptions.stream()
                    .filter(o -> !o.shortName().isEmpty() && !o.argRequired())
                    .map(o -> o.shortName()).sorted(new SortIgnoreCase()).collect(joining());
            syn.append("[-").append(clusteredOpts).append("] ");
            var remainingOpts = supportedOptions.stream()
                    .filter(o -> o.shortName().isEmpty() || o.argRequired()).collect(toList());
            var synpadding = SYN_PADDING.toCharArray();
            checkWidth(syn, synpadding);
            for (var opt : remainingOpts) {
                var optNameToPrint =
                        !opt.shortName().isEmpty() ? "-" + opt.shortName() : "--" + opt.longName();
                var optbuilder = new StringBuilder("[").append(optNameToPrint);
                if (opt.argRequired()) {
                    optbuilder.append(" ").append(opt.argName());
                }
                if (opt.multipleSpecs()) {
                    optbuilder.append(" [").append(optNameToPrint);
                    if (opt.argRequired()) {
                        optbuilder.append(" ").append(opt.argName());
                    }
                    optbuilder.append("]").append(" ...");
                }
                var optsyn = optbuilder.append("] ").toString();
                if (syn.length() + optsyn.length() >= margin) {
                    appendWords(optsyn.split(" "), synpadding, syn);
                } else {
                    syn.append(optsyn);
                }
            }
            for (Argument arg : supportedArgs) {
                var argbuilder = new StringBuilder();
                if (!arg.required()) {
                    argbuilder.append("[");
                }
                argbuilder.append(arg.name());
                if (arg.multipleSpecs()) {
                    argbuilder.append(" ...");
                }
                if (!arg.required()) {
                    argbuilder.append("]");
                }
                checkWidth(argbuilder.length(), syn, synpadding);
                syn.append(argbuilder).append(" ");
            }
            pw.println(syn.toString());
        }

        void printOptions() {
            var optsyntax = new ArrayList<String>();
            for (var opt : supportedOptions) {
                var optNm = new StringBuilder();
                if (!opt.shortName().isEmpty()) {
                    optNm.append("-").append(opt.shortName());
                }
                if (optNm.length() > 0 && !opt.longName().isEmpty()) {
                    optNm.append(",");
                }
                if (!opt.longName().isEmpty()) {
                    optNm.append("--").append(opt.longName());
                }
                var optArg = opt.argName().length() > 0 ? opt.argName() : "";
                optsyntax.add(String.format("%s %s", optNm.toString(), optArg));
            }

            maxDefLen = Math.min(maxDefLen, longestStringIn(optsyntax));

            for (int i = 0; i < supportedOptions.size(); i++) {
                var sb = new StringBuilder(SOL_PADDING);
                String optsyn = optsyntax.get(i);
                sb.append(optsyn);

                padBetweenNameAndDef(optsyn.length(), OPT_DESC_PADDING.length(), sb);

                var opt = supportedOptions.get(i);
                var optdesc = opt.description().length() > 0 ? opt.description()
                        : opt.resourceKey().length() > 0 ? RB.getString(opt.resourceKey()) : "";
                if (sb.length() + optdesc.length() >= margin) {
                    appendWords(optdesc.split(" "), getHangingIndent(), sb);
                } else {
                    sb.append(optdesc);
                }
                pw.println(sb.toString());
            }
        }

        void printArguments() {
            var argnames = new ArrayList<String>();
            for (var arg : supportedArgs) {
                argnames.add(arg.name());
            }
            for (int i = 0; i < supportedArgs.size(); i++) {
                var sb = new StringBuilder(SOL_PADDING);
                var argname = argnames.get(i);
                sb.append(argname);

                padBetweenNameAndDef(argname.length(), OPT_DESC_PADDING.length(), sb);

                var arg = supportedArgs.get(i);
                var argdesc = arg.description().length() > 0 ? arg.description()
                        : arg.resourceKey().length() > 0 ? RB.getString(arg.resourceKey()) : "";
                if (sb.length() + argdesc.length() >= margin) {
                    appendWords(argdesc.split(" "), getHangingIndent(), sb);
                } else {
                    sb.append(argdesc);
                }
                pw.println(sb.toString());
            }
        }

        void printMessageAndSyntax(String msg) {
            pw.println(msg);
            printSyntax();
        }

        void printHelp(String header, String footer) {
            printSyntax();
            if (nonNull(header) && !header.isEmpty() && !header.isBlank()) {
                pw.println();
                var hb = new StringBuilder();
                appendWords(header.split(" "), NO_PADDING, hb);
                pw.println(hb.toString());
            }
            if (!supportedOptions.isEmpty()) {
                pw.println();
                pw.println("Options:");
                printOptions();
            }
            if (!supportedArgs.isEmpty()) {
                pw.println();
                pw.println("Arguments:");
                printArguments();
            }
            if (nonNull(footer) && !footer.isEmpty() && !footer.isBlank()) {
                pw.println();
                var fb = new StringBuilder();
                appendWords(footer.split(" "), NO_PADDING, fb);
                pw.println(fb.toString());
            }
        }

        private char[] getHangingIndent() {
            if (isNull(hangingIndent)) {
                hangingIndent = new char[computeHangingIndent(maxDefLen)];
                Arrays.fill(hangingIndent, ' ');
            }
            return hangingIndent;
        }

        private void appendWords(String[] words, char[] padding, StringBuilder sb) {
            for (var word : words) {
                checkWidth(sb, padding);
                sb.append(word).append(" ");
            }
        }

        private void padBetweenNameAndDef(int nmLen, int paddingLen, StringBuilder sb) {
            var padlen = maxDefLen - nmLen + paddingLen;
            var spaces = new char[padlen];
            Arrays.fill(spaces, ' ');
            sb.append(spaces);
        }

        private void checkWidth(StringBuilder sb, char[] padding) {
            checkWidth(0, sb, padding);
        }

        private void checkWidth(int startLen, StringBuilder sb, char[] padding) {
            var linesInSb = (startLen + sb.length()) / width;
            var curLineLen = (startLen + sb.length()) % width;
            if (curLineLen >= margin
                    || (linesInSb > 0 && sb.indexOf(LS, startLen + sb.length() - width) == -1)) {
                sb.append(LS).append(padding);
            }
        }

        private int longestStringIn(Collection<String> strs) {
            int maxlen = 0;
            for (var str : strs) {
                maxlen = Math.max(maxlen, str.length());
            }
            return maxlen;
        }

        private int computeHangingIndent(int maxDefLen) {
            return SOL_PADDING.length() + maxDefLen + OPT_DESC_PADDING.length()
                    + HANGING_INDENT_PADDING.length();
        }

        static class SortIgnoreCase implements Comparator<String> {
            public int compare(String s1, String s2) {
                return s1.toLowerCase().compareTo(s2.toLowerCase());
            }
        }
    }

    /**
     * Designates the version of the application.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    static @interface Version {
        /**
         * @return the name to use in the version string.
         */
        String name();

        /**
         * @return the major component of the version.
         */
        String major() default "0";

        /**
         * @return the minor component of the version.
         */
        String minor() default "0";

        /**
         * @return the patch component of the version.
         */
        String patch() default "0";

        /**
         * @return the suffix of the version string.
         */
        String suffix() default "";
    }

    /**
     * Designates an Argument field on the command line.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    static @interface Argument {

        /**
         * @return {@code true} if this Argument can be specified multiple times.
         */
        boolean multipleSpecs() default false;

        /**
         * @return {@code true} if this Argument is required.
         */
        boolean required() default false;

        /**
         * @return text description of this Argument.
         */
        String description() default "";

        /**
         * @return key in a resource bundle that is a description of this Argument.
         */
        String resourceKey() default "";

        /**
         * @return the name of this Argument.
         */
        String name();

        /**
         * @return the position of this Argument on the command line.
         */
        int index() default -1;
    }

    /**
     * Designates an Option field on the command line.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    static @interface Option {
        /**
         * @return short (1-letter) option name.
         */
        String shortName() default "";

        /**
         * @return long option name.
         */
        String longName() default "";

        /**
         * @return {@code true} if this Option has a required argument.
         */
        boolean argRequired() default false;

        /**
         * @return {@code true} if this Argument can be specified multiple times.
         */
        boolean multipleSpecs() default false;

        /**
         * @return text description of this Option.
         */
        String description() default "";

        /**
         * @return key in a resource bundle that is a description of this Option.
         */
        String resourceKey() default "";

        /**
         * @return name of the argument of this Option.
         */
        String argName() default "";

        /**
         * @return {@code true} if this Option should trigger a help message.
         */
        boolean isHelp() default false;

        /**
         * @return {@code true} if this Option should trigger a version message.
         */
        boolean isVersion() default false;
    }
}
