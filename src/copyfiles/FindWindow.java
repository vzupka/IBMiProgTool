package copyfiles;

import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.MemberDescription;
import com.ibm.as400.access.ObjectDoesNotExistException;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.plaf.LayerUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

/**
 * This class is a dialog for entering a name - new directory name, etc.
 *
 * @author Vladimír Župka 2016
 */
public class FindWindow extends JFrame {

    static final Color DIM_BLUE = Color.getHSBColor(0.60f, 0.2f, 0.5f); // blue little saturated dim (gray)
    static final Color DIM_RED = Color.getHSBColor(0.00f, 0.2f, 0.98f); // red little
    static final Color WARNING_COLOR = new Color(255, 200, 200);

    Path matchCaseIconPathDark = Paths.get(System.getProperty("user.dir"), "icons", "matchCase1.png");
    Path matchCaseIconPathDim = Paths.get(System.getProperty("user.dir"), "icons", "matchCase2.png");

    Path prevInactiveIconPath = Paths.get(System.getProperty("user.dir"), "icons", "prevInactive.png");
    Path nextInactiveIconPath = Paths.get(System.getProperty("user.dir"), "icons", "nextInactive.png");
    Path prevActiveIconPath = Paths.get(System.getProperty("user.dir"), "icons", "prevActive.png");
    Path nextActiveIconPath = Paths.get(System.getProperty("user.dir"), "icons", "nextActive.png");

    Path parPath = Paths.get(System.getProperty("user.dir"), "paramfiles", "Parameters.txt");
    String encoding = System.getProperty("file.encoding", "UTF-8");
    final String PROP_COMMENT = "Copy files between IBM i and PC, edit and compile.";
    Properties properties;

    Container cont;
    JLabel findLabel = new JLabel("Find what:");
    static JTextField findField = new JTextField();
    JLayer fieldLayer;

    PlaceholderLayerUI layerUI = new PlaceholderLayerUI();

    JLabel replaceLabel = new JLabel("Replace with:");
    JTextField replaceField = new JTextField();
    JButton cancelButton = new JButton("Cancel");
    JButton replaceButton = new JButton("Replace");
    JButton replaceFindButton = new JButton("Replace+Find");
    JButton replaceAllButton = new JButton("Replace All");

    boolean wasReplace = false;
    String direction = "forward";

    JPanel colPanel1;
    JPanel colPanel2;
    JPanel colPanel21;
    JPanel colPanel22;
    JPanel rowPanel2;
    JPanel rowPanel3;
    JPanel panel;

    int windowWidth = 385;
    int windowHeight = 130;

    EditFile editFile;

    // Icon Aa will be dimmed or dark when clicked
    ImageIcon matchCaseIconDark = new ImageIcon(matchCaseIconPathDark.toString());
    ImageIcon matchCaseIconDim = new ImageIcon(matchCaseIconPathDim.toString());
    JToggleButton matchCaseButton = new JToggleButton();

    ImageIcon prevInactiveIcon = new ImageIcon(prevInactiveIconPath.toString());
    ImageIcon nextInactiveIcon = new ImageIcon(nextInactiveIconPath.toString());
    ImageIcon prevActiveIcon = new ImageIcon(prevActiveIconPath.toString());
    ImageIcon nextActiveIcon = new ImageIcon(nextActiveIconPath.toString());

    JButton prevButton = new JButton(prevInactiveIcon);
    JButton nextButton = new JButton(nextActiveIcon);

    String matchCase;

