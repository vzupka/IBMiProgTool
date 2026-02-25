package copyfiles;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400File;
import com.ibm.as400.access.AS400FileRecordDescription;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.FileAttributes;

import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.IFSFileInputStream;
import com.ibm.as400.access.Record;
import com.ibm.as400.access.RecordFormat;
import com.ibm.as400.access.SequentialFile;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.LayerUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position.Bias;

/**
 * Display file - PC file, IFS file, Source Member, Spooled file.
 *
 * @author Vladimír Župka, 2016
 */
public final class DisplayFile extends JFrame {

    public JTextArea textArea = new JTextArea();

    final Color VERY_LIGHT_BLUE = Color.getHSBColor(0.60f, 0.020f, 0.99f);
    final Color VERY_LIGHT_GREEN = Color.getHSBColor(0.52f, 0.020f, 0.99f);
    final Color VERY_LIGHT_PINK = Color.getHSBColor(0.025f, 0.008f, 0.99f);

    final Color WARNING_COLOR = new Color(255, 200, 200);
    final Color VERY_LIGHT_GRAY = Color.getHSBColor(0.50f, 0.01f, 0.90f);

    Highlighter.HighlightPainter currentPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.ORANGE);
    Highlighter.HighlightPainter highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);

    JLabel characterSetLabel = new JLabel();

    String[] fontNamesMac = {
        "Monospaced",
        "Courier New",
        "Monaco",
        "Andale Mono",
        "Ayuthaya",
        "Menlo",
        "PT Mono",};
    String[] fontNamesWin = {
        "Monospaced",
        "Consolas",
        "Consolas Italic",
        "Consolas Bold",
        "Consolas Bold Italic",
        "Courier New",
        "DialogInput",
        "Lucida Console",
        "MS Gothic",
        "Cascadia Code",
        "DejaVu Sans Mono",
        "Liberation Mono",};

    JLabel fontSizeLabel = new JLabel("Font size:");
    JTextField fontSizeField = new JTextField();

    JLabel searchLabel = new JLabel("Find:");
    JTextField findField = new JTextField();
    JLayer fieldLayer;

    JToggleButton matchCaseButton = new JToggleButton();
    ImageIcon matchCaseIconDark;
    ImageIcon matchCaseIconDim;

    PlaceholderLayerUI layerUI = new PlaceholderLayerUI();
    HighlightListener highlightListener = new HighlightListener();

    TextAreaMouseListener textAreaMouseListener;

    // Map containing intervals (start, end) of highligthted texts.
    TreeMap<Integer, Integer> highlightMap = new TreeMap<>();
    // Position set by mouse press or by program in FindWindow class (find or replace listeners).
    // The position is searched in the highlightMap to find the startOffset of a highlight.
    Integer curPos = 0;

    // Lists of starts and ends of highlighted texts taken from the highlightMap.
    ArrayList<Integer> startOffsets = new ArrayList<>();
    ArrayList<Integer> endOffsets = new ArrayList<>();

    int sequence = 0; // sequence number of current highlighted interval in the primary text area
    Integer startOffset; // start offset of highlighted interval
    Integer endOffset; // end offset of highlighted interval
    String direction = "forward";

    MainWindow mainWindow;

    static int windowWidth;
    static int windowHeight;
    int screenWidth;
    int screenHeight;
    int windowX;
    int windowY;

    Path matchCaseIconPathDark = Paths.get(System.getProperty("user.dir"), "icons", "matchCase1.png");
    Path matchCaseIconPathDim = Paths.get(System.getProperty("user.dir"), "icons", "matchCase2.png");

    String matchCase;

    Path prevInactiveIconPath = Paths.get(System.getProperty("user.dir"), "icons", "prevInactive.png");
    Path nextInactiveIconPath = Paths.get(System.getProperty("user.dir"), "icons", "nextInactive.png");
    Path prevActiveIconPath = Paths.get(System.getProperty("user.dir"), "icons", "prevActive.png");
    Path nextActiveIconPath = Paths.get(System.getProperty("user.dir"), "icons", "nextActive.png");

    ImageIcon prevInactiveIcon = new ImageIcon(prevInactiveIconPath.toString());
    ImageIcon nextInactiveIcon = new ImageIcon(nextInactiveIconPath.toString());
    ImageIcon prevActiveIcon = new ImageIcon(prevActiveIconPath.toString());
    ImageIcon nextActiveIcon = new ImageIcon(nextActiveIconPath.toString());

    JButton prevButton = new JButton(prevInactiveIcon);
    JButton nextButton = new JButton(nextActiveIcon);

    Path parPath = Paths.get(System.getProperty("user.dir"), "paramfiles", "Parameters.txt");
    String encoding = System.getProperty("file.encoding", "UTF-8");
    final String PROP_COMMENT = "Copy files between IBM i and PC, edit and compile.";
    Properties properties;
    String pcCharset;
    String ibmCcsid;
    int ibmCcsidInt;
    int sourceCcsidInt;

    String operatingSystem;

    JComboBox<String> fontComboBox = new JComboBox<>();

    String displayFont;
    String fontSizeString;
    int fontSize;

    JPanel globalPanel;

    String row;
    boolean nodes = true;
    boolean noNodes = false;

    GroupLayout globalPanelLayout;
    JScrollPane scrollPane;

    final String NEW_LINE = "\n";

    /**
     * Constructor
     *
     * @param textArea
     * @param mainWindow
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public DisplayFile(JTextArea textArea, MainWindow mainWindow) {
        this.textArea = textArea;
        this.mainWindow = mainWindow;

        properties = new Properties();
        try {
            BufferedReader infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
            properties.load(infile);
            infile.close();
        } catch (Exception exc) {
            exc.printStackTrace();
        }

        pcCharset = properties.getProperty("PC_CHARSET");
        if (!pcCharset.equals("*DEFAULT")) {
            // Check if charset is valid
            try {
                Charset.forName(pcCharset);
            } catch (IllegalCharsetNameException | UnsupportedCharsetException charset) {
                // If pcCharset is invalid, take ISO-8859-1
                pcCharset = "ISO-8859-1";
            }
        }

        ibmCcsid = properties.getProperty("IBM_CCSID");
        if (!ibmCcsid.equals("*DEFAULT")) {
            try {
                ibmCcsidInt = Integer.parseInt(ibmCcsid);
            } catch (Exception exc) {
                exc.printStackTrace();
                ibmCcsid = "819";
                ibmCcsidInt = 819;
            }
        }

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

        displayFont = properties.getProperty("DISPLAY_FONT");
        matchCase = properties.getProperty("MATCH_CASE");

        fontComboBox.setPreferredSize(new Dimension(140, 20));
        fontComboBox.setMaximumSize(new Dimension(140, 20));
        fontComboBox.setMinimumSize(new Dimension(140, 20));
        fontComboBox.setToolTipText("Choose font.");

        // Sets the current display font item into the input field of the combo box
        fontComboBox.setSelectedItem(displayFont);

        // This class gives the corresponding fonts to the font names in the combo box list
        fontComboBox.setRenderer(new FontComboBoxRenderer());

        fontSizeString = properties.getProperty("DISPLAY_FONT_SIZE");
        try {
            fontSize = Integer.parseInt(fontSizeString);
        } catch (NumberFormatException exc) {
            exc.printStackTrace();
            fontSizeString = "12";
            fontSize = 12;
        }

        fontSizeField.setText(fontSizeString);
        fontSizeField.setToolTipText("Enter font size.");
        fontSizeField.setPreferredSize(new Dimension(30, 20));
        fontSizeField.setMaximumSize(new Dimension(30, 20));

        // Adjust the text area
        textArea.setEditable(false);
        textArea.setFont(new Font(displayFont, Font.PLAIN, fontSize));

        // Create scroll pane with the text area inside
        scrollPane = new JScrollPane(textArea);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Light sky blue
        scrollPane.setBackground(VERY_LIGHT_BLUE);
        textArea.setBackground(VERY_LIGHT_BLUE);

        Toolkit kit = Toolkit.getDefaultToolkit();
        Dimension screenSize = kit.getScreenSize();
        screenWidth = screenSize.width;
        screenHeight = screenSize.height;
        windowWidth = 1020;
        windowHeight = screenHeight - 120;

        windowX = screenWidth / 2 - windowWidth / 2;
        windowY = 0;

        prevButton.setToolTipText("Previous match. Also Ctrl+⬆ (Cmd+⬆ in macOS).");
        prevButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        prevButton.setContentAreaFilled(false);
        prevButton.setPreferredSize(new Dimension(25, 20));

        nextButton.setToolTipText("Next match. Also Ctrl+⬇ (Cmd+⬇ in macOS).");
        nextButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        nextButton.setContentAreaFilled(false);
        nextButton.setPreferredSize(new Dimension(25, 20));

        // Icon Aa will be dimmed or dark when clicked
        matchCaseIconDark = new ImageIcon(matchCaseIconPathDark.toString());
        matchCaseIconDim = new ImageIcon(matchCaseIconPathDim.toString());

        if (matchCase.equals("CASE_INSENSITIVE")) {
            matchCaseButton.setIcon(matchCaseIconDim);
            matchCaseButton.setSelectedIcon(matchCaseIconDim);
            matchCaseButton.setToolTipText("Case insensitive.");
        } else {
            matchCaseButton.setIcon(matchCaseIconDark);
            matchCaseButton.setSelectedIcon(matchCaseIconDark);
            matchCaseButton.setToolTipText("Match case.");
        }

        matchCaseButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        matchCaseButton.setContentAreaFilled(false);
        matchCaseButton.setPreferredSize(new Dimension(20, 20));
        matchCaseButton.setMinimumSize(new Dimension(20, 20));
        matchCaseButton.setMaximumSize(new Dimension(20, 20));
        matchCaseButton.setActionCommand("matchCase");

        findField.setPreferredSize(new Dimension(300, 20));
        findField.setMaximumSize(new Dimension(300, 20));
        findField.setToolTipText("Enter text to find.");

        // Set document listener for the search field
        findField.getDocument().addDocumentListener(highlightListener);

        textAreaMouseListener = new TextAreaMouseListener();
        textArea.addMouseListener(textAreaMouseListener);

        // Set a layer of counts that overlay the search field:
        // - the sequence number of just highlighted text found
        // - how many matches were found
        fieldLayer = new JLayer<>(findField, layerUI);

        globalPanel = new JPanel();
        globalPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Lay out components in globalPanel
        globalPanelLayout = new GroupLayout(globalPanel);
        globalPanelLayout.setAutoCreateGaps(false);
        globalPanelLayout.setAutoCreateContainerGaps(false);
        GroupLayout.SequentialGroup sg = globalPanelLayout.createSequentialGroup()
                .addComponent(searchLabel)
                .addComponent(fieldLayer)
                .addComponent(prevButton)
                .addGap(3)
                .addComponent(nextButton)
                .addGap(10)
                .addComponent(matchCaseButton)
                .addGap(10)
                .addComponent(fontComboBox)
                // .addComponent(fontSizeLabel)
                .addComponent(fontSizeField)
                .addGap(10)
                .addComponent(characterSetLabel);
        GroupLayout.ParallelGroup pg = globalPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(searchLabel)
                .addComponent(fieldLayer)
                .addComponent(prevButton)
                .addComponent(nextButton)
                .addComponent(matchCaseButton)
                .addComponent(fontComboBox)
                // .addComponent(fontSizeLabel)
                .addComponent(fontSizeField)
                .addComponent(characterSetLabel);
        globalPanelLayout.setHorizontalGroup(globalPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(sg)
                .addGroup(globalPanelLayout.createSequentialGroup()
                        .addComponent(scrollPane)));
        globalPanelLayout.setVerticalGroup(globalPanelLayout.createSequentialGroup()
                .addGroup(pg)
                .addGroup(globalPanelLayout.createParallelGroup()
                        .addComponent(scrollPane)));
        globalPanel.setLayout(globalPanelLayout);
        globalPanel.setBackground(VERY_LIGHT_GRAY);

        scrollPane.setPreferredSize(new Dimension(windowWidth, windowHeight));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // "Previous" button listener
        // --------------------------
        prevButton.addActionListener(ae -> {
            direction = "backward";
            prevButton.setIcon(prevActiveIcon);
            nextButton.setIcon(nextInactiveIcon);
            if (highlightMap.lowerKey(curPos) == null) {
                curPos = highlightMap.lastKey();
            } else {
                curPos = highlightMap.lowerKey(curPos);
            }
            changeHighlight();
        });

        // "Next" button listener
        // ----------------------
        nextButton.addActionListener(ae -> {
            direction = "forward";
            prevButton.setIcon(prevInactiveIcon);
            nextButton.setIcon(nextActiveIcon);
            if (highlightMap.higherKey(curPos) == null) {
                curPos = highlightMap.firstKey();
            } else {
                curPos = highlightMap.higherKey(curPos);
            }
            changeHighlight();
        });

        // "Match case" button listener
        // ----------------------------
        matchCaseButton.addActionListener(ae -> {
            if (matchCaseButton.getSelectedIcon().equals(matchCaseIconDark)) {
                matchCaseButton.setIcon(matchCaseIconDim);
                matchCaseButton.setSelectedIcon(matchCaseIconDim);
                matchCaseButton.setToolTipText("Case insensitive. Toggle Match case.");
                matchCase = "CASE_INSENSITIVE";
            } else {
                matchCaseButton.setIcon(matchCaseIconDark);
                matchCaseButton.setSelectedIcon(matchCaseIconDark);
                matchCaseButton.setToolTipText("Match case. Toggle Case insensitive.");
                matchCase = "CASE_SENSITIVE";
            }
            changeHighlight();
            try {
                BufferedWriter outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                // Save programming language into properties
                properties.setProperty("MATCH_CASE", matchCase);
                properties.store(outfile, PROP_COMMENT);
                outfile.close();
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        });

        // Select display font from the list in combo box - listener
        // ---------------------------------------------------------
        fontComboBox.addItemListener(il -> {
            int currentCaretPos = textArea.getCaretPosition();
            JComboBox<String> source = (JComboBox) il.getSource();
            fontSizeString = fontSizeField.getText();
            try {
                fontSize = Integer.parseInt(fontSizeString);
            } catch (NumberFormatException exc) {
                exc.printStackTrace();
                fontSizeString = "12";
                fontSize = 12;
            }
            displayFont = (String) fontComboBox.getSelectedItem();
            textArea.setFont(new Font(displayFont, Font.PLAIN, fontSize));
            try {
                BufferedWriter outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                // Save programming language into properties
                properties.setProperty("DISPLAY_FONT", displayFont);
                properties.setProperty("DISPLAY_FONT_SIZE", fontSizeString);
                properties.store(outfile, PROP_COMMENT);
                outfile.close();
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        });

        // "Font size" field listener
        fontSizeField.addActionListener(al -> {
            fontSizeString = fontSizeField.getText();
            try {
                fontSize = Integer.parseInt(fontSizeString);
            } catch (NumberFormatException exc) {
                exc.printStackTrace();
                fontSizeString = "12";
                fontSize = 12;
            }
            fontSizeField.setText(fontSizeString);
            textArea.setFont(new Font(displayFont, Font.PLAIN, fontSize));
            //textArea.repaint();
            try {
                BufferedWriter outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                // Save font size into properties
                properties.setProperty("DISPLAY_FONT_SIZE", fontSizeString);
                properties.store(outfile, PROP_COMMENT);
                outfile.close();
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        });

        // Set input maps and actions for Ctrl + Arrow UP and Ctrl + Arrow DOWN on different buttons and text areas.
        ArrowUp arrowUp = new ArrowUp();
        ArrowDown arrowDown = new ArrowDown();
        Arrays.asList(prevButton, nextButton, findField, textArea).stream().map((object) -> {
            return object;
        }).forEachOrdered((object) -> {
            // Enable processing of function key Ctrl + Arrow UP = Find next hit upwards
            object.getInputMap(JComponent.WHEN_FOCUSED)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "arrowUp");
            object.getActionMap().put("arrowUp", arrowUp);

            // Enable processing of function key Ctrl + Arrow DOWN = Find next hit downwards
            object.getInputMap(JComponent.WHEN_FOCUSED)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "arrowDown");
            object.getActionMap().put("arrowDown", arrowDown);
        });

        // Enable ESCAPE key to escape from display
        // ----------------------------------------
        globalPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "escape");
        globalPanel.getActionMap().put("escape", new Escape());

        Container cont = getContentPane();
        cont.add(globalPanel);

        // Display the window.
        setSize(windowWidth, windowHeight);
        setLocation(windowX, windowY);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pack();

        findField.requestFocusInWindow();
    }

    /**
     * Display contents of the IFS file using its CCSID attribute
     *
     * @param remoteServer
     * @param ifsFilePathString
     */
    protected void displayIfsFile(AS400 remoteServer, String ifsFilePathString) {

        this.setTitle("Display IFS file  '" + ifsFilePathString + "'");

        // Contents of the file are always decoded according to its attributed CCSID.
        // Characters may be displayed incorrectly if the "IBMi CCSID" parameter
        // does not correspond to the file's attributed CCSID.
        // The user can correct the parameter "IBMi CCSID" and try again.
        try {
            IFSFile ifsFile = new IFSFile(remoteServer, ifsFilePathString);
            int attributeCCSID = ifsFile.getCCSID();

            characterSetLabel.setText("CCSID " + attributeCCSID + " was used for display.");

            byte[] inputBuffer = new byte[100000];
            byte[] workBuffer = new byte[100000];

            try (IFSFileInputStream inputStream = new IFSFileInputStream(remoteServer, ifsFilePathString)) {
                int bytesRead = inputStream.read(inputBuffer);
                while (bytesRead != -1) {
                    for (int idx = 0; idx < bytesRead; idx++) {
                        // Copy input byte to output byte
                        workBuffer[idx] = inputBuffer[idx];
                    }
                    // Copy the printable part of the work array to a new buffer that will be written out.
                    byte[] bufferToWrite = new byte[bytesRead];
                    // Copy bytes from the work buffer to the new buffer
                    for (int indx = 0; indx < bytesRead; indx++) {
                        bufferToWrite[indx] = workBuffer[indx];
                    }
                    // Create object for conversion from bytes to characters
                    AS400Text textConverter = new AS400Text(bytesRead, attributeCCSID, remoteServer);
                    // Convert byte array buffer to text line
                    String textLine = (String) textConverter.toObject(bufferToWrite);
                    // Append the line to text area
                    textArea.append(textLine + NEW_LINE);

                    // Read next input buffer
                    bytesRead = inputStream.read(inputBuffer);
                }
                // Set scroll bar to top
                textArea.setCaretPosition(0);
                // Display the window.
                setVisible(true);
                row = "Info: IFS file  " + ifsFilePathString + "  has CCSID  " + attributeCCSID + ".";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages(nodes);
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error: " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes);
        }
        // Remove message scroll listener (cancel scrolling to the last message)
        mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
    }

    /**
     * Display text area - for spooled files
     *
     * @param textAreaString
     * @param ibmCcsid
     */
    protected void displayTextArea(String textAreaString, String ibmCcsid) {

        characterSetLabel.setText("CCSID " + ibmCcsid + " was used for display.");

        // Copy text area from parameter to instance text area
        textArea.setText(textAreaString);

        scrollPane.setBackground(VERY_LIGHT_GREEN);
        textArea.setBackground(VERY_LIGHT_GREEN);

        // Set scroll bar to bottom!!
        // --------------------------
        textArea.setCaretPosition(textAreaString.length());

        setLocation(windowX + 100, windowY);
        // Display the window.
        setVisible(true);
    }

    /**
     * Display PC file using the application parameter "pcCharset".
     *
     * @param pcPathString
     */
    protected void displayPcFile(String pcPathString) {

        this.setTitle("Display PC file  '" + pcPathString + "'");

        try {
            Path filePath = Paths.get(pcPathString);
            if (Files.exists(filePath)) {
                if (pcCharset.equals("*DEFAULT")) {
                    // Set ISO-8859-1 as a default
                    pcCharset = "ISO-8859-1";
                }
                characterSetLabel.setText(pcCharset + " character set was used for display.");
                // Use PC charset parameter for conversion
                List<String> list = Files.readAllLines(filePath, Charset.forName(pcCharset));
                if (list != null) {
                    Object[] obj = (Object[]) list.stream().toArray();
                    for (int idx = 0; idx < obj.length; idx++) {
                        String text = obj[idx].toString();
                        textArea.append(text + NEW_LINE);
                    }
                }
            }

            scrollPane.setBackground(VERY_LIGHT_PINK);
            textArea.setBackground(VERY_LIGHT_PINK);

            // Set scroll bar to top
            textArea.setCaretPosition(0);

            setLocation(windowX - 100, windowY);

            // Display the window.
            setVisible(true);
            row = "Info: PC file  " + pcPathString + "  is displayed using character set  " + pcCharset
                    + "  from the application parameter.";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes);
            // Remove message scroll listener (cancel scrolling to the last message)
            mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error: Displaying PC file:  " + pcPathString
                    + "  is not a text file or has an unsuitable character set.  -  "
                    + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes); // do not add child nodes
            // Remove message scroll listener (cancel scrolling to the last message)
            //mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
        }
    }

    /**
     * Display source member using its CCSID attribute; Only data part of the source record is translated (to String -
     * UTF-16).
     *
     * @param remoteServer
     * @param as400PathString
     */
    protected void displaySourceMember(AS400 remoteServer, String as400PathString) {

        this.setTitle("Display member  '" + as400PathString + "'");

        // Set provisional source type to .MBR to convene IFS path convention
        String pathString = as400PathString;
        pathString = pathString.substring(0, pathString.lastIndexOf(".")) + ".MBR";

        // Create an AS400FileRecordDescription object that represents the file
        AS400FileRecordDescription inRecDesc = new AS400FileRecordDescription(remoteServer, pathString);
        FileAttributes fileAttributes = new FileAttributes(remoteServer, pathString);

        try {
            sourceCcsidInt = fileAttributes.getCcsid();
            characterSetLabel.setText("CCSID " + sourceCcsidInt + " was used for display.");

            // Get list of record formats of the database file
            RecordFormat[] format = inRecDesc.retrieveRecordFormat();
            // Create an AS400File object that represents the file
            SequentialFile as400seqFile = new SequentialFile(remoteServer, pathString);

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

                // Source record is composed of three source record fields: seq. number, date, source data.
                DecimalFormat df1 = new DecimalFormat("0000.00");
                DecimalFormat df2 = new DecimalFormat("000000");

                // Sequence number - 6 bytes
                String seq = df1.format((Number) inRecord.getField("SRCSEQ"));
                String seq2 = seq.substring(0, 4) + seq.substring(5);
                textLine.append(seq2);
                // Date - 6 bytes
                String srcDat = df2.format((Number) inRecord.getField("SRCDAT"));
                // textLine.append(srcDat);
                textLine.append(srcDat);
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
            // Set scroll bar to top
            textArea.setCaretPosition(0);
            // Display the window.
            setVisible(true);
            row = "Info: Source member  " + as400PathString + "  has CCSID  " + sourceCcsidInt + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes);

        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error: " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes);
        }
        // Remove message scroll listener (cancel scrolling to the last message)
        mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
    }

    /**
     *
     * @param textComponent
     * @param position
     * @throws BadLocationException
     */
    private static void scrollToCenter(JTextComponent textComponent, int position)
            throws BadLocationException {
        Rectangle rectangle = textComponent.modelToView(position);
        Container container = SwingUtilities.getAncestorOfClass(JViewport.class, textComponent);
        if (Objects.nonNull(rectangle) && container instanceof JViewport) {
            rectangle.x = (int) (rectangle.x - container.getWidth() * 0.5);
            rectangle.width = container.getWidth();
            rectangle.height = (int) (container.getHeight() * 0.5);
            textComponent.scrollRectToVisible(rectangle);
        }
    }

    /**
     *
     * @return
     */
    protected Pattern getPattern() {
        String pattern = findField.getText();
        if (Objects.isNull(pattern) || pattern.isEmpty()) {
            return null;
        }
        try {
            // Allow backslash, asterisk, plus, question mark etc.
            // The backslash must be tested first!!!         
            pattern = pattern.replace("\\", "\\\\");
            pattern = pattern.replace("*", "\\*");
            pattern = pattern.replace("+", "\\+");
            pattern = pattern.replace("?", "\\?");
            pattern = pattern.replace("$", "\\$");
            pattern = pattern.replace(".", "\\.");
            pattern = pattern.replace("[", "\\[");
            pattern = pattern.replace("^", "\\^");
            pattern = pattern.replace("_", "\\_");
            pattern = pattern.replace("|", "\\|");
            pattern = pattern.replace("{", "\\{");
            pattern = pattern.replace("(", "\\(");
            pattern = pattern.replace(")", "\\)");
            pattern = pattern.replace("`", "\\`");
            pattern = pattern.replace("%", "\\%");

            pattern = String.format("%1$s", pattern); // 1 = first argument, s = string conversion
            int flags = matchCaseButton.getSelectedIcon().equals(matchCaseIconDark) ? 0
                    : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            return Pattern.compile(pattern, flags);
        } catch (PatternSyntaxException exc) {
            findField.setBackground(WARNING_COLOR);
            return null;
        }
    }

    /**
     * Find all matches and highlight it YELLOW (highlightPainter), then hihglight current match ORANGE for the text
     * area.
     */
    protected void changeHighlight() {
        Highlighter highlighter = textArea.getHighlighter();
        highlighter.removeAllHighlights();
        findField.setBackground(Color.WHITE);
        try {
            Pattern pattern = getPattern();
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
                    highlighter.addHighlight(start, end, highlightPainter);
                    startOffsets.add(start);
                    endOffsets.add(end);
                    highlightMap.put(start, end);
                    pos = end;
                }
            }
            JLabel label = layerUI.hint;
            Highlighter.Highlight[] array = highlighter.getHighlights();
            int hits = array.length; // number of highlighted intervals found.
            if (hits > 0) { // If at least one interval was found.
                if (direction.equals("forward")) {
                    // Forward direction
                    startOffset = highlightMap.ceilingKey(curPos); // Get next interval start - greater or equal
                    if (startOffset == null) {
                        startOffset = highlightMap.ceilingKey(0); // First interval
                    }
                    endOffset = highlightMap.get(startOffset);     // This interval end
                    sequence = startOffsets.indexOf(startOffset);  // Sequence number of the interval
                    Highlighter.Highlight hh = highlighter.getHighlights()[sequence];
                    highlighter.removeHighlight(hh);
                    highlighter.addHighlight(hh.getStartOffset(), hh.getEndOffset(), currentPainter);
                    textArea.setCaretPosition(startOffset);
                    curPos = startOffset;
                } else {
                    // Backward direction
                    startOffset = highlightMap.floorKey(curPos);
                    if (startOffset == null) {
                        startOffset = highlightMap.lastKey(); // Last interval
                    }
                    endOffset = highlightMap.get(startOffset);
                    sequence = startOffsets.indexOf(startOffset);
                    Highlighter.Highlight hh = highlighter.getHighlights()[sequence];
                    highlighter.removeHighlight(hh);
                    highlighter.addHighlight(hh.getStartOffset(), hh.getEndOffset(), currentPainter);
                    textArea.setCaretPosition(startOffset);
                    curPos = startOffset;
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
        findField.repaint();
    }

    /**
     *
     */
    class HighlightListener implements DocumentListener {

        @Override
        public void changedUpdate(DocumentEvent de) {
            /* not needed */ }

        @Override
        public void insertUpdate(DocumentEvent de) {
            changeHighlight();
        }

        @Override
        public void removeUpdate(DocumentEvent de) {
            changeHighlight();
        }
    }

    /**
     * Set indicator N/M that overlays the search field and is right adjusted N - the sequence number of the text that
     * is just highlighted, M - how many matches were found.
     */
    class PlaceholderLayerUI extends LayerUI<JTextComponent> {

        public final JLabel hint = new JLabel() {
            @Override
            public void updateUI() {
                super.updateUI();
                // setForeground(UIManager.getColor("TextField.inactiveForeground"));

                // blue little saturated dim (gray)
                setForeground(Color.getHSBColor(0.60f, 0.2f, 0.5f));
                // red little saturated bright
                setBackground(Color.getHSBColor(0.00f, 0.2f, 0.98f));
            }
        };

        @Override
        public void paint(Graphics g, JComponent c) {
            super.paint(g, c);
            if (c instanceof JLayer) {
                JLayer jlayer = (JLayer) c;
                JTextComponent tc = (JTextComponent) jlayer.getView();
                if (!tc.getText().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setPaint(hint.getForeground());
                    Insets i = tc.getInsets();
                    Dimension d = hint.getPreferredSize();
                    int x = tc.getWidth() - i.right - d.width - 2;
                    int y = (tc.getHeight() - d.height) / 2;
                    g2.translate(x, y);
                    SwingUtilities.paintComponent(g2, hint, tc, 0, 0, d.width, d.height);
                    g2.dispose();
                }
            }
        }
    }

    /**
     * Inner class for Escape function key
     */
    class Escape extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent de) {
            dispose();
        }
    }

    /**
     * Inner class for Ctrl + Arrow Up function key
     */
    class ArrowUp extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent de) {
            direction = "backward";
            prevButton.setIcon(prevActiveIcon);
            nextButton.setIcon(nextInactiveIcon);
            if (highlightMap.lowerKey(curPos) == null) {
                curPos = highlightMap.lastKey();
            } else {
                curPos = highlightMap.lowerKey(curPos);
            }
            changeHighlight();
        }
    }

    /**
     * Inner class for Ctrl + Arrow Down function key
     */
    class ArrowDown extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent de) {
            direction = "forward";
            prevButton.setIcon(prevInactiveIcon);
            nextButton.setIcon(nextActiveIcon);
            if (highlightMap.higherKey(curPos) == null) {
                curPos = 0;
            } else {
                curPos = highlightMap.higherKey(curPos);
            }
            changeHighlight();
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
                        textArea.setFont(new Font(fontName, Font.PLAIN, fontSize));
                        setText(fontName);
                    }
                }
            }
            if (operatingSystem.equals("WINDOWS")) {
                for (String str : fontNamesWin) {
                    if (str.equals(fontName)) {
                        textArea.setFont(new Font(fontName, Font.PLAIN, fontSize));
                        setText(fontName);
                    }
                }
            }
            return this;
        }
    }

    /**
     * Mouse listener for primary text area.
     */
    class TextAreaMouseListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent mouseEvent) {
            Point pt = new Point(mouseEvent.getX(), mouseEvent.getY());
            curPos = textArea.getUI().viewToModel2D(textArea, pt, new Bias[0]);
            changeHighlight();
        }
    }

}
