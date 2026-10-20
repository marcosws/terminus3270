package com.github.marcosws.terminus3270;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.marcosws.terminus3270.components.CursorPosition;
import com.github.marcosws.terminus3270.components.ScreenContent;
import com.github.marcosws.terminus3270.enums.EmulatorMode;
import com.github.marcosws.terminus3270.enums.PAKey;
import com.github.marcosws.terminus3270.enums.PFKey;
import com.github.marcosws.terminus3270.enums.WaitType;
import com.github.marcosws.terminus3270.exceptions.S3270EmulatorException;
import com.github.marcosws.terminus3270.interfaces.IControlListener;
import com.github.marcosws.terminus3270.viewer.ManagerViewerContent;

/**
 * 3270Emulator is a class that provides high-level methods to interact with a 3270 terminal session. It uses an instance of S3270Session to send commands and receive responses from the host. The class includes methods to get the current screen in ASCII format, find the position of fields based on labels, send text to specific fields, and perform various actions like moving the cursor, pressing keys, and waiting for events. It abstracts the low-level details of communicating with the 3270 terminal and provides a more user-friendly interface for automation tasks.
 * @author Marcos Willian de Souza
 * @version 1.0
 * @since 2026-04
 */
public class S3270Emulator extends ManagerViewerContent implements IControlListener{
	
	private static final Logger logger = LoggerFactory.getLogger(S3270Emulator.class);
	
	private S3270Session session;

	public S3270Emulator(S3270Session session) {
		super(EmulatorMode.HEADLESS_MODE);
		logger.info("Initializing S3270Emulator with provided session");
		this.session = session;
		this.session.addCloseListener(this);
	}
	
	public S3270Emulator(S3270Session session, EmulatorMode emulatorMode) {
		super(emulatorMode);
		logger.info("Initializing S3270Emulator with provided session");
		this.session = session;
		this.session.addCloseListener(this);
		this.showViewer();
	}
	
	public S3270Session getSession() {
		logger.info("Retrieving the current S3270Session");
		return session;
	}
	
	public void pauseScript() {
		logger.info("Pausing script execution");
		pauseScreenViewer();
	}
	
	
	/**
	 * Gets the current screen from the host in ASCII format, removing control lines.
	 * Sends the "Ascii()" command and reads the response until it finds "ok". Returns the screen as a string, removing lines that start with "data:" and the last 2 control lines.
	 * The method tries to get the screen up to 10 times, waiting 150ms between each attempt, to handle cases where the screen may not be immediately available or may contain only control lines.
	 * @return Current screen from the host in ASCII format, without control lines
	 */
	public String asciiScreen() {
		
		logger.info("Getting ASCII screen from the host with retry mechanism");
		int retries = 10; 
		String screen = "";
		String rawScreen = "";

		while (retries-- > 0) {
		    rawScreen = session.sendCommand("Ascii()"); 
		    String[] lines = rawScreen.split("\n");
		    StringBuilder sb = new StringBuilder();

		    int limit = Math.max(0, lines.length - 2);
		    for (int i = 0; i < limit; i++) {
		        String line = lines[i].trim();
		        if (!line.isBlank() && !line.equals("data:")) { 
		            sb.append(line).append("\n");
		        }
		    }

		    screen = sb.toString().trim();

		    if (!screen.isBlank()) { 
		        break;
		    }
		    sleep(150); 
		}
		this.updateScreenViewer();
	    return rawScreen;
		
	}
	
	/**
	 * Gets the current screen from the host in ASCII format, removing control lines.
	 * This method is similar to asciiScreen() but does not include the retry mechanism. It
	 * assumes that the screen will be available immediately and may contain valid data. It sends the "Ascii()" command and processes the response in the same way, removing lines that start with "data:" and the last 2 control lines.
	 * @return Current screen from the host in ASCII format, without control lines
	 */
	public ScreenContent getScreen() {
		
		logger.info("Getting ASCII screen from the host without retry mechanism");
		StringBuilder screen = new StringBuilder();
		String rawScreen = session.sendCommand("Ascii()"); // pega a tela atual

	   int limit = Math.max(0, rawScreen.split("\n").length - 2); // ignora últimas 2 linhas
	    for (int i = 0; i < limit; i++) {
	        String line = rawScreen.split("\n")[i];
	        screen.append(line.replace("data:", "")).append("\n");

	    }
	    
	    return new ScreenContent(screen.toString());
		
	}
	    