    /**
     * Constructor
     *
     * @param editFile - object of the editor
     * @param pathString
     */
    public FindWindow(EditFile editFile, String pathString) {
        
        // For source member get real type
        if (pathString.contains(".FILE/")) {
            IFSFile ifsFile = new IFSFile(MainWindow.remoteServer, pathString);
            String memberType = getMemberType(ifsFile);
            pathString = pathString.substring(0, pathString.lastIndexOf(".")) + "." + memberType;
        }
        super.setTitle(pathString.substring(pathString.lastIndexOf("/") + 1));  // text in the window head

        this.editFile = editFile;

        prevButton.setToolTipText("Previous match. Also Ctrl+⬆ (Cmd+⬆ in macOS).");
        prevButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        prevButton.setContentAreaFilled(false);
        prevButton.setPreferredSize(new Dimension(25, 20));

        nextButton.setToolTipText("Next match. Also Ctrl+⬇ (Cmd+⬇ in macOS).");
        nextButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        nextButton.setContentAreaFilled(false);
        nextButton.setPreferredSize(new Dimension(25, 20));


        properties = new Properties();
        try {
            BufferedReader infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
            properties.load(infile);
            infile.close();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        
        matchCase = properties.getProperty("MATCH_CASE");
        if (matchCase.equals("CASE_INSENSITIVE")) {
        matchCaseButton.setIcon(matchCaseIconDim);
            matchCaseButton.setSelectedIcon(matchCaseIconDim);
            matchCaseButton.setToolTipText("Case insensitive. Toggle Match case.");
        } else {
            matchCaseButton.setIcon(matchCaseIconDark);
            matchCaseButton.setSelectedIcon(matchCaseIconDark);
            matchCaseButton.setToolTipText("Match case. Toggle Case insensitive.");
        }
        matchCaseButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        matchCaseButton.setContentAreaFilled(false);
        matchCaseButton.setPreferredSize(new Dimension(25, 20));

        // This window will be allways on top.
        setAlwaysOnTop(true);

        // Make window dimensions fixed.
        setResizable(false);

        // Register WindowListener for storing X and Y coordinates to properties
        addWindowListener(new FindingPatternsWindowAdapter());

        findField.setPreferredSize(new Dimension(200, 20));
        findField.setMaximumSize(new Dimension(200, 20));
        // Set document listener for the search field
        findField.getDocument().addDocumentListener(editFile.highlightListener);
        findField.setToolTipText("Enter text to find.");

        // Set a layer of counts that overlay the search field:
        // - the sequence number of just highlighted text found
        // - how many matches were found
        fieldLayer = new JLayer<>(findField, layerUI);

        replaceField.setPreferredSize(new Dimension(200, 20));
        replaceField.setMaximumSize(new Dimension(200, 20));
        replaceField.setToolTipText("Enter replacement text.");

        colPanel1 = new JPanel();
        colPanel1.setLayout(new BoxLayout(colPanel1, BoxLayout.Y_AXIS));
        colPanel1.add(findLabel);
        colPanel1.add(Box.createVerticalStrut(5));
        colPanel1.add(replaceLabel);

        colPanel2 = new JPanel();
        colPanel2.setLayout(new BoxLayout(colPanel2, BoxLayout.Y_AXIS));

        colPanel21 = new JPanel();
        colPanel22 = new JPanel();
        colPanel21.setLayout(new BoxLayout(colPanel21, BoxLayout.X_AXIS));

        colPanel21.add(fieldLayer);
        colPanel21.add(prevButton);
        colPanel21.add(nextButton);
        colPanel21.add(Box.createHorizontalStrut(5));
        //colPanel21.add(matchCaseButton);
        colPanel21.add(Box.createHorizontalGlue());
        colPanel22.setLayout(new BoxLayout(colPanel22, BoxLayout.X_AXIS));
        colPanel22.add(replaceField);
        colPanel22.add(Box.createHorizontalStrut(12));
        colPanel22.add(matchCaseButton);
        colPanel22.add(Box.createHorizontalGlue());

        colPanel2.add(Box.createVerticalGlue());
        colPanel2.add(colPanel21);
        colPanel2.add(Box.createVerticalStrut(5));
        colPanel2.add(colPanel22);
        colPanel2.add(Box.createVerticalGlue());

        rowPanel2 = new JPanel();
        rowPanel2.setLayout(new BoxLayout(rowPanel2, BoxLayout.X_AXIS));
        rowPanel2.add(colPanel1);
        rowPanel2.add(colPanel2);
        rowPanel2.add(Box.createHorizontalGlue());

        panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(Box.createVerticalGlue());
        panel.add(rowPanel2);

        rowPanel3 = new JPanel();
        rowPanel3.setLayout(new BoxLayout(rowPanel3, BoxLayout.X_AXIS));

        cancelButton.setPreferredSize(new Dimension(70, 20));
        replaceButton.setPreferredSize(new Dimension(70, 20));
        replaceFindButton.setPreferredSize(new Dimension(100, 20));
        replaceAllButton.setPreferredSize(new Dimension(100, 20));

        rowPanel3.add(cancelButton);
        rowPanel3.add(Box.createHorizontalStrut(25));
        rowPanel3.add(replaceButton);
        rowPanel3.add(replaceFindButton);
        rowPanel3.add(replaceAllButton);
        rowPanel3.add(Box.createHorizontalGlue());

        panel.add(Box.createVerticalStrut(5));
        panel.add(rowPanel3);
        panel.add(Box.createVerticalGlue());

        add(panel);


        // "Previous" button listener
        // --------------------------
        PreviousMatch previousMatch = new PreviousMatch();
        prevButton.addActionListener(previousMatch);

        // "Next" button listener
        // ----------------------
        NextMatch nextMatch = new NextMatch();
        nextButton.addActionListener(nextMatch);

        // "Match case" button listener
        // ----------------------------
        matchCaseButton.addActionListener(ae -> {
            if (matchCaseButton.getSelectedIcon().equals(matchCaseIconDark)) {
                matchCaseButton.setSelectedIcon(matchCaseIconDim);
                matchCaseButton.setToolTipText("Case insensitive. Toggle Match case.");
                matchCase = "CASE_INSENSITIVE";
            } else {
                matchCaseButton.setSelectedIcon(matchCaseIconDark);
                matchCaseButton.setToolTipText("Match case. Toggle Case insensitive.");
                matchCase = "CASE_SENSITIVE";
            }
            if (!editFile.lowerHalfActive) {
                editFile.changeHighlight();
            } else {
                editFile.changeHighlight2();
            }
            try {
                BufferedWriter outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                // Save programming language into properties
                properties.setProperty("MATCH_CASE", matchCase);
                properties.store(outfile, PROP_COMMENT);
                outfile.close();
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        });

        // "Cancel" button listener
        // ----------------------
        cancelButton.addActionListener(ae -> {
            // Clear input fields
            findField.setText("");
            replaceField.setText("");
            // Set off flags controlling replacing
            wasReplace = false; // Set off replace flag
            // Clear all highlights
            editFile.changeHighlight();
            if (editFile.textArea2 != null) {
                editFile.changeHighlight2();
            }
            setVisible(false);
        });

        // "Replace" button listener
        // ------------------------------
        replaceButton.addActionListener(ae -> {
            if (!editFile.lowerHalfActive) {
                if (!wasReplace) {
                    if (!editFile.highlightMap.isEmpty()) {
                        if (!editFile.highlightMap.isEmpty()) {
                            editFile.textArea.replaceRange(replaceField.getText(), editFile.startOffset, editFile.endOffset);
                            editFile.changeHighlight();
                            editFile.curPos = editFile.startOffset;
                        } else {
                            editFile.changeHighlight(); // No hits - erase numbers in findField
                        }
                        if (editFile.highlightMap.lowerKey(editFile.curPos) == null) {
                            editFile.curPos = editFile.highlightMap.lastKey();
                        } else {
                            editFile.curPos = editFile.highlightMap.lowerKey(editFile.curPos);
                        }
                    }
                }
            } else {
                if (!wasReplace) {
                    if (! editFile.highlightMap.isEmpty()) {
                        if (!editFile.highlightMap.isEmpty()) {
                            editFile.textArea2.replaceRange(replaceField.getText(), editFile.startOffset2, editFile.endOffset2);
                            editFile.changeHighlight2();
                            editFile.curPos2 = editFile.startOffset2;
                        } else {
                            editFile.changeHighlight2(); // No hits - erase numbers in findField
                        }
                        if (editFile.highlightMap.lowerKey(editFile.curPos2) == null) {
                            editFile.curPos2 = editFile.highlightMap.lastKey();
                        } else {
                            editFile.curPos2 = editFile.highlightMap.lowerKey(editFile.curPos2);
                        }
                    }
                }
            }
            wasReplace = true; // Set on replace flag - note that single replace occurred
        });

        // "Replace+Find" button listener
        // ------------------------------
        replaceFindButton.addActionListener(ae -> {
            if (direction.equals("forward")) {
                // Direction forward
                if (!editFile.lowerHalfActive) {
                    if (!wasReplace) {
                        if (editFile.highlightMap.ceilingKey(editFile.curPos) == null) {
                            editFile.curPos = 0;
                        } else {
                            editFile.curPos = editFile.highlightMap.ceilingKey(editFile.curPos + replaceField.getText().length());
                        }
                        if (!editFile.highlightMap.isEmpty()) {
                            editFile.textArea.replaceRange(replaceField.getText(), editFile.startOffset, editFile.endOffset);
                            editFile.changeHighlight();
                        } else {
                            editFile.changeHighlight(); // No hits - erase numbers in findField
                        }
                    }
                } else {
                    if (!wasReplace) {
                        if (editFile.highlightMap.ceilingKey(editFile.curPos2) == null) {
                            editFile.curPos2 = 0;
                        } else {
                            editFile.curPos2 = editFile.highlightMap.ceilingKey(editFile.curPos2 + replaceField.getText().length());
                        }
                        if (!editFile.highlightMap.isEmpty()) {
                            editFile.textArea2.replaceRange(replaceField.getText(), editFile.startOffset2, editFile.endOffset2);
                            editFile.changeHighlight2();
                        } else {
                            editFile.changeHighlight2(); // No hits - erase numbers in findField
                        }
                    }
                }
            } else {
                // Direction backward
                if (!editFile.lowerHalfActive) {
                    //System.out.println(editFile.highlightMap);
                    //System.out.println("curPos: " + editFile.curPos);
                    if (!wasReplace) {
                        if (editFile.highlightMap.lowerKey(editFile.curPos) == null) {
                            editFile.curPos = editFile.textArea.getText().length();
                        } else {
                            editFile.curPos = editFile.highlightMap.lowerKey(editFile.curPos + replaceField.getText().length());
                        }
                        if (!editFile.highlightMap.isEmpty()) {
                            editFile.textArea.replaceRange(replaceField.getText(), editFile.startOffset, editFile.endOffset);
                            editFile.changeHighlight();
                        } else {
                            editFile.changeHighlight(); // No hits - erase numbers in findField
                        }
                    }
                } else {
                    if (!wasReplace) {
                        if (editFile.highlightMap.lowerKey(editFile.curPos2) == null) {
                            editFile.curPos2 = editFile.textArea2.getText().length();
                        } else {
                            editFile.curPos2 = editFile.highlightMap.lowerKey(editFile.curPos2 + replaceField.getText().length());
                        }
                        if (!editFile.highlightMap.isEmpty()) {
                            editFile.textArea2.replaceRange(replaceField.getText(), editFile.startOffset2, editFile.endOffset2);
                            editFile.changeHighlight2();
                        } else {
                            editFile.changeHighlight2(); // No hits - erase numbers in findField
                        }
                    }
                }
            }
        });

        // "Replace all" button listener
        replaceAllButton.addActionListener(ae -> {
            String replacement = replaceField.getText();
            ArrayList<String> arrListPattern = new ArrayList<>();
            ArrayList<Integer> arrListStart = new ArrayList<>();
            ArrayList<Integer> arrListEnd = new ArrayList<>();
            Highlighter highlighter = editFile.textArea.getHighlighter();
            try {
                String text = editFile.textArea.getText();
                String pattern = findField.getText();
                int patternLen = pattern.length();
                int start = 1;
                int end = 0;
                while (start > 0 && end <= text.length()) {
                    start = text.toUpperCase().indexOf(findField.getText().toUpperCase(), end);
                    if (start < 0) {
                        break;
                    }
                    end = text.toUpperCase().indexOf(findField.getText().toUpperCase(), start) + patternLen;
                    // Fill array lists with: found text, start position, end position
                    arrListPattern.add(editFile.textArea.getText(start, end - start));
                    arrListStart.add(start);
                    arrListEnd.add(end);
                    highlighter.addHighlight(start, end, editFile.highlightPainter);
                    start = end;
                }
                int hits = arrListPattern.size();
                // Replace texts in intervals found by the replacement (= pattern)
                int idx = 0;
                for (idx = hits - 1; idx >= 0; --idx) {
                    editFile.textArea.replaceRange(replacement, arrListStart.get(idx), arrListEnd.get(idx));
                }
                editFile.textArea.setCaretPosition(arrListEnd.get(hits - 1));
            } catch (BadLocationException exc) {
                exc.printStackTrace();
            }
        });

        // Set input maps and actions for Ctrl + Arrow UP and Ctrl + Arrow DOWN on different buttons and text areas.
        Arrays.asList(prevButton, nextButton, findField, replaceField, replaceButton, replaceFindButton,
                editFile.textArea, editFile.textArea2).stream().map((object) -> {
                    return object;
                }).forEachOrdered((object) -> {
            // Enable processing of function key Ctrl + Arrow UP = Find next hit upwards
            object.getInputMap(JComponent.WHEN_FOCUSED)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "prev");
            object.getActionMap().put("prev", previousMatch);

            // Enable processing of function key Ctrl + Arrow DOWN = Find next hit downwards
            object.getInputMap(JComponent.WHEN_FOCUSED)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "next");
            object.getActionMap().put("next", nextMatch);
        });

        // Register document listener for the "findField" when hihglighting matched patterns.
        // ----------------------------------------------
        findField.getDocument().addDocumentListener(editFile.highlightListener);
        
        // Request focus on Find field.
        // ----------------------------
        findField.requestFocus();
    }

