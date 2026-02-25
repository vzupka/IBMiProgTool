package copyfiles;

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

    int windowWidth = 450;
    int windowHeight = 150;

    String returnedText;
    // "true" if the path leads to an IBM i library object to be converted to upper case
    boolean isIbmLibraryObject;

    /**
     * Constructor
     *
     * @param windowTitle
     * @param parentTitle
     * @param newNameTitle
     * @param parentPathString
     * @param fileName
     * @param isIbmLibraryObject
     * @param currentX
     * @param currentY
     */
    public GetTextFromDialog(String windowTitle, String parentTitle, String newNameTitle, String parentPathString, String fileName,
            boolean isIbmLibraryObject, int currentX, int currentY) {
        super();
        this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        super.setTitle(windowTitle);

        this.isIbmLibraryObject = isIbmLibraryObject;

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
                                .addGap(10)
                                .addComponent(dataPanel)
                                .addGap(10)
                                .addComponent(buttonPanel)
                        )
        );
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(titleLabel)
                                .addComponent(parentPathLabel)
                                .addGap(10)
                                .addComponent(dataPanel)
                                .addGap(10)
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
            returnedText = null;
            dispose();
        });

        getContentPane();
        add(panel);

        setSize(windowWidth, windowHeight);
        setLocation(currentX, currentY);
        setVisible(true);
        //pack();

    }

    /**
     * Special treatment of AS400 object names (Source files, members).
     */
    public String evaluateTextField() {
        if (isIbmLibraryObject) {
            returnedText = textField.getText().toUpperCase();
        } else {
            returnedText = textField.getText();
        }
        return returnedText;    
    }
}
