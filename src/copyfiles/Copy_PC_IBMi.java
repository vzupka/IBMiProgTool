package copyfiles;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400FTP;
import com.ibm.as400.access.AS400FileRecordDescription;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.IFSFileOutputStream;
import com.ibm.as400.access.Record;
import com.ibm.as400.access.RecordFormat;
import com.ibm.as400.access.SaveFile;
import com.ibm.as400.access.SequentialFile;
import java.awt.Cursor;
import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * This class performs copying files or directories from PC to IBM i.
 *
 * @author Vladimír Župka, 2016
 */
public class Copy_PC_IBMi {

    AS400 remoteServer;
    String sourcePathString;
    String targetPathString;
    MainWindow mainWindow;

    String sourcePathStringPrefix;
    String row = new String();
    String msgText;
    String qsyslib;
    String libraryName;
    String fileName;
    String saveFileName;
    String classFileName;
    String memberName;

    BufferedReader inFile;
    SequentialFile outSeqFile;
    String textLine;
    final String NEW_LINE = "\n";

    Properties properties;
    Path parPath = Paths.get(System.getProperty("user.dir"), "paramfiles", "Parameters.txt");
    String encoding = System.getProperty("file.encoding", "UTF-8");

    String userName;
    String pcCharset;
    String ibmCcsid;
    int ibmCcsidInt;

    String pcFileSep; // PC file separator ( / in unix, \ in Windows )

    String msgTextDir;

    boolean atLeastOneErrorInFiles;

    boolean seqAndDatePresent;
    boolean pcFileEmpty;

    boolean nodes = true;
    boolean noNodes = false;

    boolean fromWalk = true;
    boolean notFromWalk = false;

    boolean fromDirectory = true;
    boolean notFromDirectory = false;

    boolean toLibrary = true;
    boolean notToLibrary = false;

    /**
     * Constructor.
     *
     * @param remoteServer
     * @param sourcePathString
     * @param targetPathString
     * @param mainWindow
     */
    Copy_PC_IBMi(AS400 remoteServer, String sourcePathString, String targetPathString, MainWindow mainWindow) {
        this.remoteServer = remoteServer;
        this.sourcePathString = sourcePathString;
        this.targetPathString = targetPathString;
        this.mainWindow = mainWindow;

        // File separator for Windows (\) and unix (/)
        Properties sysProp = System.getProperties();
        if (sysProp.get("os.name").toString().contains("Windows")) {
            pcFileSep = "\\";
        } else {
            pcFileSep = "/";
        }

        properties = new Properties();
        try {
            BufferedReader infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
            properties.load(infile);
            infile.close();
        } catch (Exception exc) {
            exc.printStackTrace();
        }

        userName = properties.getProperty("USERNAME");

        pcCharset = properties.getProperty("PC_CHARSET");
        if (!pcCharset.equals("*DEFAULT")) {
            // If not *DEFAULT - Check if charset is valid
            try {
                Charset.forName(pcCharset);
            } catch (IllegalCharsetNameException
                    | UnsupportedCharsetException charset) {
                // If pcCharset is invalid, take ISO-8859-1
                pcCharset = "ISO-8859-1";
            }
        }

        ibmCcsid = properties.getProperty("IBM_CCSID");
        if (!ibmCcsid.equals("*DEFAULT")) {
            // If not *DEFAULT
            try {
                ibmCcsidInt = Integer.parseInt(ibmCcsid);
            } catch (Exception exc) {
                exc.printStackTrace();
                // If CCSID is invalid, take 500
                ibmCcsid = "500";
                ibmCcsidInt = 500;
            }
        }
    }

    /**
     * Initial method calling further methods for copying from PC to IBM i
     */
    protected void copyingFromPC() {

        if (remoteServer == null) {
            return;
        }

        // PC Paths containing /. or \. are skipped
        if (!sourcePathString.contains(pcFileSep + ".")) {

            // Path prefix is the leading part of the path up to and including the last slash.
            sourcePathStringPrefix = sourcePathString.substring(0, sourcePathString.lastIndexOf(pcFileSep) + 1);
            if (Files.isDirectory(Paths.get(sourcePathString))) {

                // From PC directory
                // -----------------
                copyFromPcDirectory(sourcePathString, targetPathString, sourcePathStringPrefix);
                // Change cursor to default
                mainWindow.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            } else {
                // From simple PC file
                // -------------------
                copyFromPcFile(sourcePathString, targetPathString, notFromWalk);
            }

            // Remove message scroll listener (cancel scrolling to the last message)
            mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
        }
    }