	/**
	 * Finds the position of a field on the screen based on a label. It splits the screen into lines, searches for the label in each line, and returns the position of the field (row and column) if found. The returned position is based on 1, meaning the first row and column are considered as 1. The column is calculated as the position of the label plus the length of the label plus 1 (for the space between the label and the field). It throws an exception if the label is not found on the screen. Example: if the screen contains "Password  ===>" on line 10, the returned position will be (10, 20) considering that "Password  ===>" has 19 characters and the field starts at column 20. Usage: Optional<CursorPosition> posOpt = findField(screen, "Password  ===>"); if (posOpt.isPresent()) { CursorPosition pos = posOpt.get(); // use pos.getRow() and pos.getCol() } else { // field not found }
	 * @param screen The current screen from the host in ASCII format
	 * @param field The text of the label that identifies the input field
	 * @return An Optional containing the position of the field as CursorPosition if found, or
	 * Optional.empty() if not found
	 */
	public Optional<CursorPosition> getPositionField(String screen, String field) {

		logger.info("Finding position of field '{}' in the screen", field);
	    String[] lines = screen.split("\n");

	    for (int i = 0; i < lines.length; i++) {
	        int col = lines[i].indexOf(field);

	        if (col >= 0) {
	            int row = i + 1;
	            int column = col + field.length() + 1;
	            return Optional.of(new CursorPosition(row, column));
	        }
	    }

	    return Optional.empty();
	}
	
	/**
	 * Overloaded method to find the position of a field on the screen using a ScreenContent object. It extracts the content from the ScreenContent object and calls the getPositionField method that takes a string as input. This method provides a convenient way to find field positions when working with ScreenContent objects.
	 * @param screen A ScreenContent object representing the current screen from the host
	 * @param field The text of the label that identifies the input field
	 * @return An Optional containing the position of the field as CursorPosition if found, or
	 * Optional.empty() if not found
	 */
	public Optional<CursorPosition> getPositionField(ScreenContent screen, String field) {
		return getPositionField(screen.getContent(), field);
	}

	/**
	 * Returns the start position of the field value, preserving the original
	 * behavior used by the main automation methods.
	 * <p>
	 * This method is intentionally separate from {@link #getPositionField(String, String)}
	 * so the legacy logic used by send/get operations remains unchanged.
	 */
	public Optional<CursorPosition> getPositionStartField(String screen, String field) {
		logger.info("Finding start position of field '{}' in the screen", field);
		String[] lines = screen.split("\n");

		for (int i = 0; i < lines.length; i++) {
			int col = lines[i].indexOf(field);
			if (col >= 0) {
				int row = i + 1;
				int column = col + field.length() + 1;
				return Optional.of(new CursorPosition(row, column));
			}
		}

		return Optional.empty();
	}

	public Optional<CursorPosition> getPositionStartField(ScreenContent screen, String field) {
		return getPositionStartField(screen.getContent(), field);
	}

	/**
	 * Sends text to a field identified by a label. It first gets the current screen, finds the position of the field using the label, moves the cursor to that position, and sends the text.
	 * @param field The text of the label that identifies the input field
	 * @param text The text to be sent to the field
	 * @return The response from s3270 for the commands sent or an error message if
	 * the label is not found
	 * @throws IOException if an I/O error occurs while sending commands or reading responses
	 */    
	public String sendTextByField(String field, String text) {

		logger.info("Sending text '{}' to field '{}' identified by label", text, field);
	    String screen = asciiScreen().replace("data:", "");
	    Optional<CursorPosition> posOpt = getPositionField(screen, field);
	    if (posOpt.isEmpty()) {
	        throw new S3270EmulatorException(
	            "Field '" + field + "' not found in screen."
	        );
	    }
	    CursorPosition pos = posOpt.get();
	    int row = pos.getRow() - 1;
	    int col = pos.getCol() - 1;
	    return moveAndSendString(row, col, text);
	}
	
	/**
	 * Sends text to a field identified by a label with specified row and column offsets. It first gets the current screen, finds the position of the field using the label, calculates the target position by applying the offsets to the original position, moves the cursor to that target position, and sends the text.
	 * @param field The text of the label that identifies the input field
	 * @param text The text to be sent to the field
	 * @param offsetRow The number of rows to offset from the original field position (positive
	 * for down, negative for up)
	 * @param offsetCol The number of columns to offset from the original field position (positive
	 * for right, negative for left)
	 * @return The response from s3270 for the commands sent or an error message if
	 * the label is not found
	 * @throws IOException if an I/O error occurs while sending commands or reading responses
	 */
	public String sendTextByField(String field, String text, int offsetRow, int offsetCol) {

		
		logger.info("Sending text '{}' to field '{}' identified by label with offsets (row: {}, col: {})", text, field, offsetRow, offsetCol);
		String screen = asciiScreen().replaceAll("data:", "");
		
		Optional<CursorPosition> posOpt = getPositionField(screen, field);
		
	    if (posOpt.isEmpty()) {
	        throw new S3270EmulatorException(
	            "Field '" + field + "' not found in screen."
	        );
	    }
	    
	    CursorPosition pos = posOpt.get();
		
		int targetRow = (offsetRow >= 0 ? Math.abs(pos.getRow() - 1) + Math.abs(offsetRow) : Math.abs(pos.getRow() - 1) - Math.abs(offsetRow));
		int targetCol = (offsetCol >= 0 ? Math.abs(pos.getCol() - 1) + Math.abs(offsetCol) : Math.abs(pos.getCol() - 1) - (field.length() + Math.abs(offsetCol)));

		return moveAndSendString(targetRow, targetCol, text);
		
	}
	