    /**
     * Shows the window and resolves a text to be searched; 
     * The actual text is obtained from EditFile object, not from the parameter.
     *
     * @param searchedText
     */
    public void finishCreatingWindow(String searchedText) {
        // Note: The parameter is not actually used. It is only tested if it is not empty.

        // Conclude showing window
        setSize(windowWidth, windowHeight);
        setLocation(editFile.windowX + 670, editFile.windowY);
        setVisible(true);
        // pack();

        // Set text selected from the text area (primary or secondary) into findField
        // and highlight matches.
        if (searchedText != null) {
            if (!editFile.lowerHalfActive) {
                if (editFile.selectionMode.equals(editFile.HORIZONTAL_SELECTION)) {
                    editFile.curPos = editFile.textArea.getSelectionStart();
                    findField.setText(editFile.textArea.getSelectedText());
                } else {
                    if (!editFile.selectionStarts.isEmpty()) {
                        editFile.curPos = editFile.selectionStarts.get(0);
                        editFile.textArea.select(editFile.selectionStarts.get(0), editFile.selectionEnds.get(0));
                        findField.setText(editFile.textArea.getSelectedText());
                    }
                    editFile.changeHighlight();
                }
                editFile.changeHighlight();
            } else {
                if (editFile.selectionMode.equals(editFile.HORIZONTAL_SELECTION)) {
                    editFile.curPos2 = editFile.textArea2.getSelectionStart();
                    findField.setText(editFile.textArea2.getSelectedText());
                } else {
                    if (!editFile.selectionStarts.isEmpty()) {
                        editFile.curPos2 = editFile.selectionStarts.get(0);
                        editFile.textArea2.select(editFile.selectionStarts.get(0), editFile.selectionEnds.get(0));
                        findField.setText(editFile.textArea2.getSelectedText());
                    }
                    editFile.changeHighlight2();
                }
                editFile.changeHighlight2();
            }
        }
    }


