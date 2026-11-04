package io.github.marcosws.terminus3270.viewer.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import io.github.marcosws.terminus3270.viewer.TerminalViewer;

/**
 * TODO implements records scripts in buttons for features
 */



public class CommandsDialog extends UIComponents implements ActionListener {
	
    private JDialog commandsDialog;
    
    private final JButton copySelectedToFieldButton;
    private final JButton copySelectedToTextButton;
    private final JButton copyButton;
    private final JButton sendTextOnlyButton;
    private final JButton sendTextByFieldButton;
    private final JButton sendTextByPositionButton;
    private final JButton getPositionButton;
    private final JButton getPositionAfterFieldButton;
    private final JButton getTextButton;
    private final JButton getTextByPositionButton;
    private final JButton clearFieldsButton;
    
    private final JTextField fieldLabel;
    private final JTextField fieldValue;
    private final JComboBox<String> directionCombo;
    private final JTextField sizeField;
    private final JTextField rowField;
    private final JTextField colField;
    
    private TerminalViewer terminalViewer;
    private ActionLogPanel actionLogPanel;
    private ActionScriptPanel actionScriptPanel;
    private StatusBar statusBar;
    private ScreenPanel screenPanel;
    private TriConsumer<String, String, String> onSendText;
    private Consumer<String> onSendTextOnly;
    private Function<String, String> onGetCursor;
    private Function<String, String> onGetPositionAfterField;
    private TriFunction<String, Integer, String, String> onGetText;
    private TriFunction<Integer, Integer, Integer, String> onGetTextByCursor;
    private TriConsumer<Integer, Integer, String> onSendTextByPosition;
    
    private JFrame frame;
    
    public JDialog getCommandsDialog() {
		return commandsDialog;
	}
    
	public void setCommandsDialog(JDialog commandsDialog) {
		this.commandsDialog = commandsDialog;
	}