	/**
	 * MoveAndSendString(row, col, text)
	 * Moves the cursor to a specific position (row and column) and sends the specified text
	 * @param row The row to move the cursor to (based on 1)
	 * @param col The column to move the cursor to (based on 1)
	 * @param text The text to be sent at the specified position
	 * @return The response from s3270 for the commands sent
	 * @throws IOException if an I/O error occurs while sending commands or reading responses
	 */
	public String moveAndSendString(int row, int col, String text) {
		
		logger.info("Moving cursor to position (row: {}, col: {}) and sending text '{}'", row, col, text);
		StringBuilder returnCommand = new StringBuilder();
		returnCommand.append("\n");
		returnCommand.append(moveCursor(row, col)).append("\n");
		returnCommand.append(sendString(text));
		return returnCommand.toString();
		
	}
	
	
	/**
	 * Waits for a specific event defined by WaitType (Unlock, InputField, Output, T3270Mode, NVTMode, Disconnect) with a default timeout of 30 seconds. It sends the "Wait(WaitType)" command to the s3270 session and returns the response. This method is useful for synchronizing actions based on specific events occurring in the terminal session.
	 * @param waitType The type of wait to be performed
	 * @return The response from s3270 for the wait command
	 * @throws IOException if an I/O error occurs while sending commands or reading responses
	 */
	public String waitFor(WaitType waitType) {
		logger.info("Waiting for event of type '{}'", waitType.getValue());
		String command = session.sendCommand("Wait(" + waitType.getValue() + ")", 30000); // espera até 30 segundos por padrão
		this.updateScreenViewer();
		return command;
	}
	
	/**
	 * Wait(WaitType, seconds)
	 * Waits for a specific event defined by WaitType (Unlock, InputField, Output
	 * T3270Mode, NVTMode, Disconnect) for a maximum number of seconds. It sends the "Wait(WaitType, seconds)" command to the s3270 session and returns the response. This method allows you to specify a custom timeout for waiting for specific events in the terminal session.
	 * Example: waitFor(WaitType.Unlock, 10) to wait for the keyboard
	 * to be unlocked for up to 10 seconds
	 * @param waitType The type of wait to be performed
	 * @param seconds The maximum time in seconds to wait for the event
	 * @return The response from s3270 for the wait command
	 * @throws IOException if an I/O error occurs while sending commands or reading responses
	 */
	public String waitFor(WaitType waitType, int seconds) {
		logger.info("Waiting for event of type '{}' with timeout of {} seconds", waitType.getValue(), seconds);
		String command = session.sendCommand("Wait(" + seconds + "," + waitType.getValue() + ")");
		this.updateScreenViewer();
		return command;
	}
	
	/**
	 * Wait(WaitType, seconds)
	 * Waits for a specific event defined by WaitType (Unlock, InputField, Output
	 * T3270Mode, NVTMode, Disconnect) for a maximum number of seconds. It sends the "Wait(WaitType, seconds)" command to the s3270 session and returns the response. This method allows you to specify a custom timeout for waiting for specific events in the terminal session.
	 * Example: waitFor(WaitType.Unlock, 10) to wait for the keyboard
	 * to be unlocked for up to 10 seconds
	 * @param waitType The type of wait to be performed
	 * @param seconds The maximum time in seconds to wait for the event
	 * @return The response from s3270 for the wait command
	 * @throws IOException if an I/O error occurs while sending commands or reading responses
	 */
	public String waitFor(WaitType waitType, long seconds) {
		logger.info("Waiting for event of type '{}' with timeout of {} seconds", waitType.getValue(), seconds);
		String command = session.sendCommand("Wait(" + seconds + "," + waitType.getValue() + ")");
		this.updateScreenViewer();
		return command;
		
	}
	
	/**
	 * Wait(seconds)
	 * Waits for a specific number of seconds. It sends the "Wait(seconds, Seconds
	 * )" command to the s3270 session and returns the response. This method is useful for introducing fixed delays in the execution of terminal automation scripts.
	 * Example: waitSeconds(5) to wait for 5 seconds
	 * @param seconds The number of seconds to wait
	 * @return The response from s3270 for the wait command
	 * @throws IOException if an I/O error occurs while sending commands or reading responses
	 */
	public String waitSeconds(long seconds) {
		logger.info("Waiting for {} seconds", seconds);
		String command = session.sendCommand("Wait(" + seconds + ",Seconds)");
		this.updateScreenViewer();
		return command;
	}
	
