package copyfiles;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.IFSFile;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Show or chhange CCSID of the IFS directory (all subobjects) or IFS file
 * @author vzupka 2025
 */
public class ChangeCCSID extends JDialog {
    
    AS400 remoteServer;
    int currentX;
    int currentY;

    JPanel panel = new JPanel();
    JPanel dataPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    GroupLayout layout = new GroupLayout(panel);
    JLabel titleLabel = new JLabel();
    JLabel parentPathLabel = new JLabel();
    JButton cancel = new JButton("Cancel");
    JButton enter = new JButton("Enter");

    int windowWidth = 200;
    int windowHeight = 100;

    String commandText;
    String[] ibmCcsids; 
    
    public ChangeCCSID() throws HeadlessException {
    }
    /**
     Constructor.
     
     @parm remoteServer
     @parm mainWindow
     @parm currentX
     @parm currentY
     */
    ChangeCCSID(AS400 remoteServer, MainWindow mainWindow, int currentX, int currentY) {
        super();
        super.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        super.setTitle("Change CCSID");

        this.remoteServer = remoteServer;
        this.currentX = currentX;
        this.currentY = currentY;
        ibmCcsids = mainWindow.ibmCcsids;
    }

    protected void changeCCSID(String ifsFilePathString) {
        ArrayList ccsids = new ArrayList<>();
        ccsids.addAll(Arrays.asList(ibmCcsids));
        String ibmCcsid;
        int ibmCcsidInt;
        JLabel ibmCcsidLabel = new JLabel("IBM i CCSID:");
        JComboBox ibmCcsidComboBox = new JComboBox(ccsids.toArray());
        ibmCcsidComboBox.setPreferredSize(new Dimension(100, 20));
        ibmCcsidComboBox.setMinimumSize(new Dimension(100, 20));
        ibmCcsidComboBox.setMaximumSize(new Dimension(100, 20));
        ibmCcsidComboBox.setEditable(true);

        IFSFile ifsFile = new IFSFile(remoteServer, ifsFilePathString);
        try {
            ibmCcsidInt = ifsFile.getCCSID();
            ibmCcsid = Integer.toString(ibmCcsidInt);
            ibmCcsidComboBox.setSelectedItem(ibmCcsid);
        } catch (IOException exc) {
            ibmCcsid = "500";
            ibmCcsidComboBox.setSelectedItem(ibmCcsid);
        }

        dataPanel.setLayout(new BoxLayout(dataPanel, BoxLayout.LINE_AXIS));

        dataPanel.add(ibmCcsidLabel);
        dataPanel.add(ibmCcsidComboBox);

        buttonPanel.add(cancel);
        buttonPanel.add(enter);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

        panel.setLayout(layout);

        layout.setAutoCreateGaps(false);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout
                                .createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(titleLabel)
                                .addComponent(parentPathLabel)
                                .addComponent(dataPanel)
                                .addGap(5)
                                .addComponent(buttonPanel)
                        )
        );
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(titleLabel)
                                .addComponent(parentPathLabel)
                                .addComponent(dataPanel)
                                .addGap(5)
                                .addComponent(buttonPanel)
                        )
        );

        // Enter button listener
        enter.addActionListener((var en) -> {
            String strCcsid = (String)ibmCcsidComboBox.getSelectedItem();
                    commandText = "CHGATR OBJ('" + ifsFilePathString + "') ATR(*CCSID) VALUE(";   
                    commandText += strCcsid; 
                    commandText +=  ") SUBTREE(*ALL) SYMLNK(*NO)";          
            // Create object for calling CL commands
            CommandCall cmdCall = new CommandCall(remoteServer);
            try {
                cmdCall.run(commandText);
            } catch (AS400SecurityException | ErrorCompletingRequestException | PropertyVetoException | IOException | InterruptedException exc) {
                exc.printStackTrace();
            }            
            dispose();
        });

        // Cancel button listener
        cancel.addActionListener(en -> {
            dispose();
        });

        Container cont = getContentPane();
        cont.add(panel);

        setSize(windowWidth, windowHeight);
        setLocation(currentX, currentY);
        setVisible(true);
        pack();
    }
    
}