	public CommandsDialog(TerminalViewer terminalViewer, 
			StatusBar statusBar, 
			ActionLogPanel actionLogPanel,
			ActionScriptPanel actionScriptPanel,
			ScreenPanel screenPanel,
			TriConsumer<String, String, String> onSendText,
            Consumer<String> onSendTextOnly,
            Function<String, String> onGetCursor,
            Function<String, String> onGetPositionAfterField,
            TriFunction<String, Integer, String, String> onGetText,
            TriFunction<Integer, Integer, Integer, String> onGetTextByCursor,
            TriConsumer<Integer, Integer, String> onSendTextByPosition) {
    	
    	this.terminalViewer = terminalViewer;
    	this.frame = terminalViewer.getFrame();
    	this.actionLogPanel = actionLogPanel;
    	this.actionScriptPanel = actionScriptPanel;
    	this.statusBar = statusBar;
    	this.screenPanel = screenPanel;
    	this.onSendText = onSendText;
    	this.onSendTextOnly = onSendTextOnly;
    	this.onGetCursor = onGetCursor;
    	this.onGetPositionAfterField = onGetPositionAfterField;
    	this.onGetText = onGetText;
    	this.onGetTextByCursor = onGetTextByCursor;
    	this.onSendTextByPosition = onSendTextByPosition;
    	
        copySelectedToFieldButton = createButton("Copy Selected To Field (Label)");
        copySelectedToFieldButton.setToolTipText("Copy text selected from screen to Field");
        copySelectedToFieldButton.addActionListener(this);
        
        copySelectedToTextButton = createButton("Copy Selected To Field (Value)");
        copySelectedToTextButton.setToolTipText("Copy text selected from screen to Text");
        copySelectedToTextButton.addActionListener(this);
        
        copyButton = createButton("Copy Text Screen (Clipboard)");
        copyButton.setToolTipText("Copy text from screen to the system clipboard");
        copyButton.addActionListener(this);
        
        sendTextOnlyButton = createButton("Send Text");
        sendTextOnlyButton.setToolTipText("Send Text Only in Default Cursor Position");
        sendTextOnlyButton.addActionListener(this);
        
        sendTextByFieldButton = createButton("Send Text By Field (Label)");
        sendTextByFieldButton.setToolTipText("Send text to the terminal; specify the Field: and the Text: - Ex: Field: User:  Text: ibmuser");
        sendTextByFieldButton.addActionListener(this);
        
        getPositionButton = createButton("Get Position");
        getPositionButton.setToolTipText("Retrieves the position—starting row and column—of the text. Enter the text in the Field: input; the result will show the Row: and Col:.");
        getPositionButton.addActionListener(this);
        
        getTextButton = createButton("Get Text");
        getTextButton.setToolTipText("Retrieves text from a field; please specify the Field: and Size:");
        getTextButton.addActionListener(this);
        
        getTextByPositionButton = createButton("Get Text From Position");
        getTextByPositionButton.setToolTipText("Retrieves text based on Row:, Col:, and Size:; use after Get Position.");
        getTextByPositionButton.setEnabled(false);
        getTextByPositionButton.addActionListener(this);
        
        getPositionAfterFieldButton = createButton("Get Position After Field");
        getPositionAfterFieldButton.setToolTipText("Posição após o label (getPositionField). Preenche Row: e Col:.");
        getPositionAfterFieldButton.addActionListener(this);
        
        sendTextByPositionButton = createButton("Send Text By Position");
        sendTextByPositionButton.setToolTipText("Envia Text em Row:/Col: via moveAndSendString. Use após Get Position After Field.");
        sendTextByPositionButton.setEnabled(false);
        sendTextByPositionButton.addActionListener(this);
        
        clearFieldsButton = createButton("Clear Field`s");
        clearFieldsButton.setToolTipText("Clear Fields: Field, Text, Row, Col and Size reset value 20");
        clearFieldsButton.addActionListener(this);
        
        
        fieldLabel = new JTextField(20);
        fieldLabel.setToolTipText("Label do campo (ex: User:)");

        fieldValue = new JTextField(20);
        fieldValue.setToolTipText("Texto a enviar (Send Text) ou resultado dos Gets");

        directionCombo = new JComboBox<>(new String[]{
                "Right Field",
                "Left Field",
                "Above Field",
                "Below Field"
        });
        directionCombo.setPreferredSize(new Dimension(120, 20));

        sizeField = new JTextField(4);
        sizeField.setText("20");
        sizeField.setToolTipText("Tamanho para Get Text / Get Text By Cursor");

        rowField = new JTextField(4);
        rowField.setToolTipText("Linha (preenchida por Get Cursor)");

        colField = new JTextField(4);
        colField.setToolTipText("Coluna (preenchida por Get Cursor)");
        
        
        DocumentListener coordListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateGetTextByCursorEnabled(); }
            public void removeUpdate(DocumentEvent e) { updateGetTextByCursorEnabled(); }
            public void changedUpdate(DocumentEvent e) { updateGetTextByCursorEnabled(); }
        };
        rowField.getDocument().addDocumentListener(coordListener);
        colField.getDocument().addDocumentListener(coordListener);
    
    }
    
   
    public void showCommandsWindow() {
        if (commandsDialog != null && commandsDialog.isDisplayable()) {
            if (commandsDialog.isVisible()) {
                commandsDialog.toFront();
                return;
            }
            commandsDialog.setVisible(true);
            commandsDialog.toFront();
            return;
        }

        commandsDialog = new JDialog(frame, "Terminal Commands", false);
        commandsDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        List<Image> icons = frame.getIconImages();
        if (icons != null && !icons.isEmpty()) {
            commandsDialog.setIconImages(icons);
        }

        JPanel commandsPanel = createCommandsPanel();
        commandsDialog.setContentPane(commandsPanel);
        commandsDialog.pack();
        commandsDialog.setMinimumSize(new Dimension(PANEL_WIDTH + 40, 520));
        commandsDialog.setSize(PANEL_WIDTH + 40, 700);

        // Posiciona à esquerda do viewer
        int x = Math.max(0, frame.getX() - (PANEL_WIDTH + 48));
        int y = frame.getY();
        commandsDialog.setLocation(x, y);

        commandsDialog.setVisible(true);
    }
    
    private static final int PANEL_WIDTH = 220;

    private JPanel createCommandsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        panel.setPreferredSize(new Dimension(PANEL_WIDTH, 0));

        JComponent title = sectionLabel("Terminal Commands");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);

        // Largura total
        addCommandField(panel, "Field (Label):", fieldLabel);
        addCommandField(panel, "Field (Value):", fieldValue);
        addCommandField(panel, "Direction:", directionCombo);

        // Largura fixa
        addCommandField(panel, "Size:", sizeField, 50);
        addCommandField(panel, "Row:", rowField, 50);
        addCommandField(panel, "Col:", colField, 50);

        panel.add(Box.createVerticalStrut(6));

        for (JButton button : List.of(
                copyButton,
                copySelectedToFieldButton,
                copySelectedToTextButton,
                sendTextOnlyButton,
                sendTextByFieldButton,
                sendTextByPositionButton,       // NOVO
                getTextButton,
                getPositionButton,
                getPositionAfterFieldButton,    // NOVO
                getTextByPositionButton,
                clearFieldsButton)) {
            addCommandButton(panel, button);
        }

        panel.add(Box.createVerticalGlue());
        return panel;
    }
    
    /**
     * Adiciona um label + campo empilhados, ambos alinhados à esquerda.
     *
     * @param fieldWidth largura desejada do campo; use -1 para ocupar toda a largura disponível.
     */
    private void addCommandField(JPanel panel, String labelText, JComponent field, int fieldWidth) {
        JLabel label = new JLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        int height = field.getPreferredSize().height;
        if (fieldWidth > 0) {
            Dimension size = new Dimension(fieldWidth, height);
            field.setPreferredSize(size);
            field.setMinimumSize(size);
            field.setMaximumSize(size);
        } else {
            field.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        }

        panel.add(field);
        panel.add(Box.createVerticalStrut(3));
    }

    private void addCommandField(JPanel panel, String labelText, JComponent field) {
        addCommandField(panel, labelText, field, -1);
    }

    private void addCommandButton(JPanel panel, JButton button) {
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, BTN_SIZE.height));
        panel.add(button);
        panel.add(Box.createVerticalStrut(4));
    }
    
    private void updatePositionDependentButtons() {
        boolean ok = !rowField.getText().trim().isEmpty()
                  && !colField.getText().trim().isEmpty();
        getTextByPositionButton.setEnabled(ok);
        sendTextByPositionButton.setEnabled(ok);
    }

    private void updateGetTextByCursorEnabled() {
        updatePositionDependentButtons();
    }


	@Override
	public void actionPerformed(ActionEvent event) {
		
		if(event.getSource().equals(copyButton)) {
            String screenText = screenPanel.getAreaScreen().getText();
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(screenText), null);
            actionLogPanel.recordAction("Copy Text", "screenCopied=true; chars=" + screenText.length());
            statusBar.setStatus("Screen copied to clipboard");
            
            actionScriptPanel.recordScript("ScreenContent screenContent = emulator.getScreen();\n" +
            		"System.out.println(screenContent.getContent());","Return Screen Text, other option: emulator.asciiScreen();");
		}
		else if(event.getSource().equals(copySelectedToFieldButton)) {
        	String selectedText = screenPanel.getAreaScreen().getSelectedText();
            if (selectedText != null) {
            	fieldLabel.setText(selectedText);
            	actionLogPanel.recordAction("Copy Text", "text='" + selectedText + "' from screen to Field;'");
            	statusBar.setStatus("Text copied '" + selectedText + "' from screen to Field");
            }
            else {
            	terminalViewer.warn("Select text in Screen");
            }
        
		}
		else if(event.getSource().equals(copySelectedToTextButton)) {
        	String selectedText = screenPanel.getAreaScreen().getSelectedText();
            if (selectedText != null) {
                fieldValue.setText(selectedText);
                actionLogPanel.recordAction("Copy Text", "text='" + selectedText + "' from screen to Text;'");
                statusBar.setStatus("Text copied '" + selectedText + "' from screen to Text");
            }
            else {
            	terminalViewer.warn("Select text in Screen");
            }
		}
		else if(event.getSource().equals(sendTextOnlyButton)) {
        	String text = fieldValue.getText();
            if (text.isEmpty()) {
            	terminalViewer.warn("Informe o Text (text).");
                return;
            }
            try {
            	statusBar.setStatus("Sending Text '" + text + "'");
                actionLogPanel.recordAction("Send Text", "text='" + text + "';'");
                onSendTextOnly.accept(text);
                statusBar.setStatus("Text sent to '" + text + "'");
                
                actionScriptPanel.recordScript("emulator.sendString(\"{1}\");".replace("{1}", text), "Sending Text '" + text + "'");
            } catch (Exception ex) {
            	statusBar.setStatus("Error: " + ex.getMessage());
                terminalViewer.error(ex.getMessage());
            }
		}
		else if(event.getSource().equals(sendTextByFieldButton)) {
            String field = fieldLabel.getText().trim();
            String text = fieldValue.getText();
            String direction = (String) directionCombo.getSelectedItem();
            if (field.isEmpty()) {
            	terminalViewer.warn("Informe o Field (label).");
                return;
            }
            try {
            	statusBar.setStatus("Sending to '" + field + "' (" + direction + ")...");
                actionLogPanel.recordAction("Send Text", "field='" + field + "'; text='" + text + "'; direction='" + direction + "'");
                onSendText.accept(field, text, direction);
                statusBar.setStatus("Text sent to '" + field + "'");
                
                
                String script = "";
                String comment = "";
                if(direction.equals("Right Field")) {
                	script = "emulator.sendTextByLabel(\"{1}\",\"{2}\");"
                    		.replace("{1}", field)
                    		.replace("{2}", text);
                	comment = "Sending Text '" + text + "'";
                }
                else if(direction.equals("Left Field")) {
                	script = "emulator.sendTextByLabel(\"{1}\",\"{2}\", 0, -1);"
                    		.replace("{1}", field)
                    		.replace("{2}", text);
                	comment = "Sending Text '" + text + "'";
                }
                else if(direction.equals("Above Field")) {
                	script = "emulator.sendTextByLabel(\"{1}\",\"{2}\", -1, 0);"
                    		.replace("{1}", field)
                    		.replace("{2}", text);
                	comment = "Sending Text '" + text + "'";
                }
                else if(direction.equals("Below Field")) {
                	script = "emulator.sendTextByLabel(\"{1}\",\"{2}\", 1, 0);"
                    		.replace("{1}", field)
                    		.replace("{2}", text);
                	comment = "Sending Text '" + text + "'";
                }
                actionScriptPanel.recordScript(script, comment);
                
            } catch (Exception ex) {
            	statusBar.setStatus("Error: " + ex.getMessage());
                terminalViewer.error(ex.getMessage());
            }
		}
		else if(event.getSource().equals(getPositionButton)) {
			String field = fieldValue.getText().trim();
            if (field.isEmpty()) {
            	terminalViewer.warn("Informe o Field (texto a localizar), ex: FAKE3270 v2.1 - SIMULADOR MAINFRAME");
                return;
            }
            try {
            	statusBar.setStatus("Getting start position of '" + field + "'...");
                actionLogPanel.recordAction("Get Position", "field='" + field + "'");

                // Retorna a posição inicial (row,col) do texto no Field
                String coords = onGetCursor.apply(field); // "row,col"
                String[] parts = coords.split("\\s*,\\s*");
                if (parts.length < 2) {
                    throw new IllegalArgumentException("Resposta de posição inválida: '" + coords + "'");
                }

                String rowValue = parts[0].trim();
                String colValue = parts[1].trim();
                rowField.setText(rowValue);
                colField.setText(colValue);
                updateGetTextByCursorEnabled();

                // Size = comprimento do texto localizado; Text = o próprio texto encontrado
                int fieldSize = field.length();
                int row = Integer.parseInt(rowValue);
                int col = Integer.parseInt(colValue);
                String value = onGetTextByCursor.apply(row, col, fieldSize);
                String normalized = value == null ? field : value;
                fieldValue.setText(normalized);
                sizeField.setText(String.valueOf(fieldSize));

                statusBar.setStatus("Position → Row: " + rowValue + "  Col: " + colValue + "  Size: " + fieldSize);
                actionLogPanel.recordAction("Get Position", "field='" + field + "'; startPosition='row=" + rowValue + ", col=" + colValue + "'; text='" + normalized + "'; size=" + fieldSize);
                actionScriptPanel.recordScript("Optional<CursorPosition> posOpt = emulator.getPositionStartField(getScreen(), \"{1}\");\n".replace("{1}", field)
                		+ "if (posOpt.isEmpty()) {\n"
                		+ "    throw new S3270EmulatorException(\"Field '\" + fieldLabel + \"' not found in screen.\");\n"
                		+ "}\n"
                		+ "CursorPosition pos = posOpt.get();","Get Position Start Field");
            } catch (Exception ex) {
            	statusBar.setStatus("Error: " + ex.getMessage());
                terminalViewer.error(ex.getMessage());
            }
		}
		else if(event.getSource().equals(getTextButton)) {
            String field = fieldLabel.getText().trim();
            Integer size = parsePositiveInt(sizeField.getText().trim(), "Size");
            String direction = (String) directionCombo.getSelectedItem();
            if (field.isEmpty()) {
            	terminalViewer.warn("Informe o Field (label).");
                return;
            }
            if (size == null) return;
            try {
            	statusBar.setStatus("Reading field '" + field + "' (" + direction + ") size=" + size + "...");
                actionLogPanel.recordAction("Get Text", "field='" + field + "'; size=" + size + "; direction='" + direction + "'");
                String value = onGetText.apply(field, size, direction);
                fieldValue.setText(value);
                statusBar.setStatus("Get Text → \"" + value + "\"");
                actionLogPanel.recordAction("Get Text", "field='" + field + "'; size=" + size + "; direction='" + direction + "'; result='" + value + "'");
                
                String script = "";
                String comment = "";
                if(direction.equals("Right Field")) {
                	script = "emulator.getTextFromField(\"{1}\",{2});"
                    		.replace("{1}", field)
                    		.replace("{2}", size.toString());
                	comment = "Get Text From Field";
                }
                else if(direction.equals("Left Field")) {
                	script = "emulator.getTextBeforeField(\"{1}\", {2});"
                    		.replace("{1}", field)
                    		.replace("{2}", size.toString());
                	comment = "Get Text Before Field";
                }
                else if(direction.equals("Above Field")) {
                	script = "emulator.getTextAboveField(\"{1}\", {2}, 1);"
                    		.replace("{1}", field)
                    		.replace("{2}", size.toString());
                	comment = "Get Text Above Field";
                }
                else if(direction.equals("Below Field")) {
                	script = "emulator.getTextBelowField(\"{1}\", {2}, 1);"
                    		.replace("{1}", field)
                    		.replace("{2}", size.toString());
                	comment = "Get Text Below Field";
                }
                actionScriptPanel.recordScript(script,comment);
                
            } catch (Exception ex) {
            	statusBar.setStatus("Error: " + ex.getMessage());
                terminalViewer.error(ex.getMessage());
            }
		}
		else if(event.getSource().equals(getTextByPositionButton)) {
            Integer row = parsePositiveInt(rowField.getText().trim(), "Row");
            Integer col = parsePositiveInt(colField.getText().trim(), "Col");
            Integer size = parsePositiveInt(sizeField.getText().trim(), "Size");
            if (row == null || col == null || size == null) return;
            try {
            	statusBar.setStatus("Reading at (" + row + "," + col + ") size=" + size + "...");
                actionLogPanel.recordAction("Get Text By Position", "row=" + row + "; col=" + col + "; size=" + size);
                String value = onGetTextByCursor.apply(row, col, size);
                fieldValue.setText(value);
                statusBar.setStatus("Get Text By Position → \"" + value + "\"");
                actionLogPanel.recordAction("Get Text By Position", "row=" + row + "; col=" + col + "; size=" + size + "; result='" + value + "'");
                actionScriptPanel.recordScript("String text = emulator.getText(new CursorPosition(" + row + "," + col + "), "+ size + ");",
                		"Get Text By Position row=" + row + "; col=" + col + "; size=" + size + "; result='" + value + "'");
            } catch (Exception ex) {
            	statusBar.setStatus("Error: " + ex.getMessage());
                terminalViewer.error(ex.getMessage());
            }
		}
		else if(event.getSource().equals(getPositionAfterFieldButton)) {
            String field = fieldLabel.getText().trim();
            if (field.isEmpty()) {
            	terminalViewer.warn("Informe o Field (label), ex: Username:");
                return;
            }
            try {
            	statusBar.setStatus("Getting position after field '" + field + "'...");
                actionLogPanel.recordAction("Get Position After Field", "field='" + field + "'");

                String coords = onGetPositionAfterField.apply(field); // "row,col"
                String[] parts = coords.split("\\s*,\\s*");
                if (parts.length < 2) {
                    throw new IllegalArgumentException("Resposta de posição inválida: '" + coords + "'");
                }

                String rowValue = parts[0].trim();
                String colValue = parts[1].trim();
                rowField.setText(rowValue);
                colField.setText(colValue);
                updatePositionDependentButtons();

                statusBar.setStatus("Position After Field → Row: " + rowValue + "  Col: " + colValue);
                actionLogPanel.recordAction("Get Position After Field",
                        "field='" + field + "'; position='row=" + rowValue + ", col=" + colValue + "'");
                actionScriptPanel.recordScript("Optional<CursorPosition> posOpt = emulator.getPositionField(getScreen(), \"{1}\");\n".replace("{1}", field)
                		+ "if (posOpt.isEmpty()) {\n"
                		+ "    throw new S3270EmulatorException(\"Field '\" + fieldLabel + \"' not found in screen.\");\n"
                		+ "}\n"
                		+ "CursorPosition pos = posOpt.get();","");
            } catch (Exception ex) {
                statusBar.setStatus("Error: " + ex.getMessage());
                terminalViewer.error(ex.getMessage());
            }
		}
		else if(event.getSource().equals(sendTextByPositionButton)) {
            Integer row = parsePositiveInt(rowField.getText().trim(), "Row");
            Integer col = parsePositiveInt(colField.getText().trim(), "Col");
            String textToSend = fieldValue.getText();
            if (row == null || col == null) return;
            try {
            	statusBar.setStatus("Sending text at (" + row + "," + col + ")...");
                actionLogPanel.recordAction("Send Text By Position",
                        "row=" + row + "; col=" + col + "; text='" + textToSend + "'");
                onSendTextByPosition.accept(row, col, textToSend);
                statusBar.setStatus("Text sent at Row: " + row + "  Col: " + col);
                actionLogPanel.recordAction("Send Text By Position",
                        "row=" + row + "; col=" + col + "; text='" + textToSend + "'; result='sent'");
                actionScriptPanel.recordScript("emulator.moveAndSendString(" + row + "," + col + "," + textToSend + ");",
                		"Send Text By Position row=" + row + "; col=" + col + "; text='" + textToSend + "'");
            } catch (Exception ex) {
            	statusBar.setStatus("Error: " + ex.getMessage());
                terminalViewer.error(ex.getMessage());
            }
		}
		else if(event.getSource().equals(clearFieldsButton)) {
			fieldLabel.setText("");
            fieldValue.setText("");
            sizeField.setText("20");
            rowField.setText("");
            colField.setText("");
		}
	 
	}
	
    private Integer parsePositiveInt(String value, String name) {
        try {
            int n = Integer.parseInt(value);
            if (n <= 0) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) {
        	terminalViewer.warn(name + " deve ser um número positivo.");
            return null;
        }
    }
	
}
