package copyfiles;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * This class is a dialog for entering a name - new directory name, etc.
 *
 * @author Vladimír Župka 2016
 */
public class GetTextFromDialog extends JDialog {

    JPanel panel = new JPanel();
    JPanel dataPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    GroupLayout layout = new GroupLayout(panel);
    JLabel titleLabel = new JLabel();
    JLabel parentPathLabel = new JLabel();
    JLabel newNameLabel = new JLabel();
    JTextField textField = new JTextField();
    JButton cancel = new JButton("Cancel");
    JButton enter = new JButton("Enter");

    int windowWidth = 460;
    int windowHeight = 150;

    String returnedText;
    // "true" if the path leads to an IBM i library object to be converted to upper case
    boolean isIbmObject;

    /**
     * Constructor
     *
     * @param windowTitle
     */
    public GetTextFromDialog(String windowTitle) {
        super();
        super.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        super.setTitle(windowTitle);
    }

    /**
     * Function getTextFromDialog displayes a window with input field 
     * and returns the text entered by the user
     * @param parentTitle
     * @param newNameTitle
     * @param parentPathString
     * @param fileName
     * @param isIbmObject
     * @param currentX
     * @param currentY
     * @return 
     */
    public String getTextFromDialog(String parentTitle, String newNameTitle, String parentPathString, String fileName,
            boolean isIbmObject, int currentX, int currentY) {
        this.isIbmObject = isIbmObject;
        // System.out.println("fileName: "+fileName);

        titleLabel.setText(parentTitle + ": ");
        parentPathLabel.setText(parentPathString);
        newNameLabel.setText(newNameTitle + ": ");
        textField.setMaximumSize(new Dimension(300, 20));
        textField.setText(fileName);

        dataPanel.setLayout(new BoxLayout(dataPanel, BoxLayout.LINE_AXIS));

        dataPanel.add(newNameLabel);
        dataPanel.add(textField);

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

        enter.addActionListener(en -> {
            evaluateTextField();
            dispose();
        });

        textField.addActionListener(tf -> {            
            evaluateTextField();
            dispose();
        });

        cancel.addActionListener(en -> {
            dispose();
        });

        getContentPane();
        add(panel);

        setSize(windowWidth, windowHeight);
        setLocation(currentX, currentY);
        setVisible(true);
        //pack();

        return returnedText;
    }

    /**
     * Special treatment of AS400 object names (Source files, members).
     */
    private void evaluateTextField() {
        if (isIbmObject) {
            returnedText = textField.getText().toUpperCase();
        } else {
            returnedText = textField.getText();
        }
    }
}
