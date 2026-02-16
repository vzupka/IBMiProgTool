package copyfiles;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.IFSFileInputStream;
import com.ibm.as400.access.IFSFileOutputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.TreeMap;

/**
 * This class performs copying files/members and directories/source files - from
 * IBM i to IBM i itself.
 *
 * @author Vladimír Župka, 2016
 */
public class Copy_IBMi_IBMi {

    AS400 remoteServer;
    String sourcePathString;
    String targetPathString;
    MainWindow mainWindow;
    String sourcePathStringPrefix;

    IFSFile inputDirFile;
    IFSFile outputDirFile;

    IFSFileInputStream ifsInStream;
    IFSFileOutputStream ifsOutStream;

    String row = new String();
    String msgText;

    String qsyslib;
    String libraryName;
    String fileName;
    String saveFileName;
    String memberName;

    Properties properties;
    Path parPath = Paths.get(System.getProperty("user.dir"), "paramfiles", "Parameters.txt");
    String encoding = System.getProperty("file.encoding", "UTF-8");

    String ibmCcsid;
    int ibmCcsidInt;
    String targetCcsidStr;
    String sourceCcsidStr;

    //boolean sourceRecordPrefixPresent;

    TreeMap<String, String> sourceFilesAndTypes = new TreeMap<>();

    boolean nodes = true;
    boolean noNodes = false;

    boolean fromWalk = true;
    boolean notFromWalk = false;

    boolean overwriteAllowed = true;

