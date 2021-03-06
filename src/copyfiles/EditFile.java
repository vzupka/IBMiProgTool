package copyfiles;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400File;
import com.ibm.as400.access.AS400FileRecordDescription;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.IFSFileInputStream;
import com.ibm.as400.access.IFSFileOutputStream;
import com.ibm.as400.access.Record;
import com.ibm.as400.access.RecordFormat;
import com.ibm.as400.access.SequentialFile;
import com.ibm.as400.access.FileAttributes;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.plaf.basic.BasicTextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;
import javax.swing.text.JTextComponent;
import javax.swing.text.LayeredHighlighter;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

/**
 * Edit file - PC file, IFS file, Source Member.
 *
 * @author Vladimír Župka, 2016
 */
public final class EditFile extends JFrame {

    JTextArea textArea;
    JTextArea textArea2;

    JPopupMenu textAreaPopupMenu = new JPopupMenu();
    JMenuItem changeSelMode = new JMenuItem();
    JMenuItem toggleCaret = new JMenuItem();

    JMenuBar menuBar;
    JMenu helpMenu;
    JMenuItem helpMenuItemEN;
    JMenuItem helpMenuItemCZ;
    JMenuItem helpMenuItemRPGIII;
    JMenuItem helpMenuItemRPGIV;
    JMenuItem helpMenuItemCOBOL;
    JMenuItem helpMenuItemDDS;

    JMenu editMenu;
    JMenuItem menuUndo;
    JMenuItem menuRedo;
    JMenuItem menuCut;
    JMenuItem menuCopy;
    JMenuItem menuPaste;
    JMenuItem menuDelete;
    JMenuItem menuDelete2;
    JMenuItem menuFind;

    boolean textAreaIsSplit = false;
    boolean lowerHalfActive = false;

    TextAreaDocListener textAreaDocListener = new TextAreaDocListener();
    TextArea2DocListener textArea2DocListener = new TextArea2DocListener();

    TextAreaMouseListener textAreaMouseListener;
    TextArea2MouseListener textArea2MouseListener;

    WindowEditFileAdapter windowEditFileListener;

    static Color originalButtonBackground;
    static Color originalButtonForeground;

    static final Color VERY_LIGHT_BLUE = Color.getHSBColor(0.60f, 0.020f, 0.99f);
    static final Color VERY_LIGHT_GREEN = Color.getHSBColor(0.52f, 0.020f, 0.99f);
    static final Color VERY_LIGHT_PINK = Color.getHSBColor(0.025f, 0.008f, 0.99f);

    static final Color WARNING_COLOR = new Color(255, 200, 200);
    static final Color DIM_BLUE = Color.getHSBColor(0.60f, 0.2f, 0.5f); // blue little saturated dim (gray)
    static final Color DIM_RED = Color.getHSBColor(0.00f, 0.2f, 0.98f); // red little saturated bright
    static final Color VERY_LIGHT_GRAY = Color.getHSBColor(0.50f, 0.01f, 0.90f);

    static final Color DARK_RED = Color.getHSBColor(0.95f, 0.95f, 0.60f);

