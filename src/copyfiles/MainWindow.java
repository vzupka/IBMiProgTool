package copyfiles;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Exception;
import com.ibm.as400.access.AS400JPing;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.Job;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.SocketProperties;
import com.ibm.as400.access.SystemValue;
import com.ibm.as400.access.OutputQueue;
import com.ibm.as400.access.PrinterFile;
import com.ibm.as400.access.ProgramCall;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DropMode;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.RowMapper;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * This is the main class of the application CopyFilesIBMi;
 *
 * Create a window containing file trees along with inner classes; Left side of
 * the window corresponds to the local host (PC); Right side is for IBM i
 * server.
 *
 * @author Vladimír Župka 2016
 */
public class MainWindow extends JFrame {

    // This value set to *true* displays Info messages.
    // This value set to *false* suppresses Info messages.
    // ==================================================
    final boolean ALLOW_INFO_MESSAGES = true;
    // ==================================================

    Properties sysProp;
    String operatingSystem;
    final String WINDOWS = "WINDOWS";
    final String UNIX = "UNIX";

    final Color DIM_BLUE = new Color(50, 60, 160);
    final Color DIM_RED = new Color(190, 60, 50);
    final Color DIM_PINK = new Color(170, 58, 128);
    static final Color BLUE_LIGHTER = Color.getHSBColor(0.60f, 0.15f, 0.998f);

    int mainWindowWidth = 1100;
    int mainWindowHeight = 850;

    int mainWindowX;
    int mainWindowY;

    int borderWidth = 10;

    GroupLayout globalPanelLayout;
    GroupLayout panelTopLayout;

    JPanel globalPanel;

    JMenuBar menuBar;
    JMenu helpMenu;
    JMenuItem helpMenuItemEN;
    JMenuItem helpMenuItemCZ;
    JMenuItem helpMenuItemRPGIII;
    JMenuItem helpMenuItemRPGIV;
    JMenuItem helpMenuItemCOBOL;
    JMenuItem helpMenuItemDDS;
    JMenuItem helpMenuItemColumns;

    JPanel panelTop;
    JPanel panelPathLeft;
    JPanel panelPathRight;

    JScrollPane scrollPaneLeft;
    JScrollPane scrollPaneRight;

    JPanel panelLeft;
    JPanel panelRight;

    JSplitPane splitPaneInner;
    JSplitPane splitPaneOuter;

    JTree leftTree;
    DefaultMutableTreeNode leftTopNode;
    DefaultTreeModel leftTreeModel;
    TreeSelectionModel leftTreeSelModel;
    TreeMap<String, Integer> leftTreeMap = new TreeMap<>();
    TreePath leftSelectedPath;
    TreePath targetTreePath;
    Integer leftRow;

    boolean nodes = true;
    boolean noNodes = false;

    JTree rightTree;
    
    DefaultMutableTreeNode rightTopNode;
    DefaultTreeModel rightTreeModel;
    TreeSelectionModel rightTreeSelModel;
    TreeMap<String, String> rightTreeMap = new TreeMap<>();
    TreePath rightSelectedPath;
    Integer rightRow;

    JTree copySourceTree;
    JTree dragSourceTree;

    JList<String> messageList;
    ArrayList<String> msgVector = new ArrayList<>();
    String row;
    MessageScrollPaneAdjustmentListenerMax messageScrollPaneAdjustmentListenerMax;

    DefaultMutableTreeNode leftNode;
    TransferHandler.TransferSupport leftInfo;


    static AS400 remoteServer;

    IFSFile ifsFile;

    
    DefaultMutableTreeNode rightNode;
    TransferHandler.TransferSupport rightInfo;

    DefaultMutableTreeNode targetNode;
    DefaultMutableTreeNode nodeLevel2;
    DefaultMutableTreeNode nodeLevel3;

    JLabel userNameLabel = new JLabel("User:");
    JTextField userNameTextField = new JTextField();

    public JTextField hostTextField = new JTextField();
    JButton serversButton = new JButton("Servers");
    
    JButton connectReconnectButton = new JButton("Connect/Reconnect");

    JLabel libraryPatternLabel = new JLabel("LIB:");
    JTextField libraryPatternTextField = new JTextField();

    JLabel filePatternLabel = new JLabel("FILE:");
    JTextField filePatternTextField = new JTextField();

    JLabel memberPatternLabel = new JLabel("MBR:");
    JTextField memberPatternTextField = new JTextField();

    ArrayList<String> charsets;
    String pcCharset;
    JComboBox<String> pcCharComboBox;
    JLabel pcCharsetLabel = new JLabel("PC charset:");
    String[] pcCharSetNames = {
        // DEFAULT
        "*DEFAULT",
        // UNICODE
        "UNICODE based:", "--------------", "UTF-8", "UTF-16",
        // ASCII
        "ASCII based:", "------------", "US-ASCII", "windows-1250", "windows-1251", "windows-1252",
        "windows-1253", "windows-1254", "windows-1255", "windows-1256", "windows-1257",
        "windows-1258", "windows-31j",
        //
        "ISO-8859-1", "ISO-8859-2", "ISO-8859-3", "ISO-8859-4", "ISO-8859-5", "ISO-8859-6",
        "ISO-8859-7", "ISO-8859-8", "ISO-8859-9", "ISO-8859-13", "ISO-8859-15", "ISO-2022-CN",
        "ISO-2022-JP", "ISO-2022-JP-2", "ISO-2022-KR", "JIS_X0201", "JIS_X0212-1990", "KOI8-R",
        "KOI8-U", "Shift_JIS", "TIS-620",
        //
        "IBM437", "IBM737", "IBM775", "IBM850", "IBM852", "IBM857", "IBM858", "IBM862", "IBM866",
        "IBM1046", "IBM874", "IBM-Thai",
        //
        "Big5", "Big5-HKSCS", "CESU-8", "EUC-JP", "EUC-KR", "GB18030", "GB2312", "GBK",
        //
        "x-Big5-HKSCS-2001", "x-Big5-Solaris", "x-COMPOUND_TEXT", "x-euc-jp-linux", "x-EUC-TW",
        "x-eucJP-Open",
        //
        "x-IBM1006", "x-IBM1025", "x-IBM1046", "x-IBM1097", "x-IBM1098", "x-IBM1112", "x-IBM1122",
        "x-IBM1123", "x-IBM1124", "x-IBM1364", "x-IBM1381", "x-IBM1383", "x-IBM300", "x-IBM33722",
        "x-IBM737", "x-IBM833", "x-IBM834", "x-IBM856", "x-IBM874", "x-IBM875", "x-IBM921",
        "x-IBM922", "x-IBM930", "x-IBM933", "x-IBM935", "x-IBM937", "x-IBM939", "x-IBM942",
        "x-IBM942C", "x-IBM943", "x-IBM943C", "x-IBM948", "x-IBM949", "x-IBM949C", " x-IBM950",
        "x-IBM964", "x-IBM970",
        //
        "x-ISCII91", "x-ISO-2022-CN-CNS", "x-ISO-2022-CN-GB", "x-iso-8859-11", "x-JIS0208",
        "x-JISAutoDetect", "x-Johab",
        //
        "x-MacArabic", "x-MacCentralEurope", "x-MacCroatian", "x-MacCyrillic", "x-MacDingbat",
        "x-MacGreek", "x-MacHebrew", "x-MacIceland", "x-MacRoman", "x-MacRomania", "x-MacSymbol",
        "x-MacThai", "x-MacTurkish", "x-MacUkraine",
        //
        "x-MS932_0213", "x-MS950-HKSCS", "x-MS950-HKSCS-XP", "x-mswin-936", "x-PCK", "x-SJIS_0213",
        //
        "x-UTF-16LE-BOM", "X-UTF-32BE-BOM", "X-UTF-32LE-BOM",
        //
        "x-windows-50220", "x-windows-50221", "x-windows-874", "x-windows-949", "x-windows-950",
        "x-windows-iso2022jp",
        // EBCDIC
        "EBCDIC based:", "-------------", "IBM037", "IBM273", "IBM277", "IBM278", "IBM280",
        "IBM284", "IBM285", "IBM290", "IBM297", "IBM300", "IBM420", "IBM424", "IBM437", "IBM500",
        "IBM775", "IBM850", "IBM852", "IBM855", "IBM857", "IBM860", "IBM861", "IBM862", "IBM863",
        "IBM864", "IBM865", "IBM866", "IBM868", "IBM869", "IBM870", "IBM871", "IBM918", "IBM1025",
        "IBM1026", "IBM01140", "IBM01141", "IBM01142", "IBM01143", "IBM01144", "IBM01145",
        "IBM01146", "IBM01147", "IBM01148", "IBM01149",};

    ArrayList<String> ccsids;
    String ibmCcsid;
    JComboBox<String> ibmCcsidComboBox;
    JLabel ibmCcsidLabel = new JLabel("IBM i CCSID:");
    String[] ibmCcsids = {
        // Default
        "*DEFAULT",
        // Universal
        "65535",
        // EBCDIC
        "EBCDIC:", "-------", "37", "256", "273", "277", "278", "280", "284", "285", "290", "297",
        "300", "420", "423", "424", "425", "500", "818", "833", "834", "835", "836", "837", "838",
        "870", "871", "875", "880", "905", "924", "1025", "1026", "1027", "1097", "1112", "1122",
        "1123", "1130", "1132", "1137", "1153", "1154", "1155", "1156", "1157", "1158", "1160",
        "1164", "1166", "1175", "4396", "4930", "4933", "5123", "5233", "8612", "9030", "12708",
        "13121", "13124", "28709", "62211", "62224", "62235", "62245",
        // ASCII
        "ASCII:", "------", "367", "437", "813", "819", "850", "851", "852", "855", "857", "858",
        "860", "861", "862", "863", "864", "865", "866", "868", "869", "874", "891", "987", "903",
        "904", "912", "814", "915", "916", "920", "921", "922", "923", "1008", "1009", "1010",
        "1011", "1012", "1013", "1014", "1015", "1016", "1017", "1018", "1019", "1025", "1026",
        "1089", "1098", "1124", "1125", "1126", "1129", "1131", "1133", "1026", "1250", "1251",
        "1252", "1253", "1254", "1255", "1256", "1257", "1258", "1275", "1280", "1281", "1282",
        "1283", "1364", "1371", "1377", "1388", "1399",
        // UNICODE
        "UNICODE:", "--------", "1200" /* UTF-16 */, "1208" /* UTF-8 */,
        "13488" /* UCS-2 */,};
 
    ArrayList<String> sourceTypes;
    String sourceType;
    JComboBox<String> sourceTypeComboBox;
    JLabel sourceTypeLabel = new JLabel("Source type:");
   
    String[] sourceFileTypes = {"C", "CBL", "CBLLE", "CLLE", "CLP", "CMD", "CPP",
         "DSPF", "LF", "MNU", "MNUCMD", "MNUDDS", "PF", "PLI", "PRTF", "REXX", "RPG", "RPGLE",
         "SQL", "SQLC", "SQLCPP", "SQLCBL", "SQLCBLLE", "SQLPLI", "SQLRPG", "SQLRPGLE", 
         "TBL", "TXT",};
    
    JLabel sourceRecordLengthLabel = new JLabel("Source line length:");
    JTextField sourceRecordLengthTextField = new JTextField();

    JLabel sourceRecordPrefixLabel = new JLabel("Complete source record:");
    JCheckBox sourceRecordPrefixCheckBox = new JCheckBox();

    JLabel overwriteOutputFileLabel = new JLabel("Overwrite data:");
    JCheckBox overwriteOutputFileCheckBox = new JCheckBox();

    JLabel disksLabel = new JLabel("Windows disks:");
    JComboBox<String> disksComboBox = new JComboBox<>();

    JLabel leftPathLabel = new JLabel("Local Path:");
    JComboBox<String> leftPathComboBox = new JComboBox<>();
    LeftPathActionListener leftPathActionListener = new LeftPathActionListener();

    JLabel rightPathLabel = new JLabel("Remote Path:");
    JComboBox<String> rightPathComboBox = new JComboBox<>();

    JScrollPane scrollMessagePane = new JScrollPane(messageList);

    Compile compile;
    static WrkSplFCall wrkSplFCall;

    // Menu items for PC
    JMenuItem findInPcFiles = new JMenuItem("Find in PC files");
    JMenuItem copyFromLeft = new JMenuItem("Copy");
    JMenuItem pasteToLeft = new JMenuItem("Paste");
    JMenuItem displayPcFile = new JMenuItem("Display");
    JMenuItem displayPcTypedFile = new JMenuItem("Display PC typed file");
    JMenuItem editPcFile = new JMenuItem("Edit");
    JMenuItem renamePcFile = new JMenuItem("Rename");
    JMenuItem insertSpooledFile = new JMenuItem("Insert spooled file");
    JMenuItem createPcDirectory = new JMenuItem("New PC directory");
    JMenuItem createPcFile = new JMenuItem("New PC file");
    JMenuItem movePcObjectToTrash = new JMenuItem("Move to trash");

    // Menu items for IBM i
    JMenuItem copyFromRight = new JMenuItem("Copy");
    JMenuItem pasteToRight = new JMenuItem("Paste");
    JMenuItem createSourcePhysicalFile = new JMenuItem("New source physical file");
    JMenuItem createSourceMember = new JMenuItem("New source member");
    JMenuItem createSaveFile = new JMenuItem("New save file");
    JMenuItem deleteSourceMember = new JMenuItem("Delete source member");
    JMenuItem deleteSourcePhysicalFile = new JMenuItem("Delete source physical file");
    JMenuItem changeCCSID = new JMenuItem("Change CCSID");
    JMenuItem clearSaveFile = new JMenuItem("Clear save file");
    JMenuItem deleteSaveFile = new JMenuItem("Delete save file");
    JMenuItem workWithSpooledFiles = new JMenuItem("Work with spooled files");
    JMenuItem displaySourceMember = new JMenuItem("Display");
    JMenuItem editSourceMember = new JMenuItem("Edit");
    JMenuItem findInSourceMembers = new JMenuItem("Find in source members");
    JMenuItem compileSourceMember = new JMenuItem("Compile source member");
    JMenuItem copyLibrary = new JMenuItem("Copy library");
    JMenuItem clearLibrary = new JMenuItem("Clear library");
    JMenuItem deleteLibrary = new JMenuItem("Delete library");
    JMenuItem createIfsDirectory = new JMenuItem("New IFS directory");
    JMenuItem createIfsFile = new JMenuItem("New IFS File");
    JMenuItem deleteIfsObject = new JMenuItem("Delete");
    JMenuItem displayIfsFile = new JMenuItem("Display");
    JMenuItem editIfsFile = new JMenuItem("Edit");
    JMenuItem findInIfsFiles = new JMenuItem("Find in IFS files");
    JMenuItem renameIfsFile = new JMenuItem("Rename");
    JMenuItem compileIfsFile = new JMenuItem("Compile IFS file");
    JMenuItem displayIfsTypedFile = new JMenuItem("Display IFS typed file");

    JPopupMenu leftTreePopupMenu = new JPopupMenu();
    JPopupMenu rightTreePopupMenu = new JPopupMenu();

    FileSystem fileSystem = FileSystems.getDefault();
    Iterable<Path> rootDirectories;
    String pcFileSep; // PC file separator ( / in unix, \ in Windows )
    String leftRoot;
    String firstLeftRootSymbol; // / in unix, C:\ in Windows
    String rightRoot;

    String leftPathString;
    String[] leftPathStrings;
    RowMapper leftRowMapper;

    String rightPathString;
    String[] rightPathStrings;
    String[] memberSourceTypes;
    String fileName;

    RowMapper rightRowMapper;

    String sourcePathString;
    String targetPathString;
    String clipboardPathString;
    String[] clipboardPathStrings;

    MouseListener leftTreeMouseListener;

    MouseListener rightTreeMouseListener;

    // Tree expansion listener for right tree
    TreeExpansionListener rightTreeExpansionListener = new RightTreeExpansionListener();


    // Current coordinates from mouse click
    static int currentX;
    static int currentY;

    // Constant for properties
    final String PROP_COMMENT = "Copy files between IBM i and PC, edit and compile.";

    Path paramfilesPath = Paths.get(System.getProperty("user.dir"), "paramfiles");
    Path workfilesPath = Paths.get(System.getProperty("user.dir"), "workfiles");
    Path logfilesPath = Paths.get(System.getProperty("user.dir"), "logfiles");

    Path parPath = Paths.get(System.getProperty("user.dir"), "paramfiles", "Parameters.txt");
    Path errPath = Paths.get(System.getProperty("user.dir"), "logfiles", "err.txt");
    Path outPath = Paths.get(System.getProperty("user.dir"), "logfiles", "out.txt");
    String encoding = System.getProperty("file.encoding", "UTF-8");
    Properties properties;
    BufferedWriter outfile;
    BufferedReader infile;

