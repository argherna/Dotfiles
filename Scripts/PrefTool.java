import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringJoiner;
import java.util.Vector;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * Tool used to view and manage Preferences.
 * 
 * <P>
 * This can be run as a GUI or from the command line. The GUI lacks
 * functionality to delete preferences nodes because for some reason calling
 * {@link Preferences#removeNode()} does not do anything but also does not
 * report any errors.
 * 
 * @see Preferences
 */
public class PrefTool implements ActionListener, ListSelectionListener, Runnable, TreeSelectionListener {

    private static final String RB_NAME = "preftool_resources";

    private static final ResourceBundle RB = ResourceBundle.getBundle(RB_NAME);

    /* Filter for Java preferences xml files, give options for file extensions. */
    private static final FileFilter PREFS_FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            } else if (f.getName().endsWith(".xml") || f.getName().endsWith(".prefs")
                    || f.getName().endsWith(".settings")) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public String getDescription() {
            return "*.xml,*.prefs,*.settings";
        }
    };

    /* The null file (NUL on Windows, /dev/null almost everywhere else). */
    private static final File DEV_NULL = System.getProperty("os.name").contains("Windows") ? new File("NUL")
            : new File("/dev/null");

    private static Preferences systemroot = Preferences.systemRoot();

    private static Preferences userroot = Preferences.userRoot();

    private DefaultTableModel pvtm;

    private DefaultTreeModel ntm;

    private JFrame f;

    private String nodeAddress = "";

    private DefaultMutableTreeNode currentNodeSelection = null;

    private PropertyChangeSupport pcs;

    private Vector<Object> currentTableSelection = null;

    private int currentTableRowSelected = -1;

    private String cliCommand;

    private List<String> arguments;

    private boolean exportSubTree = false;

    private boolean systemRoot = false;

    private InputStream in;

    private OutputStream out;

    /**
     * Construct a new instance of the command line tool.
     * 
     * @param cliCommand command to run.
     */
    public PrefTool(String cliCommand) {
        this.cliCommand = cliCommand;
    }

    /**
     * @return command to run.
     */
    public String getCliCommand() {
        return cliCommand;
    }

    /**
     * Add an argument to the command.
     * 
     * @param arg argument to add.
     */
    public void addArgument(String arg) {
        if (Objects.isNull(arguments)) {
            arguments = new ArrayList<>();
        }
        arguments.add(arg);
    }

    /**
     * Remove and return the argument at the given index or {@code null} if no
     * arguments are set.
     * 
     * @param idx the index.
     * @return the removed argument.
     */
    public String removeArgument(int idx) {
        if (Objects.isNull(arguments)) {
            arguments = new ArrayList<>();
            return null;
        }
        return arguments.remove(idx);
    }

    /**
     * @return all arguments or an empty list if none set.
     */
    public List<String> getArguments() {
        if (Objects.isNull(arguments)) {
            arguments = new ArrayList<>();
        }
        return arguments;
    }

    /**
     * @return {@code true} if using the Preferences system root.
     * 
     * @see Preferences#systemRoot()
     * @see Preferences#userRoot()
     */
    public boolean isSystemRoot() {
        return systemRoot;
    }

    /**
     * Set to {@code true} to use the system root. Default is {@code false}.
     * 
     * @param systemRoot {@code true} if using the system root.
     * 
     * @see Preferences#systemRoot()
     * @see Preferences#userRoot()
     */
    public void setSystemRoot(boolean systemRoot) {
        this.systemRoot = systemRoot;
    }

    /**
     * Sets the input stream for importing Preferences XML.
     * 
     * @param in input stream.
     * 
     * @see Preferences#importPreferences(InputStream)
     */
    public void setInputStream(InputStream in) {
        this.in = in;
    }

    /**
     * @return input stream for importing Preferences XML.
     */
    public InputStream getInputStream() {
        return in;
    }

    /**
     * Sets the output stream to use for exporting Preferences XML.
     * 
     * @param out the output stream.
     * 
     * @see Preferences#exportNode(OutputStream)
     * @see Preferences#exportSubtree(OutputStream)
     */
    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    /**
     * @return output stream to use for exporting Preferences XML.
     */
    public OutputStream getOutputStream() {
        return out;
    }

    /**
     * @return {@code true} if exporting node and subtree.
     */
    public boolean isExportSubTree() {
        return exportSubTree;
    }

    /**
     * Sets if export will export node and subtree.
     * 
     * @param exportSubTree {@code true} if exporting subtree.
     */
    public void setExportSubTree(boolean exportSubTree) {
        this.exportSubTree = exportSubTree;
    }

    /**
     * Runs the command line.
     * 
     * @throws RuntimeException if an exception is thrown during the run.
     */
    @Override
    public void run() {
        checkCommandAndArguments();
        try {
            if (cliCommand.equals("add")) {
                runAddCommand();
            } else if (cliCommand.equals("export")) {
                runExportCommand();
            } else if (cliCommand.equals("import")) {
                runImportCommand();
            } else if (cliCommand.equals("remove")) {
                runRemoveCommand();
            }
        } catch (BackingStoreException | IOException | InvalidPreferencesFormatException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks the arguments for validity related to the command being run.
     * 
     * @throws IllegalStateException    if the command is not set up correctly.
     * @throws IllegalArgumentException if an IllegalArgumentException is thrown.
     */
    private void checkCommandAndArguments() {
        if (Objects.isNull(cliCommand) || cliCommand.isEmpty() || cliCommand.isBlank()) {
            throw new IllegalStateException(RB.getString("pt.err.reqd_cmd"));
        }
        if (cliCommand.equals("add")) {
            checkAddCommand();
        } else if (cliCommand.equals("export")) {
            checkExportCommand();
        } else if (cliCommand.equals("import")) {
            checkImportCommand();
        } else if (cliCommand.equals("remove")) {
            checkRemoveCommand();
        }
    }

    /**
     * Verifies argument list is either 1 or 3 arguments long.
     * 
     * @throws IllegalStateException if the argument list is 0, 2, or greater than 3
     *                               arguments long.
     */
    private void checkAddCommand() {
        if (arguments.size() != 1 && arguments.size() != 3) {
            throw new IllegalStateException(RB.getString("pt.err_add.bad_args"));
        }
    }

    /**
     * Verifies argument list and output streem.
     * 
     * @throws IllegalStateException if no arguments are set or the output stream
     *                               was not set.
     */
    public void checkExportCommand() {
        if (arguments.isEmpty()) {
            throw new IllegalStateException(RB.getString("pt.err.no_node"));
        }
        if (Objects.isNull(out)) {
            throw new IllegalStateException(RB.getString("pt.err_exp.no_os"));
        }
    }

    /**
     * Verifies the input stream for the import command.
     * 
     * @throws IllegalStateException if the input stream was not set.
     */
    public void checkImportCommand() {
        if (Objects.isNull(in)) {
            throw new IllegalStateException(RB.getString("pt.err_no_is"));
        }
    }

    /**
     * Verifies argument list is either 1 or 3 arguments long.
     * 
     * @throws IllegalArgumentException if the argument list is not 1 or 3 arguments
     *                                  long.
     */
    public void checkRemoveCommand() {
        if (arguments.size() != 1 && arguments.size() != 2) {
            throw new IllegalArgumentException(RB.getString("pt.err_rm.bad_args"));
        }
    }

    /**
     * Runs the add command.
     * 
     * @throws BackingStoreException if a BackingStoreException is thrown.
     */
    private void runAddCommand() throws BackingStoreException {
        var proot = isSystemRoot() ? systemroot : userroot;
        if (!proot.nodeExists(arguments.get(0))) {
            addNode(proot, arguments.get(0));
        }
        if (arguments.size() == 3) {
            addKeyToNode(proot.node(arguments.get(0)), arguments.get(1), arguments.get(2));
        }
    }

    /**
     * Runs the export command.
     * 
     * @throws BackingStoreException if a BackingStoreException is thrown
     * @throws IOException           if an IOException is thrown.
     */
    private void runExportCommand() throws BackingStoreException, IOException {
        var proot = isSystemRoot() ? systemroot : userroot;
        if (!proot.nodeExists(arguments.get(0))) {
            throw new IllegalArgumentException(
                    MessageFormat.format(RB.getString("pt.err_fmt.node_dne"), arguments.get(0)));
        }
        if (exportSubTree) {
            exportPreferencesSubtree(proot.node(arguments.get(0)), out);
        } else {
            exportPreferencesNode(proot.node(arguments.get(0)), out);
        }
    }

    /**
     * Runs the import command.
     * 
     * @throws IOException                       if an IOException is thrown.
     * @throws InvalidPreferencesFormatException if an
     *                                           InvalidPreferencesFormatException
     *                                           is thrown.
     */
    private void runImportCommand() throws IOException, InvalidPreferencesFormatException {
        importPreferences(in);
    }

    /**
     * Runs the remove command.
     * 
     * @throws BackingStoreException if a BackingStoreException is thrown.
     */
    private void runRemoveCommand() throws BackingStoreException {
        var proot = isSystemRoot() ? systemroot : userroot;
        if (!proot.nodeExists(arguments.get(0))) {
            throw new IllegalArgumentException(
                    MessageFormat.format(RB.getString("pt.err_fmt.node_dne"), arguments.get(0)));
        }
        if (arguments.size() == 2) {
            deleteKeyFromNode(proot.node(arguments.get(0)), arguments.get(1));
        } else if (arguments.size() == 1) {
            proot.node(arguments.get(0)).removeNode();
        }
        proot.flush();
    }

    /**
     * Construct a new PrefTool GUI instance.
     * 
     * @throws BackingStoreException if a BackingStoreException is thrown.
     */
    public PrefTool() throws BackingStoreException {
        pcs = new PropertyChangeSupport(this);
        f = new JFrame(RB.getString("pt.fr.ttl"));

        var root = new DefaultMutableTreeNode("Preferences");
        fillTree(root);

        ntm = new DefaultTreeModel(root);
        var pnt = new JTree(ntm);
        pnt.addTreeSelectionListener(this);
        pnt.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        pnt.setName("preferencesNodeTree");

        var pnvsp = new JScrollPane(pnt);
        pnvsp.setMinimumSize(new Dimension(100, 50));
        pnvsp.setName("preferencesNodeView");
        pnvsp.setOpaque(true);

        var $5pxeb = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);

        var nal = new JLabel();
        nal.setBorder($5pxeb);
        nal.setHorizontalAlignment(SwingConstants.LEFT);
        nal.setName("nodeAddressLabel");

        pvtm = new DefaultTableModel() {
            /**
             * This implementation always returns {@code false}.
             */
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        pvtm.addColumn(RB.getString("pt.vth.nm"));
        pvtm.addColumn(RB.getString("pt.vth.typ"));
        pvtm.addColumn(RB.getString("pt.vth.val"));

        var pvt = new JTable(pvtm);
        pvt.getSelectionModel().addListSelectionListener(this);
        pvt.setFillsViewportHeight(true);
        pvt.setName("preferencesValuesTable");
        pvt.setShowGrid(false);

        var pvvsp = new JScrollPane(pvt);
        pvvsp.setMinimumSize(new Dimension(100, 50));
        pvvsp.setName("preferencesValuesView");

        var psp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        psp.setBottomComponent(pvvsp);
        psp.setDividerLocation(400);
        psp.setName("preferencesSplitPane");
        psp.setPreferredSize(new Dimension(1000, 600));
        psp.setTopComponent(pnvsp);

        var $5pxb = BorderFactory.createEmptyBorder(5, 5, 5, 5);

        var p0 = new JPanel();
        p0.setBorder($5pxb);
        p0.setSize(new Dimension(1000, 600));
        p0.setLayout(new BorderLayout());
        p0.setName("mainPanel");

        p0.add(nal, BorderLayout.PAGE_START);
        p0.add(psp, BorderLayout.CENTER);

        f.add(p0);

        var mnuItmImport = new JMenuItem();
        mnuItmImport.addActionListener(this);
        mnuItmImport.setName("mnuItmImport");
        mnuItmImport.setText(RB.getString("mnu_itm.import"));

        var mnuItmExportSubtree = new JMenuItem();
        mnuItmExportSubtree.addActionListener(this);
        mnuItmExportSubtree.setName("mnuItmExportSubtree");
        mnuItmExportSubtree.setText(RB.getString("mnu_itm.export_sbtr"));

        var mnuItmExportNode = new JMenuItem();
        mnuItmExportNode.addActionListener(this);
        mnuItmExportNode.setName("mnuItmExportNode");
        mnuItmExportNode.setText(RB.getString("node"));

        var mnuExport = new JMenu();
        mnuExport.add(mnuItmExportSubtree);
        mnuExport.add(mnuItmExportNode);
        mnuExport.setName("mnuExport");
        mnuExport.setText(RB.getString("mnu.export"));

        var mnuItmExit = new JMenuItem();
        mnuItmExit.addActionListener(this);
        mnuItmExit.setName("mnuItmExit");
        mnuItmExit.setText(RB.getString("mnu_itm.exit"));

        var mnuFile = new JMenu();
        mnuFile.setName("mnuFile");
        mnuFile.setText(RB.getString("mnu.file"));
        mnuFile.add(mnuItmImport);
        mnuFile.add(mnuExport);
        mnuFile.addSeparator();
        mnuFile.add(mnuItmExit);

        var mnuItmNewKey = new JMenuItem();
        mnuItmNewKey.addActionListener(this);
        mnuItmNewKey.setName("mnuItmNewKey");
        mnuItmNewKey.setText(RB.getString("key"));

        var mnuItmNewNode = new JMenuItem();
        mnuItmNewNode.addActionListener(this);
        mnuItmNewNode.setName("mnuItmNewNode");
        mnuItmNewNode.setText(RB.getString("node"));

        var mnuEditNew = new JMenu();
        mnuEditNew.add(mnuItmNewKey);
        mnuEditNew.add(mnuItmNewNode);
        mnuEditNew.setName("mnuEditNew");
        mnuEditNew.setText(RB.getString("mnu.new"));

        var mnuItmDeleteKey = new JMenuItem();
        mnuItmDeleteKey.addActionListener(this);
        mnuItmDeleteKey.setName("mnuItmDeleteKey");
        mnuItmDeleteKey.setText(RB.getString("mnu_itm.del_key"));

        var mnuItmEditValue = new JMenuItem();
        mnuItmEditValue.addActionListener(this);
        mnuItmEditValue.setName("mnuItmEditValue");
        mnuItmEditValue.setText(RB.getString("mnu_itm.edt_key"));

        var mnuEdit = new JMenu();
        mnuEdit.add(mnuEditNew);
        mnuEdit.add(mnuItmDeleteKey);
        mnuEdit.add(mnuItmEditValue);
        mnuEdit.setName("mnuEdit");
        mnuEdit.setText(RB.getString("mnu.edit"));

        var mnuItmRefreshTree = new JMenuItem();
        mnuItmRefreshTree.addActionListener(this);
        mnuItmRefreshTree.setName("mnuItmRefreshTree");
        mnuItmRefreshTree.setText(RB.getString("mnu_itm.ref_tree"));

        var mnuItmRefreshTable = new JMenuItem();
        mnuItmRefreshTable.addActionListener(this);
        mnuItmRefreshTable.setName("mnuItmRefreshTable");
        mnuItmRefreshTable.setText(RB.getString("mnu_itm.ref_tbl"));

        var mnuView = new JMenu();
        mnuView.add(mnuItmRefreshTree);
        mnuView.add(mnuItmRefreshTable);
        mnuView.setName("mnuView");
        mnuView.setText(RB.getString("mnu.view"));

        var mBar = new JMenuBar();
        mBar.setName("mnuBar");
        mBar.add(mnuFile);
        mBar.add(mnuEdit);
        mBar.add(mnuView);

        pcs.addPropertyChangeListener("nodeAddress", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                PrefTool.this.nodeAddress = evt.getNewValue().toString();
                nal.setText(PrefTool.this.nodeAddress);
            }
        });

        pcs.addPropertyChangeListener("currentNodeSelection", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                currentNodeSelection = (DefaultMutableTreeNode) evt.getNewValue();
                mnuExport.setEnabled(Objects.nonNull(currentNodeSelection));
                mnuItmNewKey.setEnabled(Objects.nonNull(currentNodeSelection));
                mnuItmNewNode.setEnabled(Objects.nonNull(currentNodeSelection));
            }
        });

        pcs.addPropertyChangeListener("currentTableSelection", new PropertyChangeListener() {
            @Override
            @SuppressWarnings("unchecked")
            public void propertyChange(PropertyChangeEvent evt) {
                currentTableSelection = (Vector<Object>) evt.getNewValue();
                mnuItmDeleteKey.setEnabled(Objects.nonNull(currentTableSelection));
                mnuItmEditValue.setEnabled(Objects.nonNull(currentTableSelection));
            }
        });

        f.setJMenuBar(mBar);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setName("prefTool");
        f.pack();
        resetValues();
    }

    /**
     * {@inheritDoc}
     * 
     * <P>
     * Executes application actions.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src instanceof Component) {
            Component c = (Component) src;
            if (c.getName().equals("mnuItmImport")) {
                importPreferencesFile();
            } else if (c.getName().equals("mnuItmExportSubtree")) {
                exportSubtree();
            } else if (c.getName().equals("mnuItmExportNode")) {
                exportNode();
            } else if (c.getName().equals("mnuItmExit")) {
                exitApplication();
            } else if (c.getName().equals("mnuItmNewKey")) {
                addKeyToNode();
            } else if (c.getName().equals("mnuItmDeleteKey")) {
                deleteKeyFromNode();
            } else if (c.getName().equals("mnuItmNewNode")) {
                addNode();
            } else if (c.getName().equals("mnuItmEditValue")) {
                editValue();
            } else if (c.getName().equals("mnuItmRefreshTree")) {
                refreshTree();
            } else if (c.getName().equals("mnuItmRefreshTable")) {
                refreshTable();
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <P>
     * If a Preferences or root (user or system) node is selected, its node address
     * is set, and if any values are on that node, they are filled in to the
     * preferences values table on the right. If a node has no preferences values,
     * the table is cleared.
     */
    @Override
    public void valueChanged(TreeSelectionEvent e) {
        var tp = e.getPath();
        setCurrentNodeSelection((DefaultMutableTreeNode) tp.getLastPathComponent());
        clearPreferencesValuesTable();
        if (tp.getPath().length < 2) {
            setNodeAddress(" ");
            return;
        } else {
            var na = toNodeAddress(tp);
            setNodeAddress(na);
            fillValuesTable(na);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <P>
     * If a table row is selected, then the data is set to the bound property
     * {@code currentTableSelection}. From there it can be edited or deleted.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void valueChanged(ListSelectionEvent e) {
        var rowIdx = e.getFirstIndex();
        var rows = pvtm.getDataVector();
        if (rowIdx > rows.size() || rows.size() == 0) {
            setCurrentTableSelection(null);
            setCurrentTableRowSelected(-1);
        } else {
            var rowData = rows.get(rowIdx);
            setCurrentTableSelection(rowData);
            setCurrentTableRowSelected(rowIdx);
        }
    }

    /**
     * Imports Preferences XML file.
     */
    private void importPreferencesFile() {
        var imf = getImportFile();
        if (imf != DEV_NULL) {
            try {
                importPreferences(new BufferedInputStream(new FileInputStream(imf)));
                resetValues();
                refreshTree();
            } catch (IOException | InvalidPreferencesFormatException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 
     * @return File to be imported.
     */
    private File getImportFile() {
        File f = DEV_NULL;
        JFileChooser fc = createFileChooser(RB.getString("pt.fc_ttl.imp"));
        int opt = fc.showOpenDialog(null);
        if (opt == JFileChooser.APPROVE_OPTION) {
            f = fc.getSelectedFile();
        }
        return f;
    }

    /**
     * Imports preferences from the input stream.
     * 
     * <P>
     * This method exists so it can be called from either the GUI or command line.
     * 
     * @param is the input stream.
     * @throws InvalidPreferencesFormatException if the xml coming in from the input
     *                                           stream is invalid.
     * @throws IOException                       if an IOException is thrown.
     * 
     * @see Preferences#importPreferences(java.io.InputStream)
     */
    private void importPreferences(InputStream is) throws IOException, InvalidPreferencesFormatException {
        Preferences.importPreferences(is);
    }

    /**
     * Exports Preferences subtree to Preferences XML file.
     */
    private void exportSubtree() {
        if (Objects.isNull(nodeAddress) || nodeAddress.isEmpty() || nodeAddress.isBlank()) {
            return;
        }
        var exf = getExportFile();
        if (exf != DEV_NULL) {
            try {
                exportPreferencesSubtree(getPreferencesFromNodeAddress(),
                        new BufferedOutputStream(new FileOutputStream(exf)));
            } catch (IOException | BackingStoreException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Exports Preferences node to Preferences XML file.
     */
    private void exportNode() {
        if (Objects.isNull(nodeAddress) || nodeAddress.isEmpty() || nodeAddress.isBlank()) {
            return;
        }
        var exf = getExportFile();
        if (exf != DEV_NULL) {
            try {
                exportPreferencesNode(getPreferencesFromNodeAddress(),
                        new BufferedOutputStream(new FileOutputStream(exf)));
            } catch (IOException | BackingStoreException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 
     * @return the xml File object.
     */
    private File getExportFile() {
        File f = DEV_NULL;
        JFileChooser fc = createFileChooser(RB.getString("pt.fc_ttl.exp"));
        int opt = fc.showSaveDialog(null);
        if (opt == JFileChooser.APPROVE_OPTION) {
            f = fc.getSelectedFile();
        }
        return f;
    }

    /**
     * Export the subtree under the given preferences node to the output stream.
     * 
     * <P>
     * This method exists so it can be called from either the GUI or command line.
     * 
     * @param node preferences node.
     * @param os   output stream.
     * @throws IOException           if an IOException is thrown.
     * @throws BackingStoreException if a BackingStoreException is thrown.
     * 
     * @see Preferences#exportSubtree(java.io.OutputStream)
     */
    private void exportPreferencesSubtree(Preferences node, OutputStream os) throws IOException, BackingStoreException {
        node.exportSubtree(os);
    }

    /**
     * Export the preferences node to the output stream.
     * 
     * <P>
     * This method exists so it can be called from either the GUI or command line.
     * 
     * @param node preferences node.
     * @param os   output stream.
     * @throws IOException           if an IOException is thrown.
     * @throws BackingStoreException if a BackingStoreException is thrown.
     * 
     * @see Preferences#exportNode(java.io.OutputStream)
     */
    private void exportPreferencesNode(Preferences node, OutputStream os) throws IOException, BackingStoreException {
        node.exportNode(os);
    }

    /**
     * Creates a file chooser for opening an xml file of Java preferences.
     * 
     * @param title dialog title.
     * @return file chooser.
     */
    private JFileChooser createFileChooser(String title) {
        JFileChooser fc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        fc.removeChoosableFileFilter(fc.getAcceptAllFileFilter());
        fc.setFileFilter(PREFS_FILE_FILTER);
        fc.setDialogTitle(title);
        return fc;
    }

    /**
     * Exits the application with an exit code of {@code 0}.
     */
    private void exitApplication() {
        f.setVisible(false);
        System.exit(0);
    }

    /**
     * Adds a preference key and value to the selected node.
     */
    private void addKeyToNode() {
        if (Objects.isNull(nodeAddress) || nodeAddress.isEmpty() || nodeAddress.isBlank()) {
            return;
        }
        var ui = new PreferencesKeyUI(f);
        ui.setTitle(RB.getString("pt.dlg_ttl.add_key_value"));
        ui.setKeyTextFieldEnabled(true);
        ui.showUI();
        if (ui.isInputCanceled()) {
            return;
        }
        addKeyToNode(getPreferencesFromNodeAddress(), ui.getKey(), ui.getValue());
        clearPreferencesValuesTable();
        fillValuesTable(nodeAddress);
    }

    /**
     * Update the value of the currently selected key.
     */
    private void editValue() {
        if (Objects.isNull(nodeAddress) || nodeAddress.isEmpty() || nodeAddress.isBlank()) {
            return;
        }

        if (Objects.isNull(currentTableSelection) || currentTableSelection.isEmpty()) {
            return;
        }

        var ui = new PreferencesKeyUI(f);
        ui.setTitle(RB.getString("pt.dlg_ttl.edit_value"));
        ui.setKey(currentTableSelection.get(0).toString());
        ui.setValue(currentTableSelection.get(2).toString());
        ui.setKeyTextFieldEnabled(false);
        ui.showUI();
        if (ui.isInputCanceled()) {
            return;
        }
        addKeyToNode(getPreferencesFromNodeAddress(), ui.getKey(), ui.getValue());
        clearPreferencesValuesTable();
        fillValuesTable(nodeAddress);
    }

    /**
     * Refreshes the tree view.
     */
    private void refreshTree() {
        try {
            userroot.flush();
            systemroot.flush();
            var root = (DefaultMutableTreeNode) ntm.getRoot();
            root.removeAllChildren();
            fillTree(root);
            ntm.reload();
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Refreshes the preferences keys, types, and values view.
     */
    private void refreshTable() {
        if (Objects.isNull(currentNodeSelection)) {
            return;
        }
        if (nodeAddress.isEmpty() || nodeAddress.isBlank()) {
            return;
        }
        if (currentTableRowSelected == -1) {
            return;
        }

        try {
            getPreferencesFromNodeAddress().flush();
            clearPreferencesValuesTable();
            fillValuesTable(nodeAddress);
        } catch (BackingStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Adds the key and value to the Preferences node.
     * 
     * <P>
     * This method exists so it can be called from either the GUI or command line.
     * 
     * @param node  the Preferences node.
     * @param key   the key.
     * @param value the value.
     * 
     * @see Preferences#put(String, String)
     */
    private void addKeyToNode(Preferences node, String key, String value) {
        node.put(key, value);
    }

    /**
     * Deletes a preference key from the selected node.
     */
    private void deleteKeyFromNode() {
        if (Objects.isNull(nodeAddress) || nodeAddress.isEmpty() || nodeAddress.isBlank()) {
            return;
        }
        if (Objects.isNull(currentTableSelection) || currentTableSelection.isEmpty()) {
            return;
        }
        if (currentTableRowSelected == -1) {
            return;
        }
        deleteKeyFromNode(getPreferencesFromNodeAddress(), currentTableSelection.get(0).toString());
        pvtm.removeRow(currentTableRowSelected);
    }

    /**
     * Removes a key from the Preferences node.
     * 
     * <P>
     * This method exists so it can be called from either the GUI or command line.
     * 
     * @param node the Preferences node.
     * @param key  the key.
     * 
     * @see Preferences#remove(String)
     */
    private void deleteKeyFromNode(Preferences node, String key) {
        node.remove(key);
    }

    /**
     * Adds a new Preferences node to the currently selected parent.
     */
    private void addNode() {
        if (Objects.isNull(nodeAddress) || nodeAddress.isEmpty() || nodeAddress.isBlank()) {
            return;
        }

        var path = JOptionPane.showInputDialog(f, RB.getString("pt.dlg_msg.new_node"),
                RB.getString("pt.dlg_ttl.new_node"), JOptionPane.PLAIN_MESSAGE);
        if (Objects.isNull(path) || path.isEmpty() || path.isBlank()) {
            return;
        }
        addNode(getPreferencesFromNodeAddress(), path);

        var pathComponents = path.split("\\/");
        var parent = currentNodeSelection;
        for (String pc : pathComponents) {
            var toAdd = new DefaultMutableTreeNode(pc);
            ntm.insertNodeInto(toAdd, parent, parent.getChildCount());
            parent = toAdd;
        }
    }

    /**
     * Adds a new node to the parent Preferences node.
     * 
     * <P>
     * This method exists so it can be called from either the GUI or command line.
     * 
     * @param parent parent Preferences node.
     * @param path   path to the child node.
     * 
     * @see Preferences#node(String)
     */
    private void addNode(Preferences parent, String path) {
        parent.node(path);
    }

    /**
     * User interface for manipulating Preferences keys.
     */
    private class PreferencesKeyUI {

        private static final int PREF_TEXT_COLUMNS = Preferences.MAX_KEY_LENGTH / 2;

        private static final int PREF_TEXT_ROWS = 4;

        private final Component parent;

        private final PlainDocument pkyDoc;

        private final PlainDocument pvlDoc;

        private final JComponent[] inputs;

        private boolean inputCanceled = false;

        private boolean keyTextFieldEnabled;

        private String title = "Add/Edit";

        private PropertyChangeSupport pcs;

        /**
         * Construct a new instance of PreferencesKeyUI.
         * 
         * @param parent parent component.
         */
        private PreferencesKeyUI(Component parent) {
            this.parent = parent;
            this.pcs = new PropertyChangeSupport(this);

            pkyDoc = new PlainDocument();
            pkyDoc.setDocumentFilter(new DocumentSizeFilter(Preferences.MAX_KEY_LENGTH));

            var pkyTf = new JTextField(PREF_TEXT_COLUMNS);
            pkyTf.setDocument(pkyDoc);
            pkyTf.setName("addEditDlgPreferenceKeyTextField");
            keyTextFieldEnabled = pkyTf.isEnabled();
            pcs.addPropertyChangeListener("keyTextFieldEnabled", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    boolean newValue = (boolean) evt.getNewValue();
                    keyTextFieldEnabled = newValue;
                    pkyTf.setEnabled(keyTextFieldEnabled);
                }
            });

            var pkyL = new JLabel();
            pkyL.setLabelFor(pkyTf);
            pkyL.setName("addEditDlgPreferenceKeyLabel");
            pkyL.setText(RB.getString("key"));

            pvlDoc = new PlainDocument();
            pvlDoc.setDocumentFilter(new DocumentSizeFilter(Preferences.MAX_VALUE_LENGTH));

            var pvlTa = new JTextArea(PREF_TEXT_ROWS, PREF_TEXT_COLUMNS);
            pvlTa.setDocument(pvlDoc);
            pvlTa.setName("addEditDlgPreferenceValueTextArea");

            var pvlL = new JLabel();
            pvlL.setLabelFor(pvlTa);
            pvlL.setName("addEditDlgPreferenceValueLabel");
            pvlL.setText(RB.getString("value"));

            inputs = new JComponent[] { pkyL, pkyTf, pvlL, pvlTa };
        }

        /**
         * Show the user interface.
         */
        private void showUI() {
            var result = JOptionPane.showConfirmDialog(parent, inputs, title, JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            inputCanceled = (result != JOptionPane.OK_OPTION);

            if (inputCanceled) {
                try {
                    pkyDoc.remove(0, pkyDoc.getLength());
                    pvlDoc.remove(0, pvlDoc.getLength());
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Enables or disables the key text field in the user interface.
         * 
         * <P>
         * The default value of this property is {@code true}. This is a Javabeans bound
         * property.
         * 
         * @param enabled if {@code true}, enable the key text field.
         */
        private void setKeyTextFieldEnabled(boolean enabled) {
            var oldValue = keyTextFieldEnabled;
            pcs.firePropertyChange("keyTextFieldEnabled", oldValue, enabled);
        }

        /**
         * Determine the user interface had the cancel button pushed.
         * 
         * @return {@code true} if cancel was pushed.
         */
        private boolean isInputCanceled() {
            return inputCanceled;
        }

        /**
         * 
         * @return the preference key.
         */
        private String getKey() {
            try {
                return pkyDoc.getText(0, pkyDoc.getLength());
            } catch (BadLocationException e) {
                return "";
            }
        }

        /**
         * Sets the key to display in the UI.
         * 
         * @param k the key.
         */
        private void setKey(String k) {
            try {
                pkyDoc.remove(0, pkyDoc.getLength());
                pkyDoc.insertString(0, k, null);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }

        /**
         * 
         * @return the value for the preference key.
         */
        private String getValue() {
            try {
                return pvlDoc.getText(0, pvlDoc.getLength());
            } catch (BadLocationException e) {
                return "";
            }
        }

        /**
         * Sets the value to display in the UI.
         * 
         * @param v the value.
         */
        private void setValue(String v) {
            try {
                pvlDoc.remove(0, pvlDoc.getLength());
                pvlDoc.insertString(0, v, null);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }

        /**
         * Sets the title for the dialog.
         * 
         * @param t the title.
         */
        private void setTitle(String t) {
            this.title = t;
        }
    }

    /**
     * DocumentFilter that limits the size of a Document.
     */
    private class DocumentSizeFilter extends DocumentFilter {

        private final int maxLength;

        /**
         * Constructs a DocumentSizeFilter with a maximum length.
         * 
         * @param maxLength the maximum length of a String allowed to be entered into
         *                  the underlying Document.
         */
        public DocumentSizeFilter(int maxLength) {
            if (maxLength < 0) {
                throw new IllegalArgumentException(
                        String.format("DocumentSizeFilter must have max length > 0, %d given", maxLength));
            }
            this.maxLength = maxLength;
        }

        /**
         * {@inheritDoc}
         * 
         * <P>
         * This implementation checks the current length of the document and if it is
         * greater than this instance's max length, beep.
         */
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {

            if (insertionTooLong(fb.getDocument(), string)) {
                Toolkit.getDefaultToolkit().beep();
            } else {
                super.insertString(fb, offset, string, attr);
            }
        }

        /**
         * {@inheritDoc}
         * 
         * <P>
         * This implementation checks the current length of the document and if it is
         * greater than this instance's max length, beep.
         */
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {

            if (replacementTooLong(fb.getDocument(), text, length)) {
                Toolkit.getDefaultToolkit().beep();
            } else {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        /**
         * Return {@code true} if the given text length plus the current Document
         * length exceeds this instance's max length.
         * 
         * @param document the Document
         * @param text     the String to append to the document.
         * @return {@code true} if the document length plus string length exceeds max
         *         length.
         */
        private boolean insertionTooLong(Document document, String text) {
            return document.getLength() + text.length() > maxLength;
        }

        /**
         * Return {@code true} if the length of the replacement text exceeds max
         * length.
         * 
         * @param document the Document
         * @param text     text to insert/replace
         * @param length   length of text deleted by the replacement
         * @return {@code true} if the length of the replacement text exceeds max
         *         length.
         */
        private boolean replacementTooLong(Document document, String text, int length) {
            if (Objects.isNull(text)) {
                text = "";
            }
            return document.getLength() + text.length() - length > maxLength;
        }
    }

    /**
     * Adds the User and System Preferences node names as TreeNodes to the given
     * root node.
     * 
     * @param root root node to add the user and system tree nodes to.
     * @throws BackingStoreException if a BackingStoreException is thrown.
     */
    private void fillTree(DefaultMutableTreeNode root) throws BackingStoreException {
        var user = new DefaultMutableTreeNode("User");
        addChildren(user, userroot);
        root.add(user);

        var system = new DefaultMutableTreeNode("System");
        addChildren(system, systemroot);
        root.add(system);
    }

    /**
     * Adds DefaultMutableTreeNodes representing the children of the given
     * Preferences node to the given parent.
     * 
     * <P>
     * This method is called recursively to account for child Preferences of the
     * given Preferences node.
     * 
     * @param parent parent DefaultMutableTreeNode to add children to.
     * @param node   Preferences node to add child data to.
     * @throws BackingStoreException if a BackingStoreException is thrown.
     */
    private void addChildren(DefaultMutableTreeNode parent, Preferences node) throws BackingStoreException {
        for (var cn : node.childrenNames()) {
            var ctn = new DefaultMutableTreeNode(cn);
            if (!parent.isNodeDescendant(ctn)) {
                parent.add(ctn);
                addChildren(ctn, node.node(cn));
            }
        }
    }

    /**
     * Resets the values of text fields, selections, and other components to their
     * default state.
     */
    private void resetValues() {
        setCurrentNodeSelection(null);
        setCurrentTableSelection(null);
        setCurrentTableRowSelected(-1);
        setNodeAddress(" ");
    }

    /**
     * Renders the TreePath as a node address.
     * 
     * <P>
     * The data in the TreePath is converted to a node address of the form
     * {@code root:/path/to/node}. The value of {@code root} is either {@code User}
     * or {@code System}.
     * 
     * <P>
     * If the TreePath is less than 2 nodes, the empty String is returned. If the
     * TreePath is at least 2 nodes, the {@code root:/} node address is returned.
     * Otherwise, the fully rendered node address is returned.
     * 
     * @param tp TreePath.
     * @return node address.
     */
    private String toNodeAddress(TreePath tp) {
        if (tp.getPathCount() < 2) {
            return "";
        } else if (tp.getPathCount() == 2) {
            return String.format("%s:/", tp.getPathComponent(1));
        } else {
            var na = new StringJoiner("/").add(String.format("%s:", tp.getPathComponent(1)));
            for (int i = 2; i < tp.getPathCount(); i++) {
                var tn = (DefaultMutableTreeNode) tp.getPathComponent(i);
                var nm = tn.getUserObject().toString();
                na.add(nm);
            }
            return na.toString();
        }
    }

    /**
     * Converts the nodeAddress to a Preferences node.
     * 
     * @return Preferences node.
     */
    private Preferences getPreferencesFromNodeAddress() {
        // I can control when this is called because this is private so no need to check
        // the node address for empty or blank.
        var root = nodeAddress.startsWith("User:") ? userroot : systemroot;
        return root.node(nodeAddress.split(":")[1]);
    }

    /**
     * Sets the address of the node in the node address label.
     * 
     * <P>
     * The default value of this property is a string containing a single space
     * character. This is a Javabeans bound property.
     * 
     * @param nodeAddress the node address.
     */
    private void setNodeAddress(String nodeAddress) {
        String oldValue = this.nodeAddress;
        pcs.firePropertyChange("nodeAddress", oldValue, nodeAddress);
    }

    /**
     * Sets the currently selected node in the tree.
     * 
     * <P>
     * The default value of this property is {@code null}. This is a Javabeans bound
     * property.
     * 
     * @param n the selected node.
     */
    private void setCurrentNodeSelection(DefaultMutableTreeNode n) {
        DefaultMutableTreeNode oldValue = this.currentNodeSelection;
        pcs.firePropertyChange("currentNodeSelection", oldValue, n);
    }

    /**
     * Sets the currently selected row in the table.
     * 
     * <P>
     * The default value of this property is {@code null}. This is a Javabeans bound
     * property.
     * 
     * @param selection the selected row in the table.
     */
    private void setCurrentTableSelection(Vector<Object> selection) {
        Vector<Object> oldValue = currentTableSelection;
        pcs.firePropertyChange("currentTableSelection", oldValue, selection);
    }

    /**
     * Sets the currently selected row index in the table.
     * 
     * @param row index of the selected row.
     */
    private void setCurrentTableRowSelected(int row) {
        currentTableRowSelected = row;
    }

    /**
     * Deletes everything from the values table.
     */
    private void clearPreferencesValuesTable() {
        pvtm.getDataVector().removeAllElements();
        pvtm.fireTableDataChanged();
        setCurrentTableRowSelected(-1);
    }

    /**
     * If the associated node address as any keys, fill the table with the keys,
     * values, and best guess on a type.
     * 
     * @param nodeAddress the node address
     */
    private void fillValuesTable(String nodeAddress) {
        var ary = nodeAddress.split(":");
        Preferences prefs = (ary[0].equals("User")) ? userroot.node(ary[1])
                : systemroot.node(ary[1]);
        try {
            if (prefs.keys().length > 0) {
                for (int i = 0; i < prefs.keys().length; i++) {
                    var k = prefs.keys()[i];
                    var v = prefs.get(k, "");
                    Class<?> kt = String.class;
                    // Empty values are misinterpreted, make sure the value we have is at least
                    // populated.
                    if (!v.isEmpty()) {
                        if (isBoolean(v)) {
                            kt = boolean.class;
                        } else if (isNumeric(v)) {
                            if (isInteger(v)) {
                                kt = int.class;
                            } else if (isLong(v)) {
                                kt = long.class;
                            } else if (isFloat(v)) {
                                kt = float.class;
                            } else if (isDouble(v)) {
                                kt = double.class;
                            }
                        } else if (isBase64(v)) {
                            kt = byte[].class;
                        }
                    }
                    pvtm.insertRow(i, new Object[] { k, kt.getName(), v });
                }
            }
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * Determine if the value is a boolean.
     * 
     * @param v the value.
     * @return {@code true} if the value is a boolean.
     */
    private boolean isBoolean(String v) {
        return v.equalsIgnoreCase("true") || v.equalsIgnoreCase("false") ? true : false;
    }

    /**
     * Determine if the value is numeric.
     * 
     * @param v the value.
     * @return {@code true} if the value is numeric.
     */
    private boolean isNumeric(String v) {
        try {
            NumberFormat.getInstance().parse(v);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Determine if the value is an integer.
     * 
     * @param v the value.
     * @return {@code true} if the value is an integer.
     */
    private boolean isInteger(String v) {
        try {
            Integer.parseInt(v);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Determine if the value is a long.
     * 
     * @param v the value.
     * @return {@code true} if the value is a long.
     */
    private boolean isLong(String v) {
        try {
            Long.parseLong(v);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Determine if the value is a float.
     * 
     * @param v the value.
     * @return {@code true} if the value is a float.
     */
    private boolean isFloat(String v) {
        try {
            return Float.isFinite(Float.parseFloat(v));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Determine if the value is a double.
     * 
     * @param v the value.
     * @return {@code true} if the value is a double.
     */
    private boolean isDouble(String v) {
        try {
            return Double.isFinite(Double.parseDouble(v));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Determine if the value is a base64 encoded string.
     * 
     * <P>
     * This approach is not perfect because of the number of false positives that
     * could be given. For example, the text {@code SHA1} which has 4 characters in
     * the base64 alphabet would be considered a base 64 encoded string.
     * 
     * <P>
     * I picked up the regular expression used to identify a Base 64 encoded string
     * from StackOverflow.com.
     * 
     * @param v the value.
     * @return {@code true} if the value is a base64 encoded string.
     * 
     * @see https://stackoverflow.com/a/8571649/37776
     */
    private boolean isBase64(String v) {
        var b64re = "^([A-Za-z0-9+\\/]{4})*([A-Za-z0-9+\\/]{3}=|[A-Za-z0-9+\\/]{2}==)?$";
        var p = Pattern.compile(b64re);
        var m = p.matcher(v);
        return m.find();
    }

    /**
     * Placeholder method that displays a dialog saying something isn't implemented
     * yet.
     * 
     * @param what what needs to be implemented
     */
    void todoImplementActionDialog(String what) {
        JOptionPane.showMessageDialog(f,
                MessageFormat.format(RB.getString("todo.msgdlg_fmt.implement"), what),
                RB.getString("todo.msgdlg_ttl.implement"), JOptionPane.WARNING_MESSAGE);
    }

    public void setVisible(boolean visible) {
        f.setVisible(visible);
    }

    public static void main(String[] args) {

        if (args.length == 0) {
            // Launch the GUI.
            try {
                UIManager.setLookAndFeel(
                        UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                    | UnsupportedLookAndFeelException e) {
                e.printStackTrace();
                System.exit(1);
            }
            try {
                PrefTool pt = new PrefTool();
                javax.swing.SwingUtilities.invokeLater(() -> pt.setVisible(true));
            } catch (BackingStoreException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), RB.getString("pt.err_dlg_ttl.startup"),
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        } else {
            // Launch the command line tool.
            if (args[0].equals("-h") || args[0].equals("--help")) {
                System.err.println(RB.getString("pt.err_usage.cmds"));
                System.exit(2);
            }

            var cliCommand = args[0].substring(1);
            Set<String> validCommands = Set.of("add", "export", "import", "remove");
            if (!validCommands.contains(cliCommand)) {
                System.err.println(MessageFormat.format(RB.getString("pt.err_fmt.unknown_cmd"), cliCommand));
                System.err.println(RB.getString("pt.err_usage.cmds"));
                System.exit(1);
            }
            if (args[1].equals("-h") || args[1].equals("--help")) {
                var rkey = String.format("pt.err_usage.%s", cliCommand);
                System.err.println(RB.getString(rkey));
                System.exit(2);
            }
            PrefTool pt = new PrefTool(cliCommand);
            for (int i = 1; i < args.length; i++) {
                var arg = args[i];
                if (arg.equals("-S")) {
                    pt.setSystemRoot(true);
                } else if (arg.equals("-t") && cliCommand.equals("export")) {
                    pt.setExportSubTree(true);
                } else {
                    pt.addArgument(arg);
                }
            }
            if (cliCommand.equals("export")) {
                if (pt.getArguments().size() == 2) {
                    try {
                        var fos = new FileOutputStream(pt.removeArgument(1));
                        pt.setOutputStream(fos);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    pt.setOutputStream(System.out);
                }
            } else if (cliCommand.equals("import")) {
                if (pt.getArguments().size() == 1) {
                    try {
                        var fis = new FileInputStream(pt.removeArgument(0));
                        pt.setInputStream(fis);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            pt.run();
        }
    }
}