    HighlightPainter currentPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.ORANGE);
    HighlightPainter highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
    Highlighter blockHighlighter;

    static final Color BLUE_DARKER = Color.getHSBColor(0.60f, 0.20f, 0.95f);
    HighlightPainter blockBlueDarker = new DefaultHighlighter.DefaultHighlightPainter(BLUE_DARKER);
    static final Color BLUE_LIGHTER = Color.getHSBColor(0.60f, 0.15f, 0.998f);
    HighlightPainter blockBlueLighter = new DefaultHighlighter.DefaultHighlightPainter(BLUE_LIGHTER);

    static final Color GREEN_DARKER = Color.getHSBColor(0.35f, 0.15f, 0.90f);
    HighlightPainter blockGreenDarker = new DefaultHighlighter.DefaultHighlightPainter(GREEN_DARKER);
    static final Color GREEN_LIGHTER = Color.getHSBColor(0.35f, 0.10f, 0.98f);
    HighlightPainter blockGreenLighter = new DefaultHighlighter.DefaultHighlightPainter(GREEN_LIGHTER);

    static final Color RED_DARKER = Color.getHSBColor(0.95f, 0.12f, 0.92f);
    HighlightPainter blockRedDarker = new DefaultHighlighter.DefaultHighlightPainter(RED_DARKER);
    static final Color RED_LIGHTER = Color.getHSBColor(0.95f, 0.09f, 0.98f);
    HighlightPainter blockRedLighter = new DefaultHighlighter.DefaultHighlightPainter(RED_LIGHTER);

    static final Color YELLOW_DARKER = Color.getHSBColor(0.20f, 0.15f, 0.90f);
    HighlightPainter blockYellowDarker = new DefaultHighlighter.DefaultHighlightPainter(YELLOW_DARKER);
    static final Color YELLOW_LIGHTER = Color.getHSBColor(0.20f, 0.15f, 0.96f);
    HighlightPainter blockYellowLighter = new DefaultHighlighter.DefaultHighlightPainter(YELLOW_LIGHTER);

    static final Color BROWN_DARKER = Color.getHSBColor(0.13f, 0.15f, 0.86f);
    HighlightPainter blockBrownDarker = new DefaultHighlighter.DefaultHighlightPainter(BROWN_DARKER);
    static final Color BROWN_LIGHTER = Color.getHSBColor(0.13f, 0.15f, 0.92f);
    HighlightPainter blockBrownLighter = new DefaultHighlighter.DefaultHighlightPainter(BROWN_LIGHTER);

    static final Color GRAY_DARKER = Color.getHSBColor(0.25f, 0.015f, 0.82f);
    HighlightPainter blockGrayDarker = new DefaultHighlighter.DefaultHighlightPainter(GRAY_DARKER);
    static final Color GRAY_LIGHTER = Color.getHSBColor(0.25f, 0.015f, 0.88f);
    HighlightPainter blockGrayLighter = new DefaultHighlighter.DefaultHighlightPainter(GRAY_LIGHTER);

    static final Color CURLY_BRACKETS_DARKER = Color.getHSBColor(0.25f, 0.020f, 0.75f);
    HighlightPainter curlyBracketsDarker = new DefaultHighlighter.DefaultHighlightPainter(CURLY_BRACKETS_DARKER);
    static final Color CURLY_BRACKETS_LIGHTER = Color.getHSBColor(0.25f, 0.020f, 0.86f);
    HighlightPainter curlyBracketsLighter = new DefaultHighlighter.DefaultHighlightPainter(CURLY_BRACKETS_LIGHTER);

    // Block painter
    HighlightPainter blockPainter;

    String progLanguage; // Programming language to highlight (RPG **FREE, ...)

    // Listener for edits on the current document.
    protected UndoableEditListener undoHandler = new UndoHandler();

    // UndoManager that we add edits to. 
    protected UndoManager undo = new UndoManager();

    // Actions for undo and redo
    private UndoAction undoAction = new UndoAction();
    private RedoAction redoAction = new RedoAction();


    CompileButtonListener compileButtonListener;

    FindWindow findWindow;

    JButton saveButton = new JButton("Save");

    JButton undoButton = new JButton("Undo");
    JButton redoButton = new JButton("Redo");

    JLabel shiftLabel = new JLabel("Shift selection: ");
    JButton leftShiftButton;
    JButton rightShiftButton;

    JLabel fontLabel = new JLabel("Font:");
    JTextField fontSizeField = new JTextField();

    JButton caretButton = new JButton();

    JButton selectionModeButton = new JButton();

    JButton compileButton = new JButton("Compile");
    Compile compile; // Compile class object variable

    JLabel highlightBlocksLabel = new JLabel("Blocks:");

    JComboBox<String> languageComboBox = new JComboBox<>();
    JComboBox<String> fontComboBox = new JComboBox<>();

    JButton splitUnsplitButton;
    JButton findButton;

    JLabel characterSetLabel = new JLabel();

    JScrollPane scrollPaneUpper;
    JScrollPane scrollPaneLower;
    JSplitPane splitVerticalPane;

    BoxLayout globalPanelBoxLayout;

    JPanel globalPanel;
    JPanel rowPanel1;
    JPanel colPanel1;
    JPanel colPanel2;
    JPanel colPanel21;
    JPanel colPanel22;
    JPanel rowPanel2;
    JPanel topPanel;

    HighlightListener highlightListener = new HighlightListener();

    // Map containing intervals (start, end) of highligthted texts.
    TreeMap<Integer, Integer> highlightMap = new TreeMap<>();
    // Position set by mouse press or by program in FindWindow class (find or replace listeners).
    // The position is searched in the highlightMap to find the startOffset of a highlight.
    Integer curPos = 0;
    Integer curPos2 = 0;

    // Lists of starts and ends of highlighted texts taken from the highlightMap.
    ArrayList<Integer> startOffsets = new ArrayList<>();
    ArrayList<Integer> endOffsets = new ArrayList<>();

    int sequence = 0; // sequence number of current highlighted interval in the primary text area
    int sequence2 = 0; // sequence number of current highlighted interval in the secondary text area
    Integer startOffset; // start offset of highlighted interval
    Integer endOffset; // end offset of highlighted interval
    Integer startOffset2; // start offset of highlighted interval
    Integer endOffset2; // end offset of highlighted interval

    static int windowWidth;
    static int windowHeight;
    int screenWidth;
    int screenHeight;
    int windowX;
    int windowY;

    Path parPath = Paths.get(System.getProperty("user.dir"), "paramfiles", "Parameters.txt");
    Path shiftLeftIconPath = Paths.get(System.getProperty("user.dir"), "icons", "shiftLeft.png");
    Path shiftRightIconPath = Paths.get(System.getProperty("user.dir"), "icons", "shiftRight.png");
    Path findIconPath = Paths.get(System.getProperty("user.dir"), "icons", "find.png");
    Path splitIconPath = Paths.get(System.getProperty("user.dir"), "icons", "split.png");
    Path undoIconPath = Paths.get(System.getProperty("user.dir"), "icons", "undo.png");
    Path redoIconPath = Paths.get(System.getProperty("user.dir"), "icons", "redo.png");

    BufferedReader infile;
    final String PROP_COMMENT = "Copy files between IBM i and PC, edit and compile.";
    final String SHORT_CARET = "Short caret";
    final String LONG_CARET = "Long caret";
    final String VERTICAL_SELECTION = "Vertical selection";
    final String HORIZONTAL_SELECTION = "Horizontal selection";
    final int TAB_SIZE = 4;
    final String NEW_LINE = "\n";
    String encoding = System.getProperty("file.encoding", "UTF-8");
    Properties properties;
    String userName;
    String overWriteFile;
    String pcCharset;
    String ibmCcsid;
    int ibmCcsidInt;
    int sourceCcsidInt;
    int ccsidAttribute;
    String editorFont;
    String[] fontNamesMac = {
        "Monospaced",
        "Courier",
        "Courier New",
        "Monaco",
        "Lucida Sans Typewriter",
        "Andale Mono",
        "Ayuthaya",
        "Menlo",
        "PT Mono",};
    String[] fontNamesWin = {
        "Monospaced",
        "Consolas",
        "Courier New",
        "DialogInput",
        "Lucida Console",
        "Lucida Sans Typewriter",
        "MS Gothic",
        "Source Code Pro",};
    String fontSizeString;
    int fontSize;
    String caretShape;
    String selectionMode;
    SpecialCaret specialCaret;
    SpecialCaret2 specialCaret2;
    LongCaret longCaret;
    LongCaret2 longCaret2;
    BasicTextUI.BasicCaret basicCaret;
    BasicTextUI.BasicCaret basicCaret2;

    CaretListener caretListener;

    ArrayList<Integer> selectionStarts = new ArrayList<>();
    ArrayList<Integer> selectionEnds = new ArrayList<>();

    int startSel;
    int endSel;
    String selectedText;
    String[] selectedArray;
    int caretPosition;
    int selectionStart;
    String shiftedText;

    String msgText;
    String qsyslib;
    String libraryName;
    String fileName;
    String memberName;

    String textLine;
    List<String> list;

    String row;
    boolean nodes = true;
    boolean noNodes = false;
    boolean isError = false;
    JScrollPane scrollPane;

    // Constructor parameters
    AS400 remoteServer;
    MainWindow mainWindow;
    String filePathString;
    SequentialFile outSeqFile;
    String methodName;
    String sourceType;

    // Highlighting blocks of paired statements (if - endif, dow - enddo, etc.)
    ArrayList<String> stmtsBeg = new ArrayList<>();
    ArrayList<String> stmtsEnd = new ArrayList<>();

    String operatingSystem;

    static boolean textChanged;

    /**
     * Constructor
     *
     * @param remoteServer
     * @param mainWindow
     * @param textArea
     * @param textArea2
     * @param filePathString
     * @param methodName
     */
    public EditFile(AS400 remoteServer, MainWindow mainWindow,
            JTextArea textArea, JTextArea textArea2, String filePathString, String methodName) {
        this.remoteServer = remoteServer;
        this.mainWindow = mainWindow;
        this.textArea = textArea;
        this.textArea2 = textArea2;
        this.filePathString = filePathString;
        this.methodName = methodName;

        // Create object of FindWindow class 
        findWindow = new FindWindow(this, filePathString);

        Properties sysProp = System.getProperties();
        if (sysProp.get("os.name").toString().toUpperCase().contains("MAC")) {
            operatingSystem = "MAC";
            // Adds Mac items to combo box
            for (String str : fontNamesMac) {
                fontComboBox.addItem(str);
            }
        } else if (sysProp.get("os.name").toString().toUpperCase().contains("WINDOWS")) {
            operatingSystem = "WINDOWS";
            // Adds Windows items to combo box
            for (String str : fontNamesWin) {
                fontComboBox.addItem(str);
            }
        }

        properties = new Properties();
        try {
            infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
            properties.load(infile);
            infile.close();
            overWriteFile = properties.getProperty("OVERWRITE_FILE");
            pcCharset = properties.getProperty("PC_CHARSET");
            ibmCcsid = properties.getProperty("IBM_CCSID");
            caretShape = properties.getProperty("CARET");
            selectionMode = properties.getProperty("SELECTION_MODE");
            editorFont = properties.getProperty("EDITOR_FONT");
            fontSizeString = properties.getProperty("EDITOR_FONT_SIZE");
            progLanguage = properties.getProperty("HIGHLIGHT_BLOCKS");
            userName = properties.getProperty("USERNAME");
            try {
                fontSize = Integer.parseInt(fontSizeString);
            } catch (Exception exc) {
                exc.printStackTrace();
                fontSizeString = "12";
                fontSize = 12;
            }
            try {
                ibmCcsidInt = Integer.parseInt(ibmCcsid);
            } catch (Exception exc) {
                // exc.printStackTrace();
                ibmCcsidInt = 500;
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }

        textArea.setFont(new Font(editorFont, Font.PLAIN, fontSize));
        textArea2.setFont(new Font(editorFont, Font.PLAIN, fontSize));

        isError = false;
            // Get text from the file and set it to the textArea
            if (methodName.equals("rewritePcFile")) {
                displayPcFile();
            } else if (methodName.equals("rewriteIfsFile")) {
                displayIfsFile();
            } else if (methodName.equals("rewriteSourceMember")) {
                displaySourceMember();
            }

            // Create window if there was no error in rewriting the file.
            // -------------
            if (!isError) {
                createWindow();
            } else {
                row = "Error: Connection lost.";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages(nodes);
                // Remove message scroll listener (cancel scrolling to the last message)
                mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
           }

        // Continue constructor
        // --------------------
        // Set caret position for the first time.
        textArea.setCaretPosition(0);
        caretPosition = textArea.getCaretPosition();
        textArea.requestFocus();

        // Prepare editing for different file types in primary text area.
        if (methodName.equals("rewritePcFile")) {
            scrollPane.setBackground(VERY_LIGHT_PINK);
            textArea.setBackground(VERY_LIGHT_PINK);
            // Prepare editing and make editor visible
            prepareEditingAndShow();
            row = "Info: PC file  " + filePathString + "  is displayed using character set  "
                    + pcCharset + "  from the application parameter.";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes);
            // Remove message scroll listener (cancel scrolling to the last message)
            mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
        } else if (methodName.equals("rewriteIfsFile")) {
            // Prepare editing and make editor visible
            prepareEditingAndShow();
            row = "Info: IFS file  " + filePathString + "  has CCSID  " + ccsidAttribute + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes);
        } else if (methodName.equals("rewriteSourceMember")) {
            // Prepare editing and make editor visible
            prepareEditingAndShow();
            row = "Info: Source member  " + filePathString + "  has CCSID  " + sourceCcsidInt + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes);
        }

    } // End of constructor

    /**
     * Create window method.
     */
    protected void createWindow() {

        Toolkit kit = Toolkit.getDefaultToolkit();
        Dimension screenSize = kit.getScreenSize();

        screenWidth = screenSize.width;
        screenHeight = screenSize.height;
        windowWidth = 850;
        windowHeight = screenHeight;

        windowX = screenWidth / 2 - windowWidth / 2;
        windowY = 0;

        menuBar = new JMenuBar();
        helpMenu = new JMenu("Help");
        helpMenuItemEN = new JMenuItem("Help English");
        helpMenuItemCZ = new JMenuItem("Nápověda česky");
        helpMenuItemRPGIII = new JMenuItem("RPG III forms");
        helpMenuItemRPGIV = new JMenuItem("RPG IV forms");
        helpMenuItemCOBOL = new JMenuItem("COBOL form");
        helpMenuItemDDS = new JMenuItem("DDS forms");

        helpMenu.add(helpMenuItemEN);
        helpMenu.add(helpMenuItemCZ);
        helpMenu.add(helpMenuItemRPGIII);
        helpMenu.add(helpMenuItemRPGIV);
        helpMenu.add(helpMenuItemCOBOL);
        helpMenu.add(helpMenuItemDDS);
        menuBar.add(helpMenu);

        editMenu = new JMenu("Edit");
        menuUndo = new JMenuItem("Undo");
        menuUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
        menuRedo = new JMenuItem("Redo");
        menuRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.CTRL_MASK));
        menuCut = new JMenuItem("Cut");
        menuCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
        menuCopy = new JMenuItem("Copy");
        menuCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        menuPaste = new JMenuItem("Paste");
        menuPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
        menuDelete = new JMenuItem("Delete");
        menuDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        menuDelete2 = new JMenuItem("Delete");
        menuDelete2.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
        menuFind = new JMenuItem("Find");
        menuFind.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK));
        menuBar.add(editMenu);

        editMenu.add(menuUndo);
        editMenu.add(menuRedo);
        editMenu.addSeparator();
        editMenu.add(menuCut);
        editMenu.add(menuCopy);
        editMenu.add(menuPaste);
        editMenu.addSeparator();
        editMenu.add(menuDelete);
        editMenu.add(menuDelete2);
        editMenu.addSeparator();
        editMenu.add(menuFind);

        setJMenuBar(menuBar); // In macOS on the main system menu bar above, in Windows on the window menu bar

        originalButtonBackground = new JButton().getBackground();
        originalButtonForeground = new JButton().getForeground();

        saveButton.setPreferredSize(new Dimension(80, 20));
        saveButton.setMinimumSize(new Dimension(80, 20));
        saveButton.setMaximumSize(new Dimension(80, 20));
        saveButton.setToolTipText("Also Ctrl+S (Cmd+S in macOS).");
        saveButton.setFont(saveButton.getFont().deriveFont(Font.BOLD, 12));

        // Save button will have the original black color.
        textChanged = false;

        undoButton.setPreferredSize(new Dimension(60, 20));
        undoButton.setMinimumSize(new Dimension(60, 20));
        undoButton.setMaximumSize(new Dimension(60, 20));

        redoButton.setPreferredSize(new Dimension(60, 20));
        redoButton.setMinimumSize(new Dimension(60, 20));
        redoButton.setMaximumSize(new Dimension(60, 20));

        caretButton.setPreferredSize(new Dimension(90, 20));
        caretButton.setMinimumSize(new Dimension(90, 20));
        caretButton.setMaximumSize(new Dimension(90, 20));
        caretButton.setToolTipText("Toggle short or long caret. Also right click in text area.");

        selectionModeButton.setPreferredSize(new Dimension(150, 20));
        selectionModeButton.setMinimumSize(new Dimension(150, 20));
        selectionModeButton.setMaximumSize(new Dimension(150, 20));
        selectionModeButton.setToolTipText("Toggle horizontal or vertical selection. Also right click in text area.");

        // Set selection mode as the button text
        selectionModeButton.setText(selectionMode);

        compileButton.setPreferredSize(new Dimension(90, 20));
        compileButton.setMinimumSize(new Dimension(90, 20));
        compileButton.setMaximumSize(new Dimension(90, 20));
        compileButton.setFont(compileButton.getFont().deriveFont(Font.BOLD, 12));
        compileButton.setToolTipText("Open window with compile settings.");

        fontComboBox.setPreferredSize(new Dimension(140, 20));
        fontComboBox.setMaximumSize(new Dimension(140, 20));
        fontComboBox.setMinimumSize(new Dimension(140, 20));
        fontComboBox.setToolTipText("Choose font.");

        // Sets the current editor font item into the input field of the combo box
        fontComboBox.setSelectedItem(editorFont);

        // This class assigns the corresponding fonts to the font names in the combo box list
        fontComboBox.setRenderer(new FontComboBoxRenderer());

        fontSizeField.setText(fontSizeString);
        fontSizeField.setPreferredSize(new Dimension(30, 20));
        fontSizeField.setMaximumSize(new Dimension(30, 20));
        fontSizeField.setToolTipText("Enter font size.");

        characterSetLabel.setForeground(DIM_BLUE);

        highlightBlocksLabel.setToolTipText("Blocks are compound statements like IF - ENDIF, etc.");

        languageComboBox.setPreferredSize(new Dimension(130, 20));
        languageComboBox.setMaximumSize(new Dimension(130, 20));
        languageComboBox.setMinimumSize(new Dimension(130, 20));
        languageComboBox.setToolTipText("Highlight blocks. Choose programming language. Blocks are compound statements like IF - ENDIF, etc.");
        languageComboBox.addItem("*NONE");
        languageComboBox.addItem("*ALL");
        languageComboBox.addItem("RPG **FREE");
        languageComboBox.addItem("RPG /FREE");
        languageComboBox.addItem("RPG IV fixed");
        languageComboBox.addItem("RPG III");
        languageComboBox.addItem("COBOL");
        languageComboBox.addItem("CL");
        languageComboBox.addItem("C");
        languageComboBox.addItem("C++");
        languageComboBox.addItem("SQL");

        languageComboBox.setSelectedItem(progLanguage);

        // Shift left icon and button
        ImageIcon shiftLeftImageIcon = new ImageIcon(shiftLeftIconPath.toString());
        leftShiftButton = new JButton(shiftLeftImageIcon);
        leftShiftButton.setToolTipText("Shift selection left. Also Ctrl+⬅ (Cmd+⬅ in macOS).");
        leftShiftButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        leftShiftButton.setContentAreaFilled(false);
        leftShiftButton.setPreferredSize(new Dimension(20, 20));
        leftShiftButton.setMinimumSize(new Dimension(20, 20));
        leftShiftButton.setMaximumSize(new Dimension(20, 20));

        ImageIcon shiftRightImageIcon = new ImageIcon(shiftRightIconPath.toString());
        rightShiftButton = new JButton(shiftRightImageIcon);
        rightShiftButton.setToolTipText("Shift selection right. Also Ctrl+➜(Cmd+➜ in macOS).");
        rightShiftButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        rightShiftButton.setContentAreaFilled(false);
        rightShiftButton.setPreferredSize(new Dimension(20, 20));
        rightShiftButton.setMinimumSize(new Dimension(20, 20));
        rightShiftButton.setMaximumSize(new Dimension(20, 20));

        // Magnifying glass icon and button
        ImageIcon findImageIcon = new ImageIcon(findIconPath.toString());
        findButton = new JButton(findImageIcon);
        findButton.setToolTipText("Find and replace text. Also Ctrl+F (Cmd+F in macOS).");
        findButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        findButton.setContentAreaFilled(false);
        findButton.setPreferredSize(new Dimension(20, 20));
        findButton.setMinimumSize(new Dimension(20, 20));
        findButton.setMaximumSize(new Dimension(20, 20));

        // Split icon and button
        ImageIcon splitImageIcon = new ImageIcon(splitIconPath.toString());
        splitUnsplitButton = new JButton(splitImageIcon);
        splitUnsplitButton.setToolTipText("Split/unsplit editor area.");
        splitUnsplitButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        splitUnsplitButton.setContentAreaFilled(false);
        splitUnsplitButton.setPreferredSize(new Dimension(20, 20));
        splitUnsplitButton.setMinimumSize(new Dimension(20, 20));
        splitUnsplitButton.setMaximumSize(new Dimension(20, 20));

        // Undo icon and button
        ImageIcon undoImageIcon = new ImageIcon(undoIconPath.toString());
        undoButton = new JButton(undoImageIcon);
        undoButton.setToolTipText("Undo. Also Ctrl+Z (Cmd+Z in macOS).");
        undoButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        undoButton.setContentAreaFilled(false);
        undoButton.setPreferredSize(new Dimension(20, 20));
        undoButton.setMinimumSize(new Dimension(20, 20));
        undoButton.setMaximumSize(new Dimension(20, 20));

        // Redo icon and button
        ImageIcon redoImageIcon = new ImageIcon(redoIconPath.toString());
        redoButton = new JButton(redoImageIcon);
        redoButton.setToolTipText("Redo. Also Ctrl+Y (Cmd+Y in macOS).");
        redoButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        redoButton.setContentAreaFilled(false);
        redoButton.setPreferredSize(new Dimension(20, 20));
        redoButton.setMinimumSize(new Dimension(20, 20));
        redoButton.setMaximumSize(new Dimension(20, 20));

        // Split pane (divided by horizontal line) containing two scroll panes
        splitVerticalPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // Build editor area
        // =================

        textArea.setEditable(true);

        textArea.setFont(new Font(editorFont, Font.PLAIN, fontSize));
        textArea.setTabSize(TAB_SIZE);

        textArea2.setFont(new Font(editorFont, Font.PLAIN, fontSize));
        textArea2.setTabSize(TAB_SIZE);

        textArea.setDragEnabled(true);

        // Create a scroll pane
        scrollPane = new JScrollPane(textArea);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Light sky blue for IBM i (PC color is resolved in the constructor later).
        scrollPane.setBackground(VERY_LIGHT_BLUE);
        textArea.setBackground(VERY_LIGHT_BLUE);

        // Now the scroll pane may be sized because window height is defined
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        scrollPane.setPreferredSize(new Dimension(windowWidth, windowHeight));

        // Custom deletion will be active in VERTICAL selection mode only.
        if (selectionMode.equals(VERTICAL_SELECTION)) {
            // Activate custom deletion by Delete or Backspace key
            textArea.getInputMap(JComponent.WHEN_FOCUSED)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteDel");
            textArea.getActionMap().put("deleteDel", new CustomDelete("DEL"));
            textArea.getInputMap(JComponent.WHEN_FOCUSED)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "deleteBcksp");
            textArea.getActionMap().put("deleteBcksp", new CustomDelete("BACKSPACE"));
            textArea2.getInputMap(JComponent.WHEN_FOCUSED)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteDel");
            textArea2.getActionMap().put("deleteDel", new CustomDelete("DEL"));
            textArea2.getInputMap(JComponent.WHEN_FOCUSED)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "deleteBcksp");
            textArea2.getActionMap().put("deleteBcksp", new CustomDelete("BACKSPACE"));
        } else {
            // Deactivate custom deletion in horizontal mode
            textArea.getInputMap(JComponent.WHEN_FOCUSED)
                    .remove(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
            textArea.getInputMap(JComponent.WHEN_FOCUSED)
                    .remove(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
            textArea.getActionMap().remove("deleteDel");
            textArea.getActionMap().remove("deleteBcksp");
            textArea2.getInputMap(JComponent.WHEN_FOCUSED)
                    .remove(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
            textArea2.getInputMap(JComponent.WHEN_FOCUSED)
                    .remove(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
            textArea2.getActionMap().remove("deleteDel");
            textArea2.getActionMap().remove("deleteBcksp");
        }

        rowPanel1 = new JPanel();
        rowPanel1.setLayout(new BoxLayout(rowPanel1, BoxLayout.X_AXIS));
        rowPanel1.add(caretButton);
        rowPanel1.add(Box.createHorizontalStrut(10));
        rowPanel1.add(selectionModeButton);
        rowPanel1.add(Box.createHorizontalStrut(10));
        // rowPanel1.add(shiftLabel);
        rowPanel1.add(leftShiftButton);
        rowPanel1.add(Box.createHorizontalStrut(10));
        rowPanel1.add(rightShiftButton);
        rowPanel1.add(Box.createHorizontalStrut(20));
        rowPanel1.add(undoButton);
        rowPanel1.add(Box.createHorizontalStrut(10));
        rowPanel1.add(redoButton);
        rowPanel1.add(Box.createHorizontalStrut(20));
        rowPanel1.add(saveButton);
        // Compile button is not available in PC. 
        if (!methodName.equals("rewritePcFile")) {
            rowPanel1.add(Box.createHorizontalStrut(20));
            rowPanel1.add(compileButton);
        }

//        saveButton.setSelected(true);

        rowPanel2 = new JPanel();
        GroupLayout rowPanel2Layout = new GroupLayout(rowPanel2);
        rowPanel2Layout.setHorizontalGroup(rowPanel2Layout.createSequentialGroup()
                .addGap(0)
                .addComponent(splitUnsplitButton)
                .addGap(20)
                .addComponent(fontComboBox)
                .addComponent(fontSizeField)
                .addGap(20)
                .addComponent(languageComboBox)
                .addGap(40)
                .addComponent(findButton)
                .addGap(60)
                .addComponent(characterSetLabel)
        );
        rowPanel2Layout.setVerticalGroup(rowPanel2Layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(splitUnsplitButton)
                .addComponent(fontComboBox)
                .addComponent(fontSizeField)
                .addComponent(languageComboBox)
                .addComponent(findButton)
                .addComponent(characterSetLabel)
        );
        rowPanel2.setLayout(rowPanel2Layout);

        globalPanel = new JPanel();
        GroupLayout topPanelLayout = new GroupLayout(globalPanel);
        topPanelLayout.setHorizontalGroup(topPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(rowPanel2)
                .addComponent(rowPanel1)
                .addComponent(scrollPane)
        );
        topPanelLayout.setVerticalGroup(topPanelLayout.createSequentialGroup()
                .addComponent(rowPanel2)
                .addGap(2)
                .addComponent(rowPanel1)
                .addGap(4)
                .addComponent(scrollPane)
        );
        globalPanel.setLayout(topPanelLayout);

        globalPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(globalPanel);

        // Display the window.
        setSize(windowWidth, windowHeight);
        setLocation(windowX, windowY);
        //pack();
        setVisible(true);

        // Register listeners
        // ==================
        TextAreaInitDocListener textAreaInitDocListener = new TextAreaInitDocListener();
        textArea.getDocument().addDocumentListener(textAreaInitDocListener);

        // Listener for undoable edits
        textArea.getDocument().addUndoableEditListener(undoHandler);

        // Register HelpWindow menu item listener
        helpMenuItemEN.addActionListener(ae -> {
            String command = ae.getActionCommand();
            if (command.equals("Help English")) {
                if (Desktop.isDesktopSupported()) {
                    String uri = Paths
                            .get(System.getProperty("user.dir"), "helpfiles", "IBMiProgTool_doc_EN.pdf").toString();
                    // Replace backslashes by forward slashes in Windows
                    uri = uri.replace('\\', '/');
                    uri = uri.replace(" ", "%20");
                    try {
                        // Invoke the standard browser in the operating system
                        Desktop.getDesktop().browse(new URI("file://" + uri));
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
                    uri = uri.replace('\\', '/');
                    uri = uri.replace(" ", "%20");
                    try {
                        // Invoke the standard browser in the operating system
                        Desktop.getDesktop().browse(new URI("file://" + uri));
                    } catch (Exception exc) {
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
                    uri = uri.replace('\\', '/');
                    uri = uri.replace(" ", "%20");
                    try {
                        // Invoke the standard browser in the operating system
                        Desktop.getDesktop().browse(new URI("file://" + uri));
                    } catch (Exception exc) {
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
                    uri = uri.replace('\\', '/');
                    uri = uri.replace(" ", "%20");
                    try {
                        // Invoke the standard browser in the operating system
                        Desktop.getDesktop().browse(new URI("file://" + uri));
                    } catch (Exception exc) {
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
                    uri = uri.replace('\\', '/');
                    uri = uri.replace(" ", "%20");
                    try {
                        // Invoke the standard browser in the operating system
                        Desktop.getDesktop().browse(new URI("file://" + uri));
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                }
            }
        });

        // Register HelpWindow menu item listener
        helpMenuItemDDS.addActionListener(ae -> {
            String command = ae.getActionCommand();
            if (command.equals("DDS forms")) {
                if (Desktop.isDesktopSupported()) {
                    String uri = Paths
                            .get(System.getProperty("user.dir"), "helpfiles", "DDS_forms.pdf").toString();
                    // Replace backslashes by forward slashes in Windows
                    uri = uri.replace('\\', '/');
                    uri = uri.replace(" ", "%20");
                    try {
                        // Invoke the standard browser in the operating system
                        Desktop.getDesktop().browse(new URI("file://" + uri));
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                }
            }
        });

        // Select editor font from the list in combo box - listener
        // --------------------------------------------------------
        fontComboBox.addItemListener(il -> {
            int currentCaretPos = textArea.getCaretPosition();
            JComboBox<String> source = (JComboBox) il.getSource();
            fontSizeString = fontSizeField.getText();
            try {
                fontSize = Integer.parseInt(fontSizeString);
            } catch (Exception exc) {
                exc.printStackTrace();
                fontSizeString = "12";
                fontSize = 12;
            }
            editorFont = (String) fontComboBox.getSelectedItem();
            textArea.setFont(new Font(editorFont, Font.PLAIN, fontSize));
            textArea2.setFont(new Font(editorFont, Font.PLAIN, fontSize));
            try {
                BufferedWriter outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                // Save programming language into properties
                properties.setProperty("EDITOR_FONT", editorFont);
                properties.setProperty("EDITOR_FONT_SIZE", fontSizeString);
                properties.store(outfile, PROP_COMMENT);
                outfile.close();
            } catch (Exception exc) {
                exc.printStackTrace();
            }
            // Prepare text area with highlighting blocks and show
            prepareEditingAndShow();
            textArea.requestFocusInWindow();
            textArea.setCaretPosition(currentCaretPos);
        });

        // "Font size" field listener
        // --------------------------
        fontSizeField.addActionListener(al -> {
            int currentCaretPos = textArea.getCaretPosition();
            fontSizeString = fontSizeField.getText();
            try {
                fontSize = Integer.parseInt(fontSizeString);
            } catch (Exception exc) {
                exc.printStackTrace();
                fontSizeString = "12";
                fontSize = 12;
            }
            fontSizeField.setText(fontSizeString);
            textArea.setFont(new Font(editorFont, Font.PLAIN, fontSize));
            textArea2.setFont(new Font(editorFont, Font.PLAIN, fontSize));
            try {
                BufferedWriter outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                // Save font size into properties
                properties.setProperty("EDITOR_FONT", editorFont);
                properties.setProperty("EDITOR_FONT_SIZE", fontSizeString);
                properties.store(outfile, PROP_COMMENT);
                outfile.close();
            } catch (Exception exc) {
                exc.printStackTrace();
            }
            // Prepare text area with highlighting blocks and show
            //prepareEditingAndShow();
            textArea.requestFocusInWindow();
            textArea.setCaretPosition(currentCaretPos);

        });

        // Select programming language from the list in combo box - listener
        // -----------------------------------------------------------------
        languageComboBox.addItemListener(il -> {
            // Remember caret position
            int currentCaretPos = textArea.getCaretPosition();
            JComboBox<String> source = (JComboBox) il.getSource();
            progLanguage = (String) source.getSelectedItem();
            // Highlight possible matched patterns in both primary and secondary areas 
            changeHighlight();
            changeHighlight2();
            try {
                BufferedWriter outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                // Save programming language into properties
                properties.setProperty("HIGHLIGHT_BLOCKS", progLanguage);
                properties.store(outfile, PROP_COMMENT);
                outfile.close();
            } catch (Exception exc) {
                exc.printStackTrace();
            }
            // Prepare both text areas:
            // - Set caret position in primary text area.
            // - Highlight blocks.
            // - Update progLanguage property in parameters.
            prepareEditingAndShow();

            textArea.requestFocusInWindow();
            // Set remembered caret position 
            textArea.setCaretPosition(currentCaretPos);
        });

        // Caret button and popup menu listeners
        // =====================================
        toggleCaret.addActionListener(ae -> {
            changeCaretShape();
        });
        caretButton.addActionListener(ae -> {
            changeCaretShape();
        });

        // Selection mode listeners
        // ========================
        selectionModeButton.addActionListener(ae -> {
            changeSelectionMode();
        });
        changeSelMode.addActionListener(ae -> {
            changeSelectionMode();
        });

        // Find button listener
        // --------------------
        CreateFindWindow createFindWindow = new CreateFindWindow();
        findButton.addActionListener(createFindWindow);

        // Split/Unsplit button listener
        // -----------------------------
        splitUnsplitButton.addActionListener(ae -> {
            if (!textAreaIsSplit) {
                caretPosition = textArea.getCaretPosition();
                splitTextArea();
                textAreaIsSplit = true;
            } else if (textAreaIsSplit) {
                unsplitTextArea();
                textAreaIsSplit = false;
            }
        });

        // Undo button listener and menu item listener
        undoAction = new UndoAction();
        undoButton.addActionListener(undoAction);
        menuUndo.addActionListener(undoAction);

        // Redo button listener and menu item listener
        redoAction = new RedoAction();
        redoButton.addActionListener(redoAction);
        menuRedo.addActionListener(redoAction);

        // Cut menu item listener
        CustomCut customCut = new CustomCut();
        menuCut.addActionListener(customCut);

        // Copy menu item listener
        CustomCopy customCopy = new CustomCopy();
        menuCopy.addActionListener(customCopy);

        // Paste menu item listener
        CustomPaste customPaste = new CustomPaste();
        menuPaste.addActionListener(customPaste);

        // Delete DEL menu item listener
        CustomDelete customDelete = new CustomDelete("DEL");
        menuDelete.addActionListener(customDelete);

        // Delet BACKSPACE menu item listener
        CustomDelete customDelete2 = new CustomDelete("BACKSPACE");
        menuDelete2.addActionListener(customDelete2);

        // Find menu item listener
        CreateFindWindow findWindow = new CreateFindWindow();
        menuFind.addActionListener(findWindow);

        // Save button listener
        // --------------------
        SaveAction saveAction = new SaveAction();
        saveButton.setToolTipText("Also Ctrl+S (Cmd+S in macOS).");
        saveButton.addActionListener(saveAction);

        // Left shift button listener
        // --------------------------
        ArrowLeft arrowLeft = new ArrowLeft();
        leftShiftButton.addActionListener(arrowLeft);

        // Right shift button listener
        // ---------------------------
        ArrowRight arrowRight = new ArrowRight();
        rightShiftButton.addActionListener(arrowRight);

        // Compile button listener
        // -----------------------
        compileButtonListener = new CompileButtonListener();
        compileButton.addActionListener(compileButtonListener);

        // Keyboard key listeners
        // ----------------------
        // Enable ESCAPE key to escape from editing
        globalPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "escapeCmd");
        globalPanel.getActionMap().put("escapeCmd", new Escape());

        // Enable processing of function key Ctrl + S = Save data
        globalPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "save");
        globalPanel.getActionMap().put("save", saveAction);
        textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "save");
        textArea.getActionMap().put("save", saveAction);
        textArea2.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "save");
        textArea2.getActionMap().put("save", saveAction);

        // Enable processing of function key Ctrl + Z = Undo
        globalPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "undo");
        globalPanel.getActionMap().put("undo", undoAction);
        textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "undo");
        textArea.getActionMap().put("undo", undoAction);
        textArea2.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "undo");
        textArea2.getActionMap().put("undo", undoAction);

        // Enable processing of function key Ctrl + Y = Redo
        globalPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "redo");
        globalPanel.getActionMap().put("redo", redoAction);
        textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "redo");
        textArea.getActionMap().put("redo", redoAction);
        textArea2.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "redo");
        textArea2.getActionMap().put("redo", redoAction);

        // Enable processing of function key Ctrl + F = create FindWidnow
        globalPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "crtFindWindow");
        globalPanel.getActionMap().put("crtFindWindow", createFindWindow);

        // Enable processing of function key Ctrl + F = create FindWidnow
        textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "crtFindWindow");
        textArea.getActionMap().put("crtFindWindow", createFindWindow);
        // Enable processing of function key Ctrl + F = create FindWidnow
        textArea2.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "crtFindWindow");
        textArea2.getActionMap().put("crtFindWindow", createFindWindow);

        // Enable processing of Tab key
        globalPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("TAB"), "tab");
        globalPanel.getActionMap().put("tab", new TabListener());

        // Enable processing of function key Ctrl + Arrow Left = Shift lines or rectangle left
        textArea.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "shiftLeft");
        textArea.getActionMap().put("shiftLeft", arrowLeft);
        // Enable processing of function key Ctrl + Arrow Right = Shift lines or rectangle right
        textArea.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "shiftRight");
        textArea.getActionMap().put("shiftRight", arrowRight);

        // Enable processing of function key Ctrl + Arrow Left = Shift lines or rectangle left
        textArea2.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "shiftLeft");
        textArea2.getActionMap().put("shiftLeft", arrowLeft);
        // Enable processing of function key Ctrl + Arrow Right = Shift lines or rectangle right
        textArea2.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "shiftRight");
        textArea2.getActionMap().put("shiftRight", arrowRight);

        // Enable custom processing of function key Ctrl C = Custom copy
        textArea.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK), "copy");
        textArea.getActionMap().put("copy", new CustomCopy());
        // Enable custom processing of function key Ctrl X = Custom cut
        textArea.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK), "cut");
        textArea.getActionMap().put("cut", new CustomCut());
        // Enable custom processing of function key Ctrl V = Custom paste
        textArea.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK), "paste");
        textArea.getActionMap().put("paste", new CustomPaste());

        // Enable custom processing of function key Ctrl C = Custom copy
        textArea2.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK), "copy");
        textArea2.getActionMap().put("copy", new CustomCopy());
        // Enable custom processing of function key Ctrl X = Custom cut
        textArea2.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK), "cut");
        textArea2.getActionMap().put("cut", new CustomCut());
        // Enable custom processing of function key Ctrl V = Custom paste
        textArea2.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK), "paste");
        textArea2.getActionMap().put("paste", new CustomPaste());

        // Mouse listeners for text areas
        // ------------------------------
        textAreaMouseListener = new TextAreaMouseListener();
        textArea.addMouseListener(textAreaMouseListener);
        textArea2MouseListener = new TextArea2MouseListener();
        textArea2.addMouseListener(textArea2MouseListener);

        // Register window listener
        // ------------------------
        windowEditFileListener = new WindowEditFileAdapter();
        this.addWindowListener(windowEditFileListener);

        {
            // IMPORTANT: This block must be run AFTER MOUSE LISTENER REGISTRATION
            // so that double click select a word with the BASIC caret.

            // Choose initial caret shape
            // --------------------------
            // Special caret for vertical (rectangular) selection mode
            specialCaret = new SpecialCaret();
            specialCaret2 = new SpecialCaret2();
            // Long caret for horizontal selection mode
            longCaret = new LongCaret();
            longCaret2 = new LongCaret2();

            basicCaret = new BasicTextUI.BasicCaret(); // Short catet for primary text area
            basicCaret2 = new BasicTextUI.BasicCaret(); // Short caret for secondary text area

            // Caret button with short or long caret
            caretButton.setText(caretShape); // Caret shape from parameters
            // The following settings for primary text area (textArea) will do. 
            // The secondary textArea2 will be assigned the caret in the "splitUnsplitButton" listener.
            if (caretShape.equals(LONG_CARET)) {
                if (selectionMode.equals(HORIZONTAL_SELECTION)) {
                    // Horizontal selection
                    // Set custom caret - long vertical gray line with a short red pointer
                    textArea.setCaret(longCaret);
                } else {
                    // Vertical selection
                    // Set custom caret - long vertical gray line with a short red pointer
                    textArea.setCaret(specialCaret);
                }
            } else {
                if (selectionMode.equals(HORIZONTAL_SELECTION)) {
                    // Horizontal selection
                    // For short caret set basic caret - a short vertical line
                    textArea.setCaret(basicCaret);
                } else {
                    // Vertical selection
                    // Set custom caret - long vertical gray line with a short red pointer
                    textArea.setCaret(specialCaret);
                }
            }
        } // end of block
    }

    /**
     * Display contents of the IFS file using its CCSID attribute.
     */
    protected void displayIfsFile() {
        this.setTitle("Edit IFS file  '" + filePathString + "'");

        // Contents of the file are always decoded according to its attributed CCSID.
        // Characters may be displayed incorrectly if the "IBMi CCSID" parameter
        // does not correspond to the file's attributed CCSID.
        // Correct the parameter "IBMi CCSID".
        try {
            IFSFile ifsFile = new IFSFile(remoteServer, filePathString);

            ccsidAttribute = ifsFile.getCCSID();
            characterSetLabel.setText("CCSID " + ccsidAttribute + " was used for display.");

            byte[] inputBuffer = new byte[1000000];
            byte[] workBuffer = new byte[1000000];
            textArea.setText("");

            //System.out.println("displayIfsFile - ccsidAttribute: " + ccsidAttribute);

            try (IFSFileInputStream inputStream = new IFSFileInputStream(remoteServer, filePathString)) {
                int bytesRead = inputStream.read(inputBuffer);
                while (bytesRead != -1) {
                    for (int idx = 0; idx < bytesRead; idx++) {
                        // Copy input byte to output byte
                        workBuffer[idx] = inputBuffer[idx];
                    }
                    // Copy the printable part of the work array
                    // to a new buffer that will be written out.
                    byte[] bufferToWrite = new byte[bytesRead];
                    // Copy bytes from the work buffer to the new buffer
                    for (int indx = 0; indx < bytesRead; indx++) {
                        bufferToWrite[indx] = workBuffer[indx];
                    }
                    // Create object for conversion from bytes to characters
                    AS400Text textConverter = new AS400Text(bytesRead, ccsidAttribute, remoteServer);
                    // Convert byte array buffer to text line
                    textLine = (String) textConverter.toObject(bufferToWrite);
                    // Append the line to text area
                    textArea.append(textLine + NEW_LINE);
                    // System.out.println("displayIfsFile - bytesRead: " + bytesRead);
                    // Read next input buffer
                    bytesRead = inputStream.read(inputBuffer);
                }
                inputStream.close();

                // Print NEW_LINE in one (or two) hexadecimal character(s) depending on 
                // whether it is "\n" or "\r\n".
                //System.out.println("displayIfsFile - NEW_LINE: ");
                //for (int i = 0; i < NEW_LINE.length(); i++) {
                //    System.out.print(toHex(NEW_LINE.getBytes()[i]));
                //}
                //System.out.println();
            }
        } catch (Exception exc) {
            isError = true;
            row = "Error in displaying IFS file: " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes);
        }
        // Remove message scroll listener (cancel scrolling to the last message)
        mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
    }

    /**
     * Convert a byte to two "hexadecimal" characters (for testing)
     *
     * @param aByte
     * @return
     */
    static String toHex(byte aByte) {
        int bin = (aByte < 0) ? (256 + aByte) : aByte;
        int bin0 = bin >>> 4; // upper half-byte
        int bin1 = bin % 16;  // lower half-byte
        String hex = Integer.toHexString(bin0)
                + Integer.toHexString(bin1);
        return hex;
    }

    /**
     * Display PC file using the application parameter "pcCharset".
     */
    protected void displayPcFile() {

        this.setTitle("Edit PC file  '" + filePathString + "'");

        // Disable Compile button - compilation is impossible in PC
        compileButton.removeActionListener(compileButtonListener);

        try {
            Path filePath = Paths.get(filePathString);
            if (Files.exists(filePath)) {
                if (pcCharset.equals("*DEFAULT")) {
                    // Set ISO-8859-1 as a default
                    pcCharset = "ISO-8859-1";
                }
                characterSetLabel.setText(pcCharset + " character set was used for display.");
                // Use PC charset parameter for conversion
                textArea.setText("");



                // Input will be decoded using PC charset parameter.
                BufferedReader bufferedReader = Files.newBufferedReader(filePath, Charset.forName(pcCharset));
                textLine = bufferedReader.readLine();
                while (textLine != null) {
                    textLine += NEW_LINE;
                    textArea.append(textLine);
                    // Read next line
                    textLine = bufferedReader.readLine();
                }
                bufferedReader.close();


                //list = Files.readAllLines(filePath, Charset.forName(pcCharset));
                //if (list != null) {
                //    // Concatenate all text lines from the list obtained from the file
                //    textArea.setText("");
                //    Object[] obj = (Object[]) list.stream().toArray();
                //    for (int idx = 0; idx < obj.length; idx++) {
                //        String text = obj[idx].toString();
                //        textArea.append(text + NEW_LINE);
                //    }
                //}

            }
        } catch (Exception exc) {
            isError = true;
            exc.printStackTrace();
            row = "Error: File  " + filePathString
                    + "  is not a text file or has an unsuitable character set.  -  " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes); // do not add child nodes
            // Remove message scroll listener (cancel scrolling to the last message)
            mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
        }
    }

    /**
     * Display source member using its CCSID attribute; Only data part of the source record is translated
     * (to String - UTF-16).
     */
    @SuppressWarnings("UseSpecificCatch")
    Path tmpFilePath;

    protected void displaySourceMember() {

        this.setTitle("Edit member  '" + filePathString + "'");

        FileAttributes fileAttributes = new FileAttributes(remoteServer, filePathString);

        // Create an AS400FileRecordDescription object that represents the file
        AS400FileRecordDescription inRecDesc = new AS400FileRecordDescription(remoteServer, filePathString);

        // Set editability
        textArea.setEditable(true);
        textArea.setText("");
        try {
            sourceCcsidInt = fileAttributes.getCcsid();
            characterSetLabel.setText("CCSID " + sourceCcsidInt + " was used for display.");

            // Get list of record formats of the database file
            RecordFormat[] format = inRecDesc.retrieveRecordFormat();
            // Create an AS400File object that represents the file
            SequentialFile as400seqFile = new SequentialFile(remoteServer, filePathString);
            // Set the record format (the only one)
            as400seqFile.setRecordFormat(format[0]);

            // Open the source physical file member as a sequential file
            as400seqFile.open(AS400File.READ_ONLY, 0, AS400File.COMMIT_LOCK_LEVEL_NONE);

            // Read the first source member record
            Record inRecord = as400seqFile.readNext();

            // Write source records to the PC output text file.
            // --------------------
            while (inRecord != null) {
                StringBuilder textLine = new StringBuilder();

                // Prefix is not displayed because it must not be edited!!!
                // Source record is composed of three source record fields: seq.
                // number, date, source data.
                // -- DecimalFormat df1 = new DecimalFormat("0000.00");
                // -- DecimalFormat df2 = new DecimalFormat("000000");
                // Sequence number - 6 bytes String seq = df1.format((Number)
                // --inRecord.getField("SRCSEQ"));
                // String seq2 = seq.substring(0, 4) + seq.substring(5);
                // Date - 6 bytes
                // --String srcDat = df2.format((Number)
                // inRecord.getField("SRCDAT"));

                // Data from source record (the source line)
                byte[] bytes = inRecord.getFieldAsBytes("SRCDTA");

                // Create object for conversion from bytes to characters
                // Ignore "IBM i CCSID" parameter - display characters in the
                // member.
                AS400Text textConverter = new AS400Text(bytes.length, remoteServer);
                // Convert byte array buffer to text line (String - UTF-16)
                String translatedData = (String) textConverter.toObject(bytes);

                // Append translated data to text line
                textLine.append(translatedData).append(NEW_LINE);

                // Append text line to text area
                textArea.append(textLine.toString());

                // Read next source member record
                inRecord = as400seqFile.readNext();
            }
            // Close the file
            as400seqFile.close();
        } catch (Exception exc) {
            isError = true;
            exc.printStackTrace();
            row = "Error in displaying source member: " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes);
        }
        // Remove message scroll listener (cancel scrolling to the last message)
        mainWindow.scrollMessagePane.getVerticalScrollBar()
                .removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
    }

    /**
     * Write edited text back into the file; Decide what type of file it is.
     */
    protected void rewriteFile() {
        try {
            BufferedReader infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
            properties.load(infile);
            infile.close();
        } catch (Exception exc) {
            exc.printStackTrace();
        }

        pcCharset = properties.getProperty("PC_CHARSET");
        if (pcCharset.equals("*DEFAULT")) {
            pcCharset = "ISO-8859-1";
        }

        overWriteFile = properties.getProperty("OVERWRITE_FILE");

        if (overWriteFile.equals("Y")) {
            if (methodName.equals("rewriteIfsFile")) {
                rewriteIfsFile();
            } else if (methodName.equals("rewriteSourceMember")) {
                // Save edited data from text area back to the member
                rewriteSourceMember();
            } else if (methodName.equals("rewritePcFile")) {
                // Save edited data from text area back to the member
                rewritePcFile();
            }
        } else {
            row = "Error: IFS file  " + filePathString + "  cannot be rewritten. Overwriting files is not allowed.";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
        }
    }

    /**
     * Rewrite IFS file with edited text area.
     *
     * @return
     */
    protected String rewriteIfsFile() {
        try {
            IFSFileOutputStream outStream = new IFSFileOutputStream(remoteServer, filePathString);

            String textAreaString = textArea.getText();
            byte[] byteArray;
            int nbrOfBytes = 0;
            // Decide how long in bytes the line is given target encoding.
            if (ccsidAttribute == 1200 || ccsidAttribute == 13488) {
                // Get length in bytes for conversion to Unicode 1200 (UTF-16) and 13488 (UCS-2)
                nbrOfBytes = textAreaString.getBytes(Charset.forName("UTF-16")).length;;
                System.out.println("rewriteIfsFile - nbrOfBytes: " + nbrOfBytes);
            } else if (ccsidAttribute == 1208) {
                // Get length in bytes for conversion to Unicode 1208 (UTF-8)
                nbrOfBytes = textAreaString.getBytes(Charset.forName("UTF-8")).length;
                System.out.println("rewriteIfsFile - nbrOfBytes: " + nbrOfBytes);
            } else {
                // Get length of bytes of the text line for single byte characters
                nbrOfBytes = textAreaString.length();
            }
            AS400Text textConverter = new AS400Text(nbrOfBytes, ccsidAttribute, remoteServer);
            byteArray = textConverter.toBytes(textAreaString);
            // Write text from the text area to the file
            outStream.write(byteArray);
            // Close file
            outStream.close();

            row = "Comp: IFS file  " + filePathString + "  was saved.";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
            return "";

        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error in rewriting IFS file: " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
            return "ERROR";
        }
    }

    /**
     * Rewrite PC file with edited text area.
     *
     * @return
     */
    protected String rewritePcFile() {

        Path filePath = Paths.get(filePathString);

        try {
            // Open output text file
            BufferedWriter outputFile = Files.newBufferedWriter(filePath, Charset.forName(pcCharset),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            // Write contents of data area to the file
            outputFile.write(textArea.getText());
            // Close file
            outputFile.close();

            row = "Comp: PC file  " + filePathString + "  was saved with charset  " + pcCharset + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
            return "";

        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error in rewriting PC file: " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
            return "ERROR";
        }
    }

    /**
     * Rewrite source member with edited text area using an intermediate temporary IFS file; This method is fast enough.
     *
     * @return
     */
    protected String rewriteSourceMember() {

        // Extract individual names (libraryName, fileName, memberName) from the AS400 IFS path
        extractNamesFromIfsPath(filePathString);

        // Path to the output source member
        String outMemberPathString = "/QSYS.LIB/" + libraryName + ".LIB/" + fileName
                + ".FILE" + "/" + memberName + ".MBR";

        // Enable calling CL commands
        CommandCall cmdCall = new CommandCall(remoteServer);
        IFSFileOutputStream outStream = null;
        try {
            // String[] lines = textArea.getText().split("\n");

            // If overwrite is not allowed - return
            // ------------------------------------
            if (!properties.getProperty("OVERWRITE_FILE").equals("Y")) {
                row = "Info: Member  " + libraryName + "/" + fileName + "(" + memberName + ")   cannot be overwtitten. "
                        + " Overwriting files is not allowed.";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
                return "ERROR";
            }

            // Overwrite is allowed
            // --------------------

            // First create an IFS '/home/userName directory if it does not exist
            String home_userName = "/home/" + userName;
            IFSFile ifsDir = new IFSFile(remoteServer, home_userName);
            // Create new directory if it does not exist
            ifsDir.mkdir();

            // Create hidden temporary file (with leading dot) in the directory
            String tmpFileString = home_userName + "/.tmp" + Timestamp.valueOf(LocalDateTime.now()).toString();
            IFSFile ifsTmpFile = new IFSFile(remoteServer, tmpFileString);
            IFSFile ifsTmpFilePath = new IFSFile(remoteServer, tmpFileString);
            ifsTmpFile.createNewFile();
            // Force the memeber CCSID to the IFS file as an attribute
            ifsTmpFilePath.setCCSID(sourceCcsidInt);

            // Copy edited text area to the temporary IFS file
            outStream = new IFSFileOutputStream(remoteServer, tmpFileString);
            String textAreaString = textArea.getText();
            int textLength = textAreaString.length();
            if (sourceCcsidInt == 1208) {
                textLength = 2 * textLength;
            }
            byte[] byteArray;
            AS400Text textConverter = new AS400Text(textLength, sourceCcsidInt);
            byteArray = textConverter.toBytes(textAreaString);
            // Write text from the text area to the file
            outStream.write(byteArray);
            // Close file
            outStream.close();

            // Copy data from temporary IFS file to the member. If the member does not exist it is created.
            String commandCpyFrmStmfString = "CPYFRMSTMF FROMSTMF('" + tmpFileString
                    + "') TOMBR('" + outMemberPathString + "') MBROPT(*REPLACE) CVTDTA(*AUTO) DBFCCSID(*FILE)";
            // Perform the command
            cmdCall.run(commandCpyFrmStmfString);

            // Get messages from the command if any
            AS400Message[] as400MessageList = cmdCall.getMessageList();
            // Send all messages from the command. After ESCAPE message -
            // return.
            for (AS400Message as400Message : as400MessageList) {
                if (as400Message.getType() == AS400Message.ESCAPE) {
                    row = "Error: Copy IFS file  " + tmpFileString + "  to source member  " + tmpFileString
                            + "  using command CPYFRMSTMF  -  " + as400Message.getID() + " " + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                    return "ERROR";
                } else {
                    row = "Info: Copy IFS file  " + tmpFileString + "  to source member  " + tmpFileString
                            + "  using command CPYFRMSTMF  -  " + as400Message.getID() + " " + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                }
            }

            // Delete the temporary file
            ifsTmpFile.delete();

            row = "Comp: Source member  " + libraryName + "/" + fileName + "(" + memberName + ")  was saved.";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
            return "";
        } catch (Exception exc) {
            try {
                outStream.close();
            } catch (Exception exce) {
                exce.printStackTrace();
            }
            exc.printStackTrace();
            row = "Error: 3 Data cannot be written to the source member  " + libraryName
                    + "/" + fileName + "(" + memberName + ")  -  " + exc.toString() + ". Trying to connect again.";
            System.out.println(row);
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
                // Remove message scroll listener (cancel scrolling to the last message)
                mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
                // Try to reconnect server - FILE and RECORDACCESS services
                try {
                    remoteServer = new AS400(mainWindow.hostTextField.getText(), mainWindow.userNameTextField.getText());
                    remoteServer.connectService(AS400.FILE);
                    remoteServer.connectService(AS400.RECORDACCESS);
                    System.out.println("Info 3: A new connection to the server was obtained.");
                    row = "Info 3: A new connection to the server was obtained.";
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages(noNodes);
                    return "";
                } catch (Exception ex) {
                    row = "Error 3: Getting a new connection to the server." + ex.toString();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages(noNodes);
                    exc.printStackTrace();
                }
            return "ERROR"; // Must not continue in order not to lock an object
        }
    }

    /**
     * Rewrite source member with edited text area directly, without intermediate temporary IFS file; Records are
     * written, updated or deleted, with numbers and dates using BigDecimal objects;
     *
     * THIS METHOD IS NOT USED because it is very slow.
     *
     * @return
     */
    protected String rewriteSourceMemberDirect() {

        // Extract individual names (libraryName, fileName, memberName) from the
        // AS400 IFS path
        extractNamesFromIfsPath(filePathString);

        // Path to the output source member
        String outMemberPathString = "/QSYS.LIB/" + libraryName + ".LIB/" + fileName
                + ".FILE/" + memberName + ".MBR";

        try {
            Files.delete(tmpFilePath);
            String[] lines = textArea.getText().split("\n");

            // If overwrite is not allowed - return
            // ------------------------------------
            if (!properties.getProperty("OVERWRITE_FILE").equals("Y")) {
                row = "Info: Member  " + libraryName + "/" + fileName + "(" + memberName
                        + ")   cannot be overwtitten. " + " Overwriting files is not allowed.";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
                return "ERROR";
            }

            // Overwrite is allowed
            // --------------------

            // Obtain output database file record description
            AS400FileRecordDescription outRecDesc = new AS400FileRecordDescription(remoteServer, outMemberPathString);
            // Retrieve record format from the record description
            RecordFormat[] format = outRecDesc.retrieveRecordFormat();
            // Obtain output record object
            Record outRecord = new Record(format[0]);

            msgText = "";
            if (lines.length > 0) {
                // Create the member (SequentialFile object)
                outSeqFile = new SequentialFile(remoteServer, outMemberPathString);
                // Set the record format (the only one)
                outSeqFile.setRecordFormat(format[0]);

                try {
                    outSeqFile.open(AS400File.READ_WRITE, 100000, AS400File.COMMIT_LOCK_LEVEL_NONE);
                } catch (com.ibm.as400.access.AS400Exception as400exc) {
                    as400exc.printStackTrace();
                    // Add new member if open could not be performed (when the
                    // member does not exist)
                    // (the second parameter is a text description)
                    // The new member inherits the CCSID from its parent Source
                    // physical file
                    outSeqFile.addPhysicalFileMember(memberName, "Source member " + memberName);
                    // Open the new member
                    outSeqFile.open(AS400File.READ_WRITE, 100000, AS400File.COMMIT_LOCK_LEVEL_NONE);
                }

                // Member records contain sequence and data fields
                // -----------------------------------------------
                // Get length of member data field
                int lenDTA = format[0].getFieldDescription("SRCDTA").getLength();
                // Base sequential number - 6 digits
                BigDecimal seqNumber = new BigDecimal("0000.00");
                // Increment to the previous sequential number - 6 digits
                BigDecimal increment = new BigDecimal("0001.00");

                // Get actual date and transform it to YYMMDD
                LocalDate date = LocalDate.now();
                int intYear = date.getYear();
                int intMonth = date.getMonthValue();
                int intDay = date.getDayOfMonth();
                String strYear = String.valueOf(intYear);
                String strMonth = String.valueOf(intMonth);
                if (intMonth < 10) {
                    strMonth = "0" + strMonth;
                }
                String strDay = String.valueOf(intDay);
                if (intDay < 10) {
                    strDay = "0" + strDay;
                }
                String strSrcDat = strYear.substring(2) + strMonth + strDay;

                String dataLine;
                // Process all lines
                for (int idx = 0; idx < lines.length; idx++) {
                    // System.out.println("0'" + lines[idx] + "'");
                    if (lines[idx].equals("\n")) {
                        // System.out.println("1'" + lines[idx] + "'");
                        dataLine = " ";
                    } else {
                        // System.out.println("2'" + lines[idx] + "'");
                        dataLine = lines[idx].replace("\r", "");
                    }

                    seqNumber = seqNumber.add(increment);
                    // Insert sequential number into the source record (zoned
                    // decimal, 2 d.p.)
                    outRecord.setField("SRCSEQ", seqNumber);

                    // Insert today's date YYMMDD into the source record (zoned
                    // decimal, 0 d.p.)
                    outRecord.setField("SRCDAT", new BigDecimal(strSrcDat));

                    // Adjust data line obtained from the text area - truncate
                    // or pad by spaces if necessary
                    int dataLength;
                    if (dataLine.length() >= lenDTA) {
                        // Shorten the data line to fit the data field in the
                        // record
                        dataLength = lenDTA;
                    } else {
                        // Pad the data line with spaces to fit the data field
                        // in the record
                        char[] chpad = new char[lenDTA - dataLine.length()];
                        for (int jdx = 0; jdx < chpad.length; jdx++) {
                            chpad[jdx] = ' '; // pad the data line with spaces
                        }
                        dataLine = dataLine + String.valueOf(chpad);
                        dataLength = lenDTA;
                    }
                    // Insert data to the member data field
                    outRecord.setField("SRCDTA", dataLine.substring(0, dataLength));

                    // Update the member record with adjusted data from the text
                    // area
                    try {
                        outSeqFile.positionCursor(idx + 1);
                        outSeqFile.update(outRecord);

                    } catch (Exception exc) {
                        exc.printStackTrace();
                        row = "Error: 1 Data cannot be written to the source member  " + libraryName + "/"
                                + fileName + "(" + memberName + ")  -  " + exc.toString();
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                        msgText = "ERROR";
                        break;
                    }
                }
                // Close file
                outSeqFile.close();

                // Set caret at the beginning of the text area
                textArea.setCaretPosition(0);
                textArea.requestFocus();

                row = "Comp: Source member  " + libraryName + "/" + fileName + "(" + memberName + ")  was saved.";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
                return "";
            }
        } catch (Exception exc) {
            try {
                outSeqFile.close();
            } catch (Exception exce) {
                exce.printStackTrace();
            }
            exc.printStackTrace();
            row = "Error: 3 Data cannot be written to the source member  " + libraryName + "/" + fileName
                    + "(" + memberName + ")  -  " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
            return "ERROR"; // Must not continue in order not to lock an object
        }
        return "";
    }

    /**
     * Prepare both text areas for hihglight blocks.
     */
    private void prepareEditingAndShow() {

        // Set scroll bar to last caret position
        textArea.setCaretPosition(caretPosition);

        // Get a highlighter for the primary text area
        blockHighlighter = textArea.getHighlighter();
        // Hightlight only if the option is not *NONE
        if (!progLanguage.equals("*NONE")) {
            highlightBlocks(textArea);
        }
        // Get a highlighter for the secondary text area
        blockHighlighter = textArea2.getHighlighter();
        // Hightlight only if the option is not *NONE
        if (!progLanguage.equals("*NONE")) {
            highlightBlocks(textArea2);
        }

        try {
            BufferedWriter outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
            // Save programming language into properties
            properties.setProperty("HIGHLIGHT_BLOCKS", progLanguage);
            properties.store(outfile, PROP_COMMENT);
            outfile.close();
        } catch (Exception exc) {
            exc.printStackTrace();
        }

    }

    /**
     * Change selection mode from horizontal to vertical and vice versa.
     */
    protected void changeSelectionMode() {
        try {
            int currentCaretPos = textArea.getCaretPosition();
            infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
            properties.load(infile);
            infile.close();
            selectionMode = properties.getProperty("SELECTION_MODE");
            if (selectionModeButton.getText().equals(VERTICAL_SELECTION)) {
                // Horizontal selection will be active
                // --------------------
                selectionMode = HORIZONTAL_SELECTION;
                selectionModeButton.setText(selectionMode);
                if (caretButton.getText().equals(SHORT_CARET)) {
                    // Basic caret - a short vertical line
                    textArea.setCaret(basicCaret);
                    textArea2.setCaret(basicCaret2);
                } else {
                    // Long vertical gray line with a short red pointer
                    textArea.setCaret(longCaret);
                    textArea2.setCaret(longCaret2);
                }
                // Deactivate custom deletion in horizontal mode
                textArea.getInputMap(JComponent.WHEN_FOCUSED)
                        .remove(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
                textArea.getInputMap(JComponent.WHEN_FOCUSED)
                        .remove(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
                textArea.getActionMap().remove("deleteDel");
                textArea.getActionMap().remove("deleteBcksp");
                textArea2.getInputMap(JComponent.WHEN_FOCUSED)
                        .remove(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
                textArea2.getInputMap(JComponent.WHEN_FOCUSED)
                        .remove(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
                textArea2.getActionMap().remove("deleteDel");
                textArea2.getActionMap().remove("deleteBcksp");
            } else {
                // Vertical selection will be active
                // ------------------
                selectionMode = VERTICAL_SELECTION;
                selectionModeButton.setText(selectionMode);
                // Set special caret - same for both caret shapes
                textArea.setCaret(specialCaret);
                textArea2.setCaret(specialCaret2);
                // Activate custom deletion by Delete or Backspace key
                textArea.getInputMap(JComponent.WHEN_FOCUSED)
                        .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteDel");
                textArea.getActionMap().put("deleteDel", new CustomDelete("DEL"));
                textArea.getInputMap(JComponent.WHEN_FOCUSED)
                        .put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "deleteBcksp");
                textArea.getActionMap().put("deleteBcksp", new CustomDelete("BACKSPACE"));
                textArea2.getInputMap(JComponent.WHEN_FOCUSED)
                        .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteDel");
                textArea2.getActionMap().put("deleteDel", new CustomDelete("DEL"));
                textArea2.getInputMap(JComponent.WHEN_FOCUSED)
                        .put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "deleteBcksp");
                textArea2.getActionMap().put("deleteBcksp", new CustomDelete("BACKSPACE"));
            }
            prepareEditingAndShow();
            textArea.requestFocusInWindow();
            textArea.setCaretPosition(currentCaretPos);
            BufferedWriter outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
            // Save caret shape into properties
            properties.setProperty("SELECTION_MODE", selectionMode);
            properties.store(outfile, PROP_COMMENT);
            outfile.close();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    /**
     * Change caret shape between long and short.
     */
    protected void changeCaretShape() {
        try {
            int currentCaretPos = textArea.getCaretPosition();
            infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
            properties.load(infile);
            infile.close();
            caretShape = properties.getProperty("CARET");
            if (selectionModeButton.getText().equals(HORIZONTAL_SELECTION)) {
                if (caretButton.getText().equals(LONG_CARET)) {
                    // Long caret button detected - change it to short caret.
                    caretShape = SHORT_CARET;
                    caretButton.setText(caretShape);
                    // For horizontal selection set basic caret - a short vertical line
                    textArea.setCaret(basicCaret);
                    textArea2.setCaret(basicCaret2);
                } else {
                    // Short caret button detected - change it to long caret.
                    caretShape = LONG_CARET;
                    caretButton.setText(caretShape);
                    // For horizontal selection set long caret - long vertical gray line with a short red pointer
                    textArea.setCaret(longCaret);
                    textArea2.setCaret(longCaret2);
                }
            } else {
                if (caretButton.getText().equals(LONG_CARET)) {
                    // Long caret button detected - change it to short caret.
                    caretShape = SHORT_CARET;
                    caretButton.setText(caretShape);
                    // For vertical selection set special caret - a short vertical line
                    textArea.setCaret(specialCaret);
                    textArea2.setCaret(specialCaret2);
                } else {
                    // Short caret button detected - change it to long caret.
                    caretShape = LONG_CARET;
                    caretButton.setText(caretShape);
                    // For vertical selection set special with long caret - long vertical gray line with a short red pointer
                    textArea.setCaret(specialCaret);
                    textArea2.setCaret(specialCaret2);
                }
            }

            prepareEditingAndShow();
            textArea.requestFocusInWindow();
            textArea.setCaretPosition(currentCaretPos);

            BufferedWriter outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
            // Save caret shape into properties
            properties.setProperty("CARET", caretShape);
            properties.store(outfile, PROP_COMMENT);
            outfile.close();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    /**
     * Highlight compound statements (blocks) in a simplified parsing
     *
     * @param textArea
     */
    protected void highlightBlocks(JTextArea textArea) {
        /**
         * This new-line is necessary for prevention of a never ending loop.
         * Without it the never ending loop would occur when BLOCK HIGHLIGHTING is specified
         * and the user writes some character(s) after the END of the text area
         * and then the user presses a mouse button.
         *
         * This produces an invisible effect of appending an empty new line
         * each time the user clicks the mouse button anywhere in the text area.
         */
//????        textArea.append("\n");

        stmtsBeg.clear();
        stmtsEnd.clear();

        // Define block beginning and ending symbols
        switch (progLanguage) {
            case "*ALL": {
                // Beginnings of block statements

                // Declarations
                stmtsBeg.add("DCL-DS");
                stmtsBeg.add("DCL-PR");
                stmtsBeg.add("DCL-PI");
                stmtsBeg.add("DCL-PROC");
                stmtsBeg.add("BEGSR");
                stmtsBeg.add("SECTION");
                stmtsBeg.add("INPUT-OUTPUT");
                stmtsBeg.add("WORKING-STORAGE");
                stmtsBeg.add(" LINKAGE");
                stmtsBeg.add(" DIVISION");
                stmtsBeg.add(" ENVIRONMENT");
                stmtsBeg.add(" IDENTIFICATION");
                stmtsBeg.add(" DATA ");
                stmtsBeg.add(" PROCEDURE ");
                // Loops
                stmtsBeg.add("DOW");
                stmtsBeg.add("DOW(");
                stmtsBeg.add("DOU");
                stmtsBeg.add("DOU(");
                stmtsBeg.add("DOUNTIL");
                stmtsBeg.add("DO");
                stmtsBeg.add("DO ");
                stmtsBeg.add("WHILE");
                stmtsBeg.add("DOWHILE");
                stmtsBeg.add("UNTIL");
                stmtsBeg.add("FOR ");
                stmtsBeg.add("FOR(");
                stmtsBeg.add("DOFOR");
                stmtsBeg.add("PERFORM");
                // Conditions
                stmtsBeg.add("IF");
                stmtsBeg.add("IF ");
                stmtsBeg.add("IF(");
                stmtsBeg.add("ELSEIF");
                stmtsBeg.add("ELSEIF(");
                stmtsBeg.add("THEN");
                stmtsBeg.add("ELSE");
                stmtsBeg.add("SELECT");
                stmtsBeg.add("SWITCH");
                stmtsBeg.add("CASE");
                stmtsBeg.add("DEFAULT");
                stmtsBeg.add("WHEN");
                stmtsBeg.add("WHEN(");
                stmtsBeg.add("OTHER");
                stmtsBeg.add("OTHERWISE");
                stmtsBeg.add("EVALUATE");
                // Monitors
                stmtsBeg.add("MONITOR");
                stmtsBeg.add("ON-ERROR");
                stmtsBeg.add("MONMSG");
                stmtsBeg.add("TRY");
                stmtsBeg.add("CATCH");
                // C - style
                stmtsBeg.add("{");

                // Query
                stmtsBeg.add("SELECT");
                stmtsBeg.add("FROM");
                stmtsBeg.add("WHERE");
                stmtsBeg.add("ORDER");
                stmtsBeg.add("HAVING");
                stmtsBeg.add("GROUP");
                stmtsBeg.add("CONNECT");
                stmtsBeg.add("FETCH");
                stmtsBeg.add("ORDER");
                stmtsBeg.add("ORDER");
                stmtsBeg.add("JOIN");
                stmtsBeg.add("USING");
                stmtsBeg.add("INNER");
                stmtsBeg.add("LEFT");
                stmtsBeg.add("RIGHT");
                stmtsBeg.add("FULL");
                stmtsBeg.add("EXCEPTION");
                stmtsBeg.add("CROSS");
                stmtsBeg.add("DISTINCT");
                stmtsBeg.add("TABLE");
                stmtsBeg.add("WITH");
                stmtsBeg.add("RECURSIVE");
                stmtsBeg.add("UNION");
                stmtsBeg.add("INTERSECT");
                stmtsBeg.add("EXCEPT");
                stmtsBeg.add("SET");
                stmtsBeg.add("SCHEMA");
                stmtsBeg.add("SQLSTATE");
                stmtsBeg.add("SQLCODE");

                // Data definition
                stmtsBeg.add("CREATE");
                stmtsBeg.add("INSERT");
                stmtsBeg.add("UPDATE");
                stmtsBeg.add("ALTER");
                stmtsBeg.add("DROP");

                // Special comments
                stmtsBeg.add("--;");

                // Parameter marker - question mark
                //stmtsBeg.add("?");

                // Exec for embedded SQl
                stmtsBeg.add("EXEC");
                stmtsBeg.add("SQL");

                // Parameter marker - question mark
                // stmtsBeg.add("?"); // Resolved elsewhere

                // Endings of block statements

                // Declarations 
                stmtsEnd.add("END-DS"); // RPG **FREE
                stmtsEnd.add("END-PR"); // RPG **FREE
                stmtsEnd.add("END-PI"); // RPG **FREE
                stmtsEnd.add("END-PROC"); // RPG **FREE
                stmtsEnd.add("ENDSR"); // RPG
                // Loops
                stmtsEnd.add("ENDDO"); // RPG **FREE
                stmtsEnd.add("ENDFOR"); // RPG **FREE
                stmtsEnd.add("END-PERFORM"); // COBOL
                // Conditions
                stmtsEnd.add("ENDIF"); // RPG
                stmtsEnd.add("END-IF"); // COBOL
                stmtsEnd.add("ENDSL"); // RPG
                stmtsEnd.add("ENDSELECT"); // CL
                stmtsEnd.add("END-EVALUATE"); // COBOL
                // Monitor
                stmtsEnd.add("ENDMON"); // RPG
                // C - style
                stmtsEnd.add("}"); // C

                // SQL style
                stmtsEnd.add("END-EXEC"); // In RPG III
                break;
            } // End of case *ALL

            case "RPG **FREE": {
                // Beginnings of block statements

                // Declarations
                stmtsBeg.add("DCL-DS");
                stmtsBeg.add("DCL-PR");
                stmtsBeg.add("DCL-PI");
                stmtsBeg.add("DCL-PROC");
                stmtsBeg.add("BEGSR");
                // Loops
                stmtsBeg.add("DOW");
                stmtsBeg.add("DOW(");
                stmtsBeg.add("DOU");
                stmtsBeg.add("DOU(");
                stmtsBeg.add("FOR");
                stmtsBeg.add("FOR(");
                // Conditions
                stmtsBeg.add("IF");
                stmtsBeg.add("IF ");
                stmtsBeg.add("IF(");
                stmtsBeg.add("ELSEIF");
                stmtsBeg.add("ELSEIF(");
                stmtsBeg.add("ELSE");
                stmtsBeg.add("SELECT");
                stmtsBeg.add("WHEN");
                stmtsBeg.add("WHEN(");
                stmtsBeg.add("OTHER");
                // Monitors
                stmtsBeg.add("MONITOR");
                stmtsBeg.add("ON-ERROR");

                // Ends of block statements

                // Declarations
                stmtsEnd.add("END-DS");
                stmtsEnd.add("END-PR");
                stmtsEnd.add("END-PI");
                stmtsEnd.add("END-PROC");
                stmtsEnd.add("ENDSR");
                // Loops
                stmtsEnd.add("ENDDO");
                stmtsEnd.add("ENDFOR");
                // Conditions
                stmtsEnd.add("ENDIF");
                stmtsEnd.add("ENDSL");
                // Monitor
                stmtsEnd.add("ENDMON");
                break;
            } // End of case RPG **FREE

            case "RPG /FREE": {
                // Beginnings of block statements

                // Declarations
                stmtsBeg.add("DCL-DS");
                stmtsBeg.add("DCL-PR");
                stmtsBeg.add("DCL-PI");
                stmtsBeg.add("DCL-PROC");
                stmtsBeg.add("BEGSR");
                // Loops
                stmtsBeg.add("DOW");
                stmtsBeg.add("DOW(");
                stmtsBeg.add("DOU");
                stmtsBeg.add("DOU(");
                stmtsBeg.add("FOR");
                stmtsBeg.add("FOR(");
                // Conditions
                stmtsBeg.add("IF");
                stmtsBeg.add("IF ");
                stmtsBeg.add("IF(");
                stmtsBeg.add("ELSEIF");
                stmtsBeg.add("ELSEIF(");
                stmtsBeg.add("ELSE");
                stmtsBeg.add("SELECT");
                stmtsBeg.add("WHEN");
                stmtsBeg.add("WHEN(");
                stmtsBeg.add("OTHER");
                // Monitors
                stmtsBeg.add("MONITOR");
                stmtsBeg.add("ON-ERROR");

                // Ends of block statements

                // Declarations
                stmtsEnd.add("END-DS");
                stmtsEnd.add("END-PR");
                stmtsEnd.add("END-PI");
                stmtsEnd.add("END-PROC");
                stmtsEnd.add("ENDSR");
                // Loops
                stmtsEnd.add("ENDDO");
                stmtsEnd.add("ENDFOR");
                // Conditions
                stmtsEnd.add("ENDIF");
                stmtsEnd.add("ENDSL");
                // Monitor
                stmtsEnd.add("ENDMON");
                break;
            } // End of case RPG /FREE

            case "RPG IV fixed": {
                // Beginnings of block statements

                // Declarations
                stmtsBeg.add("BEGSR");
                // Loops
                stmtsBeg.add("DO");
                stmtsBeg.add("DOW");
                stmtsBeg.add("DOW(");
                stmtsBeg.add("DOU");
                stmtsBeg.add("DOU(");
                // Conditions
                stmtsBeg.add("IF");
                stmtsBeg.add("ELSE");
                stmtsBeg.add("SELECT");
                stmtsBeg.add("WHEN");
                stmtsBeg.add("WHEN(");
                stmtsBeg.add("OTHER");
                // Monitors
                stmtsBeg.add("MONITOR");
                stmtsBeg.add("ON-ERROR");

                // End of block statements

                // Declarations
                stmtsEnd.add("ENDSR");
                // Loops
                stmtsEnd.add("ENDDO");
                stmtsEnd.add("END  ");
                // Conditions
                stmtsEnd.add("ENDIF");
                stmtsEnd.add("ENDSL");
                // Monitor
                stmtsEnd.add("ENDMON");
                break;
            } // End of case RPG IV fixed

            case "RPG III": {
                // Beginnings of block statements

                // Declarations
                stmtsBeg.add("BEGSR");
                // Loops
                stmtsBeg.add("DO");
                stmtsBeg.add("DOW");
                stmtsBeg.add("DOU");
                // Conditions
                stmtsBeg.add("IF");
                stmtsBeg.add("ELSE");
                stmtsBeg.add("SELEC");
                stmtsBeg.add("CAS");
                stmtsBeg.add("WH");
                stmtsBeg.add("OTHER");

                // Ends of block statements

                // Declarations
                stmtsEnd.add("ENDSR");
                // Loops
                stmtsEnd.add("ENDDO");
                stmtsEnd.add("END  ");
                // Conditions
                stmtsEnd.add("ENDIF");
                stmtsEnd.add("ENDSL");
                stmtsEnd.add("ENDCS");
                break;
            } // End of case RPG III

            case "CL": {
                // Beginnings of block statements

                // Declarations
                stmtsBeg.add("SUBR");
                // Loops
                stmtsBeg.add("DOUNTIL");
                stmtsBeg.add("DOWHILE");
                stmtsBeg.add("DOFOR");
                stmtsBeg.add("DO");
                // Conditions
                stmtsBeg.add("IF");
                stmtsBeg.add("THEN");
                stmtsBeg.add("ELSE");
                stmtsBeg.add("SELECT");
                stmtsBeg.add("WHEN");
                stmtsBeg.add("OTHERWISE");
                // Monitors
                stmtsBeg.add("MONMSG");

                // Ends of block statements

                // Declarations
                stmtsEnd.add("ENDSUBR");
                // Loops
                stmtsEnd.add("ENDDO");
                stmtsEnd.add("ENDSELECT");
                break;
            } // End of case CL

            case "COBOL": {
                // Beginnings of block statements

                // Declarations
                stmtsBeg.add(" SECTION");
                stmtsBeg.add("INPUT-OUTPUT");
                stmtsBeg.add("WORKING-STORAGE");
                stmtsBeg.add(" LINKAGE");
                stmtsBeg.add(" DIVISION");
                stmtsBeg.add(" ENVIRONMENT");
                stmtsBeg.add(" IDENTIFICATION");
                stmtsBeg.add(" DATA ");
                stmtsBeg.add(" PROCEDURE ");
                // Loops
                stmtsBeg.add(" PERFORM ");
                stmtsBeg.add(" UNTIL ");
                // Conditions
                stmtsBeg.add("IF ");
                stmtsBeg.add("THEN");
                stmtsBeg.add("ELSE");
                stmtsBeg.add("EVALUATE");
                stmtsBeg.add("WHEN");

                // Ends of block statements

                // Loops
                stmtsEnd.add("END-PERFORM");
                // Conditions
                stmtsEnd.add("END-IF");
                stmtsEnd.add("END-EVALUATE");
                break;
            } // End of case COBOL

            case "C": {
                // Beginnings of block statements

                // Loops
                stmtsBeg.add("MAIN ");
                stmtsBeg.add("MAIN(");
                stmtsBeg.add("WHILE ");
                stmtsBeg.add("WHILE(");
                stmtsBeg.add("FOR ");
                stmtsBeg.add("FOR(");
                stmtsBeg.add("DO ");
                stmtsBeg.add("DO(");
                // Conditions
                stmtsBeg.add("IF ");
                stmtsBeg.add("IF(");
                stmtsBeg.add("ELSE ");
                stmtsBeg.add("SWITCH");
                stmtsBeg.add("CASE");
                stmtsBeg.add("DEFAULT");
                stmtsBeg.add("{");

                // Endings of block statements

                stmtsEnd.add("}");
                break;
            } // End of case C

            case "C++": {
                // Beginnings of block statements

                // Loops
                stmtsBeg.add("MAIN ");
                stmtsBeg.add("MAIN(");
                stmtsBeg.add("WHILE ");
                stmtsBeg.add("WHILE(");
                stmtsBeg.add("FOR ");
                stmtsBeg.add("FOR(");
                stmtsBeg.add("DO ");
                stmtsBeg.add("DO(");
                // Conditions
                stmtsBeg.add("IF ");
                stmtsBeg.add("IF(");
                stmtsBeg.add("ELSE ");
                stmtsBeg.add("SWITCH");
                stmtsBeg.add("CASE");
                stmtsBeg.add("DEFAULT");
                // Monitors
                stmtsBeg.add("TRY");
                stmtsBeg.add("CATCH");
                stmtsBeg.add("{");

                // Endings of block statements

                stmtsEnd.add("}");
                break;
            } // End of case C++

            case "SQL": {
                // Beginnings of block statements

                // Query
                stmtsBeg.add("SELECT");
                stmtsBeg.add("FROM");
                stmtsBeg.add("WHERE");
                stmtsBeg.add("ORDER");
                stmtsBeg.add("HAVING");
                stmtsBeg.add("GROUP");
                stmtsBeg.add("CONNECT");
                stmtsBeg.add("FETCH");
                stmtsBeg.add("JOIN");
                stmtsBeg.add("USING");
                stmtsBeg.add("INNER");
                stmtsBeg.add("LEFT");
                stmtsBeg.add("RIGHT");
                stmtsBeg.add("FULL");
                stmtsBeg.add("EXCEPTION");
                stmtsBeg.add("CROSS");
                stmtsBeg.add("DISTINCT");
                stmtsBeg.add("TABLE");
                stmtsBeg.add("WITH");
                stmtsBeg.add("RECURSIVE");
                stmtsBeg.add("UNION");
                stmtsBeg.add("INTERSECT");
                stmtsBeg.add("EXCEPT");
                stmtsBeg.add("SET");
                stmtsBeg.add("SCHEMA");
                stmtsBeg.add("SQLSTATE");
                stmtsBeg.add("SQLCOD");
                stmtsBeg.add("SQLCODE");

                // Data definition
                stmtsBeg.add("CREATE");
                stmtsBeg.add("INSERT");
                stmtsBeg.add("UPDATE");
                stmtsBeg.add("ALTER");
                stmtsBeg.add("DROP");

                // Special comments
                stmtsBeg.add("--;");

                // Exec for embedded SQl
                stmtsBeg.add("EXEC");
                stmtsBeg.add("SQL");

                // Parameter marker - question mark
                // stmtsBeg.add("?"); // Resolved elsewhere

                // Endings of block statements

                stmtsEnd.add("END-EXEC"); // In RPG III
                break;

            } // End of case SQL

        } // End of switch

        // Find and highlight beginning block statements
        stmtsBeg.forEach(stmtBeg -> {
            highlightBlockStmt(textArea, stmtBeg, true); // true is tested as beg
        });

        // Find and highlight ending block statements
        stmtsEnd.forEach(stmtEnd -> {
            highlightBlockStmt(textArea, stmtEnd, false); // false is tested as !beg
        });
    }

    /**
     * Highlight block statements
     *
     * @param textArea
     * @param blockStmt
     * @param beg
     */
    protected void highlightBlockStmt(JTextArea textArea, String blockStmt, boolean beg) {

        // Beginnings of block statements - colors
        // ------------------------------
        if (beg && (blockStmt.equals("DCL-DS"))) {
            // DCL-DS in RPG **FREE
            blockPainter = blockBrownLighter;
        } else if (beg && blockStmt.equals("DCL-PR")) {
            // DCL-PR in RPG **FREE
            blockPainter = blockBrownLighter;
        } else if (beg && blockStmt.equals("DCL-PI")) {
            // DCL-PI in RPG **FREE
            blockPainter = blockBrownLighter;
        } else if (beg && blockStmt.equals("DOW")) {
            // DOW in RPG
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("DOW(")) {
            // DOW( in RPG **FREE
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("DOU")) {
            // DOU in RPG
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("DOU(")) {
            // DOU( in RPG **FREE
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("DOUNTIL")) {
            // DOUNTIL in CL
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("DOWHILE")) {
            // DOWHILE in CL
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("DO ")) {
            // DO in CL, C, C++, older RPG
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("DO")) {
            // DO in CL, older RPG
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("DO(")) {
            // DO in CL, C, C++, older RPG
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("WHILE ")) {
            // WHILE in C, C++
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("WHILE(")) {
            // WHILE in C, C++
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("UNTIL ")) {
            // UNTIL in C, C++, COBOL
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("FOR ")) {
            // FOR in RPG, C, C++
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("FOR(")) {
            // FOR( in RPG **FREE, C, C++
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("MAIN ")) {
            // MAIN in C, C++
            blockPainter = blockRedDarker;
        } else if (beg && blockStmt.equals("MAIN(")) {
            // MAIN in C, C++
            blockPainter = blockRedDarker;
        } else if (beg && blockStmt.equals("DOFOR")) {
            // DOFOR in CL
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals(" PERFORM ")) {
            // PERFORM in COBOL
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("IF ")) {
            // IF in RPG, COBOL, CL, C, C++
            blockPainter = blockGreenLighter;
        } else if (beg && blockStmt.equals("IF(")) {
            // IF( in RPG **FREE and RPG /free
            blockPainter = blockGreenLighter;
        } else if (beg && blockStmt.equals("IF")) {
            // IF in RPG III and RPG IV
            blockPainter = blockGreenLighter;
        } else if (beg && blockStmt.equals("ELSEIF")) {
            // ELSEIF in RPG
            blockPainter = blockGreenLighter;
        } else if (beg && blockStmt.equals("THEN")) {
            // THEN in COBOL
            blockPainter = blockGreenLighter;
        } else if (beg && blockStmt.equals("ELSE")) {
            // ELSE in RPG, CL, COBOL, C, C++
            blockPainter = blockGreenLighter;
        } else if (beg && blockStmt.equals("SELECT") && !progLanguage.equals("SQL")) {
            // SELECT in RPG IV (and COBOL - not a block, only file selection)
            blockPainter = blockYellowLighter;
        } else if (beg && blockStmt.equals("SELEC")) {
            // SELEC in RPG III
            blockPainter = blockYellowLighter;
        } else if (beg && blockStmt.equals("WHEN")) {
            // WHEN in RPG, COBOL
            blockPainter = blockYellowLighter;
        } else if (beg && blockStmt.equals("WHEN(")) {
            // WHEN( in RPG
            blockPainter = blockYellowLighter;
        } else if (beg && blockStmt.equals("WH")) {
            // WH in RPG III (as WHEQ, WHLE, ...)
            blockPainter = blockYellowLighter;
        } else if (beg && blockStmt.equals("OTHER")) {
            // OTHER in RPG
            blockPainter = blockYellowLighter;
        } else if (beg && blockStmt.equals("SWITCH")) {
            // switch in C or C++
            blockPainter = blockYellowLighter;
        } else if (beg && blockStmt.equals("CASE")) {
            // case in switch in C or C++
            blockPainter = blockYellowLighter;
        } else if (beg && blockStmt.equals(" EVALUATE ")) {
            // EVALUATE in COBOL
            blockPainter = blockYellowLighter;
        } else if (beg && blockStmt.equals("MONITOR")) {
            // MONITOR in RPG IV
            blockPainter = blockRedLighter;
        } else if (beg && blockStmt.equals("ON-ERROR")) {
            // ON-ERROR in RPG IV
            blockPainter = blockRedLighter;
        } else if (beg && blockStmt.equals("TRY")) {
            // try in C++
            blockPainter = blockRedLighter;
        } else if (beg && blockStmt.equals("CATCH")) {
            // catch in C++
            blockPainter = blockRedLighter;
        } else if (beg && blockStmt.equals("DCL-PROC")) {
            // DCL-PROC in RPG **FREE
            blockPainter = blockGrayLighter;
        } else if (beg && blockStmt.equals("BEGSR")) {
            // BEGSR in RPG
            blockPainter = blockGrayLighter;
        } else if (beg && blockStmt.equals(" SECTION")) {
            // SECTION in COBOL
            blockPainter = blockGrayLighter;
        } else if (beg && blockStmt.equals("INPUT-OUTPUT")) {
            // WORKING-STORAGE in COBOL
            blockPainter = blockGrayLighter;
        } else if (beg && blockStmt.equals("WORKING-STORAGE")) {
            // INPUT-OUTPUT in COBOL
            blockPainter = blockGrayLighter;
        } else if (beg && blockStmt.equals(" LINKAGE")) {
            // LINKAGE in COBOL
            blockPainter = blockGrayLighter;
        } else if (beg && blockStmt.equals(" DIVISION")) {
            // DIVISION in COBOL
            blockPainter = blockRedDarker;
        } else if (beg && blockStmt.equals(" ENVIRONMENT")) {
            // ENVIRONMENT in COBOL
            blockPainter = blockRedDarker;
        } else if (beg && blockStmt.equals(" IDENTIFICATION")) {
            // IDENTIFICATION in COBOL
            blockPainter = blockRedDarker;
        } else if (beg && blockStmt.equals(" DATA ")) {
            // DATA in COBOL
            blockPainter = blockRedDarker;
        } else if (beg && blockStmt.equals(" PROCEDURE ")) {
            // PROCEDURE in COBOL
            blockPainter = blockRedDarker;
        } else if (beg && blockStmt.equals("{")) {
            // in C, C++
            blockPainter = curlyBracketsLighter;
        } else if (beg && blockStmt.equals("EXEC") && progLanguage.equals("SQL")) {
            // in SQL
            blockPainter = blockRedLighter;
        } else if (beg && blockStmt.equals("SQL")) {
            // in SQL
            blockPainter = blockRedLighter;
        } else if (beg && blockStmt.equals("SELECT") && progLanguage.equals("SQL")) {
            // in SQL
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("FROM")) {
            // in SQL
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("WHERE")) {
            // in SQL
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("ORDER")) {
            // in SQL
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("HAVING")) {
            // in SQL
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("GROUP")) {
            // in SQL
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("CONNECT")) {
            // in SQL
            blockPainter = blockGreenLighter;
        } else if (beg && blockStmt.equals("FETCH")) {
            // in SQL
            blockPainter = blockBlueLighter;
        } else if (beg && blockStmt.equals("JOIN")) {
            // in SQL
            blockPainter = blockGreenLighter;
        } else if (beg && blockStmt.equals("USING")) {
            // in SQL
            blockPainter = blockGreenLighter;
        }else if (beg && blockStmt.equals("INNER")) {
            // in SQL
            blockPainter = blockGreenLighter;
        } else if (beg && blockStmt.equals("LEFT")) {
            // in SQL
            blockPainter = blockGreenLighter;
        } else if (beg && blockStmt.equals("RIGHT")) {
            // in SQL
            blockPainter = blockGreenLighter;
        } else if (beg && blockStmt.equals("FULL")) {
            // in SQL
            blockPainter = blockGreenLighter;
        } else if (beg && blockStmt.equals("EXCEPTION")) {
            // in SQL
            blockPainter = blockGreenLighter;
        } else if (beg && blockStmt.equals("CROSS")) {
            // in SQL
            blockPainter = blockGreenLighter;
        } else if (beg && blockStmt.equals("DISTINCT")) {
            // in SQL
            blockPainter = blockGreenLighter;
        } else if (beg && blockStmt.equals("TABLE") && progLanguage.equals("SQL")) {
            // in SQL
            blockPainter = blockBrownLighter;
        } else if (beg && blockStmt.equals("WITH")) {
            // in SQL
            blockPainter = blockGreenLighter;
        } else if (beg && blockStmt.equals("RECURSIVE")) {
            // in SQL
            blockPainter = blockGreenLighter;
        } else if (beg && blockStmt.equals("UNION")) {
            // in SQL
            blockPainter = blockGrayLighter;
        } else if (beg && blockStmt.equals("INTERSECT")) {
            // in SQL
            blockPainter = blockGrayLighter;
        } else if (beg && blockStmt.equals("EXCEPT")) {
            // in SQL
            blockPainter = blockGrayLighter;
        } else if (beg && blockStmt.equals("SET") && progLanguage.equals("SQL")) {
            // in SQL
            blockPainter = blockBrownLighter;
        } else if (beg && blockStmt.equals("SCHEMA")) {
            // in SQL
            blockPainter = blockBrownLighter;
        } else if (beg && blockStmt.equals("CREATE")) {
            // in SQL
            blockPainter = blockBrownLighter;
        } else if (beg && blockStmt.equals("INSERT")) {
            // in SQL
            blockPainter = blockBrownLighter;
        } else if (beg && blockStmt.equals("UPDATE")) {
            // in SQL
            blockPainter = blockBrownLighter;
        } else if (beg && blockStmt.equals("ALTER")) {
            // in SQL
            blockPainter = blockBrownLighter;
        } else if (beg && blockStmt.equals("DROP")) {
            // in SQL
            blockPainter = blockBrownLighter;
        } else if (beg && blockStmt.equals("SQLSTATE")) {
            // in SQL
            blockPainter = blockGreenLighter;
        } else if (beg && blockStmt.equals("SQLCOD")) {
            // in SQL
            blockPainter = blockGreenLighter;
        } else if (beg && blockStmt.equals("SQLCODE")) {
            // in SQL
            blockPainter = blockGreenLighter;
        } else if (beg && blockStmt.equals("--;")) {
            // in SQL - special comment (trigraph)
            blockPainter = blockYellowLighter;
        } else if (beg && blockStmt.equals("?")) {
            // in SQL - parameter marker
            blockPainter = blockRedDarker;

            // Ends of block statements - colors
            // ------------------------
        } else if (!beg && blockStmt.equals("END-DS")) {
            // END-DS in RPG **FREE
            blockPainter = blockBrownDarker;
        } else if (!beg && blockStmt.equals("END-PR")) {
            // END-PR in RPG **FREE
            blockPainter = blockBrownDarker;
        } else if (!beg && blockStmt.equals("END-PI")) {
            // END-PI in RPG **FREE
            blockPainter = blockBrownDarker;
        } else if (!beg && blockStmt.equals("ENDDO")) {
            // ENDDO in RPG, CL
            blockPainter = blockBlueDarker;
        } else if (!beg && blockStmt.equals("END  ")) {
            // END in RPGIII
            blockPainter = blockBlueDarker;
        } else if (!beg && blockStmt.equals("ENDFOR")) {
            // ENDFOR in RPG, CL
            blockPainter = blockBlueDarker;
        } else if (!beg && blockStmt.equals("END-PERFORM")) {
            // END-PERFORM in COBOL
            blockPainter = blockBlueDarker;
        } else if (!beg && blockStmt.equals("ENDIF")) {
            // ENDIF in RPG and C, C++ (in #endif)
            blockPainter = blockGreenDarker;
        } else if (!beg && blockStmt.equals("END-IF")) {
            // END-IF in COBOL
            blockPainter = blockGreenDarker;
        } else if (!beg && blockStmt.equals("ENDSL")) {
            // ENDSL in RPG
            blockPainter = blockYellowDarker;
        } else if (!beg && blockStmt.equals("ENDSELECT")) {
            // ENDSELECT in CL
            blockPainter = blockYellowDarker;
        } else if (!beg && blockStmt.equals("DEFAULT")) {
            // default in C, C++
            blockPainter = blockYellowDarker;
        } else if (!beg && blockStmt.equals("END-EVALUATE")) {
            // END-EVALUATE in COBOL
            blockPainter = blockYellowDarker;
        } else if (!beg && blockStmt.equals("ENDMON")) {
            // ENDMON in CL
            blockPainter = blockRedDarker;
        } else if (!beg && blockStmt.equals("ENDSR")) {
            // ENDSR in RPG
            blockPainter = blockGrayDarker;
        } else if (!beg && blockStmt.equals("END-PROC")) {
            // END-PROC in RPG **FREE
            blockPainter = blockGrayDarker;
        } else if (!beg && blockStmt.equals("}")) {
            // in C, C++
            blockPainter = curlyBracketsDarker;
        }

        // Inspect each line separately for ONE occurrence of the block statement.
        // Highlight only the block statement that is outside of a comment, if it is not too complex.

        String textToHighlight;

        textToHighlight = textArea.getText().toUpperCase();
        int textLength = textToHighlight.length();
        int startOfLine = 0;
        int endOfLine = 0;
        //System.out.println("endOfLine first: " + endOfLine);
        // Process all lines in the textToHighlight area
        try {
            // Find the first new-line character in the textToHighlight.
            // Index of the first new-line character is the first end of line.
            // May be -1 if no end-of-line character exists in the text area.
            endOfLine = textToHighlight.indexOf(NEW_LINE, startOfLine);

            // Process all lines in the text area
            while ( // "textToHighlight" is not empty  
                    // and the block statement is inside the whole textToHighlight (before the last NEW_LINE)
                    // and at the end of line exists at least one new-line character.
                    startOfLine > -1
                    && startOfLine < textLength
                    && endOfLine != -1) {
                if (endOfLine - startOfLine > 0) {
                    // The line has at least one character
                    int startOfBlockStmt = textToHighlight.indexOf(blockStmt, startOfLine);
                    int endOfBlockStmt = startOfBlockStmt + blockStmt.length();

                    if (startOfBlockStmt >= startOfLine && startOfBlockStmt <= endOfLine - blockStmt.length()) {
                        switch (progLanguage) {

                            case "*ALL": {
                                // The *ALL option highlights all occurrences in all languages.
                                blockHighlighter.addHighlight(startOfBlockStmt, endOfBlockStmt, blockPainter);
                                break;
                            }

                            case "RPG **FREE": {
                                // Before block statement: all spaces or empty
                                // After block statement: A space or semicolon or new line
                                if ((textToHighlight.substring(startOfLine, startOfBlockStmt)
                                        .equals(fixedLengthSpaces(startOfBlockStmt - startOfLine))
                                        || textToHighlight.substring(startOfLine, startOfBlockStmt).isEmpty())
                                        && (textToHighlight.substring(endOfBlockStmt, endOfBlockStmt + 1).equals(" ")
                                        || textToHighlight.substring(endOfBlockStmt, endOfBlockStmt + 1).equals(";")
                                        || textToHighlight.substring(endOfBlockStmt, endOfBlockStmt + 1).equals(NEW_LINE))) {
                                    blockHighlighter.addHighlight(startOfBlockStmt, endOfBlockStmt, blockPainter);
                                }
                                break;
                            } // End of case RPG **FREE

                            case "RPG /FREE": {
                                // Before block statement from column 8: all spaces
                                // After block statement: a space or new line or semicolon
                                // No asterisk comment (* in column 7)
                                // No C in col. 6
                                if (textToHighlight.length() > 7) {
                                    if ((textToHighlight.substring(startOfLine + 7, startOfBlockStmt)
                                            .equals(fixedLengthSpaces(startOfBlockStmt - (startOfLine + 7)))
                                            || textToHighlight.substring(startOfLine, startOfBlockStmt).isEmpty())
                                            && (textToHighlight.substring(endOfBlockStmt, endOfBlockStmt + 1).equals(" ")
                                            || textToHighlight.substring(endOfBlockStmt, endOfBlockStmt + 1).equals(NEW_LINE)
                                            || textToHighlight.substring(endOfBlockStmt, endOfBlockStmt + 1).equals(";"))
                                            && !textToHighlight.substring(startOfLine + 6, startOfLine + 7).equals("*")
                                            && !textToHighlight.substring(startOfLine + 5, startOfLine + 6).toUpperCase().equals("C")) {
                                        blockHighlighter.addHighlight(startOfBlockStmt, endOfBlockStmt, blockPainter);
                                    }
                                }
                                break;
                            } // End of case RPG /FREE

                            case "RPG IV fixed": {
                                // C in column 6 and no asterisk comment (* in column 7) and block statement in column 26 (Opcode)
                                if (textToHighlight.length() >= 5) {
                                    if (textToHighlight.substring(startOfLine + 5, startOfLine + 6).equals("C")
                                            && !textToHighlight.substring(startOfLine + 6, startOfLine + 7).equals("*")
                                            && startOfBlockStmt - startOfLine == 25) {
                                        blockHighlighter.addHighlight(startOfBlockStmt, endOfBlockStmt, blockPainter);
                                    }
                                }
                                break;
                            } // End of case RPG IV fixed

                            case "RPG III": {
                                // C in column 6 and no asterisk comment (* in column 7) and block statement in column 28 (Opcode)
                                if (textToHighlight.length() >= 5) {
                                    if (textToHighlight.substring(startOfLine + 5, startOfLine + 6).equals("C")
                                            && !textToHighlight.substring(startOfLine + 6, startOfLine + 7).equals("*")
                                            && startOfBlockStmt - startOfLine == 27) {
                                        blockHighlighter.addHighlight(startOfBlockStmt, endOfBlockStmt, blockPainter);
                                    }
                                }
                                break;
                            } // End of case RPG RPG III

                            case "CL": {
                                String line = textToHighlight.substring(startOfLine, endOfLine);
                                int commentLeftPos = line.indexOf("/*");
                                int commentRightPos = line.indexOf("*/");
                                // One comment exists in the line and the block statement is outside
                                // (We do not assume that there are more comments in the line.)
                                if (commentRightPos > 4 && commentLeftPos < commentRightPos
                                        && (endOfBlockStmt <= startOfLine + commentLeftPos
                                        || startOfBlockStmt >= startOfLine + commentRightPos + "*/".length())) {
                                    blockHighlighter.addHighlight(startOfBlockStmt, endOfBlockStmt, blockPainter);
                                } // Highlight block statement if there is no
                                // comment in line
                                else if (commentLeftPos == -1) {
                                    blockHighlighter.addHighlight(startOfBlockStmt, endOfBlockStmt, blockPainter);
                                }
                                break;
                            } // End of case CL

                            case "COBOL": {
                                // No asterisk or slash comment (* or / in column 7)
                                // and the block statement is in columns 12 to 72
                                if (textToHighlight.length() >= 7) {
                                    if (!textToHighlight.substring(startOfLine + 6, startOfLine + 7).equals("*")
                                            && !textToHighlight.substring(startOfLine + 6, startOfLine + 7).equals("/")
                                            //&& startOfBlockStmt - startOfLine >= 11
                                            && endOfBlockStmt - startOfLine <= 72) {
                                        blockHighlighter.addHighlight(startOfBlockStmt, endOfBlockStmt, blockPainter);
                                    }
                                }
                                break;
                            } // End of case COBOL

                            case "C": {
                                String line = textToHighlight.substring(startOfLine, endOfLine);
                                int doubleSlashPos = line.indexOf("//");
                                int commentLeftPos = line.indexOf("/*");
                                int commentRightPos = line.indexOf("*/");
                                // One comment exists in the line and the block statement is outside
                                // (We do not assume that there are more comments in the line.)
                                if (commentRightPos > 4 && commentLeftPos < commentRightPos
                                        && commentLeftPos > 0
                                        && (endOfBlockStmt <= startOfLine + commentLeftPos
                                        || startOfBlockStmt >= startOfLine + commentRightPos + "*/".length())) {
                                    blockHighlighter.addHighlight(startOfBlockStmt, endOfBlockStmt, blockPainter);
                                } else if (doubleSlashPos > -1) {
                                    if (endOfBlockStmt <= startOfLine + doubleSlashPos) {
                                        blockHighlighter.addHighlight(startOfBlockStmt, endOfBlockStmt, blockPainter);
                                    }
                                } // Highlight block statement if there is no comment in line
                                else if (commentLeftPos == -1) {
                                    blockHighlighter.addHighlight(startOfBlockStmt, endOfBlockStmt, blockPainter);
                                }
                                break;
                            } // End of case C

                            case "C++": {
                                String line = textToHighlight.substring(startOfLine, endOfLine);
                                int doubleSlashPos = line.indexOf("//");
                                int commentLeftPos = line.indexOf("/*");
                                int commentRightPos = line.indexOf("*/");
                                // One comment exists in the line and the block statement is outside
                                // (We do not assume that there are more comments in the line.)
                                if (commentRightPos > 4 && commentLeftPos < commentRightPos
                                        && commentLeftPos > 0
                                        && (endOfBlockStmt <= startOfLine + commentLeftPos
                                        || startOfBlockStmt >= startOfLine + commentRightPos + "*/".length())) {
                                    blockHighlighter.addHighlight(startOfBlockStmt, endOfBlockStmt, blockPainter);
                                } else if (doubleSlashPos > -1) {
                                    if (endOfBlockStmt <= startOfLine + doubleSlashPos) {
                                        blockHighlighter.addHighlight(startOfBlockStmt, endOfBlockStmt, blockPainter);
                                    }
                                } // Highlight block statement if there is no comment in line
                                else if (commentLeftPos == -1) {
                                    blockHighlighter.addHighlight(startOfBlockStmt, endOfBlockStmt, blockPainter);
                                }
                                break;
                            } // End of case C++

                            case "SQL": {
                                String line = textToHighlight.substring(startOfLine, endOfLine);
                                int specialCommentPos = line.indexOf("--;"); // Trigraph - special SQL line comment
                                int dashCommentPos = line.indexOf("--"); // // Double dash - ordinary SQL line comment
                                if ((blockStmt.equals("--;")) && specialCommentPos == 0) {
                                    // Highlight special comment line beginning at the start of line. Nowhere else.
                                    // Other comments are not hightlighted (double dashes and slash-asterisks).
                                    blockHighlighter.addHighlight(startOfBlockStmt, endOfBlockStmt, blockPainter);
                                } else if (specialCommentPos == -1 && dashCommentPos == -1) {
                                    // Highlight block statements other than special comment (--; trigraph) or dash comment (--). 
                                    // These block statements (SELECT, FROM, ...) are highlighted.
                                    blockHighlighter.addHighlight(startOfBlockStmt, endOfBlockStmt, blockPainter);
                                    // All question marks are highlighted in the whole textToHighlight area
                                    highlightSqlQuestionMarks();
                                }
                                break;
                            } // End of case SQL

                        } // End of switch
                    }
                }
                // Get next line
                startOfLine = textToHighlight.indexOf(NEW_LINE, startOfLine) + NEW_LINE.length();
                endOfLine = textToHighlight.indexOf(NEW_LINE, startOfLine);
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    /**
     * Highlighting question marks in the whole SQL script (text area).
     */
    protected void highlightSqlQuestionMarks() {
        LayeredHighlighter sqlQuestionMarkHighlighter = (LayeredHighlighter) textArea.getHighlighter();
        String stringPattern = "?";
        try {
            stringPattern = stringPattern.replace("?", "\\?");
            Pattern pattern = Pattern.compile(stringPattern);
            if (stringPattern == null) {
                return;
            }
            if (Objects.nonNull(stringPattern)) {
                Matcher matcher = pattern.matcher(textArea.getText(0, textArea.getText().length()));
                int pos = 0;
                int start = 0;
                int end = 0;
                while (matcher.find(pos)) {
                    start = matcher.start();
                    end = matcher.end();
                    sqlQuestionMarkHighlighter.addHighlight(start, end, blockRedDarker);
                    pos = end;
                }
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Find all matches and highlight it YELLOW (highlightPainter),
     * then hihglight current match ORANGE for PRIMARY text area.
     */
    protected void changeHighlight() {
        LayeredHighlighter layeredHighlighter = (LayeredHighlighter) textArea.getHighlighter();
        layeredHighlighter.removeAllHighlights();
        findWindow.findField.setBackground(Color.WHITE);
        try {
            Pattern pattern = findWindow.getPattern();
            if (pattern == null) {
                return;
            }
            if (Objects.nonNull(pattern)) {
                startOffsets = new ArrayList<>();
                endOffsets = new ArrayList<>();
                highlightMap.clear();
                Matcher matcher = pattern.matcher(textArea.getText(0, textArea.getText().length()));
                int pos = 0;
                int start = 0;
                int end = 0;
                while (matcher.find(pos)) {
                    start = matcher.start();
                    end = matcher.end();
                    layeredHighlighter.addHighlight(start, end, highlightPainter);
                    startOffsets.add(start);
                    endOffsets.add(end);
                    highlightMap.put(start, end);
                    pos = end;
                }
            }
            JLabel label = findWindow.layerUI.hint;
            LayeredHighlighter.Highlight[] array = layeredHighlighter.getHighlights();
            int hits = array.length; // number of highlighted intervals.
            if (hits > 0) { // If at least one interval was found.
                if (findWindow.direction.equals("forward")) {
                    // Forward direction
                    if (curPos == null) {
                        startOffset = null;
                    } else {
                        startOffset = highlightMap.ceilingKey(curPos); // Get next interval start - greater or equal
                    }
                    if (startOffset == null) {
                        startOffset = highlightMap.firstKey(); // First interval
                    }
                    endOffset = highlightMap.get(startOffset);     // This interval's end
                    sequence = startOffsets.indexOf(startOffset);  // Sequence number of the interval
                    LayeredHighlighter.Highlight hh = layeredHighlighter.getHighlights()[sequence];
                    layeredHighlighter.removeHighlight(hh);
                    layeredHighlighter.addHighlight(startOffset, endOffset, currentPainter);
                    curPos = startOffset;
                    textArea.setCaretPosition(endOffset);
                } else {
                    // Backward direction
                    if (curPos == null) {
                        startOffset = null;
                    } else {
                        startOffset = highlightMap.floorKey(curPos); // Get previous interval start - less or equal
                    }
                    if (startOffset == null) {
                        startOffset = highlightMap.lastKey(); // Last interval
                    }
                    endOffset = highlightMap.get(startOffset);
                    sequence = startOffsets.indexOf(startOffset);
                    LayeredHighlighter.Highlight hh = layeredHighlighter.getHighlights()[sequence];
                    layeredHighlighter.removeHighlight(hh);
                    layeredHighlighter.addHighlight(startOffset, endOffset, currentPainter);
                    curPos = startOffset;
                    textArea.setCaretPosition(startOffset);
                }
            }

            if (hits > 0) {
                label.setText(String.format("%02d / %02d%n", sequence + 1, hits));
            } else {
                label.setText("");
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
        findWindow.findField.repaint();
    }

    /**
     * Find all matches and highlight it YELLOW (highlightPainter),
     * then hihglight current match ORANGE for SECONDARY text area.
     */
    protected void changeHighlight2() {
        LayeredHighlighter layeredHighlighter2 = (LayeredHighlighter) textArea2.getHighlighter();
        layeredHighlighter2.removeAllHighlights();
        findWindow.findField.setBackground(Color.WHITE);
        try {
            Pattern pattern = findWindow.getPattern();
            if (pattern == null) {
                return;
            }
            if (Objects.nonNull(pattern)) {
                startOffsets = new ArrayList<>();
                endOffsets = new ArrayList<>();
                highlightMap.clear();
                Matcher matcher = pattern.matcher(textArea2.getText(0, textArea2.getText().length()));
                int pos = 0;
                int start = 0;
                int end = 0;
                while (matcher.find(pos)) {
                    start = matcher.start();
                    end = matcher.end();
                    layeredHighlighter2.addHighlight(start, end, highlightPainter);
                    startOffsets.add(start);
                    endOffsets.add(end);
                    highlightMap.put(start, end);
                    pos = end;
                }
            }
            JLabel label = findWindow.layerUI.hint;
            LayeredHighlighter.Highlight[] array = layeredHighlighter2.getHighlights();
            int hits = array.length;
            if (hits > 0) {
                if (findWindow.direction.equals("forward")) {
                    // Forward direction
                    if (curPos2 == null) {
                        startOffset2 = null;
                    } else {
                        startOffset2 = highlightMap.ceilingKey(curPos2); // Get next interval start - greater or equal
                    }
                    if (startOffset2 == null) {
                        startOffset2 = highlightMap.ceilingKey(0);
                    }
                    endOffset2 = highlightMap.get(startOffset2);
                    sequence2 = startOffsets.indexOf(startOffset2);
                    LayeredHighlighter.Highlight hh = layeredHighlighter2.getHighlights()[sequence2];
                    layeredHighlighter2.removeHighlight(hh);
                    layeredHighlighter2.addHighlight(startOffset2, endOffset2, currentPainter);
                    curPos2 = startOffset2;
                    textArea2.setCaretPosition(endOffset2);
                } else {
                    // Backward direction
                    if (curPos2 == null) {
                        startOffset2 = null;
                    } else {
                        startOffset2 = highlightMap.lowerKey(curPos2); // Get previous interval start - less or equal
                    }
                    if (startOffset2 == null) {
                        startOffset2 = highlightMap.lastKey();
                    }
                    endOffset2 = highlightMap.get(startOffset2);
                    sequence2 = startOffsets.indexOf(startOffset2);
                    LayeredHighlighter.Highlight hh = layeredHighlighter2.getHighlights()[sequence2];
                    layeredHighlighter2.removeHighlight(hh);
                    layeredHighlighter2.addHighlight(startOffset2, endOffset2, currentPainter);
                    curPos2 = startOffset2;
                    textArea2.setCaretPosition(startOffset2);
                }
            }
            if (hits > 0) {
                label.setText(String.format("%02d / %02d%n", sequence2 + 1, hits));
            } else {
                label.setText("");
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
        findWindow.findField.repaint();
    }

    /**
     * Split the text area view to an upper primary area and lower secondary area.
     */
    protected void splitTextArea() {

        // Initially, the document listener for the primary text area is not set.

        // Copy text from the primary to the secondary text area
        textArea2.setText(textArea.getText());

        // Set background for secondary text area
        if (methodName.equals("rewritePcFile")) {
            textArea2.setBackground(VERY_LIGHT_PINK);
        } else {
            textArea2.setBackground(VERY_LIGHT_BLUE);
        }

        // Set caret shapes for selection modes in the secondary text area
        if (selectionModeButton.getText().equals(HORIZONTAL_SELECTION)) {
            // Horizontal selection
            if (caretShape.equals(LONG_CARET)) {
                // Long caret
                textArea2.setCaret(longCaret2);
            } else {
                // Short basic caret
                textArea2.setCaret(basicCaret2);
            }
        } else {
            // Vertical selection
            textArea2.setCaret(specialCaret2);
        }

        // Build upper and lower scroll panes, and a split vertical scroll pane
        scrollPaneUpper = new JScrollPane();
        scrollPaneUpper.setViewportView(textArea);
        scrollPaneUpper.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPaneUpper.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        scrollPaneLower = new JScrollPane();
        scrollPaneLower.setViewportView(textArea2);
        scrollPaneLower.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPaneLower.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        splitVerticalPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitVerticalPane.setPreferredSize(new Dimension(windowWidth, windowHeight));
        splitVerticalPane.setBorder(BorderFactory.createEmptyBorder());

        splitVerticalPane.setTopComponent(scrollPaneUpper);
        splitVerticalPane.setBottomComponent(scrollPaneLower);

        splitVerticalPane.setDividerSize(6);

        double splitVerticalPaneDividerLoc = 0.50d; // 50 %
        splitVerticalPane.setDividerLocation(splitVerticalPaneDividerLoc);

        // Stabilize vertical divider always in the middle
        splitVerticalPane.setResizeWeight(0.5);
        splitVerticalPane.setAlignmentX(CENTER_ALIGNMENT);

        // Remove global panel and create it again
        this.remove(globalPanel);
        globalPanel = new JPanel();
        // Renew global panel layout
        GroupLayout topPanelLayout = new GroupLayout(globalPanel);
        topPanelLayout.setHorizontalGroup(topPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(rowPanel2)
                .addComponent(rowPanel1)
                .addComponent(splitVerticalPane)
        );
        topPanelLayout.setVerticalGroup(topPanelLayout.createSequentialGroup()
                .addComponent(rowPanel2)
                .addGap(2)
                .addComponent(rowPanel1)
                .addGap(4)
                .addComponent(splitVerticalPane)
        );
        // Set global panel layout
        globalPanel.setLayout(topPanelLayout);
        globalPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        // Add global panel to this (JFrame)
        add(globalPanel);

        // Get a highlighter for the primary text area
        blockHighlighter = textArea.getHighlighter();
        // Hightlight only if the option is not *NONE
        if (!progLanguage.equals("*NONE")) {
            highlightBlocks(textArea);
        }

        // Get a highlighter for the secondary text area
        blockHighlighter = textArea2.getHighlighter();
        // Hightlight only if the option is not *NONE
        if (!progLanguage.equals("*NONE")) {
            highlightBlocks(textArea2);
        }

        // Add document listener for the secondary text area 
        // for the first time and next time when the view is being split.
        textArea2.getDocument().addDocumentListener(textArea2DocListener);

        // Add also document listener for the primary text area.
        textArea.getDocument().addDocumentListener(textAreaDocListener);

        // Show the window
        setVisible(true);
        changeHighlight2();
    }

    /**
     * Unsplit editor area to contain only primary text area.
     */
    protected void unsplitTextArea() {

        lowerHalfActive = false;

        textArea.requestFocus();

        textArea2.getDocument().removeDocumentListener(textArea2DocListener);
        textArea.getDocument().removeDocumentListener(textAreaDocListener);

        // Create a new scroll pane
        scrollPane = new JScrollPane(textArea);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Now the scroll pane may be sized because window height is defined
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        scrollPane.setPreferredSize(new Dimension(windowWidth, windowHeight));

        this.remove(globalPanel);
        globalPanel = new JPanel();
        GroupLayout topPanelLayout = new GroupLayout(globalPanel);
        topPanelLayout.setHorizontalGroup(topPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(rowPanel2)
                .addComponent(rowPanel1)
                .addComponent(scrollPane)
        );
        topPanelLayout.setVerticalGroup(topPanelLayout.createSequentialGroup()
                .addComponent(rowPanel2)
                .addGap(2)
                .addComponent(rowPanel1)
                .addGap(4)
                .addComponent(scrollPane)
        );
        globalPanel.setLayout(topPanelLayout);

        globalPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        add(globalPanel);

        this.setVisible(true);

        if (findWindow.findField.getText().isEmpty()) {
            // Get a highlighter for the secondary text area
            blockHighlighter = textArea.getHighlighter();
            // Hightlight only if the option is not *NONE
            if (!progLanguage.equals("*NONE")) {
                highlightBlocks(textArea);
            }
        }

        textArea.setCaretPosition(caretPosition);
    }

    /**
     * Create String of spaces with a given length
     *
     * @param length
     * @return
     */
    private String fixedLengthSpaces(int length) {
        char[] spaces = new char[length];
        for (int idx = 0; idx < length; idx++) {
            spaces[idx] = ' ';
        }
        return String.copyValueOf(spaces);
    }

    /**
     * Button listener for buttons Find (<,>) and Replace and Replace+Find;
     * Note: "ReplaceAll" button has different action listener.
     */

    class HighlightListener implements DocumentListener {

        @Override
        public void changedUpdate(DocumentEvent de) {
            // not applied 
        }

        @Override
        public void insertUpdate(DocumentEvent de) {
            // Find next match
            if (!lowerHalfActive) {
                changeHighlight();
            } else {
                changeHighlight2();
            }
        }

        @Override
        public void removeUpdate(DocumentEvent de) {
            // Find next match
            if (!lowerHalfActive) {
                changeHighlight();
            } else {
                changeHighlight2();
            }
        }
    }

    /**
     * Undoable listener.
     */
    class UndoHandler implements UndoableEditListener {

        /**
         * Messaged when the Document has created an edit, the edit is added to "undo", an instance of UndoManager.
         */
        @Override
        public void undoableEditHappened(UndoableEditEvent uee) {
            undo.addEdit(uee.getEdit());
            undoAction.update();
            redoAction.update();
        }
    }

    /**
     * Undo action.
     */
    class UndoAction extends AbstractAction {

        public UndoAction() {
            super("Undo");
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                undo.undo();
            } catch (CannotUndoException cue) {
                cue.printStackTrace();
            }
            update();
            redoAction.update();
        }

        protected void update() {
            if (undo.canUndo()) {
                setEnabled(true);
                putValue(Action.NAME, undo.getUndoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Undo");
            }
        }
    }

    /**
     * Redo action.
     */
    class RedoAction extends AbstractAction {

        public RedoAction() {
            super("Redo");
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            try {
                undo.redo();
            } catch (CannotRedoException cre) {
                cre.printStackTrace();
            }
            update();
            undoAction.update();
        }

        protected void update() {
            if (undo.canRedo()) {
                setEnabled(true);
                putValue(Action.NAME, undo.getRedoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Redo");
            }
        }
    }

    /**
     * Inner class for Ctrl + S (Save) function key.
     */
    class SaveAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent ae) {
            // Save edited data from text area back to the member
            caretPosition = textArea.getCaretPosition();
            rewriteFile();

            textChanged = false; // Save button gets the original color
            checkTextChanged();

            textArea.setCaretPosition(caretPosition);
            textArea.requestFocus();
        }
    }

    /**
     * Extract individual names (libraryName, fileName, memberName) from the AS400 IFS path.
     *
     * @param as400PathString
     */
    protected void extractNamesFromIfsPath(String as400PathString) {

        qsyslib = "/QSYS.LIB/";
        if (as400PathString.startsWith(qsyslib) && as400PathString.length() > qsyslib.length()) {
            libraryName = as400PathString.substring(as400PathString.indexOf("/QSYS.LIB/")
                    + 10, as400PathString.lastIndexOf(".LIB"));
            if (as400PathString.length() > qsyslib.length() + libraryName.length() + 5) {
                fileName = as400PathString.substring(qsyslib.length() + libraryName.length()
                        + 5, as400PathString.lastIndexOf(".FILE"));
                if (as400PathString.length() > qsyslib.length() + libraryName.length() + 5 + fileName.length() + 6) {
                    memberName = as400PathString.substring(qsyslib.length() + libraryName.length()
                            + 5
                            + fileName.length()
                            + 6, as400PathString.lastIndexOf(".MBR"));
                }
            }
        }
    }

    /**
     * Inner class for Compile button.
     */
    class CompileButtonListener extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent ae) {
            if (filePathString.startsWith("/QSYS.LIB")) {
                if (mainWindow.compile == null) {
                    mainWindow.compile = new Compile(remoteServer, mainWindow, filePathString, false);
                }
                // "false" means NOT IFS file. (It is a source member.)
                mainWindow.compile.compile(filePathString, false);

            } else {
                if (mainWindow.compile == null) {
                    mainWindow.compile = new Compile(remoteServer, mainWindow, filePathString, true);
                }
                // "true" means IFS file.
                mainWindow.compile.compile(filePathString, true);
            }
        }
    }

    /**
     * Inner class for Escape function key.
     */
    class Escape extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent de) {
            dispose();
        }
    }

    /**
     * Inner class for Ctrl + F function key.
     */
    class CreateFindWindow extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent ae) {
            if (findWindow != null) {
                if (!lowerHalfActive) {
                    if (selectionMode.equals(HORIZONTAL_SELECTION)) {
                        // Horizontal selection
                        findWindow.finishCreatingWindow(textArea.getSelectedText());
                    } else {
                        // Vertical selection
                        if (!selectionStarts.isEmpty()) {
                            textArea.select(selectionStarts.get(0), selectionEnds.get(0));
                            findWindow.finishCreatingWindow(textArea.getSelectedText());
                        } else {
                            findWindow.finishCreatingWindow("");
                        }
                    }
                } else {
                    if (selectionMode.equals(HORIZONTAL_SELECTION)) {
                        findWindow.finishCreatingWindow(textArea2.getSelectedText());
                    } else {
                        if (!selectionStarts.isEmpty()) {
                            textArea2.select(selectionStarts.get(0), selectionEnds.get(0));
                            findWindow.finishCreatingWindow(textArea2.getSelectedText());
                        } else {
                            findWindow.finishCreatingWindow("");
                        }
                    }
                }
            }
        }
    }

    /**
     * Shift selected area (primary or secondary) left by one position.
     */
    protected void shiftLeft() {
        JTextArea tArea;
        if (!lowerHalfActive) {
            tArea = textArea;
        } else {
            tArea = textArea2;
        }
        if (selectionMode.equals(HORIZONTAL_SELECTION)) {

            // Horizontal selection
            selectedText = tArea.getSelectedText();
            selectionStart = tArea.getSelectionStart();
            int numberOfLines = 0;
            if (selectedText != null) {
                String[] strArr = selectedText.split("\n");
                int minPos = 1;
                if (strArr.length > 0) {
                    // If there are some lines selected, inspect all selected lines 
                    // to get position of the leftmost non-blank character
                    for (int idx = 0; idx < strArr.length; idx++) {
                        int position = 0;
                        for (position = 0; position < strArr[idx].length(); position++) {
                            if (!strArr[idx].isEmpty()) {  // If the line is not empty 
                                // Get position of the left-most non-space character (or zero)
                                if (strArr[idx].charAt(position) != ' ') {
                                    if (position < minPos) {
                                        minPos = position;
                                    }
                                }
                            }
                        }
                    }
                    shiftedText = "";
                    if (minPos > 0) {
                        // 
                        for (numberOfLines = 0; numberOfLines < strArr.length; numberOfLines++) {
                            if (!strArr[numberOfLines].isEmpty()) {
                                // Shift the non-empty line 1 position left and add a new line character.
                                strArr[numberOfLines] = strArr[numberOfLines].substring(1);
                                shiftedText += strArr[numberOfLines] + "\n";
                            } else {
                                // For empty line add a new line character.
                                shiftedText += " \n"; // 2 characters added
                            }
                        }
                        if (!selectedText.endsWith("\n")) {
                            // If the line does not end with a new line character (a selection in single-line)
                            // select the text one character shorter.
                            shiftedText = shiftedText.substring(0, shiftedText.length() - 1);
                        }
                        tArea.replaceSelection(shiftedText);
                    }
                    // Select the shifted text
                    tArea.requestFocus();
                    tArea.select(selectionStart, selectionStart + shiftedText.length());
                }
            }
        } else {

            // Vertical selection
            int cnt = selectionStarts.size();
            int idx = 0;
            try {
                while (idx < cnt) {
                    startSel = selectionStarts.get(idx);
                    endSel = selectionEnds.get(idx);
                    int line = tArea.getLineOfOffset(startSel);
                    int lineStartOffset = tArea.getLineStartOffset(line);
                    if (startSel > lineStartOffset) {
                        String selectedText = tArea.getText(startSel, endSel - startSel);
                        if (!selectedText.isEmpty()) {
                            // Insert selected text followed by a space in place of the row selection (= shif left 1 position)
                            tArea.replaceRange(selectedText + " ", startSel - 1, endSel);
                            tArea.getHighlighter().addHighlight(startSel - 1, endSel - 1, DefaultHighlighter.DefaultPainter);
                            selectionStarts.set(idx, startSel - 1);
                            selectionEnds.set(idx, endSel - 1);
                        }
                    }
                    idx++;
                }
                tArea.setCaretPosition(startSel - 1);
            } catch (Exception exc) {
                System.out.println("Error in 'tArea.getLineOfOffset(startSel)': " + exc.toString());
                exc.printStackTrace();
            }
        }
    }

    /**
     * Inner class for Ctrl + Arrow Left function key (shift left by one position).
     */
    class ArrowLeft extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent ae) {
            shiftLeft();
        }
    }

    /**
     * Shift selected area (primary or secondary) right by one position.
     */
    protected void shiftRight() {
        JTextArea tArea;
        if (!lowerHalfActive) {
            tArea = textArea;
        } else {
            tArea = textArea2;
        }
        if (selectionMode.equals(HORIZONTAL_SELECTION)) {

            // Horizontal selection
            selectedText = tArea.getSelectedText();
            selectionStart = tArea.getSelectionStart();
            int lineNbr = 0;
            char[] charArr = new char[1];
            Arrays.fill(charArr, ' ');

            if (selectedText != null) {
                String[] strArr = selectedText.split("\n");
                String[] lines = new String[strArr.length];
                shiftedText = "";
                for (lineNbr = 0; lineNbr < strArr.length; lineNbr++) {
                    lines[lineNbr] = String.valueOf(charArr) + strArr[lineNbr].substring(0, strArr[lineNbr].length());
                    shiftedText += lines[lineNbr] + "\n";
                }
                if (!selectedText.endsWith("\n")) {
                    shiftedText = shiftedText.substring(0, shiftedText.length() - 1);
                }
                tArea.replaceSelection(shiftedText);
                // Select shifted text
                tArea.requestFocus();
                tArea.select(selectionStart, selectionStart + shiftedText.length());
            }
        } else {

            // Vertical selection
            int cnt = selectionStarts.size();
            boolean eol = false;
            try {

                // Check if at least one selection end is at line end (excluding empty lines).
                int lineNbr = tArea.getLineOfOffset(selectionStarts.get(0));
                int lineStart = tArea.getLineStartOffset(lineNbr);
                int lineEnd = tArea.getText().indexOf("\n", lineStart);
                for (int jdx = 0; jdx < cnt; jdx++) {
                    endSel = selectionEnds.get(jdx);
                    if (lineEnd > lineStart && endSel == lineEnd) {
                        // Line is not empty and rectangle end is at end of the shortest line
                        eol = true;
                    }
                    lineNbr++;
                    lineStart = tArea.getLineStartOffset(lineNbr);
                    lineEnd = tArea.getText().indexOf("\n", lineStart);
                }
                // If the rectangle is at end of any line, it stops shifting.
                if (eol) {
                    return;
                }
                // If the rectangle is not at end lines it is shifted one position right. 
                int idx = 0;
                while (idx < cnt) {
                    startSel = selectionStarts.get(idx);
                    endSel = selectionEnds.get(idx);
                    if (endSel == lineEnd) {
                        break;
                    }
                    String selectedText = tArea.getText(startSel, endSel - startSel);
                    // Process non-empty lines, empty lines are skipped.
                    if (!selectedText.isEmpty()) {
                        // Insert a space plus selected text at the selection start.
                        tArea.replaceRange(" " + selectedText, startSel, endSel + 1);
                        //tArea.select(startSel + 1, endSel + 1);
                        tArea.getHighlighter().addHighlight(startSel + 1, endSel + 1, DefaultHighlighter.DefaultPainter);
                        selectionStarts.set(idx, startSel + 1);
                        selectionEnds.set(idx, endSel + 1);
                    }
                    idx++;
                }
                tArea.setCaretPosition(endSel + 1);
            } catch (Exception exc) {
                System.out.println("Error: " + exc.toString());
                exc.printStackTrace();
            }

        }
    }

    /**
     * Inner class for Ctrl + Arrow Right function key (shift right by one position).
     */
    class ArrowRight extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent ae) {
            shiftRight();
        }
    }

    /**
     * Inner class for Ctrl C - Custom copy.
     */
    class CustomCopy extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent ae) {
            JTextArea tArea;
            if (!lowerHalfActive) {
                tArea = textArea;
            } else {
                tArea = textArea2;
            }
            if (selectionMode.equals(HORIZONTAL_SELECTION)) {
                // Horiontal selection
                int startSel = tArea.getSelectionStart();
                int endSel = tArea.getSelectionEnd();
                try {
                    selectedText = tArea.getText(startSel, endSel - startSel);
                    StringSelection stringSelections = new StringSelection(selectedText);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelections, stringSelections);
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            } else {
                // Vertical selection
                selectedText = "";
                try {
                    int cnt = selectionStarts.size();
                    selectedArray = new String[cnt];
                    for (int idx = 0; idx < cnt; idx++) {
                        int start = selectionStarts.get(idx);
                        int end = selectionEnds.get(idx);
                        selectedArray[idx] = tArea.getText(start, end - start);
                        selectedText += selectedArray[idx] + '\n';
                        StringSelection stringSelections = new StringSelection(selectedText);
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelections, stringSelections);
                    }
                    // In order to paste the copied area again with copied text
                    // set caret to its original position that it has before the operation
                    int caretPos = selectionStarts.get(0);
                    tArea.setCaretPosition(caretPos);
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        }
    }

    /**
     * Inner class for Ctrl + X - Custom cut.
     */
    class CustomCut extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent ae) {
            JTextArea tArea;
            if (!lowerHalfActive) {
                tArea = textArea;
            } else {
                tArea = textArea2;
            }
            if (selectionMode.equals(HORIZONTAL_SELECTION)) {
                // Horiontal selection
                int startSel = tArea.getSelectionStart();
                int endSel = tArea.getSelectionEnd();
                try {
                    selectedText = tArea.getText(startSel, endSel - startSel);
                    StringSelection stringSelections = new StringSelection(selectedText);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelections, stringSelections);
                    tArea.replaceRange("", startSel, endSel);
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            } else {
                // Vertical selection
                selectedText = "";
                int origCaretPos = selectionStarts.get(0);
                try {
                    int cnt = selectionStarts.size();
                    selectedArray = new String[cnt];
                    for (int idx = 0; idx < cnt; idx++) {
                        int start = selectionStarts.get(idx);
                        int end = selectionEnds.get(idx);
                        selectedArray[idx] = tArea.getDocument().getText(start, end - start);
                        selectedText += selectedArray[idx] + '\n';
                        StringSelection stringSelections = new StringSelection(selectedText);
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelections, stringSelections);
                        char[] charArr = new char[end - start];
                        Arrays.fill(charArr, ' ');
                        tArea.replaceRange(String.valueOf(charArr), start, end);
                    }
                    // In order to paste the cut area again with cut text
                    // set caret to its original position that it has before the operation.
                    tArea.setCaretPosition(origCaretPos);
                    selectionStarts.clear();
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        }
    }

    /**
     * Inner class for Ctrl + V - Custom paste.
     */
    class CustomPaste extends AbstractAction {

        int cnt = 0;
        int lineNbr = 0;
        int lineNbrFirst = 0;
        int padLen = 0;

        @Override
        public void actionPerformed(ActionEvent ae) {
            JTextArea tArea;
            if (!lowerHalfActive) {
                tArea = textArea;
            } else {
                tArea = textArea2;
            }
            if (selectionMode.equals(HORIZONTAL_SELECTION)) {

                // Horiontal selection
                int caretPosition = tArea.getCaretPosition();
                Transferable tran = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(tArea);
                DataFlavor df = DataFlavor.stringFlavor;
                try {
                    String textFromClipboard = (String) tran.getTransferData(df);
                    if (caretPosition < tArea.getText().length()) {
                        tArea.replaceSelection(textFromClipboard);
                    } else {
                        tArea.append(textFromClipboard);
                    }
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            } else {

                // Vertical selection
                int caretPos = tArea.getCaretPosition();
                Transferable tran = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(tArea);
                DataFlavor df = DataFlavor.stringFlavor;
                try {
                    String textFromClipboard = (String) tran.getTransferData(df);
                    selectedArray = textFromClipboard.split("\n");

                    int lineNbr = tArea.getLineOfOffset(caretPos);
                    int lineNbrFirst = lineNbr;
                    int lineStart = tArea.getLineStartOffset(lineNbr);
                    int offset = caretPos - lineStart; // constant distance
                    int lineEnd = 0;
                    int selLenMax = 0;
                    for (cnt = 0; cnt < selectedArray.length; cnt++) {
                        if (selectedArray[cnt].length() > selLenMax) {
                            // Maximum of lengths of selected array elements
                            selLenMax = selectedArray[cnt].length();
                        }
                    }
                    // Replace characters in the text area with selected text (from Copy or Cut) starting from the caret position.
                    for (cnt = 0; cnt < selectedArray.length; cnt++) {
                        lineEnd = tArea.getText().indexOf("\n", lineStart);
                        int selLen = selectedArray[cnt].length(); // Length of selected text in the source line
                        int selPosMax = caretPos + selLenMax; // Maximum position of a character to be replaced
                        // Get maximum length of selected texts from all lines selected.                                               
                        String sel = selectedArray[cnt]; // Text of the idx-th selection 
                        // Number of spaces to pad 
                        padLen = selPosMax - lineEnd;
                        // If length of padded spaces is positive, insert spaces at the end of the line
                        if (padLen > 0) {
                            char[] charArr = new char[padLen];
                            Arrays.fill(charArr, ' ');
                            try {
                                tArea.insert(String.valueOf(charArr), lineEnd);
                            } catch (IllegalArgumentException iae) {
                                // If the number of selected lines exceeds the number of target lines available
                                // a new target line is appended for each exceeding selected line.
                                System.out.println("cnt: " + cnt);
                                System.out.println("Target line number: " + lineNbr);
                                System.out.println("lineNbr - lineNbrFirst: " + (lineNbr - lineNbrFirst));
                                System.out.println("selectedArray.length: " + selectedArray.length);
                                System.out.println("padLen: " + padLen);
                                charArr = new char[caretPos + selLen - lineStart];
                                Arrays.fill(charArr, ' ');
                                tArea.append(String.valueOf(charArr) + "\n");
                            }
                        }
                        // Replace characters from caret position in the selection length with the selection
                        tArea.replaceRange(sel, caretPos, caretPos + selLen);
                        lineNbr++; // Get next line of the text area
                        lineStart = tArea.getLineStartOffset(lineNbr);
                        caretPos = lineStart + offset;
                    }
                    // Get a highlighter for the secondary text area
                    blockHighlighter = tArea.getHighlighter();
                    // Hightlight only if the option is not *NONE
                    if (!progLanguage.equals("*NONE")) {
                        highlightBlocks(tArea);
                    }
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        }
    }

    /**
     * Inner class implementing Delete key and Backspace key actions in vertical selection mode.
     */
    class CustomDelete extends AbstractAction {

        String key;

        // Constructor
        CustomDelete(String key) {
            this.key = key;
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            JTextArea tArea;
            if (!lowerHalfActive) {
                tArea = textArea;
            } else {
                tArea = textArea2;
            }
            int cnt = selectionStarts.size();
            // When NO TEXT is selected, delete one position only (preceding or next)
            if (cnt == 0) {
                caretPosition = tArea.getCaretPosition();
                if (key.equals("BACKSPACE")) {
                    // BACKSPACE key
                    tArea.replaceRange("", caretPosition - 1, caretPosition);
                } else {
                    // DEL key
                    tArea.replaceRange("", caretPosition, caretPosition + 1);
                }
            } else {
                // When a TEXT IS SELECTED, delete selections in all lines
                try {
                    String[] lines = tArea.getText().split("\n");
                    int startSel0 = selectionStarts.get(0);
                    int lineNbr0 = tArea.getLineOfOffset(startSel0);
                    int lineStartOffset0 = tArea.getLineStartOffset(lineNbr0);
                    int lineEnd0 = tArea.getText().indexOf("\n", lineStartOffset0);
                    int lineLen0 = lineEnd0 - lineStartOffset0;
                    for (int idx = cnt - 1; idx >= 0; idx--) {
                        startSel = selectionStarts.get(idx);
                        endSel = selectionEnds.get(idx);
                        int diff = endSel - startSel;
                        int lineNbr = tArea.getLineOfOffset(startSel);
                        int lineStartOffset = tArea.getLineStartOffset(lineNbr);
                        int lineEnd = tArea.getText().indexOf("\n", lineStartOffset);
                        if (diff < lineLen0) {
                            // Partial selection of the line
                            if (!lines[lineNbr].isEmpty()) {
                                tArea.replaceRange(tArea.getText().substring(endSel, lineEnd), startSel, lineEnd);
                            }
                        } else {
                            // Whole line selected
                            int lastLineNbr = tArea.getLineOfOffset(endSel);
                            int lastLineStartOffset = tArea.getLineStartOffset(lastLineNbr);
                            int lastLineEnd = tArea.getText().indexOf('\n', lastLineStartOffset);
                            tArea.replaceRange("", startSel0, lastLineEnd + 1);
                        }
                    }
                    // Set caret to the start position in the FIRST line of the selection
                    tArea.setCaretPosition(startSel0);
                    selectionStarts.clear();

                    // Get a highlighter for the secondary text area
                    blockHighlighter = tArea.getHighlighter();
                    // Hightlight only if the option is not *NONE
                    if (!progLanguage.equals("*NONE")) {
                        highlightBlocks(tArea);
                    }
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        }
    }

    /**
     * Inner class for Tab function key; Inserts TAB_SIZE spaces in caret position.
     */
    class TabListener extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent ae) {
            textArea.insert(fixedLengthSpaces(TAB_SIZE), textArea.getCaretPosition());
        }
    }

    /**
     * Implements custom caret as a long vertical line with a short red line pointer
     * for primary text area.
     */
    public class LongCaret extends DefaultCaret {

        @Override
        public void damage(Rectangle verticalLine) {
            // give values to x, y, width,height (inherited from java.awt.Rectangle)
            x = verticalLine.x;
            y = 0;
            height = textArea.getHeight();
            width = 2;
            repaint(); // calls getComponent().repaint(x, y, width, height)
        }

        @Override
        public void paint(Graphics g) {

            JTextComponent component = getComponent();
            int dot = getDot();
            Rectangle verticalLine = null;
            try {
                verticalLine = component.modelToView(dot);
            } catch (BadLocationException e) {
                return;
            }
            if (isVisible()) {
                // The long vertical line will be light gray
                g.setColor(Color.LIGHT_GRAY);
                if (!textAreaIsSplit) {
                    g.fillRect(verticalLine.x, 0, width, textArea.getHeight());
                    // The short line segment of the caret in the y position will be red
                    g.setColor(Color.RED);
                    g.fillRect(verticalLine.x, verticalLine.y, width, fontSize);
                } else {
                    g.fillRect(verticalLine.x, 0, width, textArea2.getHeight());
                    // The short line segment of the caret in the y position will be red
                    g.setColor(Color.RED);
                    g.fillRect(verticalLine.x, verticalLine.y, width, fontSize);
                }
            }
        }
    }

    /**
     * Implements custom caret as a long vertical line with a short red line pointer
     * for secondary text area.
     */
    public class LongCaret2 extends DefaultCaret {

        @Override
        public void damage(Rectangle verticalLine) {
            // give values to x, y, width,height (inherited from java.awt.Rectangle)
            x = verticalLine.x;
            y = 0;
            height = textArea2.getHeight();
            width = 2;
            repaint(); // calls getComponent().repaint(x, y, width, height)
        }

        @Override
        public void paint(Graphics g) {

            JTextComponent component = getComponent();
            int dot = getDot();
            Rectangle verticalLine = null;
            try {
                verticalLine = component.modelToView(dot);
            } catch (BadLocationException e) {
                return;
            }
            if (isVisible()) {
                // The long vertical line will be light gray
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(verticalLine.x, 0, width, textArea2.getHeight());
                // The short line segment of the caret in the y position will be red
                g.setColor(Color.RED);
                g.fillRect(verticalLine.x, verticalLine.y, width, fontSize);
            }
        }
    }

    /**
     * Implements vertical (rectangular) selection of text in primary area.
     */
    public class SpecialCaret extends DefaultCaret {

        Point lastPoint = new Point(0, 0);

        @Override
        public void mouseMoved(MouseEvent mouseEvent) {
            lastPoint = new Point(mouseEvent.getX(), mouseEvent.getY());
            super.mouseMoved(mouseEvent);
        }

        @Override
        public void mouseClicked(MouseEvent mouseEvent) {
            if (selectionMode.equals(VERTICAL_SELECTION)) {
                super.mouseClicked(mouseEvent);
                selectionStarts.clear();
                selectionEnds.clear();
                if (mouseEvent.getClickCount() == 2 || mouseEvent.getClickCount() == 3) {
                    selectionStarts.add(textArea.getSelectionStart());
                    selectionEnds.add(textArea.getSelectionEnd());
                }
            } else {
                //super.mouseClicked(mouseEvent);
                super.mouseClicked(mouseEvent);
                selectionStarts.clear();
                selectionEnds.clear();
                if (mouseEvent.getClickCount() == 2) {
                    selectionStarts.add(textArea.getSelectionStart());
                    selectionEnds.add(textArea.getSelectionEnd());
                }
                if (mouseEvent.getClickCount() == 3) {
                    selectionStarts.add(textArea.getSelectionStart());
                    selectionEnds.add(textArea.getSelectionEnd());
                    textArea.setCaretPosition(0);
                }

            }
        }

        @Override
        protected void moveCaret(MouseEvent mouseEvent) {
            Point pt = new Point(mouseEvent.getX(), mouseEvent.getY());
            int pos = getComponent().getUI().viewToModel(getComponent(), pt);
            if (pos >= 0) {
                setDot(pos);
                Point start = new Point(Math.min(lastPoint.x, pt.x), Math.min(lastPoint.y, pt.y));
                Point end = new Point(Math.max(lastPoint.x, pt.x), Math.max(lastPoint.y, pt.y));
                customHighlight(start, end);
                textArea.setCaretPosition(selectionStarts.get(0));
            }
        }

        /**
         *
         * @param start
         * @param end
         */
        protected void customHighlight(Point start, Point end) {
            selectionStarts.clear();
            selectionEnds.clear();

//            getComponent().getHighlighter().removeAllHighlights();
            int y = start.y;
            int firstX = start.x;
            int lastX = end.x;

            int pos1 = getComponent().getUI().viewToModel(getComponent(), new Point(firstX, y));
            int pos2 = getComponent().getUI().viewToModel(getComponent(), new Point(lastX, y));
            try {
                selectionStarts.add(pos1);
                selectionEnds.add(pos2);
                getComponent().getHighlighter().addHighlight(pos1, pos2, DefaultHighlighter.DefaultPainter);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            y++;
            while (y < end.y) {
                int pos1new = getComponent().getUI().viewToModel(getComponent(), new Point(firstX, y));
                int pos2new = getComponent().getUI().viewToModel(getComponent(), new Point(lastX, y));
                if (pos1 != pos1new) {
                    pos1 = pos1new;
                    pos2 = pos2new;
                    try {
                        selectionStarts.add(pos1);
                        selectionEnds.add(pos2);
                        getComponent().getHighlighter().addHighlight(pos1, pos2, DefaultHighlighter.DefaultPainter);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                y++;
            }
        }

        /**
         *
         * @param verticalLine
         */
        @Override
        public void damage(Rectangle verticalLine) {
            if (caretShape.equals(LONG_CARET)) {
                // Long caret
                // ----------
                // give values to x, y, width,height (inherited from java.awt.Rectangle)
                x = verticalLine.x;
                y = 0; // upper edge of the vertical line is at the upper edge of the text area
                height = textArea.getHeight();
                width = 2;
                repaint(); // calls getComponent().repaint(x, y, width, height)
            } else {
                // Short caret
                // -----------
                x = verticalLine.x;
                y = verticalLine.y;
                height = fontSize + 2;
                width = 1;
                repaint();
            }
        }

        /**
         *
         * @param g
         */
        @Override
        public void paint(Graphics g) {
            JTextComponent component = getComponent();
            int dot = getDot();
            Rectangle verticalLine = null;
            try {
                verticalLine = component.modelToView(dot);
            } catch (BadLocationException ble) {
                return;
            }
            if (caretShape.equals(LONG_CARET)) {
                // Long caret
                // ----------
                // The long vertical line will be light gray
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(verticalLine.x, 0, width, height);
                // The short line segment of the caret in the y position will be red
                g.setColor(Color.RED);
                g.fillRect(verticalLine.x, verticalLine.y, 2, fontSize);
            } else {
                // Short caret
                // -----------
                g.setColor(Color.BLACK);
                g.fillRect(verticalLine.x, verticalLine.y, 1, fontSize + 2);
            }
        }
    }

    /**
     * Implements vertical (rectangular) selection of text for secondary text area.
     */
    public class SpecialCaret2 extends DefaultCaret {

        Point lastPoint = new Point(0, 0);

        @Override
        public void mouseMoved(MouseEvent mouseEvent) {
            lastPoint = new Point(mouseEvent.getX(), mouseEvent.getY());
            super.mouseMoved(mouseEvent);
        }

        @Override
        public void mouseClicked(MouseEvent mouseEvent) {
            if (selectionMode.equals(VERTICAL_SELECTION)) {
                super.mouseClicked(mouseEvent);
                selectionStarts.clear();
                selectionEnds.clear();
                selectionStarts.add(textArea2.getSelectionStart());
                selectionEnds.add(textArea2.getSelectionEnd());
            } else {
                super.mouseClicked(mouseEvent);

            }
        }

        @Override
        protected void moveCaret(MouseEvent mouseEvent) {
            Point pt = new Point(mouseEvent.getX(), mouseEvent.getY());
            int pos = getComponent().getUI().viewToModel(getComponent(), pt);
            if (pos >= 0) {
                setDot(pos);
                Point start = new Point(Math.min(lastPoint.x, pt.x), Math.min(lastPoint.y, pt.y));
                Point end = new Point(Math.max(lastPoint.x, pt.x), Math.max(lastPoint.y, pt.y));
                customHighlight(start, end);
            }
        }

        /**
         *
         * @param start
         * @param end
         */
        protected void customHighlight(Point start, Point end) {
            selectionStarts.clear();
            selectionEnds.clear();

//            getComponent().getHighlighter().removeAllHighlights();
            int y = start.y;
            int firstX = start.x;
            int lastX = end.x;

            int pos1 = getComponent().getUI().viewToModel(getComponent(), new Point(firstX, y));
            int pos2 = getComponent().getUI().viewToModel(getComponent(), new Point(lastX, y));
            textArea2.select(pos1, pos2);
            try {
                selectionStarts.add(pos1);
                selectionEnds.add(pos2);
                getComponent().getHighlighter().addHighlight(pos1, pos2, DefaultHighlighter.DefaultPainter);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            y++;
            while (y < end.y) {
                int pos1new = getComponent().getUI().viewToModel(getComponent(), new Point(firstX, y));
                int pos2new = getComponent().getUI().viewToModel(getComponent(), new Point(lastX, y));
                if (pos1 != pos1new) {
                    pos1 = pos1new;
                    pos2 = pos2new;
                    try {
                        selectionStarts.add(pos1);
                        selectionEnds.add(pos2);
                        getComponent().getHighlighter().addHighlight(pos1, pos2, DefaultHighlighter.DefaultPainter);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                y++;
            }
        }

        /**
         *
         * @param verticalLine
         */
        @Override
        public void damage(Rectangle verticalLine) {
            if (caretShape.equals(LONG_CARET)) {
                // Long caret
                // ----------
                // give values to x, y, width,height (inherited from java.awt.Rectangle)
                x = verticalLine.x;
                y = 0; // upper edge of the vertical line is at the upper edge of the text area
                height = textArea2.getHeight();
                width = 2;
                repaint(); // calls getComponent().repaint(x, y, width, height)
            } else {
                // Short caret
                // -----------
                x = verticalLine.x;
                y = verticalLine.y;
                height = fontSize + 2;
                width = 1;
                repaint();
            }
        }

        /**
         *
         * @param g
         */
        @Override
        public void paint(Graphics g) {
            JTextComponent component = getComponent();
            int dot = getDot();
            Rectangle verticalLine = null;
            try {
                verticalLine = component.modelToView(dot);
            } catch (BadLocationException ble) {
                return;
            }
            if (caretShape.equals(LONG_CARET)) {
                // Long caret
                // ----------
                // The long vertical line will be light gray
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(verticalLine.x, 0, width, height);
                // The short line segment of the caret in the y position will be red
                g.setColor(Color.RED);
                g.fillRect(verticalLine.x, verticalLine.y, 2, fontSize);
            } else {
                // Short caret
                // -----------
                g.setColor(Color.BLACK);
                g.fillRect(verticalLine.x, verticalLine.y, 1, fontSize + 2);
            }
        }
    }

    /**
     * Rendering elements in combo box "Font selection".
     */
    public class FontComboBoxRenderer extends JLabel implements ListCellRenderer {

        /**
         *
         * @param list
         * @param value
         * @param index
         * @param isSelected
         * @param cellHasFocus
         * @return
         */
        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            String fontName = value.toString();
            if (operatingSystem.equals("MAC")) {
                for (String str : fontNamesMac) {
                    if (str.equals(fontName)) {
                        setFont(new Font(fontName, Font.PLAIN, fontSize));
                        setText(fontName);
                    }
                }
            }
            if (operatingSystem.equals("WINDOWS")) {
                for (String str : fontNamesWin) {
                    if (str.equals(fontName)) {
                        setFont(new Font(fontName, Font.PLAIN, fontSize));
                        setText(fontName);
                    }
                }
            }
            return this;
        }
    }

    /**
     * Check if text was changed; if so, color Save button text dark red,
     * if not, color Save button text with original button color.
     */
    protected void checkTextChanged() {
        if (textChanged) {
            saveButton.setText("Save!");
            saveButton.setForeground(DARK_RED);
            saveButton.setToolTipText("Save text before compiling.");
        } else {
            saveButton.setForeground(originalButtonForeground);
            saveButton.setText("Save");
        }
//        saveButton.setSelected(true);
    }

    /**
     * Initial document listener for primary text area.
     */
    class TextAreaInitDocListener implements DocumentListener {

        @Override
        public void changedUpdate(DocumentEvent de) {
        }

        @Override
        public void insertUpdate(DocumentEvent de) {
            // Save button will have notification color and an exclamation mark.
            textChanged = true;
            checkTextChanged();
        }

        @Override
        public void removeUpdate(DocumentEvent de) {
            // Save button will have notification color and an exclamation mark.
            textChanged = true;
            checkTextChanged();
        }
    }

    /**
     * Document listener for primary text area.
     */
    class TextAreaDocListener implements DocumentListener {

        @Override
        public void changedUpdate(DocumentEvent de) {
        }

        @Override
        public void insertUpdate(DocumentEvent de) {
            // Save button will have notification color and an exclamation mark.
            textChanged = true;
            checkTextChanged();

            textArea2.getDocument().removeDocumentListener(textArea2DocListener);
            int offset = de.getOffset();
            int length = de.getLength();
            String str = textArea.getText().substring(offset, offset + length);
            textArea2.insert(str, offset);
            changeHighlight2();
            //System.out.println("ins: " + offset + ", " + length + " inserted chars: '" + textArea2.getText().substring(offset, offset + length) + "'");
            textArea2.getDocument().addDocumentListener(textArea2DocListener);
        }

        @Override
        public void removeUpdate(DocumentEvent de) {
            // Save button will have notification color and an exclamation mark.
            textChanged = true;
            checkTextChanged();

            textArea2.getDocument().removeDocumentListener(textArea2DocListener);
            int offset = de.getOffset();
            int length = de.getLength();
            //System.out.println("rmv: " + offset + ", " + length + " removed chars : '" + textArea2.getText().substring(offset, offset + length) + "'");
            textArea2.replaceRange("", offset, offset + length);
            changeHighlight2();
            textArea2.getDocument().addDocumentListener(textArea2DocListener);
        }
    }

    /**
     * Document listener for secondary text area.
     */
    class TextArea2DocListener implements DocumentListener {

        @Override
        public void changedUpdate(DocumentEvent de) {
        }

        @Override
        public void insertUpdate(DocumentEvent de) {
            // Save button will have notification color and an exclamation mark.
            textChanged = true;
            checkTextChanged();

            textArea.getDocument().removeDocumentListener(textAreaDocListener);
            int offset = de.getOffset();
            int length = de.getLength();
            String str = textArea2.getText().substring(offset, offset + length);
            textArea.insert(str, offset);
            changeHighlight();
            //System.out.println("ins2: " + offset + ", " + length + " inserted chars2: '" + textArea.getText().substring(offset, offset + length) + "'");
            textArea.getDocument().addDocumentListener(textAreaDocListener);
        }

        @Override
        public void removeUpdate(DocumentEvent de) {
            // Save button will have notification color and an exclamation mark.
            textChanged = true;
            checkTextChanged();

            textArea.getDocument().removeDocumentListener(textAreaDocListener);
            int offset = de.getOffset();
            int length = de.getLength();
            //System.out.println("rmv2: " + offset + ", " + length + " removed chars2 : '" + textArea.getText().substring(offset, offset + length) + "'");
            textArea.replaceRange("", offset, offset + length);
            changeHighlight();
            textArea.getDocument().addDocumentListener(textAreaDocListener);
        }
    }

    /**
     * Mouse listener for primary text area.
     */
    class TextAreaMouseListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent mouseEvent) {

            lowerHalfActive = false;
            Point pt = new Point(mouseEvent.getX(), mouseEvent.getY());
            curPos = textArea.getUI().viewToModel(textArea, pt);
            // Every click sets current highlight depending on the direction
            changeHighlight();

            // Highlight blocks if no pattern is in the findField
            blockHighlighter = textArea.getHighlighter();
            // Hightlight only if the option is not *NONE
            if (!progLanguage.equals("*NONE")) {
                highlightBlocks(textArea);
            }
            /*
            // On right click show popup menu with commands.
            if ((mouseEvent.getButton() == MouseEvent.BUTTON3)) {
                preparePopupMenu();
                // Show the menu
                textAreaPopupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
            }
             */
        }
    }

    /**
     * Mouse listener for secondary text area.
     */
    class TextArea2MouseListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent mouseEvent) {

            lowerHalfActive = true;
            Point pt = new Point(mouseEvent.getX(), mouseEvent.getY());
            curPos2 = textArea2.getUI().viewToModel(textArea2, pt);
            // Every click sets current highlight depending on the direction
            changeHighlight2();

            // Highlight blocks if no pattern is in the findField
            blockHighlighter = textArea2.getHighlighter();
            // Hightlight only if the option is not *NONE
            if (!progLanguage.equals("*NONE")) {
                highlightBlocks(textArea2);
            }
            /*
            // On right click change selection mode.
            if ((mouseEvent.getButton() == MouseEvent.BUTTON3)) {
                preparePopupMenu();
                // Show the menu
                textAreaPopupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
            }
             */
        }
    }

    /**
     * Prepare popup menu for mouse listeners.
     */
    protected void preparePopupMenu() {
        // Command "Change selection"
        String mode, shape;
        if (selectionMode.equals(HORIZONTAL_SELECTION)) {
            mode = "Vertical";
        } else {
            mode = "Horizontal";
        }
        changeSelMode.setText("Change selection to " + mode + ".");
        textAreaPopupMenu.add(changeSelMode);

        // Command "Change caret"
        if (caretShape.equals(SHORT_CARET)) {
            shape = "Long";
        } else {
            shape = "Short";
        }
        toggleCaret.setText("Change caret to " + shape + ".");
        textAreaPopupMenu.add(toggleCaret);
    }

    /**
     * Window adapter closes the FindWindow and also this window.
     */
    class WindowEditFileAdapter extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent we) {
            if (findWindow != null) {
                findWindow.dispose();
            }
        }
    }
}