	/**
	 * Wait(seconds)
	 * Waits for a specific number of seconds. It sends the "Wait(seconds, Seconds
	 * )" command to the s3270 session and returns the response. This method is useful for introducing fixed delays in the execution of terminal automation scripts.
	 * Example: waitSeconds(5) to wait for 5 seconds
	 * @param seconds The number of seconds to wait
	 * @return The response from s3270 for the wait command
	 * @throws IOException if an I/O error occurs while sending commands or reading responses
	 */
	public String waitSeconds(int seconds) {
		logger.info("Waiting for {} seconds", seconds);
		String command =  session.sendCommand("Wait(" + seconds + ",Seconds)");
		this.updateScreenViewer();
		return command;
	}
	
	/**
	 * WaitForText(text, timeoutMillis)
	 * Waits for a specific text to appear on the screen within a given timeout. It
	 * continuously checks the screen for the presence of the specified text until it is found or the timeout is reached. If the text is found, the method returns successfully; otherwise, it throws a S3270SessionException indicating that the timeout was reached without finding the text. This method is useful for synchronizing actions based on specific content appearing on the terminal screen.
	 * @param text The text to wait for on the screen
	 * @param timeoutMillis The maximum time in milliseconds to wait for the text to appear
	 * @throws S3270EmulatorException if the timeout is reached without finding the text on
	 */
	public void waitForText(String text, int timeoutMillis) {

		logger.info("Waiting for text '{}' to appear on the screen with timeout of {} milliseconds", text, timeoutMillis);
	    long start = System.currentTimeMillis();
	    while (System.currentTimeMillis() - start < timeoutMillis) {
	        if (asciiScreen().contains(text)) {
	            return;
	        }
	        sleep(200);
	    }
	    throw new S3270EmulatorException("Timeout waiting for text: " + text);
	}
		
	/**
	 * MoveCursor(row, col) moves the cursor to a specific position defined by row and column. The method sends the "MoveCursor(row, col)" command to the s3270 session and returns the response from s3270 for that command. The row and column parameters are based on 1, meaning the first row and column are considered as 1.
	 * Example: moveCursor(10, 20) to move the cursor to row 10 and column 20
	 * @param row The row to move the cursor to (based on 1)
	 * @param col The column to move the cursor to (based on 1)
	 * @return The response from s3270 for the MoveCursor command
	 * @throws IOException if an I/O error occurs while sending commands or reading responses
	 */
	public String moveCursor(int row, int col) {
		logger.info("Moving cursor to position (row: {}, col: {})", row, col);
		String command = session.sendCommand("MoveCursor(" + row + "," + col + ")");
		this.updateScreenViewer();
		return command;
	}
	
	/**
	 * MoveCursor(position) moves the cursor to a specific position defined by a CursorPosition object, which encapsulates the row and column. The method sends the "MoveCursor(row, col)" command to the s3270 session using the values from the CursorPosition object and returns the response from s3270 for that command. The row and column parameters are based on 1, meaning the first row and column are considered as 1.
	 * Example: moveCursor(new CursorPosition(10, 20)) to move the cursor
	 * to row 10 and column 20 using a CursorPosition object
	 * @param position A CursorPosition object that encapsulates the row and column to move the
	 * cursor to (based on 1)
	 * @return The response from s3270 for the MoveCursor command
	 * @throws IOException if an I/O error occurs while sending commands or reading responses
	 */
	public String moveCursor(CursorPosition position) {
		logger.info("Moving cursor to position (row: {}, col: {})", position.getRow(), position.getCol());
		String command = session.sendCommand("MoveCursor(" + position.getRow() + "," + position.getCol() + ")");
		this.updateScreenViewer();
		return command;
	}
		
	/**
	 * Tab() moves the cursor to the next editable field on the screen. It sends the "Tab()" command to the s3270 session and returns the response from s3270 for that command. This method is useful for navigating through input fields in a terminal session without needing to specify exact row and column positions.
	 * @return The response from s3270 for the Tab command
	 * @throws IOException if an I/O error occurs while sending commands or reading responses
	*/
	public String tab() {
		logger.info("Moving cursor to the next editable field using Tab()");
		String command =  session.sendCommand("Tab()");
		this.updateScreenViewer();
		return command;
	}
		
	/**
	 * Home() moves the cursor to the first position of the screen (row 1, column 1). It sends the "Home()" command to the s3270 session and returns the response from s3270 for that command. This method is useful for quickly returning to the top-left corner of the terminal screen.
	 * @return The response from s3270 for the Home command
	 * @throws IOException if an I/O error occurs while sending commands or reading responses
	*/
	public String home() {
		logger.info("Moving cursor to the first position of the screen using Home()");
		String command = session.sendCommand("Home()");
		this.updateScreenViewer();
		return command;
	}
		