    /**
     * Set indicator N/M that overlays the search field and is right adjusted;
     * N - the sequence number of the text that is just highlighted,
     * M - how many matches were found.
     */
    class PlaceholderLayerUI extends LayerUI<JTextComponent> {

        public final JLabel hint = new JLabel() {

            @Override
            public void updateUI() {
                super.updateUI();
                // setForeground(UIManager.getColor("TextField.inactiveForeground"));
                // The following foreground color is almost the same as "TextField.inactiveForeground"
                setForeground(DIM_BLUE); // blue little saturated dim - gray
                setBackground(DIM_RED); // red little saturated - bright
            }
        };

        @Override
        public void paint(Graphics g, JComponent component) {
            super.paint(g, component);
            if (component instanceof JLayer) {
                JLayer jlayer = (JLayer) component;
                JTextComponent textComponent = (JTextComponent) jlayer.getView();
                if (!textComponent.getText().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setPaint(hint.getForeground());
                    Insets insets = textComponent.getInsets();
                    Dimension dimension = hint.getPreferredSize();
                    int x = textComponent.getWidth() - insets.right - dimension.width - 2;
                    int y = (textComponent.getHeight() - dimension.height) / 2;
                    g2.translate(x, y);
                    SwingUtilities.paintComponent(g2, hint, textComponent, 0, 0, dimension.width, dimension.height);
                    g2.dispose();
                }
            }
        }
    }