    OutputStream errStream;
    OutputStream outStream;
    BufferedOutputStream bufOutStream;
    
    /**
     * Constructor.
     */
    public MainWindow() {

        try {
            // If "workfiles" directory doesn't exist, create one
            if (!Files.exists(workfilesPath)) {
                Files.createDirectory(workfilesPath);
            }
            // If directory "paramfiles" does not exist, create one.
            if (!Files.exists(paramfilesPath)) {
                Files.createDirectory(paramfilesPath);
            }
            // If directory "logfiles" does not exist, create one.
            if (!Files.exists(logfilesPath)) {
                Files.createDirectory(logfilesPath);
            }
            
            // Redirect System.err, System.out to log files err.txt, out.txt in directory "logfiles"
            errStream = Files.newOutputStream(errPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            outStream = Files.newOutputStream(outPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        PrintStream errPrintStream = new PrintStream(errStream);
        PrintStream outPrintStream = new PrintStream(outStream);
        // PrintStream console = System.out;
        System.setErr(errPrintStream);
        System.setOut(outPrintStream);

        // Get list of root directories (important for Windows)
        rootDirectories = fileSystem.getRootDirectories();

        // Get or set application properties
        // ---------------------------------
        sysProp = System.getProperties();

        // Root symbols for local host are different in Windows and unix
        //
        if (sysProp.get("os.name").toString().contains("Windows")) {
            operatingSystem = WINDOWS;
            // Windows:
            // We take C:\ as the first root symbol
            firstLeftRootSymbol = "C:\\";
            pcFileSep = "\\"; // single \

            // Insert Windows disk names (A:\, B:\, ...) in the combo box
            for (Path diskPath : rootDirectories) {
                disksComboBox.addItem(diskPath.toString());
            }
            // Set C:\ as selected disk name
            disksComboBox.setSelectedItem("C:\\");
        } else {
            // Unix systems:
            operatingSystem = UNIX;
            firstLeftRootSymbol = "/";
            pcFileSep = "/";
        }
        // Menu bar in Mac operating system will be in the system menu bar
        if (sysProp.get("os.name").toString().toUpperCase().contains("MAC")) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }

        // Get values from application properties from Parameters.txt file
        properties = new Properties();

        // If the Parameters.txt file does not exist, create one
        // with default values
        try {
            if (Files.notExists(parPath)) {
                Files.createFile(parPath);
                properties.setProperty("AUTHOR", "© Vladimír Župka, 2017 ");
                properties.setProperty("HOST", "");
                properties.setProperty("USERNAME", "");
                properties.setProperty("IBM_CCSID", "*DEFAULT");
                properties.setProperty("SOURCE_TYPE", "RPGLE");
                properties.setProperty("PC_CHARSET", "*DEFAULT");
                properties.setProperty("SOURCE_RECORD_PREFIX", ""); // or "Y"
                properties.setProperty("OVERWRITE_FILE", ""); // or "Y"
                properties.setProperty("PRINT_DEBUG", ""); // or "Y"
                properties.setProperty("LIBRARY_PATTERN", "");
                properties.setProperty("FILE_PATTERN", "");
                properties.setProperty("MEMBER_PATTERN", "");
                properties.setProperty("SOURCE_RECORD_LENGTH", "100");
                properties.setProperty("LEFT_PATH", firstLeftRootSymbol);
                properties.setProperty("RIGHT_PATH", "/");
                properties.setProperty("MAIN_WINDOW_X", "40");
                properties.setProperty("MAIN_WINDOW_Y", "40");
                properties.setProperty("COMPILE_WINDOW_X", "240");
                properties.setProperty("COMPILE_WINDOW_Y", "40");
                properties.setProperty("EDITOR_FONT", "Monospaced");
                properties.setProperty("EDITOR_FONT_SIZE", "12");
                properties.setProperty("DISPLAY_FONT", "Monospaced");
                properties.setProperty("DISPLAY_FONT_SIZE", "12");
                properties.setProperty("CARET", "Short caret"); // or "Long caret"
                properties.setProperty("SELECTION_MODE", "Horizontal selection"); // or "Vertical selection"
                properties.setProperty("HIGHLIGHT_BLOCKS", "*NONE");
                properties.setProperty("MATCH_CASE", "CASE_INSENSITIVE"); // or "CASE_SENSITIVE"
                properties.setProperty("SOURCE_ATTRIBUTES", "SAVE_SOURCE_ATTRIBUTES"); // or "NO_SOURCE_ATTRIBUTES"

                // Create a new text file in directory "paramfiles"
                outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                properties.store(outfile, PROP_COMMENT);
                outfile.close();
            }

            // Get values from properties and set variables and text fields
            infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
            properties.load(infile);

            // Set color to button text
            connectReconnectButton.setForeground(DIM_BLUE);

            // Text field to specify IBM i host IP address or domain name
            hostTextField.setText(properties.getProperty("HOST"));
            hostTextField.setPreferredSize(new Dimension(325, 20));
            hostTextField.setMinimumSize(new Dimension(325, 20));
            hostTextField.setMaximumSize(new Dimension(325, 20));
            
            userNameTextField.setText(properties.getProperty("USERNAME"));
            userNameTextField.setPreferredSize(new Dimension(90, 20));
            userNameTextField.setMinimumSize(new Dimension(90, 20));
            userNameTextField.setMaximumSize(new Dimension(90, 20));

            disksComboBox.setPreferredSize(new Dimension(50, 20));
            disksComboBox.setMinimumSize(new Dimension(50, 20));
            disksComboBox.setMaximumSize(new Dimension(50, 20));

            ibmCcsid = properties.getProperty("IBM_CCSID");
            ccsids = new ArrayList<>();
            ccsids.addAll(Arrays.asList(ibmCcsids));
            ibmCcsidComboBox = new JComboBox(ccsids.toArray());
            ibmCcsidComboBox.setPreferredSize(new Dimension(100, 20));
            ibmCcsidComboBox.setMinimumSize(new Dimension(100, 20));
            ibmCcsidComboBox.setMaximumSize(new Dimension(100, 20));
            ibmCcsidComboBox.setEditable(true);
            ibmCcsidComboBox.setSelectedItem(ibmCcsid);
            
            sourceType = properties.getProperty("SOURCE_TYPE");
            sourceTypes = new ArrayList<>();
            sourceTypes.addAll(Arrays.asList(sourceFileTypes));
            sourceTypeComboBox = new JComboBox(sourceTypes.toArray());
            sourceTypeComboBox.setToolTipText("List of possible source types. For copying to PC or IFS.");
            sourceTypeComboBox.setPreferredSize(new Dimension(110, 20));
            sourceTypeComboBox.setMinimumSize(new Dimension(110, 20));
            sourceTypeComboBox.setMaximumSize(new Dimension(110, 20));
            sourceTypeComboBox.setEditable(true);
            sourceTypeComboBox.setSelectedItem(sourceType);
            
            // Create PC charset combo box
            pcCharset = properties.getProperty("PC_CHARSET");
            charsets = new ArrayList<>();
            charsets.addAll(Arrays.asList(pcCharSetNames));
            pcCharComboBox = new JComboBox(charsets.toArray());
            pcCharComboBox.setPreferredSize(new Dimension(180, 20));
            pcCharComboBox.setMinimumSize(new Dimension(180, 20));
            pcCharComboBox.setMaximumSize(new Dimension(180, 20));
            pcCharComboBox.setEditable(true);
            pcCharComboBox.setSelectedItem(pcCharset);

            sourceRecordPrefixCheckBox.setSelected(!properties.getProperty("SOURCE_RECORD_PREFIX").isEmpty());
            sourceRecordPrefixCheckBox.setToolTipText("Whether to prepend sequence number and date (12 characters).");

            overwriteOutputFileCheckBox.setSelected(!properties.getProperty("OVERWRITE_FILE").isEmpty());
            overwriteOutputFileCheckBox.setToolTipText("Whether to allow overwriting data in file.");

            libraryPatternTextField.setText(properties.getProperty("LIBRARY_PATTERN"));
            libraryPatternTextField.setPreferredSize(new Dimension(110, 20));
            libraryPatternTextField.setMinimumSize(new Dimension(110, 20));
            libraryPatternTextField.setMaximumSize(new Dimension(110, 20));
            libraryPatternTextField.setToolTipText("Library search pattern. Can use * and ? wild cards.");

            filePatternTextField.setText(properties.getProperty("FILE_PATTERN"));
            filePatternTextField.setPreferredSize(new Dimension(110, 20));
            filePatternTextField.setMinimumSize(new Dimension(110, 20));
            filePatternTextField.setMaximumSize(new Dimension(110, 20));
            filePatternTextField.setToolTipText("Source file search pattern. Can use * and ? wild cards.");

            memberPatternTextField.setText(properties.getProperty("MEMBER_PATTERN"));
            memberPatternTextField.setPreferredSize(new Dimension(110, 20));
            memberPatternTextField.setMinimumSize(new Dimension(110, 20));
            memberPatternTextField.setMaximumSize(new Dimension(110, 20));
            memberPatternTextField.setToolTipText("Member search pattern. Can use * and ? wild cards.");

            sourceRecordLengthTextField.setText(properties.getProperty("SOURCE_RECORD_LENGTH"));
            sourceRecordLengthTextField.setPreferredSize(new Dimension(60, 20));
            sourceRecordLengthTextField.setMinimumSize(new Dimension(60, 20));
            sourceRecordLengthTextField.setMaximumSize(new Dimension(60, 20));
            sourceRecordLengthTextField.setToolTipText("Length of text line in source member. When creating source physical file.");

            rightRoot = properties.getProperty("RIGHT_PATH");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }  // end of constructor

    /**
     * Create window containing trees with initial files in upper part; Left tree
     * shows local file system; Right tree shows IBM i file system (IFS).
     */
    public void createWindow() {
        // Correct path strings and update them in properties
        leftPathString = correctLeftPathString(properties.getProperty("LEFT_PATH"));
        rightPathString = correctRightPathString(properties.getProperty("RIGHT_PATH"));

        // Set window coordinates from application properties
        mainWindowX = Integer.parseInt(properties.getProperty("MAIN_WINDOW_X"));
        mainWindowY = Integer.parseInt(properties.getProperty("MAIN_WINDOW_Y"));

        globalPanel = new JPanel();
        globalPanelLayout = new GroupLayout(globalPanel);

        menuBar = new JMenuBar();
        helpMenu = new JMenu("Help");
        helpMenuItemEN = new JMenuItem("Help English");
        helpMenuItemCZ = new JMenuItem("Nápověda česky");
        helpMenuItemRPGIII = new JMenuItem("RPG III forms");
        helpMenuItemRPGIV = new JMenuItem("RPG IV forms");
        helpMenuItemCOBOL = new JMenuItem("COBOL form");
        helpMenuItemDDS = new JMenuItem("DDS forms");
        helpMenuItemColumns = new JMenuItem("Column numbering");

        helpMenu.add(helpMenuItemEN);
        helpMenu.add(helpMenuItemCZ);
        helpMenu.add(helpMenuItemRPGIII);
        helpMenu.add(helpMenuItemRPGIV);
        helpMenu.add(helpMenuItemCOBOL);
        helpMenu.add(helpMenuItemDDS);
        helpMenu.add(helpMenuItemColumns);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar); // In macOS on the main system menu bar above, in Windows on the window menu bar

        panelTop = new JPanel();

        panelTopLayout = new GroupLayout(panelTop);
        panelTop.setLayout(panelTopLayout);

        panelPathLeft = new JPanel();
        panelPathLeft.setLayout(new BoxLayout(panelPathLeft, BoxLayout.LINE_AXIS));

        scrollPaneLeft = new JScrollPane();
        scrollPaneLeft.setBorder(BorderFactory.createEmptyBorder());

        panelPathRight = new JPanel();
        panelPathRight.setLayout(new BoxLayout(panelPathRight, BoxLayout.LINE_AXIS));

        scrollPaneRight = new JScrollPane();
        scrollPaneRight.setBorder(BorderFactory.createEmptyBorder());

        // Windows: Disks combo box is included in order to choose proper root
        // directory (A:\, C:\, ...)
        Component diskLabelWin;
        Component disksComboBoxWin;
        if (operatingSystem.equals(WINDOWS)) {
            diskLabelWin = disksLabel;
            disksComboBoxWin = disksComboBox;
        } else { //
            // Unix (Mac): Empty component instead of combo box
            diskLabelWin = new JLabel("");
            disksComboBoxWin = new JLabel("");
        }
        disksComboBox.setToolTipText("List of root directories.");

        // Lay out components in panelTop
        panelTopLayout.setAutoCreateGaps(false);
        panelTopLayout.setAutoCreateContainerGaps(false);
        panelTopLayout.setHorizontalGroup(panelTopLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(panelTopLayout.createSequentialGroup()
                        .addComponent(serversButton)
                        .addComponent(hostTextField)
                        .addGap(5)
                        .addComponent(userNameLabel)
                        .addComponent(userNameTextField)
                        .addComponent(connectReconnectButton)
                        //.addComponent(printDebugCheckBox)
                        .addGap(5)
                        .addComponent(libraryPatternLabel)
                        .addComponent(libraryPatternTextField)
                        .addGap(5)
                        .addComponent(filePatternLabel)
                        .addComponent(filePatternTextField)
                        .addGap(5)
                        .addComponent(memberPatternLabel)
                        .addComponent(memberPatternTextField)
                        .addGap(5)
                        .addComponent(sourceTypeLabel)
                        .addComponent(sourceTypeComboBox)
                       
                )
                .addGroup(panelTopLayout.createSequentialGroup()
                        .addComponent(pcCharsetLabel)
                        .addComponent(pcCharComboBox)
                        .addComponent(ibmCcsidLabel)
                        .addComponent(ibmCcsidComboBox)
                        .addComponent(sourceRecordLengthLabel)
                        .addComponent(sourceRecordLengthTextField)
                        .addComponent(sourceRecordPrefixLabel)
                        .addComponent(sourceRecordPrefixCheckBox)
                        .addComponent(overwriteOutputFileLabel)
                        .addComponent(overwriteOutputFileCheckBox)
                        .addComponent(diskLabelWin)
                        .addComponent(disksComboBoxWin)));
        panelTopLayout.setVerticalGroup(panelTopLayout.createSequentialGroup()
                .addGroup(panelTopLayout.createParallelGroup(Alignment.CENTER)
                        .addComponent(serversButton)
                        .addComponent(hostTextField)
                        .addGap(5)
                        .addComponent(userNameLabel)
                        .addComponent(userNameTextField)
                        .addComponent(connectReconnectButton)
                        //.addComponent(printDebugCheckBox)
                        .addGap(5)
                        .addComponent(libraryPatternLabel)
                        .addComponent(libraryPatternTextField)
                        .addGap(5)
                        .addComponent(filePatternLabel)
                        .addComponent(filePatternTextField)
                        .addGap(5)
                        .addComponent(memberPatternLabel)
                        .addComponent(memberPatternTextField)
                        .addGap(5)                        
                        .addComponent(sourceTypeLabel)
                        .addComponent(sourceTypeComboBox)                       
                )
                .addGroup(panelTopLayout.createParallelGroup(Alignment.CENTER)
                        .addComponent(pcCharsetLabel)
                        .addComponent(pcCharComboBox)
                        .addComponent(ibmCcsidLabel)
                        .addComponent(ibmCcsidComboBox)
                        .addComponent(sourceRecordLengthLabel)
                        .addComponent(sourceRecordLengthTextField)
                        .addComponent(sourceRecordPrefixLabel)
                        .addComponent(sourceRecordPrefixCheckBox)
                        .addComponent(overwriteOutputFileLabel)
                        .addComponent(overwriteOutputFileCheckBox)
                        .addComponent(diskLabelWin)
                        .addComponent(disksComboBoxWin)));
        panelTop.setLayout(panelTopLayout);

        panelPathLeft.add(leftPathLabel);

        leftPathComboBox.setEditable(true);
        panelPathLeft.add(leftPathComboBox);

        panelPathRight.add(rightPathLabel);

        rightPathComboBox.setEditable(true);
        panelPathRight.add(rightPathComboBox);

        panelLeft = new JPanel();
        panelLeft.setLayout(new BorderLayout());
        panelLeft.add(panelPathLeft, BorderLayout.NORTH);
        panelLeft.add(scrollPaneLeft, BorderLayout.CENTER);

        panelRight = new JPanel();
        panelRight.setLayout(new BorderLayout());
        panelRight.add(panelPathRight, BorderLayout.NORTH);
        panelRight.add(scrollPaneRight, BorderLayout.CENTER);

        // Split pane inner - divided by horizontal divider
        splitPaneInner = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPaneInner.setLeftComponent(panelLeft);
        splitPaneInner.setRightComponent(panelRight);
        splitPaneInner.setDividerSize(6);
        splitPaneInner.setBorder(BorderFactory.createEmptyBorder());

        // Scroll pane for message list
        scrollMessagePane.setBorder(BorderFactory.createEmptyBorder());

        // Split pane outer - divided by vertical divider
        splitPaneOuter = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPaneOuter.setTopComponent(splitPaneInner);
        splitPaneOuter.setBottomComponent(scrollMessagePane);
        splitPaneOuter.setDividerSize(6);
        splitPaneOuter.setBorder(BorderFactory.createEmptyBorder());

        // This adjustment listener shows the scroll pane at the FIRST MESSAGE.
        messageScrollPaneAdjustmentListenerMax = new MessageScrollPaneAdjustmentListenerMax();

        // List of messages for placing into message scroll pane
        messageList = new JList();
        // Decision what color the message will get
        messageList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value.toString().startsWith("Comp")) {
                    this.setForeground(DIM_BLUE);
                } else if (value.toString().startsWith("Err")) {
                    this.setForeground(DIM_RED);
                } else if (value.toString().startsWith("Info")) {
                    this.setForeground(Color.GRAY);
                } else if (value.toString().startsWith("Wait")) {
                    this.setForeground(DIM_PINK);
                } else {
                    this.setForeground(Color.BLACK);
                }
                return component;
            }
        });

        // Build messageTable and put it to scrollMessagePane and panelMessages
        buildMessageList();

        // Create left tree showing local file system
        // ----------------
        leftRoot = properties.getProperty("LEFT_PATH");

        // ----------------------------------------------
        // Create new left side
        // ----------------------------------------------
        // Create split panes containing the PC file tree on the left side of the window
        createNewLeftSide(leftRoot);

        // Lay out the window components using GroupLayout
        // -----------------------------
        globalPanelLayout.setAutoCreateGaps(false);
        globalPanelLayout.setAutoCreateContainerGaps(false);

        globalPanelLayout.setHorizontalGroup(globalPanelLayout.createSequentialGroup().addGroup(globalPanelLayout
                .createParallelGroup().addComponent(panelTop).addComponent(splitPaneOuter)));
        globalPanelLayout.setVerticalGroup(globalPanelLayout.createParallelGroup().addGroup(globalPanelLayout
                .createSequentialGroup().addComponent(panelTop).addComponent(splitPaneOuter)));

        // Create a global panel to wrap the layout
        globalPanel.setLayout(globalPanelLayout);
        // Set border to the global panel - before it is visible
        globalPanel.setBorder(BorderFactory.createLineBorder(globalPanel.getBackground(), borderWidth));

        // When the split pane is visible - can divide it by percentage
        // Percentage to reveal the first message line height when the scroll pane is full
        splitPaneInner.setDividerLocation(.50d);   // 50 %
        splitPaneOuter.setDividerLocation(.835d);  // 83.5 %

        // Stabilize vertical divider always in the middle
        splitPaneInner.setResizeWeight(0.5);

        // Register WindowListener for storing X and Y coordinates to properties
        addWindowListener(new MainWindowAdapter());

        // Register HelpWindow menu item listener
        helpMenuItemEN.addActionListener(ae -> {
            String command = ae.getActionCommand();
            if (command.equals("Help English")) {
                if (Desktop.isDesktopSupported()) {
                    String uri = Paths
                            .get(System.getProperty("user.dir"), "helpfiles", "IBMiProgTool_doc_EN.pdf").toString();
                    // Replace backslashes by forward slashes in Windows
                    uri = "file://" + uri;
                    uri = uri.replace('\\', '/');
                    uri = uri.replace(" ", "%20");
                    System.out.println("uri: "+ uri);
                    try {
                        // Invoke the standard browser in the operating system
                        Desktop.getDesktop().browse(new URI(uri));
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                }
            }
        });
        // Register HelpWindow menu item listener
        helpMenuItemCZ.addActionListener(ae -> {
            String command = ae.getActionCommand();
            if (command.equals("Nápověda česky")) {
                if (Desktop.isDesktopSupported()) {
                    String uri = Paths
                            .get(System.getProperty("user.dir"), "helpfiles", "IBMiProgTool_doc_CZ.pdf").toString();
                    // Replace backslashes by forward slashes in Windows
                    uri = "file://" + uri;
                    uri = uri.replace('\\', '/');
                    uri = uri.replace(" ", "%20");
                    try {
                        // Invoke the standard browser in the operating system
                        Desktop.getDesktop().browse(new URI(uri));
                    } catch (IOException | URISyntaxException exc) {
                        exc.printStackTrace();
                    }
                }
            }
        });
        // Register HelpWindow menu item listener
        helpMenuItemRPGIII.addActionListener(ae -> {
            String command = ae.getActionCommand();
            if (command.equals("RPG III forms")) {
                if (Desktop.isDesktopSupported()) {
                    String uri = Paths
                            .get(System.getProperty("user.dir"), "helpfiles", "RPG_III_forms.pdf").toString();
                    // Replace backslashes by forward slashes in Windows
                     uri = "file://" + uri;
                    uri = uri.replace('\\', '/');
                    uri = uri.replace(" ", "%20");
                    try {
                        // Invoke the standard browser in the operating system
                        Desktop.getDesktop().browse(new URI(uri));
                    } catch (IOException | URISyntaxException exc) {
                        exc.printStackTrace();
                    }
                }
            }
        });
        // Register HelpWindow menu item listener
        helpMenuItemRPGIV.addActionListener(ae -> {
            String command = ae.getActionCommand();
            if (command.equals("RPG IV forms")) {
                if (Desktop.isDesktopSupported()) {
                    String uri = Paths
                            .get(System.getProperty("user.dir"), "helpfiles", "RPG_IV_forms.pdf").toString();
                    // Replace backslashes by forward slashes in Windows
                    uri = "file://" + uri;
                    uri = uri.replace('\\', '/');
                    uri = uri.replace(" ", "%20");
                    try {
                        // Invoke the standard browser in the operating system
                        Desktop.getDesktop().browse(new URI(uri));
                    } catch (IOException | URISyntaxException exc) {
                        exc.printStackTrace();
                    }
                }
            }
        });
        // Register HelpWindow menu item listener
        helpMenuItemCOBOL.addActionListener(ae -> {
            String command = ae.getActionCommand();
            if (command.equals("COBOL form")) {
                if (Desktop.isDesktopSupported()) {
                    String uri = Paths
                            .get(System.getProperty("user.dir"), "helpfiles", "COBOL_form.pdf").toString();
                    // Replace backslashes by forward slashes in Windows
                    uri = "file://" + uri;
                    uri = uri.replace('\\', '/');
                    uri = uri.replace(" ", "%20");
                    try {
                        // Invoke the standard browser in the operating system
                        Desktop.getDesktop().browse(new URI(uri));
                    } catch (IOException | URISyntaxException exc) {
                        exc.printStackTrace();
                    }
                }
            }
        });
        // Register HelpWindow menu item listener
        helpMenuItemDDS.addActionListener((var ae) -> {
            String command = ae.getActionCommand();
            if (command.equals("DDS forms")) {
                if (Desktop.isDesktopSupported()) {
                    String uri = Paths
                            .get(System.getProperty("user.dir"), "helpfiles", "DDS_forms.pdf").toString();
                    // Replace backslashes by forward slashes in Windows
                    uri = "file://" + uri;
                    uri = uri.replace('\\', '/');
                    uri = uri.replace(" ", "%20");
                    try {
                        // Invoke the standard browser in the operating system
                        Desktop.getDesktop().browse(new URI(uri));
                    } catch (IOException | URISyntaxException exc) {
                        exc.printStackTrace();
                    }
                }
            }
        });
        // Register HelpWindow menu item listener
        helpMenuItemColumns.addActionListener((var ae) -> {
            String command = ae.getActionCommand();
            if (command.equals("Column numbering")) {
                if (Desktop.isDesktopSupported()) {
                    String uri = Paths
                            .get(System.getProperty("user.dir"), "helpfiles", "Column_numbering.pdf").toString();
                    // Replace backslashes by forward slashes in Windows
                    uri = "file://" + uri;
                    uri = uri.replace('\\', '/');
                    uri = uri.replace(" ", "%20");
                    try {
                        // Invoke the standard browser in the operating system
                        Desktop.getDesktop().browse(new URI(uri));
                    } catch (IOException | URISyntaxException exc) {
                        exc.printStackTrace();
                    }
                }
            }
        });


        // Set left path string as selected in the left combo box
        leftPathComboBox.setSelectedItem(leftPathString);
        // Set also right path string in the right combo box
        rightPathComboBox.setSelectedItem(rightPathString);

        //
        // User name text field action
        // ---------------------------
        userNameTextField.addActionListener(ae -> {
            userNameTextField.setText(userNameTextField.getText().toUpperCase());
            // Create the updated text file in directory "paramfiles"
            try {
                infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
                properties.load(infile);
                infile.close();
                properties.setProperty("USERNAME", userNameTextField.getText());
                outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                properties.store(outfile, PROP_COMMENT);
                outfile.close();
                refreshWindow();
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        });
        //
        // Host text field action
        // ----------------------
        hostTextField.addActionListener(ae -> {
            hostTextField.setText(hostTextField.getText());
            // Connect or reconnect the server
            connectReconnectRefresh();
        });
        //
        // Server button action
        // --------------------
        serversButton.addActionListener(ae -> {
            new Servers(this);
        });

        //
        // Source Type combo box item listener
        // ---------------------
        sourceTypeComboBox.addItemListener(il -> {
            // JComboBox<String> source = new
            // JComboBox<String>((String[])il.getSource());
            JComboBox<String[]> source = (JComboBox) il.getSource();
            sourceType = (String) source.getSelectedItem();
            // Create the updated text file in directory "paramfiles"
            try {
                infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
                properties.load(infile);
                infile.close();
                properties.setProperty("SOURCE_TYPE", sourceType);
                outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                properties.store(outfile, PROP_COMMENT);
                outfile.close();
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        });
        
        //
        // Library pattern text field action
        // -------------------------
        libraryPatternTextField.addActionListener(ae -> {
            libraryPatternTextField.setText(libraryPatternTextField.getText().toUpperCase());
            // Create the updated text file in directory "paramfiles"
            try {
                infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
                properties.load(infile);
                infile.close();
                properties.setProperty("LIBRARY_PATTERN", libraryPatternTextField.getText());
                outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                properties.store(outfile, PROP_COMMENT);
                outfile.close();

                connectReconnectRefresh();

            } catch (IOException exc) {
                exc.printStackTrace();
            }
        });
        //
        // Source file pattern text field action
        // ------------------------------------
        filePatternTextField.addActionListener(ae -> {
            filePatternTextField.setText(filePatternTextField.getText().toUpperCase());
            // Create the updated text file in directory "paramfiles"
            try {
                infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
                properties.load(infile);
                infile.close();
                properties.setProperty("FILE_PATTERN", filePatternTextField.getText());
                outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                properties.store(outfile, PROP_COMMENT);
                outfile.close();

                connectReconnectRefresh();

            } catch (IOException exc) {
                exc.printStackTrace();
            }
        });
        //
        // Member pattern text field action
        // -------------------------------
        memberPatternTextField.addActionListener(ae -> {
            memberPatternTextField.setText(memberPatternTextField.getText().toUpperCase());
            // Create the updated text file in directory "paramfiles"
            try {
                infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
                properties.load(infile);
                infile.close();
                properties.setProperty("MEMBER_PATTERN", memberPatternTextField.getText());
                outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                properties.store(outfile, PROP_COMMENT);
                outfile.close();

                connectReconnectRefresh();

            } catch (IOException exc) {
                exc.printStackTrace();
            }
        });
        //
        // Source record length text field action
        // --------------------------------------
        sourceRecordLengthTextField.addActionListener(ae -> {
            String srcRecLen = sourceRecordLengthTextField.getText();
            try {
                Integer.valueOf(srcRecLen);
            } catch (NumberFormatException nfe) {
                // If the user enters non-integer text, take default value
                srcRecLen = "80";
            }
            sourceRecordLengthTextField.setText(srcRecLen);

            // Create the updated text file in directory "paramfiles"
            try {
                infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
                properties.load(infile);
                infile.close();
                outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                properties.setProperty("SOURCE_RECORD_LENGTH", srcRecLen);
                properties.store(outfile, PROP_COMMENT);
                outfile.close();
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        });
        //
        // Connect/Reconnect button action
        // -------------------------------
        connectReconnectButton.addActionListener(ae -> {
            connectReconnectRefresh();
        });


        /***
        //
        // ==============================????
        // Print debug info check box - Yes = "Y", No = ""
        // -----------------------------------------------
        printDebugCheckBox.addItemListener(il -> {
            Object source = il.getSource();
            if (source == printDebugCheckBox) {
                String check;
                if (printDebugCheckBox.isSelected()) {
                    check = "Y";
                    // Check connection and keep connection alive in background.
                    chkConn = new CheckConnection(remoteServer);
                    System.out.println("chkConn.execute()");
                    chkConn.execute();
                } else {
                    check = "";
                    // Stop checking connection.
                    //chkConn = new CheckConnection(remoteServer);
                    System.out.println("chkConn.cancel()");
                    if (chkConn != null) {
                        chkConn.cancel(true);
                        chkConn = null;
                    }
                }
                // Create the updated text file in directory "paramfiles"
                try {
                    infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
                    properties.load(infile);
                    infile.close();
                    outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                    properties.setProperty("PRINT_DEBUG", check);
                    properties.store(outfile, PROP_COMMENT);
                    outfile.close();
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        });
        // ==============================????
        ***/
        
        //
        // PC charset combo box
        // --------------------
        // Select charset name from the list in combo box - listener
        pcCharComboBox.addItemListener(il -> {
            JComboBox source = (JComboBox) il.getSource();
            pcCharset = (String) source.getSelectedItem();
            if (!pcCharset.equals("*DEFAULT")) {
                // Check if charset is valid
                try {
                    Charset.forName(pcCharset);
                } catch (IllegalCharsetNameException | UnsupportedCharsetException charset) {
                    // If pcCharset is invalid, take ISO-8859-1
                    pcCharset = "ISO-8859-1";
                    pcCharComboBox.setSelectedItem(pcCharset);
                }
            }
            // Create the updated text file in directory "paramfiles"
            try {
                infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
                properties.load(infile);
                infile.close();
                properties.setProperty("PC_CHARSET", pcCharset);
                outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                properties.store(outfile, PROP_COMMENT);
                outfile.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        });
        //
        // IBM i CCSID combo box item listener
        // ---------------------
        ibmCcsidComboBox.addItemListener(il -> {
            JComboBox source = (JComboBox) il.getSource();
            ibmCcsid = (String) source.getSelectedItem();
            if (!ibmCcsid.equals("*DEFAULT")) {
                try {
                    Integer.valueOf(ibmCcsid);
                } catch (NumberFormatException exc) {
                    exc.printStackTrace();
                    ibmCcsid = "500";
                    ibmCcsidComboBox.setSelectedItem(ibmCcsid);
                }
            }

            // Create the updated text file in directory "paramfiles"
            try {
                infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
                properties.load(infile);
                infile.close();
                outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                properties.setProperty("IBM_CCSID", ibmCcsid);
                properties.store(outfile, PROP_COMMENT);
                outfile.close();
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        });
        //
        // Source record pattern check box - Yes = "Y", No = ""
        // ---------------------------------------------------
        sourceRecordPrefixCheckBox.addItemListener(il -> {
            Object source = il.getSource();
            if (source == sourceRecordPrefixCheckBox) {
                String check;
                if (sourceRecordPrefixCheckBox.isSelected()) {
                    check = "Y";
                } else {
                    check = "";
                }
                // Create the updated text file in directory "paramfiles"
                try {
                    infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
                    properties.load(infile);
                    infile.close();
                    outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                    properties.setProperty("SOURCE_RECORD_PREFIX", check);
                    properties.store(outfile, PROP_COMMENT);
                    outfile.close();
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        });
        //
        // Overwrite output file(s) check box - Yes = "Y", No = ""
        // -------------------------------------------------------
        overwriteOutputFileCheckBox.addItemListener(il -> {
            Object source = il.getSource();
            if (source == overwriteOutputFileCheckBox) {
                String check;
                if (overwriteOutputFileCheckBox.isSelected()) {
                    check = "Y";
                } else {
                    check = "";
                }
                // Create the updated text file in directory "paramfiles"
                try {
                    infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
                    properties.load(infile);
                    infile.close();
                    outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                    properties.setProperty("OVERWRITE_FILE", check);
                    properties.store(outfile, PROP_COMMENT);
                    outfile.close();
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        });

        //
        // Left popup menu on Right mouse click
        // ====================================

        //
        // Find text in multiple PC files
        //
        findInPcFiles.addActionListener(ae -> {
            copySourceTree = leftTree;
            // Set clipboard path string for find operation
            clipboardPathStrings = leftPathStrings;
            new SearchWindow(remoteServer, this, "PC");  // no need for a variable
        });

        //
        // Send to remote server (IBM i)
        //
        copyFromLeft.addActionListener(ae -> {
            copySourceTree = leftTree;
            // Set clipboard path string for paste operation
            clipboardPathStrings = leftPathStrings;
        });


        //
        // Receive from remote server (IBM i) or PC itself
        //
        pasteToLeft.addActionListener(ae -> {
            if (copySourceTree == rightTree) {
                row = "Wait: Copying from IBM i to PC . . .";
                msgVector.add(row);
                showMessages();
      
                // Change transferred source type to .MBR for IFS transfer in all transferred members
                for (int idx = 0; idx < clipboardPathStrings.length; idx++) {
                    if (clipboardPathStrings[idx].contains(".FILE/") && !clipboardPathStrings[idx].endsWith("SAVF"))  {
                        clipboardPathStrings[idx] = clipboardPathStrings[idx].substring(0, 
                                clipboardPathStrings[idx].lastIndexOf(".")) + ".MBR";
                    }
                }
                
                // Paste from IBM i to PC
                ParallelCopy_IBMi_PC parallelCopy_IMBI_PC = new ParallelCopy_IBMi_PC(remoteServer, clipboardPathStrings, leftPathStrings[0], null, this);
                parallelCopy_IMBI_PC.execute();
            } 
            else if (copySourceTree == leftTree) {
                row = "Wait: Copying from PC to PC . . .";
                msgVector.add(row);
                showMessages(nodes);
                // Paste from PC to PC
                ParallelCopy_PC_PC parallelCopy_PC_PC = new ParallelCopy_PC_PC(clipboardPathStrings, leftPathStrings[0], null, this);
                parallelCopy_PC_PC.execute();
            }
        });

        // Insert spooled file that is copied from directory *workfiles* and, renamed,
        // into selected directory *targetPathString*
        insertSpooledFile.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            sourcePathString = Paths
                    .get(System.getProperty("user.dir"), "workfiles", "SpooledFile.txt").toString();
            targetPathString = leftPathStrings[0];
            copyAndRenameFile("SpooledFile.txt");
            reloadLeftSide(nodes);
        });

        // Display PC file
        displayPcFile.addActionListener(ae -> {
            clipboardPathStrings = leftPathStrings;
            // Display all selected files
            for (int idx = 0; idx < clipboardPathStrings.length; idx++) {
                sourcePathString = clipboardPathStrings[idx];
                JTextArea textArea = new JTextArea();
                // This is a way to display a PC file directly from Java:
                DisplayFile dspf = new DisplayFile(textArea, this);
                dspf.displayPcFile(sourcePathString);
            }
        });

        // Display PC typed file
        displayPcTypedFile.addActionListener(ae -> {
            clipboardPathStrings = leftPathStrings;
            // Display all selected files
            for (int idx = 0; idx < clipboardPathStrings.length; idx++) {
                sourcePathString = clipboardPathStrings[idx];
            if (Desktop.isDesktopSupported()) {
                String uriString = sourcePathString;
                uriString = uriString.substring(1);  // remove leading slash (/)
                uriString = firstLeftRootSymbol + uriString;  // prepend C:\ for Windows or / for UNIX
                // Replace backslashes by forward slashes in Windows
                uriString = "file://" + uriString;
                uriString = uriString.replace('\\', '/');
                uriString = uriString.replace(" ", "%20");
                ///System.out.println("URI: " + uriString);
                try {
                    URI uri = new URI(uriString); 
                    // Invoke the standard browser in the operating system
                    Desktop.getDesktop().browse(uri);
                } catch (IOException | URISyntaxException exc) {
                    exc.printStackTrace();
                    ///System.out.println("Error URI: " + exc.getLocalizedMessage());
                    row = "Error URI: " + exc.getLocalizedMessage();
                    msgVector.add(row);
                    showMessages(nodes);
                }
            }

            }
        });

        // Edit PC file
        editPcFile.addActionListener(ae -> {
            clipboardPathStrings = leftPathStrings;
            for (int idx = 0; idx < clipboardPathStrings.length; idx++) {
                sourcePathString = clipboardPathStrings[idx];
                JTextArea textArea = new JTextArea();
                JTextArea textArea2 = new JTextArea();
                EditFile edtf = new EditFile(remoteServer, this, textArea, textArea2, leftPathString, "rewritePcFile");
            }
        });

        // Rename PC file
        renamePcFile.addActionListener(ae -> {
            RenamePcObject rnmpf = new RenamePcObject(this, pcFileSep, currentX, currentY);
            rnmpf.renamePcObject(leftPathString);
        });

        // Create PC directory
        createPcDirectory.addActionListener(ae -> {
            clipboardPathStrings = leftPathStrings;
            if (clipboardPathStrings.length > 0) {
                leftPathString = clipboardPathStrings[0];
                CreateAndDeleteInPC cpcd = new CreateAndDeleteInPC(this, "createPcDirectory", currentX, currentY);
                cpcd.createAndDeleteInPC();
                reloadLeftSide(nodes);
            }
        });

        // Create PC file
        createPcFile.addActionListener(ae -> {
            CreateAndDeleteInPC cpcf = new CreateAndDeleteInPC(this, "createPcFile", currentX, currentY);
            cpcf.createAndDeleteInPC();
            reloadLeftSide(nodes);
        });

        // Move PC objects to trash
        movePcObjectToTrash.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            // Move selected objects to trash
            // ------------------------------
            // Set clipboard path strings for paste operation
            clipboardPathStrings = leftPathStrings;

            for (String clipboardPathString1 : clipboardPathStrings) {
                // Set path string for the following class
                leftPathString = clipboardPathString1;
                // Move one object to trash
                CreateAndDeleteInPC dpco = new CreateAndDeleteInPC(this, "movePcObjectToTrash", currentX, currentY);
                dpco.createAndDeleteInPC();
            }
            // Remove selected nodes
            TreePath[] paths = leftTree.getSelectionPaths();
            for (TreePath path : paths) {
                leftNode = (DefaultMutableTreeNode) (path.getLastPathComponent());
                leftTreeModel.removeNodeFromParent(leftNode);
            }
        });

        //
        // Right popup menu on Right mouse click
        // =====================================

        //
        // Find text in multiple IFS files
        //
        findInIfsFiles.addActionListener(ae -> {
            copySourceTree = rightTree;
            // Set clipboard path string for find operation
            clipboardPathStrings = rightPathStrings;
            new SearchWindow(remoteServer, this, "IFS");  // no need for a variable
        });

        // Send to PC or IBM i itself
        copyFromRight.addActionListener(ae -> {
            copySourceTree = rightTree;
            // Set clipboard path string for paste operation
            clipboardPathStrings = rightPathStrings;
        });

        // Receive from PC or IBM i itself
        // -------------------------------
        pasteToRight.addActionListener(ae -> {
            // This listener keeps the scroll pane at the LAST MESSAGE.
            // It is removed at the end of the method of the background task.
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            sourcePathString = clipboardPathString;
            targetPathString = rightPathStrings[0];
            if (copySourceTree == rightTree) {
                // Paste from IBM i to IBM i
                row = "Wait: Copying from IBM i to IBM i . . .";
                msgVector.add(row);
                showMessages();

                ParallelCopy_IBMi_IBMi parallelCopy_IMBI_IBMI = new ParallelCopy_IBMi_IBMi(remoteServer, clipboardPathStrings, targetPathString, null, this);
                parallelCopy_IMBI_IBMI.execute();

            } else if (copySourceTree == leftTree) {
                // Paste from PC to IBM i
                row = "Wait: Copying from PC to IBM i . . .";
                msgVector.add(row);
                showMessages();

                ParallelCopy_PC_IBMi parallelCopy_PC_IBMI = new ParallelCopy_PC_IBMi(remoteServer, clipboardPathStrings, targetPathString, null, this);
                parallelCopy_PC_IBMI.execute();
            }
        });

        // Copy library
        copyLibrary.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            ifsFile = new IFSFile(remoteServer, rightPathString);
            CreateAndDeleteInIBMi crtdlt = new CreateAndDeleteInIBMi(remoteServer, ifsFile, this, "copyLibrary", currentX, currentY);
            crtdlt.createAndDeleteInIBMi(currentX, currentY);
            reloadRightSide();
        });

        // Clear library
        clearLibrary.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);

            // Set clipboard path strings for paste operation
            clipboardPathStrings = rightPathStrings;

            for (int idx = 0; idx < clipboardPathStrings.length; idx++) {
                rightPathString = clipboardPathStrings[idx];
                ifsFile = new IFSFile(remoteServer, rightPathString);

                // Clear selected libraries
                // ------------------------
                CreateAndDeleteInIBMi crtdlt = new CreateAndDeleteInIBMi(remoteServer, ifsFile, this, "clearLibrary", currentX, currentY);
                crtdlt.createAndDeleteInIBMi(currentX, currentY);
            }
            // Reload nodes of cleared libraries
            TreePath[] paths = rightTree.getSelectionPaths();
            for (int indx = 0; indx < paths.length; indx++) {
                rightNode = (DefaultMutableTreeNode) (paths[indx].getLastPathComponent());
                reloadRightSide();
            }
        });

        // Delete library
        deleteLibrary.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);

            // Set clipboard path strings for paste operation
            clipboardPathStrings = rightPathStrings;

            for (int idx = 0; idx < clipboardPathStrings.length; idx++) {
                rightPathString = clipboardPathStrings[idx];
                ifsFile = new IFSFile(remoteServer, rightPathString);
                // Delete selected libraries
                // -------------------------
                CreateAndDeleteInIBMi crtdlt = new CreateAndDeleteInIBMi(remoteServer, ifsFile, this, "deleteLibrary", currentX, currentY);
                crtdlt.createAndDeleteInIBMi(currentX, currentY);
            }
            // Remove selected nodes
            TreePath[] paths = rightTree.getSelectionPaths();
            for (int indx = 0; indx < paths.length; indx++) {
                rightNode = (DefaultMutableTreeNode) (paths[indx].getLastPathComponent());
                rightTreeModel.removeNodeFromParent(rightNode);
            }
        });

        // Create IFS directory in a parent directory
        createIfsDirectory.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            ifsFile = new IFSFile(remoteServer, rightPathString);
            CreateAndDeleteInIBMi crtdlt = new CreateAndDeleteInIBMi(remoteServer, ifsFile, this, "createIfsDirectory", currentX, currentY);
            crtdlt.createAndDeleteInIBMi(currentX, currentY);
            reloadRightSide();
        });

        // Create IFS directory in a parent directory
        createIfsFile.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            ifsFile = new IFSFile(remoteServer, rightPathString);
            CreateAndDeleteInIBMi crtdlt = new CreateAndDeleteInIBMi(remoteServer, ifsFile, this, "createIfsFile", currentX, currentY);
            crtdlt.createAndDeleteInIBMi(currentX, currentY);
            reloadRightSide();
        });

        // Create AS400 Source Physical File
        createSourcePhysicalFile.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            ifsFile = new IFSFile(remoteServer, rightPathString);
            CreateAndDeleteInIBMi crtdlt = new CreateAndDeleteInIBMi(remoteServer, ifsFile, this, "createSourcePhysicalFile", currentX, currentY);
            crtdlt.createAndDeleteInIBMi(currentX, currentY);
            reloadRightSide();
        });

        // Create AS400 Source Member
        createSourceMember.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            ifsFile = new IFSFile(remoteServer, rightPathString);

            CreateSourceFileMember crtsfm = new CreateSourceFileMember(remoteServer, ifsFile, this, "Source Physical File", currentX, currentY);
            
            //CreateAndDeleteInIBMi crtdlt = new CreateAndDeleteInIBMi(remoteServer, ifsFile, this, "createSourceMember", currentX, currentY);
            //crtdlt.createAndDeleteInIBMi(currentX, currentY);

            reloadRightSide();
        });

        // Create Save File
        createSaveFile.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            ifsFile = new IFSFile(remoteServer, rightPathString);
            CreateAndDeleteInIBMi crtdlt = new CreateAndDeleteInIBMi(remoteServer, ifsFile, this, "createSaveFile", currentX, currentY);
            crtdlt.createAndDeleteInIBMi(currentX, currentY);
            reloadRightSide();
        });

        // Clear Save File
        clearSaveFile.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            ifsFile = new IFSFile(remoteServer, rightPathString);
            CreateAndDeleteInIBMi crtdlt = new CreateAndDeleteInIBMi(remoteServer, ifsFile, this, "clearSaveFile", currentX, currentY);
            crtdlt.createAndDeleteInIBMi(currentX, currentY);
        });

        // Delete IFS object (directory or file)
        deleteIfsObject.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);

            // Delete selected objects
            // -----------------------
            // Set clipboard path strings for paste operation
            clipboardPathStrings = rightPathStrings;

            for (int idx = 0; idx < clipboardPathStrings.length; idx++) {
                rightPathString = clipboardPathStrings[idx];
                ifsFile = new IFSFile(remoteServer, rightPathString);

                CreateAndDeleteInIBMi crtdlt = new CreateAndDeleteInIBMi(remoteServer, ifsFile, this, "deleteIfsObject", currentX, currentY);
                crtdlt.createAndDeleteInIBMi(currentX, currentY);
            }
            // Remove selected nodes
            TreePath[] paths = rightTree.getSelectionPaths();
            for (TreePath path : paths) {
                rightNode = (DefaultMutableTreeNode) (path.getLastPathComponent());
                rightTreeModel.removeNodeFromParent(rightNode);
            }
        });

        // Delete AS400 Source Member
        deleteSourceMember.addActionListener((ActionEvent ae) -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);

            // Set clipboard path strings for paste operation
            clipboardPathStrings = rightPathStrings;

            for (String clipboardPathString1 : clipboardPathStrings) {
                rightPathString = clipboardPathString1;
                ifsFile = new IFSFile(remoteServer, rightPathString);
                // Delete selected objects
                // -----------------------
                CreateAndDeleteInIBMi crtdlt = new CreateAndDeleteInIBMi(remoteServer, ifsFile, this, "deleteSourceMember", currentX, currentY);
                crtdlt.createAndDeleteInIBMi(currentX, currentY);
            }

            // Remove selected nodes
            TreePath[] paths = rightTree.getSelectionPaths();
            for (int indx = 0; indx < paths.length; indx++) {
                rightNode = (DefaultMutableTreeNode) (paths[indx].getLastPathComponent());
                rightTreeModel.removeNodeFromParent(rightNode);
            }
        });

        // Delete AS400 Source Physical File
        deleteSourcePhysicalFile.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);

            // Set clipboard path strings for paste operation
            clipboardPathStrings = rightPathStrings;

            for (String clipboardPathString1 : clipboardPathStrings) {
                rightPathString = clipboardPathString1;
                ifsFile = new IFSFile(remoteServer, rightPathString);
                // Delete selected objects
                // -----------------------
                CreateAndDeleteInIBMi crtdlt = new CreateAndDeleteInIBMi(remoteServer, ifsFile, this, "deleteSourcePhysicalFile", currentX, currentY);
                crtdlt.createAndDeleteInIBMi(currentX, currentY);
            }

            // Remove selected nodes
            TreePath[] paths = rightTree.getSelectionPaths();
            for (TreePath path : paths) {
                rightNode = (DefaultMutableTreeNode) (path.getLastPathComponent());
                rightTreeModel.removeNodeFromParent(rightNode);
            }
        });

        // Delete Save File
        deleteSaveFile.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);

            // Set clipboard path strings for paste operation
            clipboardPathStrings = rightPathStrings;

            for (String clipboardPathString1 : clipboardPathStrings) {
                rightPathString = clipboardPathString1;
                ifsFile = new IFSFile(remoteServer, rightPathString);
                // Delete selected objects
                // -----------------------
                CreateAndDeleteInIBMi crtdlt = new CreateAndDeleteInIBMi(remoteServer, ifsFile, this, "deleteSaveFile", currentX, currentY);
                crtdlt.createAndDeleteInIBMi(currentX, currentY);
            }

            // Remove selected nodes
            TreePath[] paths = rightTree.getSelectionPaths();
            for (int indx = 0; indx < paths.length; indx++) {
                rightNode = (DefaultMutableTreeNode) (paths[indx].getLastPathComponent());
                rightTreeModel.removeNodeFromParent(rightNode);
            }
        });

        // Work with spooled files
        workWithSpooledFiles.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            String className = this.getClass().getSimpleName();
            // Spool table window must be created only once
            if (wrkSplFCall == null) {
                // first "false" stands for *ALL users (not *CURRENT user), 
                // second "true" stands for "create spooled file table".
                wrkSplFCall = new WrkSplFCall(remoteServer, this, rightPathString,
                        false, // not *CURRENT user
                        currentX, currentY, className,
                        true // create spooled file table
                );
            }
            wrkSplFCall.execute();
        });

        // Display IFS file
        displayIfsFile.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            JTextArea textArea = new JTextArea();
            DisplayFile dspf = new DisplayFile(textArea, this);
            dspf.displayIfsFile(remoteServer, rightPathString);
        });

        // Edit IFS file with source types suffix (e.g. .CLLE, .RPGLE, etc.)
        editIfsFile.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            JTextArea textArea = new JTextArea();
            JTextArea textArea2 = new JTextArea();
            // Creation of the object is enough; Its constructor will do all that is needed. 
            new EditFile(remoteServer, this, textArea, textArea2, rightPathString, "rewriteIfsFile");
        });

        // Change CCSID
        changeCCSID.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            ChangeCCSID chgCcsid = new ChangeCCSID(remoteServer, this, currentX, currentY);
            chgCcsid.changeCCSID(rightPathString);
        });

        // Rename IFS file
        renameIfsFile.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            RenameIfsObject rnmifsf = new RenameIfsObject(remoteServer, this, currentX, currentY);
            rnmifsf.renameIfsObject(rightPathString);
        });

        // Compile IFS file
        compileIfsFile.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            if (compile == null) {
                compile = new Compile(remoteServer, this, rightPathString, true);
            }
            // "true" stands for "IFS file" as a source text
            compile.compile(rightPathString, true);
        });

        // Display IBM i Source Member
        displaySourceMember.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            JTextArea textArea = new JTextArea();
            DisplayFile dspf = new DisplayFile(textArea, this);
            dspf.displaySourceMember(remoteServer, rightPathString);
        });

        // Edit IBM i Source Member
        editSourceMember.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            JTextArea textArea = new JTextArea();
            JTextArea textArea2 = new JTextArea();
            String pathStringMbr = rightPathString.substring(0, rightPathString.lastIndexOf(".")) + ".MBR";
            new EditFile(remoteServer, this, textArea, textArea2, pathStringMbr, "rewriteSourceMember");
        });

        // Compile Source Member
        compileSourceMember.addActionListener(ae -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            if (compile == null) {
                compile = new Compile(remoteServer, this, rightPathString, false);
            }
            // "false" means "IFS file" is NOT a source text
            compile.compile(rightPathString, false);
        });

        //
        // Find text in multiple Source Members
        //
        findInSourceMembers.addActionListener(ae -> {
            copySourceTree = rightTree;
            // Set clipboard path string for find operation
            if (rightPathString.contains(".FILE/")) {
                sourceType = rightPathStrings[0].substring(rightPathStrings[0].lastIndexOf("."));
                clipboardPathStrings = rightPathStrings;
                new SearchWindow(remoteServer, this, "MBR");  // no need for a variable
            }
        });

        // Left path combo box listener
        // ----------------------------
        //
        // For Windows only:
        // =================
        if (operatingSystem.equals(WINDOWS)) {
            // Item listener for DISKS ComboBox reacts on item selection with
            // the
            // mouse
            disksComboBox.addItemListener(il -> {
                JComboBox<String> comboBox = (JComboBox) il.getSource();
                leftPathString = (String) comboBox.getSelectedItem();

                // Remove the old and create a new combo box for left path selection
                panelPathLeft.remove(leftPathComboBox);
                leftPathComboBox = new JComboBox<>();
                leftPathComboBox.setEditable(true);
                panelPathLeft.add(leftPathComboBox);

                leftPathComboBox.addItem(leftPathString);
                leftPathComboBox.setSelectedIndex(0);

                // Register a new ActionListener to the new combo box
                leftPathComboBox.addActionListener(leftPathActionListener);

                // Get the first item (disk symbol or file system root) from the
                // combo box and make it leftRoot
                leftRoot = leftPathString;
                // Make the disk symbol also firstLeftRootSymbol
                firstLeftRootSymbol = leftPathString;
                // Clear and set the tree map with leftRoot and row number 0
                leftTreeMap.clear();
                leftTreeMap.put(leftRoot, 0);
                // Create a new tree and table on the left side of the window
                createNewLeftSide(leftPathString);
            });
        } // End Windows

        // Processing continues for both Windows and Unix:
        //
        // Register action listener for LEFT PATH ComboBox reacts on text change
        // in its input field (first time)
        leftPathComboBox.addActionListener(leftPathActionListener);

        // Component listener reacting to WINDOW RESIZING
        ComponentListener resizingListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent componentEvent) {
                mainWindowWidth = componentEvent.getComponent().getWidth();
                mainWindowHeight = componentEvent.getComponent().getHeight();
                double splitPaneInnerDividerLoc = 0.50d; // 50 %
                splitPaneInner.setDividerLocation(splitPaneInnerDividerLoc);
            }
        };
        // Register window resizing listener
        addComponentListener(resizingListener);

        // Add the global panel to the frame, NOT container
        add(globalPanel);

        // Set initial size and width of the window (if not packed)
        setSize(mainWindowWidth, mainWindowHeight);

        // Set window coordinates from application properties
        setLocation(mainWindowX, mainWindowY);

        // Show the window on the screen
        setVisible(true);
        
        // pack the window (ignore size)
        pack(); 
    }

    /**
     * Getting a new connection to the remote server
     * @return 
     */
    protected boolean connectReconnect() {
        
        // Update properties from Library, File, Member input fields
        try {
            infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
            properties.load(infile);
            infile.close();
            properties.setProperty("LIBRARY_PATTERN", libraryPatternTextField.getText());
            properties.setProperty("FILE_PATTERN", filePatternTextField.getText());
            properties.setProperty("MEMBER_PATTERN", memberPatternTextField.getText());
            outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
            properties.store(outfile, PROP_COMMENT);
            outfile.close();
        } catch (IOException exc) {
            exc.printStackTrace();
        }

        // Keeps the scroll pane at the LAST MESSAGE.
        scrollMessagePane.getVerticalScrollBar().addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);

        /**
         * ===========================================================
         * ================ Connetion to the server ==================
         * ===========================================================
         */

        AS400JPing pingObj = new AS400JPing(hostTextField.getText());
        long timeoutMilliscconds = 8000;
        pingObj.setTimeout(timeoutMilliscconds);
        
        if (!pingObj.ping()) {
            row = "Error: Server " + hostTextField.getText() + " timed out "
                    + timeoutMilliscconds + " milliseconds.";
            msgVector.add(row);
            showMessages(noNodes);
            return false;
        }

        // Create AS400 object for IBM i SERVER.
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        remoteServer = new AS400(hostTextField.getText(), userNameTextField.getText() /*  , PASSWORD  */ );
        // The third parameter (password) should NOT be specified. The user must sign on.
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        // Define objects for printing for the AS400 object
        var qprint = new PrinterFile​(remoteServer, "/QSYS.LIB/QGPL.LIB/QPRINT.FILE");
        var qprintQueue = new OutputQueue(remoteServer, "/QSYS.LIB/QGPL.LIB/QPRINT.OUTQ");
        
        // Set socket different properties 
        SocketProperties socketProperties = new SocketProperties();
        socketProperties.setLoginTimeout(0); // Login timeout - no limit
        socketProperties.setSoTimeout(0);    // Receive timeout - no limit
        socketProperties.setKeepAlive(true); // Set keep alive
        remoteServer.setSocketProperties(socketProperties);
       
        try {
            // Connect FILE service in advance
            remoteServer.connectService(AS400.FILE);

            // Obtain and show he system value QCCSID
            SystemValue sysVal = new SystemValue(remoteServer, "QCCSID");
            row = "System value QCCSID is " + sysVal.getValue() + ".";
            msgVector.add(row);
            // Reload LEFT side. Do not use Right side! It would enter a loop.
            showMessages(noNodes);
            
            // Get server job (NUMBER, USER, NAME)
            ProgramCall pgm = new ProgramCall(remoteServer);
            pgm.setThreadSafe(true);  // Indicates that the program is to be run on-thread.

            Job serverJob = pgm.getServerJob(); 
            serverJob.getSubsystem(); 
            row = "Subsystem: " + serverJob.getSubsystem() + ", Job: " +serverJob + ".";
            msgVector.add(row);

            /***** Not applicable to the printed form of the job log 
               with the command DSPJOBLOG OUTPUT(*PRINT).
            serverJob.setLoggingLevel(4);
            serverJob.setLoggingSeverity(0);
            serverJob.setLoggingText(Job.LOGGING_TEXT_SECLVL);
            serverJob.setLoggingCLPrograms(Job.LOG_CL_PROGRAMS_YES);
            //serverJob.setInquiryMessageReply(Job.INQUIRY_MESSAGE_REPLY_SYSTEM_REPLY_LIST);
            serverJob.setPrinterDeviceName(Job.PRINTER_DEVICE_NAME_SYSTEM_VALUE);
            *****/
            
            // Reload LEFT side. Do not use Right side! It would enter a loop.
            showMessages(noNodes);

        } catch (AS400SecurityException | ErrorCompletingRequestException | 
                ObjectDoesNotExistException | RequestNotSupportedException | IOException | InterruptedException exc) {
            exc.printStackTrace();
            row = "Error: Connection to server  " + hostTextField.getText() + "  failed.  -  " + exc.toString();
            msgVector.add(row);
            showMessages(noNodes);
            // Change cursor to default
            setCursor(Cursor.getDefaultCursor());
            // Remove setting last element of messages
            scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(messageScrollPaneAdjustmentListenerMax);

//????            // Check connection and keep connection alive in background.
//????            chkConn = new CheckConnection(remoteServer);
//????            chkConn.execute();

            return true;
        }
        /**
         * ===================================================================
         * ================ End of connection to the server ==================
         * ===================================================================
         */

        // Show completion message when connection to IBM i server connected.
        row = "Comp: Server IBM i  " + hostTextField.getText() + "  has been connected to user  " + remoteServer.getUserId()
                + "  and is retrieving the Integrated File System.";
        msgVector.add(row);
        showMessages(noNodes);
        // Change cursor to default
        setCursor(Cursor.getDefaultCursor());
        // Remove setting last element of messages
        scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(messageScrollPaneAdjustmentListenerMax);

