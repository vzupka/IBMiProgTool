package copyfiles;

import java.util.List;

import javax.swing.SwingWorker;
import javax.swing.TransferHandler;

import com.ibm.as400.access.AS400;

public class ParallelCopy_IBMi_PC extends SwingWorker<String, String> {
   AS400 remoteServer;
   String[] clipboardPathStrings;
   String targetPathString;
   TransferHandler.TransferSupport leftInfo;
   MainWindow mainWindow;

   boolean nodes = true;

   ParallelCopy_IBMi_PC(AS400 remoteServer, String[] clipboardPathStrings, String targetPathString, 
         TransferHandler.TransferSupport leftInfo, MainWindow mainWindow) {
      this.remoteServer = remoteServer;
      this.clipboardPathStrings = clipboardPathStrings;
      this.targetPathString = targetPathString;
      this.leftInfo = leftInfo;
      this.mainWindow = mainWindow;
   }

   /**
    * Perform method copyingFromPC(), it runs as a SwingWorker background task
    * and returns a message text.
    * 
    * @return
    */
   @Override
   public String doInBackground() {
      parallelCopy_IBMi_PC();
      return "";
   }

   /**
    * Concludes the SwingWorker background task getting the message text
    * (task's result).
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
    * Transfer all clipboard objects to the target directory
    */
    protected void parallelCopy_IBMi_PC() {

        for (String sourcePathString : clipboardPathStrings) {
            Copy_IBMi_PC ibmpc = new Copy_IBMi_PC(remoteServer, sourcePathString, targetPathString, mainWindow);
            ibmpc.copyingToPC(sourcePathString, targetPathString);
        }
        if (leftInfo != null) {
            mainWindow.expandLeftTreeNode(leftInfo);
        } else {
            mainWindow.reloadLeftSide(nodes);
        }
        mainWindow.scrollMessagePane.getVerticalScrollBar()
                .removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
    }
}
