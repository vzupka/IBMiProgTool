package copyfiles;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Exception;
import com.ibm.as400.access.AS400File;
import com.ibm.as400.access.AS400FileRecordDescription;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.IFSFileInputStream;
import com.ibm.as400.access.Record;
import com.ibm.as400.access.RecordFormat;
import com.ibm.as400.access.SequentialFile;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.LayerUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.JTextComponent;

/**
 * This class searches for a text pattern in multiple files.
 *
 * @author Vladimír Župka 2017
 */
public class SearchWindow extends JFrame {

    final Color DIM_BLUE = Color.getHSBColor(0.60f, 0.2f, 0.5f); // blue little saturated dim (gray)
    final Color DIM_RED = Color.getHSBColor(0.00f, 0.2f, 0.98f); // red little

    static final Color WARNING_COLOR = new Color(255, 200, 200);

    Path matchCaseIconPathDark = Paths.get(System.getProperty("user.dir"), "icons", "matchCase1.png");
    Path matchCaseIconPathDim = Paths.get(System.getProperty("user.dir"), "icons", "matchCase2.png");

    Properties properties;
    Path parPath = Paths.get(System.getProperty("user.dir"), "paramfiles", "Parameters.txt");
    String encoding = System.getProperty("file.encoding", "UTF-8");
    final String PROP_COMMENT = "Copy files between IBM i and PC, edit and compile.";

    JLabel findLabel = new JLabel("Find:");
    JTextField findField = new JTextField();

    JButton cancelButton = new JButton("Cancel");

    int windowWidth = 550;
    int windowHeight = 400;

    JPanel rowPanel1;
    JPanel rowPanel2;
    JPanel topPanel;
    int topPanelHeigtht = 50;
    JScrollPane scrollPane;
    JTable matchedFilesTable;
    MatchedFilesTableCellRenderer tableCellRenderer;

    JPanel panel;

    // Icon "Aa" will be dimmed or dark when clicked
    ImageIcon matchCaseIconDark = new ImageIcon(matchCaseIconPathDark.toString());
    ImageIcon matchCaseIconDim = new ImageIcon(matchCaseIconPathDim.toString());

    JToggleButton matchCaseButton = new JToggleButton();

    final String NEW_LINE = "\n";

    String inputText;
    Pattern pattern;

    AS400 remoteServer;
    MainWindow mainWindow;
    String fileType;

    static final String PATH = "File path";
    static final String HITS = "Hits";
    String[] colNames = {PATH, HITS};
    String[] filePaths = {};
    int[] fileHits = {};
    ArrayList<String> filePathsArrList;
    ArrayList<Integer> fileHitsArrList;

    // Model for selection of rows in the table
    ListSelectionModel rowSelectionModel;

    JPopupMenu popupMenu = new JPopupMenu();
    JMenuItem displayFile = new JMenuItem("Display file");
    JMenuItem editFile = new JMenuItem("Edit file");

    String qsyslib;
    String libraryName;
    String fileName;
    String memberName;

    MouseListener tableMouseListener;

    JTextArea textArea;
    String matchCase;

