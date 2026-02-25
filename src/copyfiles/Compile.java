package copyfiles;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400JPing;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.MemberDescription;
import com.ibm.as400.access.Job;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.SpooledFile;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;

/**
 * Compile source files or IFS files
 *
 * @author Vladimír Župka 2017
 */
public class Compile extends JDialog {

    int windowWidth = 850;
    int windowHeight = 400;

    final Color DIM_BLUE = new Color(50, 60, 160);
    final Color DIM_RED = new Color(190, 60, 50);
    final Color DIM_PINK = new Color(170, 58, 128);

    // Path to UserLibraryList.lib file
    Path libraryListPath = Paths.get(System.getProperty("user.dir"), "workfiles", "UserLibraryList.lib");
    // Path to CurrentLibrary.lib file
    Path currentLibraryPath = Paths.get(System.getProperty("user.dir"), "workfiles", "CurrentLibrary.lib");
    // Path to CompileAttributes.lib file
    Path compileCommandsPath = Paths.get(System.getProperty("user.dir"), "workfiles", "CompileAttributes.lib");
    // Path to SrcAttrib.png file
    Path saveSrcAttribIconPathDark = Paths.get(System.getProperty("user.dir"), "icons", "saveSrcAttrib.png");
    // Path to noSrcAttrib.png file
    Path noSrcAttribIconPathDim = Paths.get(System.getProperty("user.dir"), "icons", "noSrcAttrib.png");
    // Path to clear SrcAttrib.png file
    Path clearSrcAttribIconPath = Paths.get(System.getProperty("user.dir"), "icons", "clearSrcAttrib.png");

    Container cont;
    JPanel globalPanel;
    JPanel titlePanel;
    JPanel commandSelectionPanel;
    JPanel parameterPanel;
    JPanel commandPanel;
    JPanel buttonPanel;

    JList<String> messageList;
    JScrollPane scrollMessagePane = new JScrollPane(messageList);

    ArrayList<String> libraryNameVector = new ArrayList<>();
    ArrayList<String> msgVector = new ArrayList<>();

    String row;
    MessageScrollPaneAdjustmentListenerMax messageScrollPaneAdjustmentListenerMax;

    GroupLayout globalPanelLayout;
    GroupLayout cmdSelLayout;
    GroupLayout paramLayout;

    JLabel pathLabel = new JLabel();

    static WrkSplFCall wrkSplFCall;
    WrkSplF wrkSplf;
    String ibmCcsid;

    AS400 remoteServer;
    MainWindow mainWindow;
    String hostAddress;
    String qsyslib;
    String libraryName;
    String fileName;
    String memberName;

    JLabel sourceTypeLabel;
    ArrayList<String> sourceTypes;
    String sourceType;
    JComboBox sourceTypeComboBox;

    JLabel compileCommandLabel;
    ArrayList<String> compileCommands;
    TreeMap<String, String> compileCommandsMap = new TreeMap<>();
    String compileCommandName;
    String commandObjectType;

    JComboBox compileCommandsComboBox;

    JTextArea commandTextArea = new JTextArea();
    JScrollPane areaScrollPane = new JScrollPane(commandTextArea);
    String sourceAttributes; // Custom input compile parameters
    String[] attributesArray;
    String attributes;
    ArrayList<String> attributesArrayList;
    ArrayList<String> commandNames;
    ArrayList<String> commandNamesArrayList;

    // Icon Aa will be dimmed or dark when clicked
    ImageIcon saveSrcAttribIconDark = new ImageIcon(saveSrcAttribIconPathDark.toString());
    ImageIcon noSrcAttribIconDim = new ImageIcon(noSrcAttribIconPathDim.toString());
    ImageIcon clearSrcAttribIcon = new ImageIcon(clearSrcAttribIconPath.toString());
    JToggleButton sourceAttributesButton = new JToggleButton();
    JButton clearSrcAttribButton = new JButton();
    JButton changeLibraryListButton;
    
    TreeMap<String, String> sourceFilesAndTypes = new TreeMap<>();
    TreeMap<String, ArrayList<String>> sourceTypesAndCommands = new TreeMap<>();

    String[] commandNameArray;
    String commandText;
    ActionListener commandsComboBoxListener;
    ActionListener sourceTypeComboBoxListener;
    ActionListener librariesComboBoxListener;

    JLabel libraryPatternLabel = new JLabel("Library pattern:");
    JTextField libraryPatternTextField = new JTextField();
    String libraryPattern;
    String libraryField;
    String libraryWildCard;

    ArrayList<String> librariesArrayList;
    JComboBox librariesComboBox;
    JLabel librariesLabel = new JLabel("Compiled object       Library:");
    String libNamePar;

    JLabel objectNameLabel = new JLabel("Object:");
    JTextField objectNameFld;
    String objNamePar;

    JButton cancelButton = new JButton("Cancel");
    JButton performButton = new JButton("Perform command");
    JButton lastSplfButton = new JButton("Last spooled file");
    JButton spooledFileButton = new JButton("Spooled files");
    JButton jobLogButton = new JButton("Job log");
    JButton clearButton = new JButton("Clear messages");

    String compileNotSupported = "Compile not supported.";

    Properties properties;
    Path parPath = Paths.get(System.getProperty("user.dir"), "paramfiles", "Parameters.txt");
    String PROP_COMMENT = "Copy files between IBM i and PC, edit and compile.";
    String encoding = System.getProperty("file.encoding", "UTF-8");
    String sourceTypePar;

    int compileWindowX;
    int compileWindowY;

    String compileWindowXString;
    String compileWindowYString;

    String pathString;
    boolean ifs;