    /**
     * Copying simple PC file to IBMi IFS directory/file or to Source File/Source Member or to Save File
     *
     * @param sourcePathString
     * @param targetPathString
     * @param fromWalk
     * @return
     */
    protected String copyFromPcFile(String sourcePathString, String targetPathString, boolean fromWalk) {

        if (!sourcePathString.contains(pcFileSep + ".")) {
            try {
                // Extract individual names (libraryName, fileName, memberName) from the AS400 IFS path
                extractNamesFromIfsPath(targetPathString);

                // Path to PC file
                Path pcFilePath = Paths.get(sourcePathString);

                // IFS file or directory object
                IFSFile targetPath = new IFSFile(remoteServer, targetPathString);
                String outFileName;

                // PC file to general IFS directory or file:
                // =========================================
                //
                if (!targetPathString.startsWith("/QSYS.LIB")) {

                    if (targetPath.isDirectory()) {
                        //
                        // to IFS directory:
                        // -----------------
                        // IFS file name = IFS directory name + PC file name
                        outFileName = targetPathString + "/" + pcFilePath.getFileName();

                        if (outFileName.endsWith(".savf")) {
                            copyToSaveFile(sourcePathString, outFileName, notToLibrary);
                            return "";
                        }
                    } else {
                        //
                        // to IFS file:
                        // ------------
                        // IFS file name does not change
                        outFileName = targetPathString;

                        // Save file
                        if (outFileName.endsWith(".savf")) {
                            copyToSaveFile(sourcePathString, outFileName, notToLibrary);
                            return "";
                        }
                        // If input PC file ends with .savf, output IFS file must also end with .savf
                        if (sourcePathString.endsWith(".savf") && !outFileName.endsWith(".savf")) {
                            row = "Error: PC file  " + sourcePathString
                                    + "  ending with suffix \".savf\" cannot be copied to IFS file  "
                                    + outFileName + "  with a different suffix.";
                            mainWindow.msgVector.add(row);
                            mainWindow.showMessages();
                            return "ERROR";
                        }
                    }

                    // Create IFS file object
                    IFSFile outFilePath = new IFSFile(remoteServer, outFileName);
                    
                    // If the file exists and is not allowed to be overwritten 
                    if (outFilePath.exists() && !properties.getProperty("OVERWRITE_FILE").equals("Y")) {
                        row = "Error: PC file  " + sourcePathString + "  was NOT copied to the existing file  "
                                + outFileName + ". Overwriting files is not allowed.";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                        return "ERROR";
                    }

                    // If the IFS file does not exist - create one with a CCSID
                    if (!outFilePath.exists()) {
                        outFilePath.createNewFile();
                        if (ibmCcsid.equals("*DEFAULT")) {
                            outFilePath.setCCSID(819); // set CCSID 819 as a default for IFS files
                        } else {
                            outFilePath.setCCSID(ibmCcsidInt); // set CCSID as it is in the IBM i CCSID parameter
                        }
                    }

                    //
                    // Binary copy
                    // -----------
                    //

                    // If both IBM i CCSID and PC charset is *DEFAULT then no data conversion is done
                    if (ibmCcsid.equals("*DEFAULT") && pcCharset.equals("*DEFAULT")) {

                        // Allocate buffer for data
                        ByteBuffer byteBuffer = ByteBuffer.allocate(2000000);

                        // Open output IFS file - SHARED for all users, REWRITE data
                        IFSFileOutputStream ifsOutStream = new IFSFileOutputStream(remoteServer, outFileName, IFSFileOutputStream.SHARE_ALL, false);

                        // Open the input PC file
                        FileChannel fileChannel = (FileChannel) Files.newByteChannel(pcFilePath);

                        // Copy PC file to IFS file with byte buffer
                        int bytesRead = fileChannel.read(byteBuffer);
                        while (bytesRead > 0) {
                            ifsOutStream.write(byteBuffer.array(), 0, bytesRead);
                            byteBuffer.rewind(); // Set start of buffer to read next bytes into
                            bytesRead = fileChannel.read(byteBuffer);
                        }
                        // Close files
                        ifsOutStream.close();
                        fileChannel.close();

                        if (fromWalk) {
                            row = "Info: PC file  " + sourcePathString + "  was copied binary to IFS file  " + outFileName + ".";
                        } else {
                            row = "Comp: PC file  " + sourcePathString + "  was copied binary to IFS file  " + outFileName + ". "
                                    + "CCSID " + outFilePath.getCCSID();
                        }
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();

                        return "";
                    }

                    // Convert CCSID from text to integer
                    try {
                        ibmCcsidInt = Integer.parseInt(ibmCcsid);
                    } catch (NumberFormatException exc) {
                        exc.printStackTrace();
                        // If invalid, take IFS default CCSID 819
                        ibmCcsid = "819";
                        ibmCcsidInt = 819;
                    }

                    //
                    // No conversion of text files
                    // -------------
                    //
                    // If both sides have corresponding Unicode or single byte European character sets
                    // no conversion id necessary and transfer is faster.
                    if (pcCharset.toUpperCase().equals("UTF-8") && ibmCcsid.equals("1208")
                            || pcCharset.toUpperCase().equals("UTF-16") && ibmCcsid.equals("1200")
                            || pcCharset.toUpperCase().equals("UTF-16") && ibmCcsid.equals("13488")
                            || pcCharset.toUpperCase().equals("WINDOWS-1250") && ibmCcsid.equals("1250") // ASCII Windows
                            || pcCharset.toUpperCase().equals("WINDOWS-1251") && ibmCcsid.equals("1251") // ASCII Windows
                            || pcCharset.toUpperCase().equals("CP1250") && ibmCcsid.equals("1250") // ASCII Windows
                            || pcCharset.toUpperCase().equals("CP1251") && ibmCcsid.equals("1251") // ASCII Windows
                            || pcCharset.toUpperCase().equals("ISO-8859-1") && ibmCcsid.equals("819") // ASCII Latin-1
                            || pcCharset.toUpperCase().equals("ISO-8859-1") && ibmCcsid.equals("858") // ASCII Latin-1
                            || pcCharset.toUpperCase().equals("ISO-8859-2") && ibmCcsid.equals("912") // ASCII Latin-2
                            || pcCharset.toUpperCase().equals("IBM500") && ibmCcsid.equals("500") // EBCDIC Latin-1
                            || pcCharset.toUpperCase().equals("CP500") && ibmCcsid.equals("500") // EBCDIC Latin-1
                            || pcCharset.toUpperCase().equals("IBM870") && ibmCcsid.equals("870") // EBCDIC Latin-2
                            ) {

                        // Allocate buffer for data
                        ByteBuffer byteBuffer = ByteBuffer.allocate(2000000);

                        // Open output IFS file - SHARED for all users, REWRITE data
                        //
                        IFSFileOutputStream ifsOutStream = new IFSFileOutputStream(remoteServer, outFileName, IFSFileOutputStream.SHARE_ALL, false);
                        // Force this CCSID as an attribute to the output IFS file
                        outFilePath.setCCSID(ibmCcsidInt);

                        // Open the input PC file
                        FileChannel fileChannel = (FileChannel) Files.newByteChannel(pcFilePath);

                        // Copy PC file to IFS file with byte buffer
                        int bytesRead = fileChannel.read(byteBuffer);
                        while (bytesRead > 0) {
                            for (int idx = 0; idx < bytesRead; idx++) {
                                // New line byte must be changed for EBCDIC
                                if (byteBuffer.get(idx) == 0x15) {
                                    byteBuffer.put(idx, (byte) 0x25);
                                }
                            }
                            ifsOutStream.write(byteBuffer.array(), 0, bytesRead);

                            byteBuffer.rewind(); // Set start of buffer to read next bytes into
                            bytesRead = fileChannel.read(byteBuffer);
                        }
                        // Close files
                        ifsOutStream.close();
                        fileChannel.close();

                        if (fromWalk) {
                            row = "Info: PC file  " + sourcePathString + "  was copied unchanged to IFS file  "
                                    + outFileName
                                    + " wigh CCSID " + ibmCcsid + ".";
                        } else {
                            row = "Comp: PC file  " + sourcePathString + "  was copied unchanged to IFS file  "
                                    + outFileName
                                    + " with CCSID " + ibmCcsid + ".";
                        }
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();

                    } else {

                        //
                        // Conversion from pcCharset to ibmCcsid
                        // -------------------------------------
                        //
                        //System.out.println("ibmCcsidInt Conv: " + ibmCcsidInt);
                        byte[] byteArray = new byte[2000000];

                        // Open input
                        BufferedReader bufferedReader;
                        if (pcCharset.equals("*DEFAULT")) {
                            pcCharset = "ISO-8859-1";
                        }
                        // Input will be decoded using PC charset parameter.
                        bufferedReader = Files.newBufferedReader(pcFilePath, Charset.forName(pcCharset));
                        // }
                        // Open output
                        IFSFileOutputStream ifsOutStream = new IFSFileOutputStream(remoteServer, outFileName, IFSFileOutputStream.SHARE_ALL, false, ibmCcsidInt);
                        // Force the CCSID from application parameter to the IFS file as an attribute
                        outFilePath.setCCSID(ibmCcsidInt);

                        // Copy data
                        int nbrOfBytes = 0;
                        textLine = bufferedReader.readLine();
                        while (textLine != null) {
                            textLine += NEW_LINE;
                            // Decide how long in bytes the line is given target encoding.
                            if (ibmCcsid.equals("1200") || ibmCcsid.equals("13488")) {
                                // Get length in bytes for conversion to Unicode 1200 (UTF-16) and 13488 (UCS-2)
                                nbrOfBytes = textLine.getBytes(Charset.forName("UTF-16")).length;
                            } else if (ibmCcsid.equals("1208")) {
                                // Get length in bytes for conversion UTF-8 -> 1208
                                nbrOfBytes = textLine.getBytes(Charset.forName("UTF-8")).length;
                            } else {
                                // Get length of bytes of the text line for single byte characters
                                nbrOfBytes = textLine.length();
                            }

                            // Create text converter with correct length in bytes
                            AS400Text textConverter = new AS400Text(nbrOfBytes, ibmCcsidInt, remoteServer);
                            try {
                                byteArray = textConverter.toBytes(textLine);
                            } catch (Exception exc) {
                                exc.printStackTrace();
                                row = "Error: 1 Copying PC text file  " + sourcePathString + "  to IFS file  "
                                        + targetPathString
                                        + ".  Convert  " + pcCharset + " -> " + ibmCcsid + ".  -  " + exc.toString();
                                mainWindow.msgVector.add(row);
                                mainWindow.showMessages();
                                return "ERROR";
                            }
                            ifsOutStream.write(byteArray);
                            // Read next line
                            textLine = bufferedReader.readLine();
                        }
                        bufferedReader.close();
                        ifsOutStream.close();

                        if (fromWalk) {
                            row = "Info: PC file  " + sourcePathString + "  was copied to IFS file  " + outFileName
                                    + ",  Convert " + pcCharset + " -> " + ibmCcsid + ".";
                        } else {
                            row = "Comp: PC file  " + sourcePathString + "  was copied to IFS file  " + outFileName
                                    + ",  Convert " + pcCharset + " -> " + ibmCcsid + ".";
                        }
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                    }
                    return "";
                } //
                //
                // PC file to a Library object (LIB, SAVF, PF-SRC, member):
                // ========================================================
                else if (targetPathString.endsWith(".LIB")) { //

                    // Default CCSID for Library objects is 500 (EBCDIC Latin-1)
                    if (ibmCcsid.equals("*DEFAULT")) {
                        ibmCcsid = "500";
                        ibmCcsidInt = 500;
                    }

                    //
                    // PC file with suffix .savf to LIBRARY
                    // ------------------------------------
                    if (pcFilePath.getFileName().toString().endsWith(".savf")) {
                        msgText = copyToSaveFile(sourcePathString, targetPathString, toLibrary);
                    } 
                    // PC file without .savf suffix is NOT ALLOWED to copy to LIBRARY!
                    else {
                        row = "Error: PC file  " + sourcePathString
                                + "  without .savf suffix cannot be copied to the library  "
                                + libraryName + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                    }

                // PC file with suffix .savf to SAVE FILE 
                // --------------------------------------
                } else if (targetPathString.contains(".LIB")
                        && targetPathString.endsWith(".SAVF")) {
                    // Target save file should have suffix .FILE in the IFS notation 
                    // but has suffix .SAVF in the visible tree structure.
                    // Create modified path and check if it is real save file.
                    String modifíedTargetString = targetPath.toString().substring(0, targetPath.toString().lastIndexOf(".") + 1) + "FILE";
                    IFSFile modifiedPath = new IFSFile(remoteServer, modifíedTargetString);
                    if (modifiedPath.getSubtype().equals("SAVF")) {
                        // For copy use the original path string
                        msgText = copyToSaveFile(sourcePathString, targetPathString, notToLibrary);
                        if (!msgText.isEmpty()) {
                            row = "Comp: PC file  " + sourcePathString + "  was NOT copied to the save file  "
                                    + libraryName + "/" + fileName + ".";
                            mainWindow.msgVector.add(row);
                            mainWindow.showMessages();
                        }
                    }
                    
                // PC file to SOURCE FILE
                // ----------------------
                } else if (targetPathString.endsWith(".FILE")) {

                    // File ending with .savf is not allowed to source member
                    if (sourcePathString.endsWith(".savf")) {
                        row = "Error: PC file  " + sourcePathString + "  ending with .savf cannot be copied to source file  "
                                + libraryName + "/" + fileName + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                        return "ERROR";
                    }
                    // Insert to source file as a member
                    msgText = copyToSourceFile(sourcePathString, targetPathString);
                    if (!msgText.isEmpty()) {
                        row = "Comp: PC file  " + sourcePathString + "  was NOT copied to the existing source physical file  " // + libraryName + "/" + fileName + "."
                                ;
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                    }
                    
                // PC file to SOURCE MEMBER
                // ------------------------
                } else if (targetPathString.contains(".FILE/")) {
                    if (Files.isDirectory(pcFilePath)) {

                        msgText = copyToSourceMember(sourcePathString, targetPathString, fromDirectory);
                    } else {
                        // File ending with .savf is not allowed to source member
                        if (sourcePathString.endsWith(".savf")) {
                            row = "Error: PC file  " + sourcePathString
                                    + "  ending with .savf cannot be copied to source member  "
                                    + libraryName + "/" + fileName + "/" + memberName + ".";
                            mainWindow.msgVector.add(row);
                            mainWindow.showMessages();
                            return "ERROR";
                        }
                        // Rewrite source member
                        msgText = copyToSourceMember(sourcePathString, targetPathString, notFromDirectory);
                    }
                    if (!msgText.isEmpty()) {
                        row = "Comp: PC file  " + sourcePathString
                                + "  was NOT copied to the existing source physical member  "
                                + libraryName + "/" + fileName + "(" + memberName + ").";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                    }
                }

            } catch (Exception exc) {
                exc.printStackTrace();
                row = "Error: 2 Copying PC file  " + sourcePathString + "  to IFS file  " + targetPathString + ". Convert  "
                        + pcCharset + " -> " + ibmCcsid + ".  -  " + exc.toString();
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
                return "ERROR";
            }
        }
        return msgText;
    }

