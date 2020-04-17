import static java.lang.String.format;
import static java.util.Collections.EMPTY_MAP;

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Map;

/**
 * Like the cat utility, only for entries in a zip file.
 */
class ZipCat {

    @SuppressWarnings("unchecked")
    private static final Map<String, String> LOAD_ENV = EMPTY_MAP;

    /**
     * Print the contents of an entry in a zip file to the screen.
     * 
     * <p>
     * This program takes 2 arguments:
     * 
     * <ul>
     * <li>path to the zip file.
     * <li>path to the entry in the zip file to print.
     * </ul>
     * 
     * If the path to the zip file is not absolute, then it is relative to the
     * current directory.
     * 
     * @param args the command line arguments.
     */
    public static void main(String... args) {

        if (args.length < 2) {
            System.err.printf("Usage: %s <zipfile> <zipfile entry>%n", ZipCat.class.getSimpleName());
            System.exit(1);
        }

        var fileuri = URI.create(format("jar:file:%s", args[0]));

        try (var fs = FileSystems.newFileSystem(fileuri, LOAD_ENV)) {
            var entryToCat = fs.getPath(args[1]);
            Files.lines(entryToCat).forEach(s -> System.out.println(s));
        } catch (Exception e) {
            System.err.printf("%s: error: %s%n", ZipCat.class.getSimpleName(), e.getMessage());
            System.exit(1);
        }
    }
}