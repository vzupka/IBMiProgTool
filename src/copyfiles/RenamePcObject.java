package copyfiles;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 
 * @author Vladimír Župka, 2017
 */
public class RenamePcObject {

   MainWindow mainWindow;
   String pcFileSep;

   int currentX;
   int currentY;

   /**
    * Constructor.
    * 
    * @param mainWindow
    * @param pcFileSep
    */
   RenamePcObject(MainWindow mainWindow, String pcFileSep, int currentX, int currentY) {
      this.mainWindow = mainWindow;
      this.pcFileSep = pcFileSep;
      this.currentX = currentX;
      this.currentY = currentY;
   }

   /**
    * Rename or move the file
    * 
    * @param oldPathString
    */
   protected void renamePcObject(String oldPathString) {
      GetTextFromDialog getText = new GetTextFromDialog("Rename PC object");
      String oldFilePrefix = oldPathString.substring(0, oldPathString.lastIndexOf(pcFileSep));
      String oldFileName = oldPathString.substring(oldPathString.lastIndexOf(pcFileSep) + 1);

      // "false" stands for not changing result to upper case
      String newFileName = getText.getTextFromDialog("Parent directory", "New name", oldFilePrefix, oldFileName, false, currentX, currentY);
      if (newFileName == null) {
         return;
      }
      if (newFileName.isEmpty()) {
         newFileName = oldFileName;
      }
      String renamedPathString = oldFilePrefix + pcFileSep + newFileName;
      try {
         // Rename the file
         // ---------------
         Files.move(Paths.get(oldPathString), Paths.get(renamedPathString));
      } catch (Exception exc) {
         exc.printStackTrace();
         String row = "Error: Renaming PC object -  " + exc.toString();
         mainWindow.msgVector.add(row);
         mainWindow.showMessages(true);
      }
      // Change left node (source node selected by mouse click)
      mainWindow.leftNode.setUserObject(newFileName);
      mainWindow.leftTreeModel.nodeChanged(mainWindow.leftNode);

      // Send completion message
      String row = "Comp: PC object  " + oldPathString + "  was renamed to  " + renamedPathString + ".";
      mainWindow.msgVector.add(row);
      mainWindow.showMessages(false);
      mainWindow.scrollMessagePane.getVerticalScrollBar()
               .removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);

   }
}