	/**
	 * String(text) sends the specified text to the current cursor position on the screen. It sends the "String(text)" command to the s3270 session, where text is the string to be sent. The method returns the response from s3270 for that command. This method is useful for entering text into input fields or sending commands directly at the current cursor location.
	 * @param text The text to be sent at the current cursor position
	 * @return The response from s3270 for the String command
	 * @throws IOException if an I/O error occurs while sending commands or reading responses
	*/
	public String sendString(String text) {
		logger.info("Sending text '{}' at the current cursor position", text);
		String command = session.sendCommand("String(\"" + text + "\")");
		this.updateScreenViewer();
		return command;
	}
	
	/**
	 * DeleteField() deletes the content of the current field where the cursor is located. It sends the "DeleteField()" command to the s3270 session and returns the response from s3270 for that command. This method is useful for clearing input fields before entering new data.
	 * @return The response from s3270 for the DeleteField command
	 * @throws IOException if an I/O error occurs while sending commands or reading responses
	 */
	public String deleteField() {
		logger.info("Deleting the content of the current field using DeleteField()");
		String command = session.sendCommand("DeleteField");
		this.updateScreenViewer();
		return command;
	}
		
	/**
	 * EraseEOF() deletes all characters from the current cursor position to the end of the line. It sends the "EraseEOF()" command to the s3270 session and returns the response from s3270 for that command. This method is useful for clearing the rest of a line after a certain point, allowing you to enter new data without affecting the content before the cursor.
	 * @return The response from s3270 for the EraseEOF command
	 * @throws IOException if an I/O error occurs while sending commands or reading responses
	*/
	public String eraseEOF() {
		logger.info("Erasing from the current position to the end of the line using EraseEOF()");
		String command = session.sendCommand("EraseEOF");
		this.updateScreenViewer();
		return command;
	}
	
	/**
	 * EraseInput() deletes all characters from the current cursor position to the end of the field. It sends the "EraseInput()" command to the s3270 session and returns the response from s3270 for that command. This method is useful for clearing the rest of an input field after a certain point, allowing you to enter new data without affecting the content before the cursor.
	 * @return The response from s3270 for the EraseInput command
	 * @throws IOException if an I/O error occurs while sending commands or reading responses
	 */
	public String eraseInput() {
		logger.info("Erasing from the current position to the end of the field using EraseInput()");
		String command = session.sendCommand("EraseInput");
		this.updateScreenViewer();
		return command;
	}
	
	/**
	 * Reset() resets the screen to its initial state, clearing all fields and returning to the default layout. It sends the "Reset()" command to the s3270 session and returns the response from s3270 for that command. This method is useful for starting fresh on a screen or recovering from an unexpected state.
	 * @return The response from s3270 for the Reset command
	 * @throws IOException if an I/O error occurs while sending commands or reading responses 
	 */
	public String reset() {
		logger.info("Resetting the screen using Reset()");
		String command = session.sendCommand("Reset");
		this.updateScreenViewer();
		return command;
	}
		
	/**
	 *  Enter() simulates pressing the Enter key, which typically submits the current screen or moves to the next line. It sends the "Enter()" command to the s3270 session and returns the response from s3270 for that command. This method is essential for navigating through screens and submitting data in a terminal session.
	 *  @return The response from s3270 for the Enter command
	 *  @throws IOException if an I/O error occurs while sending commands or reading responses
	 */
	public String enter() {
		logger.info("Pressing the Enter key using Enter()");
		String command = session.sendCommand("Enter");
		this.updateScreenViewer();
		return command;
	}
		
	/**
	 * Presses the PF n key, where n is the number of the PF key (1 to 24). It sends the "PF(n)" command to the s3270 session, where n is the value of the PFKey enum. The method returns the response from s3270 for that command. This method is useful for simulating the pressing of function keys in a terminal session.	
	 * @param pfKey The PF key to be pressed, represented by the PFKey enum
	 * @return The response from s3270 for the PF command
	 * @throws IOException if an I/O error occurs while sending commands or reading responses
	*/
	public String pressPF(PFKey pfKey) {
		logger.info("Pressing the PF{} key using PF({})", pfKey.getValue(), pfKey.getValue());
		String command = session.sendCommand("PF(" + pfKey.getValue() + ")");
		this.updateScreenViewer();
		return command;
	}
		
	/**
	 * Presses the PA n key, where n is the number of the PA key (1 to 3). It sends the "PA(n)" command to the s3270 session, where n is the value of the PAKey enum. The method returns the response from s3270 for that command. This method is useful for simulating the pressing of attention keys in a terminal session.
	 * @param paKey The PA key to be pressed, represented by the PAKey enum
	 * @return The response from s3270 for the PA command
	 * @throws IOException if an I/O error occurs while sending commands or reading responses
	 */
	public String pressPA(PAKey paKey) {
		logger.info("Pressing the PA{} key using PA({})", paKey.getValue(), paKey.getValue());
		String command = session.sendCommand("PA(" + paKey.getValue() + ")");
		this.updateScreenViewer();
		return command;
	}
		
