package copyfiles;

import java.util.List;

import javax.swing.SwingWorker;
import javax.swing.TransferHandler;

public class ParallelCopy_PC_PC extends SwingWorker<String, String> {

    String[] clipboardPathStrings;
    String targetPathString;
    TransferHandler.TransferSupport leftInfo;
    MainWindow mainWindow;

    boolean nodes = true;

    ParallelCopy_PC_PC(String[] clipboardPathStrings, String targetPathString,
            TransferHandler.TransferSupport leftInfo, MainWindow mainWindow) {
        this.clipboardPathStrings = clipboardPathStrings;
        this.targetPathString = targetPathString;
        this.leftInfo = leftInfo;
        this.mainWindow = mainWindow;
    }

    /**
     * Perform method parallelCopy_PC_PC(), it runs as a SwingWorker background task and returns a
     * message text.
     *
     * @return
     */
    @Override
    public String doInBackground() {
        parallelCopy_PC_PC();
        return "";
    }

    /**
     * Concludes the SwingWorker background task getting the message text (task's result).
     */
    @Override
    public void done() {
    }

    /**
     * Updates message by "published" intermediate messages
     *
     * @param strings
     */
    @Override
    protected void process(List<String> strings) {
    }

    /**
     *
     */
    protected void parallelCopy_PC_PC() {
        // Add message scroll listener (cancel scrolling to the last message)
//      mainWindow.scrollMessagePane.getVerticalScrollBar()
//               .addAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
        for (String sourcePathString : clipboardPathStrings) {
            Copy_PC_PC pcpc = new Copy_PC_PC(sourcePathString, targetPathString, mainWindow);
            pcpc.copy_PC_PC();
        }
        if (leftInfo != null) {
            mainWindow.expandLeftTreeNode(leftInfo);
        } else {
            mainWindow.reloadLeftSide(nodes);
        }
        mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
    }
}
