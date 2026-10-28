package io.github.marcosws.terminus3270.utils;

public class Utils {
	
	/**
	 * Wraps the given text in a box so it can be displayed in a JTextArea.
	 * Handles multi-line text (e.g. a full 3270 screen) and pads each line
	 * to the width of the longest one.
	 *
	 * @param text the text retrieved from the 3270 terminal
	 * @return the framed text
	 */
	public static String frameText(String text) {
	    if (text == null) {
	        text = "";
	    }

	    String[] lines = text.replace("\t", "    ").split("\\R", -1);

	    int width = 0;
	    for (String line : lines) {
	        width = Math.max(width, line.length());
	    }

	    String horizontalBorder = "─".repeat(width + 2);
	    StringBuilder framed = new StringBuilder();

	    framed.append('┌').append(horizontalBorder).append("┐\n");
	    for (String line : lines) {
	        framed.append("│ ")
	              .append(line)
	              .append(" ".repeat(width - line.length()))
	              .append(" │\n");
	    }
	    framed.append('└').append(horizontalBorder).append('┘');

	    return framed.toString();
	}

}