	/**
	 * Clear() clears the entire screen, removing all text and fields. It sends the "Clear()" command to the s3270 session and returns the response from s3270 for that command. This method is useful for resetting the screen to a blank state before entering new data or navigating to a different screen.
	 * @return The response from s3270 for the Clear command
	 * @throws IOException if an I/O error occurs while sending commands or reading responses 
	 */ 
	public String clear() {
		logger.info("Clearing the entire screen using Clear()");
		String command = session.sendCommand("Clear()");
		this.updateScreenViewer();
		return command;
	}
		
	/**
	* Presses the Backspace key, which moves the cursor one position to the left and deletes the character at that position. It is commonly used to correct mistakes while typing. The method sends the "Backspace()" command to the s3270 session and returns the response from s3270 for that command.
	* @return The response from s3270 for the Backspace command
	* @throws IOException if an I/O error occurs while sending commands or reading responses
	*/
	public String backspace() {
		logger.info("Pressing the Backspace key using Backspace()");
		String command = session.sendCommand("Backspace()");
		this.updateScreenViewer();
		return command;
	}
		
	/**
	 * Sleep(millis) introduces a delay in the execution of the program for a specified number of milliseconds. It uses Thread.sleep() to pause the current thread and handles InterruptedException by re-interrupting the thread and printing the stack trace. This method is useful for adding fixed delays between actions in terminal automation scripts, especially when waiting for certain conditions to be met on the screen.
	 * @param millis The number of milliseconds to sleep
	 * @throws InterruptedException if the sleep is interrupted while waiting
	 */
	public void sleep(int millis) {
		logger.info("Sleeping for {} milliseconds", millis);
		try {
			Thread.sleep(millis);
		} 
		catch (InterruptedException e) {
			logger.warn("Sleep interrupted", e);
			Thread.currentThread().interrupt();
		}
	}
	
	/**
	 * Gets text from the screen starting from a specific position (row and column) and for a specified number of characters. It retrieves the current screen in ASCII format, splits it into lines, and extracts the substring based on the given position and size. The method assumes that the position is based on 1 (the first row and column are considered as 1) and that the sizeText does not exceed the line length from the specified column.
	 * @param position The starting position (row and column) to get the text from, based on 1
	 * @param sizeText The number of characters to be obtained starting from the specified position
	 * @return The text obtained from the screen based on the specified position and size
	 * @throws S3270SessionException if the specified position is invalid or if the size
	 * exceeds the line length from the specified column
	 * Example: if the screen contains "Username: user123" on line 5, and you want to get the username starting from column 11 with a size of 7, you would call getText(new CursorPosition(5, 11), 7) and it would return "user123".
	 * 
	 */
	public String getText(CursorPosition position, int sizeText) {
		logger.info("Getting text from the screen starting from position (row: {}, col: {}) with size {}", position.getRow(), position.getCol(), sizeText);
		String screen = asciiScreen();
		String[] lines = screen.split("\\n");
		if (position == null || sizeText <= 0) {
			this.updateScreenViewer();
			return "";
		}
		if (position.getRow() < 1 || position.getRow() > lines.length) {
			this.updateScreenViewer();
			return "";
		}
		String line = lines[position.getRow() - 1];
		int start = Math.max(0, position.getCol() - 1);
		if (start >= line.length()) {
			this.updateScreenViewer();
			return "";
		}
		int end = Math.min(line.length(), start + sizeText);
		this.updateScreenViewer();
		return line.substring(start, end);
	}
	
	public String getText(int row, int col, int sizeTest) {
		CursorPosition cursorPosition = new CursorPosition(row, col);
		return getText(cursorPosition, sizeTest);
	}
	
	
	/**
	 * GetTextByField(field, sizeText)
	 * English: Gets text from the screen based on a field identified by a label. It first finds the position of the field using the label, and then retrieves the text starting from that position for a specified number of characters. The method assumes that the position is based on 1 (the first row and column are considered as 1) and that the sizeText does not exceed the line length from the specified column. It throws an exception if the label is not found on the screen.
	 * @param field The text of the label that identifies the input field
	 * @param sizeText The number of characters to be obtained starting from the field position
	 * @return The text obtained from the screen based on the field position and size specified
	 * @throws S3270SessionException if the label is not found on the screen
	 */
	public String getTextByField(String field, int sizeText) {
		logger.info("Getting text from the screen based on field '{}' with size {}", field, sizeText);
		CursorPosition position;
		Optional<CursorPosition> posOpt = getPositionField(asciiScreen(), field);
		if(posOpt.isPresent()) 
			position = posOpt.get();
		else
			throw new S3270EmulatorException("Field '" + field + "' not found in screen.");
		return getText(position, sizeText);
	}
	
