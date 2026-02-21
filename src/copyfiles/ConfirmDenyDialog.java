package copyfiles;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * This class is a dialog to confirm or deny
 *
 * @author Vladimír Župka 2026
 */
public class ConfirmDenyDialog extends JDialog {

    JPanel panel = new JPanel();
    JPanel dataPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    GroupLayout layout = new GroupLayout(panel);
    JLabel titleLabel = new JLabel();
    JLabel parentPathLabel = new JLabel();
    JLabel newNameLabel = new JLabel("");
    JLabel fileLabel = new JLabel("");
    JButton cancel = new JButton("Cancel");
    JButton enter = new JButton("Enter");
    String answer = "";
    
    int windowWidth = 450;
    int windowHeight = 130;

    
    /**
     * Constructor
     *
     * @param windowTitle
     * @param parentTitle
     * @param newNameTitle
     * @param parentPathString
     * @param fileName
     * @param currentX
     * @param currentY
     */
    public ConfirmDenyDialog(String windowTitle, String parentTitle, String newNameTitle, String parentPathString, String fileName,
            int currentX, int currentY) {
        super();
        this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        super.setTitle(windowTitle);

        //titleLabel.setText(parentTitle + ": ");
        parentPathLabel.setText(parentPathString);
        //parentPathLabel.setFont(parentPathLabel.getFont().deriveFont(Font.BOLD, 15)); 
        newNameLabel.setText(newNameTitle + ": ");
        fileLabel.setMaximumSize(new Dimension(500, 20));
        fileLabel.setText(fileName);
        fileLabel.setFont(fileLabel.getFont().deriveFont(Font.BOLD, 15)); 
        dataPanel.setLayout(new BoxLayout(dataPanel, BoxLayout.LINE_AXIS));

        dataPanel.add(newNameLabel);
        dataPanel.add(fileLabel);

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
            answer = enter.getText();
            returnAnswer();
            dispose();
        });

        cancel.addActionListener(en -> {
            answer = cancel.getText();
            returnAnswer();
            dispose();
        });

        getContentPane();
        add(panel);

        setSize(windowWidth, windowHeight);
        setLocation(currentX, currentY);
        setVisible(true);
        pack();

    }

    /**
     * Returnw true or false for Enter or Cancel
     * @return 
     */
    public boolean returnAnswer() {
        return answer.equals("Enter");
    }
}

