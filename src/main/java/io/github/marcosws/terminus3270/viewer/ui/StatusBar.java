package io.github.marcosws.terminus3270.viewer.ui;

import java.awt.Color;
import java.awt.Font;
import java.time.LocalTime;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class StatusBar extends UIComponents{

    private final JLabel statusBar;
    
    public JLabel getStatusBar() {
		return statusBar;
	}

	public StatusBar() {
        // ===== Status Bar =====
        statusBar = new JLabel(" Ready ");
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        statusBar.setOpaque(true);
        statusBar.setBackground(new Color(240, 240, 240));
        statusBar.setFont(new Font("SansSerif", Font.PLAIN, 12));
    }
    
    public void setStatus(String message) {
        String text = " [" + LocalTime.now().format(TIME_FMT) + "]  "
                + (message == null ? "" : message) + " ";
        SwingUtilities.invokeLater(() -> statusBar.setText(text));
    }
	
	
}
