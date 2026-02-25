package copyfiles;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;

/**
 * This class performs copying files or directories from PC to PC itself.
 *
 * @author Vladimír Župka, 2016
 */
public class Copy_PC_PC {

    String sourcePathString;
    String targetPathString;
    MainWindow mainWindow;

    String sourcePathStringPrefix;
    String row = new String();

    Properties properties;
    Path parPath = Paths.get(System.getProperty("user.dir"), "paramfiles", "Parameters.txt");
    String encoding = System.getProperty("file.encoding", "UTF-8");

    String pcFileSep; // PC file separator ( / in unix, \ in Windows )

    boolean nodes = true;
    boolean noNodes = false;

    /**
     * Constructor.
     *
     * @param sourcePathString
     * @param targetPathString
     * @param mainWindow
     */
    Copy_PC_PC(String sourcePathString, String targetPathString, MainWindow mainWindow) {
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
    }

    /**
     * Initial method calling further methods for copying from PC to PC.
     */
    protected void copy_PC_PC() {

        if (targetPathString.equals(sourcePathString)) {
            return;
        }

        // PC Paths containing /. or \. are skipped
        if (!sourcePathString.contains(pcFileSep + ".")) {

            // Add message scroll listener (cancel scrolling to the last message)
//         mainWindow.scrollMessagePane.getVerticalScrollBar()
//                  .addAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);

            // Path prefix is the leading part of the path up to and including the last slash:
            // e.g.  /Users/vzupka/Moje dokumenty/ZZZZ/CCC/DDD/TESTPROGC.RPGLE   
            // ----> /Users/vzupka/Moje dokumenty/ZZZZ/CCC/DDD/
            sourcePathStringPrefix = sourcePathString.substring(0, sourcePathString.lastIndexOf(pcFileSep));
            if (Files.isDirectory(Paths.get(sourcePathString))) {

                // From PC directory
                // -----------------
                copyPcDirectory(sourcePathString, targetPathString, sourcePathStringPrefix);

            } else {

                // From simple PC file
                // -------------------
                copyPcFile(sourcePathString, targetPathString);
            }
        }

        // Remove message scroll listener (cancel scrolling to the last message)
        //mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
    }

    /**
     * Copy simple PC file to PC directory or file.
     *
     * @param sourcePathString
     * @param targetPathString
     * @return
     */
    @SuppressWarnings("UseSpecificCatch")
    protected String copyPcFile(String sourcePathString, String targetPathString) {

        // Cannot copy to itself
        if (sourcePathString.equals(targetPathString)) {
            return null;
        }

        // Path to source file
        Path sourceFilePath = Paths.get(sourcePathString);
        // Path to target file
        Path targetPath = Paths.get(targetPathString);
        try {

            if (Files.isDirectory(targetPath)) {
                // Copying file to directory
                // -------------------------

                // If copying to a directory, build target file path by appending file name
                String filePathString = targetPathString + pcFileSep + sourceFilePath.getFileName();
                targetPath = Paths.get(filePathString);

                // If the output file already exists and overwrite is not allowed - return with error.                
                if (Files.exists(targetPath) && !properties.getProperty("OVERWRITE_FILE").equals("Y")) {
                    row = "Error1: PC file  " + sourcePathString + "  was NOT copied to the existing file  "
                            + targetPathString + ". Overwriting files is not allowed.";
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages(noNodes);
                    return "ERROR";
                }

                // Copy file
                Files.copy(sourceFilePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            } else {
                // Copying file to file
                // --------------------

                // If the output file already exists and overwrite is not allowed - return with error.                
                if (Files.exists(targetPath) && !properties.getProperty("OVERWRITE_FILE").equals("Y")) {
                    row = "Error2: PC file  " + sourcePathString + "  was NOT copied to the existing file  "
                            + targetPathString + ". Overwriting files is not allowed.";
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages(noNodes);
                    return "ERROR";
                }

                // Copy file 
                Files.copy(sourceFilePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            }
            row = "Comp: PC file  " + sourcePathString + "  was copied to PC file  " + targetPathString + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes);
            return "";
        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error: Copying PC file  " + sourcePathString + "  to PC file  " + targetPathString + ".  -  " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes);
            return "ERROR";
        }
    }

    /**
     * Copy from PC directory to PC directory.
     *
     * @param sourcePathString
     * @param targetPathString
     * @param sourcePathStringPrefix
     * @return
     */
    protected String copyPcDirectory(String sourcePathString, String targetPathString, String sourcePathStringPrefix) {
        Path sourcePath = Paths.get(sourcePathString);

        // Cannot copy to itself
        if (sourcePathString.equals(targetPathString)) {
            return null;
        }

        // Create the Visitor object
        Visitor visitor = new Visitor();
        try {
            // Walk through the input directory and copy subdirectories and subfiles
            Files.walkFileTree(sourcePath, visitor);

            row = "Comp: Copying PC directory  " + sourcePathString + "  to PC directory  " + targetPathString + "  has ended.";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes);
            return "";

        } catch (Exception exc) {
            exc.printStackTrace();
            row = "Error: Copying PC directory  " + sourcePathString + "  to PC directory  " + targetPathString + ".  -  " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes);
            return "ERROR";
        }
    }

    /**
     * Class Visitor controls recursive processing of the input directory tree.
     */
    class Visitor extends SimpleFileVisitor<Path> {

        Path newDirectory;
        Path pcObject;

        @Override
        public FileVisitResult visitFile(Path pdObject, BasicFileAttributes attr) {
            this.pcObject = pdObject;
            Path newFile = newDirectory.resolve(pdObject.getFileName());
            try {
                // Copy input file as a new file or overwrite the existing file
                // -----------------------------
                Files.copy(pdObject, newFile, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);

                row = "Info: Copy PC file  " + newFile + "  to PC didrectory  " + targetPathString + ".";
                mainWindow.msgVector.add(row);
                mainWindow.showMessages(nodes);
                return FileVisitResult.CONTINUE;
            } catch (Exception exc) {
                exc.printStackTrace();
                row = "Error: Copying PC file  " + newFile + "  to PC didrectory  " + targetPathString + ".  -  " + exc.toString();
                mainWindow.msgVector.add(row);
                mainWindow.showMessages(nodes);
                return FileVisitResult.SKIP_SIBLINGS;
            }
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attr) {
            Path targetPath = Paths.get(targetPathString);
            Path prefixPath = Paths.get(sourcePathStringPrefix);
            newDirectory = targetPath.resolve(prefixPath.relativize(dir));
            try {
                // Copy input directory as a new directory
                // ---------------------------------------
                Files.copy(dir, newDirectory);

                row = "Info: Copying PC directory  " + newDirectory + "  to PC directory  " + targetPathString + ".";
                mainWindow.msgVector.add(row);
                return FileVisitResult.CONTINUE;
            } catch (Exception exc) {
                exc.printStackTrace();
                row = "Error: Copying PC directory  " + newDirectory + "  to PC directory  " + targetPathString + ".  -  " + exc.toString();
                mainWindow.msgVector.add(row);
                mainWindow.showMessages(nodes);
                return FileVisitResult.SKIP_SUBTREE;
            }
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            exc.printStackTrace();
            row = "Error: Copying PC object  " + pcObject + "  to PC object  " + targetPathString + ".  -  " + exc.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes);
            return FileVisitResult.TERMINATE;
        }
    }

}