    /**
     * Constructor.
     *
     * @param remoteServer
     * @param sourcePathString
     * @param targetPathString
     * @param mainWindow
     */
    Copy_IBMi_IBMi(AS400 remoteServer, String sourcePathString, String targetPathString, MainWindow mainWindow) {
        this.remoteServer = remoteServer;
        this.sourcePathString = sourcePathString;
        this.targetPathString = targetPathString;
        this.mainWindow = mainWindow;

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
            // For non *DEFAULT
            try {
                ibmCcsidInt = Integer.parseInt(ibmCcsid);
            } catch (Exception exc) {
                exc.printStackTrace();
                ibmCcsid = "819";
                ibmCcsidInt = 819;
            }
        } else {
            // For *DEFAULT
            ibmCcsid = "819";
            ibmCcsidInt = 819;
        }
/*
        String prefix = properties.getProperty("SOURCE_RECORD_PREFIX");
            sourceRecordPrefixPresent = prefix.isEmpty();
*/
        if (properties.getProperty("OVERWRITE_FILE").equals("Y")) {
            overwriteAllowed = true;
        } else {
            overwriteAllowed = false;
        }
/*
        // Table of Standard Source Physical File Names (keys) and default Source Types (values)
        sourceFilesAndTypes.put("QBASSRC", "BAS");
        sourceFilesAndTypes.put("QCBLLESRC", "CBLLE");
        sourceFilesAndTypes.put("QCLSRC", "CLLE");
        sourceFilesAndTypes.put("QCMDSRC", "CMD");
        sourceFilesAndTypes.put("QCSRC", "C");
        sourceFilesAndTypes.put("QDDSSRC", "PF");
        sourceFilesAndTypes.put("QFTNSRC", "FTN");
        sourceFilesAndTypes.put("QCBLSRC", "CBL");
        sourceFilesAndTypes.put("QMENUSRC", "MNUDDS");
        sourceFilesAndTypes.put("QMNUSRC", "MENU");
        sourceFilesAndTypes.put("QPASSRC", "PAS");
        sourceFilesAndTypes.put("QPLISRC", "PLI");
        sourceFilesAndTypes.put("QPNLSRC", "PNLGRP");
        sourceFilesAndTypes.put("QREXSRC", "REXX");
        sourceFilesAndTypes.put("QRMCSRC", "RMC");
        sourceFilesAndTypes.put("QRPGLESRC", "RPGLE");
        sourceFilesAndTypes.put("QRPGSRC", "RPG");
        sourceFilesAndTypes.put("QS36PRC", "OCL36");
        sourceFilesAndTypes.put("QS36SRC", "UNS36");
        sourceFilesAndTypes.put("QSRVSRC", "BND");
        sourceFilesAndTypes.put("QTBLSRC", "TBL");
        sourceFilesAndTypes.put("QTXTSRC", "TXT");
        sourceFilesAndTypes.put("QUDSSRC", "QRY38");
*/        
    }

    /**
     * Initial method calling further methods for copying from IBM i to IBMi.
     */
    protected void copy_IBMi_IBMi() {


        if (remoteServer == null) {
            return;
        }

        // Set member type fo .MBR to convene IFS path convention
        if (sourcePathString.contains(".FILE/") && !sourcePathString.endsWith("SAVF"))  {
             sourcePathString = sourcePathString.substring(0, 
                     sourcePathString.lastIndexOf(".")) + ".MBR";
         }
         
        inputDirFile = new IFSFile(remoteServer, sourcePathString);
        outputDirFile = new IFSFile(remoteServer, targetPathString);
        
        // Modification of paths for Save Files
        // ------------------------------------
        // From Save File - replace type .SAVF with .FILE 
        if (sourcePathString.startsWith("/QSYS.LIB") && sourcePathString.toUpperCase().endsWith(".SAVF")) {
            inputDirFile = new IFSFile(remoteServer, sourcePathString.replace(".SAVF", ".FILE"));
        }
        // To Save File - replace type .savf with .FILE
        if (targetPathString.startsWith("/QSYS.LIB") && targetPathString.toUpperCase().endsWith(".SAVF")) {
            outputDirFile = new IFSFile(remoteServer, targetPathString.replace(".savf", ".FILE"));
        }

        // Path prefix is the leading part of the path up to and including the last slash:
        // e.g. /home/vzupka/ILESRC -> /home/vzupka/
        sourcePathStringPrefix = sourcePathString.substring(0, sourcePathString.lastIndexOf("/") + 1);

        // Decide which copy method will be called according to the source and target paths.
        // ---------------------------------------
        try {

            if (inputDirFile.isDirectory() && outputDirFile.isDirectory()) {

                // From directory to directory
                // ---------------------------
                if (sourcePathString.startsWith("/QSYS.LIB") && targetPathString.startsWith("/QSYS.LIB")) {
                    // From Source physical file to Source file or Library
                    copyFromSourceFile(remoteServer, sourcePathString, targetPathString);

                } else if (sourcePathString.startsWith("/QSYS.LIB") && !targetPathString.startsWith("/QSYS.LIB")) {
                    // From Source physical file to IFS directory
                    copyFromSourceFile(remoteServer, sourcePathString, targetPathString);

                } else if (!sourcePathString.startsWith("/QSYS.LIB") && targetPathString.startsWith("/QSYS.LIB")) {
                    // From IFS directory to Source physical file
                    copyFromIfsDirectory(sourcePathString, targetPathString, sourcePathStringPrefix);

                } else if (!sourcePathString.startsWith("/QSYS.LIB") && !targetPathString.startsWith("/QSYS.LIB")) {
                    // From IFS directory to IFS directory
                    copyToIfsDirectory(sourcePathString, targetPathString, sourcePathStringPrefix);
                }

            } else if (inputDirFile.isFile() && outputDirFile.isDirectory()) {
                //
                // From file to directory 
                // ----------------------
                if (sourcePathString.startsWith("/QSYS.LIB") && targetPathString.startsWith("/QSYS.LIB")) {
                    if (sourcePathString.contains(".FILE/") && !sourcePathString.endsWith(".SAVF")) {
                        // From Member to Source file 
                        copyFromSourceMember(remoteServer, sourcePathString, targetPathString);
                    } else if (sourcePathString.endsWith(".SAVF")) {
                        // From Save file to Save file in different Library
                        copyFromSaveFile(remoteServer, sourcePathString, targetPathString);
                    }

                } else if (sourcePathString.startsWith("/QSYS.LIB") && !targetPathString.startsWith("/QSYS.LIB")) {
                    if (sourcePathString.contains(".FILE/")) {
                        // From Member to IFS directory
                        copyFromSourceMember(remoteServer, sourcePathString, targetPathString);
                    } else if (sourcePathString.endsWith(".SAVF")) {
                        // From Save file to file in IFS directory
                        copyFromSaveFile(remoteServer, sourcePathString, targetPathString);
                    }

                } else if (!sourcePathString.startsWith("/QSYS.LIB") && targetPathString.startsWith("/QSYS.LIB")) {
                    if (sourcePathString.endsWith(".savf") && targetPathString.endsWith(".LIB")) {
                        // From IFS file to Save file in a Library
                        copyFromIfsFile(sourcePathString, targetPathString);
                    } else if (targetPathString.endsWith(".FILE")) {
                        // From IFS file to Source file
                        copyToSourceFile(remoteServer, sourcePathString, targetPathString);
                    }

                } else if (!sourcePathString.startsWith("/QSYS.LIB") && !targetPathString.startsWith("/QSYS.LIB")) {
                    // From IFS directory to IFS directory
                    copyToIfsDirectory(sourcePathString, targetPathString, sourcePathStringPrefix);
                }

            }
            //
            // From file to file
            // -----------------
            else  {
System.out.println("sourcePathString1: "+sourcePathString);
System.out.println("targetPathString1: "+targetPathString);
               // From file to file
                if (sourcePathString.startsWith("/QSYS.LIB") && targetPathString.startsWith("/QSYS.LIB")) {

                    if (sourcePathString.contains(".FILE/")) {
                        // From Member to Member
                        copyFromSourceMember(remoteServer, sourcePathString, targetPathString);
                    } else if (sourcePathString.endsWith(".SAVF")) {
                        // From Save file to Save file in another Library
                        copyFromSaveFile(remoteServer, sourcePathString, targetPathString);
                    }

                } else if (sourcePathString.startsWith("/QSYS.LIB") && !targetPathString.startsWith("/QSYS.LIB")) {
                    if (sourcePathString.contains(".FILE/")) {
                        // From Member to IFS file
                        copyFromSourceMember(remoteServer, sourcePathString, targetPathString);
                    } else {
                        // From Save file to IFS file
                        copyFromSaveFile(remoteServer, sourcePathString, targetPathString);
                    }

                } else if (!sourcePathString.startsWith("/QSYS.LIB") && targetPathString.startsWith("/QSYS.LIB")) {
                    if (targetPathString.contains(".FILE/")) {
                        // From IFS file to Member
                        copyToSourceMember(sourcePathString, targetPathString);
                    } else {
                        // From IFS file to Save file
                        copyToSaveFile(sourcePathString, targetPathString);
                    }

                } else if (!sourcePathString.startsWith("/QSYS.LIB") && !targetPathString.startsWith("/QSYS.LIB")) {
                    // From IFS file to IFS file
                    copyToIfsFile(sourcePathString, targetPathString, fromWalk);
                }
            }

        } catch (Exception exc) {
            exc.printStackTrace();
        }

        // Remove message scroll listener (cancel scrolling to the last message)
        mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
    }

    /**
     *
     * @param sourcePathString
     * @param targetPathString
     * @return
     */
    protected String copyFromIfsFile(String sourcePathString, String targetPathString) {

        if (targetPathString.equals(sourcePathString)) {
            return "ERROR";
        }
        // Path to input IFS file
        inputDirFile = new IFSFile(remoteServer, sourcePathString);
        // Path to output IFS file
        outputDirFile = new IFSFile(remoteServer, targetPathString);

        try {

            // Library system:
            // ---------------
            if (targetPathString.startsWith("/QSYS.LIB/")) {

                extractNamesFromIfsPath(sourcePathString);

                if (targetPathString.endsWith(".SAVF")) {

                    // IFS file to Save File:
                    // ----------------------

                    msgText = copyToSaveFile(sourcePathString, targetPathString);

                    if (msgText.isEmpty()) {
                        row = "Comp: IFS file  " + sourcePathString + "  was copied to Save file  "
                                + libraryName + "/" + fileName + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(nodes);
                        return "";
                    } else {
                        row = "Comp: IFS file  " + sourcePathString + "  was NOT copied to Save file  "
                                + libraryName + "/" + fileName + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(nodes);
                        return "ERROR";
                    }
                } else if (targetPathString.endsWith(".LIB")) {
                    //
                    // IFS file to Library
                    // -------------------
                    copyToSaveFile(sourcePathString, targetPathString);

                } else if (targetPathString.endsWith(".FILE")) {
                    //
                    // IFS file to Source file
                    // -----------------------

                    msgText = copyToSourceFile(remoteServer, sourcePathString, targetPathString);

                    if (msgText.isEmpty()) {
                        row = "Comp: IFS file  " + targetPathString + "  was copied to  Source file  "
                                + libraryName + "/" + fileName + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(nodes);
                        return "";
                    } else {
                        row = "Comp File: IFS fileSource file  " + libraryName + "/" + fileName
                                + "  was NOT copied to Source file  " + libraryName + "/" + fileName + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(nodes);
                        return "ERROR";
                    }

                } else {

                    // IFS file to Member
                    // ------------------

                    msgText = copyToSourceMember(sourcePathString, targetPathString);

                    if (msgText.isEmpty()) {
                        row = "Comp: Source member  " + libraryName + "/" + fileName + "(" + memberName
                                + ")  was copied to IFS file  " + targetPathString + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(nodes);
                        return "";
                    } else {
                        row = "Comp Source member  " + libraryName + "/" + fileName + "(" + memberName
                                + ")  was NOT copied to IFS file  " + targetPathString + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(nodes);
                        return "ERROR";
                    }
                }
                return msgText;
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error: Copying from IFS file " + targetPathString + "  -  " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes);
            return "ERROR";
        }
        return "";
    }

    /**
     * Copy IFS directory to Source file;
     *
     * @param sourcePathString
     * @param targetPathString
     * @param sourcePathStringPrefix
     * @return
     */
    protected String copyFromIfsDirectory(String sourcePathString, String targetPathString, String sourcePathStringPrefix) {

        // Cannot copy to itself
        if (targetPathString.equals(sourcePathString)) {
            return null;
        }

        // Path to input IFS file
        inputDirFile = new IFSFile(remoteServer, sourcePathString);
        // Path to output IFS file
        outputDirFile = new IFSFile(remoteServer, targetPathString);

        try {
            //
            // Library system:
            // ---------------
            if (targetPathString.startsWith("/QSYS.LIB/")) {
                if (outputDirFile.isDirectory()) {

                    // IFS directory (Source file) to Library:
                    // ---------------------------------------
                    if (targetPathString.endsWith(".LIB")) {
                        // Copy IFS directory to a Library is not allowed.
                        row = "Error: Copying IFS directory to Library is not allowed. "
                                + "First create a source physial file with a CCSID of your choice.";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                        return "ERROR";
                    }

                    // IFS directory (Source file) to Source file:
                    // -------------------------------------------
                    if (outputDirFile.isSourcePhysicalFile()) {
                        msgText = copyToSourceFile(remoteServer, sourcePathString, targetPathString);
                    }
                    return msgText;
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error: Copying  " + sourcePathString + "  to  " + targetPathString + "  -  "
                    + exc.toString() + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes);
        }
        return "";
    }

    /**
     * Copy IFS file or Source member or Save file to an IFS file; If the target
     * IFS file does not exist, one is created.
     *
     * @param targetPathString
     * @param sourcePathString
     * @param fromWalk
     * @return
     */
    protected String copyToIfsFile(String sourcePathString, String targetPathString, boolean fromWalk) {

        if (targetPathString.equals(sourcePathString)) {
            return "ERROR";
        }
        // Path to input IFS file
        inputDirFile = new IFSFile(remoteServer, sourcePathString);
        // Path to output IFS file
        outputDirFile = new IFSFile(remoteServer, targetPathString);

        try {

            //
            // Library system:
            // ---------------
            if (sourcePathString.startsWith("/QSYS.LIB/")) {

                extractNamesFromIfsPath(sourcePathString);
                if (sourcePathString.endsWith(".FILE") && inputDirFile.isSourcePhysicalFile()) { //
                    //
                    // Source file to IFS file
                    // -----------------------

                    if (targetPathString.endsWith(".savf")) {
                        row = "Error: Source file  " + libraryName + "/" + fileName
                                + "  cannot be copied to IFS file  " + targetPathString
                                + "  ending with .savf.";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(nodes);
                        return "ERROR";
                    }

                    msgText = copyFromSourceFile(remoteServer, sourcePathString, targetPathString);

                    if (msgText.isEmpty()) {
                        row = "Comp: Source file  " + libraryName + "/" + fileName
                                + "  was copied to IFS file  " + targetPathString + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(nodes);
                        return "";
                    } else {
                        row = "Comp File: Source file  " + libraryName + "/" + fileName
                                + "  was NOT copied to IFS file.";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(nodes);
                        return "ERROR";
                    }

                }

                if (sourcePathString.contains(".FILE/")) {
                    //
                    // Member to IFS file
                    // ------------------

                    if (targetPathString.endsWith(".savf")) {
                        row = "Error: Source member  " + libraryName + "/" + fileName + "(" + memberName
                                + ")  cannot be copied to IFS file  " + targetPathString
                                + "  ending with .savf.";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(nodes);
                        return "ERROR";
                    }

                    msgText = copyFromSourceMember(remoteServer, sourcePathString, targetPathString);

                    if (msgText.isEmpty()) {
                        row = "Comp: Source member  " + libraryName + "/" + fileName + "(" + memberName
                                + ")  was copied to IFS file  " + targetPathString + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(nodes);
                        return "";
                    } else {
                        row = "Comp File: Source member  " + libraryName + "/" + fileName + "("
                                + memberName + ")  was NOT copied to IFS file.";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(nodes);
                        return "ERROR";
                    }
                } //
                //
                // Save File to IFS file:
                // ---------
                else if (inputDirFile.toString().contains(".LIB")
                        && inputDirFile.toString().endsWith(".SAVF")) {

                    msgText = copyFromSaveFile(remoteServer, sourcePathString, targetPathString);
                    return msgText;
                }

            } else {//
                //
                // IFS (Integrated File System):
                // -----------------------------

                if (outputDirFile.isDirectory()) {
                    // When IFS directory, add source file name
                    // ------------------
                    // IFS file path string = target IFS directory + source IFS file
                    // name
                    targetPathString = sourcePathStringPrefix + "/" + inputDirFile.getName();
                    outputDirFile = new IFSFile(remoteServer, targetPathString);
                }

                // From IFS stream file to IFS stream file (no directories are involved)
                // =======================================
                try {

                    byte[] inputByteArray = new byte[2000000];
                    int bytesRead;

                    // If the output file already exists and overwrite is not allowed - return.
                    // ---------------------------------
                    if (outputDirFile.exists() && !overwriteAllowed) {
                        row = "Error: IFS file  " + inputDirFile
                                + "  was NOT copied to the existing file  " + targetPathString
                                + ". Overwriting files is not allowed.";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(noNodes);
                        return "ERROR";
                    }

                    // Get input file CCSID attribute
                    int inputFileCcsid = inputDirFile.getCCSID();
                    // Target file CCSID attribute - not yet ready
                    int outputFileCcsid;

                    // If the output IFS file does not exist - create an empty file and continue.
                    // -------------------------------------
                    if (!outputDirFile.exists()) {
                        // If target file does not exist, create one and set its CCSID from the input file
                        outputDirFile.createNewFile();
                        outputDirFile.setCCSID(inputFileCcsid);
                    }
                    outputFileCcsid = outputDirFile.getCCSID();

                    // Open input IFS file
                    ifsInStream = new IFSFileInputStream(remoteServer, sourcePathString);

                    //
                    // No conversion (binary)
                    // ----------------------
                    //
                    if (ibmCcsid.equals("*DEFAULT") || outputFileCcsid == inputFileCcsid) {

                        ifsOutStream = new IFSFileOutputStream(remoteServer, targetPathString);

                        // Copy IFS file to IFS file reading input stream to byte array and using output stream for output
                        bytesRead = ifsInStream.read(inputByteArray); // Read first portion of bytes
                        // Repeat if at least one byte was read
                        while (bytesRead > 0) {
                            // Write out the bytes read before
                            ifsOutStream.write(inputByteArray, 0, bytesRead);
                            bytesRead = ifsInStream.read(inputByteArray); // Read next portion of bytes
                        }
                        // Close files
                        ifsOutStream.close();
                        ifsInStream.close();
                        if (fromWalk) {
                            row = "Info: IFS file  " + sourcePathString
                                    + "  was copied unchanged (binary) to IFS file  " + targetPathString
                                    + ". " + outputFileCcsid + " -> " + outputFileCcsid + ".";
                        } else {
                            row = "Comp: IFS file  " + sourcePathString
                                    + "  was copied unchanged (binary) to IFS file  " + targetPathString
                                    + ". " + outputFileCcsid + " -> " + outputFileCcsid + ".";
                        }
                        //
                    } else {
                        //
                        // Conversion from source IFS file's CCSID to target IFS file's CCSID
                        // ----------

                        // Open output IFS file
                        ifsOutStream = new IFSFileOutputStream(remoteServer, targetPathString, outputFileCcsid);

                        // Copy IFS file to IFS file reading input stream to byte
                        // array and using byte array for output
                        // Read first portion of bytes
                        bytesRead = ifsInStream.read(inputByteArray);
                        // Repeat if at least one byte was read
                        while (bytesRead > 0) {

                            // Convert input byte array with input CCSID to String
                            // (UTF-16)
                            AS400Text textConverter = new AS400Text(bytesRead, inputFileCcsid, remoteServer);
                            String text = (String) textConverter.toObject(inputByteArray);

                            // Convert data from String (UTF-16) to outpu byte array
                            // with output CCSID
                            AS400Text textConverter2 = new AS400Text(bytesRead, outputFileCcsid, remoteServer);
                            byte[] outputByteArray = (byte[]) textConverter2.toBytes(text);

                            // Write converted text in second byte array to output
                            // stream
                            ifsOutStream.write(outputByteArray, 0, outputByteArray.length);

                            // Read next byte array
                            bytesRead = ifsInStream.read(inputByteArray);

                            // Close files
                            ifsOutStream.close();
                            ifsInStream.close();
                            if (fromWalk) {
                                row = "Info: IFS file  " + sourcePathString + "  was copied to IFS file  "
                                        + targetPathString + ", Convert  " + inputFileCcsid + " -> "
                                        + outputFileCcsid;
                            } else {
                                row = "Comp: IFS file  " + sourcePathString + "  was copied to IFS file  "
                                        + targetPathString + ", Convert  " + inputFileCcsid + " -> "
                                        + outputFileCcsid;
                            }
                        }
                    }
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages(nodes);
                    return "";

                } catch (Exception exc) {
                    exc.printStackTrace();
                    row = "Error: Copying to IFS file " + targetPathString + "  -  " + exc.toString();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages(nodes);
                    return "ERROR";
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error: Copying to IFS file " + targetPathString + "  -  " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes);
            return "ERROR";
        }
        return "";
    }

    /**
     * Copy IFS directory/file or Source file/member or Save file to IFS
     * directory; If the output IFS file does not exist, one is created.
     *
     * @param sourcePathString
     * @param targetPathString
     * @param sourcePathStringPrefix
     * @return
     */
    protected String copyToIfsDirectory(String sourcePathString, String targetPathString, String sourcePathStringPrefix) {

        // Cannot copy to itself
        if (targetPathString.equals(sourcePathString)) {
            return null;
        }

        extractNamesFromIfsPath(sourcePathString);
        // Path to input IFS file
        inputDirFile = new IFSFile(remoteServer, sourcePathString);
        // Path to output IFS file
        outputDirFile = new IFSFile(remoteServer, targetPathString);

        try {
            //
            // From Library system:
            // --------------------
            if (sourcePathString.startsWith("/QSYS.LIB/")) {
                if (inputDirFile.isDirectory()) {

                    // If target is IFS directory, NOT Library

                    if (!targetPathString.startsWith("/QSYS.LIB/")) {

                        // Source Physical File to IFS directory:
                        // --------------------
                        if (inputDirFile.isSourcePhysicalFile()) {

                            // Copy from Source file to Source file
                            msgText = copyFromSourceFile(remoteServer, sourcePathString, targetPathString);

                            if (msgText.isEmpty()) {
                                row = "Comp: Source physical file  " + libraryName + "/" + fileName
                                        + "  was copied to IFS directory  " + targetPathString + ".";
                            } else {
                                row = "Comp: Source physical file  " + libraryName + "/" + fileName
                                        + "  was NOT completely copied to IFS directory  " + targetPathString
                                        + ".";
                            }
                        } else if (sourcePathString.endsWith(".LIB")) {

                            // Library to IFS directory/file is an ERROR!
                            // -------
                            row = "Error: IBM i library  " + libraryName + "  cannot be copied to IFS.";
                            msgText = "ERROR";
                        }
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(nodes);
                        return msgText;

                    } else if (sourcePathString.contains(".FILE/")) {

                        // Member of Source Physical File to IFS file:
                        // ------
                        msgText = copyFromSourceMember(remoteServer, sourcePathString, targetPathString);
                        if (msgText.isEmpty()) {
                            row = "Comp: Source member  " + libraryName + "/" + fileName + "(" + memberName
                                    + ")  was copied to IFS directory  " + targetPathString + ".";
                        } else {
                            row = "Comp: Source member  " + libraryName + "/" + fileName + "(" + memberName
                                    + ")  was NOT copied to IFS directory  " + targetPathString + ".";
                        }
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(nodes);
                        return msgText;

                    } else if (sourcePathString.endsWith(".SAVF")) {

                        // Save File to IFS directory/file:
                        // ---------
                        copyFromSaveFile(remoteServer, sourcePathString, targetPathString);
                        return msgText;
                    }
                } else {
                    // If target is Library
                    // Copy IFS directory to a Library is not allowed.
                    row = "Error: Copy IFS directory to a Library is not allowed.";
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                    return "ERROR";
                }
            } else
            // IFS (Integrated File System):
            // -----------------------------
            // Path prefix is the leading part of the path up to and including the
            // last slash:
            // ifsPathStringPrefix = sourcePathString.substring(0,
            // sourcePathString.indexOf(ifsPath.getName()));
            {
                if (inputDirFile.isDirectory()) {

                    // Target is IFS directory

                    if (!targetPathString.startsWith("/QSYS.LIB/")) {

                        //
                        // IFS directory: to IFS directory
                        // --------------

                        try {
                            // Create the first shadow directory in IFS target directory.
                            // The name of the directory is the target IFS path string plus source file name.
                            // (Source file name is source path string minus prefix.)
                            targetPathString = targetPathString + "/"
                                    + inputDirFile.toString().substring(sourcePathStringPrefix.length());
                            outputDirFile = new IFSFile(remoteServer, targetPathString);
                            if (!outputDirFile.exists()) {

                                outputDirFile.mkdir();

                                row = "Info: IFS directory  " + targetPathString + "  was created.";
                                mainWindow.msgVector.add(row);
                                mainWindow.showMessages(nodes);
                                msgText = "";
                            }
                        } catch (Exception exc) {
                            msgText = "ERROR";
                            exc.printStackTrace();
                            row = "Error: Copying IFS directory  " + sourcePathString
                                    + "  to IFS directory  " + targetPathString + "  -  " + exc.toString()
                                    + ".";
                            mainWindow.msgVector.add(row);
                            mainWindow.showMessages(nodes);
                            return "ERROR"; // Fatal error - no continuation is possible
                        }

                        // Create parameter for the next method call
                        inputDirFile = new IFSFile(remoteServer, sourcePathString);

                        // Create nested shadow directories in IFS target directory
                        msgText = walkIfsDirectory_CreateNestedIfsDirectories(inputDirFile, targetPathString);
                        // Copy IFS files to appropriate IFS shadow directories
                        msgText = copyIfsFilesToIfsDirectories(inputDirFile, targetPathString, sourcePathStringPrefix);

                        if (msgText.isEmpty()) {
                            row = "Comp: IFS directory  " + sourcePathString
                                    + "  was copied to IFS directory  " + targetPathString + ".";
                            mainWindow.msgVector.add(row);
                            mainWindow.showMessages(nodes);
                        } else {
                            row = "Comp: IFS directory  " + sourcePathString
                                    + "  was NOT completely copied to IFS directory  " + targetPathString
                                    + ".";
                            mainWindow.msgVector.add(row);
                            mainWindow.showMessages(nodes);
                        }
                        return msgText;
                    } //
                } //
                // IFS stream file: to IFS directory
                // ----------------
                else {

                    // inputDirFile = new IFSFile(remoteServer, sourcePathString);

                    // If the output IFS file does not exist - create an empty file
                    // and continue.
                    // -------------------------------------
                    // IFS file = IFS direcrory + IFS file
                    String newTargetPathString = targetPathString + "/" + inputDirFile.getName();
                    outputDirFile = new IFSFile(remoteServer, targetPathString);

                    if (!inputDirFile.exists()) {
                        inputDirFile.createNewFile();

                        row = "Info: IFS directory  " + targetPathString + "  was created.";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                        msgText = "";
                    }

                    // Copy to IFS file
                    msgText = copyToIfsFile(inputDirFile.toString(), newTargetPathString, notFromWalk);

                    if (msgText.isEmpty()) {
                        row = "Comp: IFS file  " + sourcePathString + "  was copied to IFS directory  "
                                + targetPathString + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(nodes);
                    } else {
                        row = "Comp: IFS file  " + sourcePathString
                                + "  was NOT copied to IFS directory  " + targetPathString + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(nodes);
                    }
                    return msgText;
                }

                // IFS directory to Library is NOT applicable
                // ------------------------------------------

            }
        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error: Copying  " + sourcePathString + "  to  " + targetPathString + "  -  "
                    + exc.toString() + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes);
        }
        return "";
    }

    /**
     * Walk through IFS directory recursively to create shadow directories in IFS
     * target directory; Shadow directories are named by last names of the IFS
     * directory paths so that only one-level directories are inserted to the IFS
     * target directory.
     *
     * @param sourcePathString
     * Source IFS directory path
     * @param targetPathString
     * Target IFS directory name
     * @return
     */
    protected String walkIfsDirectory_CreateNestedIfsDirectories(IFSFile sourcePathString, String targetPathString) {
        try {
            // Get list of objects in the IFS directory that may be directories and
            // single files.
            // Here we process directories only.
            IFSFile[] objectList = sourcePathString.listFiles();
            // Process only non-empty directory
            if (objectList.length > 0) {
                for (IFSFile subDirectory : objectList) {

                    // Only IFS sub-directories are processed
                    if (subDirectory.isDirectory()) {
                        // Newly created IFS directory path is built as
                        // IFS directory path string from parameter plus
                        // the last part (leaf) of the subDirectory path
                        String newTargetPathString = targetPathString + "/" + subDirectory.getName();

                        // Create the new nested IFS directory if it does not exist
                        IFSFile newOutputDirFile = new IFSFile(remoteServer, newTargetPathString);
                        if (!newOutputDirFile.exists()) {
                            newOutputDirFile.mkdir();
                        }
                        row = "Info: IFS directory  " + newTargetPathString + "  was created.";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages(nodes);

                        // Recursive call with IFS sub-directory and new IFS path
                        // (parent of the new IFS directory)
                        walkIfsDirectory_CreateNestedIfsDirectories(subDirectory, newTargetPathString);
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
     * Copying IFS files to IFS directories created before (see
     * walkIfsDirectory_CreateNestedIfsDirectories method).
     *
     * @param ifsPath
     * IFS directory path
     * @param targetPathString
     * Target IFS directory name
     * @param ifsPathStringPrefix
     * @return
     */
    protected String copyIfsFilesToIfsDirectories(IFSFile ifsPath, String targetPathString, String ifsPathStringPrefix) {
        String msgTextDir;
        boolean atLeastOneErrorInFiles = false;

        try {
            IFSFile[] objectList = ifsPath.listFiles();
            if (objectList.length != 0) {
                for (IFSFile inputDirFile : objectList) {
                    String newDirPathString = targetPathString + "/" + inputDirFile.getName();
                    //
                    // IFS directory
                    if (inputDirFile.isDirectory()) {

                        // Recursive call with different parameter values
                        copyIfsFilesToIfsDirectories(inputDirFile, newDirPathString, ifsPathStringPrefix);
                    } else //
                    // Simple IFS file
                    {
                        if (inputDirFile.isFile()) {
                            ifsPathStringPrefix = inputDirFile.toString().substring(0, inputDirFile.toString().lastIndexOf("/"));
                            // Append
                            String newFilePathString = targetPathString
                                    + inputDirFile.toString().substring(ifsPathStringPrefix.length());

                            // Copy the IFS file to IFS directory created before
                            msgTextDir = copyToIfsFile(inputDirFile.toString(), newFilePathString, fromWalk);

                            if (!msgTextDir.isEmpty()) {
                                atLeastOneErrorInFiles = true;
                            }
                            if (atLeastOneErrorInFiles) {
                                row = "Comp: IFS file  " + inputDirFile.toString()
                                        + "  was copied to IFS directory  " + newFilePathString + ".";
                            } else {
                                row = "Error: IFS file  " + inputDirFile.toString()
                                        + "  was NOT copied to IFS directory  " + newFilePathString + ".";
                            }
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
     * Copy Source member to Source file or Library.
     *
     * @param remoteServer
     * @param sourcePathString
     * @param targetPathString
     * @return
     */
    protected String copyFromSourceMember(AS400 remoteServer, String sourcePathString, String targetPathString) {

        msgText = "";

        // Get object names from source path string: qsyslib, library, file,
        // member.
        extractNamesFromIfsPath(sourcePathString);
        // Save the object names for command "from" parameters
        String inLibName = libraryName;
        String inFileName = fileName;
        String inMbrName = memberName;

        // Build new member name appended by source type
        String typedMemberName = inMbrName;
        String sourceType;
        if (properties.get("SOURCE_TYPE").equals("*DEFAULT")) {
            // Get default source type for standard source physical file name (QCLSRC, QRPGLESRC, ...)
            sourceType = getDefaultSourceType(fileName);
        } else {
            // Get source type from combo box
            sourceType = (String) properties.get("SOURCE_TYPE");
        }
        // Add the source type to member name
        typedMemberName += "." + sourceType;

        // Create source file path
        IFSFile sourcePath = new IFSFile(remoteServer, sourcePathString);
        // Create target file path
        IFSFile targetPath = new IFSFile(remoteServer, targetPathString);

        try {

            // Enable calling CL commands
            CommandCall cmdCall = new CommandCall(remoteServer);

            String newTargetPathString;

            // Determine kind of target: Source File / Source Member / IFS file / IFS directory.
            // =========================

            // To Source File or Member
            // ------------------------
            if (targetPath.isSourcePhysicalFile()) {

                // For both Source file or member, CPYSRCPF command is used.

                // Get object names from target path string for command "to"
                // parameters
                extractNamesFromIfsPath(targetPathString);

                // Target is a directory (source file)
                if (targetPath.isDirectory()) {
                    // If target is IFS directory, the new file path will be:
                    // directory + typed member name
                    newTargetPathString = targetPathString + "/" + typedMemberName;
                    // Target is file (member)
                } else {
                    // If target is IFS file, the new file path will be:
                    // file's directory (prefix up to last "/") + typed member name
                    newTargetPathString = targetPathString;
                }

                // Check if member names are equal and overwriting data is allowed
                targetPath = new IFSFile(remoteServer, newTargetPathString);
                if (targetPath.exists() && !overwriteAllowed) {
                    // IFS file exists and overwriting data is NOT allowed
                    row = "Error: Source member " + inLibName + "/" + inFileName + "(" + inMbrName + ") "
                            + "cannot be copied source member  " + libraryName + "/" + fileName + "("
                            + memberName + ") . Overwriting files is not allowed.";
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages(nodes);
                    return "ERROR";
                }

                //
                // Copy Source Physical File command
                // ---------------------------------
                String command_CPYSRCF = "CPYSRCF FROMFILE(" + inLibName + "/" + inFileName + ") "
                        + "TOFILE(" + libraryName + "/" + fileName + ") FROMMBR(" + inMbrName + ") TOMBR("
                        + memberName + ") MBROPT(*REPLACE)";

                // Perform the command
                cmdCall.run(command_CPYSRCF);

                int sourceCcsid = sourcePath.getCCSID();
                sourceCcsidStr = String.valueOf(sourceCcsid);

                int targetCcsid = targetPath.getCCSID();
                targetCcsidStr = String.valueOf(targetCcsid);

                // Get messages from the command if any
                AS400Message[] as400MessageList = cmdCall.getMessageList();
                // Send all messages from the command. After ESCAPE message - return.
                for (AS400Message as400Message : as400MessageList) {
                    if (as400Message.getType() == AS400Message.ESCAPE) {
                        row = "Error: Copy source member  " + inLibName + "/" + inFileName + "("
                                + inMbrName + ")  using command CPYSRCF   -  " + as400Message.getID() + " "
                                + as400Message.getText();
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                        return "ERROR";
                    } else {
                        row = "Info: Copy source member  " + inLibName + "/" + inFileName + "("
                                + inMbrName + ")  using command CPYSRCF  -  " + as400Message.getID() + " "
                                + as400Message.getText();
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                    }
                }

                row = "Info: Source member " + inLibName + "/" + inFileName + "(" + inMbrName
                        + ") was copied to source file  " + libraryName + "/" + fileName + "("
                        + memberName + "). Convert " + sourceCcsidStr + " -> " + targetCcsidStr + ".";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages(nodes);
                return "";

            } else {
                if (targetPath.isDirectory()) {
                    // To directory
                    // ------------
                    // If target is IFS directory, the new file path will be:
                    // directory + typed member name
                    newTargetPathString = targetPathString + "/" + typedMemberName;

                    // To file
                    // -------
                } else {
                    // If target is IFS file, the new file path will be:
                    // file's directory (prefix up to last "/") + typed member name
                    newTargetPathString = targetPathString.substring(0, targetPathString.lastIndexOf("/")) + "/"
                            + typedMemberName;
                }
            }

            // For both directory and file continue.
            // ---------------------------
            targetPath = new IFSFile(remoteServer, newTargetPathString);
            // Check if overwriting data is allowed
            if (targetPath.exists() && !overwriteAllowed) {
                // IFS file exists and overwriting data is NOT allowed
                row = "Error: Source member " + libraryName + "/" + fileName + "(" + typedMemberName
                        + ") cannot be copied to IFS directory " + targetPathString
                        + ". Overwriting files is not allowed.";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages(nodes);
                return "ERROR";
            }
            //
            // Copy source member to IFS file command
            // ------------------------------
            String commandText = "CPYTOSTMF FROMMBR('" + sourcePathString + "') TOSTMF('"
                    + newTargetPathString + "') STMFOPT(*REPLACE) "
                    + "CVTDTA(*AUTO) STMFCCSID(*STMF) ENDLINFMT(*LF)";

            // Perform the command
            cmdCall.run(commandText);

            // Obtain CCSID attribute of the target IFS file
            targetPath = new IFSFile(remoteServer, newTargetPathString);
            int sourceCcsid = sourcePath.getCCSID();
            sourceCcsidStr = String.valueOf(sourceCcsid);
            int targetCcsid;
            if (targetPath.exists()) {
                // CCSID attribute of IFS file
                targetCcsid = targetPath.getCCSID();
            } else {
                // CCSID attribute of source file
                targetCcsid = sourcePath.getCCSID();
            }
            targetCcsidStr = String.valueOf(targetCcsid);

            // Get messages from the command if any
            AS400Message[] as400MessageList = cmdCall.getMessageList();
            // Send all messages from the command. After ESCAPE message - return.
            for (AS400Message as400Message : as400MessageList) {
                if (as400Message.getType() == AS400Message.ESCAPE) {
                    row = "Error: Copy source member using command CPYTOSTMF to IFS file with CCSID "
                            + targetCcsidStr + ".  -  " + as400Message.getID() + " "
                            + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                    return "ERROR";
                } else {
                    row = "Info: Copy source member using command CPYTOSTMF to IFS file with CCSID "
                            + targetCcsidStr + ".  -  " + as400Message.getID() + " "
                            + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                }
            }
            // Positive completion message
            row = "Comp: Source member  " + sourcePathString + "  with CCSID " + sourceCcsidStr
                    + " was copied to IFS file  " + newTargetPathString + "  with CCSID attribute "
                    + targetCcsidStr + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();

        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error: Copy source member using command CPYTOSTMF to IFS file with CCSID "
                    + targetCcsidStr + ".  -  " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
        }
        return msgText;
    }

    /**
     * Copy Source File (all members) to IFS directory.
     *
     * @param remoteServer
     * @param sourcePathString
     * @param targetPathString
     * @return
     */
    protected String copyFromSourceFile(AS400 remoteServer, String sourcePathString, String targetPathString) {

        // Extract individual names (library, file, member) from the path of the source physical file
        extractNamesFromIfsPath(sourcePathString);
        String inLibName = libraryName;
        String inFileName = fileName;

        // Path to the input source file
        String inSourceFilePath = "/QSYS.LIB/" + libraryName + ".LIB/" + fileName + ".FILE";

        // Enable calling CL commands
        CommandCall cmdCall = new CommandCall(remoteServer);

        IFSFile inIfsDirectory = new IFSFile(remoteServer, inSourceFilePath);
        IFSFile outIfsDirectory = new IFSFile(remoteServer, targetPathString);

        try {

            // Check if target is a Source Physical File
            if (targetPathString.endsWith(".FILE") && outIfsDirectory.isSourcePhysicalFile()) {

                // Target is another source file
                // -----------------------------
                // Command CPYSRCF will be used.

                // Get object names from target path string for command "to"
                // parameters
                extractNamesFromIfsPath(targetPathString);
                String toFile = libraryName + "/" + fileName; // parameter TOFILE

                if (!overwriteAllowed) {
                    // Member is not overwtitten but printed.
                    toFile = "*PRINT"; // parameter TOFILE
                    row = "Info: Existing members of the source file  " + libraryName + "/" + fileName
                            + " will not be overwritten but printed. Overwriting files is not allowed.";
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                }

                // Build text of the command to copy all members
                String command_CPYSRCF = "CPYSRCF FROMFILE(" + inLibName + "/" + inFileName
                        + ") TOFILE(" + toFile + ") FROMMBR(*ALL) TOMBR(*FROMMBR) MBROPT(*REPLACE)";

                // Perform the command
                cmdCall.run(command_CPYSRCF);

                // Get messages from the command if any
                AS400Message[] as400MessageList = cmdCall.getMessageList();
                // Send all messages from the command. After ESCAPE message -
                // return.
                for (AS400Message as400Message : as400MessageList) {
                    if (as400Message.getType() == AS400Message.ESCAPE) {
                        row = "Error: Command CPYSRCF   -  " + as400Message.getID() + " "
                                + as400Message.getText();
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                        return "ERROR";
                    } else {
                        row = "Info: Command CPYSRCF  -  " + as400Message.getID() + " "
                                + as400Message.getText();
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                    }
                }
                if (!overwriteAllowed) {
                    row = "Comp: Members of source file " + inLibName + "/" + inFileName
                            + "  of matching names were copied to printer file QSYSPRT.";
                } else {
                    row = "Comp: Source file " + inLibName + "/" + inFileName
                            + "  was copied to source file  " + libraryName + "/" + fileName + ".";
                }
                mainWindow.msgVector.add(row);
                mainWindow.showMessages(nodes);
                return "";

            } else {
                // Check if target is a Library or IFS directory
                if (targetPathString.startsWith("/QSYS.LIB")) {

                    // Target is a Library
                    // -------------------
                    // Command CPYF will be used.

                    // Get input Library name from the path string of the input Source Physical File
                    extractNamesFromIfsPath(inSourceFilePath);
                    inLibName = libraryName;
                    inFileName = fileName;

                    extractNamesFromIfsPath(targetPathString);
                    String outLibName = libraryName;

                    String commandText = "CPYF FROMFILE(" + inLibName + "/" + inFileName + ")  TOFILE("
                            + outLibName + "/" + inFileName
                            + ") FROMMBR(*ALL) TOMBR(*FROMMBR) MBROPT(*REPLACE) CRTFILE(*YES) FMTOPT(*MAP)";

                    // Enable calling CL commands
                    CommandCall command = new CommandCall(remoteServer);
                    try {
                        // Run the command CPYF
                        command.run(commandText);

                        // Get messages from the command if any
                        AS400Message[] as400MessageList = command.getMessageList();
                        String msgType;
                        // Send all messages from the command. After ESCAPE message -
                        // return.
                        for (AS400Message as400Message : as400MessageList) {
                            if (as400Message.getType() == AS400Message.ESCAPE) {
                                msgType = "Error";
                                row = msgType + ": message from the CPYF command is " + as400Message.getID()
                                        + " " + as400Message.getText();
                                mainWindow.msgVector.add(row);
                                mainWindow.showMessages();
                                return "";
                            } else {
                                msgType = "Info";
                                row = msgType + ": message from the CPYF command is " + as400Message.getID()
                                        + " " + as400Message.getText();
                                mainWindow.msgVector.add(row);
                                mainWindow.showMessages();
                            }
                        }

                    } catch (Exception exc) {
                        exc.printStackTrace();
                        row = "Error: Copying source physical file  " + inSourceFilePath + " - "
                                + exc.toString() + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                        return "ERROR";
                    }

                    row = "Comp: Source physical file  " + inLibName + "/" + inFileName
                            + "  was copied to library  " + outLibName + ".";
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();

                    return "";
                } else {

                    // Target is an IFS directory
                    // --------------------------

                    // If the IFS directory name differs from the Source Physical File name 
                    // ---------------------------------
                    //  Create NEW directory with the Source Physical File name.
                    String ifsSrcName = inIfsDirectory.getName();
                    ifsSrcName = ifsSrcName.substring(0, ifsSrcName.indexOf("."));
                    String outDirEndName = targetPathString.substring(targetPathString.lastIndexOf("/") + 1);
                    if (!ifsSrcName.equals(outDirEndName)) {
                        targetPathString = targetPathString + "/" + ifsSrcName;
                        outIfsDirectory = new IFSFile(remoteServer, targetPathString);
                        outIfsDirectory.mkdir();
                    } // Continue as if IFS directory had the same name

                    // If the IFS directory has the same name as Source Physical File 
                    // --------------------------------------
                    // Copy all members from Source file to the IFS directory
                    boolean atLeastOneErrorInMembers = false;
                    IFSFile[] ifsFiles = inIfsDirectory.listFiles();
                    for (IFSFile ifsFile : ifsFiles) {
                        // Insert new IFS file or rewrite existing IFS file
                        String msgTextMbr = copyFromSourceMember(remoteServer, ifsFile.toString(), targetPathString + "/"
                                + ifsFile.getName());

                        // If at least one message is not empty - note error
                        if (!msgTextMbr.isEmpty()) {
                            atLeastOneErrorInMembers = true;
                        }
                    }

                    if (!atLeastOneErrorInMembers) {
                        row = "Comp: Source physical file  " + libraryName + "/" + fileName
                                + "  was copied to IFS directory  " + targetPathString + ".";
                        mainWindow.msgVector.add(row);
                    } else {
                        row = "Error: Source physical file  " + libraryName + "/" + fileName
                                + "  was NOT completely copied to IFS directory  " + targetPathString + ".";
                        mainWindow.msgVector.add(row);
                    }
                    mainWindow.showMessages();
                    msgText = atLeastOneErrorInMembers ? "ERROR" : "";
                    return msgText;
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error: Copying from source physical file  " + libraryName + "/" + fileName + "  -  "
                    + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
            return "ERROR";
        }
    }

    /**
     *
     * @param sourcePathString
     * @param targetPathString
     * @return
     */
    protected String copyToSourceMember(String sourcePathString, String targetPathString) {

        // Get object names from IFS path string: qsyslib, library, file, member.
        extractNamesFromIfsPath(targetPathString);

        try {
            // Create source file path
            IFSFile sourcePath = new IFSFile(remoteServer, sourcePathString);
            // Create target file path
            IFSFile targetPath = new IFSFile(remoteServer, targetPathString);

            // Check if member name is correct
            try {
                targetPath.exists();
            } catch (Exception exc) {
                row = "Error: IFS file  " + sourcePathString + "  cannot be copied to source member  "
                        + libraryName + "/" + fileName + "(" + memberName + ") . Member name is invalid.";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages(nodes);
                return "ERROR";
            }
            // Check if overwriting data is allowed
            if (targetPath.exists() && !overwriteAllowed) {
                // IFS file exists and overwriting data is NOT allowed
                row = "Error: IFS file  " + sourcePathString + "  cannot be copied to source member  "
                        + libraryName + "/" + fileName + "(" + memberName
                        + ") . Overwriting files is not allowed.";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages(nodes);
                return "ERROR";
            }

            // Change type suffix of the target file (path to MEMBER) to ".MBR" to comvene CPYFRMSTMF command
            String tps = targetPathString.substring(0, targetPathString.lastIndexOf(".")) + ".MBR";
 
            // Copy IFS file to Member
            String command_CPYFRMSTMF = "CPYFRMSTMF FROMSTMF('" + sourcePathString + "') TOMBR('"
                    + tps
                    + "') MBROPT(*REPLACE) CVTDTA(*AUTO) STMFCCSID(*STMF) DBFCCSID(*FILE)";

            // Enable calling CL commands
            CommandCall cmdCall = new CommandCall(remoteServer);
            
            cmdCall.run(command_CPYFRMSTMF);
            int sourceCcsid = sourcePath.getCCSID();
            sourceCcsidStr = String.valueOf(sourceCcsid);

            int targetCcsid = targetPath.getCCSID();
            targetCcsidStr = String.valueOf(targetCcsid);

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

            // Get source type from application parameters
            String sourceType = (String) properties.get("SOURCE_TYPE");
            // If *DEFAULT set standard source type, otherwise keep application parameter
            if (sourceType.equals("*DEFAULT")) {
                // Get default source type for standard source physical file name
                // (QCLSRC, QRPGLESRC, ...)
                sourceType = getDefaultSourceType(fileName);
            }

            String command_CHGPFM = "CHGPFM FILE(" + libraryName + "/" + fileName + ") MBR("
                    + memberName + ") SRCTYPE(" + sourceType + ")";

            // Perform command CGHPFM to correct source type of the member
            cmdCall.run(command_CHGPFM);

            // Get messages from the command if any
            as400MessageList = cmdCall.getMessageList();
            // Send all messages from the command. After ESCAPE message - return.
            for (AS400Message as400Message : as400MessageList) {
                if (as400Message.getType() == AS400Message.ESCAPE) {
                    row = "Error: Command CHGPFM   -  " + as400Message.getID() + " "
                            + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                    return "ERROR";
                } else {
                    row = "Info: Command CHGPFM  -  " + as400Message.getID() + " "
                            + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                }
            }

            row = "Comp: IFS file  " + sourcePathString + "  was copied to source member  "
                    + targetPathString + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();

        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error: Copy IFS file  " + sourcePathString + "  to source member  "
                    + targetPathString + "  using command CPYFRMSTMF  -  " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
        }

        msgText = "";
        return msgText;
    }

    /**
     * Copy IFS directory to Source File (all members).
     *
     * @param remoteServer
     * @param sourcePathString
     * @param targetPathString
     * @return
     */
    protected String copyToSourceFile(AS400 remoteServer, String sourcePathString, String targetPathString) {

        // Extract individual names (library, file, member) from the AS400 IFS
        // path
        extractNamesFromIfsPath(targetPathString);

        // Path to the output source file
        String outSourceFilePath = "/QSYS.LIB/" + libraryName + ".LIB/" + fileName + ".FILE";

        try {
            IFSFile inIfsDirFile = new IFSFile(remoteServer, sourcePathString);
            IFSFile outIfsDirectory = new IFSFile(remoteServer, outSourceFilePath);

            // If the Source Physical File name does not exist
            // -----------------------------------------------
            // Create Source Physical File.
            if (!outIfsDirectory.exists()) {
                outIfsDirectory.mkdir();
            }

            boolean atLeastOneErrorInMembers = false;

            // Copy IFS files to Source Members
            if (inIfsDirFile.isDirectory()) {
                // From IFS directory to Source file
                // ---------------------------------
                IFSFile[] ifsFiles = inIfsDirFile.listFiles();

                // Copy all objects of the IFS directory
                for (IFSFile ifsFile : ifsFiles) {
                    // Correct the source type
                    String ifsFileName = ifsFile.getName();
                    String mbrName = ifsFileName;
                    if (ifsFileName.lastIndexOf(".") >= 0) {
                        mbrName = ifsFileName.substring(0, ifsFileName.lastIndexOf("."));
                    }

                    // Directories, are not copied
                    if (ifsFile.isDirectory()) {
                        row = "Info: IFS directory  " + ifsFile
                                + "  was NOT copied to source physical file  " + libraryName + "/"
                                + fileName + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                        continue;
                        // Save files are not copied
                    } else if (ifsFileName.endsWith(".savf")) {
                        row = "Info: IFS save file  " + ifsFile
                                + "  was NOT copied to source physical file  " + libraryName + "/"
                                + fileName + ".";
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                        continue;
                    }

                    // Copy IFS file to Source member
                    String msgTextMbr = copyToSourceMember(ifsFile.toString(), targetPathString + "/" + mbrName + ".MBR");

                    // If at least one message is not empty - note error
                    if (!msgTextMbr.isEmpty()) {
                        atLeastOneErrorInMembers = true;
                    }
                }

                if (!atLeastOneErrorInMembers) {
                    row = "Comp: IFS directory  " + sourcePathString
                            + "  was copied to source physical file  " + libraryName + "/" + fileName
                            + ".";
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                } else {
                    row = "Comp: IFS directory  " + sourcePathString
                            + "  was NOT completely copied to source physical file  " + libraryName + "/"
                            + fileName + ".";
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                }
                msgText = atLeastOneErrorInMembers ? "ERROR" : "";
                return msgText;

            } else {

                // From IFS file to Source file
                // ----------------------------

                // Correct source type
                String ifsFileName = inIfsDirFile.getName();
                String mbrName = ifsFileName;
                if (ifsFileName.lastIndexOf(".") >= 0) {
                    mbrName = ifsFileName.substring(0, ifsFileName.lastIndexOf("."));
                }

                // Copy IFS object to Source file
                String msgTextMbr = copyToSourceMember(sourcePathString, targetPathString + "/" + mbrName + ".MBR");

                // If at least one message is not empty - note error
                if (!msgTextMbr.isEmpty()) {
                    atLeastOneErrorInMembers = true;
                }
                if (!atLeastOneErrorInMembers) {
                    row = "Comp: IFS file  " + sourcePathString
                            + "  was copied to Source physical file  " + libraryName + "/" + fileName
                            + ".";
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages(nodes);
                } else {
                    row = "Comp: IFS file  " + sourcePathString
                            + "  was NOT copied to Source physical file  " + libraryName + "/" + fileName
                            + ".";
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages(nodes);
                }
                msgText = atLeastOneErrorInMembers ? "ERROR" : "";
                return msgText;

            }
        } catch (IOException exc) {
            exc.printStackTrace();
            row = "Error: Copying from IFS object  " + sourcePathString + "  -  " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes);
            return "ERROR";
        }
    }

    /**
     * Copy Save File to IFS file.
     *
     * @param remoteServer
     * @param sourcePathString
     * @param targetPathString
     * @return
     */
    protected String copyFromSaveFile(AS400 remoteServer, String sourcePathString, String targetPathString) {

        // Enable calling CL commands
        CommandCall cmdCall = new CommandCall(remoteServer);

        IFSFile sourcePath = new IFSFile(remoteServer, sourcePathString);
        IFSFile targetPath = new IFSFile(remoteServer, targetPathString);

        // Extract individual names (library, file, member) from the AS400 IFS path
        extractNamesFromIfsPath(sourcePathString);
        String inLibName = libraryName;
        String inFileName = saveFileName;

        // Extract individual names (library, file, member) from the AS400 IFS
        // path
        extractNamesFromIfsPath(targetPathString);
        try {

            // To Library
            // ----------
            if (targetPathString.startsWith("/QSYS.LIB")) {

                // Build command string
                String command_CRTDUPOBJ = "CRTDUPOBJ OBJ(" + inFileName + ") FROMLIB(" + inLibName
                        + ") OBJTYPE(*FILE) " + "TOLIB(" + libraryName + ") NEWOBJ(*OBJ) DATA(*YES)";

                IFSFile newTargetPath = targetPath;

                // For copy to Library, derive new target file path (by appending source file name)
                if (targetPathString.endsWith(".LIB")) {
                    newTargetPath = new IFSFile(remoteServer, targetPathString + "/" + sourcePath.getName());
                }
                // Check if the file to copy already exists and if can be overwritten.
                if (newTargetPath.exists() && !overwriteAllowed) {
                    row = "Error: Save file  " + inLibName + "/" + fileName
                            + "  was NOT copied. Overwriting is not allowed.";
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                    return "ERROR";
                }

                // Perform the command. This command does not allow overwriting files with matching name,
                // even if allowed by "Overwrite data" parameter.
                cmdCall.run(command_CRTDUPOBJ);

                // Get messages from the command if any
                AS400Message[] as400MessageList = cmdCall.getMessageList();
                // Send all messages from the command. After ESCAPE message - return.
                for (AS400Message as400Message : as400MessageList) {
                    if (as400Message.getType() == AS400Message.ESCAPE) {
                        row = "Error: Copy save file with command CRTDUPOBJ   -  " + as400Message.getID()
                                + " " + as400Message.getText();
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                        return "ERROR";
                    } else {
                        row = "Info: Copy save file with command CRTDUPOBJ  -  " + as400Message.getID()
                                + " " + as400Message.getText();
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                    }
                }

                row = "Comp: Save file  " + libraryName + "/" + fileName + "  was copied to library  "
                        + targetPathString + ".";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
                return "";

            } // To IFS directory
            // ----------------
            else {

                // Extract individual names (library, file, member) from the AS400 IFS path
                extractNamesFromIfsPath(sourcePathString);
                String toStmf = targetPathString;

                if (targetPath.isDirectory()) {
                    toStmf = targetPathString + "/" + sourcePath.getName();
                }
                sourcePathString = sourcePathString.replace(".SAVF", ".FILE");
                toStmf = toStmf.replace(".FILE", ".savf");
                IFSFile newTargetPath = new IFSFile(remoteServer, toStmf);

                // Check if the file to copy already exists and if can be overwritten.
                if (newTargetPath.exists() && !overwriteAllowed) {
                    row = "Error: Save file  " + inLibName + "/" + inFileName
                            + "  was NOT copied to existing save file  " + newTargetPath.getName().replace(".SAVF", ".savf")
                            + ". Overwriting is not allowed.";
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                    return "ERROR";
                }

                // Copy from save file to IFS file
                String command_CPYTOSTMF = "CPYTOSTMF FROMMBR('" + sourcePathString + "') " + "TOSTMF('"
                        + toStmf + "') STMFOPT(*REPLACE)";
                command_CPYTOSTMF = command_CPYTOSTMF.replace(".SAVF", ".savf");

                // Perform the command
                cmdCall.run(command_CPYTOSTMF);

                // Get messages from the command if any
                AS400Message[] as400MessageList = cmdCall.getMessageList();
                // Send all messages from the command. After ESCAPE message -
                // return.
                for (AS400Message as400Message : as400MessageList) {
                    if (as400Message.getType() == AS400Message.ESCAPE) {
                        row = "Error: Copy save file with command CPYTOSTMF   -  " + as400Message.getID()
                                + " " + as400Message.getText();
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                        return "ERROR";
                    } else {
                        row = "Info: Copy save file with command CPYTOSTMF  -  " + as400Message.getID()
                                + " " + as400Message.getText();
                        mainWindow.msgVector.add(row);
                        mainWindow.showMessages();
                    }
                }

                row = "Comp: Save file  " + libraryName + "/" + fileName
                        + "  was copied to IFS directory  " + targetPathString + ".";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();

                return "";

            }
        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error: Copying save file  " + libraryName + "/" + fileName + "  -  "
                    + exc.toString() + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
            return "ERROR";
        }
    }

    /**
     *
     * @param sourcePathString
     * @return
     */
    protected String copyToSaveFile(String sourcePathString, String targetPathString) {

        // Get object names from IFS path string: qsyslib, library, file, member.
        extractNamesFromIfsPath(targetPathString);
        try {
            // Create target file path
            IFSFile sourcePath = new IFSFile(remoteServer, sourcePathString);
            // Create target file path
            IFSFile targetPath = new IFSFile(remoteServer, targetPathString);

            if (targetPathString.endsWith(".LIB")) {
                // Add file name to directory path
                targetPathString = targetPathString + "/" + sourcePath.getName();
            }
            // Replace .savf suffix to .FILE for the save file
            targetPathString = targetPathString.replace(".savf", ".FILE");

            targetPath = new IFSFile(remoteServer, targetPathString);

            if (targetPath.exists() && !overwriteAllowed) {
                // IFS file exists and overwriting data is NOT allowed
                row = "Error: IFS file  " + sourcePathString + "  cannot be copied to save file  "
                        + libraryName + "/" + fileName + "(" + memberName
                        + ") . Overwriting files is not allowed.";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages(nodes);
                return "ERROR";
            }

            // Copy IFS file to Save file
            String command_CPYFRMSTMF = "CPYFRMSTMF FROMSTMF('" + sourcePathString + "') TOMBR('"
                    + targetPathString + "') MBROPT(*REPLACE) CVTDTA(*AUTO) STMFCCSID(*STMF)";

            // Enable calling CL commands
            CommandCall cmdCall = new CommandCall(remoteServer);

            cmdCall.run(command_CPYFRMSTMF);

            // Get messages from the command if any
            AS400Message[] as400MessageList = cmdCall.getMessageList();
            // Send all messages from the command. After ESCAPE message - return.
            for (AS400Message as400Message : as400MessageList) {
                if (as400Message.getType() == AS400Message.ESCAPE) {
                    row = "Error: Copy IFS file to save file with command CPYFRMSTMF  -  "
                            + as400Message.getID() + " " + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                    return "ERROR";
                } else {
                    row = "Info: Copy IFS file to save file with command CPYFRMSTMF  -  "
                            + as400Message.getID() + " " + as400Message.getText();
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                }
            }

            row = "Comp: IFS file  " + sourcePathString + "  was copied to save file  "
                    + targetPathString + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error: Copy IFS file to save file with command CPYFRMSTMF  -  " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
        }

        msgText = "";
        return msgText;
    }

    /**
     * Get default source type for standard source physical file name (QCLSRC,
     * QRPGLESRC, ...)
     *
     * @param sourceFileName
     * @return
     */
    protected String getDefaultSourceType(String sourceFileName) {
        String sourceType;
        sourceType = sourceFilesAndTypes.get(sourceFileName);
        if (sourceType == null) {
            sourceType = "MBR";
        }
        return sourceType;
    }

    /**
     * Extract individual names (libraryName, fileName, memberName, saveFileName) from the IBM
     * i IFS path.
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
                        if (as400PathString.contains(".FILE/")) {
                            memberName = as400PathString.substring(as400PathString.lastIndexOf("/") + 1, as400PathString.lastIndexOf("."));
                        }
                    } else if (as400PathString.endsWith(".SAVF")) {
                        saveFileName = as400PathString.substring(as400PathString.lastIndexOf("/") + 1, as400PathString.lastIndexOf(".SAVF"));
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