//????        // =================????
//????        if (printDebugCheckBox.isSelected()) {
//????            // Check connection and keep connection alive in background.
//????            chkConn = new CheckConnection(remoteServer);
//????            System.out.println("chkConn.execute()");
//????            chkConn.execute();
//????        } else {
//????            // Stop checking connection.
//????            System.out.println("chkConn.cancel()");
//????            if (chkConn != null) {
//????                chkConn.cancel(true);
//????                chkConn = null;
//????            }
//????        }
//????        // =================????

        return true;
    }

    /**
     *
     */
    protected void connectReconnectRefresh() {
        while (connectReconnect()) {
            // Search for MEMBERS without specifying a library or a file. This operation is long-lasting.
            // ----------------------------------------------------------
            if ((libraryPatternTextField.getText().isEmpty() || libraryPatternTextField.getText().equals("*"))
                    && (filePatternTextField.getText().isEmpty() || filePatternTextField.getText().equals("*"))
                    && (!memberPatternTextField.getText().isEmpty() && !memberPatternTextField.getText().equals("*"))) {
                int n = JOptionPane.showConfirmDialog(this, """
                                                                Searching for members in ALL libraries and ALL files  
                                                                takes longer time. 
                                                                Would you still like to continue?""",
                        "Warning!", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (n == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            refreshWindow();
            break;
        }
    }

    /**
     * Add child nodes to the LEFT side node along with the path to the PC
     * directory path given in parameters.
     *
     * @param pathParam
     * @param nodeParam
     */
    protected void addPCNodes(Path pathParam, DefaultMutableTreeNode nodeParam) {

        if (nodeParam == null) {
            return;
        }

        // Level 1
        // -------
        // Add child nodes to the current node of the tree
        // First, remove all children in order to create all new child nodes
        nodeParam.removeAllChildren();
        if (Files.isDirectory(pathParam)) {
            try ( // Get list of objects in the directory
                    Stream<Path> stream = Files.list(pathParam)
            // Process level 2 of objects (children of node level 1 - the root)
            ) {
                stream.sorted().forEach(pathLevel2 -> {
                    Path relativePathLevel2;
                    // Eliminate directories whose names begin with a dot
                    if (!pathLevel2.toString().contains(pcFileSep + ".")) {
                        if (pathLevel2.getParent() != null) {
                            relativePathLevel2 = pathLevel2.getParent().relativize(pathLevel2);
                        } else {
                            relativePathLevel2 = pathLevel2;
                        }
                        nodeLevel2 = new DefaultMutableTreeNode(relativePathLevel2);
                        nodeParam.add(nodeLevel2);

                        // Level 2
                        // -------
                        nodeLevel2.removeAllChildren();
                        if (Files.isDirectory(pathLevel2)) {
                            // Process each node level 2 if it is a directory. Resulting in nodes level 3
                            try ( // Get list of objects in the directory level 2
                                    Stream<Path> stream2 = Files.list(pathLevel2)) {
                                // Process each node level 2 if it is a directory. Resulting in nodes level 3
                                stream2.sorted().forEach(pathLevel3 -> {
                                    Path relativePathLevel3;
                                    // Eliminate directories whose names begin with a dot
                                    if (!pathLevel3.toString().contains(pcFileSep + ".")) {
                                        if (pathLevel3.getParent() != null) {
                                            relativePathLevel3 = pathLevel3.getParent().relativize(pathLevel3);
                                        } else {
                                            relativePathLevel3 = pathLevel3;
                                        }
                                        nodeLevel3 = new DefaultMutableTreeNode(relativePathLevel3);
                                        nodeLevel2.add(nodeLevel3);

                                        //// No other levels are necessary
                                        //// -----------------------------
                                        /*
                                        if (false) {  // never performed. for experiment!!
                                        // Level 3
                                        nodeLevel3.removeAllChildren();
                                        if (Files.isDirectory(pathLevel3)) {
                                        try {
                                        // Get list of objects in the
                                        // directory level 3
                                        Stream<Path> stream3 = Files.list(pathLevel3);
                                        // Process each node level 3 if it is a directory. Resulting in nodes level 4
                                        stream3.sorted().forEach(pathLevel4 -> {
                                        Path relativePathLevel4;
                                        // Eliminate directories whose names begin with a dot
                                        if (!pathLevel4.toString().contains(pcFileSep + ".")) {
                                        if (pathLevel4.getParent() != null) {
                                        relativePathLevel4 = pathLevel4.getParent().relativize(pathLevel4);
                                        } else {
                                        relativePathLevel4 = pathLevel4;
                                        }
                                        DefaultMutableTreeNode nodeLevel4 = new DefaultMutableTreeNode(relativePathLevel4);
                                        nodeLevel3.add(nodeLevel4);
                                        } // Level 3 - Dotted file // names - "/." or "\."
                                        });
                                        stream3.close();
                                        } catch (Exception exc) {
                                        exc.printStackTrace();
                                        row = "Info:  " + exc.toString();
                                        msgVector.add(row);
                                        // no nodes - important not to recurse
                                        showMessages(noNodes);
                                        }
                                        } // End Level 3
                                        }
                                        //// ----------------
                                        //// End other levels
                                        */
                                    } // Level 2 - Dotted file names - "/." or // "\."
                                });

                            } catch (Exception exc) {
                                exc.printStackTrace();
                                row = "Info:  " + exc.toString();
                                msgVector.add(row);
                                // no nodes - important not to recurse
                                showMessages(noNodes);
                            }
                        } // End Level 2

                    } // Level 1 - Dotted file names - "/." or "\."
                });

            } catch (IOException exc) {
                exc.printStackTrace();
                row = "Error:  " + exc.toString();
                msgVector.add(row);
                showMessages(noNodes); // no nodes - important not to recurse
            }
        } // End Level 1
    }

    /**
     * Create split panes containing the PC file tree and table on the left side
     * of the window.
     *
     * @param leftRoot
     */
    protected void createNewLeftSide(String leftRoot) {

        // Create new left hand tree
        leftPathString = leftRoot;
        leftTopNode = new DefaultMutableTreeNode(leftPathString);
        leftTreeMap.put(leftRoot, 0);

        // Create left hand tree
        leftTree = new JTree(leftTopNode);

        // Create tree model
        leftTreeModel = (DefaultTreeModel) leftTree.getModel();

        // Mouse listener for left tree
        leftTreeMouseListener = new LeftTreeMouseAdapter();
        leftTree.addMouseListener(leftTreeMouseListener);

        // Tree expansion listener for left tree
        TreeExpansionListener leftTreeExpansionListener = new LeftTreeExpansionListener();
        leftTree.addTreeExpansionListener(leftTreeExpansionListener);

        leftTree.setDragEnabled(true);
        leftTree.setDropMode(DropMode.USE_SELECTION);
        leftTree.setTransferHandler(new LeftTreeTransferHandler(this));

        leftTreeSelModel = leftTree.getSelectionModel();
        leftTreeSelModel.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        leftRowMapper = leftTreeSelModel.getRowMapper();

        leftTree.setRootVisible(true);
        leftTree.setShowsRootHandles(true);
        leftTree.setSelectionRow(0);

        // Add PC nodes to the left tree
        leftTreeMap.put(leftPathString, 0);
        addPCNodes(Paths.get(leftPathString), leftTopNode);
        leftTree.expandRow(0);

        // Place the left tree in scroll pane
        scrollPaneLeft.setViewportView(leftTree);

        // Change cursor to default
        // setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        // Remove setting last element of messages
        scrollMessagePane.getVerticalScrollBar()
                .removeAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
    }

    /**
     * Create the IBM i file tree on the right side of the window;
     * IBM i files are objects and differ conceptually from PC files,
     * therefore they are processed differently.
     *
     * @param currentRightRoot
     */
    protected void createNewRightSide(String currentRightRoot) {
        // Set wait-cursor (rotating wheel?)
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        // Keeps the scroll pane at the LAST MESSAGE.
        scrollMessagePane.getVerticalScrollBar()
                .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);

        // Set file system root symbol to the combo box
        //      rightPathComboBox.addItem(currentRightRoot);

        // Right tree showing IBM i file system (IFS)
        // ----------
        rightPathString = currentRightRoot;
        rightTopNode = new DefaultMutableTreeNode(currentRightRoot);
        rightTreeMap.put(currentRightRoot, "");

        // Create right hand tree
        rightTree = new JTree(rightTopNode);
        
        // Create tree model
        rightTreeModel = (DefaultTreeModel) rightTree.getModel();

        // Mouse listener for RIGHT TREE reacts on row number selected by mouse
        // left or right click
        rightTreeMouseListener = new RightTreeMouseAdapter();
        rightTree.addMouseListener(rightTreeMouseListener);

        rightTree.setDragEnabled(true);
        rightTree.setDropMode(DropMode.USE_SELECTION);
        rightTree.setTransferHandler(new RightTreeTransferHandler(this));

        rightTreeSelModel = rightTree.getSelectionModel();
        rightTreeSelModel.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        rightRowMapper = rightTreeSelModel.getRowMapper();
         
        rightTree.setRootVisible(true);
        rightTree.setShowsRootHandles(true);
        rightTree.setSelectionRow(0);

        rightPathString = correctRightPathString(rightPathString);

        // Right root must start with a slash
        if (!rightPathString.startsWith("/")) {
            // Show error message when the path string does not start with a slash.
            row = "Error: Right path string   " + rightPathString + "   does not start with a slash. "
                    + "Correct the path string and try again.";
            msgVector.add(row);
            showMessages(noNodes);

            // Change cursor to default
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            // Remove setting last element of messages
            scrollMessagePane.getVerticalScrollBar()
                    .removeAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            return;
        }

        if (remoteServer == null) {
            row = "Error: Connection to server  " + properties.getProperty("HOST") + "  failed!!!!!!.";
            msgVector.add(row);
            showMessages(noNodes);
            // Change cursor to default
            setCursor(Cursor.getDefaultCursor());
            // Remove setting last element of messages
            scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
        } else {

            // Create an object of IFSFile from the path string for adding nodes
            ifsFile = new IFSFile(remoteServer, rightPathString);

            // Add IFS nodes for the right tree in parallel background process
            // -------------
            AddAS400Nodes an = new AddAS400Nodes(remoteServer, ifsFile, rightTopNode, this);
            an.execute();
            // No statements may be here!
        }
    }

    /**
     * Correct left path string in properties, Empty path is replaced by a root
     * symbol (/ or C:\) Trailing slash is removed if path is not a root symbol.
     *
     * @param pathString
     * @return
     */
    protected String correctLeftPathString(String pathString) {

        if (pathString.lastIndexOf(pcFileSep) > 0 && !pathString.equals(firstLeftRootSymbol)) {
            // Get path pattern (parent)
            pathString = pathString.substring(0, pathString.lastIndexOf(pcFileSep));
        }
        return pathString;
    }

    /**
     * Correct right path string in properties, Empty path is replaced by slash
     * (/), Trailing slash is removed if path is not slash.
     *
     * @param rightString
     * @return
     */
    protected String correctRightPathString(String rightString) {
        // Right path may not be empty
        if (rightString.isEmpty()) {
            rightString = "/";
        }
        // Remove ending slash from non-root right path
        if (!rightString.equals("/") && rightString.endsWith("/")) {
            rightString = rightString.substring(0, rightString.lastIndexOf("/"));
        }
        return rightString;
    }

    /**
     * Refresh data in the window according to input fields in the top panel, and path panels.
     */
    protected void refreshWindow() {

        // Set application properties using input values from text fields
        properties.setProperty("HOST", hostTextField.getText());

        String userName = userNameTextField.getText().toUpperCase();
        userNameTextField.setText(userName);
        properties.setProperty("USERNAME", userName);

        if (!pcCharset.equals("*DEFAULT")) {
            // Check if charset is valid
            try {
                Charset.forName(pcCharset);
            } catch (IllegalCharsetNameException | UnsupportedCharsetException charset) {
                // If pcCharset is invalid, take ISO-8859-1
                pcCharset = "ISO-8859-1";
                pcCharComboBox.setSelectedItem(pcCharset);
            }
            properties.setProperty("PC_CHARSET", pcCharset);
        }

        if (!ibmCcsid.equals("*DEFAULT")) {
            try {
                Integer.parseInt(ibmCcsid);
            } catch (Exception exc) {
                exc.printStackTrace();
                ibmCcsid = "819";
                ibmCcsidComboBox.setSelectedItem(ibmCcsid);
            }
            properties.setProperty("IBM_CCSID", ibmCcsid);
        }

        // Source record length requires special treatment due to integer value
        String srcRecLen = sourceRecordLengthTextField.getText();
        try {
            Integer.valueOf(srcRecLen);
        } catch (NumberFormatException nfe) {
            // If the user enters non-integer text, take default value
            srcRecLen = "80";
        }
        sourceRecordLengthTextField.setText(srcRecLen);
        properties.setProperty("SOURCE_RECORD_LENGTH", srcRecLen);

//        properties.setProperty("SOURCE_TYPE", sourceType);

        libraryPatternTextField.setText(libraryPatternTextField.getText().toUpperCase());
        properties.setProperty("LIBRARY_PATTERN", libraryPatternTextField.getText());

        filePatternTextField.setText(filePatternTextField.getText().toUpperCase());
        properties.setProperty("FILE_PATTERN", filePatternTextField.getText());

        memberPatternTextField.setText(memberPatternTextField.getText().toUpperCase());
        properties.setProperty("MEMBER_PATTERN", memberPatternTextField.getText());

        // Create the updated text file in directory "paramfiles"
        try {
            outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
            properties.store(outfile, PROP_COMMENT);
            outfile.close();
        } catch (Exception exc) {
            exc.printStackTrace();
        }

        // Create right side of the window split panes (tree and table of IBM i).
        // The left side was created before.
        // rightRoot = properties.getProperty("RIGHT_PATH");
        rightRoot = (String) rightPathComboBox.getSelectedItem();

        // Register Action listener for RIGHT ComboBox which reacts on text change in its input field
        rightPathComboBox.addActionListener(new RightPathActionListener());

        createNewRightSide(rightRoot);

        // Unregister Action listener for RIGHT ComboBox which reacts on text change in its input field
        rightPathComboBox.removeActionListener(new RightPathActionListener());

    }

    /**
     * Reload node structure in the left side of the window
     *
     * @param addChildNodes
     */
    protected void showMessages(boolean addChildNodes) {
        scrollMessagePane.getVerticalScrollBar()
                .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
        ////reloadLeftSide(addChildNodes);
        buildMessageList();
        scrollMessagePane.getVerticalScrollBar()
                .removeAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
        // Make the message table visible in the message scroll pane
        scrollMessagePane.setViewportView(messageList);
    }

    /**
     *
     * @param addChildNodes
     */
    protected void reloadLeftSide(boolean addChildNodes) {
        if (addChildNodes) {
            if (leftNode != null) {
                // Add children to the node
                addPCNodes(Paths.get(leftPathString), leftNode);
            }
        }
        // Note that the structure of the node (children) changed
        leftTreeModel.nodeStructureChanged(leftNode);
        // Reload that node only (the other nodes remain unchanged)
        leftTreeModel.reload(leftNode);
        leftTree.expandRow(leftRow);
    }

    /**
     * Reload node structure in the right side of the window and show messages.
     */
    protected void showMessages() {
        scrollMessagePane.getVerticalScrollBar()
                .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
        buildMessageList();
        // Reload node structure in the right side of the window
        ////reloadRightSide();
        scrollMessagePane.getVerticalScrollBar()
                .removeAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
        // Make the message table visible in the message scroll pane
        scrollMessagePane.setViewportView(messageList);
    }

    /**
     * Reload node structure in the right side of the window.
     */
    protected void reloadRightSide() {
        if (rightPathString.contains(".FILE") && !rightPathString.endsWith(".FILE")) {
            rightPathString = rightPathString.substring(0, rightPathString.lastIndexOf(".")) + ".MBR";
        }
        ifsFile = new IFSFile(remoteServer, rightPathString);
        // Add IFS nodes (children) for the right tree in parallel background process
        AddAS400Nodes an = new AddAS400Nodes(remoteServer, ifsFile, rightNode, this);
        an.execute();
    }

    /**
     * Build message list.
     */
    protected void buildMessageList() {
        messageList.setSelectionBackground(Color.WHITE);
        // Suppressing some Info messages conditionally.
        // ---------------------------------------------
        // If you want to allow Info messages change the ALLOW_INFO_MESSAGES value
        // to *true*.
        ArrayList<String> newMsgVector = new ArrayList<>();
        // Inspect all messages in msgVector
        for (String message : msgVector) {
            // If the message is Info type and does not contain
            // AccessDeniedException, allow it.
            if (message.startsWith("Info") && ALLOW_INFO_MESSAGES
                    && !message.contains("AccessDeniedException")) {
                newMsgVector.add(message);
            } else if (!message.startsWith("Info")) {
                // Other types of messages are always allowed.
                newMsgVector.add(message);
            }
            msgVector = newMsgVector;
        }
        // End of selecting Info messages.

        // Fill message list with elements of the array list
        messageList.setListData(msgVector.toArray(new String[msgVector.size()]));
        // Make the message table visible in the message scroll pane
        scrollMessagePane.setViewportView(messageList);
    }

    /**
     * Copy and rename file.
     *
     * @param existingFileName
     */
    protected void copyAndRenameFile(String existingFileName) {
        // Target path string points to the directory to which the new file is to be inserted
        // "false" stands for not changing result to upper case
        String newFileName = new GetTextFromDialog("Insert spooled file", "Directory", "New name", targetPathString
                + pcFileSep, existingFileName, false, currentX, currentY).evaluateTextField();
        try {
            // User canceled creating the directory
            if (newFileName == null) {
                return;
            }
            // Copy nd rename the file to a new file name
            Path spoolPath = Paths.get(sourcePathString);
            Path newFilePath = Paths.get(targetPathString + pcFileSep + newFileName);

            // Copy command
            Files.copy(spoolPath, newFilePath, StandardCopyOption.COPY_ATTRIBUTES);
            row = "Comp: PC file  " + newFileName + "  was inserted to directory  " 
                    + targetPathString + ".";
            msgVector.add(row);
            showMessages(nodes);
        } catch (IOException exc) {
            exc.printStackTrace();
            row = "Error: PC file  " + newFileName + "  was NOT inserted to directory  "
                    + targetPathString + ".  -  " + exc.toString();
            msgVector.add(row);
            showMessages(nodes);
        }
        scrollMessagePane.getVerticalScrollBar()
                .removeAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
    }

    /**
     *
     * @param treePath
     * @return
     */
    protected String getStringFromLeftPath(TreePath treePath) {
        // Build absolute file path string from selected TreePath object
        String pathString = "";
        Object[] objects = treePath.getPath();
        for (Object object : objects) {
            pathString += object;
            // Path string must NOT be a slash (unix) nor A:\, B:\, ... (Windows)
            if (!pathString.equals("/") && !pathString.matches("[a-zA-Z]:\\\\")) {
                pathString += pcFileSep;
            }
        }
        return pathString;
    }

    /**
     *
     * @param treePath
     * @return
     */
    protected String getStringFromRightPath(TreePath treePath) {
        // Build absolute file path string from selected TreePath object
        String pathString = "";
        Object[] objects = treePath.getPath();
        for (Object object : objects) {
            pathString += object;
            // Path string must NOT be a slash
            if (!pathString.equals("/")) {
                pathString += "/";
            }
        }
        return pathString;
    }

    /**
     *
     * @param pathString
     * @param root
     * @return
     */
    protected TreePath getTreePathFromString(String pathString, String root) {
        TreePath treePath;

        if (pathString.equals(pcFileSep) || pathString.equals("")) {
            Object ob = new DefaultMutableTreeNode(pathString);
            treePath = new TreePath(ob);
            return treePath;
        }
        String pathBeg = root;
        String pathEnd = leftPathString.substring(pathBeg.length());

        String[] strs = pathEnd.split(pcFileSep);
        Object[] obj = new Object[strs.length];

        obj[0] = new DefaultMutableTreeNode(pathBeg);
        if (pathEnd.length() > 0) {
            for (int idx = 1; idx < strs.length; idx++) {
                obj[idx] = new DefaultMutableTreeNode(strs[idx]);
            }
        }
        treePath = new TreePath(obj);
        return treePath;
    }

    /**
     *
     * @author vzupka
     *
     */
    class LeftTreeExpansionListener implements TreeExpansionListener, TreeWillExpandListener {

        public void treeExpanded(TreeExpansionEvent treeExpansionEvent) {

            // Get tree path from the event
            // leftSelectedPath = treeExpansionEvent.getPath();
            JTree tree = (JTree) treeExpansionEvent.getSource();

            // Set colors of selected node
            DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) tree.getCellRenderer();
            renderer.setTextSelectionColor(Color.BLACK);
            renderer.setBackgroundSelectionColor(BLUE_LIGHTER);
            renderer.setBorderSelectionColor(Color.WHITE);

            leftRow = tree.getMinSelectionRow();
            leftSelectedPath = tree.getPathForRow(leftRow);

            // Get path string from the tree path just being expanded
            leftPathString = getStringFromLeftPath(leftSelectedPath);
            leftPathString = correctLeftPathString(leftPathString);

            // Get the node from the tree path
            leftNode = (DefaultMutableTreeNode) leftSelectedPath.getLastPathComponent();

            // Change tree root
            leftRoot = leftPathString;

            // Reload children of the node
            reloadLeftSide(nodes);
        }

        @Override
        public void treeCollapsed(TreeExpansionEvent treeExpansionEvent) {
        }

        @Override
        public void treeWillExpand(TreeExpansionEvent treeExpansionEvent) {
        }

        @Override
        public void treeWillCollapse(TreeExpansionEvent treeExpansionEvent) {
        }
    }

    /**
     *
     * @author vzupka
     *
     */
    class RightTreeExpansionListener implements TreeExpansionListener, TreeWillExpandListener {

        @Override
        public void treeExpanded(TreeExpansionEvent treeExpansionEvent) {

            // Get tree path from the event
            JTree tree = (JTree) treeExpansionEvent.getSource();

            // Set colors of selected node
            DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) tree.getCellRenderer();
            renderer.setTextSelectionColor(Color.BLACK);
            renderer.setBackgroundSelectionColor(BLUE_LIGHTER);
            renderer.setBorderSelectionColor(Color.WHITE);
            
            rightRow = tree.getMinSelectionRow();
            rightSelectedPath = tree.getPathForRow(rightRow);

            // Get path string from the tree path just being expanded
            rightPathString = getStringFromRightPath(rightSelectedPath);
            rightPathString = correctRightPathString(rightPathString);
            if (rightPathString.contains(".FILE") && !rightPathString.endsWith(".FILE")) {
                rightPathString = rightPathString.substring(0, rightPathString.lastIndexOf(".")) + ".MBR";
            }

            // Get the node from the tree path
            rightNode = (DefaultMutableTreeNode) rightSelectedPath.getLastPathComponent();

            // Change tree root
            rightRoot = rightPathString;
            // Reload children of the node
            reloadRightSide();
        }

        @Override
        public void treeCollapsed(TreeExpansionEvent treeExpansionEvent) {
        }

        @Override
        public void treeWillExpand(TreeExpansionEvent treeExpansionEvent) {
        }

        @Override
        public void treeWillCollapse(TreeExpansionEvent treeExpansionEvent) {
        }
    }

    /**
     * Mouse adapter for left tree.
     */
    class LeftTreeMouseAdapter extends MouseAdapter {

        private void popupEvent(MouseEvent mouseEvent) {

            Component component = mouseEvent.getComponent();
            Point pt = mouseEvent.getPoint();
            SwingUtilities.convertPointToScreen(pt, component);
            currentX = (int) pt.getX();
            currentY = (int) pt.getY();

            // Source tree for Drag and Drop transfer
            dragSourceTree = leftTree;

            // Get row number of the selected node
            leftRow = leftTree.getRowForLocation(mouseEvent.getX(), mouseEvent.getY());
            // Get tree path of the selected node
            leftSelectedPath = leftTree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
            if (leftSelectedPath == null) {
                return;
            }

            // Create new left node
            leftNode = (DefaultMutableTreeNode) leftSelectedPath.getLastPathComponent();

            // Create array of all selected tree paths
            TreePath[] leftPaths = leftTree.getSelectionPaths();

            // Create array of all selected row numbers (not actually used)
            int[] leftPathsRows = leftRowMapper.getRowsForPaths(leftPaths);

            // Get string from the tree path of the selected node
            leftPathString = getStringFromLeftPath(leftSelectedPath);
            // Remove trailing file separator from the path string
            leftPathString = correctLeftPathString(leftPathString);

            // Change left root to selected path
            leftRoot = leftPathString;

            // Get all path strings from the tree paths array
            leftPathStrings = new String[leftPaths.length];
            String[] pathStrings = new String[leftPaths.length];

            // Fill the array with children's path strings
            leftTreeMap.clear();
            for (int idx = 0; idx < leftPaths.length; idx++) {
                // Get string from tree path
                pathStrings[idx] = getStringFromLeftPath(leftPaths[idx]);
                // Remove trailing file separator from the path string
                pathStrings[idx] = correctLeftPathString(pathStrings[idx]);
                // Get row number from the rows array (not actually used)
                leftRow = leftPathsRows[idx];

                leftPathStrings[idx] = pathStrings[idx];
                // System.out.println("leftPathStrings[" + idx + "]: " + leftPathStrings[idx]);

                // Set row number to the map (path string, row number)
                // for possible later scrolling to the path node
                leftTreeMap.put(pathStrings[idx], leftPathsRows[idx]);

                // Add the new path string to the combo box
                leftPathComboBox.addItem(pathStrings[idx]);
            }

            // Set row number to the map (path string, row number) for possible
            // later scrolling to the path node
            leftTreeMap.put(leftPathString, leftRow);

            // Add or remove menu items in the left pop-up menu
            if (Files.isDirectory(Paths.get(leftPathString))) {
                // PC directory
                leftTreePopupMenu.removeAll();
                leftTreePopupMenu.add(createPcDirectory);
                leftTreePopupMenu.add(createPcFile);
                leftTreePopupMenu.add(renamePcFile);
                leftTreePopupMenu.add(copyFromLeft);
                leftTreePopupMenu.add(pasteToLeft);
                leftTreePopupMenu.add(insertSpooledFile);
                leftTreePopupMenu.add("");
                leftTreePopupMenu.add(movePcObjectToTrash);
            } else {
                // Single PC file
                leftTreePopupMenu.removeAll();
                leftTreePopupMenu.add(displayPcFile);
                if (!leftPathString.endsWith(".savf") || mouseEvent.getClickCount() == 2) {
                    // PC files regarded as save files are not allowed to be edited.
                    // This prevents unwanted editing.
                    leftTreePopupMenu.add(editPcFile);
                }
                leftTreePopupMenu.add(renamePcFile);
                leftTreePopupMenu.add(copyFromLeft);
                leftTreePopupMenu.add(pasteToLeft);
                leftTreePopupMenu.add(findInPcFiles);
                leftTreePopupMenu.add(displayPcTypedFile);
                leftTreePopupMenu.add("");
                leftTreePopupMenu.add(movePcObjectToTrash);
                // On double click run "editPcFile"
                if (mouseEvent.getClickCount() == 2
                        && (mouseEvent.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK) {
                    clipboardPathStrings = leftPathStrings;
                    for (String clipboardPathString1 : clipboardPathStrings) {
                        sourcePathString = clipboardPathString1;
                        JTextArea textArea = new JTextArea();
                        JTextArea textArea2 = new JTextArea();
                        new EditFile(remoteServer, MainWindow.this, textArea, textArea2, leftPathString, "rewritePcFile");
                    }
                }
            }

            // On right click pop up the menu
            if ((mouseEvent.getButton() == MouseEvent.BUTTON3)) {
                System.out.println("Right click PC: " + leftPathString);
                leftTreePopupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
            }
        }

        @Override
        public void mousePressed(MouseEvent mouseEvent) {
            if ((mouseEvent.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK
                    || (mouseEvent.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) == MouseEvent.BUTTON3_DOWN_MASK) {
                popupEvent(mouseEvent);
                leftTree.expandPath(leftSelectedPath);
            } else if (mouseEvent.isPopupTrigger()) {
                popupEvent(mouseEvent);
            }
        }
    }

    /**
     * Mouse adapter for ritht tree.
     */
    public class RightTreeMouseAdapter extends MouseAdapter {

        private void popupEvent(MouseEvent mouseEvent) throws ErrorCompletingRequestException {

            Component component = mouseEvent.getComponent();
            Point pt = mouseEvent.getPoint();
            SwingUtilities.convertPointToScreen(pt, component);
            currentX = (int) pt.getX() + 100;  // set left corner of the new window right of click point
            currentY = (int) pt.getY();

            // Source tree for Drag and Drop transfer
            dragSourceTree = rightTree;

            // Get row number of the selected node
            rightRow = rightTree.getRowForLocation(mouseEvent.getX(), mouseEvent.getY());
            // Get tree path of the selected node
            rightSelectedPath = rightTree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
            if (rightSelectedPath == null) {
                return;
            }
            // Create new right node
            rightNode = (DefaultMutableTreeNode) rightTree.getLastSelectedPathComponent();

            // Create array of all selected tree paths
            TreePath[] rightPaths = rightTree.getSelectionPaths();

            // Create array of all selected row numbers (not actually used)
            int[] rightPathsRows = rightRowMapper.getRowsForPaths(rightPaths);

            memberSourceTypes = new String[rightPathsRows.length];

            // Get string from the tree path of the selected node
            rightPathString = getStringFromRightPath(rightSelectedPath);
            // Remove trailing file separator from the path string
            rightPathString = correctRightPathString(rightPathString);


            // Replace source type with MBR
            if (rightPathString.contains(".FILE") && !rightPathString.endsWith(".FILE")) {
                rightPathString = rightPathString.substring(0, rightPathString.lastIndexOf(".")) + ".MBR";
            }


            ifsFile = new IFSFile(remoteServer, rightPathString);
            try {
                fileName = ifsFile.getName();
                if (rightPathString.endsWith(".FILE") && ifsFile.getSubtype().equals("SAVF")) {
                    String bareFileName = fileName.substring(0, fileName.indexOf(".FILE"));
                    String saveFileName = bareFileName + ".SAVF";
                    rightPathString = saveFileName;
                }
            } catch (AS400SecurityException | IOException exc) {
                exc.printStackTrace();
                row = "Error: Some serious error.";
                msgVector.add(row);
                showMessages(nodes);
                // Remove message scroll listener (cancel scrolling to the last message)
                scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            }
            // Change right root to selected path
            rightRoot = rightPathString;

            // Get all path strings from the tree paths array
            rightPathStrings = new String[rightPaths.length];
            String[] pathStrings = new String[rightPaths.length];

            // Fill the array with children's path strings
            rightTreeMap.clear();
            for (int idx = 0; idx < rightPaths.length; idx++) {
                // Get string from tree path
                pathStrings[idx] = getStringFromRightPath(rightPaths[idx]);
                // Remove trailing file separator from the path string
                pathStrings[idx] = correctRightPathString(pathStrings[idx]);
                
                // Get row number from the rows array (not actually used)
                ///rightRow = rightPathsRows[idx];
                
                // Get actual source types from the path strings
                if (pathStrings[idx].contains(".FILE") && !pathStrings[idx].endsWith(".FILE")) {
                    memberSourceTypes[idx] = pathStrings[idx].substring(pathStrings[idx].lastIndexOf(".") + 1);
                } else {
                    memberSourceTypes[idx] = "TXT";
                }

                rightPathStrings[idx] = pathStrings[idx];
                // System.out.println("rightPathStrings[" + idx + "]: " + rightPathStrings[idx]);

                // Set row number to the map (path string, row number)
                // for possible later scrolling to the path node
                ///rightTreeMap.put(pathStrings[idx], rightPathsRows[idx]);
                rightTreeMap.put(pathStrings[idx], memberSourceTypes[idx]);
                
                // Correct suffix of the member from source type to .MBR
                if (pathStrings[idx].contains(".FILE") && !pathStrings[idx].endsWith(".FILE")) {
                    pathStrings[idx] = pathStrings[idx].substring(0, pathStrings[idx].lastIndexOf(".")) + ".MBR";
                }

                // Add the new path string to the combo box
                rightPathComboBox.addItem(pathStrings[idx]);
            }
            
            // Set row number to the map (path string, row number) for possible later scrolling to the path node
            ///rightTreeMap.put(rightPathString, rightRow);
                       
            try {
                // Add or remove menu items in the right pop-up menu
                if (ifsFile.isDirectory()) {
                    if (rightPathString.startsWith("/QSYS.LIB")) {

                        // System library - only spooled files
                        if (rightPathString.equals("/QSYS.LIB")) {
                            rightTreePopupMenu.removeAll();
                            rightTreePopupMenu.add(workWithSpooledFiles);
                        } //
                        // Non-QSYS library
                        else if (rightPathString.endsWith(".LIB")) {
                            rightTreePopupMenu.removeAll();
                            rightTreePopupMenu.add(createSourcePhysicalFile);
                            rightTreePopupMenu.add(createSaveFile);
                            rightTreePopupMenu.add(pasteToRight);
                            rightTreePopupMenu.add("");
                            rightTreePopupMenu.add(copyLibrary);
                            rightTreePopupMenu.add("");
                            rightTreePopupMenu.add("-----------");
                            rightTreePopupMenu.add(clearLibrary);
                            rightTreePopupMenu.add(deleteLibrary);
                        } //
                        // Source Physical File
                        else if (rightPathString.endsWith(".FILE")) {
                            if (ifsFile.isSourcePhysicalFile()) {
                                rightTreePopupMenu.removeAll();
                                rightTreePopupMenu.add(createSourceMember);
                                rightTreePopupMenu.add(renameIfsFile);
                                rightTreePopupMenu.add(copyFromRight);
                                rightTreePopupMenu.add(pasteToRight);
                                rightTreePopupMenu.add("");
                                rightTreePopupMenu.add(deleteSourcePhysicalFile);
                            }
                        }
                    } else { //
                        // General IFS directory
                        rightTreePopupMenu.removeAll();
                        rightTreePopupMenu.add(createIfsDirectory);
                        rightTreePopupMenu.add(createIfsFile);
                        rightTreePopupMenu.add(changeCCSID);
                        rightTreePopupMenu.add(renameIfsFile);
                        rightTreePopupMenu.add(copyFromRight);
                        rightTreePopupMenu.add(pasteToRight);
                        rightTreePopupMenu.add("");
                        rightTreePopupMenu.add(deleteIfsObject);
                    }
                } //
                // Source Member
                else if (rightPathString.contains(".FILE") ) {
                    rightTreePopupMenu.removeAll();
                    rightTreePopupMenu.add(displaySourceMember);
                    rightTreePopupMenu.add(editSourceMember);
                    rightTreePopupMenu.add(renameIfsFile);
                    rightTreePopupMenu.add(copyFromRight);
                    rightTreePopupMenu.add(pasteToRight);
                    rightTreePopupMenu.add(compileSourceMember);
                    rightTreePopupMenu.add(findInSourceMembers);
                    rightTreePopupMenu.add("");
                    rightTreePopupMenu.add(deleteSourceMember);

                    // On double click run "editSourceMember"
                    // Source member
                    if (rightPathString.contains(".FILE") 
                            && mouseEvent.getClickCount() == 2
                            && (mouseEvent.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK) {
                        scrollMessagePane.getVerticalScrollBar()
                                .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
                        JTextArea textArea = new JTextArea();
                        JTextArea textArea2 = new JTextArea();
                        String pathStringMbr = rightPathString.substring(0, rightPathString.lastIndexOf(".")) + ".MBR";
                        EditFile edtf = new EditFile(remoteServer, MainWindow.this, textArea, textArea2, pathStringMbr, "rewriteSourceMember");
                    }

                } //
                // Save file
                else if (rightPathString.startsWith("/QSYS.LIB") && rightPathString.endsWith(".SAVF")) {
                    rightTreePopupMenu.removeAll();
                    rightTreePopupMenu.add(copyFromRight);
                    rightTreePopupMenu.add(pasteToRight);
                    rightTreePopupMenu.add(clearSaveFile);
                    rightTreePopupMenu.add("");
                    rightTreePopupMenu.add(deleteSaveFile);
                } //
                // Output queue
                else if (rightPathString.endsWith(".OUTQ")) {
                    rightTreePopupMenu.removeAll();
                    rightTreePopupMenu.add(workWithSpooledFiles);
                } //
                // Single general IFS file
                else {
                    rightTreePopupMenu.removeAll();
                    rightTreePopupMenu.add(displayIfsFile);
                    if (!rightPathString.endsWith(".savf")) {
                        // IFS files regarded as save files are not allowed to be edited.
                        // This is a prevention from unwanted editing.
                        rightTreePopupMenu.add(editIfsFile);
                    }
                    rightTreePopupMenu.add(changeCCSID);
                    rightTreePopupMenu.add(renameIfsFile);
                    rightTreePopupMenu.add(copyFromRight);
                    rightTreePopupMenu.add(pasteToRight);
                    if (rightPathString.toUpperCase().endsWith("CBLLE")
                            || rightPathString.toUpperCase().endsWith(".CLLE")
                            || rightPathString.toUpperCase().endsWith(".CLP")
                            || rightPathString.toUpperCase().endsWith(".C")
                            || rightPathString.toUpperCase().endsWith(".CPP")
                            || rightPathString.toUpperCase().endsWith(".CBL")
                            || rightPathString.toUpperCase().endsWith(".RPGLE")
                            || rightPathString.toUpperCase().endsWith(".RPG")
                            || rightPathString.toUpperCase().endsWith(".SQL")
                            || rightPathString.toUpperCase().endsWith(".SQLC")
                            || rightPathString.toUpperCase().endsWith(".SQLCPP")
                            || rightPathString.toUpperCase().endsWith(".SQLCBL")
                            || rightPathString.toUpperCase().endsWith(".SQLCBLLE")
                            || rightPathString.toUpperCase().endsWith(".SQLRPG")
                            || rightPathString.toUpperCase().endsWith(".SQLRPGLE")) {
                    }

                    // On double click run "editIfsFile" 
                    // Ordinary IFS file, not Save File
                    if (!rightPathString.startsWith("/QSYS.LIB") && !rightPathString.endsWith(".savf")
                            && mouseEvent.getClickCount() == 2
                            && (mouseEvent.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK) {
                        scrollMessagePane.getVerticalScrollBar()
                                .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
                        JTextArea textArea = new JTextArea();
                        JTextArea textArea2 = new JTextArea();
                        new EditFile(remoteServer, MainWindow.this, textArea, textArea2, rightPathString, "rewriteIfsFile");
                    }
                    rightTreePopupMenu.add(findInIfsFiles);
                    rightTreePopupMenu.add(compileIfsFile);
                    rightTreePopupMenu.add("");
                    rightTreePopupMenu.add(deleteIfsObject);
                }

                // On right click pop up the menu
                if ((mouseEvent.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) == MouseEvent.BUTTON3_DOWN_MASK) {
                    System.out.println("Right click IBMi: " + rightPathString);
                    rightTreePopupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                }
            } catch (AS400Exception | AS400SecurityException | IOException exc) {
                exc.printStackTrace();
                row = "Error: Connection to the server lost. - " + exc.toString() + ". Trying to connect again.";
                msgVector.add(row);
                showMessages(noNodes);
                // Remove message scroll listener (cancel scrolling to the last message)
                scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
                // Try to reconnect server - FILE and RECORDACCESS services
                try {
                    remoteServer = new AS400(hostTextField.getText(), userNameTextField.getText());
                    remoteServer.connectService(AS400.FILE);
                    remoteServer.connectService(AS400.RECORDACCESS);
                    row = "Info: A new connection to the server was obtained.";
                    msgVector.add(row);
                    showMessages(noNodes);
                } catch (AS400SecurityException | IOException ex) {
                    row = "Error: Getting a new connection to the server." + ex.toString();
                    msgVector.add(row);
                    showMessages(noNodes);
                    exc.printStackTrace();
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent mouseEvent) {
            if ((mouseEvent.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK
                    || (mouseEvent.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) == MouseEvent.BUTTON3_DOWN_MASK) {
                try {
                    popupEvent(mouseEvent);
                    rightTree.expandPath(rightSelectedPath);
                } catch (ErrorCompletingRequestException ex) {
                    Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (mouseEvent.isPopupTrigger()) {
                try {
                    popupEvent(mouseEvent);
                } catch (ErrorCompletingRequestException ex) {
                    Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Action listener for LEFT PATH ComboBox reacts on text change in its input field.
     */
    class LeftPathActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent ae) {
            JComboBox<String> source = (JComboBox) ae.getSource();
            // Get path string from the item typed in combo box text field
            leftPathString = (String) source.getSelectedItem();

            if (leftPathString.isEmpty()) {
                leftPathString = firstLeftRootSymbol;
            }

            source.setSelectedItem(leftPathString);

            leftTreeMap.put(leftPathString, leftRow);

            // Update "LEFT_PATH" property in Parameters.txt file
            properties.setProperty("LEFT_PATH", leftPathString);
            // Create the updated text file in directory "paramfiles"
            try {
                outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                properties.store(outfile, PROP_COMMENT);
                outfile.close();
            } catch (IOException exc) {
                exc.printStackTrace();
            }

            // Create new left side from the selected leftPathString = leftRoot
            createNewLeftSide(leftPathString);
        }
    }

    /**
     * Action listener for RIGHT PATH ComboBox reacts on text change in its input field.
     * Change may be done by selecting an item in the combo box, or user text entry.
     */
    class RightPathActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent ae) {
            JComboBox<String> source = (JComboBox) ae.getSource();
            // Get path string from the item typed in combo box text field
            rightPathString = ((String) source.getSelectedItem());

            // System.out.println("rightPathString Right path listener:" + rightPathString);

            if (rightPathString.isEmpty()) {
                rightPathString = "/";
            }

            if (rightPathString.toUpperCase().startsWith("/QSYS.LIB")) {
                rightPathString = rightPathString.toUpperCase();
            }
            rightPathString = correctRightPathString(rightPathString);
            source.setSelectedItem(rightPathString);

            ///rightTreeMap.put(rightPathString, rightRow);

            // Update "RIGHT_PATH" property in Parameters.txt file
            properties.setProperty("RIGHT_PATH", rightPathString);
            // Create the updated text file in directory "paramfiles"
            try {
                outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                properties.store(outfile, PROP_COMMENT);
                outfile.close();
            } catch (IOException exc) {
                exc.printStackTrace();
            }

            // Create new object for the right path IFS file (important)
            ifsFile = new IFSFile(remoteServer, rightPathString);
            rightPathComboBox.setForeground(Color.BLACK);
            try {
                if (!ifsFile.exists()) {  
                    // If this object does not exist, send error message 
                    // and no object is created
                    rightPathComboBox.setForeground(DIM_RED);
                    row = "Error: " + rightPathString + " is not an IFS object";
                    msgVector.add(row);
                    showMessages();
                }
                else {
                    // Create new right side from the selected rightPathString
                    createNewRightSide(rightPathString);
                }
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        }

    }

    /**
     * Adjustment listener for MESSAGE scroll pane.
     */
    class MessageScrollPaneAdjustmentListenerMax implements AdjustmentListener {

        @Override
        public void adjustmentValueChanged(AdjustmentEvent ae) {
            // Set scroll pane to the bottom - the last element
            ae.getAdjustable().setValue(ae.getAdjustable().getMaximum());
            // Set scroll pane to the top - the first element
            ////ae.getAdjustable().setValue(ae.getAdjustable().getMinimum());
            // Set scroll pane to the top - the fírst element and stays at the visible amount
            ////ae.getAdjustable().setValue(ae.getAdjustable().getVisibleAmount());
        }
    }

    /**
     * Drag and drop from right to left (or left to left) - transfer handler
     */
    public class LeftTreeTransferHandler extends TransferHandler {

        MainWindow mainWindow;

        LeftTreeTransferHandler(MainWindow mainWindow) {
            this.mainWindow = mainWindow;
        }

        @Override
        public int getSourceActions(JComponent component) {
            return TransferHandler.COPY;
        }

        @Override
        protected Transferable createTransferable(JComponent component) {
            return new StringSelection("");
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            return info.isDrop();
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
            if (!info.isDrop()) {
                return info.isDrop();
            }

            leftInfo = info;

            JTree.DropLocation dl = (JTree.DropLocation) info.getDropLocation();
            // Target path
            targetTreePath = dl.getPath();

            targetPathString = getStringFromLeftPath(targetTreePath);

            // Remove trailing file separator from the path string
            targetPathString = correctLeftPathString(targetPathString);

            // ------------------------
            if (dragSourceTree == leftTree) {

                // Copy from PC to PC itself
                // =========================

                row = "Wait: Copying from PC to PC . . .";
                msgVector.add(row);
                showMessages();

                // Set clipboard path strings for paste operation
                clipboardPathStrings = leftPathStrings;

                // Perform copying from left to left
                // ---------------------------------
                ParallelCopy_PC_PC parallelCopy_PC_PC = new ParallelCopy_PC_PC(
                        clipboardPathStrings, targetPathString, leftInfo, mainWindow);
                parallelCopy_PC_PC.execute();
            }

            // -------------------------
            if (dragSourceTree == rightTree) {

                // Copy from IBM i to PC
                // =====================

                row = "Wait: Copying from IBM i to PC . . .";
                msgVector.add(row);
                showMessages();

                // Set clipboard path strings for paste operation
                clipboardPathStrings = rightPathStrings;

                // Change transferred source type to .MBR for IFS transfer in all transferred members
                for (int idx = 0; idx < clipboardPathStrings.length; idx++) {
                    if (clipboardPathStrings[idx].contains(".FILE/") && !clipboardPathStrings[idx].endsWith("SAVF"))  {
                        clipboardPathStrings[idx] = clipboardPathStrings[idx].substring(0, 
                                clipboardPathStrings[idx].lastIndexOf(".")) + ".MBR";
                    }
                }
                
                // Perform copying from right to left
                // ----------------------------------
                ParallelCopy_IBMi_PC parallelCopy_IBMi_PC = new ParallelCopy_IBMi_PC(
                            remoteServer, clipboardPathStrings, targetPathString, leftInfo, mainWindow);
                parallelCopy_IBMi_PC.execute();
            }
            return true;
        }
    }

    /**
     *
     * @param info
     */
    protected void expandLeftTreeNode(TransferHandler.TransferSupport info) {

        // Target node has more children
        // -----------
        targetNode = (DefaultMutableTreeNode) leftTree.getLastSelectedPathComponent();
        // Add target node for inserted children
        if (targetNode != null) {
            // Add newly received children to the node
            addPCNodes(Paths.get(targetPathString), targetNode);
        }
        // Get coordinates of target node
        TransferHandler.DropLocation dropLocation = info.getDropLocation();
        double dropX = dropLocation.getDropPoint().getX();
        double dropY = dropLocation.getDropPoint().getY();

        // Get index from coordinates of the drop node
        leftRow = leftTree.getRowForLocation((int) dropX, (int) dropY);
        // Expand that node on that index
        leftTree.expandRow(leftRow);
        // Note that the children were added
        leftTreeModel.nodeStructureChanged(targetNode);
    }

    /**
     * Drag and drop from left to right - transfer handler.
     */
    class RightTreeTransferHandler extends TransferHandler {

        MainWindow mainWindow;

        RightTreeTransferHandler(MainWindow mainWindow) {
            this.mainWindow = mainWindow;
        }

        @Override
        public int getSourceActions(JComponent component) {
            return TransferHandler.COPY;
        }

        @Override
        protected Transferable createTransferable(JComponent component) {
            return new StringSelection("");
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            return info.isDrop();
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
            if (!info.isDrop()) {
                return false;
            }

            rightInfo = info;

            JTree.DropLocation dl = (JTree.DropLocation) info.getDropLocation();
            // Target path
            rightSelectedPath = dl.getPath();
            // Target node
            rightNode = (DefaultMutableTreeNode) rightTree.getLastSelectedPathComponent();

            rightPathString = getStringFromRightPath(rightSelectedPath);


            // Remove trailing file separator from the right path string
            targetPathString = correctRightPathString(rightPathString);
            // ------------------------
            if (dragSourceTree == leftTree) {

                // Copy from PC
                // ============

                row = "Wait: Copying from PC to IBM i . . .";
                msgVector.add(row);
                showMessages();

                // Set clipboard path strings for paste operation
                clipboardPathStrings = leftPathStrings;

                // Perform copying from left to right
                // ----------------------------------
                ParallelCopy_PC_IBMi parallelCopy_PC_IBMI = new ParallelCopy_PC_IBMi(
                        remoteServer, clipboardPathStrings, targetPathString, rightInfo, mainWindow);
                parallelCopy_PC_IBMI.execute();
            }

            // -------------------------
            if (dragSourceTree == rightTree) {

                // Copy from IBM i itself
                // ======================

                row = "Wait: Copying from IBM i to IBM i . . .";
                msgVector.add(row);
                showMessages();

                // Set clipboard path strings for paste operation
                clipboardPathStrings = rightPathStrings;

                // Perform copying from right to right
                // -----------------------------------
                ParallelCopy_IBMi_IBMi parallelCopy_IMBI_IBMI = new ParallelCopy_IBMi_IBMi(
                        remoteServer, clipboardPathStrings, targetPathString, rightInfo, mainWindow);
                parallelCopy_IMBI_IBMI.execute();
            }
            return true;
        }
    }

    /**
     * Expand right tree node after change.
     *
     * @param info
     */
    protected void expandRightTreeNode(TransferHandler.TransferSupport info) {

        IFSFile targetPath = new IFSFile(remoteServer, targetPathString);

        // Target node
        // -----------
        targetNode = (DefaultMutableTreeNode) rightTree.getLastSelectedPathComponent();
        // Add target node for inserted children
        if (targetNode != null) {
            // Add IFS nodes (children) for the right tree in parallel background process
            AddAS400Nodes an = new AddAS400Nodes(remoteServer, targetPath, targetNode, this);
            an.execute();
        }

        // Get coordinates of target node
        TransferHandler.DropLocation dropLocation = info.getDropLocation();
        double dropX = dropLocation.getDropPoint().getX();
        double dropY = dropLocation.getDropPoint().getY();

        // Get index from coordinates of the drop node
        rightRow = rightTree.getRowForLocation((int) dropX, (int) dropY);
        // Expand that node on that index
        rightTree.expandRow(rightRow);

        // Note that the children were added
        rightTreeModel.nodeStructureChanged(targetNode);
    }

    /**
     * Window adapter setting current coordinates of the window to properties.
     */
    class MainWindowAdapter extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent we) {
            // Get actual main window coordinates
            int mainWindowX = we.getWindow().getX();
            int mainWindowY = we.getWindow().getY();
            try {
                // Read properties file
                infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
                properties.load(infile);

                // Set main window coordinates to properties
                properties.setProperty("MAIN_WINDOW_X", String.valueOf(mainWindowX));
                properties.setProperty("MAIN_WINDOW_Y", String.valueOf(mainWindowY));

                // Set new properties to the file in directory "paramfiles"
                outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                properties.store(outfile, PROP_COMMENT);
                outfile.close();
            } catch (IOException exc) {
                exc.printStackTrace();
            }
            System.exit(0);
        }
    }

    /**
     * Check connection by ping; In failure try to connect sevices.
     */
    /*
    public class CheckConnection extends SwingWorker<Void, Void> {

        AS400JPing pingObject;
        boolean ping_FILE;
        boolean ping_COMMAND;
        boolean ping_RECORDACCESS;
        boolean ping_PRINT;
    */
        /**
         * Constructor.
         */
        /*
        CheckConnection(AS400 remoteServer) {
            pingObject = new AS400JPing(properties.getProperty("HOST"));
        }
        */
        /**
         * This method performs the background task:
         * Checking AS400 connection by ping to the server and keeping connection alive.
         *
         */
        /*
        @Override
        @SuppressWarnings("UseSpecificCatch")
        public Void doInBackground() {
            int delay = 10000;
            // Endless loop
            while (true) {
                // boolean isCancelled = isCancelled();
                // System.out.println("isCancelled: " + isCancelled);
                // When cancel(true) was issued on this object stop running
                if (isCancelled()) {
                    return null;
                }
                // Ping to the server for host server services.
                ping_FILE = pingObject.ping(AS400.FILE);
                ping_COMMAND = pingObject.ping(AS400.COMMAND);
                ping_RECORDACCESS = pingObject.ping(AS400.RECORDACCESS);
                ping_PRINT = pingObject.ping(AS400.PRINT);

                while (!ping_FILE) {
                    try {
                        System.out.println("FILE");
                        // Try to connect service again
                        remoteServer.connectService(AS400.FILE);
                        row = "Comp: FILE service reconnected.";
                        msgVector.add(row);
                        showMessages(noNodes);
                        break; // break the loop when the connection attempt is successful
                    } catch (Exception exc) {
                        row = "Error: getting new connection to FILE service: " + exc.toString();
                        msgVector.add(row);
                        showMessages(noNodes);
                        //exc.printStackTrace();
                        // Delay in milliseconds before a new connection attempt
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException iex) {
                        }
                        continue;
                    }
                }

                while (!ping_COMMAND) {
                    try {
                        System.out.println("COMMAND");
                        // Try to connect service again
                        remoteServer.connectService(AS400.COMMAND);
                        row = "Comp: COMMAND service reconnected.";
                        msgVector.add(row);
                        showMessages(noNodes);
                        break; // break the loop when the connection attempt is successful
                    } catch (Exception exc) {
                        row = "Error: getting new connection to COMMAND service: " + exc.toString();
                        msgVector.add(row);
                        showMessages(noNodes);
                        //exc.printStackTrace();
                        // Delay in milliseconds before a new connection attempt
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException iex) {
                        }
                        continue;
                    }
                }

                while (!ping_RECORDACCESS) {
                    try {
                        System.out.println("RECORDACCESS");
                        // Try to connect service again
                        remoteServer.connectService(AS400.RECORDACCESS);
                        row = "Comp: RECORDACCESS service reconnected.";
                        msgVector.add(row);
                        showMessages(noNodes);
                        break; // break the loop when the connection attempt is successful
                    } catch (Exception exc) {
                        row = "Error: getting new connection to RECORDACCESS service: " + exc.toString();
                        msgVector.add(row);
                        showMessages(noNodes);
                        //exc.printStackTrace();
                        // Delay in milliseconds before a new connection attempt
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException iex) {
                        }
                        continue;
                    }
                }

                while (!ping_PRINT) {
                    try {
                        System.out.println("PRINT");
                        // Try to connect service again
                        remoteServer.connectService(AS400.PRINT);
                        row = "Comp: PRINT service reconnected.";
                        msgVector.add(row);
                        showMessages(noNodes);
                        break; // break the loop when the connection attempt is successful
                    } catch (Exception exc) {
                        row = "Error: getting new connection to PRINT service: " + exc.toString();
                        msgVector.add(row);
                        showMessages(noNodes);
                        //exc.printStackTrace();
                        // Delay in milliseconds before a new connection attempt
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException iex) {
                        }
                    }
                }
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException iex) {
                }
            }
        }
    }
    */

    /**
     * Main method of the MainWindow class
     *
     * @param args
     */
    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> {
            try {
                // Set operating system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException |
                    UnsupportedLookAndFeelException exc) {
                exc.printStackTrace();
            }
            MainWindow mainWindow = new MainWindow();
            mainWindow.createWindow();
        });
    }
}
