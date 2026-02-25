package copyfiles;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400JPing;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.PrintParameterList;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.SpooledFile;
import com.ibm.as400.access.SpooledFileList;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * Create window and table for spooled files, work with spooled files.
 *
 * @author Vladimír Župka, 2016
 */
public class WrkSplF extends JDialog {

    int windowWidth = 775;
    int windowHeight = 600;

    AS400 remoteServer;
    MainWindow mainWindow;
    String rightPathString;
    boolean currentUser;

    Path parPath = Paths.get(System.getProperty("user.dir"), "paramfiles", "Parameters.txt");
    String encoding = System.getProperty("file.encoding", "UTF-8");
    Path spoolTextPath = Paths.get(System.getProperty("user.dir"), "workfiles", "SpooledFile.txt");

    Properties properties;
    String pcCharset;
    String ibmCcsid;
    int ibmCcsidInt;

    Container cont;
    JPanel globalPanel;
    JPanel panelTop;
    GroupLayout panelTopLayout;
    JScrollPane scrollPane;
    GroupLayout globalPanelLayout;

    JTable spoolTable;
    SpoolTableModel spoolTableModel;
    Object[][] spoolRows;
    int nbrOfRows;
    int nbrOfCols = 9;
    String[] columnNames = new String[nbrOfCols];

    // Model for selection of rows in the table
    ListSelectionModel rowSelectionModel;
    int leadIndex;
    int minIndex;
    int maxIndex;
    ArrayList<Integer> selIndexArrList = new ArrayList<>();

    // Index of a table row selected (by the user or the program)
    int rowIndex;

    JLabel nameLabel = new JLabel("File name");
    JTextField nameTextField = new JTextField();

    JLabel numberLabel = new JLabel("File num.");
    JTextField numberTextField = new JTextField();

    JLabel pagesLabel = new JLabel("Pages");
    JTextField pagesTextField = new JTextField();

    JLabel jobLabel = new JLabel("Job name");
    JTextField jobTextField = new JTextField();

    JLabel userLabel = new JLabel("User");

    JComboBox<String> userComboBox = new JComboBox<>();
    ArrayList<String> spoolUsers = new ArrayList<>();

    JLabel jobNumberLabel = new JLabel("Job num.");
    JTextField jobNumberTextField = new JTextField();

    JLabel dateLabel = new JLabel("Date");
    JTextField dateTextField = new JTextField();

    JLabel timeLabel = new JLabel("Time");
    JTextField timeTextField = new JTextField();

    JButton refreshButton = new JButton("Refresh");

    // Array lists for creating the spooled file table
    ArrayList<String> nameArrList = new ArrayList<>();
    ArrayList<String> numberArrList = new ArrayList<>();
    ArrayList<String> pagesArrList = new ArrayList<>();
    ArrayList<String> jobArrList = new ArrayList<>();
    ArrayList<String> userArrList = new ArrayList<>();
    ArrayList<String> jobNumberArrList = new ArrayList<>();
    ArrayList<String> dateArrList = new ArrayList<>();
    ArrayList<String> timeArrList = new ArrayList<>();
    ArrayList<String> queueArrList = new ArrayList<>();
    ArrayList<SpooledFile> splfArrList = new ArrayList<>();

    // Alternative array lists for refreshing the spooled file table
    ArrayList<String> nameArrListWork = new ArrayList<>();
    ArrayList<String> numberArrListWork = new ArrayList<>();
    ArrayList<String> pagesArrListWork = new ArrayList<>();
    ArrayList<String> jobArrListWork = new ArrayList<>();
    ArrayList<String> userArrListWork = new ArrayList<>();
    ArrayList<String> jobNumberArrListWork = new ArrayList<>();
    ArrayList<String> dateArrListWork = new ArrayList<>();
    ArrayList<String> timeArrListWork = new ArrayList<>();
    ArrayList<String> queueArrListWork = new ArrayList<>();
    ArrayList<SpooledFile> splfArrListWork = new ArrayList<>();

    MouseListener spoolTableMouseListener;
    ScrollPaneAdjustmentListenerMax scrollPaneAdjustmentListenerMax;

    ListSelectionModel spoolTableSlectionlModel;

    JTextArea spoolTextArea;

    SpooledFile splf;
    AS400JPing pingObject;
    boolean ping_PRINT;

    String namePar;
    String numberPar;
    String pagesPar;
    String jobPar;
    String userPar;
    String jobNumberPar;
    String datePar;
    String timePar;
    String queuePar;

    JMenuItem displaySpooledFiles = new JMenuItem("Display");
    JMenuItem copySpooledFile = new JMenuItem("Copy");
    JMenuItem deleteSpooledFile = new JMenuItem("Delete");
    JPopupMenu spoolPopupMenu = new JPopupMenu();

    boolean leftButtonClicked = false;
    String row;
    boolean nodes = true;
    boolean noNodes = false;

    int compileWindowX;
    int compileWindowY;
    String callingClassName;

    /**
     * Constructor
     *
     * @param remoteServer
     * @param mainWindow
     * @param rightPathString
     */
    WrkSplF(AS400 remoteServer, MainWindow mainWindow, String rightPathString,
            boolean currentUser, int compileWindowX, int compileWindowY, String callingClassName) {
        this.remoteServer = remoteServer;
        this.mainWindow = mainWindow;
        this.rightPathString = rightPathString;
        this.currentUser = currentUser;
        this.compileWindowX = compileWindowX;
        this.compileWindowY = compileWindowY;
        this.callingClassName = callingClassName;
        super.setModalityType(Dialog.ModalityType.MODELESS);

        properties = new Properties();
        try {
            BufferedReader infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
            properties.load(infile);
            infile.close();
        } catch (IOException exc) {
            exc.printStackTrace();
        }
        userPar = properties.getProperty("USERNAME");

        pcCharset = properties.getProperty("PC_CHARSET");
        if (!pcCharset.equals("*DEFAULT")) {
            // Check if charset is valid
            try {
                Charset.forName(pcCharset);
            } catch (IllegalCharsetNameException
                    | UnsupportedCharsetException charset) {
                // If pcCharset is invalid, take UTF-8
                pcCharset = "UTF-8";
            }
        } else {
            // *DEFAULT for spooled file is UTF-8
            pcCharset = "UTF-8";
        }

        ibmCcsid = properties.getProperty("IBM_CCSID");
        if (!ibmCcsid.equals("*DEFAULT")) {
            try {
                ibmCcsidInt = Integer.parseInt(ibmCcsid);
            } catch (Exception exc) {
                // If ibmCcsid is not numeric, take 65535
                exc.printStackTrace();
                ibmCcsid = "65535";
                ibmCcsidInt = 65535;
            }
        } else {
            // *DEFAULT for spooled file is 65535
            ibmCcsid = "65535";
            ibmCcsidInt = 65535;
        }
                // Register window listener
        // ------------------------
        SpoolWindowAdapter spoolWindowAdapter = new SpoolWindowAdapter();
        this.addWindowListener(spoolWindowAdapter);

    }

