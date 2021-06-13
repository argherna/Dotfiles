import static java.lang.System.Logger.Level.ERROR;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import java.io.File;
import java.io.PrintWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

@CmdLineArgsProcessor.Version(name = "CmdLineArgsProcessor", major = "1")
class CmdLineArgsProcessor implements Runnable {
    private static final ResourceBundle RB =
            ResourceBundle.getBundle("messages_cmdlineargsprocessor");

    private static final System.Logger LOGGER =
            System.getLogger(CmdLineArgsProcessor.class.getName(), RB);

    @Argument(name = "IN_FILE", resourceKey = "arg.inputfile", required = true, index = 0)
    private File inputfile;

    @Option(shortName = "o", longName = "offline", resourceKey = "opt.offline")
    private boolean offline;

    @Option(longName = "print-diagnostics", shortName = "X", resourceKey = "opt.print-diag")
    private boolean printDiagnostics;

    @Option(shortName = "S", longName = "session", resourceKey = "opt.session", argRequired = true,
            argName = "SESSION_NAME")
    private String sessionName;

    @Argument(name = "URL", resourceKey = "arg.url", required = true, index = 1)
    private URL url;

    @Argument(name = "SUIT", resourceKey = "arg.suit", index = 2)
    private Suit suit;

    @Argument(name = "REQUEST_ITEM", resourceKey = "arg.request.items", multipleSpecs = true)
    private String[] requestItems;

    @Option(longName = "script-arg", resourceKey = "opt.scriptarg", argRequired = true,
            argName = "ARG", multipleSpecs = true)
    private String[] scriptArgs;

    @Option(shortName = "f", longName = "optional-file", resourceKey = "opt.optfiles",
            argRequired = true, argName = "FILE", multipleSpecs = true)
    private File[] optfiles;

    @Option(shortName = "h", longName = "help", isHelp = true, resourceKey = "opt.help")
    private boolean help;

    @Option(shortName = "V", longName = "version", isVersion = true, resourceKey = "opt.version")
    private boolean version;

    @Option(shortName = "t", longName = "timeunit", resourceKey = "opt.timeunit",
            argRequired = true, argName = "TIMEUNIT")
    private TimeUnit timeunit;

    public static void main(String[] args) {
        int returncode = CommandLine.run(new CmdLineArgsProcessor(), RB, args);
        System.exit(returncode);
    }

    CmdLineArgsProcessor() {
    }

    @Override
    public void run() {
        System.out.println(CmdLineArgsProcessor.class.getName());

        System.out.println("  --< Options, no args >------------------------");
        System.out.println();
        System.out.println("    -X,--print-diagnostics = " + printDiagnostics);
        System.out.println("    -o,--offline           = " + offline);
        System.out.println();
        System.out.println();
        System.out.println("  --< Options with args >-----------------------");
        System.out.println();
        System.out.println("    -S,--session           = " + sessionName);
        if (nonNull(scriptArgs) && scriptArgs.length > 0) {
            System.out.println();
            System.out.println("    --script-arg           = " + Arrays.toString(scriptArgs));
        }
        if (nonNull(optfiles) && optfiles.length > 0) {
            System.out.println();
            System.out.println("    --optional-file        = " + Arrays.toString(optfiles));
        }
        if (nonNull(timeunit)) {
            System.out.println();
            System.out.println("    -t,--timeunit          = " + timeunit.toString());
        }
        System.out.println();
        System.out.println();

        System.out.println("  --< Arguments >-------------------------------");
        System.out.println();
        System.out.println("    IN_FILE                = " + inputfile.toString());
        System.out.println("    IN_FILE type           = " + inputfile.getClass().getName());
        System.out.println();
        System.out.println("    URL                    = " + url.toString());
        System.out.println("    URL type               = " + url.getClass().getName());
        System.out.println();
        if (nonNull(suit)) {
            System.out.println("    SUIT                   = " + suit.toString());
            System.out.println("    SUIT type              = " + suit.getClass().getName());
        }
        if (nonNull(requestItems) && requestItems.length > 0) {
            System.out.println();
            System.out.println("    REQUEST_ITEM           = " + Arrays.toString(requestItems));
            System.out.println("    REQUEST_ITEM type      = " + requestItems.getClass().getName());
        }
    }

