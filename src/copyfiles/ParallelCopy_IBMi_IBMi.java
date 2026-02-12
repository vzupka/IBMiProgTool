package copyfiles;

import java.util.List;

import javax.swing.SwingWorker;
import javax.swing.TransferHandler;

import com.ibm.as400.access.AS400;

public class ParallelCopy_IBMi_IBMi extends SwingWorker<String, String> {
   
   AS400 remoteServer;
   String[] clipboardPathStrings; 
   String targetPathString;
   TransferHandler.TransferSupport rightInfo;
   MainWindow mainWindow;
   
   /**
    * Constructor
    * 
    * @param clipboardPathStrings
    * @param rightPathStrings
    * @param mainWindow
    */
   ParallelCopy_IBMi_IBMi(AS400 remoteServer, String[] clipboardPathStrings, String targetPathString, 
         TransferHandler.TransferSupport rightInfo, MainWindow mainWindow) {
      this.remoteServer = remoteServer;
      this.clipboardPathStrings = clipboardPathStrings;
      this.targetPathString = targetPathString;
      this.rightInfo = rightInfo;
      this.mainWindow = mainWindow;
   }
   
   /**
    * Perform method parallelCopy_IBMi_IBMi(), it runs as a SwingWorker background task and returns a message text.
    * 
    * @return
    */
   @Override
   public String doInBackground() {
      parallelCopy_IBMi_IBMi();
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
    * Copying from IBM i to IBMi.
    */
   protected void parallelCopy_IBMi_IBMi() {
       // Copy from IBM i to IBM i
       for (String sourcePathString : clipboardPathStrings) {
           Copy_IBMi_IBMi cpii = new Copy_IBMi_IBMi(remoteServer, sourcePathString,
                   targetPathString, mainWindow);
           cpii.copy_IBMi_IBMi();
       }
      if (rightInfo != null) {
         mainWindow.expandRightTreeNode(rightInfo);
      } else {
         mainWindow.reloadRightSide();
      }
      mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
   }
}
