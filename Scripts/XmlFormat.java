import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Set;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Indent XML input.
 * 
 * <p>
 * Set the system property {@code xmlformat.indent} to control the amount of spaces used to indent
 * the XML (default is {@value XmlFormat#DEFAULT_INDENT}).
 * 
 * <p>
 * Set the system property {@code xmlformat.showtraces} to {@code true} to show stack traces from
 * exceptions.
 */
class XmlFormat implements Runnable {

    private static final Boolean SHOWTRACES = Boolean.getBoolean("xmlformat.showtraces");

    private static final Integer DEFAULT_INDENT = 2;

    private static final Integer INDENT = Integer.getInteger("xmlformat.indent", DEFAULT_INDENT);

    private boolean indent = true;

    private boolean omitXmlDecl = false;

    private Charset charset = Charset.defaultCharset();

    private InputStream in = System.in;

    private PrintWriter out = new PrintWriter(System.out);

    public static void main(String... args) {
        try {
            var program = args.length > 0 ? parseArgs(new XmlFormat(), args) : new XmlFormat();
            program.run();
        } catch (Exception e) {
            System.err.printf("error %s: %s%n", XmlFormat.class.getName(), e.getMessage());
            if (SHOWTRACES) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    XmlFormat() {
    }

    @Override
    public void run() {

        var pw = getOut();

        try {
            var transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, getIndent());
            if (isIndent()) {
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
                        Integer.toString(INDENT));
            }
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, getCharset().name());
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, getOmitXmlDecl());
            var sw = new StringWriter();
            var transformedDoc = new StreamResult(sw);
            var xmlSource = new StreamSource(getIn());
            transformer.transform(xmlSource, transformedDoc);

            // Output string to byte array
            var formattedXml = sw.toString().replaceAll("\n *\n", "\n").replace("--><!", "-->\n<!")
                    .replace("?><", "?>\n<");
            pw.println(formattedXml);
            pw.flush();
        } catch (Exception e) {
            System.err.printf("error %s: %s%n", getClass().getName(), e.getMessage());
            if (SHOWTRACES) {
                e.printStackTrace();
            }
            System.exit(1);
        } finally {
            try {
                in.close();
                pw.close();
            } catch (IOException e) {
                if (SHOWTRACES) {
                    e.printStackTrace();
                }
            }
        }
    }

    boolean isIndent() {
        return indent;
    }

    String getIndent() {
        return indent ? "yes" : "no";
    }

    void setIndent(boolean indent) {
        this.indent = indent;
    }

    Charset getCharset() {
        return charset;
    }

    void setCharset(Charset charset) {
        this.charset = charset;
    }

    InputStream getIn() {
        return in;
    }

    void setIn(InputStream in) {
        this.in = in;
    }

    PrintWriter getOut() {
        return out;
    }

    void setOut(PrintWriter out) {
        this.out = out;
    }

    boolean isOmitXmlDecl() {
        return omitXmlDecl;
    }

    String getOmitXmlDecl() {
        return omitXmlDecl ? "yes" : "no";
    }

    void setOmitXmlDecl(boolean omitXmlDecl) {
        this.omitXmlDecl = omitXmlDecl;
    }

    private static XmlFormat parseArgs(XmlFormat instance, String... args) throws IOException {
        var requireArgs = Set.of('C', 'i', 'o');
        for (int i = 0; i < args.length; i++) {
            var arg = args[i];
            if (arg.startsWith("-")) {
                if (arg.length() > 2) {
                    var opts = arg.substring(1).toCharArray();
                    for (int j = 0; j < opts.length; j++) {
                        if (opts[j] == 'h') {
                            showUsage(instance);
                            showOptions();
                            System.exit(2);
                        } else if (!requireArgs.contains(opts[j])) {
                            setFieldOn(instance, opts[j]);
                        } else if (j != opts.length - 1 || i >= args.length - 1) {
                            showErrorAndUsage(instance,
                                    String.format("-%c missing required argument!", opts[j]));
                            System.exit(1);
                        } else {
                            setFieldOn(instance, opts[j], args[++i]);
                        }
                    }
                } else {
                    var opt = arg.charAt(1);
                    if (opt == 'h') {
                        showUsage(instance);
                        showOptions();
                        System.exit(2);
                    } else if (!requireArgs.contains(opt)) {
                        setFieldOn(instance, opt);
                    } else if (i >= args.length - 1) {
                        showErrorAndUsage(instance,
                                String.format("-%c missing required argument!", opt));
                        System.exit(1);
                    } else {
                        setFieldOn(instance, opt, args[++i]);
                    }
                }
            }
        }
        return instance;
    }

    private static void setFieldOn(XmlFormat instance, char opt, String arg) throws IOException {
        switch (opt) {
            case 'C':
                try {
                    instance.setCharset(Charset.forName(arg));
                } catch (Exception e) {
                    instance.setCharset(Charset.defaultCharset());
                }
                break;
            case 'i':
                // Assume this is a file. An exception will be thrown if not found.
                instance.setIn(new BufferedInputStream(new FileInputStream(arg)));
                break;
            case 'o':
                // Assume this is going to be a file.
                instance.setOut(new PrintWriter(new File(arg)));
                break;
            default:
                break;
        }
    }

    private static void setFieldOn(XmlFormat instance, char opt) {
        switch (opt) {
            case 'N':
                instance.setIndent(false);
                break;
            case 'O':
                instance.setOmitXmlDecl(true);
                break;
            default:
                break;
        }
    }

    private static void showErrorAndUsage(XmlFormat instance, String error) {
        System.err.printf("error %s: %s%n", instance.getClass().getName(), error);
        showUsage(instance);
    }

    private static void showUsage(XmlFormat instance) {
        System.err.printf("usage: %s [-hNO] [-C charset] [-i infile] [-o outfile]%n",
                instance.getClass().getName());
    }

    private static void showOptions() {
        System.err.println();
        System.err.println("Formats XML input.");
        System.err.println();
        System.err.println("Options:");
        System.err.println(" -C charset    Charset to write output to (default is "
                + Charset.defaultCharset() + ").");
        System.err.println(" -h            Shows help message and exits.");
        System.err.println(" -i infile     Input file (default is System.in).");
        System.err.println(" -N            Do not indent the output.");
        System.err.println(" -O            Omit the XML declaration in the output.");
        System.err.println(" -o outfile    Output file of formatted XML (default is System.out).");
        System.err.println();
        System.err.println(
                "Set system property xmlformat.indent to control the number of spaces of output");
        System.err.println("(default is 2).");
    }
}
