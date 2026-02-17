package copyfiles;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.SpooledFile;
import java.awt.Cursor;
import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

/**
 * Create a window for reading and displaying spooled files from output queues (*OUTQ).
 *
 * @author Vladimír Župka, 2016
 */
public class WrkSplFCall extends SwingWorker<String, String> {

    WrkSplF wrkSplf;

    AS400 remoteServer;
    MainWindow mainWindow;
    String rightPathString;
    boolean currentUser;
    int compileWindowX;
    int compileWindowY;
    String className;
    boolean spoolTable;
    String userPar;
    String ibmCcsid;

    Path parPath = Paths.get(System.getProperty("user.dir"), "paramfiles", "Parameters.txt");
    String encoding = System.getProperty("file.encoding", "UTF-8");

    /**
     * Constructor.
     *
     * @param remoteServer
     * @param mainWindow
     * @param rightPathString
     * @param currentUser
     */
    WrkSplFCall(AS400 remoteServer, MainWindow mainWindow, String rightPathString,
            boolean currentUser, int compileWindowX, int compileWindowY, String className, boolean spoolTable) {
        this.remoteServer = remoteServer;
        this.mainWindow = mainWindow;
        this.rightPathString = rightPathString;
        this.currentUser = currentUser;
        this.compileWindowX = compileWindowX;
        this.compileWindowY = compileWindowY;
        this.className = className;
        this.spoolTable = spoolTable;

        Properties properties = new Properties();
        try {
            BufferedReader infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
            properties.load(infile);
            infile.close();
        } catch (Exception exc) {
            exc.printStackTrace();
        }

        userPar = properties.getProperty("USERNAME");

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
    }

    /**
     * Perform method createSpoolWindow(), it runs as a SwingWorker background task.
     *
     * @return
     */
    @Override
    public String doInBackground() {
        wrkSplf = new WrkSplF(remoteServer, mainWindow, rightPathString, currentUser, compileWindowX, compileWindowY, className);
        if (spoolTable) {
            createSpoolTable();
        } else {
            displayLastSpooledFile();
        }
        return "";
    }

    /**
     * Concludes the SwingWorker background task; it is not needed here.
     */
    @Override
    public void done() {
    }

    /**
     * Create window to work with spooled files in a table.
     */
    protected void createSpoolTable() {
        // Change cursor to wait cursor
        mainWindow.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        wrkSplf.createSpoolWindow(currentUser);

        // Change cursor to default
        mainWindow.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        // Remove message scroll listener (cancel scrolling to the last message)
        mainWindow.scrollMessagePane.getVerticalScrollBar().
                removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
    }

    /**
     * Get the last from the list of all spooled files.
     */
    protected void displayLastSpooledFile() {
        // Change cursor to wait cursor
        mainWindow.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // Resulting splf is the last from the list of all spooled files belonging to the current user.
        SpooledFile splf = wrkSplf.selectSpooledFiles("", "", "", userPar, "", "", "", "");
        if (splf != null) {
            String spoolTextAreaString = wrkSplf.convertSpooledFile(splf);
            JTextArea textArea = new JTextArea();
            DisplayFile dspf = new DisplayFile(textArea, mainWindow);
            dspf.displayTextArea(spoolTextAreaString, ibmCcsid);
        } 
        // Change cursor to default
        mainWindow.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        // Remove message scroll listener (cancel scrolling to the last message)
        mainWindow.scrollMessagePane.getVerticalScrollBar().
                removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
    }
}
