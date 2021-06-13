import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Preferences tool.
 * 
 * <p>
 * Set the system property {@code pref.showtraces} to {@code true} to show stack traces from
 * exceptions.
 */
@Pref.Version(name = "Pref", major = "1")
class Pref implements Runnable {

    private static final Boolean SHOWTRACES =
            Boolean.getBoolean(String.format("%s.showtraces", Pref.class.getName().toLowerCase()));

    private static ResourceBundle RB = ResourceBundle.getBundle("messages_pref");

    private PrefCommand prefCommand = PrefCommand.ADD;

    public static void main(String... args) {
        new Pref().run();
    }

    @Override
    public void run() {
        try {
            switch (prefCommand) {
                case ADD:
                    doAdd();
                    break;
                case EXPORT:
                    doExport();
                    break;
                case IMPORT:
                    doImport();
                    break;
                case RM:
                    doRm();
                    break;
            }
        } catch (Exception e) {
            System.err.println(MessageFormat.format(RB.getString("error.general"),
                    getClass().getName(), e.getMessage()));
            if (SHOWTRACES) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    void doAdd() {
        throw new UnsupportedOperationException("ADD not implemented.");
    }
    
    void doExport() {
        throw new UnsupportedOperationException("EXPORT not implemented.");
    }
    
    void doImport() {
        throw new UnsupportedOperationException("IMPORT not implemented.");
    }
    
    void doRm() {
        throw new UnsupportedOperationException("RM not implemented.");
    }
    
    /**
     * Command to execute.
     */
    static enum PrefCommand {
        /** Add/update a Preference key-value pair. */
        ADD,
        /** Export/view a preference node as Xml. */
        EXPORT,
        /** Import preference nodes as Xml. */
        IMPORT,
        /** Remove/delete a Preference key or node. */
        RM;
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
        String major()

        default "0";

        /**
         * @return the minor component of the version.
         */
        String minor()

        default "0";

        /**
         * @return the patch component of the version.
         */
        String patch()

        default "0";

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
        boolean multipleSpecs()

        default false;

        /**
         * @return {@code true} if this Argument is required.
         */
        boolean required()

        default false;

        /**
         * @return text description of this Argument.
         */
        String description()

        default "";

        /**
         * @return key in a resource bundle that is a description of this Argument.
         */
        String resourceKey()

        default "";

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
