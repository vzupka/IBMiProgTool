package copyfiles;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.SaveFile;
import java.awt.Cursor;
import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.TreeMap;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author vzupka
 */
public class CreateAndDeleteInIBMi extends SwingWorker<String, String> {

    AS400 remoteServer;
    MainWindow mainWindow;
    IFSFile ifsFile;
    String row;
    String methodName;

    String qsyslib;
    String libraryName;
    String fileName;
    String saveFileName;
    String memberName;

    TreeMap<String, String> sourceFilesAndTypes;
    
    Path parPath = Paths.get(System.getProperty("user.dir"), "paramfiles", "Parameters.txt");
    String encoding = System.getProperty("file.encoding", "UTF-8");
    String ibmCcsid;
    int ibmCcsidInt;

    String sourceRecordLength;

    int currentX;
    int currentY;

    Properties properties;

    /*
    *  Constructor
    */
    CreateAndDeleteInIBMi(AS400 remoteServer, IFSFile ifsFile, MainWindow mainWindow,
            String methodName, int currentX, int currentY) {
        this.remoteServer = remoteServer;
        this.ifsFile = ifsFile;
        this.mainWindow = mainWindow;
        this.methodName = methodName;
        this.currentX = currentX;
        this.currentY = currentY;

        properties = new Properties();
        try {
            BufferedReader infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
            properties.load(infile);
            infile.close();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        ibmCcsid = properties.getProperty("IBM_CCSID");
        if (!ibmCcsid.equals("*DEFAULT")) {
            try {
                ibmCcsidInt = Integer.parseInt(ibmCcsid);
            } catch (Exception exc) {
                exc.printStackTrace();
                ibmCcsid = "500";
                ibmCcsidInt = 500;
            }
        } 
    }

    @Override
    public String doInBackground() {
        createAndDeleteInIBMi(currentX, currentY);
        return row;
    }

    /**
     * Concludes the SwingWorker background task getting the message text (task's
     * result).
     */
    @Override
    @SuppressWarnings("UseSpecificCatch")
    public void done() {
        try {
            row = get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates message by "published" intermediate messages
     *
     * @param strings
     */
    //    @Override
    //    protected void process(List<String> strings) {
    // empty method body
    //    }
    /**
     * Decision what method to call.
     *
     * @param currentX
     * @param currentY
     */
    @SuppressWarnings("UseSpecificCatch")
    protected void createAndDeleteInIBMi(int currentX, int currentY) {

        // Set wait-cursor (rotating wheel?)
        //mainWindow.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        if (methodName.equals("createIfsDirectory")) {
            createIfsDirectory();
        }
        if (methodName.equals("createIfsFile")) {
            createIfsFile();
        }
        if (methodName.equals("createSourcePhysicalFile")) {
            createSourcePhysicalFile();
        }
        if (methodName.equals("createSourceMember")) {
            createSourceMember();
        }
        if (methodName.equals("createSaveFile")) {
            createSaveFile();
        }
        if (methodName.equals("clearSaveFile")) {
            clearSaveFile();
        }
        if (methodName.equals("deleteSourceMember")) {
            deleteSourceMember();
        }
        if (methodName.equals("deleteSourcePhysicalFile")) {
            deleteSourcePhysicalFile();
        }
        if (methodName.equals("deleteIfsObject")) {
            deleteIfsObject();
        }
        if (methodName.equals("deleteSaveFile")) {
            deleteSaveFile();
        }
        if (methodName.equals("copyLibrary")) {
            copyLibrary();
        }
        if (methodName.equals("clearLibrary")) {
            clearLibrary();
        }
        if (methodName.equals("deleteLibrary")) {
            deleteLibrary();
        }

        // Change cursor to default
        mainWindow.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        // Remove message scroll listener (cancel scrolling to the last message)
        mainWindow.scrollMessagePane.getVerticalScrollBar()
                .removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
    }

    /**
     * Create IFS directory.
     */
    protected void createIfsDirectory() {

        // Set default IBM CCSID for *DEFAULT value
        if (ibmCcsid.equals("*DEFAULT")) {
            ibmCcsid = "819"; // Default for IFS directory: ASCII ISO-8859-1, Latin Alphabet No. 1
        }
        
        // Enable calling CL commands
        CommandCall cmdCall = new CommandCall(remoteServer);

        // "false" stands for not changing result to upper case
        String directoryName = new GetTextFromDialog("CREATE NEW DIRECTORY")
                .getTextFromDialog("Parent directory", "New directory name", mainWindow.rightPathString + "/", "", false, currentX, currentY);

        // User canceled creating the directory
        if (directoryName == null) {
            return;
        }
        if (directoryName.isEmpty()) {
            directoryName = "New directory";
        }
        try {
            // Get path to the newly created directory by adding its name to the parent directory path
            ifsFile = new IFSFile(remoteServer, mainWindow.rightPathString + "/" + directoryName);
            // Create new directory
            ifsFile.mkdir();

            // String for command CHGATR to set CCSID attribute of the new directory
            String command_CHGATR = "CHGATR OBJ('" + mainWindow.rightPathString + "/" + directoryName
                    + "') ATR(*CCSID) VALUE(" + ibmCcsid + ") SUBTREE(*NONE)";
            // Perform the command
            cmdCall.run(command_CHGATR);

            // Get messages from the command if any
            AS400Message[] as400MessageList = cmdCall.getMessageList();
            // Send all messages from the command. After ESCAPE message - return.
            for (AS400Message as400Message : as400MessageList) {
                if (as400Message.getType() == AS400Message.ESCAPE) {
                    row = "Error: Change CCSID attribute with command CHGATR   -  "
                            + as400Message.getID() + " " + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                } else {
                    row = "Info: Change CCSID attribute with command CHGATR  -  " + as400Message.getID()
                            + " " + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                }
            }

            row = "Comp: IFS directory  " + ifsFile.toString() + "  created in directory  "
                    + mainWindow.rightPathString + "  -  CCSID " + ibmCcsid + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
        } catch (Exception exc) {
            exc.printStackTrace();

            row = "Error:" + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
        }
    }

    /**
     * Create IFS file.
     */
    @SuppressWarnings("UseSpecificCatch")
    protected void createIfsFile() {
        
        // Enable calling CL commands
        CommandCall cmdCall = new CommandCall(remoteServer);
        
        // Set default IBM CCSID for *DEFAULT value
        if (ibmCcsid.equals("*DEFAULT")) {
            ibmCcsid = "819"; // Default for a new IFS file: ASCII ISO-8859-1, Latin Alphabet No. 1
        }

        // "false" stands for not changing result to upper case
        String fileName = new GetTextFromDialog("CREATE NEW FILE")
                .getTextFromDialog("Parent directory", "New file name", mainWindow.rightPathString
                + "/", "", false, currentX, currentY);

        // User canceled creating the directory
        if (fileName == null) {
            return;
        }
        if (fileName.isEmpty()) {
            fileName = "New file";
        }
        try {
            // Get path to the newly created directory by adding its name to the parent directory path
            ifsFile = new IFSFile(remoteServer, mainWindow.rightPathString + "/" + fileName);

            // Create new empty file
            ifsFile.createNewFile();

            // String for command CHGATR to set CCSID attribute of the new directory
            String command_CHGATR = "CHGATR OBJ('" + mainWindow.rightPathString + "/" + fileName
                    + "') ATR(*CCSID) VALUE(" + ibmCcsid + ") SUBTREE(*NONE)";
            // Perform the command
            cmdCall.run(command_CHGATR);

            // Get messages from the command if any
            AS400Message[] as400MessageList = cmdCall.getMessageList();
            // Send all messages from the command. After ESCAPE message - return.
            for (AS400Message as400Message : as400MessageList) {
                if (as400Message.getType() == AS400Message.ESCAPE) {
                    row = "Error: Change CCSID attribute with command CHGATR   -  "
                            + as400Message.getID() + " " + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                } else {
                    row = "Info: Change CCSID attribute with command CHGATR  -  " + as400Message.getID()
                            + " " + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                }
            }

            row = "Comp: IFS file  " + ifsFile.toString() + "  created in directory  "
                    + mainWindow.rightPathString + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
        } catch (Exception exc) {
            exc.printStackTrace();

            row = "Error:" + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
        }
    }

    /**
     * Create Source Member.
     */
    protected void createSourceMember() {
/*        
        
        sourceFilesAndTypes = new TreeMap<>();
        // Table of Standard Source Physical File Names (keys) and default Source Types (values)
        sourceFilesAndTypes.put("QCLSRC", "CLLE");
        sourceFilesAndTypes.put("QDDSSRC", "DSPF");
        sourceFilesAndTypes.put("QRPGLESRC", "RPGLE");
        sourceFilesAndTypes.put("QRPGSRC", "RPG");
        sourceFilesAndTypes.put("QCBLLESRC", "CBLLE");
        sourceFilesAndTypes.put("QCBLSRC", "CBL");
        sourceFilesAndTypes.put("QCMDSRC", "CMD");
        sourceFilesAndTypes.put("QCSRC", "C");
        sourceFilesAndTypes.put("QTBLSRC", "TBL");

        // Get library name from the IFS path string
        extractNamesFromIfsPath(mainWindow.rightPathString);
        // "true" stands for changing result to upper case
        memberName = new GetTextFromDialog("CREATE NEW SOURCE MEMBER")
                .getTextFromDialog("Library", "Source member name", libraryName, "", true, currentX, currentY);
        if (memberName == null) {
            return;
        }
        if (memberName.isEmpty()) {
            memberName = "@NEWMEMBER";
        }
        
//      String sourceType = getDefaultSourceType(fileName);

        // Set source type from the combo box located in MainWindow
        String sourceType = (String) mainWindow.sourceTypeComboBox.getSelectedItem();

        // Build command ADDPFM to create a member in the source physical file
        String commandText = "ADDPFM FILE(" + libraryName + "/" + fileName + ") MBR(" + memberName
                + ")" + " TEXT('Member " + memberName + "')" + " SRCTYPE(" + sourceType + ")";

        // Enable calling CL commands
        CommandCall command = new CommandCall(remoteServer);
        try {
            // Run the command
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
                    mainWindow.showMessages();
                    return;
                } else {
                    msgType = "Info";
                    row = msgType + ": message from the ADDPFM command is " + as400Message.getID() + " "
                            + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();

            row = "Error: Creating source member  " + ifsFile.toString() + " - " + exc.toString()
                    + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
        }
    */
    }

    /**
     * Create Source Physical File.
     */
    protected void createSourcePhysicalFile() {
        
        // Set default IBM CCSID for *DEFAULT value
        if (ibmCcsid.equals("*DEFAULT")) {
            ibmCcsid = "500"; // Default for a new Source Physical File: EBCDIC ISO-8859-1, Latin Alphabet No. 1
        }        

        // Get library name from the IFS path string
        extractNamesFromIfsPath(mainWindow.rightPathString);
        // "true" stands for changing result to upper case
        String sourceFileName = new GetTextFromDialog("CREATE NEW SOURCE PHYSICAL FILE")
                .getTextFromDialog("Library", "Source physical file name", libraryName, "", true, currentX, currentY);
        if (sourceFileName == null) {
            return;
        }
        if (sourceFileName.isEmpty()) {
            sourceFileName = "QRPGLESRC";
        }
        // Get and adjust source record length
        sourceRecordLength = (String) properties.get("SOURCE_RECORD_LENGTH");
        // The property contains all digits. It was made certain when the user entered the value.
        int sourceRecordLengthInt = Integer.parseInt(sourceRecordLength);
        // Source record must contain 12 byte prefix to data line: sequence number (6) and date (6)
        sourceRecordLengthInt += 12;

        // Build command CRTSRCPF to create a source physical file with certain CCSID in the library
        String commandText = "CRTSRCPF FILE(" + libraryName + "/" + sourceFileName + ") " + "RCDLEN("
                + sourceRecordLengthInt + ") CCSID(" + ibmCcsid + ")";
        // Enable calling CL commands
        CommandCall command = new CommandCall(remoteServer);
        try {
            // Run the command
            command.run(commandText);

            // Get messages from the command if any
            AS400Message[] as400MessageList = command.getMessageList();
            String msgType;
            // Send all messages from the command. After ESCAPE message - return.
            for (AS400Message as400Message : as400MessageList) {
                if (as400Message.getType() == AS400Message.ESCAPE) {
                    msgType = "Error";
                    row = msgType + ": message from the CRTSRCPF command is " + as400Message.getID()
                            + " " + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                    return;
                } else {
                    msgType = "Info";
                    row = msgType + ": message from the CRTSRCPF command is " + as400Message.getID()
                            + " " + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();

            row = "Error: Creating source physical file  " + ifsFile.toString() + " - "
                    + exc.toString() + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
            return;
        }

        row = "Comp: Source physical file  " + sourceFileName + "  was created in  " + libraryName
                + ".";
        mainWindow.msgVector.add(row);
        mainWindow.showMessages();
    }

    /**
     * Create Save File.
     */
    @SuppressWarnings("UseSpecificCatch")
    protected void createSaveFile() {

        extractNamesFromIfsPath(mainWindow.rightPathString);
        // "true" stands for changing result to upper case
        saveFileName = new GetTextFromDialog("CREATE NEW SAVE FILE")
                .getTextFromDialog("Library", "Save file name", libraryName, "", true, currentX, currentY);
        if (saveFileName == null) {
            return;
        }
        try {
            SaveFile saveFile = new SaveFile(remoteServer, libraryName, saveFileName);
            saveFile.create();

            row = "Comp: Save file  " + saveFileName + "  was created in library  " + libraryName
                    + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
        } catch (Exception exc) {
            exc.printStackTrace();

            row = "Error at creating save file  " + libraryName + "/" + saveFileName
                    + ".  System message is:  " + exc.toString() + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
        }
    }

    /**
     * Clear Save File.
     */
    @SuppressWarnings("UseSpecificCatch")
    protected void clearSaveFile() {

        extractNamesFromIfsPath(mainWindow.rightPathString);
        try {
            SaveFile saveFile = new SaveFile(remoteServer, libraryName, saveFileName);
            saveFile.clear();

            row = "Comp: Save file  " + libraryName + "/" + saveFileName + "  was cleared.";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(false);
        } catch (Exception exc) {
            exc.printStackTrace();

            row = "Error clearing save file  " + mainWindow.rightPathString + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(false);
        }

    }

    /**
     * Delete Source Member.
     */
    @SuppressWarnings("UseSpecificCatch")
    protected void deleteSourceMember() {
        // Source member was recognized as file with path ending with .MBR

        extractNamesFromIfsPath(mainWindow.rightPathString);

        try {
            // Delete  the member
            String ifs = ifsFile.toString();
            if (!ifsFile.toString().endsWith(".MBR")) {  // if member name ends with ".sourcetype" instaad of .MBR
                ifs = ifs.substring(0, ifs.lastIndexOf(".") + 1) + "MBR";  // the source type is replaced by .MBR for deletion
                ifsFile = new IFSFile(remoteServer, ifs);
                memberName = ifs.substring(ifs.indexOf(".FILE/") + 6);
            }
            //System.out.println(ifs);
            ifsFile.delete();

            row = "Comp: Source member " + libraryName + "/" + fileName + "(" + memberName
                    + ")  was deleted.";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();

            // PARENT NODE of deleted node will be reloaded and the deleted node will disappear from the tree.
            // Get parent path string
            //mainWindow.rightPathString = mainWindow.rightPathString.substring(0, mainWindow.rightPathString.lastIndexOf("/"));
            // Get parent node
            //mainWindow.rightNode = (DefaultMutableTreeNode) mainWindow.rightNode.getParent();
            //mainWindow.showMessages();

        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error: Source member  " + ifsFile.toString() + " - " + exc.toString() + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
        }
    }

    /**
     * Delete Source Physical File.
     */
    protected void deleteSourcePhysicalFile() {

        // First, show initial message "deleting . . ."
        extractNamesFromIfsPath(ifsFile.toString());

        try {
            // Source physical file is an IFS directory: 
            // Delete all members and the file 
            if (ifsFile.isSourcePhysicalFile()) {
                // Get list of members                
                IFSFile[] ifsFiles = ifsFile.listFiles();

                for (IFSFile member : ifsFiles) {

                    extractNamesFromIfsPath(member.toString());

                    // Delete member 
                    member.delete();

                    row = "Info: Source physical file member " + libraryName + "/" + fileName + "("
                            + memberName + ")  was deleted.";
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                }

                // Finally delete the directory (members' parent)
                extractNamesFromIfsPath(ifsFile.toString());

                // Delete source file
                ifsFile.delete();

                row = "Comp: Source physical file  " + libraryName + "/" + fileName + "  was deleted.";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();

                // PARENT NODE of deleted node will be reloaded and the deleted node will disappear from the tree.
                // Get parent path string
                //mainWindow.rightPathString = mainWindow.rightPathString.substring(0, mainWindow.rightPathString.lastIndexOf("/"));
                // Get parent node
                //mainWindow.rightNode = (DefaultMutableTreeNode) mainWindow.rightNode.getParent();
                //mainWindow.showMessages();
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error: Source physical file  " + ifsFile.toString() + " - " + exc.toString() + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
        }
    }

    /**
     * Delete Save File.
     */
    @SuppressWarnings("UseSpecificCatch")
    protected void deleteSaveFile() {

        extractNamesFromIfsPath(mainWindow.rightPathString);
        try {
            // Save file was recognized as type .FILE, subtype SAVF.
            SaveFile saveFile = new SaveFile(remoteServer, libraryName, saveFileName);
            saveFile.delete();

            row = "Comp: Save file  " + libraryName + "/" + saveFileName + "  was deleted.";
            mainWindow.msgVector.add(row);

            // PARENT NODE of deleted node will be reloaded and the deleted node will disappear from the tree.
            // Get parent path string
            mainWindow.rightPathString = mainWindow.rightPathString.substring(0, mainWindow.rightPathString.lastIndexOf("/"));
            // Get parent node
            mainWindow.rightNode = (DefaultMutableTreeNode) mainWindow.rightNode.getParent();
            mainWindow.showMessages();
        } catch (Exception exc) {
            exc.printStackTrace();

            row = "Error deleting save file  " + mainWindow.rightPathString + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
        }
    }

    /**
     * Delete general IFS object (file or directory).
     */
    protected void deleteIfsObject() {
        // Delete IFS object (a file or a directory with all nested files and directories).
        try {
            if (!ifsFile.isDirectory()) {
                // Simple file:
                // ------------
                // Delete the file
                ifsFile.delete();

                row = "Comp: IFS file " + ifsFile.toString() + "  was deleted.";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
            } else { //
                // Directory:
                // ----------
                // Delete all nested directories and files
                walkIfsDirectory_DeleteNestedDirectories(mainWindow.rightPathString);

                row = "Comp: IFS directory " + ifsFile.toString() + "  was deleted.";
                mainWindow.msgVector.add(row);

                // PARENT NODE of deleted node will be reloaded and the deleted node will disappear from the tree.
                // Get parent node
                mainWindow.rightNode = (DefaultMutableTreeNode) mainWindow.rightNode.getParent();
                mainWindow.showMessages();

                // Remove message scroll listener (cancel scrolling to the last message)
                mainWindow.scrollMessagePane.getVerticalScrollBar()
                        .removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
            }
        } catch (Exception exc) {
            exc.printStackTrace();

            row = "Error: IFS object " + ifsFile.toString() + " - " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
        }
    }

    /**
     * Walk through IFS directory recursively to delete nested directories and
     * files.
     *
     * @param ifsPathString
     */
    protected void walkIfsDirectory_DeleteNestedDirectories(String ifsPathString) {

        // Create IFSFile object representing an IFS object (directory or file)
        IFSFile ifsDirectory = new IFSFile(remoteServer, ifsPathString);
        try {
            // Get list of objects in the current directory
            IFSFile[] inObjects = ifsDirectory.listFiles();
            // First delete files in all nested directories walking through directories (going into recursion)
            for (IFSFile inObject : inObjects) {
                String inPathName = inObject.toString();
                try {
                    // Path to a single file in the directory
                    // - delete the file if it exists
                    if (!inObject.isDirectory()) {
                        if (inObject.exists()) {

                            row = "Info: Deleting IFS file " + inObject + ".";
                            mainWindow.msgVector.add(row);
                            mainWindow.showMessages();
                            inObject.delete();
                        }
                    }
                    // Path to a directory in the directory
                    // - call this method recursively to delete remaining files in all nested directories
                    if (inObject.isDirectory()) {
                        // Recursive call with different IFS path 
                        walkIfsDirectory_DeleteNestedDirectories(inPathName);
                    }
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
            // After deleting files delete all directories (going back from recursion)            

            row = "Info: Deleting IFS directory " + ifsDirectory + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
            ifsDirectory.delete();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    /**
     * Copy library
     */
    protected void copyLibrary() {
        extractNamesFromIfsPath(mainWindow.rightPathString);

        // "false" stands for not changing result to upper case
        String newLibraryName = new GetTextFromDialog("COPY LIBRARY")
                .getTextFromDialog("Library name", "New library name", libraryName, "", true, currentX, currentY);
        // Enable calling CL commands
        CommandCall command = new CommandCall(remoteServer);

        // Build command CPYLIB to copy the library
        String commandText = "CPYLIB FROMLIB(" + libraryName + ") TOLIB(" + newLibraryName + ") CRTLIB(*YES) ";

        try {
            // Run the command
            command.run(commandText);

            // Get messages from the command if any
            AS400Message[] as400MessageList = command.getMessageList();
            String msgType;
            // Send all messages from the command. After ESCAPE message - return.
            for (AS400Message as400Message : as400MessageList) {
                if (as400Message.getType() == AS400Message.ESCAPE) {
                    msgType = "Error";
                    row = msgType + ": message from the CPYLIB command is  " + as400Message.getID()
                            + " " + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                    return;
                } else {
                    msgType = "Info";
                    row = msgType + ": message from the CPYLIB command is  " + as400Message.getID()
                            + " " + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();

            row = "Error: Copying library  " + libraryName + "  -  " + exc.toString() + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
            return;
        }

        row = "Comp: Library  " + libraryName + "  was copied to  " + newLibraryName + ".";
        mainWindow.msgVector.add(row);

        // PARENT NODE of deleted node will be reloaded and the deleted node will disappear from the tree.
        // Get parent path string
        mainWindow.rightPathString = mainWindow.rightPathString
                .substring(0, mainWindow.rightPathString.lastIndexOf("/"));
        // Get parent node
        mainWindow.rightNode = (DefaultMutableTreeNode) mainWindow.rightNode.getParent();
        mainWindow.showMessages();

    }

    /**
     * Clear library
     */
    protected void clearLibrary() {
        extractNamesFromIfsPath(mainWindow.rightPathString);

        // Enable calling CL commands
        CommandCall command = new CommandCall(remoteServer);

        // Build command CLRLIB to clear the library
        String commandText = "CLRLIB LIB(" + libraryName + ")";

        try {
            // Run the command
            command.run(commandText);

            // Get messages from the command if any
            AS400Message[] as400MessageList = command.getMessageList();
            String msgType;
            // Send all messages from the command. After ESCAPE message - return.
            for (AS400Message as400Message : as400MessageList) {
                if (as400Message.getType() == AS400Message.ESCAPE) {
                    msgType = "Error";
                    row = msgType + ": message from the CLRLIB command is  " + as400Message.getID()
                            + " " + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                    return;
                } else {
                    msgType = "Info";
                    row = msgType + ": message from the CLRLIB command is  " + as400Message.getID()
                            + " " + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();

            row = "Error: Clearing library  " + libraryName + "  -  " + exc.toString() + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
            return;
        }

        row = "Comp: Library  " + libraryName + "  was cleared.";
        mainWindow.msgVector.add(row);
        mainWindow.showMessages();
    }

    /**
     * Delete library
     */
    protected void deleteLibrary() {
        extractNamesFromIfsPath(mainWindow.rightPathString);

        // Enable calling CL commands
        CommandCall command = new CommandCall(remoteServer);

        // Build command DLTLIB to delete the library
        String commandText = "DLTLIB LIB(" + libraryName + ")";

        try {
            // Run the command
            command.run(commandText);

            // Get messages from the command if any
            AS400Message[] as400MessageList = command.getMessageList();
            String msgType;
            // Send all messages from the command. After ESCAPE message - return.
            for (AS400Message as400Message : as400MessageList) {
                if (as400Message.getType() == AS400Message.ESCAPE) {
                    msgType = "Error";
                    row = msgType + ": message from the DLTLIB command is  " + as400Message.getID()
                            + " " + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                    return;
                } else {
                    msgType = "Info";
                    row = msgType + ": message from the DLTLIB command is  " + as400Message.getID()
                            + " " + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();

            row = "Error: Deleting library  " + libraryName + "  -  " + exc.toString() + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
            return;
        }

        row = "Comp: Library  " + libraryName + "  was deleted.";
        mainWindow.msgVector.add(row);
        mainWindow.showMessages();
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
                    } else if (as400PathString.endsWith(".SAVF")) {
                        saveFileName = as400PathString.substring(as400PathString.lastIndexOf("/") + 1, as400PathString.lastIndexOf(".SAVF"));
                    }
                }
            }
        } catch (Exception exc) {
            fileName = "";
            saveFileName = "";
            System.out.println("as400PathString: " + as400PathString);
            exc.printStackTrace();
        }
    }
    
    /**
     * Get default source type for standard source physical file name (QCLSRC,
     * QRPGLESRC, ...)
     *
     * @param sourceFileName
     * @return
     */
/*
    protected String getDefaultSourceType(String sourceFileName) {
        String sourceType;
        sourceType = sourceFilesAndTypes.get(sourceFileName);
        if (sourceType == null) {
            sourceType = "TXT";
        }
        return sourceType;
    }
*/
}
