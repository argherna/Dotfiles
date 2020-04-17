import java.io.CharArrayWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Indent XML read in from {@link System#in System.in}.
 * 
 * <p>
 * Set the system property {@code xmlformat.indent} to control the amount of spaces used to indent
 * the XML (default is {@value XmlFormat#DEFAULT_INDENT}).
 */
class XmlFormat {

    private static final Integer DEFAULT_INDENT = 2;

    private static final Integer INDENT = Integer.getInteger("xmlformat.indent", DEFAULT_INDENT);

    public static void main(String... args) {

        try (var in = System.in) {
            var transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
                    Integer.toString(INDENT));
            var caw = new CharArrayWriter();
            var transformedDoc = new StreamResult(caw);
            var xmlSource = new StreamSource(in);
            transformer.transform(xmlSource, transformedDoc);

            // Output string to byte array
            System.out.println(caw.toCharArray());
            System.exit(0);
        } catch (Exception e) {
            System.err.println("%s:error: " + e.getMessage());
            System.exit(1);
        }
    }
}
