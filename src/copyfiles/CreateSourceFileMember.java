/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package copyfiles;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.IFSFile;
import java.awt.Dialog;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
  * @author Vladimír Župka 2026
 */
public class CreateSourceFileMember extends JDialog {

    JPanel panel = new JPanel();
    JPanel buttonPanel = new JPanel();
    JPanel namePanel = new JPanel();
    JPanel typePanel = new JPanel();
    GroupLayout layout = new GroupLayout(panel);
    JLabel titleLabel = new JLabel();
    String parentTitle;
    JLabel parentPathLabel = new JLabel();
    JLabel memberNameLabel = new JLabel("Member name: ");
    JTextField textField = new JTextField();
    JComboBox<String> sourceTypeComboBox;
    JButton cancel = new JButton("Cancel");
    JButton enter = new JButton("Enter");

    int windowWidth = 460;
    int windowHeight = 160;

    AS400 remoteServer;
    MainWindow mainWindow;
    IFSFile ifsFile;
    String row;

    String qsyslib;
    String libraryName;
    String fileName;
    String memberName;
    
    final String PROP_COMMENT = "Copy files between IBM i and PC, edit and compile.";
    Path parPath = Paths.get(System.getProperty("user.dir"), "paramfiles", "Parameters.txt");
    BufferedReader infile;
    BufferedWriter outfile;
    String encoding = System.getProperty("file.encoding", "UTF-8");


    Properties properties;
    String sourceType;