    /**
     * Constructor
     *
     * @param remoteServer
     * @param mainWindow
     * @param pathString
     * @param ifs
     */
    public Compile(AS400 remoteServer, MainWindow mainWindow, String pathString, boolean ifs) {
        super();
        super.setModalityType(Dialog.ModalityType.MODELESS);
        super.setTitle("Compile  '" + pathString + "'");

        this.mainWindow = mainWindow;        
        this.remoteServer = remoteServer;
        this.pathString = pathString;
        this.ifs = ifs;

        try {
            if (!Files.exists(compileCommandsPath)) {
                Files.createFile(compileCommandsPath);
            }
        } catch (IOException exc) {
            exc.printStackTrace();
        }

        globalPanel = new JPanel();
        titlePanel = new JPanel();
        commandSelectionPanel = new JPanel();
        parameterPanel = new JPanel();
        commandPanel = new JPanel();
        buttonPanel = new JPanel();

        cmdSelLayout = new GroupLayout(commandSelectionPanel);
        paramLayout = new GroupLayout(parameterPanel);

        // Source types
        String[] sourceFileTypes = {"C", "CBL", "CBLLE", "CLLE", "CLP", "CMD", "CPP", "DSPF", "LF",
            "PF", "PRTF", "RPG", "RPGLE", "SQL", "SQLC", "SQLCPP", "SQLCBL", "SQLCBLLE", "SQLRPG",
            "SQLRPGLE", "TBL", "PNLGRP"};

        // Command lists for source types
        ArrayList<String> C = new ArrayList<>();
        ArrayList<String> CBL = new ArrayList<>();
        ArrayList<String> CBLLE = new ArrayList<>();
        ArrayList<String> CLLE = new ArrayList<>();
        ArrayList<String> CLP = new ArrayList<>();
        ArrayList<String> CMD = new ArrayList<>();
        ArrayList<String> CPP = new ArrayList<>();
        ArrayList<String> DSPF = new ArrayList<>();
        ArrayList<String> LF = new ArrayList<>();
        ArrayList<String> PF = new ArrayList<>();
        ArrayList<String> PRTF = new ArrayList<>();
        ArrayList<String> RPG = new ArrayList<>();
        ArrayList<String> RPGLE = new ArrayList<>();
        ArrayList<String> SQL = new ArrayList<>();
        ArrayList<String> SQLC = new ArrayList<>();
        ArrayList<String> SQLCPP = new ArrayList<>();
        ArrayList<String> SQLCBL = new ArrayList<>();
        ArrayList<String> SQLCBLLE = new ArrayList<>();
        ArrayList<String> SQLRPG = new ArrayList<>();
        ArrayList<String> SQLRPGLE = new ArrayList<>();
        ArrayList<String> TBL = new ArrayList<>();
        ArrayList<String> PNLGRP = new ArrayList<>();

        // Compile commands added to array lists
        C.add("CRTBNDC");
        C.add("CRTCMOD");
        CBL.add("CRTCBLPGM");
        CBLLE.add("CRTBNDCBL");
        CBLLE.add("CRTCBLMOD");
        CLLE.add("CRTBNDCL");
        CLLE.add("CRTCLMOD");
        CLP.add("CRTCLPGM");
        CMD.add("CRTCMD");
        CPP.add("CRTBNDCPP");
        CPP.add("CRTCPPMOD");
        DSPF.add("CRTDSPF");
        LF.add("CRTLF");
        PF.add("CRTPF");
        PRTF.add("CRTPRTF");
        RPG.add("CRTRPGPGM");
        RPGLE.add("CRTBNDRPG");
        RPGLE.add("CRTRPGMOD");
        SQL.add("RUNSQLSTM");
        SQLC.add("CRTSQLCI *MODULE");
        SQLC.add("CRTSQLCI *PGM");
        SQLC.add("CRTSQLCI *SRVPGM");
        SQLCPP.add("CRTSQLCPPI");
        SQLCBL.add("CRTSQLCBL");
        SQLCBLLE.add("CRTSQLCBLI *PGM");
        SQLCBLLE.add("CRTSQLCBLI *SRVPGM");
        SQLCBLLE.add("CRTSQLCBLI *MODULE");
        SQLRPG.add("CRTSQLRPG");
        SQLRPGLE.add("CRTSQLRPGI *PGM");
        SQLRPGLE.add("CRTSQLRPGI *SRVPGM");
        SQLRPGLE.add("CRTSQLRPGI *MODULE");
        TBL.add("CRTTBL");
        PNLGRP.add("CRTPNLGRP");

        // Table of Source Types (keys) and Compile Commands array lists (values)
        sourceTypesAndCommands.put("C", C);
        sourceTypesAndCommands.put("CBL", CBL);
        sourceTypesAndCommands.put("CBLLE", CBLLE);
        sourceTypesAndCommands.put("CLLE", CLLE);
        sourceTypesAndCommands.put("CLP", CLP);
        sourceTypesAndCommands.put("CMD", CMD);
        sourceTypesAndCommands.put("CPP", CPP);
        sourceTypesAndCommands.put("DSPF", DSPF);
        sourceTypesAndCommands.put("LF", LF);
        sourceTypesAndCommands.put("PF", PF);
        sourceTypesAndCommands.put("PRTF", PRTF);
        sourceTypesAndCommands.put("RPG", RPG);
        sourceTypesAndCommands.put("RPGLE", RPGLE);
        sourceTypesAndCommands.put("SQL", SQL);
        sourceTypesAndCommands.put("SQLC", SQLC);
        sourceTypesAndCommands.put("SQLCPP", SQLCPP);
        sourceTypesAndCommands.put("SQLCBL", SQLCBL);
        sourceTypesAndCommands.put("SQLCBLLE", SQLCBLLE);
        sourceTypesAndCommands.put("SQLRPG", SQLRPG);
        sourceTypesAndCommands.put("SQLRPGLE", SQLRPGLE);
        sourceTypesAndCommands.put("TBL", TBL);
        sourceTypesAndCommands.put("PNLGRP", PNLGRP);

        // Source types combo box - fill with data
        sourceTypes = new ArrayList<>();
        sourceTypes.addAll(Arrays.asList(sourceFileTypes));

        sourceTypeComboBox = new JComboBox(sourceTypes.toArray());
        sourceTypeComboBox.setToolTipText("Select from possible source types or enter one.");
        sourceTypeComboBox.setPreferredSize(new Dimension(100, 20));
        sourceTypeComboBox.setMinimumSize(new Dimension(100, 20));
        sourceTypeComboBox.setMaximumSize(new Dimension(100, 20));
        sourceTypeComboBox.setEditable(true);

        // Get application parameters from "Parameters.txt" file.
        getAppProperties();

        hostAddress = properties.getProperty("HOST");
        
        // Create compile commands combo box with preselected item
        compileCommandsComboBox = new JComboBox();
        compileCommandsComboBox.setToolTipText("Select from possible compile commands.");
        compileCommandsComboBox.setPreferredSize(new Dimension(170, 20));
        compileCommandsComboBox.setMinimumSize(new Dimension(170, 20));
        compileCommandsComboBox.setMaximumSize(new Dimension(170, 20));
        compileCommandsComboBox.setEditable(true);

        sourceTypeLabel = new JLabel("Source type:");
        compileCommandLabel = new JLabel("Compile command:");

        sourceAttributes = properties.getProperty("SOURCE_ATTRIBUTES");
        
        if (sourceAttributes.equals("SAVE_SOURCE_ATTRIBUTES")) {
            sourceAttributesButton.setIcon(saveSrcAttribIconDark);
            sourceAttributesButton.setSelectedIcon(saveSrcAttribIconDark);
            sourceAttributesButton.setToolTipText("Save source attributes. Toggle Do not save source attributes.");
        } else {
            sourceAttributesButton.setIcon(noSrcAttribIconDim);
            sourceAttributesButton.setSelectedIcon(noSrcAttribIconDim);
            sourceAttributesButton.setToolTipText("Do not save source attributes. Toggle Save source attributes.");
        }
        sourceAttributesButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        sourceAttributesButton.setContentAreaFilled(true);
        sourceAttributesButton.setPreferredSize(new Dimension(25, 20));

        clearSrcAttribButton.setIcon(clearSrcAttribIcon);
        clearSrcAttribButton.setToolTipText("Clear source attributes.");
        clearSrcAttribButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        clearSrcAttribButton.setContentAreaFilled(true);
        clearSrcAttribButton.setPreferredSize(new Dimension(25, 20));

        changeLibraryListButton = new JButton("Library list");
        changeLibraryListButton.setToolTipText("Set user library list and current library.");
        // Set and create the panel layout
        commandSelectionPanel.setLayout(cmdSelLayout);
        cmdSelLayout.setHorizontalGroup(cmdSelLayout.createSequentialGroup()
                .addGroup(cmdSelLayout.createSequentialGroup()
                        .addComponent(sourceTypeLabel)
                        .addComponent(sourceTypeComboBox)
                        .addGap(5)
                        .addComponent(compileCommandLabel)
                        .addComponent(compileCommandsComboBox)
                        .addGap(5)
                        .addComponent(sourceAttributesButton)
                        .addComponent(clearSrcAttribButton)
                        .addGap(10)
                        .addComponent(changeLibraryListButton)));
        cmdSelLayout.setVerticalGroup(cmdSelLayout.createSequentialGroup()
                .addGroup(cmdSelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(sourceTypeLabel)
                        .addComponent(sourceTypeComboBox)
                        .addGap(5)
                        .addComponent(compileCommandLabel)
                        .addComponent(compileCommandsComboBox)
                        .addGap(5)
                        .addComponent(sourceAttributesButton)
                        .addComponent(clearSrcAttribButton)
                        .addGap(10)
                        .addComponent(changeLibraryListButton)));

        libraryPatternTextField.setToolTipText("Library search pattern. Can use * and ? wild cards.");
        libraryPatternTextField.setText(libraryPattern);
        libraryPatternTextField.setPreferredSize(new Dimension(100, 20));
        libraryPatternTextField.setMinimumSize(new Dimension(100, 20));
        libraryPatternTextField.setMaximumSize(new Dimension(100, 20));

        String[] selectedLibraries = getListOfLibraries(libraryPattern);

        // Source types combo box - fill with data
        librariesArrayList = new ArrayList<>();
        librariesArrayList.addAll(Arrays.asList(selectedLibraries));
        librariesComboBox = new JComboBox(librariesArrayList.toArray());
        librariesComboBox.setToolTipText("Select library name of the compiled object.");
        librariesComboBox.setPreferredSize(new Dimension(120, 20));
        librariesComboBox.setMinimumSize(new Dimension(120, 20));
        librariesComboBox.setMaximumSize(new Dimension(120, 20));
        librariesComboBox.setEditable(true);

        objectNameFld = new JTextField();
        objectNameFld.setToolTipText("Enter name of compiled object.");
        objectNameFld.setPreferredSize(new Dimension(100, 20));
        objectNameFld.setMinimumSize(new Dimension(100, 20));
        objectNameFld.setMaximumSize(new Dimension(100, 20));

        cancelButton.setToolTipText("Return to previous window.");
        performButton.setToolTipText("Start compilation.");
        lastSplfButton.setToolTipText("Display the last spooled file for the current user.");
        spooledFileButton.setToolTipText("Display list of spooled files for the current user.");
        jobLogButton.setToolTipText("Print actual contents of job log.");
        clearButton.setToolTipText("Delete all messages from the message area.");

        // Set and create the parameter panel layout
        parameterPanel.setLayout(paramLayout);
        paramLayout.setHorizontalGroup(paramLayout.createSequentialGroup()
                .addGroup(paramLayout.createSequentialGroup()
                        .addComponent(librariesLabel)
                        .addComponent(librariesComboBox)
                        .addGap(5)
                        .addComponent(objectNameLabel)
                        .addComponent(objectNameFld)
                        .addGap(5)
                        .addComponent(libraryPatternLabel)
                        .addComponent(libraryPatternTextField)));
        paramLayout.setVerticalGroup(paramLayout.createSequentialGroup()
                .addGroup(paramLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(libraryPatternLabel)
                        .addComponent(libraryPatternTextField)
                        .addComponent(librariesLabel)
                        .addComponent(librariesComboBox)
                        .addGap(5)
                        .addComponent(objectNameLabel)
                        .addComponent(objectNameFld)
                        .addGap(5)
                        .addComponent(libraryPatternLabel)
                        .addComponent(libraryPatternTextField)));

        buttonPanel.add(cancelButton);
        buttonPanel.add(performButton);
        buttonPanel.add(lastSplfButton);
        buttonPanel.add(spooledFileButton);
        buttonPanel.add(jobLogButton);
        buttonPanel.add(clearButton);

        // Set layout and dimensions of panels
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.LINE_AXIS));
        titlePanel.setPreferredSize(new Dimension(windowWidth, 60));
        titlePanel.setMinimumSize(new Dimension(windowWidth, 60));
        titlePanel.setMaximumSize(new Dimension(windowWidth, 60));
        // The 'pathLabel' is placed in 'titlePanel
        pathLabel.setFont(pathLabel.getFont().deriveFont(Font.BOLD, 18));

        commandSelectionPanel.setLayout(new BoxLayout(commandSelectionPanel, BoxLayout.LINE_AXIS));
        commandSelectionPanel.setPreferredSize(new Dimension(windowWidth, 40));
        commandSelectionPanel.setMinimumSize(new Dimension(windowWidth, 40));
        commandSelectionPanel.setMaximumSize(new Dimension(windowWidth, 40));

        parameterPanel.setLayout(new BoxLayout(parameterPanel, BoxLayout.LINE_AXIS));
        parameterPanel.setPreferredSize(new Dimension(windowWidth, 40));
        parameterPanel.setMinimumSize(new Dimension(windowWidth, 40));
        parameterPanel.setMaximumSize(new Dimension(windowWidth, 40));
                
        commandPanel.setLayout(new BoxLayout(commandPanel, BoxLayout.LINE_AXIS));
        commandPanel.setPreferredSize(new Dimension(windowWidth, 90));
        commandTextArea.setLineWrap(true);
        commandTextArea.setWrapStyleWord(true);
        commandTextArea.setFont(new Font("Monospaced", Font.PLAIN, 16));
        areaScrollPane.setBorder(BorderFactory.createEmptyBorder());
        commandPanel.add(areaScrollPane);  // 'areaScrollPane' contains 'commandTextArea'

        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setPreferredSize(new Dimension(windowWidth, 40));
        buttonPanel.setMinimumSize(new Dimension(windowWidth, 40));
        buttonPanel.setMaximumSize(new Dimension(windowWidth, 40));
        

        // Scroll pane for message list
        scrollMessagePane.setPreferredSize(new Dimension(windowWidth, 180));
        scrollMessagePane.setBorder(BorderFactory.createEmptyBorder());

        // List of messages for placint into message scroll pane
        messageList = new JList<>();

        // Background color of message list
        messageList.setSelectionBackground(Color.WHITE);

        // Decision what color the message will get
        messageList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                Component component = super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                if (value.toString().contains("*COMPLETION")) {
                    this.setForeground(DIM_BLUE);
                } else if (value.toString().contains("*ESCAPE")) {
                    this.setForeground(DIM_RED);
                } else if (value.toString().contains("*INFORMATIONAL")) {
                    this.setForeground(Color.BLACK);
                } else if (value.toString().contains("*NOTIFY")) {
                    this.setForeground(DIM_PINK);
                } else {
                    this.setForeground(Color.GRAY);
                }
                return component;
            }
        });

        // Build list of messages
        buildMessageList();

        // Make the message table visible in the message scroll pane
        scrollMessagePane.setViewportView(messageList);
        // Create scroll pane adjustment listener
        messageScrollPaneAdjustmentListenerMax = new MessageScrollPaneAdjustmentListenerMax();

        createGlobalPanelLayout();

        // Listeners for command selection panel
        // -------------------------------------
        //
        // Source type combo box
        sourceTypeComboBoxListener = new SourceTypeComboBoxListener();
        // Compile command combo box
        commandsComboBoxListener = new CommandsComboBoxListener();
        // Libraries combo box
        librariesComboBoxListener = new LibrariesComboBoxListener();
        librariesComboBox.addActionListener(librariesComboBoxListener);

        // Save source attributes button listener
        sourceAttributesButton.addActionListener((ActionEvent ae) -> {
            if (sourceAttributesButton.getSelectedIcon().equals(saveSrcAttribIconDark)) {
                sourceAttributesButton.setSelectedIcon(noSrcAttribIconDim);
                sourceAttributesButton.setToolTipText("Do not save source attributes. Toggle Save source attributes.");
                sourceAttributes = "NO_SOURCE_ATTRIBUTES";
            } else {
                sourceAttributesButton.setSelectedIcon(saveSrcAttribIconDark);
                sourceAttributesButton.setToolTipText("Save source attributes. Toggle Do not save source attributes.");
                sourceAttributes = "SAVE_SOURCE_ATTRIBUTES";
            }
            // Save custom defaults mode into properties
            try (BufferedWriter outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding))) {
                // Save custom defaults mode into properties
                properties.setProperty("SOURCE_ATTRIBUTES", sourceAttributes);
                properties.store(outfile, PROP_COMMENT);
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        });

        // Clear source attributes button listener
        clearSrcAttribButton.addActionListener(ae -> {
            try {  // Empty the custom defaults file
                Files.write(compileCommandsPath, new ArrayList(), StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException exc) {
                row = "Error: Clearing of file  " + compileCommandsPath + "  failed.";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
            }
            compileCommandsMap.clear();  // Clear also commands map
            row = "Comp:  Custom defaults of compile commands were cleared. (File  " + compileCommandsPath + "  is empty).";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
        });

        // Listener for Change library list button 
        changeLibraryListButton.addActionListener((var en) -> {
            compileWindowX = this.getX();
            compileWindowY = this.getY();
            // Call window to change library list
            new ChangeLibraryList(remoteServer, compileWindowX, compileWindowY);
        });

        // Listeners for parameter panel
        // -----------------------------
        // Library pattern listener
        libraryPatternTextField.addActionListener(en -> {
            libraryPattern = libraryPatternTextField.getText().toUpperCase();
            libraryPatternTextField.setText(libraryPattern);
            String[] librariesArr = getListOfLibraries(libraryPattern);
            // Disable Libraries combo box listener so tHat writing in the box list does not invoke its listener.
            //librariesComboBox.removeActionListener(librariesComboBoxListener);
            librariesComboBox.removeAllItems();
            for (String librariesArr1 : librariesArr) {
                librariesComboBox.addItem(librariesArr1);
            }
        });

        // Object name text field listener
        objectNameFld.addActionListener(en -> {
            objNamePar = objectNameFld.getText();
            compileCommandName = (String) compileCommandsComboBox.getSelectedItem();

            if (sourceAttributes.equals("SAVE_SOURCE_ATTRIBUTES")) {
                updateModifiedAttributes("object");
            }

            commandText = buildCommand(compileCommandName, libNamePar, objNamePar);
            if (commandText == null) {
                commandTextArea.setText(compileNotSupported);
                commandTextArea.setForeground(DIM_RED);
            } else {
                commandTextArea.setForeground(DIM_BLUE);
                commandTextArea.setText(commandText);
            }
            
        });

        // Listeners for button panel
        // --------------------------
        //
        // Cancel button listener
        cancelButton.addActionListener(en -> {
            // Get current window coordinates
            compileWindowX = this.getX();
            compileWindowY = this.getY();
            // Save the coordinates for future display
            saveWindowCoordinates(compileWindowX, compileWindowY);
            // Make the window invisible.
            this.setVisible(false);
        });

        // Enable ESCAPE key to escape from display
        // ----------------------------------------
        globalPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "escape");
        globalPanel.getActionMap().put("escape", new Escape());

        // This listener does the same as the Cancel button
        addWindowListener(new WindowClosing());

        // Perform command button listener
        performButton.addActionListener(en -> {
            performCommand(commandTextArea.getText());  // text may have been modified by user
        });

        // Job log button listener
        jobLogButton.addActionListener(en -> {
            printJobLog();
        });

        // Last spooled file button listener
        lastSplfButton.addActionListener(en -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            extractNamesFromIfsPath(pathString);
            String className = this.getClass().getSimpleName();
            String userPar = properties.getProperty("USERNAME");
            ibmCcsid = properties.getProperty("IBM_CCSID");
            if (!ibmCcsid.equals("*DEFAULT")) {
                try {
                } catch (Exception exc) {
                    // If ibmCcsid is not numeric, take 65535
                    exc.printStackTrace();
                    ibmCcsid = "65535";
                }
            } else {
                // *DEFAULT for spooled file is 65535
                ibmCcsid = "65535";
            }
            // Display last spooled file directly in "WrkSplF" class
            wrkSplf = new WrkSplF(remoteServer, mainWindow, this.pathString, true, compileWindowX, compileWindowY, className);

            // Resulting splf is the last from the list of all spooled files belonging to the current user.
            SpooledFile splf = wrkSplf.selectSpooledFiles("", "", "", userPar, "", "", "", "");
            if (splf != null) {
                String spoolTextAreaString = wrkSplf.convertSpooledFile(splf);
                JTextArea textArea = new JTextArea();
                DisplayFile dspf = new DisplayFile(textArea, mainWindow);
                dspf.displayTextArea(spoolTextAreaString, ibmCcsid);
            } 
            // Remove message scroll listener (cancel scrolling to the last message)
            mainWindow.scrollMessagePane.getVerticalScrollBar().
                    removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
        });

        // Spooled file button listener
        spooledFileButton.addActionListener(en -> {
            scrollMessagePane.getVerticalScrollBar()
                    .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
            String className = this.getClass().getSimpleName();
            // Spool table window must be created only once
            if (wrkSplFCall == null) {
                // "true" stands for *CURRENT user, "false",
                // second "true" stands for "createSpoolTable".
                wrkSplFCall = new WrkSplFCall(remoteServer, mainWindow, this.pathString,
                        true, // *CURRENT user
                        compileWindowX, compileWindowY, className,
                        true);  // create spool file table
            }
            wrkSplFCall.execute(); // Run in parallel SwingWorker
        });

        // Clear messages button listener
        clearButton.addActionListener(en -> {
            msgVector.clear();  // empty array list
            messageList.removeAll();
            scrollMessagePane.setViewportView(messageList);
            reloadMessages();
            this.pack();
        });

        cont = getContentPane();

        cont.add(globalPanel);

        // Make the window visible
        // -----------------------
        setSize(windowWidth, windowHeight);

        setLocation(compileWindowX, compileWindowY);
        // No necessity to setting the window visible in constructor.
        setVisible(true);
        pack();
    }

    /**
     * This method is called from the MainWindow class. This method gets
     * application parameters from "Parameters.txt" file, updates custom default
     * parameters for source file and concludes window creation (show).
     *
     * @param pathString
     * @param ifs
     */
    public void compile(String pathString, boolean ifs) {
        this.pathString = pathString;
        this.ifs = ifs;
        //System.out.println("pathString: "+pathString);

        // Get actual source type of the source member
        IFSFile ifsFile = new IFSFile(remoteServer, pathString);
        if(pathString.startsWith("/QSYS.LIB")) {
            sourceType = getMemberType(ifsFile);
            String memberPathString = pathString.substring(0, pathString.lastIndexOf(".") + 1) + sourceType;
            super.setTitle("Compile  '" + memberPathString + "'");  // rewrite window title
        } else {

        // Get source type from IFS path string
            sourceType = pathString.substring(pathString.lastIndexOf(".") + 1);
        }
        // Disable combo boxes listeners
        // -----------------------------
        // Disable combo boxes listeners so that writing in them does not invoke their listeners.
        sourceTypeComboBox.removeActionListener(sourceTypeComboBoxListener);
        compileCommandsComboBox.removeActionListener(commandsComboBoxListener);
        librariesComboBox.removeActionListener(librariesComboBoxListener);

        // Get application parameters from "Parameters.txt" file.
        getAppProperties();

        // Decide if source file attributes modified by the user are to be saved or not.
        if (sourceAttributes.equals("SAVE_SOURCE_ATTRIBUTES")) {
            // Save and update modified attributes in "CompileAttributes.lib" for the source file to be compiled.
            attributesArray = new String[4];
            attributes = updateModifiedAttributes("compile");            
            attributesArray = attributes.split(",");
            ///sourceType = attributesArray[0];
            setCommandNames(sourceType);
            sourceTypeComboBox.setSelectedItem(sourceType);
            compileCommandName = attributesArray[1];
            compileCommandsComboBox.setSelectedItem(compileCommandName);
            librariesComboBox.setSelectedItem(attributesArray[2]);
            objectNameFld.setText(attributesArray[3]);
            libNamePar = attributesArray[2];
            objNamePar = attributesArray[3];
        } else {
            // The attributes of the source file are not to be saved.
            setCommandNames(sourceType);
            commandNames = getSourceFileAttributes(sourceType);
            // Get default (first) command name and set it to the combo box
            compileCommandName = commandNames.get(0);
            compileCommandsComboBox.setSelectedItem(compileCommandName);
            // Get default object names (libNamePar, objNamePar)
            getDefaultObjectNames();
        }

        // Build command text given compile command name (CRT...)
        commandText = buildCommand(compileCommandName, libNamePar, objNamePar);
        if (commandText == null) {
            commandTextArea.setText(compileNotSupported);
            commandTextArea.setForeground(DIM_RED);
        } else {
            commandTextArea.setForeground(DIM_BLUE);
            commandTextArea.setText(commandText);
        }

        // Enable combo boxes listeners
        // ----------------------------
        // Enable compile commands combo box listener
        compileCommandsComboBox.addActionListener(commandsComboBoxListener);
        // Enable source type combo box listener  - sets also compileCommandsComboBox.
        sourceTypeComboBox.addActionListener(sourceTypeComboBoxListener);
        librariesComboBox.addActionListener(librariesComboBoxListener);

        // Set the window visible again if it was closed (by click on close icon) or canceled by Cancel button.
        setVisible(true);
        //pack();
    }

    /**
     * Update user modified attributes in "CompileAttributes.lib" for the source
     * file to be compiled, The attributes are sourceType, compileCommand, library, and object.
     * The item (row) in the CompileAttributes.lib file
     * consists of four elements separated by a comma:
     * "pathString,sourceType,compileCommand,library,object".
     * A map is constructed for these items where pathString is a key and the
     * "attributes" is a value.
     *
     * @param whenCalled
     * @return
     */
    protected String updateModifiedAttributes(String whenCalled) {
        // Remember the source type and compile command name in the file "CompileAttributes.lib"
        String filePath;
        String attributesLoop;
        String cmdName;
        String libName;
        String objName;

        // Set array list of command names depending on source type
        commandNames = getSourceFileAttributes(sourceType);
        // Set default (first) command name
        cmdName = commandNames.get(0);
        // Set command names in the list of combo box
        setCommandNames(sourceType);
        // Set default object names (libNamePar, objNamePar)
        getDefaultObjectNames();
        // Build attributes separated by comma
        attributes = sourceType + "," + cmdName + "," + libNamePar + "," + objNamePar;

        compileCommandsComboBox.removeActionListener(commandsComboBoxListener);

        try {
            // Set default attributes
            List<String> items = Files.readAllLines(compileCommandsPath);
            // Read (<file-path>, <source-type, command-name>, library-name>, object-name>) 
            // from all items and write them in a map.
            for (String item : items) {
                filePath = item.substring(0, item.indexOf(","));
                if (filePath.equals(pathString)) {
                    // This filePath matches - take the attributes from the loop for the following processing
                    attributesLoop = item.substring(item.indexOf(",") + 1);
                    compileCommandsMap.put(filePath, attributesLoop); // Adds or updates the attributes.
                    attributes = attributesLoop;
                }
            }

            // Split attributes into the new array
            attributesArray = attributes.split(",");

            if (whenCalled.equals("compile")) {
                attributesArray[1] = cmdName;
                // Otherwise do nothing - attributes and attributesArray already set.
            }
            if (whenCalled.equals("sourceType")) {
                setCommandNames(sourceType);
                cmdName = compileCommands.get(0);
                attributes = sourceType + "," + cmdName + "," + attributesArray[2] + "," + attributesArray[3];
            }
            if (whenCalled.equals("commands")) {
                cmdName = compileCommandName;
                attributes = attributesArray[0] + "," + cmdName + "," + attributesArray[2] + "," + attributesArray[3];
            }
            if (whenCalled.equals("library")) {
                libName = (String) librariesComboBox.getSelectedItem();
                attributes = attributesArray[0] + "," + attributesArray[1] + "," + libName + "," + attributesArray[3];
            }
            if (whenCalled.equals("object")) {
                objName = objectNameFld.getText();
                attributes = attributesArray[0] + "," + attributesArray[1] + "," + attributesArray[2] + "," + objName;
            }
            compileCommandsMap.put(pathString, attributes);
           
            // Write contents of the map to array list.
            attributesArrayList = new ArrayList<>();
            if (!compileCommandsMap.isEmpty()) {
                compileCommandsMap.forEach((key, value) -> {
                    attributesArrayList.add(key + "," + value);
                });
            }

            // Write array list to the file.
            Files.write(compileCommandsPath, attributesArrayList);
            
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return attributes;
    }

    /**
     * Set command names belonging to the source type to the combo box 
     *
     * @param sourceType
     */
    protected void setCommandNames(String sourceType) {
        // Default values of command names for the given sourceType.
        // Set predefined commands (one or more) corresponing to the source type to the command combo box.
        compileCommands = getSourceFileAttributes(sourceType);
        compileCommandsComboBox.removeActionListener(commandsComboBoxListener);
        compileCommandsComboBox.removeAllItems();
        for (int idx = 0; idx < compileCommands.size(); idx++) {
            compileCommandsComboBox.addItem(compileCommands.get(idx));
        }
    }

    /**
     * Get compile commands array for a source type.
     *
     * @param sourceType
     * @return
     */
    protected ArrayList<String> getSourceFileAttributes(String sourceType) {
        commandNamesArrayList = sourceTypesAndCommands.get(sourceType);
        if (commandNamesArrayList == null) {
            commandNamesArrayList = new ArrayList<>();
            commandNamesArrayList.add("CRTCLPGM");
        }
        return commandNamesArrayList;
    }

    /**
     * Build text of compile command given command name and parameters
     *
     * @param compileCommand
     * @param libNamePar
     * @param objNamePar
     * @return
     */
    protected String buildCommand(String compileCommand, String libNamePar, String objNamePar) {

        switch (compileCommand) {
            // CLLE, CLP 
            case "CRTBNDCL": // CLLE 
            {
                commandText = compileCommand + " PGM(" + libNamePar + "/" + objNamePar + ") ";
                if (this.ifs) {
                    commandText += "SRCSTMF('" + pathString + "') ";
                    break;
                } else {
                    commandText += "SRCFILE(" + libraryName + "/" + fileName + ") SRCMBR(" + memberName + ") ";
                }
                commandText += " OUTPUT(*PRINT) DBGVIEW(*ALL)";
                break;
            }
            case "CRTCLPGM": // CLP
            {
                commandText = compileCommand + " PGM(" + libNamePar + "/" + objNamePar + ") ";
                if (this.ifs) {
                    // IFS not available.
                    return null;
                } else {
                    commandText += "SRCFILE(" + libraryName + "/" + fileName + ") SRCMBR(" + memberName + ") ";
                }
                commandText += " OUTPUT(*PRINT)";
                break;
            }
            case "CRTCLMOD": // CLLE
            {
                commandText = compileCommand + " MODULE(" + libNamePar + "/" + objNamePar + ") ";
                if (this.ifs) {
                    commandText += "SRCSTMF('" + pathString + "') ";
                    break;
                } else {
                    commandText += "SRCFILE(" + libraryName + "/" + fileName + ") SRCMBR(" + memberName + ") ";
                }
                commandText += " OUTPUT(*PRINT) DBGVIEW(*ALL)";
                break;
            }

            // OPM RPG
            case "CRTRPGPGM": {
                commandText = compileCommand + " PGM(" + libNamePar + "/" + objNamePar + ") ";
                if (this.ifs) {
                    // IFS not available.
                    return null;
                } else {
                    commandText += "SRCFILE(" + libraryName + "/" + fileName + ") SRCMBR(" + memberName + ") ";
                }
                commandText += " OPTION(*SOURCE)";
                break;

            }
            // OPM Cobol
            case "CRTCBLPGM": {
                commandText = compileCommand + " PGM(" + libNamePar + "/" + objNamePar + ") ";
                if (this.ifs) {
                    // IFS not available.
                    return null;
                } else {
                    commandText += "SRCFILE(" + libraryName + "/" + fileName + ") SRCMBR(" + memberName + ") ";
                }
                commandText += " OPTION(*SOURCE *PRINT)";
                break;
            }
            // ILE languages without SQL
            case "CRTBNDRPG":
            case "CRTBNDCBL":
            case "CRTBNDC":
            case "CRTBNDCPP": {
                commandText = compileCommand + " PGM(" + libNamePar + "/" + objNamePar + ") ";
                if (this.ifs) {
                    commandText += "SRCSTMF('" + pathString + "') ";
                } else {
                    commandText += "SRCFILE(" + libraryName + "/" + fileName + ") SRCMBR(" + memberName + ") ";
                }
                commandText += " DBGVIEW(*ALL) OUTPUT(*PRINT)";
                if (compileCommand.equals("CRTBNDC") || compileCommand.equals("CRTBNDCPP")) {
                    commandText += " OPTION(*SHOWINC)";
                }
                break;
            }
            // Compiling ILE languages to modules
            case "CRTRPGMOD":
            case "CRTCBLMOD":
            case "CRTCMOD":
            case "CRTCPPMOD": {
                commandText = compileCommand + " MODULE(" + libNamePar + "/" + objNamePar + ") ";
                if (this.ifs) {
                    commandText += "SRCSTMF('" + pathString + "') ";
                } else {
                    commandText += "SRCFILE(" + libraryName + "/" + fileName + ") SRCMBR(" + memberName + ") ";
                }
                commandText += " DBGVIEW(*ALL) OUTPUT(*PRINT)";
                if (compileCommand.equals("CRTCMOD") || compileCommand.equals("CRTCPPMOD")) {
                    commandText += " OPTION(*SHOWINC)";
                }
                break;
            }
            // SQL versions of ILE languages
            case "CRTSQLRPGI *PGM":
            case "CRTSQLRPGI *SRVPGM":
            case "CRTSQLRPGI *MODULE":
            case "CRTSQLCBLI *PGM":
            case "CRTSQLCBLI *SRVPGM":
            case "CRTSQLCBLI *MODULE":
            case "CRTSQLCI *MODULE":
            case "CRTSQLCI *PGM":
            case "CRTSQLCI *SRVPGM": {
                commandNameArray = compileCommand.split(" ");
                if (commandNameArray.length == 2) {
                    commandObjectType = commandNameArray[1];
                } else {
                    commandObjectType = "";
                }
                commandText = commandNameArray[0] + " OBJ(  " + libNamePar + "/" + objNamePar + "  ) ";
                if (this.ifs) {
                    commandText += "SRCSTMF('" + pathString + "') ";
                } else {
                    commandText += "SRCFILE(" + libraryName + "/" + fileName + ") SRCMBR(" + memberName + ") ";
                }
                commandText += " OBJTYPE(" + commandObjectType + ") " // objectType: *MODULE, *PGM, *SRVPGM
                        + " OUTPUT(*PRINT) DBGVIEW(*SOURCE)";
                break;
            }
            // C++ creates ony *MODULE
            case "CRTSQLCPPI": {
                commandText = compileCommand + " OBJ(" + libNamePar + "/" + objNamePar + ") ";
                if (this.ifs) {
                    commandText += "SRCSTMF('" + pathString + "') ";
                } else {
                    commandText += "SRCFILE(" + libraryName + "/" + fileName + ") SRCMBR(" + memberName + ") ";
                }
                commandText += " OUTPUT(*PRINT) DBGVIEW(*SOURCE)";
                break;
            }
            // SQL versions of OPM languages. No possibility to choose object type
            case "CRTSQLRPG":
            case "CRTSQLCBL": {
                if (this.ifs) {
                    // IFS not available.
                    return null;
                } else {
                    commandText = compileCommand + " PGM(" + libNamePar + "/" + objNamePar + ") "
                            + "SRCFILE(" + libraryName + "/" + fileName + ") OUTPUT(*PRINT) SRCMBR(" + memberName + ") ";
                }
                break;
            }
            // Physical file
            case "CRTPF": {
                if (this.ifs) {
                    // IFS not available.
                    return null;
                } else {
                    commandText = compileCommand + " FILE(  " + libNamePar + "/" + objNamePar + "  ) "
                            + "SRCFILE(" + libraryName + "/" + fileName + ") SRCMBR(" + memberName + ") "
                            + "FILETYPE(*DATA) MBR(*FILE) OPTION(*LIST)";
                }
                break;
            }
            // Logical file
            case "CRTLF": {
                if (this.ifs) {
                    // IFS not available.
                    return null;
                } else {
                    commandText = compileCommand + " FILE(" + libNamePar + "/" + objNamePar + ") "
                            + "SRCFILE(" + libraryName + "/" + fileName + ") "
                            + "SRCMBR(" + memberName + ") FILETYPE(*DATA) MBR(*FILE) "
                            + "DTAMBRS(*ALL) OPTION(*LIST)";
                }
                break;
            }
            // Display file
            case "CRTDSPF": {
                if (this.ifs) {
                    // IFS not available.
                    return null;
                } else {
                    commandText = compileCommand + " FILE(" + libNamePar + "/" + objNamePar + ") "
                            + "SRCFILE(" + libraryName + "/" + fileName + ") "
                            + "DEV(*REQUESTER) OPTION(*LIST) SRCMBR(" + memberName + ") ";
                }
                break;
            }
            // Printer file
            case "CRTPRTF": {
                if (this.ifs) {
                    // IFS not available.
                    return null;
                } else {
                    commandText = compileCommand + " FILE(" + libNamePar + "/" + objNamePar + ") "
                            + "SRCFILE(" + libraryName + "/" + fileName + ") "
                            + "OPTION(*LIST) SRCMBR(" + memberName + ") ";
                }
                break;
            }
            // Command
            case "CRTCMD": {
                if (this.ifs) {
                    // IFS not available.
                    return null;
                } else {
                    commandText = compileCommand + " CMD(" + libNamePar + "/" + objNamePar + ") "
                            + "PGM(*LIBL/" + memberName + ") " + "SRCFILE(" + libraryName + "/"
                            + fileName + ") SRCMBR(" + memberName + ") VLDCKR(*NONE) ";
                }
                break;
            }
            // Table
            case "CRTTBL": {
                if (this.ifs) {
                    // IFS not available.
                    return null;
                } else {
                    commandText = compileCommand + " TBL(" + libNamePar + "/" + objNamePar
                            + ") SRCFILE(" + libraryName + "/"
                            + fileName + ") SRCMBR(" + memberName + ") TBLTYPE(*CVT)";
                }
                break;
            }
            // Panel group (help)
            case "CRTPNLGRP": {
                if (this.ifs) {
                    // IFS not available.
                    return null;
                } else {
                    commandText = compileCommand + " PNLGRP(" + libNamePar + "/" + objNamePar
                            + ") SRCFILE(" + libraryName + "/"
                            + fileName + ") SRCMBR(" + memberName + ") ";
                }
                break;
            }            
            // SQL script - is performed, not compiled!
            case "RUNSQLSTM": {
                if (this.ifs) {
                    commandText = compileCommand + " SRCSTMF('" + pathString + "') "
                            + "MARGINS(200) OPTION(*LIST) OUTPUT(*PRINT) ";
                } else {
                    commandText = compileCommand + " SRCFILE(" + libraryName + "/" + fileName + ") "
                            + "SRCMBR(" + memberName + ") MARGINS(200) OPTION(*LIST) OUTPUT(*PRINT) ";
                }
                break;
            }
            default: {
            }

        } // end of switch

        return commandText;
    }

    /**
     *
     * @param compileCommandText
     */
    protected void performCommand(String compileCommandText) {

        if (compileCommandText == null) {
            return;
        }

        // First ping to server - If unsuccessful, send message and return
        AS400JPing pingObj = new AS400JPing(hostAddress);
        long timeoutMilliscconds = 8000;
        pingObj.setTimeout(timeoutMilliscconds);
        
        if (!pingObj.ping()) {
            row = "Error: Server " + hostAddress + " timed out "
                    + timeoutMilliscconds + " milliseconds.";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
            return;
        }
        
        // Create object for calling CL commands
        CommandCall cmdCall = new CommandCall(remoteServer);
        try {
            // Get current server job
            Job currentJob = cmdCall.getServerJob();

            // Get library list from the file "UserLibraryList.lib"
            String liblParameter = "";
            List<String> items = Files.readAllLines(libraryListPath);
            if (!items.isEmpty()) {
                items.get(0);
                String[] userUserLibraryList = items.get(0).split(",");
                for (int idx = 1; idx < userUserLibraryList.length; idx++) {
                    liblParameter += userUserLibraryList[idx].trim() + " ";
                }
            }
            if (liblParameter.isEmpty()) {
                liblParameter = "*NONE";
            }
            String curlibParameter = "";
            List<String> curlib = Files.readAllLines(currentLibraryPath);
            if (!curlib.isEmpty()) {
                // The only item is the current library name or *CRTDFT
                curlibParameter += curlib.get(0);
            }
            // Build command CHGLIBL
            String commandChgLiblText = "CHGLIBL LIBL(" + liblParameter + ") CURLIB(" + curlibParameter + ")";
            // Perform the GHGLIBL command
            cmdCall.run(commandChgLiblText);
            /*
            String[] libList = currentJob.getUserLibraryList();
            for (String libList1: libList) {
                System.out.println("libList " + libList1);
            }
            */
            // Set job attributes
            currentJob.setLoggingLevel(4);
            currentJob.setLoggingCLPrograms(Job.LOG_CL_PROGRAMS_YES);
            currentJob.setLoggingSeverity(0);
            currentJob.setLoggingText(Job.LOGGING_TEXT_SECLVL);
            currentJob.setInquiryMessageReply(Job.INQUIRY_MESSAGE_REPLY_DEFAULT);
            currentJob.commitChanges();
            // Get and print job attributes            
            //System.out.println("currentJob.getSubsystem() " + currentJob.getSubsystem());
            //System.out.println("currentJob.getName() " + currentJob.getName());
            //System.out.println("currentJob.getUser() " + currentJob.getUser());
            //System.out.println("currentJob.getNumber() " + currentJob.getNumber());
            //System.out.println("currentJob.getLogingText() " + currentJob.getLoggingCLPrograms());
            //System.out.println("currentJob.getLoggingCLPrograms() " + currentJob.getLoggingCLPrograms());
            //System.out.println("currentJob.getInquiryMessageReply() " + currentJob.getInquiryMessageReply());

            // ---------------------------
            // Perform the compile command
            // ---------------------------
            cmdCall.run(compileCommandText);

            // Get messages from the command if any
            AS400Message[] messagelist = cmdCall.getMessageList();
            String[] strArr = new String[messagelist.length];
            // Print all messages
            int intMsgType;
            String strMsgType = "";
            for (int idx = 0; idx < messagelist.length; idx++) {
                intMsgType = messagelist[idx].getType();
                switch (intMsgType) {
                    case AS400Message.ESCAPE -> strMsgType = "*ESCAPE";
                    case AS400Message.DIAGNOSTIC -> strMsgType = "*DIAGNOSTIC";
                    case AS400Message.COMPLETION -> strMsgType = "*COMPLETION";
                    case AS400Message.NOTIFY -> strMsgType = "*NOTIFY";
                    case AS400Message.INQUIRY -> strMsgType = "*INQUIRY";
                    case AS400Message.REPLY_SYSTEM_DEFAULT_USED -> strMsgType = "*REPLY_SYSTEM_DEFAULT_USED";
                    case AS400Message.SENDERS_COPY -> strMsgType = "*SENDERS_COPY";
                    case AS400Message.INFORMATIONAL -> strMsgType = "*INFORMATIONAL";
                    default -> {
                    }
                }
                strArr[idx] = messagelist[idx].getID() + " " + strMsgType + ": " + messagelist[idx].getText();
                row = strArr[idx] + "\n";
                msgVector.add(row);
                if (!messagelist[idx].getHelp().isEmpty()) {
                    strArr[idx] = "       " + messagelist[idx].getHelp();
                    row = strArr[idx] + "\n";
                    msgVector.add(row);
                }
            }
            reloadMessages();

        } catch (AS400SecurityException | ErrorCompletingRequestException | 
                ObjectDoesNotExistException | PropertyVetoException | IOException | InterruptedException exc) {
            exc.printStackTrace();
        }
    }

    /**
     *
     */
    protected void printJobLog() {
        try {
            // Create object for calling CL commands
            CommandCall cmdCall = new CommandCall(remoteServer);
            Job currentJob = cmdCall.getServerJob();
            String name = currentJob.getName();
            String user = currentJob.getUser();
            String number = currentJob.getNumber();
            // Build command DSPJOBLOG so that the job log is printed
            String commandDspJobLog = "DSPJOBLOG JOB( " + number + "/" + user + "/" + name 
                    + " ) OUTPUT(*PRINT) MSGF(*MSG) DUPJOBOPT(*MSG)";

            // Perform the DSPJOBLOG command
            cmdCall.run(commandDspJobLog);
        } catch (AS400SecurityException | ErrorCompletingRequestException | 
                PropertyVetoException | IOException | InterruptedException exc) {
            exc.printStackTrace();
        }
    }

    /**
     * Get list of all libraries whose names conform to the pattern defined in
     * the input field
     *
     * @param libraryPattern
     * @return
     */
    private String[] getListOfLibraries(String libraryPattern) {

        libraryField = libraryPattern;
        if (libraryField.isEmpty()) {
            libraryPattern = "*";
        }
        libraryWildCard = libraryPattern.replace("*", ".*");
        libraryWildCard = libraryWildCard.replace("?", ".");

        IFSFile ifsFile = new IFSFile(remoteServer, "/QSYS.LIB");
        if (ifsFile.getName().equals("QSYS.LIB")) {
            try {
                // Get list of selected libraries
                IFSFile[] ifsFiles = ifsFile.listFiles(libraryPattern + ".LIB");
                libraryNameVector.clear();;
                // Add the selected library names to the vector
                for (IFSFile ifsFileLevel2 : ifsFiles) {
                    String bareLibraryName = ifsFileLevel2.getName().substring(0, ifsFileLevel2.getName().indexOf("."));
                    libraryNameVector.add(bareLibraryName);
                }
                // Add "current library"
                libraryNameVector.add("*CURLIB");
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        }
        String[] libraryArray = new String[libraryNameVector.size()];
        libraryArray = libraryNameVector.toArray(libraryArray);
        return libraryArray;
    }

    /**
     * Reload messages
     */
    protected void reloadMessages() {
        scrollMessagePane.getVerticalScrollBar()
                .addAdjustmentListener(messageScrollPaneAdjustmentListenerMax);

        buildMessageList();

        scrollMessagePane.getVerticalScrollBar()
                .removeAdjustmentListener(messageScrollPaneAdjustmentListenerMax);
        this.setVisible(true);
        this.repaint();
        this.pack();
    }

    /**
     * Build message list.
     */
    private void buildMessageList() {

        // Fill message list with elements of array list
        messageList.setListData(msgVector.toArray(new String[msgVector.size()]));

        // Make the message table visible in the message scroll pane
        scrollMessagePane.setViewportView(messageList);
    }

    /**
     *
     */
    private void createGlobalPanelLayout() {

        globalPanelLayout = new GroupLayout(globalPanel);
        globalPanelLayout.setAutoCreateGaps(false);
        globalPanelLayout.setAutoCreateContainerGaps(false);

        // Set and create the global panel layout
        globalPanel.setLayout(globalPanelLayout);
        globalPanelLayout.setHorizontalGroup(globalPanelLayout.createSequentialGroup()
                .addGroup(globalPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(titlePanel)
                        .addComponent(commandSelectionPanel)
                        .addComponent(parameterPanel)
                        .addComponent(commandPanel)
                        .addComponent(buttonPanel)
                        .addComponent(scrollMessagePane)));
        globalPanelLayout.setVerticalGroup(globalPanelLayout.createSequentialGroup()
                .addGroup(globalPanelLayout.createSequentialGroup()
                        .addComponent(titlePanel)
                        .addComponent(commandSelectionPanel)
                        .addComponent(parameterPanel)
                        .addComponent(commandPanel)
                        .addComponent(buttonPanel)
                        .addComponent(scrollMessagePane)));
        globalPanel.setBorder(BorderFactory.createLineBorder(globalPanel.getBackground(), 5));
    }

    /**
     * @param windowX
     * @param windowY */
    protected void saveWindowCoordinates(int windowX, int windowY) {
        properties.setProperty("COMPILE_WINDOW_X", String.valueOf(windowX));
        properties.setProperty("COMPILE_WINDOW_Y", String.valueOf(windowY));

        // Create the updated text file in directory "paramfiles"
        PROP_COMMENT = "Copy files between IBM i and PC, edit and compile.";
        try (BufferedWriter outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding))) {
            properties.store(outfile, PROP_COMMENT);
        }
        catch (IOException exc) {
            exc.printStackTrace();
        }
    }


    /**
     *
     */
    class SourceTypeComboBoxListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent ae) {
            // Disable combo boxes because writing in them must not invoke their listeners.
            sourceTypeComboBox.removeActionListener(sourceTypeComboBoxListener);
            compileCommandsComboBox.removeActionListener(commandsComboBoxListener);
            sourceType = (String) sourceTypeComboBox.getSelectedItem();
            sourceTypeComboBox.setSelectedItem(sourceType);  // set source type to the combo box

            // if the selected string is not in the list of source file types
            if (sourceTypes.indexOf(sourceType) == -1) {  // string might be typed by the user
                sourceTypeComboBox.setSelectedItem(sourceType);  // and set it to the combo box
            }

            
            
            setCommandNames(sourceType);
            commandNames = getSourceFileAttributes(sourceType);
            compileCommandName = commandNames.get(0);
            compileCommandsComboBox.setSelectedIndex(0);

            // Build command text given compile command name (CRT...)
            commandText = buildCommand(compileCommandName, libNamePar, objNamePar);
            if (commandText == null) {
                commandTextArea.setText(compileNotSupported);
                commandTextArea.setForeground(DIM_RED);
            } else {
                commandTextArea.setForeground(DIM_BLUE);
                commandTextArea.setText(commandText);
            }

            if (sourceAttributes.equals("SAVE_SOURCE_ATTRIBUTES")) {
                updateModifiedAttributes("sourceType");
            }

            // Set result object library and object name.
            libNamePar = (String) librariesComboBox.getSelectedItem();
            objNamePar = objectNameFld.getText();

            // Enable compile commands combo box listener
            compileCommandsComboBox.addActionListener(commandsComboBoxListener);
            // Enable source type combo box listener  - sets also compileCommandsComboBox.
            sourceTypeComboBox.addActionListener(sourceTypeComboBoxListener);
        }
    }

    /**
     * Call the buildCommand() method and get the command text.
     */
    class CommandsComboBoxListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent ae) {
            // Disable combo boxes because writing in them must not invoke their listeners.
            sourceTypeComboBox.removeActionListener(sourceTypeComboBoxListener);
            compileCommandsComboBox.removeActionListener(commandsComboBoxListener);

            compileCommandName = (String) compileCommandsComboBox.getSelectedItem();
            sourceType = (String) sourceTypeComboBox.getSelectedItem();

            if (sourceAttributes.equals("SAVE_SOURCE_ATTRIBUTES")) {
                updateModifiedAttributes("commands");
            }

            compileCommandsComboBox.setSelectedItem(compileCommandName);
            // Set result object library and object name.
            libNamePar = (String) librariesComboBox.getSelectedItem();
            objNamePar = objectNameFld.getText();

            // Build command text given compile command name (CRT...)
            commandText = buildCommand(compileCommandName, libNamePar, objNamePar);
            if (commandText == null) {
                commandTextArea.setText(compileNotSupported);
                commandTextArea.setForeground(DIM_RED);
            } else {
                commandTextArea.setForeground(DIM_BLUE);
                commandTextArea.setText(commandText);
            }
            // Enable compile commands combo box listener
            compileCommandsComboBox.addActionListener(commandsComboBoxListener);
            // Enable source type combo box listener  - sets also compileCommandsComboBox.
            sourceTypeComboBox.addActionListener(sourceTypeComboBoxListener);

        }
    }

    /**
     * Call the buildCommand() method and get the command text.
     */
    class LibrariesComboBoxListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent ae) {
            // Get selected item of the libraries combo box field
            libNamePar = (String) librariesComboBox.getSelectedItem();

            if (sourceAttributes.equals("SAVE_SOURCE_ATTRIBUTES")) {
                updateModifiedAttributes("library");
            }
            // Build command text given compile command name (CRT...)
            commandText = buildCommand(compileCommandName, libNamePar, objNamePar);
            if (commandText == null) {
                commandTextArea.setText(compileNotSupported);
                commandTextArea.setForeground(DIM_RED);
            } else {
                commandTextArea.setForeground(DIM_BLUE);
                commandTextArea.setText(commandText);
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
     * Dispose of the main class window and the child window.
     */
    class WindowClosing extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent we) {
            JDialog jDialog = (JDialog) we.getSource();
            // Get current window coordinates
            int windowX = we.getWindow().getX();
            int windowY = we.getWindow().getY();
            // Save the coordinates for future display
            saveWindowCoordinates(windowX, windowY);
            // Make the window invisible
            jDialog.setVisible(false);
            //wrkSplFCall = null;  // destroy spool table
        }
    }

    /**
     * Get application parameters from "Parameters.txt" file.
     */
    private void getAppProperties() {
        // Read application parameters
        properties = new Properties();
        try (BufferedReader infile = Files.newBufferedReader(parPath, Charset.forName(encoding))) {
            properties.load(infile);
        } catch (IOException exc) {
            exc.printStackTrace();
        }

        compileWindowXString = properties.getProperty("COMPILE_WINDOW_X");
        compileWindowYString = properties.getProperty("COMPILE_WINDOW_Y");
        compileWindowX = Integer.parseInt(compileWindowXString);
        compileWindowY = Integer.parseInt(compileWindowYString);
        libraryPattern = properties.getProperty("LIBRARY_PATTERN");
        libraryPatternTextField.setText(libraryPattern);
        sourceTypePar = properties.getProperty("SOURCE_TYPE");
        properties.getProperty("IBM_CCSID");
        properties.getProperty("USERNAME");
        // Path label differs for IFS file and Source member
        titlePanel.add(pathLabel);
        if (this.ifs) {
            pathLabel.setText("Compile IFS file  " + pathString);
        } else {
            extractNamesFromIfsPath(pathString);
            pathLabel.setText("Compile source member  " + libraryName + "/" + fileName + "(" + memberName + ")");
        }
    }

    /**
     *
     * @return
     */
    protected String setDefaultSourceType() {
        // Obtain source type from IFS file or source file
        if (this.ifs) {
            // IFS file
            sourceTypePar = pathString.substring(pathString.lastIndexOf(".") + 1).toUpperCase();
            sourceTypeComboBox.setSelectedItem(sourceTypePar);
        } 
        else {
            // Source file 
            extractNamesFromIfsPath(pathString);
            if (sourceTypePar.equals("*DEFAULT")) {
                // Set source type according to the standard Source file (QRPGLESRC, ...)
//                sourceTypePar = getDefaultSourceType(fileName);
            }
            // Source type combo box - fill with source type
            sourceTypeComboBox.setSelectedItem(sourceTypePar);
        }
        return sourceTypePar;
    }

    /**
     * Set result object library and object name.
     */
    protected void getDefaultObjectNames() {
    if (this.ifs) {
            // IFS file
            try {
                // Get library name parameter from the library combo box 
                libNamePar = (String) librariesComboBox.getSelectedItem();

                // Derive object name (or member name) from the IFS path string
                String fname = pathString.substring(pathString.lastIndexOf("/") + 1);
                // Take max 10 characters
                if (fname.length() >= 10) {
                    objNamePar = fname.substring(0, 10).toUpperCase();
                } else {
                    objNamePar = fname.toUpperCase();
                }
                // Remove ending dot if any
                if (objNamePar.indexOf(".") > 0) {
                    objNamePar = objNamePar.substring(0, objNamePar.indexOf("."));
                }
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        } else {
            // Source file
            libNamePar = libraryName;
            objNamePar = memberName;

        }
        libraryPatternTextField.setText(libraryPattern);
        librariesComboBox.setSelectedItem(libNamePar);
        objectNameFld.setText(objNamePar);
    }

    /**
     * Extract individual names (libraryName, fileName, memberName) from the
     * AS400 IFS path.
     *
     * @param as400PathString
     */
    protected void extractNamesFromIfsPath(String as400PathString) {

        qsyslib = "/QSYS.LIB/";
        if (as400PathString.startsWith(qsyslib) && as400PathString.length() > qsyslib.length()) {
            libraryName = as400PathString.substring(as400PathString.indexOf("/QSYS.LIB/") + 10,
                    as400PathString.lastIndexOf(".LIB"));
            if (as400PathString.length() > qsyslib.length() + libraryName.length() + 5) {
                fileName = as400PathString.substring(qsyslib.length() + libraryName.length() + 5,
                        as400PathString.lastIndexOf(".FILE"));
                if (as400PathString.length() > qsyslib.length() + libraryName.length() + 5
                        + fileName.length() + 6) {
                    memberName = as400PathString.substring(
                            qsyslib.length() + libraryName.length() + 5 + fileName.length() + 6,
                            as400PathString.lastIndexOf("."));
                }
            }
        }
    }
    
            
    /**
     * Get source type of the source file member
     * 
     * @param member
     * @return 
     */
    protected String getMemberType(IFSFile member) {
        MemberDescription​ memberDescription = new MemberDescription​(remoteServer, member.toString());   
        String srcType = "";
        try {
            srcType = (String) memberDescription.getValue(MemberDescription​.SOURCE_TYPE);            
        } catch (AS400SecurityException | ErrorCompletingRequestException | 
                ObjectDoesNotExistException | IOException | InterruptedException exc) {
            exc.printStackTrace();
        }
        return srcType;
    }

}