	/**
	 * GetTextBeforeField(field, sizeText)
	 * English: Gets text from the screen that is located before a field identified by a label. It first finds the position of the field using the label, calculates the new column position by subtracting the size of the text and the length of the field from the original column position, and then retrieves the text starting from that new position for a specified number of characters. The method assumes that the position is based on 1 (the first row and column are considered as 1) and that the sizeText does not exceed the line length from the calculated column. It throws an exception if the label is not found on the screen.
	 * @param field The text of the label that identifies the input field
	 * @param sizeText The number of characters to be obtained starting from the calculated position before the field
	 * @return The text obtained from the screen based on the calculated position before the field and size specified
	 * @throws S3270EmulatorException if the label is not found on the screen
	 */
	public String getTextBeforeField(String field, int sizeText) {
		logger.info("Getting text from the screen before field '{}' with size {}", field, sizeText);
		CursorPosition position;
		Optional<CursorPosition> posOpt = getPositionField(asciiScreen(), field);
		if(posOpt.isPresent()) 
			position = posOpt.get();
		else
			throw new S3270EmulatorException("Field '" + field + "' not found in screen.");
		int calcColumn = Math.abs((position.getCol() - 1) - sizeText);
		int newColumn = Math.abs(calcColumn - field.length());
		return getText(new CursorPosition(position.getRow(), newColumn), sizeText);
	}
	
	
	/**
	 * GetTextBottonField(field, sizeText, offsetRow)
	 * English: Gets text from the screen that is located below a field identified by a label
	 * It first finds the position of the field using the label, calculates the new row position by adding the offsetRow to the original row position, and then retrieves the text starting from that new position for a specified number of characters. The method assumes that the position is based on 1 (the first row and column are considered as 1) and that the sizeText does not exceed the line length from the original column. It throws an exception if the label is not found on the screen.
	 * Example: if the screen contains "Username: user123" on line 5,
	 * and you want to get the text located 2 rows below starting from the same column with a size of 10, you would call getTextBottonField("Username:", 10, 2) and it would return the text located at that position.
	 * @param field
	 * @param sizeText
	 * @param offsetRow
	 * @return
	 */
	public String getTextBottonField(String field, int sizeText, int offsetRow) {
		logger.info("Getting text from the screen below field '{}' with size {} and row offset {}", field, sizeText, offsetRow);
		CursorPosition position;
		Optional<CursorPosition> posOpt = getPositionField(asciiScreen(), field);
		if(posOpt.isPresent()) 
			position = posOpt.get();
		else
			throw new S3270EmulatorException("Field '" + field + "' not found in screen.");
		int newRow = Math.abs(position.getRow() + offsetRow);
		int newCol = Math.abs((position.getCol() - 1) - field.length());
		return getText(new CursorPosition(newRow, newCol), sizeText);
	}
	
	/**
	 * GetTextTopField(field, sizeText, offsetRow)
	 * English: Gets text from the screen that is located above a field identified by a label. It first finds the position of the field using the label, calculates the new row position by subtracting the offsetRow from the original row position, and then retrieves the text starting from that new position for a specified number of characters. The method assumes that the position is based on 1 (the first row and column are considered as 1) and that the sizeText does not exceed the line length from the original column. It throws an exception if the label is not found on the screen.
	 * Example: if the screen contains "Username: user123" on line 5, and you want to get the text located 2 rows above starting from the same column with a size of 10, you would call getTextTopField("Username:", 10, 2) and it would return the text located at that position.
	 * @param field The text of the label that identifies the input field
	 * @param sizeText The number of characters to be obtained starting from the calculated position above the field
	 * @param offsetRow The number of rows to offset from the original field position (positive for down, negative for up)
	 * @return The text obtained from the screen based on the calculated position above the field and size specified
	 * @throws S3270EmulatorException if the label is not found on the screen
	 */
	public String getTextTopField(String field, int sizeText, int offsetRow) {
		logger.info("Getting text from the screen above field '{}' with size {} and row offset {}", field, sizeText, offsetRow);
		CursorPosition position;
		Optional<CursorPosition> posOpt = getPositionField(asciiScreen(), field);
		if(posOpt.isPresent()) 
			position = posOpt.get();
		else
			throw new S3270EmulatorException("Field '" + field + "' not found in screen.");
		int newRow = Math.abs(position.getRow() - offsetRow);
		int newCol = Math.abs((position.getCol() - 1) - field.length());
		return getText(new CursorPosition(newRow, newCol), sizeText);
	}

	@Override
	public ScreenContent getScreenContent() {
		return getScreen();
	}
	
    @Override
    public void onClose() {
        this.closeViewer();
    }
    
