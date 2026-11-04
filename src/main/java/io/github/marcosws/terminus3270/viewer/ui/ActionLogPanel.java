package io.github.marcosws.terminus3270.viewer.ui;

import java.awt.Color;
import java.awt.Font;
import java.time.LocalTime;

import javax.swing.BorderFactory;
import javax.swing.JTextArea;

public class ActionLogPanel extends UIComponents{
	
    private final JTextArea actionLogArea;

    
    public JTextArea getActionLogArea() {
		return actionLogArea;
	}


	public ActionLogPanel() {
	   
        actionLogArea = new JTextArea(10, 84);
        actionLogArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        actionLogArea.setEditable(false);
        actionLogArea.setLineWrap(false);
        actionLogArea.setFocusable(true);
        actionLogArea.setBackground(new Color(245, 247, 250));
        actionLogArea.setBorder(BorderFactory.createCompoundBorder(
        		BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        actionLogArea.append("=== Terminal actions log ===\n");
    	
    	
    }
    
	
    public void recordAction(String action, String details) {
        String time = LocalTime.now().format(TIME_FMT);
        String log = "[" + time + "] " + action + (details != null && !details.isEmpty() ? " | " + details : "");
        actionLogArea.append(log + "\n");
        actionLogArea.setCaretPosition(actionLogArea.getDocument().getLength());
    }

}
