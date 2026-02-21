package copyfiles;

import com.ibm.as400.access.AS400;
import java.awt.Color;
import java.awt.Cursor;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.text.BadLocationException;

/**
 * This SwingWorker class finds matching files (that contain text patterns).
 *
 * @author Vladimír Župka 2017
 */
public class SearchMatchingFiles extends SwingWorker<String, String> {

    MainWindow mainWindow;
    SearchWindow searchWindow;
    String fileType;

    // Constructor
    SearchMatchingFiles(AS400 remoteServer, MainWindow mainWindow, SearchWindow searchWindow, String fileType) {
        this.mainWindow = mainWindow;
        this.searchWindow = searchWindow;
        this.fileType = fileType;
    }

    /**
     * Perform method findMatchingFiles(), it runs as a SwingWorker background task.
     */
    @Override
    public String doInBackground() {
        searchMatchingFiles();
        return "";
    }

    /**
     * Concludes the SwingWorker background task; it is not needed here.
     */
    @Override
    public void done() {
    }

    protected void searchMatchingFiles() {

        searchWindow.inputText = searchWindow.findField.getText();
        // Pattern depends on the intensity of "Aa" icon.
        // Dim = Case insensitive,
        // Dark = Match case.
        searchWindow.pattern = searchWindow.getPattern();
        searchWindow.filePathsArrList = new ArrayList<>();
        searchWindow.fileHitsArrList = new ArrayList<>();

        // Search all selected files
        for (String clipboardPathString : mainWindow.clipboardPathStrings) {
            mainWindow.sourcePathString = clipboardPathString;
            searchWindow.textArea = new JTextArea();
            // Decide what type of file is being processed and read data to the text area.
            switch (fileType) {
                case "PC":
                    searchWindow.textArea = searchWindow.readPcFile();
                    break;
                case "IFS":
                    searchWindow.textArea = searchWindow.readIfsFile();
                    break;
                case "MBR":
                    searchWindow.textArea = searchWindow.readSourceMember();
                    break;
                default:
                    break;
            }
            // Search for pattern in the text area
            if (searchWindow.pattern == null) {
                return;
            }
            int hits = 0;
            if (Objects.nonNull(searchWindow.pattern)) {
                try {
                    Matcher matcher = searchWindow.pattern
                            .matcher(searchWindow.textArea.getText(0, searchWindow.textArea.getText().length()));
                    int pos = 0;

                    while (matcher.find(pos)) {
                        //int start = matcher.start();
                        int end = matcher.end();
                        pos = end;
                        hits++;
                    }
                } catch (BadLocationException exc) {
                    exc.printStackTrace();
                }
                if (hits == 0) {
                    continue; // Skip file with no text matches.
                }
                // Add file path and the number of its matched patterns to an array list.
                searchWindow.filePathsArrList.add(clipboardPathString);
                searchWindow.fileHitsArrList.add(hits);
            }
        }
        // Copy array lists to arrays for table rows.
        searchWindow.filePaths = new String[searchWindow.filePathsArrList.size()];
        searchWindow.fileHits = new int[searchWindow.fileHitsArrList.size()];
        for (int file = 0; file < searchWindow.filePathsArrList.size(); file++) {
            if (fileType.equals("MBR")) {
                searchWindow.extractNamesFromIfsPath(searchWindow.filePathsArrList.get(file));
                searchWindow.filePaths[file] = searchWindow.libraryName + "/" + searchWindow.fileName + "(" + searchWindow.memberName + ")";
            } else {
                // Put whole path to the table column
                searchWindow.filePaths[file] = searchWindow.filePathsArrList.get(file);
            }
            searchWindow.fileHits[file] = searchWindow.fileHitsArrList.get(file);
        }

        // Recreate panel and show window
        // (Both panel and scroll pane must be cleared)
        searchWindow.panel.removeAll();
        searchWindow.scrollPane.removeAll();
        searchWindow.scrollPane.removeAll();
        searchWindow.scrollPane = new JScrollPane(searchWindow.matchedFilesTable);
        searchWindow.scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        // Add top panel
        searchWindow.panel.add(searchWindow.topPanel);
        // Add scroll pane
        searchWindow.panel.add(searchWindow.scrollPane);
        searchWindow.setVisible(true);

        mainWindow.row = "Comp: Search for matching files ended.";
        mainWindow.msgVector.add(mainWindow.row);
        mainWindow.showMessages(mainWindow.noNodes);
        // Change cursor to default
        mainWindow.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        // Remove setting last element of messages
        mainWindow.scrollMessagePane.getVerticalScrollBar()
                .removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
    }
}