    @Override
    protected void handleSendTextFromViewer(String field, String text, String direction) {
        logger.info("Viewer SendText → field='{}', text='{}', direction='{}'", field, text, direction);

        switch (direction) {
            case "Right Field":
                sendTextByField(field, text);
                break;
            case "Left Field":
                sendTextByField(field, text, 0, -1);
                break;
            case "Top Field":
                sendTextByField(field, text, -1, 0);
                break;
            case "Botton Field":
                sendTextByField(field, text, 1, 0);
                break;
            default:
                sendTextByField(field, text);
        }
    }

    @Override
    protected String handleGetTextFromViewer(String field, int size, String direction) {
        logger.info("Viewer GetText → field='{}', size={}, direction='{}'", field, size, direction);

        switch (direction) {
            case "Right Field":
                return getTextByField(field, size);

            case "Left Field":
                return getTextBeforeField(field, size);

            case "Top Field":
                return getTextTopField(field, size, 1);   // 1 linha acima

            case "Botton Field":
                return getTextBottonField(field, size, 1); // 1 linha abaixo

            default:
                return getTextByField(field, size);
        }
    }
    
    @Override
    protected void handleKeyFromViewer(String keyName) {
        logger.info("Viewer key pressed: {}", keyName);
        setStatus("Sending " + keyName + "...");

        switch (keyName) {
            case "Enter"       : enter(); break;
            case "Tab"         : tab(); break;
            case "Home"        : home(); break;
            case "Backspace"   : backspace(); break;
            case "Clear"       : clear(); break;
            case "Reset"       : reset(); break;
            case "DeleteField" : deleteField(); break;
            case "EraseEOF"    : eraseEOF(); break;
            case "EraseInput"  : eraseInput(); break;

            // PF1 .. PF24
            case "PF1"  : pressPF(PFKey.PF1); break;
            case "PF2"  : pressPF(PFKey.PF2); break;
            case "PF3"  : pressPF(PFKey.PF3); break;
            case "PF4"  : pressPF(PFKey.PF4); break;
            case "PF5"  : pressPF(PFKey.PF5); break;
            case "PF6"  : pressPF(PFKey.PF6); break;
            case "PF7"  : pressPF(PFKey.PF7); break;
            case "PF8"  : pressPF(PFKey.PF8); break;
            case "PF9"  : pressPF(PFKey.PF9); break;
            case "PF10" : pressPF(PFKey.PF10); break;
            case "PF11" : pressPF(PFKey.PF11); break;
            case "PF12" : pressPF(PFKey.PF12); break;
            case "PF13" : pressPF(PFKey.PF13); break;
            case "PF14" : pressPF(PFKey.PF14); break;
            case "PF15" : pressPF(PFKey.PF15); break;
            case "PF16" : pressPF(PFKey.PF16); break;
            case "PF17" : pressPF(PFKey.PF17); break;
            case "PF18" : pressPF(PFKey.PF18); break;
            case "PF19" : pressPF(PFKey.PF19); break;
            case "PF20" : pressPF(PFKey.PF20); break;
            case "PF21" : pressPF(PFKey.PF21); break;
            case "PF22" : pressPF(PFKey.PF22); break;
            case "PF23" : pressPF(PFKey.PF23); break;
            case "PF24" : pressPF(PFKey.PF24); break;

            // PA1 .. PA3
            case "PA1" : pressPA(PAKey.PA1); break;
            case "PA2" : pressPA(PAKey.PA2); break;
            case "PA3" : pressPA(PAKey.PA3); break;

            default : {
                logger.warn("Unknown key from viewer: {}", keyName);
                setStatus("Unknown key: " + keyName);
            }
        }

        setStatus(keyName + " sent");
    }
    
    @Override
    protected String handleGetCursorFromViewer(String fieldLabel) {
        // Usa a posição inicial real do conteúdo do campo para manter a semântica de
        // "coluna do início do texto" sem alterar a lógica legacy do getPositionField.
        Optional<CursorPosition> posOpt = getPositionStartField(getScreen(), fieldLabel);
        if (posOpt.isEmpty()) {
            throw new S3270EmulatorException("Field '" + fieldLabel + "' not found in screen.");
        }
        CursorPosition pos = posOpt.get();
        // Formato esperado pelo viewer: "row,col"
        return pos.getRow() + "," + pos.getCol();
    }

    @Override
    protected String handleGetTextFromViewer(String fieldLabel, Integer size) {
        return getTextByField(fieldLabel, size);
    }

    @Override
    protected String handleGetTextByCursorFromViewer(Integer row, Integer col, Integer size) {
        return getText(new CursorPosition(row, col), size);
    }

    @Override
    protected void handleAbortSessionFromViewer() {
        logger.info("Abort Session requested from viewer");
        try {
            if (session != null) {
                try {
                    session.quit(); // Quit + close do processo
                } catch (Exception e) {
                    logger.warn("Error on quit, forcing close", e);
                    session.close();
                }
            }
        } finally {
            closeViewer();
        }
    }

}