    static enum Suit {
        HEARTS, SPADES, DIAMONDS, CLUBS;
    }

    @SubCommand(name = "run0", resourceKey = "subcommand.run0", options = {"h", "o", "S"},
            requiredArguments = "URL")
    void subCommand0() {
        System.out.printf(">>>> Hello from %s()%n",
                Thread.currentThread().getStackTrace()[1].getMethodName());
        System.out.println("  --< Options, no args >------------------------");
        System.out.println();
        System.out.println("    -X,--print-diagnostics = " + printDiagnostics);
        System.out.println("    -o,--offline           = " + offline);
        System.out.println();
        System.out.println();
        System.out.println("  --< Options with args >-----------------------");
        System.out.println();
        System.out.println("    -S,--session           = " + sessionName);
        if (nonNull(scriptArgs) && scriptArgs.length > 0) {
            System.out.println();
            System.out.println("    --script-arg           = " + Arrays.toString(scriptArgs));
        }
        if (nonNull(optfiles) && optfiles.length > 0) {
            System.out.println();
            System.out.println("    --optional-file        = " + Arrays.toString(optfiles));
        }
        if (nonNull(timeunit)) {
            System.out.println();
            System.out.println("    -t,--timeunit          = " + timeunit.toString());
        }
        System.out.println();
        System.out.println();

        System.out.println("  --< Arguments >-------------------------------");
        System.out.println();
        System.out.println("    URL                    = " + url.toString());
        System.out.println("    URL type               = " + url.getClass().getName());
        System.out.println();
        if (nonNull(suit)) {
            System.out.println("    SUIT                   = " + suit.toString());
            System.out.println("    SUIT type              = " + suit.getClass().getName());
        }
        if (nonNull(requestItems) && requestItems.length > 0) {
            System.out.println();
            System.out.println("    REQUEST_ITEM           = " + Arrays.toString(requestItems));
            System.out.println("    REQUEST_ITEM type      = " + requestItems.getClass().getName());
        }


    }