    /**
     * Constructor.
     *
     * @param remoteServer
     * @param mainWindow
     * @param fileType
     */
    public SearchWindow(AS400 remoteServer, MainWindow mainWindow, String fileType) {
        this.remoteServer = remoteServer;
        this.mainWindow = mainWindow;
        this.fileType = fileType;

        properties = new Properties();
        try {
            BufferedReader infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
            properties.load(infile);
            infile.close();
        } catch (Exception exc) {
            exc.printStackTrace();
        }

        matchCase = properties.getProperty("MATCH_CASE");

        if (matchCase.equals("CASE_INSENSITIVE")) {
            matchCaseButton.setIcon(matchCaseIconDim);
            matchCaseButton.setSelectedIcon(matchCaseIconDim);
            matchCaseButton.setToolTipText("Case insensitive. Toggle Match case.");
        } else {
            matchCaseButton.setIcon(matchCaseIconDark);
            matchCaseButton.setSelectedIcon(matchCaseIconDark);
            matchCaseButton.setToolTipText("Match case. Toggle Case insensitive.");
        }

        matchCaseButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        matchCaseButton.setContentAreaFilled(false);
        matchCaseButton.setPreferredSize(new Dimension(25, 20));

        // Register WindowListener for storing X and Y coordinates to properties
        addWindowListener(new SearchWindowAdapter());

        findField.requestFocus();
        findField.setPreferredSize(new Dimension(200, 20));
        findField.setMaximumSize(new Dimension(200, 20));
        // Set document listener for the search field
        findField.setToolTipText("Enter text to find.");

        rowPanel1 = new JPanel();
        rowPanel1.setLayout(new BoxLayout(rowPanel1, BoxLayout.X_AXIS));
        rowPanel1.add(findLabel);
        rowPanel1.add(findField);
        rowPanel1.add(matchCaseButton);
        rowPanel1.add(Box.createHorizontalGlue());

        rowPanel2 = new JPanel();
        rowPanel2.setLayout(new BoxLayout(rowPanel2, BoxLayout.X_AXIS));
        rowPanel2.add(cancelButton);
        rowPanel2.add(Box.createHorizontalGlue());

        topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(rowPanel1);
        topPanel.add(rowPanel2);

        // Create table object with data model
        matchedFilesTable = new JTable(new MatchedFilesTableModel());
        matchedFilesTable.setGridColor(Color.lightGray);
        matchedFilesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        matchedFilesTable.getTableHeader().setFont(findLabel.getFont().deriveFont(Font.BOLD, 12));

        // Define table column model
        TableColumnModel tcm = matchedFilesTable.getColumnModel();
        // Define table cell renderer
        tableCellRenderer = new MatchedFilesTableCellRenderer();
        // Define table columns
        TableColumn tc0 = tcm.getColumn(0);
        tc0.setCellRenderer(tableCellRenderer);
        tc0.setPreferredWidth(windowWidth - 30);
        TableColumn tc1 = tcm.getColumn(1);
        tc1.setCellRenderer(tableCellRenderer);
        tc1.setPreferredWidth(35);
        // Table headers alignment
        DefaultTableCellRenderer defRend = (DefaultTableCellRenderer) matchedFilesTable.getTableHeader().getDefaultRenderer();
        defRend.setHorizontalAlignment(JLabel.CENTER);
        matchedFilesTable.getTableHeader().setDefaultRenderer(defRend);

        // Create scroll pane with the table
        scrollPane = new JScrollPane(matchedFilesTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Create and register row selection model (for selecting a single row)
        // ---------------------------------------
        rowSelectionModel = matchedFilesTable.getSelectionModel();
        matchedFilesTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        // Row selection model registration
        rowSelectionModel.addListSelectionListener(sl -> {
            matchedFilesTable.getSelectionModel().getLeadSelectionIndex();    
        });

        // Component listener reacting to WINDOW RESIZING
        // ------------------
        ComponentListener resizingListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent componentEvent) {
                windowWidth = componentEvent.getComponent().getWidth();
                windowHeight = componentEvent.getComponent().getHeight();
                topPanel.setPreferredSize(new Dimension(windowWidth, topPanelHeigtht));
                scrollPane.setPreferredSize(new Dimension(windowWidth, windowHeight - topPanelHeigtht));
            }
        };
        // Register window resizing listener
        addComponentListener(resizingListener);


        // Mouse listener for left tree
        tableMouseListener = new TableMouseListener();
        matchedFilesTable.addMouseListener(tableMouseListener);

        panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Box.createVerticalStrut(5));
        // Add top panel
        panel.add(topPanel);
        // Add scroll pane
        panel.add(scrollPane);

        // Add panel to frame
        add(panel);

        // Conclude showing window
        setSize(windowWidth, windowHeight);
        setLocation(0, 0);
        setVisible(true);
        //pack();

        // "Match case" button listener
        // ----------------------------
        matchCaseButton.addActionListener(ae -> {
            if (matchCaseButton.getSelectedIcon().equals(matchCaseIconDark)) {
                matchCaseButton.setSelectedIcon(matchCaseIconDim);
                matchCaseButton.setToolTipText("Case insensitive. Toggle Match case.");
                matchCase = "CASE_INSENSITIVE";
            } else {
                matchCaseButton.setSelectedIcon(matchCaseIconDark);
                matchCaseButton.setToolTipText("Match case. Toggle Case insensitive.");
                matchCase = "CASE_SENSITIVE";
            }
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

        // "Cancel" button listener
        // ----------------------
        cancelButton.addActionListener(ae -> {
            // Clear input fields
            findField.setText("");
            setVisible(false);
        });

        // Find field listener gets matches and creates lists of file paths and number of hits (number of matches).
        // -------------------
        findField.addActionListener(ae -> {
            // This process goes in parallel.
            SearchMatchingFiles findMatchingFiles = new SearchMatchingFiles(remoteServer, mainWindow, this, fileType);
            findMatchingFiles.execute();
        });

        // Display file
        displayFile.addActionListener(ae -> {
            for (int selectedRow : matchedFilesTable.getSelectedRows()) {
                displayFile(selectedRow);
            }
        });

        // Edit file
        editFile.addActionListener(ae -> {
            for (int selectedRow : matchedFilesTable.getSelectedRows()) {
                editFile(selectedRow);
            }
        });
    }

    /**
     * Create and compile a Pattern object for a text in the findField.
     *
     * @return
     */
    protected Pattern getPattern() {
        String pattern = inputText;
        if (Objects.isNull(pattern) || pattern.isEmpty()) {
            return null;
        }
        try {
            pattern = String.format(pattern);
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
            
            int flags = matchCaseButton.getSelectedIcon().equals(matchCaseIconDark) ? 0
                    : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            return Pattern.compile(pattern, flags);
        } catch (PatternSyntaxException ex) {
            findField.setBackground(WARNING_COLOR);
            return null;
        }
    }

    /**
     * Display file depending on its type.
     */
    protected void displayFile(int selectedRow) {
        JTextArea textArea = new JTextArea();
        DisplayFile dspf = new DisplayFile(textArea, mainWindow);
        if (fileType.equals("PC")) {
            // PC file
            dspf.displayPcFile(filePathsArrList.get(selectedRow));
        } else if (fileType.equals("MBR")) {
            // MBR - source member
            dspf.displaySourceMember(remoteServer, filePathsArrList.get(selectedRow));
        } else if (fileType.equals("IFS")) {
            // IFS file
            dspf.displayIfsFile(remoteServer, filePathsArrList.get(selectedRow));
        }
        // Triggers method "dspf.changeHighlight()" internally.
        dspf.findField.setText(inputText);
    }

    /**
     * Edit file depending on its type.
     */
    protected void editFile(int selectedRow) {
        EditFile edtf = null;
        JTextArea textArea = new JTextArea();
        JTextArea textArea2 = new JTextArea();
        if (fileType.equals("PC")) {
            // PC file
            edtf = new EditFile(remoteServer, mainWindow, textArea, textArea2, filePathsArrList.get(selectedRow), "rewritePcFile");
        } else if (fileType.equals("MBR")) {
            // MBR - source member (extra modification to convene IFS path convention
            String pathStringMbr = filePathsArrList.get(selectedRow).substring(0, filePathsArrList.get(selectedRow).lastIndexOf(".")) + ".MBR";
            edtf = new EditFile(remoteServer, mainWindow, textArea, textArea2, pathStringMbr, "rewriteSourceMember");
        } else if (fileType.equals("IFS")) {
            // IFS file
            edtf = new EditFile(remoteServer, mainWindow, textArea, textArea2, filePathsArrList.get(selectedRow), "rewriteIfsFile");
        }
        // Triggers method "dspf.changeHighlight()" internally.
        edtf.findWindow.findField.setText(inputText);
    }

    /**
     * Read PC file data into text area.
     *
     * @return
     */
    protected JTextArea readPcFile() {
        try {
            Path filePath = Paths.get(mainWindow.sourcePathString);
            if (Files.exists(filePath)) {
                if (mainWindow.pcCharset.equals("*DEFAULT")) {
                    // Set ISO-8859-1 as a default
                    mainWindow.pcCharset = "ISO-8859-1";
                }
                // Use PC charset parameter for conversion
                List<String> list = Files.readAllLines(filePath, Charset.forName(mainWindow.pcCharset));
                if (list != null) {
                    Object[] obj = (Object[]) list.stream().toArray();
                    for (int jdx = 0; jdx < obj.length; jdx++) {
                        String text = obj[jdx].toString();
                        textArea.append(text + NEW_LINE);
                    }
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            mainWindow.row = "Error: Displaying PC file:  " + mainWindow.sourcePathString
                    + "  is not a text file or has an unsuitable character set.  -  " + exc.toString();
            mainWindow.msgVector.add(mainWindow.row);
            mainWindow.showMessages(mainWindow.nodes); // do not add child nodes
            // Remove message scroll listener (cancel scrolling to the last message)
            mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
        }
        return textArea;
    }

    /**
     * Read IFS file data into text area.
     *
     * @return
     */
    protected JTextArea readIfsFile() {
        try {
            IFSFile ifsFile = new IFSFile(remoteServer, mainWindow.sourcePathString);
            int attributeCCSID = ifsFile.getCCSID();

            byte[] inputBuffer = new byte[100000];
            byte[] workBuffer = new byte[100000];

            try (IFSFileInputStream inputStream = new IFSFileInputStream(remoteServer, mainWindow.sourcePathString)) {
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
                mainWindow.row = "Info: IFS file  " + mainWindow.sourcePathString + "  has CCSID  " + attributeCCSID + ".";
                mainWindow.msgVector.add(mainWindow.row);
                mainWindow.showMessages(mainWindow.nodes);
            } catch (IOException | AS400SecurityException exc) {
                exc.printStackTrace();
                mainWindow.row = "Error: " + exc.toString();
                mainWindow.msgVector.add(mainWindow.row);
                mainWindow.showMessages(mainWindow.nodes);
            }
        } catch (IOException exc) {
            exc.printStackTrace();
            mainWindow.row = "Error: " + exc.toString();
            mainWindow.msgVector.add(mainWindow.row);
            mainWindow.showMessages(mainWindow.nodes);
        }
        
        // Remove message scroll listener (cancel scrolling to the last message)
        mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
        return textArea;
    }

    /**
     * Read Source Member data into text area.
     *
     * @return
     */
    protected JTextArea readSourceMember() {

        // Set provisional source type to .MBR to convene IFS path convention
        String pathString = mainWindow.sourcePathString;
        pathString = pathString.substring(0, pathString.lastIndexOf(".")) + ".MBR";

        IFSFile ifsFile = new IFSFile(remoteServer, pathString);

        // Create an AS400FileRecordDescription object that represents the file
        AS400FileRecordDescription inRecDesc = new AS400FileRecordDescription(remoteServer, pathString);

        try {
            // Decide what CCSID is appropriate for displaying the member
            int ccsidAttribute = ifsFile.getCCSID();

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

                // Source record is composed of three source record fields: seq.
                // number, date, source data.
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
            mainWindow.row = "Info: Source member  " + mainWindow.sourcePathString + "  has CCSID  " + ccsidAttribute + ".";
            mainWindow.msgVector.add(mainWindow.row);
            mainWindow.showMessages(mainWindow.nodes);
        } catch (AS400Exception | AS400SecurityException | PropertyVetoException | IOException | InterruptedException exc) {
            exc.printStackTrace();
            mainWindow.row = "Error: " + exc.toString();
            mainWindow.msgVector.add(mainWindow.row);
            mainWindow.showMessages(mainWindow.nodes);
        }
        // Remove message scroll listener (cancel scrolling to the last message)
        mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);

        return textArea;
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
     * Set indicator N/M that overlays the search field and is right adjusted;
     * N - the sequence number of the text that is just highlighted,
     * M - how many matches were found.
     */
    class PlaceholderLayerUI extends LayerUI<JTextComponent> {

        public final JLabel hint = new JLabel() {

            @Override
            public void updateUI() {
                super.updateUI();
                // setForeground(UIManager.getColor("TextField.inactiveForeground"));
                // The following foreground color is almost the same as "TextField.inactiveForeground"
                setForeground(DIM_BLUE); // blue little saturated dim - gray
                setBackground(DIM_RED); // red little saturated - bright
            }
        };

        @Override
        public void paint(Graphics g, JComponent component) {
            super.paint(g, component);
            if (component instanceof JLayer) {
                JLayer jlayer = (JLayer) component;
                JTextComponent textComponent = (JTextComponent) jlayer.getView();
                if (!textComponent.getText().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setPaint(hint.getForeground());
                    Insets insets = textComponent.getInsets();
                    Dimension dimension = hint.getPreferredSize();
                    int x = textComponent.getWidth() - insets.right - dimension.width - 2;
                    int y = (textComponent.getHeight() - dimension.height) / 2;
                    g2.translate(x, y);
                    SwingUtilities.paintComponent(g2, hint, textComponent, 0, 0, dimension.width, dimension.height);
                    g2.dispose();
                }
            }
        }
    }

    /**
     * Table data model.
     */
    class MatchedFilesTableModel extends AbstractTableModel {

        public int getColumnCount() {
            return colNames.length;
        }

        public int getRowCount() {
            return filePaths.length;
        }

        public String getColumnName(int col) {
            return colNames[col];
        }

        public Object getValueAt(int row, int col) {
            Object obj = null;
            if (colNames[col].equals(PATH)) {
                obj = filePaths[row];
            } else if (colNames[col].equals(HITS)) {
                obj = fileHits[row];
            }
            return obj;
        }

        public boolean isCellEditable(
                int row,
                int col) {
            return false;
        }
    }

    /**
     * Window adapter clears text in findField and closes the window.
     */
    class SearchWindowAdapter extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent we) {
            JFrame win = (JFrame) we.getSource();
            // Clear input field
            findField.setText("");
            win.setVisible(false);
        }
    }

    /**
     * Table cell renderer.
     */
    class MatchedFilesTableCellRenderer extends JLabel implements TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable tabulka,
                Object obj,
                boolean isSelected,
                boolean isFocussed,
                int row,
                int col) {
            setText(obj.toString());
            setOpaque(true);
            if (isSelected) {
                setBackground(DIM_RED);
            } else {
                setBackground(Color.WHITE);
            }
            if (colNames[col].equals(PATH)) {
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
            if (colNames[col].equals(HITS)) {
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
            return this;
        }

    }

    /**
     * Mouse listener shows popup menu (on right button click) or invokes file editing (on double click).
     */
    class TableMouseListener extends MouseAdapter {

        private void popupEvent(MouseEvent mouseEvent) {
            popupMenu.add(displayFile);
            popupMenu.add(editFile);

            // On double click run "editFile"
            if (mouseEvent.getClickCount() == 2
                    && (mouseEvent.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK) {
                for (int selectedRow : matchedFilesTable.getSelectedRows()) {
                    editFile(selectedRow);
                }
            }

            // On right click pop up the menu
            if ((mouseEvent.getButton() == MouseEvent.BUTTON3)) {
                popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
            }
        }

        @Override
        public void mousePressed(MouseEvent mouseEvent) {
            if ((mouseEvent.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK
                    || (mouseEvent.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) == MouseEvent.BUTTON3_DOWN_MASK) {
                popupEvent(mouseEvent);
            } else if (mouseEvent.isPopupTrigger()) {
                popupEvent(mouseEvent);
            }
        }
    }
}
