import java.io.PrintWriter;

public class PropertyBanner implements Runnable {

    private static final Character DASH = '-';

    private static final Character LINE_COMMENT = '#';

    private static final Character SPACE = ' ';

    private static final Integer DEFAULT_WIDTH = 80;

    private static final PrintWriter DEFAULT_PRINTWRITER = new PrintWriter(System.out);

    private final int width;

    private final PrintWriter pw;

    private final String text;

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

    private static void showUsageAndExit(String message, int code) {
        var usage = String.format("java %s \"text\"", PropertyBanner.class.getName());
        System.err.println(message);
        System.err.println(usage);
        System.exit(code);
    }

    public PropertyBanner(String text) {
        this.pw = DEFAULT_PRINTWRITER;
        this.text = text;
        this.width = DEFAULT_WIDTH;
    }

    @Override
    public void run() {
        printBorder();
        pw.println(LINE_COMMENT);
        printText();
        pw.println(LINE_COMMENT);
        printBorder();
    }

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