    @SubCommand(name = "run1", resourceKey = "subcommand.run1", options = {"h", "o", "S"},
            requiredArguments = "IN_FILE", arguments = "REQUEST_ITEM")
    void subCommand1() {
        System.err.printf(">>>> Hello from %s()%n",
                Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    static class VersionString implements Callable<String> {

        private final Object instance;

        private final boolean includePlatformData;

        VersionString(Object instance) {
            this(instance, false);
        }

        VersionString(Object instance, boolean includePlatformData) {
            this.instance = instance;
            this.includePlatformData = includePlatformData;
        }

        @Override
        public String call() throws Exception {
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
    }

    private static String versionString(Object instance, boolean includePlatformData) {
        var verstr = new VersionString(instance, includePlatformData);
        try {
            return verstr.call();
        } catch (Throwable e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else if (e instanceof Error) {
                throw (Error) e;
            } else {
                throw new RuntimeException(e);
            }
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

        private List<SubCommand> supportedSubCommands = new ArrayList<>();

        private Map<SubCommand, Method> supportedSubCommandMethodsBySubCommand = new HashMap<>();

        private Map<String, Field> supportedOptionsByShortName = new HashMap<>();

        private Map<String, Field> supportedOptionsByLongName = new HashMap<>();

        private Method subCommandMethod;

        /**
         * Construct a new CommandLine instance.
         */
        CommandLine(Object instance, ResourceBundle rb) {
            this.instance = instance;
            initializeSubCommands();
            initializeSupportedArgumentsAndOptions();
            this.help = new Help(instance.getClass().getName(), supportedArgsList,
                    supportedOptsList, supportedSubCommands);
            this.rb = rb;
        }

        private void initializeSubCommands() {
            var c = instance.getClass();
            var methods = c.getDeclaredMethods();

            for (var method : methods) {
                var subcommand = method.getAnnotation(SubCommand.class);
                if (nonNull(subcommand)) {
                    supportedSubCommandMethodsBySubCommand.put(subcommand, method);
                    supportedSubCommands.add(subcommand);
                }
            }
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
            var subCommandProcessed = supportedSubCommands.isEmpty();
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
                if (!subCommandProcessed && isSubCommand(currentArg)) {
                    var subCommand = getSubCommand(currentArg);
                    subCommandMethod = supportedSubCommandMethodsBySubCommand.get(subCommand);
                    subCommandProcessed = true;
                } else if (!allOptsProcessed && isNull(curFld)
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
                                    if (nonNull(subCommandMethod)) {
                                        SubCommand subCommand =
                                                subCommandMethod.getAnnotation(SubCommand.class);
                                        help.printHelp(
                                                "TODO: get name of subcommand to build resource key name for header",
                                                "TODO: get name of subcommand to build resource key name for footer",
                                                subCommand);
                                    } else {
                                        help.printHelp(rb.getString("cmdline.help.header"),
                                                rb.getString("cmdline.help.footer"));
                                    }
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
                        if (nonNull(subCommandMethod)) {
                            SubCommand subCommand =
                                    subCommandMethod.getAnnotation(SubCommand.class);
                            help.printHelp(
                                    "TODO: get name of subcommand to build resource key name for header",
                                    "TODO: get name of subcommand to build resource key name for footer",
                                    subCommand);
                        } else {
                            help.printHelp(rb.getString("cmdline.help.header"),
                                    rb.getString("cmdline.help.footer"));
                        }
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
                        // TODO: need to account for SubCommand required argument before index...
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

            if (!supportedSubCommands.isEmpty() && isNull(subCommandMethod)) {
                help.printMessageAndSyntax(
                        MessageFormat.format(rb.getString("cmdline.error.subcommand.required"),
                                instance.getClass().getName()));
                System.exit(1);
            }

            // Make sure no lingering @Argument array types need to be set.
            if (nonNull(argArray)) {
                setFieldValueOn(curFld, argArray);
            }

            // Make sure all required args are set.
            if (!argFields.isEmpty()) {
                SubCommand subcommand =
                        nonNull(subCommandMethod) ? subCommandMethod.getAnnotation(SubCommand.class)
                                : null;
                var reqdArgNames = Arrays.asList(subcommand.requiredArguments());
                for (Field f : argFields) {
                    var argument = f.getAnnotation(Argument.class);
                    if (nonNull(subcommand)) {
                        var argName = argument.name();
                        if (reqdArgNames.contains(argName)) {
                            // print an error message for the subcommand and show the subcommand
                            // syntax.
                            help.printMessageAndSyntax(MessageFormat.format(
                                    rb.getString("cmdline.error.subcommand.missing.arg"),
                                    instance.getClass().getName(), subcommand.name(), argName),
                                    subcommand);
                            System.exit(1);
                        }
                    } else if (argument.required()) {
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

        private boolean isSubCommand(String arg) {
            var isSubCommand = false;
            for (var subCommand : supportedSubCommands) {
                isSubCommand = arg.equals(subCommand.name());
                if (isSubCommand) {
                    break;
                }
            }
            return isSubCommand;
        }

        private SubCommand getSubCommand(String arg) {
            SubCommand subCommand = null;
            for (var sc : supportedSubCommands) {
                if (arg.equals(sc.name())) {
                    subCommand = sc;
                    break;
                }
            }
            return requireNonNull(subCommand,
                    MessageFormat.format(rb.getString("cmdline.error.missing.subcommand"),
                            instance.getClass().getName(), arg));
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
                @SuppressWarnings({"unchecked", "unused"})
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

        @SuppressWarnings("unchecked")
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

        private Method getSubcommandMethod() {
            return subCommandMethod;
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
                var subcommandMethod = cmdLine.getSubcommandMethod();
                if (nonNull(subcommandMethod)) {
                    subcommandMethod.invoke(instance);
                } else {
                    instance.run();
                }
                return 0;
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException
                    | NoSuchMethodException | RuntimeException e) {
                LOGGER.log(ERROR, rb, "cmdline.error.runtime.exception.msg",
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

        private final List<Argument> nonSubCommandArgs;

        private final List<Option> supportedOptions;

        private final List<Option> nonSubCommandOptions;

        private final List<SubCommand> supportedSubCommands;

        private final Map<String, Argument> argumentsByName;

        private final Map<String, Option> optionsByShortName;

        private final Map<String, Option> optionsByLongName;

        private final String command;

        private final int margin;

        private char[] hangingIndent;

        private int maxDefLen = 40;

        private PrintWriter pw;

        Help(String command, List<Argument> arguments, List<Option> options,
                List<SubCommand> subcommands) {
            this.command = command;
            this.supportedArgs = arguments;
            this.supportedOptions = options;
            this.supportedSubCommands = subcommands;
            this.margin = (int) (width - (.1 * width));
            this.argumentsByName = supportedArgs.stream().collect(toMap(a -> a.name(), a -> a));
            this.nonSubCommandArgs = filterNonSubCommandArguments();
            this.optionsByShortName =
                    supportedOptions.stream().filter(o -> !o.shortName().isEmpty())
                            .collect(toMap(o -> o.shortName(), o -> o));
            this.optionsByLongName = supportedOptions.stream().filter(o -> !o.longName().isEmpty())
                    .collect(toMap(o -> o.longName(), o -> o));
            this.nonSubCommandOptions = filterNonSubCommandOptions();
            pw = new PrintWriter(System.out, true);
        }

        void printMessage(String message) {
            pw.println(message);
        }

        void printSyntax() {
            var syn = new StringBuilder();
            fillUsageSyntax(syn);
            fillClusteredOptionsSyntax(syn, nonSubCommandOptions);
            var synpadding = SYN_PADDING.toCharArray();
            checkWidth(syn, synpadding);
            fillNonClusteredOptionsSyntax(syn, getNonClusteredOptions(nonSubCommandOptions),
                    synpadding);
            if (!supportedSubCommands.isEmpty()) {
                syn.append('{');
                for (Iterator<SubCommand> subCommands = supportedSubCommands.iterator(); subCommands
                        .hasNext();) {
                    var subCommand = subCommands.next();
                    syn.append(subCommand.name());
                    if (subCommands.hasNext()) {
                        syn.append(',');
                    }
                }
                syn.append("} ");
            }
            fillArgumentSyntax(syn, nonSubCommandArgs, synpadding);
            pw.println(syn.toString());
        }

        void printSyntax(SubCommand subCommand) {
            var syn = new StringBuilder();
            fillUsageSyntax(syn, subCommand);
            var subCommandOptions = getSubCommandOptions(subCommand.options());
            fillClusteredOptionsSyntax(syn, subCommandOptions);
            var synpadding = SYN_PADDING.toCharArray();
            checkWidth(syn, synpadding);
            fillNonClusteredOptionsSyntax(syn, getNonClusteredOptions(subCommandOptions),
                    synpadding);
            fillArgumentSyntax(syn,
                    getSubCommandArguments(subCommand.requiredArguments(), subCommand.arguments()),
                    synpadding);
            pw.println(syn.toString());
        }

        void printOptions(List<Option> options) {
            var optsyntax = new ArrayList<String>();
            for (var opt : options) {
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

            for (int i = 0; i < options.size(); i++) {
                var sb = new StringBuilder(SOL_PADDING);
                String optsyn = optsyntax.get(i);
                sb.append(optsyn);

                padBetweenNameAndDef(optsyn.length(), OPT_DESC_PADDING.length(), sb);

                var opt = options.get(i);
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

        void printSubCommands() {
            var subCommandNames = new ArrayList<String>();
            for (var subCommand : supportedSubCommands) {
                subCommandNames.add(subCommand.name());
            }

            for (int i = 0; i < supportedSubCommands.size(); i++) {
                var sb = new StringBuilder(SOL_PADDING);
                var subCommandName = subCommandNames.get(i);
                sb.append(subCommandName);

                padBetweenNameAndDef(subCommandName.length(), OPT_DESC_PADDING.length(), sb);

                var subCommand = supportedSubCommands.get(i);
                var subCommandDesc =
                        subCommand.description().length() > 0 ? subCommand.description()
                                : subCommand.resourceKey().length() > 0
                                        ? RB.getString(subCommand.resourceKey())
                                        : "";
                if (sb.length() + subCommandDesc.length() >= margin) {
                    appendWords(subCommandDesc.split(" "), getHangingIndent(), sb);
                } else {
                    sb.append(subCommandDesc);
                }
                pw.println(sb.toString());
            }
        }

        void printArguments(List<Argument> arguments) {
            var argnames = new ArrayList<String>();
            for (var arg : arguments) {
                argnames.add(arg.name());
            }
            for (int i = 0; i < arguments.size(); i++) {
                var sb = new StringBuilder(SOL_PADDING);
                var argname = argnames.get(i);
                sb.append(argname);

                padBetweenNameAndDef(argname.length(), OPT_DESC_PADDING.length(), sb);

                var arg = arguments.get(i);
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

        void printMessageAndSyntax(String msg, SubCommand subCommand) {
            pw.println(msg);
            printSyntax(subCommand);
        }

        void printHelp(String header, String footer) {
            printSyntax();
            if (nonNull(header) && !header.isEmpty() && !header.isBlank()) {
                pw.println();
                var hb = new StringBuilder();
                appendWords(header.split(" "), NO_PADDING, hb);
                pw.println(hb.toString());
            }
            if (!nonSubCommandOptions.isEmpty()) {
                pw.println();
                pw.println("Options:");
                printOptions(nonSubCommandOptions);
            }
            if (!supportedSubCommands.isEmpty()) {
                pw.println();
                pw.println("Subcommands:");
                printSubCommands();
            }
            if (!nonSubCommandArgs.isEmpty()) {
                pw.println();
                pw.println("Arguments:");
                printArguments(nonSubCommandArgs);
            }
            if (nonNull(footer) && !footer.isEmpty() && !footer.isBlank()) {
                pw.println();
                var fb = new StringBuilder();
                appendWords(footer.split(" "), NO_PADDING, fb);
                pw.println(fb.toString());
            }
        }

        void printHelp(String header, String footer, SubCommand subCommand) {
            printSyntax(subCommand);
            if (nonNull(header) && !header.isEmpty() && !header.isBlank()) {
                pw.println();
                var hb = new StringBuilder();
                appendWords(header.split(" "), NO_PADDING, hb);
                pw.println(hb.toString());
            }
            var subCommandOptions = getSubCommandOptions(subCommand.options());
            if (!subCommandOptions.isEmpty()) {
                pw.println();
                pw.println("Options:");
                printOptions(subCommandOptions);
            }
            var subCommandArguments =
                    getSubCommandArguments(subCommand.requiredArguments(), subCommand.arguments());
            if (!subCommandArguments.isEmpty()) {
                pw.println();
                pw.println("Arguments:");
                printArguments(subCommandArguments);
            }
            if (nonNull(footer) && !footer.isEmpty() && !footer.isBlank()) {
                pw.println();
                var fb = new StringBuilder();
                appendWords(footer.split(" "), NO_PADDING, fb);
                pw.println(fb.toString());
            }
        }

        private void fillUsageSyntax(StringBuilder syn) {
            fillUsageSyntax(syn, null);
        }

        private void fillUsageSyntax(StringBuilder syn, SubCommand subCommand) {
            syn.append("usage: ").append(command).append(" ");
            if (nonNull(subCommand)) {
                syn.append(subCommand.name()).append(" ");
            }
        }

        private void fillClusteredOptionsSyntax(StringBuilder syn, Collection<Option> options) {
            syn.append("[-").append(getClusteredOptions(options)).append("] ");
        }

        private void fillArgumentSyntax(StringBuilder syn, Collection<Argument> arguments,
                char[] synpadding) {
            for (var arg : arguments) {
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
        }

        private void fillNonClusteredOptionsSyntax(StringBuilder syn, Collection<Option> options,
                char[] synpadding) {
            for (var opt : options) {
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
        }

        private List<Option> getSubCommandOptions(String[] subCommandOptNames) {
            var subCommandOptions = new ArrayList<Option>();
            for (var optName : subCommandOptNames) {
                var opt = optionsByShortName.containsKey(optName) ? optionsByShortName.get(optName)
                        : optionsByLongName.get(optName);
                if (nonNull(opt)) {
                    subCommandOptions.add(opt);
                }
            }
            return subCommandOptions;
        }

        private List<Argument> getSubCommandArguments(String[] subCommandRequiredArgNames,
                String[] subCommandArgNames) {
            var subCommandArguments = new ArrayList<Argument>();
            for (var argName : subCommandRequiredArgNames) {
                var arg = argumentsByName.get(argName);
                if (nonNull(arg)) {
                    subCommandArguments.add(arg);
                }
            }
            for (var argName : subCommandArgNames) {
                var arg = argumentsByName.get(argName);
                if (nonNull(arg)) {
                    subCommandArguments.add(arg);
                }
            }
            return subCommandArguments;
        }

        /** @return clustered option string that are not associated with subcommands. */
        private String getClusteredOptions(Collection<Option> options) {
            return options.stream().filter(o -> !o.shortName().isEmpty() && !o.argRequired())
                    .map(o -> o.shortName()).sorted(new SortIgnoreCase()).collect(joining());
        }

        /** @return List of nonclusterable Options that are not associated with subcommands. */
        private List<Option> getNonClusteredOptions(Collection<Option> options) {
            return options.stream().filter(o -> o.shortName().isEmpty() || o.argRequired())
                    .collect(toList());
        }

        private List<Option> filterNonSubCommandOptions() {
            var supportedOptsCopy = new ArrayList<Option>(supportedOptions);
            Collections.copy(supportedOptsCopy, supportedOptions);
            for (var subCommand : supportedSubCommands) {
                var optsAry = subCommand.options();
                for (var optName : optsAry) {
                    var opt = optionsByShortName.containsKey(optName)
                            ? optionsByShortName.get(optName)
                            : optionsByLongName.get(optName);
                    if (nonNull(opt) && !opt.isHelp() && !opt.isVersion()) {
                        supportedOptsCopy.remove(opt);
                    }
                }
            }
            return supportedOptsCopy;
        }

        private List<Argument> filterNonSubCommandArguments() {
            var supportedArgsCopy = new ArrayList<Argument>(supportedArgs);
            Collections.copy(supportedArgsCopy, supportedArgs);
            for (var subCommand : supportedSubCommands) {
                var argsAry = subCommand.arguments();
                for (var argName : argsAry) {
                    var arg = argumentsByName.get(argName);
                    if (nonNull(arg)) {
                        supportedArgsCopy.remove(arg);
                    }
                }
                argsAry = subCommand.requiredArguments();
                for (var argName : argsAry) {
                    var arg = argumentsByName.get(argName);
                    if (nonNull(arg)) {
                        supportedArgsCopy.remove(arg);
                    }
                }
            }
            return supportedArgsCopy;
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

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    static @interface SubCommand {

        String name();

        String[] arguments() default {};

        String[] requiredArguments() default {};

        String[] options() default {};

        String resourceKey() default "";

        String description() default "";
    }
}