    /**
     * Create and compile a Pattern object for a text in the findField.
     *
     * @return
     */
    protected Pattern getPattern() {
        String pattern = findField.getText();

        if (Objects.isNull(pattern) || pattern.isEmpty()) {
            return null;
        }
        try {
            // Allow backslash, asterisk, plus, question mark etc.
            // The backslash must be tested first!!!         
            pattern = pattern.replace("\\", "\\\\");
            pattern = pattern.replace("*", "\\*");
            pattern = pattern.replace("+", "\\+");
            pattern = pattern.replace("?", "\\?");
            pattern = pattern.replace("$", "\\$");
            pattern = pattern.replace(".", "\\.");
            pattern = pattern.replace("[", "\\[");
            pattern = pattern.replace("^", "\\^");
            pattern = pattern.replace("_", "\\_");
            pattern = pattern.replace("|", "\\|");
            pattern = pattern.replace("{", "\\{");
            pattern = pattern.replace("(", "\\(");
            pattern = pattern.replace(")", "\\)");
            pattern = pattern.replace("`", "\\`");
            pattern = pattern.replace("%", "\\%");
            
            pattern = String.format("%1$s", pattern);
            int flags = matchCaseButton.getSelectedIcon().equals(matchCaseIconDark) ? 0
                    : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            return Pattern.compile(pattern, flags);
        } catch (PatternSyntaxException ex) {
            findField.setBackground(WARNING_COLOR);
            return null;
        }
    }

