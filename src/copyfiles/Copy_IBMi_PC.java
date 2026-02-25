package copyfiles;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400FTP;
import com.ibm.as400.access.AS400File;
import com.ibm.as400.access.AS400FileRecordDescription;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.IFSFileInputStream;
import com.ibm.as400.access.MemberDescription;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.Record;
import com.ibm.as400.access.RecordFormat;
import com.ibm.as400.access.SequentialFile;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.Properties;
import java.util.TreeMap;

/**
 * This class performs copying files or directories from IBM i to PC.
 *
 * @author Vladimír Župka, 2016
 */
public class Copy_IBMi_PC {

    AS400 remoteServer;
    String sourcePathString;
    String targetPathString;
    MainWindow mainWindow;
    String ifsPathStringPrefix;

    String row = new String();
    String msgText;

    String qsyslib;
    String libraryName;
    String fileName;
    String saveFileName;
    String memberName;
    String sourceType;
    
    Properties properties;
    Path parPath = Paths.get(System.getProperty("user.dir"), "paramfiles", "Parameters.txt");
    String encoding = System.getProperty("file.encoding", "UTF-8");

    String pcCharset;
    String ibmCcsid;
    int ibmCcsidInt;

    boolean sourceRecordPrefixPresent;

    String pcFileSep; // PC file separator ( / in unix, \ in Windows )
    String NEW_LINE;

    TreeMap<String, String> sourceFilesAndTypes = new TreeMap<>();

    boolean nodes = true;
    boolean noNodes = false;

    boolean fromWalk = true;
    boolean notFromWalk = false;

