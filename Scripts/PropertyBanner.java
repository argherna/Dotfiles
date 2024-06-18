import java.io.PrintWriter;

/**
 * Prints a banner to be put into properties files.
 */
public class PropertyBanner implements Runnable {

    private static final Character DASH = '-';

    private static final Character LINE_COMMENT = '#';

    private static final Character SPACE = ' ';

    private static final Integer DEFAULT_WIDTH = 80;

    private static final PrintWriter DEFAULT_PRINTWRITER = new PrintWriter(System.out);

    private final int width;

    private final PrintWriter pw;

    private final String text;

    /**
     * The main.
     * 
     * <P>
     * This program takes an argument of a quoted string to be used as the text in
     * the banner. Typical usage is:
     * 
     * <PRE>{@code 
     * java PropertyBanner.java "Text to center in the banner"
     * }</PRE>
     * 
     * <P>
     * Output will be written to {@link System#out} and will look like this:
     * 
     * <PRE>
     * # ----------------------------------------------
     * #
     * #          Text to center in the banner
     * #
     * # ----------------------------------------------
     * </PRE>
     * 
     * <P>
     * The actual text will be 80 characters wide. The above is displayed for
     * formatting purposes only.
     * 
     * @param args command line arguments.
     */
    public static void main(String... args) {
        if (args.length < 1) {
            showUsageAndExit("No text set!", 2);
        }

        if (args[0].length() > DEFAULT_WIDTH) {
            showUsageAndExit("Text needs to be < " + DEFAULT_WIDTH + " characters!", 1);
        }

        var app = new PropertyBanner(args[0]);
        app.run();
    }

    /**
     * Show a error message, followed by a short usage message, and then exits with
     * the given code.
     * 
     * @param message error message.
     * @param code exit code.
     */
    private static void showUsageAndExit(String message, int code) {
        var usage = String.format("java %s \"text\"", PropertyBanner.class.getName());
        System.err.println(message);
        System.err.println(usage);
        System.exit(code);
    }

    /**
     * Construct a new instance of PropertyBanner.
     * 
     * @param text text for the banner.
     */
    public PropertyBanner(String text) {
        this.pw = DEFAULT_PRINTWRITER;
        this.text = text;
        this.width = DEFAULT_WIDTH;
    }

    /**
     * Runs the program.
     */
    @Override
    public void run() {
        printBorder();
        pw.println(LINE_COMMENT);
        printText();
        pw.println(LINE_COMMENT);
        printBorder();
    }

    /**
     * Prints the banner border.
     */
    private void printBorder() {
        var printed = 0;
        pw.print(LINE_COMMENT);
        printed++;
        pw.print(SPACE);
        printed++;
        for (var i = printed; i < width; i++) {
            pw.print(DASH);
        }
        pw.println();
        pw.flush();
    }

    /**
     * Prints the text centered on an 80-character line.
     */
    private void printText() {
        var spaces = (width - text.length()) / 2;
        pw.print(LINE_COMMENT);
        pw.print(SPACE);
        for (int i = 0; i < spaces - 2; i++) {
            pw.print(SPACE);
        }
        pw.println(text);
        pw.flush();
    }
}
