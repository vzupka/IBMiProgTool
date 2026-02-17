package copyfiles;

import java.util.List;

import javax.swing.SwingWorker;
import javax.swing.TransferHandler;

import com.ibm.as400.access.AS400;

public class ParallelCopy_PC_IBMi extends SwingWorker<String, String> {
   
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
   ParallelCopy_PC_IBMi(AS400 remoteServer, String[] clipboardPathStrings, String targetPathString, 
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
      parallelCopy_PC_IBMi();
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
    * Copying from PC to IBMi.
    */
   protected void parallelCopy_PC_IBMi() {
       // Copy from PC to IBM i
       for (String sourcePathString : clipboardPathStrings) {
           Copy_PC_IBMi cpfpc = new Copy_PC_IBMi(remoteServer, sourcePathString,
                   targetPathString, mainWindow);
           cpfpc.copyingFromPC();
       }
      if (rightInfo != null) {
         mainWindow.expandRightTreeNode(rightInfo);
      } else {
         mainWindow.reloadRightSide();
      }
      mainWindow.scrollMessagePane.getVerticalScrollBar().removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
   }
}
