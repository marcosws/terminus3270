package io.github.marcosws.terminus3270.viewer.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import io.github.marcosws.terminus3270.viewer.TerminalViewer;

public class KeysDialog extends UIComponents {
	
    private JDialog keysDialog;
    private JFrame frame;
    private final Consumer<String> onKeyActionRef;
    private TerminalViewer terminalViewer;
    private ActionLogPanel actionLogPanel;
    private ActionScriptPanel actionScriptPanel;
    private StatusBar statusBar;
    
    public JDialog getKeysDialog() {
		return keysDialog;
	}
    
	public void setKeysDialog(JDialog keysDialog) {
		this.keysDialog = keysDialog;
	}

	public KeysDialog(
			TerminalViewer terminalViewer, 
			ActionLogPanel actionLogPanel,
			ActionScriptPanel actionScriptPanel,
			StatusBar statusBar,
			Consumer<String> onKeyActionRef) {
    	this.terminalViewer = terminalViewer;
    	this.frame = terminalViewer.getFrame();
    	this.actionLogPanel = actionLogPanel;
    	this.actionScriptPanel = actionScriptPanel;
    	this.statusBar = statusBar;
    	this.onKeyActionRef = onKeyActionRef;
    }
	
	public void showKeysWindow() {
        if (keysDialog != null && keysDialog.isDisplayable()) {
            if (keysDialog.isVisible()) {
                keysDialog.toFront();
                return;
            }
            keysDialog.setVisible(true);
            keysDialog.toFront();
            return;
        }

        keysDialog = new JDialog(frame, "Terminal Keys", false);
        keysDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        List<Image> icons = frame.getIconImages();
        if (icons != null && !icons.isEmpty()) {
            keysDialog.setIconImages(icons);
        }

        JPanel keysPanel = createKeysPanel(onKeyActionRef);
        keysDialog.setContentPane(keysPanel);
        keysDialog.pack();
        keysDialog.setMinimumSize(new Dimension(180, 520));
        keysDialog.setSize(180, 640);

        // Posiciona ao lado direito do viewer, se couber na tela
        int x = frame.getX() + frame.getWidth() + 8;
        int y = frame.getY();
        keysDialog.setLocation(x, y);

        keysDialog.setVisible(true);
    }

    public JPanel createKeysPanel(Consumer<String> onKeyAction) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        panel.setPreferredSize(new Dimension(160, 0));

        panel.add(sectionLabel("Navigation"));
        panel.add(keyButton("Enter", onKeyAction));
        panel.add(keyButton("Tab", onKeyAction));
        panel.add(keyButton("Home", onKeyAction));
        panel.add(keyButton("Backspace", onKeyAction));
        panel.add(Box.createVerticalStrut(6));

        panel.add(sectionLabel("Edit"));
        panel.add(keyButton("Clear", onKeyAction));
        panel.add(keyButton("Reset", onKeyAction));
        panel.add(keyButton("Delete Field", onKeyAction));
        panel.add(keyButton("Erase EOF", onKeyAction));
        panel.add(keyButton("Erase Input", onKeyAction));
        panel.add(Box.createVerticalStrut(8));

        panel.add(sectionLabel("PF Keys"));
        JPanel pfGrid = new JPanel(new GridLayout(0, 3, 3, 3));
        for (int i = 1; i <= 24; i++) {
            pfGrid.add(smallKeyButton("PF" + i, onKeyAction));
        }
        panel.add(pfGrid);
        panel.add(Box.createVerticalStrut(8));

        panel.add(sectionLabel("PA Keys"));
        JPanel paRow = new JPanel(new GridLayout(1, 3, 3, 3));
        paRow.add(smallKeyButton("PA1", onKeyAction));
        paRow.add(smallKeyButton("PA2", onKeyAction));
        paRow.add(smallKeyButton("PA3", onKeyAction));
        panel.add(paRow);

        panel.add(Box.createVerticalGlue());
        return panel;
    }
    
    private JButton keyButton(String name, Consumer<String> onKeyAction) {
        JButton btn = new JButton(name);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        btn.setFocusable(false);
        btn.addActionListener(e -> fireKey(name, onKeyAction));
        return btn;
    }

    private JButton smallKeyButton(String name, Consumer<String> onKeyAction) {
        JButton btn = new JButton(name);
        btn.setMargin(new Insets(2, 2, 2, 2));
        btn.setFont(new Font("SansSerif", Font.PLAIN, 10));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        btn.setFocusable(false);
        btn.addActionListener(e -> fireKey(name, onKeyAction));
        return btn;
    }
    
    private void fireKey(String name, Consumer<String> onKeyAction) {
    	statusBar.setStatus("Key: " + name);
    	actionLogPanel.recordAction("Control Key", "key='" + name + "'");
        try {
            onKeyAction.accept(name);
            statusBar.setStatus(name + " sent");
            actionLogPanel.recordAction("Control Key", "key='" + name + "'; result='sent'");
            
            actionScriptPanel.recordScript(scriptFormat(name), "Key = ".concat(name));
        } catch (Exception ex) {
        	statusBar.setStatus("Error: " + ex.getMessage());
        	actionLogPanel.recordAction("Control Key", "key='" + name + "'; error='" + ex.getMessage() + "'");
        	terminalViewer.error(ex.getMessage());
        }
    }
    
    private String scriptFormat(String name) {
    	
    	if(name.contains("PF")) {
    		return "emulator.pressPF(PFKey.{1});".replace("{1}", name);
    	}
    	else if(name.contains("PA")) {
    		return "emulator.pressPA(PAKey.{1});".replace("{1}", name);
    	}
    	else if(name.equalsIgnoreCase("enter")) {
    		return "emulator.enter();";
    	}
    	else if(name.equalsIgnoreCase("tab")) {
    		return "emulator.tab();";
    	}
    	else if(name.equalsIgnoreCase("home")) {
    		return "emulator.home();";
    	}
    	else if(name.equalsIgnoreCase("backspace")) {
    		return "emulator.backspace();";
    	}
    	else if(name.equalsIgnoreCase("clear")){
    		return "emulator.clear();";
    	}
    	else if(name.equalsIgnoreCase("reset")){
    		return "emulator.reset();";
    	}
        else if(name.equalsIgnoreCase("delete field")){
        	return "emulator.deleteField();";
        }
        else if(name.equalsIgnoreCase("erase eof")){
        	return "emulator.eraseEOF();";
        }
        else if(name.equalsIgnoreCase("erase input")){
        	return "emulator.eraseInput();";
        }
		return "";
    	
    }

}