    /**
     * Copying a PC directory to IFS directory or to Library/Source File.
     *
     * @param sourcePathString
     * @param targetPathString
     * @param sourcePathStringPrefix
     */
    protected void copyFromPcDirectory(String sourcePathString, String targetPathString,
            String sourcePathStringPrefix) {

        // Get object and member names (libraryName, fileName, memberName
        extractNamesFromIfsPath(targetPathString);

        if (!sourcePathString.contains(pcFileSep + ".")) {

            IFSFile targetPath = new IFSFile(remoteServer, targetPathString + "/"
                    + sourcePathString.substring(sourcePathStringPrefix.length()));

            //
            // To IFS (Integrated File System):
            // --------------------------------
            if (!targetPathString.startsWith("/QSYS.LIB")) {

                // Create the first shadow IFS directory
                try {
                    if (!targetPath.exists()) {

                        // Create new directory in target IFS directory with PC file ending name
                        targetPath.mkdir();

                        // Add message text to the message table and show it
                        row = "Info: Directory  " + sourcePathString + "  was created in IFS directory  " + targetPathString
                                + ".";
                        mainWindow.msgVector.add(row);
                    }
                } catch (Exception exc) {
                    exc.printStackTrace();
                    row = exc.toString();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                    return;
                }

                // Create shadow directories in the first shadow IFS directory created above
                msgText = walkPcDirectory_CreateNestedIfsDirectories(remoteServer, sourcePathString, targetPath);
                // Copy PC files to appropriate IFS shadow directories
                msgText = copyPcFilesToIfsDirectories(sourcePathString, targetPath, sourcePathStringPrefix);

                // Construct a message in the message table and show it
                if (msgText.isEmpty()) {
                    row = "Comp: PC directory  "
                            + sourcePathString + "  was copied to IFS directory  " + targetPathString + ".";
                } else {
                    row = "Comp: PC directory  "
                            + sourcePathString + "  was NOT completely copied to IFS directory  " + targetPathString + ".";
                }
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
            }

            // To Library system:
            // ------------------
            if (targetPathString.startsWith("/QSYS.LIB")) {

                if (ibmCcsid.equals("*DEFAULT")) {
                    ibmCcsid = "500";
                }
                
                //
                // To LIBRARY:
                // -----------
                // Create new source physical file in the library and call copyToSourceFile() method
                if (targetPathString.endsWith(".LIB")) {

                    // Extract individual names (libraryName, fileName, memberName) from the AS400 IFS path
                    extractNamesFromIfsPath(targetPathString);
                    // Extract a new file name from the last component of the PC path string.
                    // The file name extracted by the method before was null!
                    fileName = sourcePathString.substring(sourcePathString.lastIndexOf(pcFileSep) + 1);

                    // Create new source physical file
                    String sourceRecordLength = (String) properties.get("SOURCE_RECORD_LENGTH");
                    // The property contains all digits. It was made certain when the user entered the value.
                    int sourceRecordLengthInt = Integer.parseInt(sourceRecordLength);
                    // Source record must contain 12 byte prefix to data line: sequence number (6) and date (6)
                    sourceRecordLengthInt += 12;

                    // Build command CRTSRCPF to create a source physical file with certain CCSID in the library
                    String commandText = "CRTSRCPF FILE(" + libraryName + "/" + fileName + ") "
                            + "RCDLEN(" + sourceRecordLengthInt + ") CCSID(" + ibmCcsid + ")";

                    // Enable calling CL commands
                    CommandCall cmdCall = new CommandCall(remoteServer);

                    try {
                        // Run the command
                        cmdCall.run(commandText);

                        // Get messages from the command if any
                        AS400Message[] as400MessageList = cmdCall.getMessageList();
                        // Send all messages from the command. After ESCAPE message - return.
                        for (AS400Message as400Message : as400MessageList) {
                            if (as400Message.getType() == AS400Message.ESCAPE) {
                                row = "Error: Create source physical file  " + libraryName + "/" + fileName
                                        + " using CRTSRCPF command  -  "
                                        + as400Message.getID() + " " + as400Message.getText();
                                mainWindow.msgVector.add(row);
                                mainWindow.showMessages();
                                return;
                            } else {
                                row = "Info: Create source physical file  " + libraryName + "/" + fileName
                                        + " using CRTSRCPF command  -  "
                                        + as400Message.getID() + " " + as400Message.getText();
                                mainWindow.msgVector.add(row);
                                mainWindow.showMessages();
                            }
                        }
                    } catch (AS400SecurityException | ErrorCompletingRequestException | PropertyVetoException | IOException | InterruptedException exc) {
                        exc.printStackTrace();
                        row = "Error: Copying PC directory  " + sourcePathString + "  to source physical File  "
                                + libraryName + "/" + fileName + "  -  " + exc.toString();
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                        return; // Must return! Could be fatal error (e. g. lock of the source file)!
                    }

                    // Copy members to the new source physical file in a library
                    msgText = copyToSourceFile(sourcePathString, targetPathString + "/" + fileName + ".FILE");

                    if (msgText.isEmpty()) {
                        row = "Comp: PC directory  " + sourcePathString + "  was copied to source physical file  "
                                + libraryName + "/" + fileName + ".";
                    } else {
                        row = "Comp: PC directory  " + sourcePathString
                                + "  was NOT completely copied to source physical file  "
                                + libraryName + "/" + fileName + ".";
                    }
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                }
                
                //
                // To already existing or just created SOURCE PHYSICAL FILE`:
                // ---------------------------------------------------------
                else if (targetPathString.endsWith(".FILE")) {

                    // Copy single PC file to source file to existing member or as a new member
                    msgText = copyToSourceFile(sourcePathString, targetPathString);

                    if (msgText.isEmpty()) {
                        row = "Comp: PC directory  " + sourcePathString + "  was copied to source physical file  "
                                + libraryName + "/" + fileName + ".";
                    } else {
                        row = "Comp: PC directory  " + sourcePathString
                                + "  was NOT completely copied to source physical file  "
                                + libraryName + "/" + fileName + ".";
                    }
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                }
                
                //
                // To existing source physical file MEMBER:
                // ----------------------------------------
                else if (targetPathString.endsWith(".MBR")) {
                    row = "Error: PC directory  "
                            + sourcePathString + "  cannot be copied to a source physical file member  " + libraryName + "/"
                            + fileName + "(" + memberName + ").";
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                }
            }
        }
    }

