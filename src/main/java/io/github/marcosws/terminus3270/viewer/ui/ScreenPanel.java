package io.github.marcosws.terminus3270.viewer.ui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JTextArea;

public class ScreenPanel {
	
    private static final String FONT_TYPE_LINUX = "Monospaced";
    private static final String FONT_TYPE_WINDOWS = "Consolas";
    
    private final JTextArea areaScreen;
    
	public JTextArea getAreaScreen() {
		return areaScreen;
	}

	public ScreenPanel() {
		
        String osName = System.getProperty("os.name").toLowerCase();
        String fontType = (osName.contains("win") ? FONT_TYPE_WINDOWS : FONT_TYPE_LINUX);
        
        areaScreen = new JTextArea(28, 84);
        areaScreen.setFont(new Font(fontType, Font.PLAIN, 12));
        areaScreen.setEditable(false);
        areaScreen.setFocusable(true);
        areaScreen.setBackground(Color.BLACK);
        areaScreen.setForeground(Color.CYAN);
		
	}

}
