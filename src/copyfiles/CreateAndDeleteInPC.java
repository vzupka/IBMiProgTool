package copyfiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import com.sun.jna.platform.FileUtils;
import java.awt.Desktop;
/**
 * 
 * @author vzupka
 */
public class CreateAndDeleteInPC {

   String methodName;
   MainWindow mainWindow;
   String row;

   boolean nodes = true;

   int currentX;
   int currentY;

   /**
    * Constructor.
    */
   CreateAndDeleteInPC(MainWindow mainWindow, String methodName, int currentX, int currentY) {
      this.mainWindow = mainWindow;
      this.methodName = methodName;
      this.currentX = currentX;
      this.currentY = currentY;
   }

   /**
    * Decision what method to call.
    */
   protected void createAndDeleteInPC() {

      //leftPathString = mainWindow.leftPathString;

      // Add message scroll listener (cancel scrolling to the last message)
      mainWindow.scrollMessagePane.getVerticalScrollBar()
               .addAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);

      if (methodName.equals("createPcDirectory")) {
         createPcDirectory();
      }
      if (methodName.equals("createPcFile")) {
         createPcFile();
      }
      if (methodName.equals("movePcObjectToTrash")) {
         movePcObjectToTrash();
      }
      
      // Remove message scroll listener (cancel scrolling to the last message)
      mainWindow.scrollMessagePane.getVerticalScrollBar()
               .removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
   }

   /**
    * Create PC directory.
    */
   protected void createPcDirectory() {

      // "false" stands for not changing result to upper case
      //String directoryName = new GetTextFromDialog("CREATE NEW DIRECTORY")
      //         .getTextFromDialog("Parent directory", "New directory name",
      //                  mainWindow.leftPathString + mainWindow.pcFileSep, "", false, currentX, currentY);
      String directoryName = new GetTextFromDialog("CREATE NEW DIRECTORY", "Parent directory", "New directory name", 
              mainWindow.leftPathString + mainWindow.pcFileSep, "", false, currentX, currentY).evaluateTextField();

      // User canceled creating the directory
      if (directoryName == null) {
         return;
      }
      if (directoryName.isEmpty()) {
         directoryName = "New directory";
      }
      try {
         // Create the new PC directory
         Files.createDirectory(Paths.get(mainWindow.leftPathString + mainWindow.pcFileSep + directoryName));

         row = "Comp: PC directory " + mainWindow.leftPathString + mainWindow.pcFileSep + directoryName
                  + " was created.";
         mainWindow.msgVector.add(row);
         mainWindow.showMessages(nodes);
      } catch (IOException exc) {
         exc.printStackTrace();

         row = "Error: PC directory " + mainWindow.leftPathString + mainWindow.pcFileSep + directoryName
                  + " was NOT created.  -  " + exc.toString();
         mainWindow.msgVector.add(row);
         mainWindow.showMessages(nodes);
      }
   }

   /**
    * Create PC file.
    */
   protected void createPcFile() {
      // Get new file name from the dialog
      // "false" stands for not changing result to upper case
      //String fileName = new GetTextFromDialog("CREATE NEW FILE")
      //         .getTextFromDialog("Parent directory", "New file name",
      //               mainWindow.leftPathString + mainWindow.pcFileSep, "", false, currentX, currentY);
      String fileName = new GetTextFromDialog("CREATE NEW FILE", "Parent directory", "New file name", 
              mainWindow.leftPathString + mainWindow.pcFileSep, "", false, currentX, currentY).evaluateTextField();

      // User canceled creating the directory
      if (fileName == null) {
         return;
      }
      if (fileName.isEmpty()) {
         fileName = "New file";
      }
      try {
         // Create the file
         Files.createFile(Paths.get(mainWindow.leftPathString + mainWindow.pcFileSep + fileName));

         row = "Comp: Empty PC file " + mainWindow.leftPathString + mainWindow.pcFileSep + fileName
                  + " was created in directory " + mainWindow.leftPathString + ".";
         mainWindow.msgVector.add(row);
         mainWindow.showMessages(nodes);
      } catch (IOException exc) {
         exc.printStackTrace();
         row = "Error: PC file " + mainWindow.leftPathString + mainWindow.pcFileSep + fileName
                  + " was NOT created.  -  " + exc.toString();
         mainWindow.msgVector.add(row);
         mainWindow.showMessages(nodes);
      }
   }

   /**
    * Delete PC object (file or directory).
    */
   protected void movePcObjectToTrash() {
      FileUtils fileUtils = FileUtils.getInstance();
      if (fileUtils.hasTrash()) {
         try {

            if (!Files.isDirectory(Paths.get(mainWindow.leftPathString))) {
               // Simple file:
               // ------------
               ///fileUtils.moveToTrash(new File[] { new File(mainWindow.leftPathString) }); // not functioning
               Desktop desktop = Desktop.getDesktop();
               desktop.moveToTrash(new File(mainWindow.leftPathString));

               row = "Comp: PC file  " + mainWindow.leftPathString + "  was moved to trash.";
               mainWindow.msgVector.add(row);
               mainWindow.showMessages(nodes);
            } else { 
               // Directory:
               // ----------
               // Delete all nested directories and files
               Path dirPath = Paths.get(mainWindow.leftPathString);

               walkPcDirectory_MoveToTrash(dirPath);

               row = "Comp: PC object " + mainWindow.leftPathString + " was moved to trash.";
               mainWindow.msgVector.add(row);

               // PARENT NODE of deleted node will be reloaded  and the deleted node will disappear from the tree.
               // Get parent node
               //mainWindow.leftNode = (DefaultMutableTreeNode) mainWindow.leftNode.getParent();
               mainWindow.showMessages(nodes);
               
               // Remove message scroll listener (cancel scrolling to the last message)
               mainWindow.scrollMessagePane.getVerticalScrollBar()
                        .removeAdjustmentListener(mainWindow.messageScrollPaneAdjustmentListenerMax);
            }
         } catch (IOException ioe) {
            ioe.printStackTrace();
            row = "Error: Moving PC object  " + mainWindow.leftPathString + "  to trash  -  " + ioe.toString();
            mainWindow.msgVector.add(row);
            mainWindow.showMessages(nodes);
         }
      } else {
         System.out.println("Error: Trash is not available.");
         row = "Error: Trash is not available.";
         mainWindow.msgVector.add(row);
         mainWindow.showMessages(nodes);
      }
   }

   /**
    * Moves (first) files and (then) directories to trash recursively.
    * 
    * @param folder
    * @throws IOException
    */
   public void walkPcDirectory_MoveToTrash(final Path folder) throws IOException {
      FileUtils fileUtils = FileUtils.getInstance();

      Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
         @Override
         public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

            if (fileUtils.hasTrash()) {
               ///fileUtils.moveToTrash(new File[] { new File(file.toString()) }); // not functioning
               Desktop desktop = Desktop.getDesktop();
               desktop.moveToTrash(new File(file.toString()));
               
               row = "Info: Moving PC file  " + file + "  to trash.";
               mainWindow.msgVector.add(row);
               //mainWindow.showMessages(nodes);
            }
            return FileVisitResult.CONTINUE;
         }

         @Override
         public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (exc != null) {
               throw exc;
            }

            if (fileUtils.hasTrash()) {
               ///fileUtils.moveToTrash(new File[] { new File(dir.toString()) }); // not functioning
               Desktop desktop = Desktop.getDesktop();
               desktop.moveToTrash(new File(dir.toString()));
               
               row = "Info: Moving PC directory  " + dir + "  to trash.";
               mainWindow.msgVector.add(row);
               //mainWindow.showMessages(nodes);
            }
            return FileVisitResult.CONTINUE;
         }
      });
   }
}