    /**
     * Walk through PC directory recursively to create shadow directories in IFS target directory. Shadow directories are
     * named by last names of the PC directory paths so that only one-level directories are inserted to the IFS target
     * directory.
     *
     * @param remoteServer
     * @param sourcePathString
     * @param ifsPath
     * @return
     */
    protected String walkPcDirectory_CreateNestedIfsDirectories(AS400 remoteServer, String sourcePathString,
            IFSFile ifsPath) {

        try {
            Stream<Path> stream = Files.list(Paths.get(sourcePathString));

            stream.forEach(inPath -> {
                String pcPathName = inPath.toString();
                // Path to the new shadow directory to be created in IFS
                String newDirPathString = ifsPath.toString() + "/"
                        + pcPathName.substring(pcPathName.lastIndexOf(pcFileSep) + 1);

                IFSFile ifsNewPath = new IFSFile(remoteServer, newDirPathString);

                // Only directories in PC are processed (not single files)
                if (Files.isDirectory(inPath)) {
                    try {
                        if (!ifsNewPath.toString().contains(pcFileSep + ".")) {
                            // Create new shadow IFS directory
                            ifsNewPath.mkdir();
                            // Add message text to the message table and show it
                            row = "Info: Directory  " + pcPathName
                                    + "  was created in IFS directory  " + ifsPath.toString() + ".";
                            mainWindow.msgVector.add(row);
                            mainWindow.showMessages();
                        }
                        // Recursive call with different PC path
                        walkPcDirectory_CreateNestedIfsDirectories(remoteServer, pcPathName, ifsNewPath);
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                }
            });
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return "";
    }

    /**
     * Copying PC files to IFS directories created before (see walkPcDirectory_CreateNestedIfsDirectories method).
     *
     * @param sourcePathString
     * Source PC directory name
     * @param ifsPath
     * Target IFS directory path
     * @param sourcePathStringPrefix
     * @return
     */
    protected String copyPcFilesToIfsDirectories(String sourcePathString, IFSFile ifsPath,
            String sourcePathStringPrefix) {
        msgTextDir = "";
        atLeastOneErrorInFiles = false;

        try {
            if (!sourcePathString.contains(pcFileSep + ".")) {

                // Get list of PC directory objects
                Stream<Path> stream = Files.list(Paths.get(sourcePathString));

                // Process all objects of the list
                stream.forEach(inPath -> {
                    String pcPathName = inPath.toString();
                    String pcFileName = inPath.getFileName().toString();
                    String nextPcFile = sourcePathString + pcFileSep + pcFileName;

                    // Path to the new shadow directory is the target IFS path plus PC file name ending the PC path
                    String newDirPathString = ifsPath.toString() + "/" + pcFileName;

                    // Create object of IFSFile type
                    IFSFile ifsNewPath = new IFSFile(remoteServer, newDirPathString);
                    
                    //
                    // PC directory:
                    // -------------
                    if (Files.isDirectory(inPath)) {
                        if (!pcPathName.contains(pcFileSep + ".")) {
                            // Recursive call with new IFS path object
                            copyPcFilesToIfsDirectories(pcPathName, ifsNewPath, sourcePathStringPrefix);
                        }
                    } else
                        
                    //
                    // Simple PC file:
                    // ---------------
                    // Add PC file name ("leaf" of the PC path) to get file path to the PC file to be copied
                    {
                        if (!nextPcFile.contains(pcFileSep + ".")) {

                            // Copy from PC file to IFS file (that will be created given the new path name)
                            msgTextDir = copyFromPcFile(nextPcFile, ifsNewPath.toString(), fromWalk);

                            if (!msgTextDir.isEmpty()) {
                                atLeastOneErrorInFiles = true;
                            }

                            // Add a message in the message table and show it
                            if (atLeastOneErrorInFiles) {
                                row = "Comp: PC file  " + nextPcFile + "  was NOT copied to IFS directory  "
                                        + ifsNewPath.toString() + ".";
                            } else {
                                row = "Comp: PC file  " + nextPcFile + "  was copied to IFS directory  " + ifsNewPath.toString()
                                        + ".";
                            }
                        }
                    }
                });
                msgText = atLeastOneErrorInFiles ? "ERROR" : "";
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error PC files to IFS: " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
        }
        return msgText;
    }


    /**
     * Copy PC text file to the IBM i source member.
     *
     * @param sourcePathString
     * @param targetPathString
     * @param fromDirectory
     * @return
     */
    protected String copyToSourceMember(String sourcePathString, String targetPathString, boolean fromDirectory) {

        if (ibmCcsid.equals("*DEFAULT")) {
            ibmCcsid = "500";
            ibmCcsidInt = 500;
        }

        // Extract individual names (libraryName, fileName, memberName) from the AS400 IFS path
        extractNamesFromIfsPath(targetPathString);

        if (sourcePathString.endsWith(pcFileSep)) {
            sourcePathString = sourcePathString.substring(0, sourcePathString.lastIndexOf(pcFileSep));
        }
        // Get source type from the PC file name
        String sourceType;

        if (sourcePathString.lastIndexOf(".") > 0) {
            // If file name has postfix with a dot, the posfix will be the source type
            sourceType = sourcePathString.substring(sourcePathString.lastIndexOf(".") + 1);
        } else {
            // If file name does not have a postfix (there is no dot), the source type will be MBR
            sourceType = "MBR";
        }
        // Path to input PC text file
        Path inTextFile = Paths.get(sourcePathString);

        // Path to the output source member
        String outMemberPathString = "/QSYS.LIB/" + libraryName + ".LIB/" + fileName + ".FILE" + "/" + memberName + ".MBR";

        IFSFile ifsMbr = new IFSFile(remoteServer, outMemberPathString);

        String clrPfmCommand;
        String chgPfmCommand;

        // Enable calling CL commands
        CommandCall cmdCall = new CommandCall(remoteServer);

        msgText = "";

        try {

            // Open PC input file regarded as a text file.
            if (pcCharset.equals("*DEFAULT")) {
                // Decode input using its encoding. Ignore PC charset parameter.
                inFile = Files.newBufferedReader(inTextFile);
            } else {
                // Decode input using PC charset parameter.
                inFile = Files.newBufferedReader(inTextFile, Charset.forName(pcCharset));
            }
            try {
                // Read the first text line
                textLine = inFile.readLine();
                // If an error is found in the first line of the file,
                // processing is broken, the error is caught (see catch blocks), a message is reported,
                // and no member is created.
            } catch (IOException exc) {
                exc.printStackTrace();
                row = "Error: Data of the PC file  " + sourcePathString + "  cannot be copied to the source physical file  "
                        + libraryName + "/" + fileName + "  -  " + exc.toString();
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
                msgText = "ERROR";
                return msgText;
            }

            // If the output member already exists and overwrite is not allowed - return
            // -----------------------------------
            if (ifsMbr.exists()
                    && !properties.getProperty("OVERWRITE_FILE").equals("Y")) {
                row = "Info: PC file  " + sourcePathString + "  cannot be copied to the existing source physical member  "
                        + libraryName + "/" + fileName + "(" + memberName + "). Overwriting files is not allowed.";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
                return "ERROR";
            }

            // Otherwise: Member does not exist or overwrite of the existing member is allowed.
            // ---------

            // Decide if the input PC file contains prefix consisting of
            // SEQUENCE NUMBER and DATE information fields in the first 6 + 6 positions.
            try {
                // Non-empty member can be checked for information fields if the line is longer than 12
                if (textLine != null && textLine.length() > 12) {
                    int seqNbr = Integer.parseInt(textLine.substring(0, 6));
                    int date = Integer.parseInt(textLine.substring(6, 12));
                    seqAndDatePresent = true;
                } else {
                    // Otherwise the line cannot have information fields
                    seqAndDatePresent = false;
                }
            } catch (NumberFormatException nfe) {
                seqAndDatePresent = false;
            }

            if (seqAndDatePresent) {
                // Input records contain sequence and data fields
                // ----------------------------------------------

                // Copying is done directly using a sequential file preserving sequence and data field values.

                // Clear physical file member
                clrPfmCommand = "CLRPFM FILE(" + libraryName + "/" + fileName + ") MBR(" + memberName + ")";
                cmdCall.run(clrPfmCommand);

                // Obtain output database file record description
                AS400FileRecordDescription outRecDesc = new AS400FileRecordDescription(remoteServer, outMemberPathString);
                // Retrieve record format from the record description
                RecordFormat[] format = outRecDesc.retrieveRecordFormat();
                // Obtain output record object
                Record outRecord = new Record(format[0]);

                //
                // Note: Now create the member if no error was found in the first text line.
                // -----
                //
                // Create the member (SequentialFile object)
                outSeqFile = new SequentialFile(remoteServer, outMemberPathString);

                // Set the record format (the only one)
                outSeqFile.setRecordFormat(format[0]);

                try {
                    // Open the member
                    outSeqFile.open();

                } catch (com.ibm.as400.access.AS400Exception as400exc) {
                    // as400exc.printStackTrace();
                    // Add new member if open could not be performed (when the member does not exist)
                    // (the second parameter is a text description)
                    // The new member inherits the CCSID from its parent Source physical file
                    outSeqFile.addPhysicalFileMember(memberName, "Source member " + memberName);

                    // Change member to set its Source Type
                    chgPfmCommand = "CHGPFM FILE(" + libraryName + "/" + fileName + ") MBR(" + memberName + ") SRCTYPE("
                            + sourceType + ")";
                    // Perform the CL command
                    cmdCall.run(chgPfmCommand);

                    // Open the new member
                    outSeqFile.open();
                }

                // Process all lines
                while (textLine != null) {
                    // Get lengths of three fields of the source record
                    int lenSEQ = format[0].getFieldDescription("SRCSEQ").getLength();
                    int lenDAT = format[0].getFieldDescription("SRCDAT").getLength();

                    // Set the three field values from the input text line according to their lengths
                    outRecord.setField("SRCSEQ", new BigDecimal(new BigInteger(textLine.substring(0, lenSEQ)), 2));
                    outRecord.setField("SRCDAT", new BigDecimal(textLine.substring(lenSEQ, lenSEQ + lenDAT)));
                    outRecord.setField("SRCDTA", textLine.substring(lenSEQ + lenDAT));
                    try {
                        // Write source record
                        outSeqFile.write(outRecord);
                        // Read next text line
                        textLine = inFile.readLine();
                    } catch (Exception exc) {
                        exc.printStackTrace();
                        row = "Error: 1 Data of the PC file  " + sourcePathString
                                + "  cannot be copied to the source physical file  "
                                + libraryName + "/" + fileName + "  -  " + exc.toString();
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                        msgText = "ERROR";
                        break;
                    }
                }
                // Close files
                inFile.close();
                outSeqFile.close();

            } else {
                
                // Input records DO NOT contain sequence and data fields
                // -----------------------------------------------------

                // Copying is done indirectly using a temporary IFS file in the /home/userName directory.

                // First create an IFS '/home/userName directory if it does not exist
                String home_userName = "/home/" + userName;
                IFSFile ifsDir = new IFSFile(remoteServer, home_userName);
                // Create new directory
                ifsDir.mkdir();

                // String for command CHGATR to set CCSID attribute of the new directory
                String command_CHGATR = "CHGATR OBJ('" + home_userName + ") ATR(*CCSID) VALUE(" + ibmCcsid
                        + ") SUBTREE(*ALL)";
                // Perform the command
                cmdCall.run(command_CHGATR);

                // Create hidden temporary file (with leading dot) in the directory
                String tmp_File = home_userName + "/.tmp" + Timestamp.valueOf(LocalDateTime.now()).toString();
                IFSFile ifsTmpFile = new IFSFile(remoteServer, tmp_File);
                ifsTmpFile.createNewFile();

                // Copy PC file to temporary IFS file
                copyFromPcFile(sourcePathString, tmp_File, notFromWalk);
 
                // Change type suffix to ".MBR" to comvene CPYFRMSTMF command
                String tps = targetPathString.substring(0, targetPathString.lastIndexOf(".")) + ".MBR";
                // Copy data from temporary IFS file to the member. If the member does not exist it is created.
                String commandCpyFrmStmfString = "CPYFRMSTMF FROMSTMF('" + tmp_File + "') TOMBR('"
                        + tps
                        + "') MBROPT(*REPLACE) CVTDTA(*AUTO) STMFCCSID(*STMF) DBFCCSID(*FILE)";
                // Perform the command
                cmdCall.run(commandCpyFrmStmfString);
                
                // Get messages from the command if any
                AS400Message[] as400MessageList = cmdCall.getMessageList();
                // Send all messages from the command. After ESCAPE message - return.
                for (AS400Message as400Message : as400MessageList) {
                    if (as400Message.getType() == AS400Message.ESCAPE) {
                        row = "Error: Copy IFS file  " + sourcePathString + "  to source member  "
                                + targetPathString + "  using command CPYFRMSTMF  -  " + as400Message.getID()
                                + " " + as400Message.getText();
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                        return "ERROR";
                    } else {
                        row = "Info: Copy IFS file  " + sourcePathString + "  to source member  "
                                + targetPathString + "  using command CPYFRMSTMF  -  " + as400Message.getID()
                                + " " + as400Message.getText();
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                    }
                }
                
                // Change member to set its Source Type
                chgPfmCommand = "CHGPFM FILE(" + libraryName + "/" + fileName + ") MBR(" + memberName + ") SRCTYPE("
                        + sourceType + ")";
                // Perform the CL command
                cmdCall.run(chgPfmCommand);

                // Delete the temporary file
                ifsTmpFile.delete();
            }
            row = "Info: PC file  " + sourcePathString + "  was copied to source physical file member  "
                    + libraryName + "/" + fileName + "(" + memberName + "). Convert " + pcCharset + " -> " + ibmCcsid
                    + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();

            if (!msgText.isEmpty()) {
                return "ERROR";
            }
            if (msgText.isEmpty() && !fromDirectory) {
                row = "Comp: PC file  " + sourcePathString + "  was copied to source physical file member  "
                        + libraryName + "/" + fileName + "(" + memberName + "). Convert " + pcCharset + " -> " + ibmCcsid
                        + ".";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
                return "";
            }
            if (!msgText.isEmpty() && !fromDirectory) {
                row = "Comp: PC file  " + sourcePathString + "  was NOT completely copied to source physical file member  "
                        + libraryName + "/" + fileName + "(" + memberName + "). Convert " + pcCharset + " -> " + ibmCcsid
                        + ".";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
                return "ERROR";
            }

        } catch (Exception exc) {
            try {
                inFile.close();
                outSeqFile.close();
            } catch (Exception exce) {
                exce.printStackTrace();
            }
            exc.printStackTrace();
            row = "Error: 3 PC file  " + sourcePathString + "  cannot be copied to the source physical file  "
                    + libraryName + "/" + fileName + "  -  " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
            return "ERROR"; // Must not continue in order not to lock an object
        }
        return "";
    }

    
    /**
     * Copy PC directory or a simple text file to the IBM i source physical file.
     *
     * @param sourcePathString
     * @param targetPathString
     * @return
     */
    protected String copyToSourceFile(String sourcePathString, String targetPathString) {

        mainWindow.rightSelectedPath = mainWindow.rightTree.getPathForLocation(MainWindow.currentX, MainWindow.currentY);

        // Extract individual names (libraryName, fileName, memberName) from the AS400 IFS path
        extractNamesFromIfsPath(targetPathString);

        Path pcPath = Paths.get(sourcePathString);

        if (Files.isDirectory(pcPath)) {

            // From PC directory to Source Physical File:
            // -----------------

            // Copy PC files from directory to Source Physical File
            atLeastOneErrorInFiles = false;
            try {

                Stream<Path> inputStream = Files.list(pcPath);
                pcFileEmpty = true;

                inputStream.forEach(pcFile -> {

                    String pathName = pcFile.toString();
                    String pcFileName = pcFile.getFileName().toString();
                    memberName = pcFileName;

                    if (!pathName.contains(pcFileSep + ".")) {
                        // If the PC file has a suffix after a dot, remove the suffix from member name
                        if (pathName.lastIndexOf(".") > 0) {
                            memberName = pcFileName.substring(0, pcFileName.lastIndexOf("."));
                        }

                        pcFileEmpty = false;

                        // Copy PC file to Source Physical File MEMBER (insert or rewrite)
                        msgTextDir = copyToSourceMember(pathName, targetPathString + "/" + memberName
                                + ".MBR", fromDirectory);
                    }
                });
                inputStream.close();

                if (pcFileEmpty) {
                    msgTextDir = "";
                }

                if (!msgTextDir.isEmpty()) {
                    atLeastOneErrorInFiles = true;
                }
                if (atLeastOneErrorInFiles) {
                    row = "Error: PC directory  " + sourcePathString
                            + "  was NOT completely copied to source physical file  " + libraryName + "/" + fileName + ".";
                } else {
                    row = "Comp: PC directory  " + sourcePathString + "  was copied to source physical file  " + libraryName
                            + "/" + fileName + ".";
                }
                msgText = atLeastOneErrorInFiles ? "ERROR" : "";
                // mainWindow.msgVector.add(row);
                // mainWindow.showMessages();

            } catch (Exception exc) {
                exc.printStackTrace();
                row = "Error: PC file  " + sourcePathString + "  cannot be copied to the source physical file  "
                        + libraryName + "/" + fileName + "  -  " + exc.toString();
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
                return "ERROR";
            }
        } else {

            // From simple PC file to Source Physical File:
            // -------------------
            String pcFileName = pcPath.getFileName().toString();
            memberName = pcFileName.substring(0, pcFileName.lastIndexOf("."));
            msgText = copyToSourceMember(sourcePathString, targetPathString + "/" + memberName + ".MBR", notFromDirectory);
            // Send messages if the PC file was copied directly to an existing source member
            if (msgText.isEmpty()) {
                row = "Comp1: PC file  " + sourcePathString + "  was copied to source physical file member  "
                        + libraryName + "/" + fileName + "(" + memberName + ").";
                // mainWindow.msgVector.add(row);
                // mainWindow.showMessages();
            } else {
                row = "Error: PC file  " + sourcePathString + "  was NOT copied to source physical file member  "
                        + libraryName + "/" + fileName + "(" + memberName + ").";
                // mainWindow.msgVector.add(row);
                // mainWindow.showMessages();
                return msgText;
            }
        }
        return msgText;
    }

    /**
     * Copy PC file to a new or existing Save File
     *
     * @param targetPathString
     * @param sourcePathString
     * @param toLibrary
     * @return
     */
    @SuppressWarnings("UseSpecificCatch")
    protected String copyToSaveFile(String sourcePathString, String targetPathString, boolean toLibrary) {

        // Extract individual names (libraryName, fileName, memberName) from the AS400 IFS path
        extractNamesFromIfsPath(targetPathString);

        String saveFilePathString;

        // Copy to LIBRARY
        if (toLibrary) {
            // Save file name is derived from the PC file name without suffix .savf
            saveFileName = sourcePathString.substring(sourcePathString.lastIndexOf(pcFileSep)
                    + 1, sourcePathString.lastIndexOf(".savf"));
            // Save file path string is derived from the path string of the library by adding the PC file name with suffix
            // .SAVF in upper case
            saveFilePathString = targetPathString + "/"
                    + sourcePathString.substring(sourcePathString.lastIndexOf(pcFileSep) + 1).toUpperCase();

            // Create a new Save File if it does not exist
            try {
                SaveFile saveFile = new SaveFile(remoteServer, libraryName, saveFileName);
                if (!saveFile.exists()) {
                    saveFile.create();
                }
            } catch (Exception exc) {
                exc.printStackTrace();
                row = "Error1: " + exc.toString();
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
                return "ERROR";
            }

            // Copy the PC file to Save file using FTP (File Transfer Protocol)
            AS400FTP ftp = new AS400FTP(remoteServer);
            try {
                // FTP Binary data transfer
                // ftp.setDataTransferType(AS400FTP.BINARY); // not necessary when suffix is .savf
                // FTP Put command
                ftp.put(sourcePathString, saveFilePathString);
                ftp.disconnect();
                row = "Comp: PC file  " + sourcePathString + "  was copied to save file  " + libraryName + "/"
                        + saveFileName + ".";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
                return "";
            } catch (Exception exc) {
                exc.printStackTrace();
                row = "Error: Copying PC file  " + sourcePathString + "  to save file  " + libraryName + "/" + saveFileName
                        + "  failed:  " + exc.toString();
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
                return "ERROR";
            }
        } //
        // Copy to IFS FILE
        else {
            // Save file name is derived from PC file name excluding suffix .savf
            saveFileName = sourcePathString.substring(sourcePathString.lastIndexOf(pcFileSep) + 1);
            if (!sourcePathString.endsWith(".savf")) {
                row = "Error: Copying PC save file  " + sourcePathString
                        + "  ending with suffix \".savf\" cannot be copied to the existing file  " + targetPathString
                        + "  with a different suffix.";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
                return "ERROR";
            } else {
                // Copy the PC file to Save file using FTP (File Transfer Protocol)
                AS400FTP ftp = new AS400FTP(remoteServer);
                try {
                    // FTP Binary data transfer
                    // ftp.setDataTransferType(AS400FTP.BINARY); // not necessary when suffix is .savf
                    // FTP Put command
                    ftp.put(sourcePathString, targetPathString);
                    ftp.disconnect();
                    row = "Comp: PC save file  " + sourcePathString + "  was copied to IFS save file  " + targetPathString
                            + ".";
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                    return "";

                } catch (Exception exc) {
                    exc.printStackTrace();
                    row = "Error: Copying PC save file  " + sourcePathString + "  to IFS save file  " + targetPathString
                            + "  failed:  " + exc.toString();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                    return "ERROR";
                }
            }
        }
    }

    /**
     * Copy PC file to a new or existing Java class file
     *
     * @param targetPathString
     * @param sourcePathString
     * @param toLibrary
     * @return
     */
    @SuppressWarnings("UseSpecificCatch")
    protected String copyToClassFile(String sourcePathString, String targetPathString, boolean toLibrary) {

        // Extract individual names (libraryName, fileName, memberName) from the AS400 IFS path
        extractNamesFromIfsPath(targetPathString);

        String classFilePathString;

        // Copy to LIBRARY
        if (toLibrary) {
            // Save file name is derived from the PC file name without suffix .class
            classFileName = sourcePathString.substring(sourcePathString.lastIndexOf(pcFileSep)
                    + 1, sourcePathString.lastIndexOf(".class"));
            // Save file path string is derived from the path string of the library by adding the PC file name with suffix
            // .SAVF in upper case
            classFilePathString = targetPathString + "/"
                    + sourcePathString.substring(sourcePathString.lastIndexOf(pcFileSep) + 1).toUpperCase();

            // Create a new Save File if it does not exist
            try {
                SaveFile classFile = new SaveFile(remoteServer, libraryName, classFileName);
                if (!classFile.exists()) {
                    classFile.create();
                }
            } catch (Exception exc) {
                exc.printStackTrace();
                row = "Error1: " + exc.toString();
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
                return "ERROR";
            }

            // Copy the PC file to Save file using FTP (File Transfer Protocol)
            AS400FTP ftp = new AS400FTP(remoteServer);
            try {

                // FTP Binary data transfer
                // ftp.setDataTransferType(AS400FTP.BINARY); // not necessary when suffix is .class
                // FTP Put command
                ftp.put(sourcePathString, classFilePathString);
                ftp.disconnect();
                row = "Comp: PC file  " + sourcePathString + "  was copied to Java class file  " + libraryName + "/"
                        + classFileName + ".";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
                return "";

            } catch (Exception exc) {
                exc.printStackTrace();
                row = "Error: Copying PC file  " + sourcePathString + "  to Java class file  " + libraryName + "/" + classFileName
                        + "  failed:  " + exc.toString();
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
                return "ERROR";
            }
        } //
        // Copy to IFS FILE
        else {
            // Save file name is derived from PC file name excluding suffix .class
            classFileName = sourcePathString.substring(sourcePathString.lastIndexOf(pcFileSep) + 1);

            if (!sourcePathString.endsWith(".class")) {
                row = "Error: Copying PC Java class file  " + sourcePathString
                        + "  ending with suffix \".class\" cannot be copied to the existing file  " + targetPathString
                        + "  with a different suffix.";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
                return "ERROR";
            } else {
                // Copy the PC file to Save file using FTP (File Transfer Protocol)
                AS400FTP ftp = new AS400FTP(remoteServer);
                try {
                    // FTP Binary data transfer
                    ftp.setDataTransferType(AS400FTP.BINARY);
                    // FTP Put command
                    ftp.put(sourcePathString, targetPathString);
                    ftp.disconnect();
                    row = "Comp: PC Java class file  " + sourcePathString + "  was copied to IFS Java class file  " + targetPathString
                            + ".";
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                    return "";

                } catch (Exception exc) {
                    exc.printStackTrace();
                    row = "Error: Copying PC Java class file  " + sourcePathString + "  to IFS Java class file  " + targetPathString
                            + "  failed:  " + exc.toString();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                    return "ERROR";
                }
            }
        }
    }

    /**
     * Extract individual names (libraryName, fileName, memberName, saveFileName, classFileName) from the AS400 IFS path.
     *
     */
    protected void extractNamesFromIfsPath(String as400PathString) {
        try {
            qsyslib = "/QSYS.LIB/";
            if (as400PathString.startsWith(qsyslib) && as400PathString.length() > qsyslib.length()) {
                libraryName = as400PathString.substring(as400PathString.indexOf("/QSYS.LIB/") + 10, as400PathString.lastIndexOf(".LIB"));
                if (as400PathString.length() > qsyslib.length() + libraryName.length() + 5) {
                    if (as400PathString.contains(".FILE")) {
                        fileName = as400PathString.substring(qsyslib.length() + libraryName.length() + 5, as400PathString.lastIndexOf(".FILE"));
                        //if (as400PathString.endsWith(".MBR")) {
                            memberName = as400PathString.substring(as400PathString.lastIndexOf("/") + 1, as400PathString.lastIndexOf("."));
                        //}
                    } else if (as400PathString.endsWith(".SAVF")) {
                        saveFileName = as400PathString.substring(as400PathString.lastIndexOf("/") + 1, as400PathString.lastIndexOf(".SAVF"));
                    } else if (as400PathString.endsWith(".CLASS")) {
                        classFileName = as400PathString.substring(as400PathString.lastIndexOf("/") + 1, as400PathString.lastIndexOf(".CLASS"));
                    }
                }
            }
        } catch (Exception exc) {
            fileName = "";
            saveFileName = "";
            exc.printStackTrace();
        }
    }
}
