package copyfiles;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.MemberDescription;
import com.ibm.as400.access.ObjectDoesNotExistException;
import java.beans.PropertyVetoException;
import java.io.IOException;

/**

 @author Vladimír Župka, 2017
 */
public class RenameIfsObject {

    AS400 remoteServer;
    MainWindow mainWindow;
    String row;

    int currentX;
    int currentY;

    /**
     Constructor.
     
     @parm remoteServer
     @parm mainWindow
     @parm currentX
     @parm currentY
     */
    RenameIfsObject(AS400 remoteServer, MainWindow mainWindow, int currentX, int currentY) {
        this.remoteServer = remoteServer;
        this.mainWindow = mainWindow;
        this.currentX = currentX;
        this.currentY = currentY;
    }

    /**
     Rename IFS file.

     @param oldPathString
     */
    protected void renameIfsObject(String oldPathString) {
        //System.out.println("oldPathString: "+oldPathString);
        GetTextFromDialog getText = new GetTextFromDialog("Rename IBM i object");
        String newName;
        String newFileName;
        String renamedPathString;

        IFSFile oldIfsFile = new IFSFile(remoteServer, oldPathString);
        IFSFile newIfsFile;

        // Library objects containing suffix .FILE or .MBR 
        // are treated differently from non-library IFS objects
        
        if (oldPathString.startsWith("/QSYS.LIB")) {
            //
            // Library object
            // --------------
            String oldFilePrefix = oldPathString.substring(0, oldPathString.lastIndexOf("/") + 1);
            String oldFileName = oldPathString.substring(oldPathString.lastIndexOf("/") + 1, oldPathString.lastIndexOf("."));
            String originalSuffix = oldPathString.substring(oldPathString.lastIndexOf("."));
            String newSuffix = originalSuffix;
            
            // "false" stands for IBM object, "true" stands for PC object
            newName = getText.getTextFromDialog("Parent directory",
                    "New name:", oldFilePrefix, oldFileName, false, currentX, currentY);
            newName = newName.toUpperCase();
            if (newName == null) {
               return;
            }
            if (newName.isEmpty()) {
               newName = oldFileName;
            }
            if (newName.length() > 10) {
                newName = newName.substring(0, 10);
            }
            String ifsPathString = oldFilePrefix + newName + originalSuffix;
            if (".MBR".equals(originalSuffix) ) {
                ifsPathString = oldFilePrefix + newName + ".MBR";
            } 

            oldIfsFile = new IFSFile(remoteServer, oldPathString);
            if (".MBR".equals(originalSuffix)) {
                // Get the actual source type
                newSuffix = getMemberType(oldIfsFile);
            }
            newFileName = newName + "." + newSuffix;
            newFileName = newFileName.toUpperCase();
            newIfsFile = new IFSFile(remoteServer, ifsPathString);  
            // String with original suffix .MBR or .FILE only for IFSFile function renameTo()
            renamedPathString = oldFilePrefix + newFileName;
        }
        else {
            // 
            // IFS non-library object (there is no suffix)
            // ----------------------
            String oldFilePrefix = oldPathString.substring(0, oldPathString.lastIndexOf("/") + 1);
            String oldFileName = oldPathString.substring(oldPathString.lastIndexOf("/") + 1);
            // "false" stands for IBM object, "true" stands for PC object
            newName = getText.getTextFromDialog("Parent directory",
                "New name:", oldFilePrefix, oldFileName, false, currentX, currentY);
            newFileName = newName;
            renamedPathString = oldFilePrefix + newFileName;
            newIfsFile = new IFSFile(remoteServer, renamedPathString);
        }
        
        // Renaming old IFS object to new IFS object
        if (newIfsFile.getCanonicalPath().equals(oldIfsFile.getCanonicalPath())) {
            return;  // Do not rename to identical name
        }
        if (!mainWindow.rightNode.isRoot()) {
            try {
                // Rename the old to new IFS file
                boolean renamed = oldIfsFile.renameTo(newIfsFile);
                // If not renamed for any error, send message
                // System.out.println("newIfsFile.getCanonicalPath(): "+newIfsFile.getCanonicalPath());
                if (!renamed) {
                    row = "Error: Renaming IFS file  " + oldPathString + "  to  " + renamedPathString + "  failed.";
                    mainWindow.msgVector.add(row);
                    mainWindow.showMessages();
                    return;
                }
            } catch (PropertyVetoException | IOException exc) {
                exc.printStackTrace();
                row = "Error: Renaming IFS file  -  " + exc.toString();
                mainWindow.msgVector.add(row);
                mainWindow.showMessages();
            }
            // Change left node (source node selected by mouse click)
            mainWindow.rightNode.setUserObject(newFileName);
            mainWindow.leftTreeModel.nodeChanged(mainWindow.rightNode);
            // Send completion message
            row = "Comp: IFS file  " + oldPathString + "  was renamed to  " + renamedPathString + ".";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
            mainWindow.repaint();
        } else {
            // Send error message
            row = "Error: IFS file  " + oldPathString + "  was not renamed becuase it is in the TOP NODE.";
            mainWindow.msgVector.add(row);
            mainWindow.showMessages();
            mainWindow.repaint();
            
        }
    }
    /**
     * Get source type of the source file member
     * 
     * @param member
     * @return 
     */
    protected String getMemberType(IFSFile member) {
        MemberDescription​ memberDescription = new MemberDescription​(remoteServer, member.toString());   
        String srcType = "";
        try {
            srcType = (String) memberDescription.getValue(MemberDescription​.SOURCE_TYPE);            
        } catch (AS400SecurityException | ErrorCompletingRequestException | 
                ObjectDoesNotExistException | IOException | InterruptedException exc) {
            exc.printStackTrace();
        }
        return srcType;
    }
    
}