    /**
     * Constructor.
     *
     * @param remoteServer
     * @param sourcePathString
     * @param targetPathString
     * @param mainWindow
     */
    Copy_IBMi_PC(AS400 remoteServer, String sourcePathString, String targetPathString, MainWindow mainWindow) {
        this.remoteServer = remoteServer;
        this.sourcePathString = sourcePathString;
        this.targetPathString = targetPathString;
        this.mainWindow = mainWindow;

        // File separator for Windows (\) and unix (/)
        Properties sysProp = System.getProperties();
        if (sysProp.get("os.name").toString().toUpperCase().contains("WINDOWS")) {
            pcFileSep = "\\";
            NEW_LINE = "\r\n"; // Windows new line: CR LF
        } else {
            pcFileSep = "/";  // macOS, unix new line: LF
            NEW_LINE = "\n"; 
        }

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
            } catch (IllegalCharsetNameException
                    | UnsupportedCharsetException charset) {
                // If pcCharset is invalid, take ISO-8859-1
                pcCharset = "ISO-8859-1";
            }
        }

        ibmCcsid = properties.getProperty("IBM_CCSID");
        if (!ibmCcsid.equals("*DEFAULT")) {
            try {
                ibmCcsidInt = Integer.parseInt(ibmCcsid);
            } catch (NumberFormatException exc) {
                exc.printStackTrace();
                ibmCcsid = "819";
                ibmCcsidInt = 819;
            }
        }

        String prefix = properties.getProperty("SOURCE_RECORD_PREFIX");
        if (prefix.isEmpty()) {
            sourceRecordPrefixPresent = false;
        } else {
            sourceRecordPrefixPresent = true;
        }
    }

    /**
     * Initial method calling further methods for copying from IBM i to PC
     *
     * @param sourcePathString
     * @param targetPathString
     */
    protected void copyingToPC(String sourcePathString, String targetPathString) {

        if (remoteServer == null) {
            return;
        }

        IFSFile ifsDirFile = new IFSFile(remoteServer, sourcePathString);

        // Path prefix is the leading part of the path up to and including the last slash:
        // e.g.  /home/vzupka/ILESRC  ->  /home/vzupka/
        ifsPathStringPrefix = sourcePathString.substring(0, sourcePathString.indexOf(ifsDirFile.getName()));
        if (Files.isDirectory(Paths.get(targetPathString))) {

            // PC directory
            copyToPcDirectory(sourcePathString, targetPathString, ifsPathStringPrefix);
        } else { //

            // Single PC file
            copyToPcFile(sourcePathString, targetPathString, notFromWalk);
        }

        // Remove message scroll listener (cancel scrolling to the last message)
        mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
    }

    /**
     * Copying IBM i IFS file or Source member or Save file to PC file;
     * If the PC file does not exist, one is created.
     *
     * @param pcPathString
     * @param as400PathString
     * @param fromWalk
     * @return
     */
    protected String copyToPcFile(String as400PathString, String pcPathString, boolean fromWalk) {

        // Path to PC file
        Path pcFilePath = Paths.get(pcPathString);
        // Object from which to copy
        IFSFile ifsDirFile = new IFSFile(remoteServer, as400PathString);
        try {

            //
            // Source physical file is a directory and cannot be copied to PC file
            if (ifsDirFile.isSourcePhysicalFile()) {
                row = "Error: Source physical file  " + as400PathString + "  cannot be copied to file  " + pcPathString + ".";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages(noNodes);
                return "ERROR";
            }

            //
            // IFS directory cannot be copied to PC file
            if (ifsDirFile.isDirectory()) {
                row = "Error: IFS directory  " + as400PathString + "  cannot be copied to PC file  " + pcPathString + ".";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages(noNodes);
                return "ERROR";
            }
            // 
            // IFS file not ending with SAVF and not allowed to be overwritten cannot be copied to PC file
            if (ifsDirFile.isFile() && !as400PathString.toUpperCase().endsWith("SAVF")
                    && !properties.getProperty("OVERWRITE_FILE").equals("Y")) {
                row = "Error: IFS file  " + as400PathString + "  cannot be copied to PC file  " + pcPathString + ".";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages(noNodes);
                return "ERROR";
            }

            //
            // Library system:
            // ===============

            // Source physical file MEMBER:
            if (sourcePathString.startsWith("/QSYS.LIB/")) {

                extractNamesFromIfsPath(as400PathString);

                if (sourcePathString.contains(".FILE/")) {
                    if (pcPathString.toUpperCase().endsWith(".SAVF")) {
                        row = "Error: Source member  " + libraryName + "/" + fileName + "(" + memberName + ")  cannot be copied to PC file  "
                                + pcPathString + "  ending with .savf.";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(noNodes);
                        return "ERROR";
                    }

                    //
                    // Member to PC file
                    // ------
                    msgText = copyFromSourceMember(remoteServer, sourcePathString, pcPathString);

                    if (msgText.isEmpty()) {
                        row = "Comp: Source member  " + libraryName + "/" + fileName + "(" + memberName
                                + ")  was copied to PC file  " + pcPathString + " using charset " + pcCharset + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(nodes);
                        return "";
                    } else {
                        row = "Comp File: Source member  " + libraryName + "/" + fileName + "(" + memberName + ")  was NOT copied to PC file  " + pcPathString + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(noNodes);
                        return "ERROR";
                    }
                }
                //
                // Save File to PC file:
                // ---------            
                else if (sourcePathString.toUpperCase().endsWith(".SAVF")
                        && pcPathString.toUpperCase().endsWith(".SAVF")) {
                    msgText = copyFromSaveFile(remoteServer, sourcePathString, pcPathString);
                    return msgText;
                }

            } else {
                //
                // IFS (Integrated File System):
                // =============================

                // From IFS stream file to PC file (no directories are involved)     
                try {

                    byte[] byteArray = new byte[20000000];
                    int bytesRead;

                    // If the output file already exists and overwrite is not allowed - return.                
                    // ---------------------------------                    
                    if (Files.exists(pcFilePath) && !properties.getProperty("OVERWRITE_FILE").equals("Y")) {
                        row = "Error: IFS file  " + ifsDirFile + "  was NOT copied to the existing file  "
                                + pcPathString + ". Overwriting files is not allowed.";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(noNodes);
                        return "ERROR";
                    }

                    // If the output PC file does not exist - create an empty file and continue.
                    // ------------------------------------
                    if (Files.notExists(pcFilePath)) {
                        Files.createFile(pcFilePath);
                    }

                    // Copy "save" file from IFS to PC file
                    // ----------------
                    if (sourcePathString.endsWith(".savf")) {
                        if (!pcPathString.endsWith(".savf")) {
                            // IFS file with suffix .savf cannot be copied to PC file with different suffix
                            row = "Error: IFS file  " + ifsDirFile + "  ending with suffix \".savf\" cannot be copied to the existing file  "
                                    + pcPathString + "  with a different suffix.";
                            mainWindow.msgVector.add(row);
                            mainWindow.showMessages(noNodes);
                            return "ERROR";
                        }
                        if (pcPathString.endsWith(".savf")) {
                            // Copy the Save file to PC file using FTP (File Transfer Protocol)
                            AS400FTP ftp = new AS400FTP(remoteServer);
                            try {
                                // FTP Binary data transfer
                                // ftp.setDataTransferType(AS400FTP.BINARY); // not necessary when suffix is .savf
                                // FTP Get command
                                ftp.get(sourcePathString, pcPathString);
                                ftp.disconnect();
                                row = "Comp: IFS save file  " + sourcePathString + "  was copied to PC  save file  " + pcPathString + ".";
                                mainWindow.msgVector.add(row);
                                mainWindow.showMessages(noNodes);
                                return "";

                            } catch (Exception exc) {
                                exc.printStackTrace();
                                row = "Error: Copying IFS save file  " + sourcePathString + "  to PC  save file  " + pcPathString
                                        + "  failed:  " + exc.toString();
                                mainWindow.msgVector.add(row);
                                mainWindow.showMessages(noNodes);
                                return "ERROR";
                            }
                        }
                    }

                    // Binary copy
                    // -----------
                    // If both PC charset and IBM i CCSID is *DEFAULT then no data conversion is done
                    if (pcCharset.equals("*DEFAULT") && ibmCcsid.equals("*DEFAULT")) {

                        // Open input IFS file
                        IFSFileInputStream ifsInStream = new IFSFileInputStream(remoteServer, as400PathString);

                        // Open the output PC file as buffered output stream
                        OutputStream outStream = Files.newOutputStream(pcFilePath, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                        BufferedOutputStream bufOutStream = new BufferedOutputStream(outStream);

                        // Copy IFS file to PC file reading input stream to byte array and using byte buffer for output
                        // Read first portion of bytes
                        bytesRead = ifsInStream.read(byteArray);
                        // Repeat if at least one byte was read
                        while (bytesRead > 0) {
                            // Write out bytes read before
                            bufOutStream.write(byteArray, 0, bytesRead);
                            // Read next portion of bytes
                            bytesRead = ifsInStream.read(byteArray);
                        }
                        // Close files
                        bufOutStream.close();
                        ifsInStream.close();
                        if (fromWalk) {
                            row = "Info: IFS file  " + as400PathString + "  was copied unchanged (binary) to PC file  "
                                    + pcPathString + ".";
                            mainWindow.msgVector.add(row);
                            mainWindow.showMessages(noNodes);
                        } else {
                            row = "Comp: IFS file  " + as400PathString + "  was copied unchanged (binary) to PC file  "
                                    + pcPathString + ".";
                            mainWindow.msgVector.add(row);
                            mainWindow.showMessages(noNodes);
                        }

                        // Data conversion is done
                        // -----------------------
                    } else {
                        //
                        IFSFile ifsFile = new IFSFile(remoteServer, as400PathString);
                        if (ibmCcsid.equals("*DEFAULT")) {
                            ibmCcsidInt = ifsFile.getCCSID(); // CCSID attribute of the input file
                        }
                        // Open input IFS file
                        IFSFileInputStream ifsInStream = new IFSFileInputStream(remoteServer, as400PathString);

                        // Open output text file
                        if (pcCharset.equals("*DEFAULT")) {
                            pcCharset = "ISO-8859-1";
                        }
                        BufferedWriter outfileText = Files.newBufferedWriter(pcFilePath, Charset.forName(pcCharset),
                                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

                        // Copy IFS file to PC file reading input stream to byte array and using byte buffer for output
                        // Read first portion of bytes
                        bytesRead = ifsInStream.read(byteArray);
                        // Repeat if at least one byte was read
                        while (bytesRead > 0) {

                            // Convert input file data using "IBM i CCSID" parameter
                            AS400Text textConverter = new AS400Text(bytesRead, ibmCcsidInt, remoteServer);

                            // Convert byte array buffer to translated data text
                            String str = (String) textConverter.toObject(byteArray);
                            String translatedData = new String(str.getBytes(pcCharset), pcCharset);

                            // Write translated data to the text file
                            outfileText.write(translatedData);

                            // Read next byte array
                            bytesRead = ifsInStream.read(byteArray);
                        }

                        // Close files
                        outfileText.close();
                        ifsInStream.close();
                        if (fromWalk) {
                            row = "Info: IFS file  " + as400PathString + "  was copied to PC file  " + pcPathString
                                    + ",  " + ibmCcsid + " -> " + pcCharset + ".";
                            mainWindow.msgVector.add(row);
                            mainWindow.showMessages(noNodes);
                        } else {
                            row = "Comp: IFS file  " + as400PathString + "  was copied to PC file  " + pcPathString
                                    + ",  " + ibmCcsid + " -> " + pcCharset + ".";
                            mainWindow.msgVector.add(row);
                            mainWindow.showMessages(noNodes);
                        }
                    }
                    return "";

                } catch (Exception exc) {
                    exc.printStackTrace();
                    row = "Error: Copying to PC file " + pcPathString + "  failed.  -  " + exc.toString();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages(noNodes);
                    return "ERROR";
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error: Copying to PC file " + pcPathString + "  -  " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(noNodes);
            return "ERROR";
        }
        return "";
    }

    /**
     * Copy IBM i IFS directory/file or Source file/member or Save file
     * to PC directory;
     * If the output PC file does not exist, one is created.
     *
     *
     * @param as400PathString
     * @param pcPathString
     * @param ifsPathStringPrefix
     * @return
     */
    @SuppressWarnings("UseSpecificCatch")
    protected String copyToPcDirectory(String as400PathString, String pcPathString, String ifsPathStringPrefix) {

        extractNamesFromIfsPath(as400PathString);

        // Object from which to copy
        IFSFile ifsPath = new IFSFile(remoteServer, as400PathString);

        if (pcPathString == null) {
            // Alert - set target!
        }
        sourcePathString = ifsPath.getPath();
        try {
            //
            // Library system:
            // ---------------
            if (sourcePathString.startsWith("/QSYS.LIB/")) {
                if (ifsPath.isDirectory()) {

                    // Source Physical File to PC directory:
                    // --------------------
                    if (ifsPath.isSourcePhysicalFile()) {
                        msgText = copyFromSourceFile(remoteServer, sourcePathString, pcPathString);
                        if (msgText.isEmpty()) {
                            row = "Comp: Source physical file  " + libraryName + "/" + fileName + "  was copied to PC directory  " + pcPathString + ".";
                            mainWindow.msgVector.add(row);
                            mainWindow.showMessages(nodes);
                        } else {
                            row = "Comp: Source physical file  " + libraryName + "/" + fileName + "  was NOT completely copied to PC directory  " + pcPathString + ".";
                            mainWindow.msgVector.add(row);
                            mainWindow.showMessages(noNodes);
                        }
                    } else if (sourcePathString.endsWith(".LIB")) {

                        // Library to PC directory/file:
                        // -------
                        row = "Error: IBM i library  " + libraryName + "  cannot be copied to PC. Only files from the library can be copied.";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(noNodes);
                        msgText = "ERROR";
                    }
                    return msgText;
                } else if (sourcePathString.contains(".FILE/")) {

                    // Member of Source Physical File to PC file:
                    // ------
                    msgText = copyFromSourceMember(remoteServer, sourcePathString, pcPathString);
                    if (msgText.isEmpty()) {
                        row = "Comp: Source member  " + libraryName + "/" + fileName + "(" + memberName + ")  was copied to PC directory  " + pcPathString + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(nodes);
                    } else {
                        row = "Comp: Source member  " + libraryName + "/" + fileName + "(" + memberName + ")  was NOT copied to PC directory  " + pcPathString + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(noNodes);
                    }
                    return msgText;
                } else if (sourcePathString.endsWith(".SAVF")) {
                    
                    // Save File to PC directory/file:
                    // ---------
                    copyFromSaveFile(remoteServer, sourcePathString, pcPathString);
                    return msgText;
                }
            } else//
            //
            // IFS (Integrated File System):
            // -----------------------------
            // Path prefix is the leading part of the path up to and including the last slash:
            // ifsPathStringPrefix = sourcePathString.substring(0, sourcePathString.indexOf(ifsPath.getName())); 
            {
                if (ifsPath.isDirectory()) {
                    //
                    // IFS directory: to PC directory
                    // --------------  
                    try {
                        // Create the first shadow directory in PC target directory. 
                        // The name of the directory is the IFS path name without the path prefix.
                        pcPathString = pcPathString + pcFileSep
                                + ifsPath.toString().substring(ifsPathStringPrefix.length());

                        if (!Files.exists(Paths.get(pcPathString))) {
                            Files.createDirectories(Paths.get(pcPathString));

                            row = "Info: PC directory  " + pcPathString + "  was created.";
                            mainWindow.msgVector.add(row);
                            mainWindow.showMessages(nodes);
                            msgText = "";
                        }
                    } catch (Exception exc) {
                        msgText = "ERROR";
                        exc.printStackTrace();
                        row = "Error: Copying IFS directory  " + sourcePathString + "  to PC directory  " + pcPathString + "  -  " + exc.toString() + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(noNodes);
                        return "ERROR"; // Fatal error - no continuation is possible
                    }

                    // Create parameter for the next method call
                    IFSFile ifsDirectory = new IFSFile(remoteServer, sourcePathString);

                    // Create nested shadow directories in PC target directory
                    msgText = walkIfsDirectory_CreateNestedPcDirectories(ifsDirectory, pcPathString);
                    // Copy IFS files to appropriate PC shadow directories
                    msgText = copyIfsFilesToPcDirectories(ifsDirectory, pcPathString, ifsPathStringPrefix);

                    if (msgText.isEmpty()) {
                        row = "Comp: IFS directory  " + sourcePathString + "  was copied to PC directory  " + pcPathString + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(nodes);
                    } else {
                        row = "Comp: IFS directory  " + sourcePathString + "  was NOT completely copied to PC directory  " + pcPathString + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(noNodes);
                    }
                    return msgText;
                } //
                //
                // IFS stream file: to PC directory
                // ----------------
                else {
                    IFSFile ifsFile = new IFSFile(remoteServer, sourcePathString);

                    // If the output PC file does not exist - create an empty file and continue.
                    // ------------------------------------
                    // PC file = PC direcrory + IFS file 
                    String pcFilePathString = pcPathString + pcFileSep + ifsFile.getName();
                    Path pcFilePath = Paths.get(pcPathString);

                    // If the file does not exist, create one.
                    if (Files.notExists(pcFilePath)) {
                        Files.createFile(pcFilePath);
                    }

                    // Copy the IFS file to PC file
                    msgText = copyToPcFile(ifsFile.toString(), pcFilePathString, notFromWalk);

                    if (msgText.isEmpty()) {
                        row = "Comp: IFS file  " + sourcePathString + "  was copied to PC directory  " + pcPathString + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(nodes);
                    } else {
                        row = "Comp: IFS file  " + sourcePathString + "  was NOT copied to PC directory  " + pcPathString + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(noNodes);
                    }
                    return msgText;
                }
            }
            return "";
        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error: Copying  " + sourcePathString + "  to  " + pcPathString + "  -  " + exc.toString() + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(noNodes);
            return "ERROR";
        }
    }

    /**
     * Walk through IFS directory recursively to create shadow directories in PC
     * target directory; Shadow directories are named by last names of the IFS
     * directory paths so that only one-level directories are inserted to the PC
     * target directory.
     *
     * @param ifsDirectory IFS directory path
     * @param pcDirPathString Target PC directory name
     * @return
     */
    protected String walkIfsDirectory_CreateNestedPcDirectories(IFSFile ifsDirectory, String pcDirPathString) {

        try {
            // Get list of objects in the IFS directory that may be directories and single files.
            // Here we process directories only.
            IFSFile[] objectList = ifsDirectory.listFiles();
            // Process only non-empty directory
            if (objectList.length > 0) {
                for (IFSFile subDirectory : objectList) {
                    // Only IFS sub-directories are processed
                    if (subDirectory.isDirectory()) {
                        // Newly created PC directory path is built as 
                        // PC directory path string from parameter  plus
                        // the last part (leaf) of the subDirectory path
                        String newPcDirectoryPathString = pcDirPathString + pcFileSep + subDirectory.getName();

                        // Create the new nested PC directory if it does not exist
                        if (!Files.exists(Paths.get(newPcDirectoryPathString))) {
                            Files.createDirectory(Paths.get(newPcDirectoryPathString));
                            row = "Info: PC directory  " + newPcDirectoryPathString + "  was created.";
                            mainWindow.msgVector.add(row);
                            mainWindow.showMessages(nodes);
                        }
                        // Recursive call with IFS sub-directory and new PC path (parent of the new PC directory)
                        walkIfsDirectory_CreateNestedPcDirectories(subDirectory, newPcDirectoryPathString);
                    }
                }
            }
            return "";
        } catch (Exception exc) {
            exc.printStackTrace();
            return "ERROR";
        }
    }

    /**
     * Copying AS400 IFS files to PC directories created before (see
     * walkIfsDirectory_CreateNestedPcDirectories method).
     *
     * @param ifsPath IFS directory path
     * @param pcDirPathString Target PC directory name
     * @param ifsPathStringPrefix
     * @return
     */
    @SuppressWarnings("UseSpecificCatch")
    protected String copyIfsFilesToPcDirectories(IFSFile ifsPath, String pcDirPathString, String ifsPathStringPrefix) {
        String msgTextDir;
        boolean atLeastOneErrorInFiles = false;

        try {
            IFSFile[] objectList = ifsPath.listFiles();
            if (objectList.length != 0) {
                for (IFSFile ifsDirFile : objectList) {
                    String newDirPathString = pcDirPathString + pcFileSep + ifsDirFile.getName();
                    //
                    // IFS directory
                    if (ifsDirFile.isDirectory()) {

                        // Recursive call with different parameter values
                        copyIfsFilesToPcDirectories(ifsDirFile, newDirPathString, ifsPathStringPrefix);
                    } else //
                    // Single IFS file
                    {
                        if (ifsDirFile.isFile()) {
                            ifsPathStringPrefix = ifsDirFile.toString().substring(0, ifsDirFile.toString().lastIndexOf("/"));
                            // Append 
                            String newFilePathString = pcDirPathString + ifsDirFile.toString().substring(ifsPathStringPrefix.length());

                            // Copy the IFS file to PC directory created before
                            msgTextDir = copyToPcFile(ifsDirFile.toString(), newFilePathString, fromWalk);

                            if (!msgTextDir.isEmpty()) {
                                atLeastOneErrorInFiles = true;
                            }
                            /*                     
                     if (atLeastOneErrorInFiles) {
                        row = "Comp: IFS file  " + ifsDirFile.toString() + "  was copied to PC directory  " + newFilePathString + ".";
                     } else {
                        //row = "Error: IFS file  " + ifsDirFile.toString() + "  was NOT copied to PC directory  " + newFilePathString + ".";
                     }
                     mainWindow.msgVector.add(row);
                             */
                        }
                    }
                }
                msgText = atLeastOneErrorInFiles ? "ERROR" : "";
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return msgText;
    }

    /**
     * Copy Source physical member to a text file in PC (rewrite existing file or write newly created file).
     *
     * @param remoteServer
     * @param as400PathString
     * @param pcPathString
     * @return
     */
    @SuppressWarnings("UseSpecificCatch")
    protected String copyFromSourceMember(AS400 remoteServer, String as400PathString, String pcPathString) {

        // Get actual source type of the source member
        IFSFile ifsFile = new IFSFile(remoteServer, as400PathString);
        sourceType = getMemberType(ifsFile);

        Path pcFilePath = Paths.get(pcPathString);
        String pcFileName = pcFilePath.getFileName().toString();
        
        extractNamesFromIfsPath(as400PathString);

        // Add the source type to the member name
        memberName += "." + sourceType;

        // The new PC file name is the PC path string plus the member name, 
        // i.e. substring of the last part of the as400PathString.
        String pathEnding;
        String newPcFilePathString;
        Path newPcFilePath = Paths.get(pcPathString);
                
        // To PC directory:
        // ----------------
        if (Files.isDirectory(pcFilePath)) {

            // Append source member name to the PC path string to get the target PC file path
            pathEnding = memberName;
            newPcFilePathString = newPcFilePath.toString() + "/" + pathEnding;
            newPcFilePath = Paths.get(newPcFilePathString);

            // If the output PC file does not exist or overwriting data is allowed (if the file exists)
            // -------------------------------------------------------------------
            if (Files.notExists(newPcFilePath) || properties.getProperty("OVERWRITE_FILE").equals("Y")) {
                // Copy data from the member to PC file
                msgText = copyDataFromMemberToFile(remoteServer, as400PathString, newPcFilePathString);
            } else {
                // PC file exists and overwriting data is NOT allowed
                // --------------------------------------------------
                row = "Info: Source member " + libraryName + "/" + fileName + "(" + memberName + ") cannot be copied to PC directory "
                        + pcPathString + ". Overwriting files is not allowed.";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages(noNodes);
                return "ERROR";
            }
        } else //
        //    
        // To PC file:
        // -----------
        {
            if (Files.exists(pcFilePath)) {
                // path ending for existing file
                pathEnding = pcFileName;
            } else {
                // path ending for new file
                pathEnding = memberName;
            }
            // Replace PC file name with pathEnding, which is the PC file name or source member name
            // (append path ending to the parent of the PC file)
            newPcFilePath = pcFilePath.getParent();
            newPcFilePathString = newPcFilePath.toString() + "/" + pathEnding;

            // Copy member data to the PC file
            // Note: Check for overwrite is done in this method
            msgText = copyDataFromMemberToFile(remoteServer, as400PathString, newPcFilePathString);
        }
        return msgText;
    }

    /**
     * Copy data from source member to PC file;
     * If the output PC file does not exist, one is created.
     *
     * @param remoteServer
     * @param as400PathString
     * @param pcPathString
     * @return
     */
    @SuppressWarnings("UseSpecificCatch")
    protected String copyDataFromMemberToFile(AS400 remoteServer, String as400PathString, String pcPathString) {
        IFSFile ifsFile = new IFSFile(remoteServer, as400PathString);

        // Create an AS400FileRecordDescription object that represents the file
        AS400FileRecordDescription inRecDesc = new AS400FileRecordDescription(remoteServer, as400PathString);

        Path pcFilePath = Paths.get(pcPathString);
        try {
            int ccsidAttribute = ifsFile.getCCSID();
            int ccsidForDisplay = ccsidAttribute;
            // A member could inherit CCSID 1208 (UTF-8) from its parent Source physial file.
            // In this case, the universal CCSID 65535 is assumed.
            if (ccsidAttribute == 1208) {
                ccsidForDisplay = 65535;
            }
            if (ibmCcsidInt == 1208) {
                ibmCcsidInt = 65535;
            }

            // Get list of record formats of the database file
            RecordFormat[] format = inRecDesc.retrieveRecordFormat();
            // Create an AS400File object that represents the file
            SequentialFile as400seqFile = new SequentialFile(remoteServer, as400PathString);
            // Set the record format (the only one)
            as400seqFile.setRecordFormat(format[0]);

            // Open the source physical file member as a sequential file
            as400seqFile.open(AS400File.READ_ONLY, 0, AS400File.COMMIT_LOCK_LEVEL_NONE);

            // If the output file already exists and overwrite is not allowed - return.
            // ---------------------------------
            // 
            if (Files.exists(pcFilePath) && !properties.getProperty("OVERWRITE_FILE").equals("Y")) {
                row = "Info: Source member " + libraryName + "/" + fileName + "(" + memberName + ") cannot be copied to PC file "
                        + pcPathString + ". Overwriting files is not allowed.";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages(noNodes);
                return "ERROR";
            }

            // If the file does not exist, create it
            if (!Files.exists(pcFilePath)) {
                Files.createFile(pcFilePath);
            }

            // Rewrite the PC file with records from the source member
            // -------------------
            //
            // Open the output PC text file - with specified character set
            // ----------------------------            
            // Characters read from input are translated to the specified character set if possible.
            // If input characters are incapable to be translated, an error message is reported (UnmappableCharacterException).
            BufferedWriter pcOutFile;
            if (pcCharset.equals("*DEFAULT")) {
                // Ignore PC charset parameter for binary transfer.
                pcOutFile = Files.newBufferedWriter(pcFilePath,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            } else {
                // Use PC charset parameter for conversion.
                pcOutFile = Files.newBufferedWriter(pcFilePath, Charset.forName(pcCharset),
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            }

            // Read the first source member record
            Record inRecord = as400seqFile.readNext();

            // Write source records to the PC output text file.
            // --------------------
            while (inRecord != null) {
                StringBuilder textLine = new StringBuilder();
                if (sourceRecordPrefixPresent) {

                    // Source record is composed of three source record fields: seq. number, date, source data.
                    DecimalFormat df1 = new DecimalFormat("0000.00");
                    DecimalFormat df2 = new DecimalFormat("000000");

                    String seq = df1.format((Number) inRecord.getField("SRCSEQ"));
                    String seq2 = seq.substring(0, 4) + seq.substring(5);
                    textLine.append(seq2);
                    String srcDat = df2.format((Number) inRecord.getField("SRCDAT"));
                    textLine.append(srcDat);
                }
                // Otherwise, source record consists of source data only

                // Convert source data into byte array
                byte[] byteArray = inRecord.getFieldAsBytes("SRCDTA");

                String translatedData;
                // Translate member data using parameter "IBM i CCSID"
                AS400Text textConverter = new AS400Text(byteArray.length, ibmCcsidInt, remoteServer);
                if (ibmCcsid.equals("*DEFAULT")) {
                    // Translate member data using its CCSID attribute (possibly modified)
                    textConverter = new AS400Text(byteArray.length, ccsidForDisplay, remoteServer);
                }

                // Convert byte array buffer to String text line (UTF-16)
                String str = (String) textConverter.toObject(byteArray);

                if (pcCharset.equals("*DEFAULT")) {
                    // Assign "ISO-8859-1" as default charset
                    pcCharset = "ISO-8859-1";
                }
                // Translate the String into PC charset
                translatedData = new String(str.getBytes(pcCharset), pcCharset);

                textLine.append(translatedData).append(NEW_LINE);

                // Write the translated text line to the PC file
                pcOutFile.write(textLine.toString());

                // Read next source member record
                inRecord = as400seqFile.readNext();
            }
            // Close the files
            pcOutFile.close();
            as400seqFile.close();

            row = "Info: Source member " + libraryName + "/" + fileName + "(" + memberName
                    + ")  was copied to PC file  " + pcPathString + " using charset " + pcCharset + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(noNodes);
            return "";
        } catch (Exception exc) {
            exc.printStackTrace();

            row = "Error: Copying member  " + libraryName + "/" + fileName + "(" + memberName
                    + ")  -  " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(noNodes);
            return "ERROR";
        }
    }

    /**
     * Copy Source File (all members) to PC directory.
     *
     * @param remoteServer
     * @param as400PathString
     * @param pcPathString
     * @return
     */
    @SuppressWarnings("UseSpecificCatch")
    protected String copyFromSourceFile(AS400 remoteServer, String as400PathString, String pcPathString) {

        // Extract individual names (library, file, member) from the AS400 IFS path 
        extractNamesFromIfsPath(as400PathString);

        // Path to the input source file
        String inSourceFilePath = "/QSYS.LIB/" + libraryName + ".LIB/" + fileName + ".FILE";

        try {
            IFSFile ifsDirectory = new IFSFile(remoteServer, inSourceFilePath);
            // from Source Physical Files only
            if (ifsDirectory.isSourcePhysicalFile()) {
                // to PC directory only
                if (Files.isDirectory(Paths.get(pcPathString))) {
                    String pcDirectoryName = pcPathString.substring(pcPathString.lastIndexOf(pcFileSep) + 1);

                    // If the PC directory name differs from the Source Physical File name 
                    // -------------------------------------------------------------------
                    //  Create NEW directory with the Source Physical File name.
                    if (!pcDirectoryName.equals(fileName)) {
                        pcPathString = pcPathString + pcFileSep + fileName;
                        try {
                            Files.createDirectory(Paths.get(pcPathString));
                        } catch (Exception dir) {
                            dir.printStackTrace();
                            row = "Error: Creating new PC directory  " + pcPathString + "  -  " + dir.toString();
                            mainWindow.msgVector.add(row);
                            mainWindow.showMessages(noNodes);
                            return "ERROR";
                        }
                    }

                    // If the PC directory has the same name as Source Physical File - NO new directory is created and processing continues. 
                    // -------------------------------------
                    // Copy members to the PC directory
                    boolean atLeastOneErrorInMembers = false;
                    IFSFile[] ifsFiles = ifsDirectory.listFiles();
                    for (IFSFile ifsFile : ifsFiles) {

                        // Insert new or rewrite existing file in PC directory
                        String msgTextMbr = copyFromSourceMember(
                                remoteServer, ifsFile.toString(), pcPathString + pcFileSep + ifsFile.getName());

                        // If at least one message is not empty - note error 
                        if (!msgTextMbr.isEmpty()) {
                            atLeastOneErrorInMembers = true;
                        }
                    }
                    if (atLeastOneErrorInMembers) {
                        row = "Comp: Source physical file  " + libraryName + "/" + fileName + "  was copied to PC directory  " + pcPathString + ".";
                    } else {
                        row = "Error: Source physical file  " + libraryName + "/" + fileName + "  was NOT completely copied to PC directory  " + pcPathString + ".";
                    }
                    msgText = atLeastOneErrorInMembers ? "ERROR" : "";
                }
            }
            return msgText;
        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error: Copying from source physical file  " + libraryName + "/" + fileName
                    + "  -  " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(noNodes);
            return "ERROR";
        }
    }

    /**
     * Copy Save File to PC directory or PC file as .savf type.
     *
     * @param remoteServer
     * @param as400PathString
     * @param pcPathString
     * @return
     */
    protected String copyFromSaveFile(AS400 remoteServer, String as400PathString, String pcPathString) {

        AS400FTP ftp = new AS400FTP(remoteServer);
        String filePathString;
        String directoryOrFile;

        try {
            // Get names of parts from the IFS path string
            extractNamesFromIfsPath(as400PathString);
            // Check if PC object is directory or file
            if (Files.isDirectory(Paths.get(pcPathString))) {
                filePathString = pcPathString + pcFileSep + saveFileName + ".savf";
                directoryOrFile = "directory";
            } else {
                filePathString = pcPathString;
                directoryOrFile = "file";
            }

            if (!Files.exists(Paths.get(filePathString))) {
                Files.createFile(Paths.get(filePathString));
            } else {
                if (!filePathString.endsWith(".savf")) {
                    row = "Error: Save file  " + libraryName + "/" + saveFileName + "  cannot be coppied to the PC file  "
                            + filePathString + ".  Target file name must end with suffix .savf.";
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages(noNodes);
                    return "ERROR";
                }
            }

            // Binary data transfer
            ftp.setDataTransferType(AS400FTP.BINARY);
            // Get command
            ftp.get(as400PathString, filePathString);
            ftp.disconnect();

            row = "Comp: Save file  " + libraryName + "/" + saveFileName + "  was copied to PC " + directoryOrFile + " " + pcPathString + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes);
            return "";
        } catch (IOException exc) {
            exc.printStackTrace();
            row = "Error: Copying save file  " + libraryName + "/" + saveFileName + "  -  " + exc.toString() + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(noNodes);
            return "ERROR";
        }
    }

    /**
     * Get default source type for standard source physical file name (QCLSRC, QRPGLESRC, . . .)
     *
     * @param sourceFileName
     * @return
     */
    protected String getDefaultSourceType(String sourceFileName) {
        String sourceType;
        sourceType = sourceFilesAndTypes.get(sourceFileName);
        if (sourceType == null) {
            sourceType = "TXT";
        }
        return sourceType;
    }

    /**
     * Extract individual names (libraryName, fileName, memberName, saveFileName) from the IBM i IFS path.
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
                        //if (as400PathString.endsWith(".MBR")) {
                            memberName = as400PathString.substring(as400PathString.lastIndexOf("/") + 1, as400PathString.lastIndexOf("."));
                        //}
                    } else if (as400PathString.endsWith(".SAVF")) {
                        saveFileName = as400PathString.substring(qsyslib.length() + libraryName.length() + 5, as400PathString.lastIndexOf(".SAVF"));
                    }
                }
            }
        } catch (Exception exc) {
            fileName = "";
            saveFileName = "";
            exc.printStackTrace();
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