    /**
     * Create window with table of spooled files.
     * @param currentUser
     * @return 
     */
    public JDialog createSpoolWindow(boolean currentUser) {

        if (currentUser) {
            userPar = properties.getProperty("USERNAME");
        } else {
            userPar = "";
        }
        
        // Select all spooled files for the user
        splf = selectSpooledFiles("", "", "", "", userPar, "", "", "");
        if (splf == null) {
            row = "Error: Spooled file list cannot be obtained. It may be empty.";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(noNodes); // do not add child nodes
            mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
            return this;
        }

        panelTop = new JPanel();
        panelTop.setPreferredSize(new Dimension(windowWidth, 60));
        panelTop.setMinimumSize(new Dimension(windowWidth, 60));
        panelTop.setMaximumSize(new Dimension(windowWidth, 60));
        globalPanel = new JPanel();

        nameTextField.setPreferredSize(new Dimension(90, 20));
        nameTextField.setMinimumSize(new Dimension(90, 20));
        nameTextField.setMaximumSize(new Dimension(90, 20));

        numberTextField.setPreferredSize(new Dimension(60, 20));
        numberTextField.setMinimumSize(new Dimension(60, 20));
        numberTextField.setMaximumSize(new Dimension(60, 20));

        pagesTextField.setPreferredSize(new Dimension(60, 20));
        pagesTextField.setMinimumSize(new Dimension(60, 20));
        pagesTextField.setMaximumSize(new Dimension(60, 20));

        jobTextField.setPreferredSize(new Dimension(90, 20));
        jobTextField.setMinimumSize(new Dimension(90, 20));
        jobTextField.setMaximumSize(new Dimension(90, 20));

        // Create and fill combo box with unique spooled files user names
        userComboBox = new JComboBox(spoolUsers.toArray());
        userComboBox.setPreferredSize(new Dimension(90, 20));
        userComboBox.setMinimumSize(new Dimension(90, 20));
        userComboBox.setMaximumSize(new Dimension(90, 20));
        userComboBox.setEditable(true);
        // Insert the first user name into the combo box field to display its spool files
        userComboBox.setSelectedItem("");

        jobNumberTextField.setPreferredSize(new Dimension(60, 20));
        jobNumberTextField.setMinimumSize(new Dimension(60, 20));
        jobNumberTextField.setMaximumSize(new Dimension(60, 20));

        dateTextField.setPreferredSize(new Dimension(70, 20));
        dateTextField.setMinimumSize(new Dimension(70, 20));
        dateTextField.setMaximumSize(new Dimension(70, 20));

        timeTextField.setPreferredSize(new Dimension(60, 20));
        timeTextField.setMinimumSize(new Dimension(60, 20));
        timeTextField.setMaximumSize(new Dimension(60, 20));

        // Lay out components in panelTop
        panelTopLayout = new GroupLayout(panelTop);
        panelTopLayout.setAutoCreateGaps(true);
        panelTopLayout.setAutoCreateContainerGaps(false);

        GroupLayout.ParallelGroup pg1 = panelTopLayout.createParallelGroup(Alignment.CENTER);
        pg1.addComponent(timeLabel);
        if (callingClassName.equals("Compile")) {
            pg1.addComponent(refreshButton);
        }

        GroupLayout.ParallelGroup pg2 = panelTopLayout.createParallelGroup(Alignment.CENTER);
        pg2.addComponent(nameTextField);
        pg2.addComponent(numberTextField);
        pg2.addComponent(pagesTextField);
        pg2.addComponent(jobTextField);
        pg2.addComponent(userComboBox);
        pg2.addComponent(jobNumberTextField);
        pg2.addComponent(dateTextField);
        pg2.addComponent(timeTextField);
        if (callingClassName.equals("Compile")) {
            pg2.addComponent(refreshButton);
        }

        panelTopLayout.setHorizontalGroup(panelTopLayout.createSequentialGroup()
                .addGroup(panelTopLayout.createParallelGroup()
                        .addComponent(nameLabel)
                        .addComponent(nameTextField))
                .addGroup(panelTopLayout.createParallelGroup()
                        .addComponent(numberLabel)
                        .addComponent(numberTextField))
                .addGroup(panelTopLayout.createParallelGroup()
                        .addComponent(pagesLabel)
                        .addComponent(pagesTextField))
                .addGroup(panelTopLayout.createParallelGroup()
                        .addComponent(jobLabel)
                        .addComponent(jobTextField))
                .addGroup(panelTopLayout.createParallelGroup()
                        .addComponent(userLabel)
                        .addComponent(userComboBox))
                .addGroup(panelTopLayout.createParallelGroup()
                        .addComponent(jobNumberLabel)
                        .addComponent(jobNumberTextField))
                .addGroup(panelTopLayout.createParallelGroup()
                        .addComponent(dateLabel)
                        .addComponent(dateTextField))
                .addGroup(panelTopLayout.createParallelGroup()
                        .addComponent(timeLabel)
                        .addComponent(timeTextField))
                .addGroup(pg1));

        panelTopLayout.setVerticalGroup(panelTopLayout.createSequentialGroup()
                .addGroup(panelTopLayout.createParallelGroup(Alignment.CENTER)
                        .addComponent(nameLabel)
                        .addComponent(numberLabel)
                        .addComponent(pagesLabel)
                        .addComponent(jobLabel)
                        .addComponent(userLabel)
                        .addComponent(jobNumberLabel)
                        .addComponent(dateLabel)
                        .addComponent(timeLabel)
                        .addComponent(timeLabel))
                .addGroup(pg2));

        panelTop.setLayout(panelTopLayout);

        globalPanel = new JPanel();

        globalPanelLayout = new GroupLayout(globalPanel);
        globalPanelLayout.setAutoCreateGaps(true);
        globalPanelLayout.setAutoCreateContainerGaps(true);

        // Create spool table.
        // ===================
        spoolTable = createSpoolTable();

        // Mouse listener reacts on the row number selected by mouse click or double click
        spoolTableMouseListener = new SpoolTableMouseAdapter();
        // Register mouse listener to the table
        spoolTable.addMouseListener(spoolTableMouseListener);

        // Make the table visible in the scroll pane
        scrollPane = new JScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setViewportView(spoolTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        globalPanelLayout.setAutoCreateGaps(false);
        globalPanelLayout.setAutoCreateContainerGaps(false);
        globalPanelLayout.setHorizontalGroup(globalPanelLayout.createSequentialGroup()
                .addGroup(globalPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(panelTop)
                        .addComponent(scrollPane)));
        globalPanelLayout.setVerticalGroup(globalPanelLayout.createSequentialGroup()
                .addComponent(panelTop)
                .addComponent(scrollPane));

        globalPanel.setLayout(globalPanelLayout);
        globalPanel.setBorder(BorderFactory.createLineBorder(globalPanel.getBackground(), 10));

        scrollPaneAdjustmentListenerMax = new ScrollPaneAdjustmentListenerMax();
        scrollPane.getVerticalScrollBar().addAdjustmentListener(scrollPaneAdjustmentListenerMax);

        cont = getContentPane();
        cont.add(globalPanel);

        //
        // Make the window visible
        // -----------------------
        setSize(windowWidth, windowHeight);
        setLocation(compileWindowX + 350, compileWindowY + 300);
        setVisible(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pack();
        scrollPane.getVerticalScrollBar().removeAdjustmentListener(scrollPaneAdjustmentListenerMax);

        //
        // Input fields action listeners
        // -----------------------------
        nameTextField.addActionListener(ae -> {
            namePar = nameTextField.getText().toUpperCase();
            nameTextField.setText(namePar);
            refreshSpoolTable(userPar);
            repaint();
        });
        numberTextField.addActionListener(ae -> {
            numberPar = numberTextField.getText().toUpperCase();
            numberTextField.setText(numberPar);
            refreshSpoolTable(userPar);
            repaint();
        });
        pagesTextField.addActionListener(ae -> {
            pagesPar = pagesTextField.getText().toUpperCase();
            pagesTextField.setText(pagesPar);
            refreshSpoolTable(userPar);
            repaint();
        });
        jobTextField.addActionListener(ae -> {
            jobPar = jobTextField.getText().toUpperCase();
            jobTextField.setText(jobPar);
            refreshSpoolTable(userPar);
            repaint();
        });
        //
        // Spool user combo box
        // --------------------
        // Select user name from the list in combo box - item listener
        userComboBox.addItemListener(il -> {
            repaint();
            JComboBox source = (JComboBox) il.getSource();
            // Selected item becomes userPar parameter for later spool file selection
            userPar = ((String) source.getSelectedItem()).toUpperCase();
            // Select all spooled files for THIS USER with all other parameters empty
            // refreshSpoolTable("", "", "", userPar, "", "", "");
            refreshSpoolTable(userPar);
        });
        // Select user name from the list in combo box - action listener
        userComboBox.addActionListener(al -> {
            repaint();
            JComboBox source = (JComboBox) al.getSource();
            // Selected item becomes userPar parameter for later spool file selection
            userPar = ((String) source.getSelectedItem()).toUpperCase();
            // Select all spooled files for THIS USER with all other parameters empty
            // refreshSpoolTable("", "", "", userPar, "", "", "");
            refreshSpoolTable(userPar);
            repaint();
        });
        //
        // Text fields listeners
        // ---------------------
        jobNumberTextField.addActionListener(ae -> {
            jobNumberPar = jobNumberTextField.getText().toUpperCase();
            jobNumberTextField.setText(jobNumberPar);
            refreshSpoolTable(userPar);
            repaint();
        });
        dateTextField.addActionListener(ae -> {
            datePar = dateTextField.getText().toUpperCase();
            dateTextField.setText(datePar);
            refreshSpoolTable(userPar);
            repaint();
        });
        timeTextField.addActionListener(ae -> {
            timePar = timeTextField.getText().toUpperCase();
            timeTextField.setText(timePar);
            refreshSpoolTable(userPar);
            repaint();
        });
        //
        // Refresh button listener
        // -----------------------
        refreshButton.addActionListener(ae -> {
            scrollPane.getVerticalScrollBar().addAdjustmentListener(scrollPaneAdjustmentListenerMax);
            userPar = properties.getProperty("USERNAME");
            // Select spooled files again
            splf = selectSpooledFiles(namePar, numberPar, pagesPar, jobPar, userPar, jobNumberPar, datePar, timePar);
            if (splf == null) {
                row = "Error: Spooled file list cannot be obtained. It may be empty.";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages(noNodes); // do not add child nodes
                mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
            }
            // Create a new spool table
            spoolTable = createSpoolTable();
            // Refresh the spool table in the window for the current user name
            refreshSpoolTable(userPar);

            scrollPane.getVerticalScrollBar().removeAdjustmentListener(scrollPaneAdjustmentListenerMax);

            // Register mouse listener to the new table
            spoolTable.addMouseListener(spoolTableMouseListener);
        });


        // Enable ESCAPE key to escape from editing
        // ----------------------------------------
        globalPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "escape");
        globalPanel.getActionMap().put("escape", new Escape());

        // Display spooled files menu item (one or more rows can be selected)
        // -------------------------------
        displaySpooledFiles.addActionListener((ActionEvent ae) -> {
            // Read input stream and convert spooled file into text
            displaySpooledFiles();
        });

        // Copy spooled file menu item
        // -----------------
        copySpooledFile.addActionListener(ae -> {
            // Convert single spooled file to text area
            convertSpooledFileCall(leadIndex);
        });

        // Delete spooled file menu item
        // -------------------
        deleteSpooledFile.addActionListener(ae -> {
            deleteSpooledFile();
        });

        // Completing message will be sent only when retrieving ALL spooled files
        if (rightPathString.equals("/QSYS.LIB")) {
            row = "Comp: Retrieving spooled files completed.";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(noNodes);
            mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
        }
        return this;  // return object of the window
    }

    /**
     * Create table with information about spooled files; each spooled file in a table row.
     *
     * @return
     */
    protected JTable createSpoolTable() {
        spoolTableModel = new SpoolTableModel();

        spoolTable = new JTable(spoolTableModel);
        spoolTable.setGridColor(Color.WHITE);
        spoolTable.setRowHeight(20);
        spoolTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Row selection model (selection of single row)
        spoolTableSlectionlModel = spoolTable.getSelectionModel();
        spoolTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Row selection model registration
        spoolTableSlectionlModel.addListSelectionListener(sl -> {
            rowSelectionModel = (ListSelectionModel) sl.getSource();
            leadIndex = 0;
            // If some rows are selected:
            if (!rowSelectionModel.isSelectionEmpty()) {
                leadIndex = rowSelectionModel.getLeadSelectionIndex();
                // Find out which indexes are selected.
                minIndex = rowSelectionModel.getMinSelectionIndex();
                maxIndex = rowSelectionModel.getMaxSelectionIndex();
                // Write indexes of selected table rows into array list
                // for later use (delete spooled files)
                selIndexArrList = new ArrayList<>();
                for (int idx = minIndex; idx <= maxIndex; idx++) {
                    if (rowSelectionModel.isSelectedIndex(idx)) {
                        selIndexArrList.add(idx);
                    }
                }
            } else { //
                // If no row is selected
                leadIndex = -1;
            }
        });

        // Column model for column rendering and editing
        TableColumnModel tcm = spoolTable.getColumnModel();
        // Table column array for rendering rows
        TableColumn[] tc;
        tc = new TableColumn[nbrOfCols];
        for (int col = 0; col < nbrOfCols; col++) {
            // Get table column object from the model
            tc[col] = tcm.getColumn(col);
        }
        tc[0].setPreferredWidth(100);
        tc[1].setPreferredWidth(70);
        tc[2].setPreferredWidth(60);
        tc[3].setPreferredWidth(100);
        tc[4].setPreferredWidth(95);
        tc[5].setPreferredWidth(65);
        tc[6].setPreferredWidth(75);
        tc[7].setPreferredWidth(60);
        tc[8].setPreferredWidth(135);
        tc[0].setHeaderValue("File name");
        tc[1].setHeaderValue("File num.");
        tc[2].setHeaderValue("Pages");
        tc[3].setHeaderValue("Job name");
        tc[4].setHeaderValue("User");
        tc[5].setHeaderValue("Job num");
        tc[6].setHeaderValue("Date");
        tc[7].setHeaderValue("Time");
        tc[8].setHeaderValue("Output queue");

        // Create the table array with data (number of rows is now known from the preceding loop)
        spoolRows = new Object[nbrOfRows][nbrOfCols];
        // Fill the table array with data from array lists
        for (int row = 0; row < nbrOfRows; row++) {
            spoolRows[row][0] = nameArrList.get(row);
            spoolRows[row][1] = numberArrList.get(row);
            spoolRows[row][2] = pagesArrList.get(row);
            spoolRows[row][3] = jobArrList.get(row);
            spoolRows[row][4] = userArrList.get(row);
            spoolRows[row][5] = jobNumberArrList.get(row);
            spoolRows[row][6] = dateArrList.get(row);
            spoolRows[row][7] = timeArrList.get(row);
            spoolRows[row][8] = queueArrList.get(row);
        }
        return spoolTable;
    }

    /**
     * Refresh spool table in scroll pane according to selection conditions entered by the user in text fields.
     *
     * @param userPar
     */
    protected void refreshSpoolTable(String userPar) {

        // Get actual values from input fields
        // except for the User which is passed as a call parameter
        namePar = nameTextField.getText().toUpperCase();
        numberPar = numberTextField.getText().toUpperCase();
        pagesPar = pagesTextField.getText().toUpperCase();
        jobPar = jobTextField.getText().toUpperCase();
        jobNumberPar = jobNumberTextField.getText().toUpperCase();
        datePar = dateTextField.getText().toUpperCase();
        timePar = timeTextField.getText().toUpperCase();

        // Clear array lists
        nameArrListWork = new ArrayList<>();
        numberArrListWork = new ArrayList<>();
        pagesArrListWork = new ArrayList<>();
        jobArrListWork = new ArrayList<>();
        userArrListWork = new ArrayList<>();
        jobNumberArrListWork = new ArrayList<>();
        dateArrListWork = new ArrayList<>();
        timeArrListWork = new ArrayList<>();
        queueArrListWork = new ArrayList<>();
        splfArrListWork = new ArrayList<>();

        // Find entries in the array lists which satisfy parameter conditions
        nbrOfRows = 0;
        for (int idx = 0; idx < nameArrList.size(); idx++) {
            if (nameArrList.get(idx).toUpperCase().contains(namePar.toUpperCase())
                    && numberArrList.get(idx).toUpperCase().contains(numberPar.toUpperCase())
                    && pagesArrList.get(idx).toUpperCase().contains(pagesPar.toUpperCase())
                    && jobArrList.get(idx).toUpperCase().contains(jobPar.toUpperCase())
                    && userArrList.get(idx).toUpperCase().contains(userPar.toUpperCase())
                    && jobNumberArrList.get(idx).toUpperCase().contains(jobNumberPar.toUpperCase())
                    && dateArrList.get(idx).toUpperCase().contains(datePar.toUpperCase())
                    && timeArrList.get(idx).toUpperCase().contains(timePar.toUpperCase())) {
                nameArrListWork.add(nameArrList.get(idx));
                numberArrListWork.add(numberArrList.get(idx));
                pagesArrListWork.add(pagesArrList.get(idx));
                jobArrListWork.add(jobArrList.get(idx));
                userArrListWork.add(userArrList.get(idx));
                jobNumberArrListWork.add(jobNumberArrList.get(idx));
                dateArrListWork.add(dateArrList.get(idx));
                timeArrListWork.add(timeArrList.get(idx));
                queueArrListWork.add(queueArrList.get(idx));
                splfArrListWork.add(splfArrList.get(idx));
                nbrOfRows++;
            }
        }
        // Create the table array with data (number of rows is now known from the preceding loop)
        spoolRows = new Object[nbrOfRows][nbrOfCols];
        // Fill the table array with data from work array lists
        for (int row = 0; row < nbrOfRows; row++) {
            spoolRows[row][0] = nameArrListWork.get(row);
            spoolRows[row][1] = numberArrListWork.get(row);
            spoolRows[row][2] = pagesArrListWork.get(row);
            spoolRows[row][3] = jobArrListWork.get(row);
            spoolRows[row][4] = userArrListWork.get(row);
            spoolRows[row][5] = jobNumberArrListWork.get(row);
            spoolRows[row][6] = dateArrListWork.get(row);
            spoolRows[row][7] = timeArrListWork.get(row);
            spoolRows[row][8] = queueArrListWork.get(row);
        }
        // Important! when number of rows changes.
        scrollPane.setViewportView(spoolTable);
    }

    /**
     * Select spooled files according to path string from the node selected from the right tree and to criteria given in
     * parameters;
     * The path string is a file of type .OUTQ or library QSYS.LIB; Information from the spooled files is written to array lists.
     *
     * @param namePar
     * @param numberPar
     * @param pagesPar
     * @param jobPar
     * @param userPar
     * @param jobNumberPar
     * @param datePar
     * @param timePar
     * @return
     */
    protected SpooledFile selectSpooledFiles(String namePar, String numberPar, String pagesPar, String jobPar,
            String userPar, String jobNumberPar, String datePar, String timePar) {
        this.namePar = namePar;
        this.numberPar = numberPar;
        this.pagesPar = pagesPar;
        this.jobPar = jobPar;
        this.userPar = userPar;
        this.jobNumberPar = jobNumberPar;
        this.datePar = datePar;
        this.timePar = timePar;

        nbrOfRows = 0;

        // Check connection to PRINT service before creating a spooled file list.
        // ======================================================================
        pingObject = new AS400JPing(properties.getProperty("HOST"));
        //pingObject.setTimeout(1000);
        ping_PRINT = pingObject.ping(AS400.PRINT);
        boolean isPingOk = ping_PRINT;
        while (!isPingOk) {
            row = "Error: Ping to server  " + properties.getProperty("HOST") + "  failed. Reconnecting PRINT service.";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(noNodes);
            try {
                ping_PRINT = pingObject.ping(AS400.PRINT);
                isPingOk = ping_PRINT;
                System.out.println("pingOk: " + isPingOk);
                Thread.sleep(2000);
            } catch (Exception exc) {
                row = "Error: Ping: " + exc.toString();
                System.out.println("Error: Ping: " + exc.toString());
                mainWindow.msgVector.add(row);
                mainWindow.showMessages(noNodes);
                exc.printStackTrace();
            }
        }
        try {
            remoteServer.connectService(AS400.PRINT);
        } catch (Exception exc) {
            row = "Error: Getting connection to PRINT service: " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(noNodes);
            exc.printStackTrace();
        }

        // Create object representing spooled files
        SpooledFileList splfList = new SpooledFileList(remoteServer);
        try {
            // Parameter for selection all users for the first time or a specific user if not empty
            if (rightPathString.equals("/") || rightPathString.equals("/QSYS.LIB")) {
                row = "Wait: Retrieving list of all spooled files . . .";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages(noNodes);
                mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
                splfList.setUserFilter("*ALL");
                splfList.setQueueFilter("/QSYS.LIB/%ALL%.LIB/%ALL%.OUTQ");
                // Remove message scroll listener (cancel scrolling to the last message)
                mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
            } else if (currentUser) {
                // Spooled files for current user will be selected
                splfList.setUserFilter("*CURRENT");
            } else {
                // Spooled files will be selected only from the output queue /QSYS.LIB/xxx.LIB/yyy.OUTQ
                // (right path string from the right file tree)
                splfList.setUserFilter("*ALL");
                splfList.setQueueFilter(rightPathString);
            }

            // Selection of spooled files is asynchronous
            splfList.openAsynchronously();
            // Wait for completion
            splfList.waitForListToComplete();

            // Get list of all spooled files
            Enumeration<SpooledFile> spooledFiles = splfList.getObjects();

            // Clear array lists containing characteristics of spooled files
            // that will be placed in table columns
            nameArrList = new ArrayList<>();
            numberArrList = new ArrayList<>();
            pagesArrList = new ArrayList<>();
            jobArrList = new ArrayList<>();
            userArrList = new ArrayList<>();
            spoolUsers = new ArrayList<>();
            jobNumberArrList = new ArrayList<>();
            dateArrList = new ArrayList<>();
            timeArrList = new ArrayList<>();
            queueArrList = new ArrayList<>();
            splfArrList = new ArrayList<>();

            // Select spooled files according to selection parameters
            while (spooledFiles.hasMoreElements()) {
                // Get next spooled file
                splf = (SpooledFile) spooledFiles.nextElement();
                if (splf != null) {

                    String splfFileNumberChar = String.valueOf(splf.getNumber());
                    String splfFilePagesChar = String.valueOf(splf.getIntegerAttribute(SpooledFile.ATTR_PAGES));

                    // If selection parameters are satisfied, select the spooled file characteristics
                    // to the array lists for the table
                    if (splf.getName().contains(namePar)
                            && splfFileNumberChar.contains(numberPar)
                            && splfFilePagesChar.contains(pagesPar)
                            && splf.getJobName().contains(jobPar)
                            && splf.getJobUser().contains(userPar)
                            && splf.getJobNumber().contains(jobNumberPar)
                            && splf.getCreateDate().contains(datePar)
                            && splf.getCreateTime().contains(timePar)) {
                        //System.out.println("namePar 2: " + namePar);
                        //System.out.println("splf.getName() 2: " + splf.getName());
                        //System.out.print("File name selectSpoolFiles2: " + splf.getName());
                        //System.out.print(" \tFile number: " + splfFileNumberChar);
                        //System.out.print(" \tJob name: " + splf.getJobName());
                        //System.out.print(" \tUser name: " + splf.getJobUser());
                        //System.out.print(" \tJob number: " + splf.getJobNumber());
                        //System.out.print(" \tDate: " + splf.getCreateDate());
                        //System.out.print(" \tTime: " + splf.getCreateTime());
                        //System.out.println();
                        nameArrList.add(splf.getName());
                        numberArrList.add(splfFileNumberChar);
                        pagesArrList.add(splfFilePagesChar);
                        jobArrList.add(splf.getJobName());
                        userArrList.add(splf.getJobUser());
                        spoolUsers.add(splf.getJobUser());
                        jobNumberArrList.add(splf.getJobNumber());
                        dateArrList.add(splf.getCreateDate());
                        timeArrList.add(splf.getCreateTime());
                        // Qualified name of output queue, e.g. QGPL/QPRINT
                        queueArrList.add(getQualifiedQueueNameFromIfsPath(splf.getStringAttribute(SpooledFile.ATTR_OUTPUT_QUEUE)));
                        splfArrList.add(splf);

                        nameArrListWork.add(splf.getName());
                        numberArrListWork.add(splfFileNumberChar);
                        pagesArrListWork.add(splfFilePagesChar);
                        jobArrListWork.add(splf.getJobName());
                        userArrListWork.add(splf.getJobUser());
                        jobNumberArrListWork.add(splf.getJobNumber());
                        dateArrListWork.add(splf.getCreateDate());
                        timeArrListWork.add(splf.getCreateTime());
                        // Qualified name of output queue, e.g. QGPL/QPRINT
                        queueArrListWork.add(getQualifiedQueueNameFromIfsPath(splf.getStringAttribute(SpooledFile.ATTR_OUTPUT_QUEUE)));
                        splfArrListWork.add(splf);

                        nbrOfRows++;
                    }
                }
            }
            // Ensure uniqueness of user names in array list (over linked hash set)
            Set<String> linkedHashSet = new LinkedHashSet<>();
            linkedHashSet.addAll(spoolUsers);
            spoolUsers.clear();
            spoolUsers.addAll(linkedHashSet);

            // System.out.println("Name FFF: " + this.splf.getName());
            // System.out.println("nbrOfRows: " + nbrOfRows);
            // Last spooled file selected
            return splf;

        } catch (Exception exc) {
            System.out.println("Error: " + exc.toString());
            exc.printStackTrace();
            return null;
        }
    }

    /**
     * Read input stream and convert byte array buffers to text file (SpooledFile.txt) and to text area (spoolTextArea).
     *
     * @param splf
     * @return
     */
    @SuppressWarnings("ConvertToTryWithResources")
    protected String convertSpooledFile(SpooledFile splf) {
        /*
        String splfFileNumberChar = String.valueOf(splf.getNumber());
        System.out.print("File name convertSpooledfile: "
                + splf.getName());
        System.out.print(" \tFile number: " + splfFileNumberChar);
        System.out.print(" \tJob name: "
                + splf.getJobName());
        System.out.print(" \tUser name: " + splf.getJobUser());
        System.out.print(" \tJob number: " + splf.getJobNumber());
        System.out.print(" \tDate: "
                + splf.getCreateDate());
        System.out.print(" \tTime: " + splf.getCreateTime());
        System.out.println();
        */
        
        if (splf == null) {
            return null;
        }
        try {
            Integer numberParInt = splf.getNumber();

            PrintParameterList printParameterList = new PrintParameterList();
            printParameterList.setParameter(SpooledFile.ATTR_SPOOLFILE, namePar);
            printParameterList.setParameter(SpooledFile.ATTR_SPLFNUM, numberParInt);
            printParameterList.setParameter(SpooledFile.ATTR_JOBNAME, jobPar);
            printParameterList.setParameter(SpooledFile.ATTR_JOBUSER, userPar);
            printParameterList.setParameter(SpooledFile.ATTR_JOBNUMBER, jobNumberPar);
            printParameterList.setParameter(SpooledFile.ATTR_DATE, datePar);
            printParameterList.setParameter(SpooledFile.ATTR_TIME, timePar);
            InputStream inputStream = splf.getInputStream(printParameterList);

            // Open output text file
            BufferedWriter outfileText = Files.newBufferedWriter(spoolTextPath, Charset.forName(pcCharset), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

            // Create empty text area for converted lines
            spoolTextArea = new JTextArea();

            byte[] workBuffer = new byte[2000000];

            byte[] inputBuffer = new byte[20000];

            final int SINGLE_CHARACTERS = 10;
            final int CLASS = 15;

            final int COMMAND_CLASSES = 20;
            final int PRESENTATION_POSITION = 30;
            final int TRANSPARENT = 40;
            final int VERTICAL_CHANNEL_SELECT = 50;
            final int RVPP = 80;
            final int AHPP = 90;
            final int AVPP = 100;
            final int RHPP = 110;
            final int NBR_TRANSP_BYTES = 120;
            final int VERTICAL_CHANNEL = 130;
            final int CLASS_COUNTDOWN = 180;

            int endPointer = 0;

            int classCount = 0;
            int machineState = SINGLE_CHARACTERS;

            int bytesRead = inputStream.read(inputBuffer);

            int currentPosInLine = 0;
            int resumePosInLine = 0; // End position in line for continuation when replacing mode ends
            boolean isReplacingPrecedingBytes = false;

            while (bytesRead != -1) {

                for (int idx = 0; idx < bytesRead; idx++) {

                    byte byteRead = inputBuffer[idx];
                    // --------------------------------------------------------------------------------------
                    // This state machine interprets print control characters and sequences read from input
                    // and writes the resulted bytes in a work buffer.
                    //
                    // NOTE:
                    // =====
                    //
                    // The relatively straightforward algorithm is complicated by necessity to resolve
                    // the situation when Absolute Presentation Position is less than current position of the byte just
                    // written.
                    // This is resolved by introducing a "replacing mode" that is set in the machine state
                    // AHPP (Absolue Horizontal Presentation Position)
                    // and tested in the machine state SINGLE_CHARACTERS, section "New line characters".
                    // The replacing mode ends when one of the New Line characters is read (commonly 0x15).
                    //
                    // This situation is recognized in spooled file QPDSPJOB under heading Job Shared Member Locks
                    // when the text
                    //
                    // 'Logical member . . . . . . . . : QAOKL02A'
                    //
                    // replaces already printed text
                    //
                    // ' File . . . . : QAOKL02A Library . . . . . : QUSRSYS'
                    //
                    // with leading spaces.
                    // --------------------------------------------------------------------------------------
                    switch (machineState) {

                        case SINGLE_CHARACTERS: {
                            // Skipped control characters
                            if (byteRead == 0x00 // Null
                                    || byteRead == 0x01 // ASCII Transparency (ATRN)
                                    || byteRead == 0x02 // ?
                                    || byteRead == 0x03 // ?
                                    || byteRead == 0x09 // Superscript (SPS)
                                    || byteRead == 0x0A // Repeat (RPT)
                                    || byteRead == 0x0D // Carriage Return (CR)
                                    || byteRead == 0x16 // Backspace (BS)
                                    || byteRead == 0x1A // Unit Backspace (UBS)
                                    || byteRead == 0x23 // Word Underscore (WUS)
                                    || byteRead == 0x2A // Switch (SW)
                                    || byteRead == 0x2F // Bell/Stop (BEL/STP)
                                    || byteRead == 0x36 // Numeric Backspace (NBS)
                                    || byteRead == 0x38 // Subscript (SBS)
                                    ) {
                                // System.out.print(byteToHex(byteRead));
                                break;
                            }
                            // New Line characters
                            // ===================
                            if (byteRead == 0x06 // Required New Line (RNL)
                                    //|| byteRead == 0x0B // Vertical Tab (VT)
                                    || byteRead == 0x0C // Form Feed (FF)
                                    || byteRead == 0x15 // New Line (NL)
                                    || byteRead == 0x1E // Interchange Record Separator (IRE)
                                    || byteRead == 0x25 // Line Feed/Index (LF/INX)
                                    || byteRead == 0x33 // Index Return (IRT)
                                    || byteRead == 0x3A // Required Form Feed/Required Page End (RFF/RFE)
                                    ) {
                                // System.out.print(byteToHex(byteRead));
                                // System.out.println();

                                // If a text on the left of the already written text is being replaced
                                // due to presentation position which is less than current position -
                                // - set pointer to the resume position.
                                if (isReplacingPrecedingBytes) {
                                    // Set pointer to resume position in line
                                    endPointer = endPointer + resumePosInLine - currentPosInLine;
                                    // End replacing mode
                                    isReplacingPrecedingBytes = false;
                                }
                                // Interpreted by CR and LF - Carriage Return and Line Feed
                                workBuffer[endPointer] = 0x0d; // CR
                                //endPointer++;
                                workBuffer[endPointer] = 0x25; // LF in EBCDIC!
                                endPointer++;
                                currentPosInLine = 0;
                                break;
                            }
                            // Presentation position (34)
                            if (byteRead == 0x34) {
                                // System.out.println("Presentation Position command: " + byteToHex(byteRead));
                                machineState = PRESENTATION_POSITION;
                                break;
                            }
                            // Command classes introduced by 2B
                            if (byteRead == 0x2B) {
                                // System.out.println("B2 command classes: " + byteToHex(byteRead));
                                machineState = COMMAND_CLASSES;
                                break;
                            }
                            // Horizontal tab (HT)
                            if (byteRead == 0x05 // Horizontal Tab (HT)
                                    ) {
                                // System.out.print("HT Horizontal tab: " + byteToHex(byteRead));

                                // This command is not used by Twinax
                                break;
                            }
                            // Indent tab (IT)
                            if (byteRead == 0x39 // Indent tab (IT)
                                    ) {
                                // System.out.print("IT Indent tab: " + byteToHex(byteRead));

                                // This command is not used by Twinax
                                break;
                            }
                            // Transparent (TRN)
                            if (byteRead == 0x35) {
                                // System.out.print("TRN Transparent tab: " + byteToHex(byteRead));
                                // System.out.print(byteToHex(byteRead));

                                machineState = TRANSPARENT;
                                break;
                            }
                            // Vertical Channel Select (VCS)
                            if (byteRead == 0x04) {
                                // System.out.print("VCS Vertical Channel Select: " + byteToHex(byteRead));
                                // System.out.print(byteToHex(byteRead));

                                // Interpreted by CR and LF - Carriage Return and Line Feed
                                workBuffer[endPointer] = 0x0d; // CR
                                //endPointer++;
                                workBuffer[endPointer] = 0x25; // LF
                                //endPointer++;
                                machineState = VERTICAL_CHANNEL_SELECT;
                                break;
                            }
                            // All other (printable) characters (<= EF)
                            // --------------------------------
                            if (byteRead <= 0xef) {
                                // System.out.print(byteToHex(byteRead));
                                if (byteRead == 0x3f) {
                                    workBuffer[endPointer] = 0x40;
                                } else {
                                    workBuffer[endPointer] = byteRead;
                                }
                                endPointer++;
                                currentPosInLine++;
                                break;
                            }
                            break;
                        }
                        case PRESENTATION_POSITION: {
                            // System.out.println("Presentation position type: " + byteToHex(byteRead));

                            // Cast to *byte* is important because some types are negative numbers!
                            if (byteRead == (byte) 0x4c // Relative Vertical Presentation Position (RVPP)
                                    ) {
                                machineState = RVPP;
                                break;
                            } else if (byteRead == (byte) 0xc0 // Absolute Horizontal Presentation Position (AHPP)
                                    ) {
                                machineState = AHPP;
                                break;
                            } else if (byteRead == (byte) 0xc4 // Absolute Vertical Presentation Position (AVPP)
                                    ) {
                                machineState = AVPP;
                                break;
                            } else if (byteRead == (byte) 0xc8 // Relative Horizontal Presentation Position (RHPP)
                                    ) {
                                machineState = RHPP;
                                break;
                            } else {
                                // System.out.println("Invalid Presentation position type: " + byteToHex(byteRead));
                            }
                            break;
                        }
                        case RVPP: {
                            // Relative Vertical Presentation Position (RVPP)
                            // System.out.println("RVPP: " + byteToHex(byteRead));

                            // Insert (count - 1) new lines
                            for (int cnt = 0; cnt < byteRead - 1; cnt++) {
                                // Interpreted by CR and LF - Carriage Return and Line Feed
                                workBuffer[endPointer] = 0x0d; // CR
                                //endPointer++;
                                workBuffer[endPointer] = 0x25; // LF
                                //endPointer++;
                            }
                            machineState = SINGLE_CHARACTERS;
                            break;
                        }
                        case AHPP: {
                            // Absolute Horizontal Presentation Position (AHPP)
                            // ================================================
                            // System.out.println("AHPP hex: " + byteToHex(byteRead));
                            // System.out.println("AHPP dec: " + unsignedByteToInt(byteRead));
                            // If the presentation position is LESS than last position in line -
                            // - insert new line and reset the last position in line to zero (start of line)
                            if (unsignedByteToInt(byteRead) < currentPosInLine) {
                                // Begin replacing mode in which already written bytes in the work buffer
                                // are being replaced by new bytes read from input.
                                isReplacingPrecedingBytes = true;
                                // Remember the current position in the line as a new beginning for adding bytes
                                // after the replacing mode ends.
                                resumePosInLine = currentPosInLine;
                                // Set pointer back to the work buffer where already written text in the line began
                                endPointer = endPointer - currentPosInLine + byteRead - 1;
                                // Set new last posiion in line to the first presentation position in the line
                                currentPosInLine = byteRead;
                            } else {
                                // Add RELATIVE number of spaces (absolute presentation position - currentPosInLine)
                                int numberOfSpaces = unsignedByteToInt(byteRead) - currentPosInLine - 1;
                                for (int cnt = 0; cnt < numberOfSpaces; cnt++) {
                                    workBuffer[endPointer] = 0x40; // Insert a space
                                    endPointer++;
                                }
                                // Increment current position in line by the number of added spaces
                                currentPosInLine += numberOfSpaces;
                            }
                            machineState = SINGLE_CHARACTERS;
                            break;
                        }
                        case AVPP: {
                            // Absolute Vertical Presentation Position (AVPP)
                            // System.out.println("AVPP: " + byteToHex(byteRead));

                            // Insert (count + 2) new lines - usually 3 lines before a heading
                            // because the count is usually 01
                            for (int cnt = 0; cnt < byteRead + 2; cnt++) {
                                // Interpreted by CR and LF - Carriage Return and Line Feed
                                workBuffer[endPointer] = 0x0d; // CR
                                //endPointer++;
                                workBuffer[endPointer] = 0x25; // LF
                                //endPointer++;
                            }
                            machineState = SINGLE_CHARACTERS;
                            break;
                        }
                        case RHPP: {
                            // Relative Horizontal Presentation Position (RHPP)
                            // System.out.println("RHPP: " + byteToHex(byteRead));
                            // Insert (count -1) spaces (assume that RHPP is a positive number)
                            for (int cnt = 0; cnt < byteRead - 1; cnt++) {
                                workBuffer[endPointer] = 0x40; // Interpret by inserting a space
                                endPointer++;
                            }
                            machineState = SINGLE_CHARACTERS;
                            break;
                        }
                        case TRANSPARENT: {
                            // System.out.print(byteToHex(byteRead));
                            // Next byte will be skipped - number of bytes following this command
                            // not to be checked for printed datastream commands
                            machineState = NBR_TRANSP_BYTES;
                            break;
                        }
                        case NBR_TRANSP_BYTES: {
                            // System.out.print(byteToHex(byteRead));
                            // No operation will be done (but machineState changed)
                            machineState = SINGLE_CHARACTERS;
                            break;
                        }
                        case VERTICAL_CHANNEL_SELECT: {
                            // System.out.print(byteToHex(byteRead));
                            machineState = VERTICAL_CHANNEL;
                            break;
                        }
                        case VERTICAL_CHANNEL: {
                            // System.out.print(byteToHex(byteRead));
                            // Vertical channel ID
                            // X'7A' = 10
                            // X'7B' = 11
                            // X'7C' = 12
                            // X'81' = 1
                            // X'82' = 2
                            // X'83' = 3
                            // X'84' = 4
                            // X'85' = 5
                            // X'86' = 6
                            // X'87' = 7
                            // X'88' = 8
                            // X'89' = 9
                            // Interpreted by CR and LF - Carriage Return and Line Feed
                            workBuffer[endPointer] = 0x0d; // CR
                            //endPointer++;
                            workBuffer[endPointer] = 0x25; // LF
                            //endPointer++;
                            machineState = SINGLE_CHARACTERS;
                            break;
                        }
                        case COMMAND_CLASSES: {
                            // System.out.println("Class: " + byteToHex(byteRead));
                            // hexLine += byteToHex(byteRead);
                            machineState = CLASS;
                            break;
                        }
                        case CLASS: {
                            // System.out.print(byteToHex(byteRead));
                            // This byte is the number (count) of this and following bytes.
                            // Save the count for later comparison.
                            classCount = byteRead;
                            // System.out.println("Class count = " + classCount);
                            machineState = CLASS_COUNTDOWN;
                            break;
                        }
                        case CLASS_COUNTDOWN: {
                            // System.out.print(byteToHex(byteRead));
                            // Decrement class count and compare if greater than zero
                            classCount--;
                            if (classCount > 0) {
                                // System.out.println("Class count = " + classCount);
                                machineState = CLASS_COUNTDOWN;
                            } else {
                                // System.out.println("Class Countdown 0");
                                machineState = SINGLE_CHARACTERS;
                            }
                            break;
                        }
                        default: {
                            // System.out.println("\nUNKNOWN CONTROL CHARACTER! " + byteToHex(byteRead));
                        }
                    } // end switch
                    // This is the end of the state machine
                } // end for

                // System.out.println("\n FIRST BYTE: " + byteToHex(inputBuffer[0]));
                // Copy the printable part of the work array (up to *endPointer* position)
                // to a new buffer that will be written out.
                byte[] bufferToWrite = new byte[endPointer];
                // Copy bytes from the work buffer to the new buffer
                for (int indx = 0; indx < endPointer; indx++) {
                    bufferToWrite[indx] = workBuffer[indx];
                }

                // Create object for conversion from bytes to characters
                AS400Text textConverter = new AS400Text(endPointer, ibmCcsidInt, remoteServer);

                // int CCSID = textConverter.getCcsid();
                // System.out.println(" CCSID: " + CCSID);
                // Convert byte array buffer to text line
                String textLine = (String) textConverter.toObject(bufferToWrite);
                // System.out.println(textLine);

                // Write text line to the text file
                outfileText.write(textLine);

                // Write text line to text area
                spoolTextArea.append(textLine);

                // Read next input buffer
                bytesRead = inputStream.read(inputBuffer);
                // Reset pointer in work buffer so it points to the beginning
                endPointer = 0;
            }
            // Close files
            inputStream.close();
            outfileText.close();

            row = "Info: Spooled file characters were converted using CCSID  " + ibmCcsid + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(noNodes); // do not add child nodes
            mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
            return spoolTextArea.getText();

        } catch (AS400SecurityException | ErrorCompletingRequestException | RequestNotSupportedException | IOException | InterruptedException exc) {
            System.out.println("Error: " + exc.toString());
            exc.printStackTrace();
            row = "Error: Spooled file CCSID   '" + ibmCcsid
                    + "'   or  text file character set   '" + pcCharset + "'   is not correct.  -  " + exc.toString();
            mainWindow.msgVector.add(row);
            row = "Set \"IBM i CCSID\" to 65535 and press button \"Spooled file\" to display the spooled file again.";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(noNodes); // do not add child nodes
            mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
            return "ERROR";
        }
    }

    /**
     * Call conversion of the selected spooled file; Info for the called method is taken from the table row selected by
     * the user.
     *
     * @param leadIndex
     * @return
     */
    protected String convertSpooledFileCall(int leadIndex) {
        // Lead index is the last index from the last interval if intervals are selected.
        // Lead index is the only index if a single row is selected.
        rowIndex = leadIndex;
        // Get parameters for method convertSpooledFile() from selected table row
        namePar = (String) spoolRows[rowIndex][0];
        numberPar = (String) spoolRows[rowIndex][1];
        jobPar = (String) spoolRows[rowIndex][2];
        userPar = (String) spoolRows[rowIndex][3];
        jobNumberPar = (String) spoolRows[rowIndex][4];
        datePar = (String) spoolRows[rowIndex][5];
        timePar = (String) spoolRows[rowIndex][6];
        queuePar = (String) spoolRows[rowIndex][7];
        // Spooled file object was saved in the work array list
        // when all spooled files were selected
        splf = splfArrListWork.get(rowIndex);

        // Convert the spooled file and returns empty string or "ERROR"
        return convertSpooledFile(splf);
    }

    /**
     * Display spooled files.
     */
    protected void displaySpooledFiles() {
        try {
            BufferedReader infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
            properties.load(infile);
            infile.close();
            ibmCcsid = properties.getProperty("IBM_CCSID");
            try {
                ibmCcsidInt = Integer.parseInt(ibmCcsid);
            } catch (Exception exc) {
                // If ibmCcsid is not numeric, take 65535
                exc.printStackTrace();
                ibmCcsid = "65535";
                ibmCcsidInt = 65535;
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }

        // Display selected spooled files.
        for (int idx = 0; idx < selIndexArrList.size(); idx++) {
            rowIndex = selIndexArrList.get(idx);
            // Read input stream and convert spooled file into text
            String returnText = convertSpooledFileCall(rowIndex);
            // If no error in character set is recognized - display the spooled file in a window
            if (!returnText.equals("ERROR")) {
                // Display information in the text area obtained from the text file containing spooled file text.
                JTextArea textArea = new JTextArea();
                DisplayFile dspf = new DisplayFile(textArea, mainWindow);
                dspf.displayTextArea(spoolTextArea.getText(), ibmCcsid);
            }
        }
        createSpoolTable();
    }

    /**
     * Delete selected spooled file and refresh the table.
     */
    protected void deleteSpooledFile() {
        // Save the original user for whom the spooled table was displayed
        String userParSaved = userComboBox.getSelectedItem().toString();

        // Backward loop
        for (int idx = selIndexArrList.size() - 1; idx >= 0; idx--) {
            rowIndex = selIndexArrList.get(idx);

            namePar = (String) spoolRows[rowIndex][0];
            numberPar = (String) spoolRows[rowIndex][1];
            pagesPar = (String) spoolRows[rowIndex][2];
            jobPar = (String) spoolRows[rowIndex][3];
            userPar = (String) spoolRows[rowIndex][4];
            jobNumberPar = (String) spoolRows[rowIndex][5];
            datePar = (String) spoolRows[rowIndex][6];
            timePar = (String) spoolRows[rowIndex][7];
            // Spooled file object was saved in the work array list
            // when all spooled files were selected
            splf = splfArrListWork.get(rowIndex);
            try {
                // Delete selected spooled file
                splf.delete();

                // Remove the array list elements with this row index
                nameArrList.remove(rowIndex);
                numberArrList.remove(rowIndex);
                pagesArrList.remove(rowIndex);
                jobArrList.remove(rowIndex);
                userArrList.remove(rowIndex);
                jobNumberArrList.remove(rowIndex);
                dateArrList.remove(rowIndex);
                timeArrList.remove(rowIndex);
                queueArrList.remove(rowIndex);
                splfArrList.remove(rowIndex);
            } catch (Exception exc) {
                exc.printStackTrace();
            } finally {
                // Remove selection from the same number of table rows that remained after deletion.
                rowSelectionModel.removeSelectionInterval(0, selIndexArrList.size());
                // Refresh spooled file table for the original user for whom the table was displayed
                refreshSpoolTable(userParSaved);
            }
        }
    }

    /**
     * Convert unsigned byte to integer even if the byte is negative number (the most significant bit is 1 - for example
     * in byte 0x80)
     *
     * @param aByte
     */
    int unsignedByteToInt(byte aByte) {
        return aByte & 0xFF;
    }

    /**
     * Get output queue name (libraryName/object name) from the IBM i IFS path.
     *
     * @param as400PathString
     * @return
     */
    protected String getQualifiedQueueNameFromIfsPath(String as400PathString) {

        String qsyslib = "/QSYS.LIB/";
        if (as400PathString.startsWith(qsyslib) && as400PathString.length() > qsyslib.length()) {
            String libraryName = as400PathString.substring(as400PathString.indexOf("/QSYS.LIB/")
                    + 10, as400PathString.lastIndexOf(".LIB"));
            if (as400PathString.length() > qsyslib.length() + libraryName.length() + 5) {
                String queueName = as400PathString.substring(qsyslib.length() + libraryName.length()
                        + 5, as400PathString.lastIndexOf(".OUTQ"));
                return libraryName + "/" + queueName;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Convert byte to hex string
     *
     * @param aByte
     * @return hex string
     */
    static String byteToHex(byte aByte) {
        int bin = (aByte < 0) ? (256 + aByte) : aByte;
        int bin0 = bin >>> 4; // upper half-byte
        int bin1 = bin % 16; // lower half-byte
        String hex = Integer.toHexString(bin0) + Integer.toHexString(bin1);
        return hex;
    }

    /**
     * Data model provides methods to fill data from the source to table cells for display. It is applied every time when
     * any change in data source occurs.
     */
    class SpoolTableModel extends AbstractTableModel {

        static final long serialVersionUID = 1L;

        // Returns number of columns
        @Override
        public int getColumnCount() {
            return nbrOfCols;
        }

        // Returns number of rows
        @Override
        public int getRowCount() {
            return nbrOfRows;
        }

        /* // Sets number of rows public void setRowCount(int rowCount) { nbrOfRows = rowCount; } */
        // Returns column name
        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        // Data transfer from the source to a cell for display. It is applied
        // automatically at any change of data source but also when ENTER or TAB key is pressed or when clicked by a
        // mouse.
        // Double click or pressing a data key invokes the cell editor method - getTableCellEditorComponent().
        // The method is called at least as many times as is the number of cells in the table.
        @Override
        public Object getValueAt(int row, int col) {
            // System.out.println("getValueAt: (" + row + "," + col + "): " + rows[row][col]);
            // Return the value for display in the table
            return spoolRows[row][col].toString();
        }

        // Write input data from the cell back to the data source for
        // display in the table. A change in the data source invokes method
        // getValueAt().
        // The method is called after the cell editor ends its activity.
        @Override
        public void setValueAt(Object obj, int row, int col) {
            // Assign the value from the cell to the data source.
            spoolRows[row][col] = obj;
            // System.out.println("setValueAt: (" + row + "," + col + "): " + rows[row][col]);
        }

        // Get class of the column value - it is important for the cell editor
        // could be invoked and could determine e.g. the way of aligning of the
        // text in the cell.
        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public Class getColumnClass(int col) {
            return getValueAt(0, col).getClass();
        }

        // Determine whicn cells are editable or not
        @Override
        public boolean isCellEditable(int row, int col) {
            return false; // column 0 - RRN - cannot be changed
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
     * Listening to mouse click on the spooled table line.
     */
    class SpoolTableMouseAdapter extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent mouseEvent) {
            // Display single spooled file directly 
            if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
                // Double left click on the table row displays the spooled file.
                // Only one row can be double clicked.
                if (mouseEvent.getClickCount() == 2) {
                    rowIndex = selIndexArrList.get(0);
                    // Read input stream and convert spooled file into text
                    String returnText = convertSpooledFileCall(rowIndex);
                    // If no error in character set is recognized - display the spooled file in a window
                    if (!returnText.equals("ERROR")) {
                        // Display information in the text area obtained from the text file containing spooled file text.
                        JTextArea textArea = new JTextArea();
                        DisplayFile dspf = new DisplayFile(textArea, mainWindow);
                        dspf.displayTextArea(spoolTextArea.getText(), ibmCcsid);
                    }
                }
            } //
            // Show context menu on right click
            else if (mouseEvent.getButton() == MouseEvent.BUTTON3) {
                spoolPopupMenu.removeAll();
                spoolPopupMenu.add(displaySpooledFiles);
                spoolPopupMenu.add(copySpooledFile);
                spoolPopupMenu.add("");
                spoolPopupMenu.add(deleteSpooledFile);
                // Show the context menu
                spoolPopupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
            }
        }
    }

    /**
     * Adjustment listener for scroll pane.
     */
    class ScrollPaneAdjustmentListenerMax implements AdjustmentListener {

        @Override
        public void adjustmentValueChanged(AdjustmentEvent ae) {
            // Set scroll pane to the bottom - the last element
            ae.getAdjustable().setValue(ae.getAdjustable().getMaximum());
        }
    }

    /**
     * Window adapter listens to the window closing icon.
     */
    class SpoolWindowAdapter extends WindowAdapter {

        // Dispose of the window
        @Override
        public void windowClosing(WindowEvent we) {
            Compile.wrkSplFCall = null;
            MainWindow.wrkSplFCall = null;
            dispose();
        }
    }
}