    /**
     * Inner class for "previous match" button and Ctrl + Arrow Up function key.
     */
    class PreviousMatch extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent ae) {
            direction = "backward";
            prevButton.setIcon(prevActiveIcon);
            nextButton.setIcon(nextInactiveIcon);
            if (!wasReplace) {
                if (!editFile.lowerHalfActive) {
                    if (editFile.highlightMap.lowerKey(editFile.curPos) == null) {
                        editFile.curPos = editFile.highlightMap.lastKey();
                    } else {
                        editFile.curPos = editFile.highlightMap.lowerKey(editFile.curPos);
                    }
                    editFile.changeHighlight();
                } else {
                    if (editFile.highlightMap.lowerKey(editFile.curPos2) == null) {
                        editFile.curPos2 = editFile.highlightMap.lastKey();
                    } else {
                        editFile.curPos2 = editFile.highlightMap.lowerKey(editFile.curPos2);
                    }
                    editFile.changeHighlight2();
                }
            }
            wasReplace = false; // Set off Replace flag
        }
    }

    /**
     * Inner class for "next match" button and Ctrl + Arrow Down function key.
     */
    class NextMatch extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent ae) {
            direction = "forward";
            prevButton.setIcon(prevInactiveIcon);
            nextButton.setIcon(nextActiveIcon);
            if (!editFile.lowerHalfActive) {
                if (editFile.highlightMap.higherKey(editFile.curPos) == null) {
                    editFile.curPos = editFile.highlightMap.firstKey();
                } else {
                    editFile.curPos = editFile.highlightMap.higherKey(editFile.curPos);
                }
                editFile.changeHighlight();
            } else {
                if (editFile.highlightMap.higherKey(editFile.curPos2) == null) {
                    editFile.curPos2 = editFile.highlightMap.firstKey();
                } else {
                    editFile.curPos2 = editFile.highlightMap.higherKey(editFile.curPos2);
                }
                editFile.changeHighlight2();
            }
            wasReplace = false; // Set off Replace flag
        }
    }

    /**
     * Window adapter clears text in findField and closes the window.
     */
    class FindingPatternsWindowAdapter extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent we) {
            JFrame jFrame = (JFrame) we.getSource();
            // Clear input fields
            findField.setText("");
            replaceField.setText("");
            // Set off flags controlling replacing
            wasReplace = false; // Set off replace flag
            // Clear all highlights
            editFile.changeHighlight();
            if (editFile.textArea2 != null) {
                editFile.changeHighlight2();
            }
            jFrame.setVisible(false);
        }
    }
    
    /**
     * Get source type of the source file member
     * 
     * @param member
     * @return 
     */
    protected String getMemberType(IFSFile member) {
        MemberDescription​ memberDescription = new MemberDescription​(MainWindow.remoteServer, member.toString());   
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
