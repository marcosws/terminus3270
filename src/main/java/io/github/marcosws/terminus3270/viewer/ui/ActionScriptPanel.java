package io.github.marcosws.terminus3270.viewer.ui;

import java.awt.Color;
import java.awt.Font;
import java.time.LocalTime;

import javax.swing.BorderFactory;
import javax.swing.JTextArea;

public class ActionScriptPanel extends UIComponents{
	
    private final JTextArea actionScriptArea;

    
    public JTextArea getActionScriptArea() {
		return actionScriptArea;
	}


	public ActionScriptPanel() {
	   
        actionScriptArea = new JTextArea(10, 84);
        actionScriptArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        actionScriptArea.setEditable(false);
        actionScriptArea.setLineWrap(false);
        actionScriptArea.setFocusable(true);
        actionScriptArea.setBackground(new Color(245, 247, 250));
        actionScriptArea.setBorder(BorderFactory.createCompoundBorder(
        		BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        actionScriptArea.append("// ======== Script =========\n");
    	
    	
    }
    
	
    public void recordScript(String script, String comment) {
        String time = LocalTime.now().format(TIME_FMT);
        String commentScript = "// [" + time + "] " + (comment != null && !comment.isEmpty() ? " | " + comment : "");
        actionScriptArea.append(commentScript + "\n");
        actionScriptArea.append(script + "\n");
        actionScriptArea.setCaretPosition(actionScriptArea.getDocument().getLength());
    }

}