    /**
     * Constructor
     *
     * @param remoteServer
     * @param ifsFile
     * @param mainWindow
     * @param parentTitle
     * @param currentX
     * @param currentY
     */
    public CreateSourceFileMember(AS400 remoteServer, IFSFile ifsFile, MainWindow mainWindow, String parentTitle, int currentX, int currentY) {
        super();
        super.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        super.setTitle("CREATE NEW SOURCE PHYSICAL FILE MEMBER");
        this.remoteServer = remoteServer;
        this.mainWindow = mainWindow;
        this.parentTitle = parentTitle;
        this.ifsFile = ifsFile;

        properties = new Properties();

        titleLabel.setText(parentTitle + ": ");
        parentPathLabel.setText(ifsFile.toString());
        textField.setMaximumSize(new Dimension(300, 20));
 
        ArrayList<String> sourceTypes;
        JLabel memberTypeLabel = new JLabel("Member type:");

        String[] sourceFileTypes = {"C", "CBL", "CBLLE", "CLLE", "CLP", "CMD", "CPP",
            "DSPF", "LF", "MNU", "MNUCMD", "MNUDDS", "PF", "PLI", "PRTF", "REXX", "RPG", "RPGLE",
            "SQL", "SQLC", "SQLCPP", "SQLCBL", "SQLCBLLE", "SQLPLI", "SQLRPG", "SQLRPGLE", 
            "TBL", "TXT",};

        sourceTypes = new ArrayList<>();
        sourceTypes.addAll(Arrays.asList(sourceFileTypes));
        
        sourceTypeComboBox = new JComboBox(sourceTypes.toArray());
        sourceTypeComboBox.setToolTipText("List of possible source types.");
        sourceTypeComboBox.setPreferredSize(new Dimension(110, 20));
        sourceTypeComboBox.setMinimumSize(new Dimension(110, 20));
        sourceTypeComboBox.setMaximumSize(new Dimension(110, 20));
        sourceTypeComboBox.setEditable(true);                
        try {
            infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
            properties.load(infile);
            sourceType = properties.getProperty("SOURCE_TYPE");
            sourceTypeComboBox.setSelectedItem(sourceType);
            infile.close();
        } catch (IOException exc) {
            exc.printStackTrace();
        }

        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.LINE_AXIS));
        namePanel.add(memberNameLabel);
        namePanel.add(textField);
        typePanel.setLayout(new BoxLayout(typePanel, BoxLayout.LINE_AXIS));
        typePanel.add(memberTypeLabel);
        typePanel.add(sourceTypeComboBox);

        buttonPanel.add(cancel);
        buttonPanel.add(enter);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

        panel.setLayout(layout);

        layout.setAutoCreateGaps(false);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout
                                .createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(titleLabel)
                                .addComponent(parentPathLabel)
                                .addComponent(namePanel)
                                .addComponent(typePanel)
                                .addGap(5)
                                .addComponent(buttonPanel)
                        )
        );
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(titleLabel)
                                .addComponent(parentPathLabel)
                                .addComponent(namePanel)
                                .addComponent(typePanel)
                                .addGap(5)
                                .addComponent(buttonPanel)
                        )
        );

        // Source Type combo box item listener
        sourceTypeComboBox.addItemListener(il -> {
            JComboBox<String[]> source = (JComboBox) il.getSource();
            sourceType = (String) source.getSelectedItem();
        });

        // Enter button acton listener
        enter.addActionListener(en -> {
            createMember(currentX, currentY);
            dispose();
        });

        // Text field action listener
        textField.addActionListener(tf -> {            
            createMember(currentX, currentY);
            dispose();
        });

        // Cancel button action listener
        cancel.addActionListener(en -> {
            dispose();
        });

        getContentPane();
        add(panel);

        setSize(windowWidth, windowHeight);
        setLocation(currentX, currentY);
        setVisible(true);
        //pack();

    }
    
        /**
     * Create Source Member.
     * 
     * @param currentX
     * @param currentY
     */
    protected void createMember(int currentX, int currentY) {

        extractNamesFromIfsPath(ifsFile.toString());

        memberName = textField.getText().toUpperCase();
            try {
                outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                properties.setProperty("SOURCE_TYPE", sourceType);
                properties.store(outfile, PROP_COMMENT);
                outfile.close();                
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        mainWindow.sourceTypeComboBox.setSelectedItem(sourceType);
        // Build command ADDPFM to create a member in the source physical file
        String commandText = "ADDPFM FILE(" + libraryName + "/" + fileName + ") MBR(" + memberName
                + ")" + " TEXT('Member " + memberName + "')" + " SRCTYPE(" + sourceType + ")";

        // Enable calling CL commands
        CommandCall command = new CommandCall(remoteServer);
        try {
            // Run the command ADDPFM to create the member
            command.run(commandText);

            // Get messages from the command if any
            AS400Message[] as400MessageList = command.getMessageList();
            String msgType;
            // Send all messages from the command. After ESCAPE message - return.
            for (AS400Message as400Message : as400MessageList) {
                if (as400Message.getType() == AS400Message.ESCAPE) {
                    msgType = "Error";
                    row = msgType + ": message from the ADDPFM command is " + as400Message.getID() + " "
                            + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages(true);
                    return;
                } else {                    
                    msgType = "Info";
                    row = msgType + ": message from the ADDPFM command is " + as400Message.getID() + " "
                            + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages(true);
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();

            row = "Error: Creating source member  " + ifsFile.toString() + " - " + exc.toString()
                    + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(true);
        }

        row = "Comp: Source member " + ifsFile.toString() + "  was created in  " + libraryName + "/" + fileName
                + ".";
        mainWindow.msgVector.add(row);
        mainWindow.showMessages(true);
    }


    /**
     * Extract individual names (libraryName, fileName, saveFileName, memberName) from the
     * AS400 IFS path.
     *
     * @param as400PathString
     */
    protected void extractNamesFromIfsPath(String as400PathString) {
        try {
            qsyslib = "/QSYS.LIB/";
            if (as400PathString.startsWith(qsyslib) && as400PathString.length() > qsyslib.length()) {
                libraryName = as400PathString.substring(as400PathString.indexOf("/QSYS.LIB/") + 10, as400PathString.lastIndexOf(".LIB"));
                if (as400PathString.length() > qsyslib.length() + libraryName.length() + 5) {
                    if (as400PathString.contains(".FILE")) {
                        fileName = as400PathString.substring(qsyslib.length() + libraryName.length() + 5, as400PathString.lastIndexOf(".FILE"));
                        if (as400PathString.endsWith(".MBR")) {
                            memberName = as400PathString.substring(as400PathString.lastIndexOf("/") + 1, as400PathString.lastIndexOf(".MBR"));
                        }
                    }
                }
            }
        } catch (Exception exc) {
            System.out.println("as400PathString: " + as400PathString);
            exc.printStackTrace();
        }
    }
    
}
