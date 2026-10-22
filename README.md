# Terminus 3270

A Java library for automating and interacting with 3270 terminals, using high-level commands to simplify testing and integration with legacy systems.

## Overview

The **terminus3270** project provides an object-oriented API for controlling 3270 sessions, encapsulating common commands such as navigation, field entry, screen reading, waiting for events, and handling special keys.

The main class is `S3270Emulator`, which uses an instance of `S3270Session` to communicate with the terminal.

## Key Features

- Retrieve the screen in ASCII format
- Find fields by label and send text to them
- Move the cursor
- Press special keys (Enter, PF, PA, Tab, etc.)
- Wait for events or specific text to appear on the screen
- Extract text from specific positions or fields

## Usage Example

The following example demonstrates how to use the `terminus3270` library to interact with a 3270 terminal:

```java
import com.github.marcosws.terminus3270.S3270Emulator;
import com.github.marcosws.terminus3270.exceptions.S3270SessionException;

public class Main {

    public static void main(String[] args) {

        try {

            // Initialize the 3270 session
            S3270Session session = new S3270Session();
            S3270Emulator emulator = new S3270Emulator(session);

            emulator.getSession().connect("fake", "23");

            // Send text to a field identified by its label
            emulator.sendTextByField("Username:", "admin");

            // Press Enter
            emulator.enter();

            // Wait until text appears on the screen
            if (emulator.waitForText("Welcome", 5000)) {
                System.out.println("Login successful!");
            } else {
                System.out.println("Login failed.");
            }

            // Disconnect from the 3270 session
            emulator.disconnect();

        } catch (S3270SessionException e) {

            System.err.println("Error interacting with the 3270 terminal: " + e.getMessage());

        }
    }
}
```

## Terminal Viewer

Open the interactive screen viewer by creating the emulator in `VIEWER_MODE`:

```java
S3270Session session = new S3270Session();
S3270Emulator emulator = new S3270Emulator(session, EmulatorMode.VIEWER_MODE);
emulator.getSession().connect("host", "23");

// ... script

// Pauses the script until the user clicks "Continue" in the TerminalViewer.
emulator.pauseScript(); // It only works in EmulatorMode.VIEWER_MODE.

```


## Main Methods

### Session and Viewer

| Method | Description |
|---|---|
| `S3270Emulator(S3270Session session)` | Creates an emulator in headless mode using the supplied session. |
| `S3270Emulator(S3270Session session, EmulatorMode emulatorMode)` | Creates an emulator using the specified mode and opens the viewer. |
| `getSession()` | Returns the current `S3270Session` instance. |
| `pauseScript()` | Pauses script execution through the screen viewer, allowing the current terminal screen to be inspected. |
| `getScreenContent()` | Returns the current screen content as a `ScreenContent` object. |
| `onClose()` | Closes the viewer when the session is closed. |

### Screen Reading and Field Detection

| Method | Description |
|---|---|
| `asciiScreen()` | Requests the current screen in ASCII format, retrying up to 10 times when the response contains no usable screen content. |
| `getScreen()` | Requests the current screen and returns it as a `ScreenContent` object. |
| `getPositionField(String screen, String field)` | Finds a label in a screen string and returns the field position as an `Optional<CursorPosition>`. Rows and columns in the returned position are 1-based. |
| `getPositionField(ScreenContent screen, String field)` | Finds a label in a `ScreenContent` object and returns the field position as an `Optional<CursorPosition>`. |
| `getText(CursorPosition position, int sizeText)` | Extracts a specified number of characters from the screen, starting at the supplied position. |
| `getTextByField(String field, int sizeText)` | Finds a field by its label and extracts text starting at the calculated field position. |
| `getTextBeforeField(String field, int sizeText)` | Extracts text positioned before a field identified by its label. |
| `getTextBottonField(String field, int sizeText, int offsetRow)` | Extracts text below a field identified by its label, using the specified row offset. The method name is `getTextBottonField` in the current API. |
| `getTextTopField(String field, int sizeText, int offsetRow)` | Extracts text above a field identified by its label, using the specified row offset. |

### Text Entry and Cursor Navigation

| Method | Description |
|---|---|
| `sendTextByField(String field, String text)` | Finds a field by its label, moves the cursor to its calculated position, and sends the text. |
| `sendTextByField(String field, String text, int offsetRow, int offsetCol)` | Finds a field by its label, applies row and column offsets, then sends the text at the resulting position. |
| `moveAndSendString(int row, int col, String text)` | Moves the cursor to the specified position and sends text. |
| `moveCursor(int row, int col)` | Moves the cursor to the specified row and column. |
| `moveCursor(CursorPosition position)` | Moves the cursor to the row and column stored in a `CursorPosition` object. |
| `sendString(String text)` | Sends text at the current cursor position. |
| `tab()` | Moves the cursor to the next editable field. |
| `home()` | Moves the cursor to the home position at the top-left of the screen. |

### Keyboard and Field Operations

| Method | Description |
|---|---|
| `enter()` | Presses Enter. |
| `pressPF(PFKey pfKey)` | Presses the PF key represented by the supplied `PFKey` enum value (PF1–PF24). |
| `pressPA(PAKey paKey)` | Presses the PA key represented by the supplied `PAKey` enum value (PA1–PA3). |
| `deleteField()` | Deletes the content of the field containing the cursor. |
| `eraseEOF()` | Erases characters from the cursor position to the end of the line. |
| `eraseInput()` | Erases input from the cursor position to the end of the field. |
| `reset()` | Sends the terminal Reset command. |
| `clear()` | Sends the Clear command to clear the terminal screen. |
| `backspace()` | Sends the Backspace command. |

### Waiting and Timing

| Method | Description |
|---|---|
| `waitFor(WaitType waitType)` | Waits for the specified terminal event, using a default timeout of 30 seconds. |
| `waitFor(WaitType waitType, int seconds)` | Waits for the specified terminal event for up to the given number of seconds. |
| `waitFor(WaitType waitType, long seconds)` | Overload that accepts the timeout in a `long` value. |
| `waitSeconds(int seconds)` | Waits for the specified number of seconds. |
| `waitSeconds(long seconds)` | Overload that accepts the duration in a `long` value. |
| `waitForText(String text, int timeoutMillis)` | Repeatedly checks the screen until the specified text appears or the timeout is reached. Throws `S3270EmulatorException` on timeout. |
| `sleep(int millis)` | Pauses the current thread for the specified number of milliseconds. If interrupted, restores the thread's interrupted status. |

## Exception Handling

The library throws the custom `S3270EmulatorException` exception for session-related errors, such as failing to find fields or timing out while waiting for text.

Example:

```java
try {
    emulator.waitForText("Login successful", 3000);
} catch (S3270EmulatorException e) {
    // Log and handle the exception
}
```

## External Dependency: s3270

`terminus3270` uses [s3270](https://github.com/pmattes/x3270), a scripting-oriented IBM 3270 terminal emulator from the x3270 project.

The s3270 executable is **not bundled or distributed** with `terminus3270`. Users are responsible for obtaining, installing, and configuring a compatible version for their operating system.

- **Project:** https://github.com/pmattes/x3270
- **Documentation:** https://x3270.miraheze.org/wiki/Main_Page
- **License information:** https://github.com/pmattes/x3270/blob/main/LICENSE.md

`terminus3270` is an independent, open-source project. It is not affiliated with, endorsed by, or sponsored by the x3270 project or its developers.

All rights to s3270 and the x3270 project remain with their respective copyright holders. Please consult the upstream project for its license terms and installation instructions.

## Tests

The tests use JUnit 5 and Mockito to mock sessions and validate the behavior of high-level methods.

## Project Structure

- `src/main/java/com/github/marcosws/terminus3270/` — Main source code
- `src/test/java/com/github/marcosws/terminus3270/` — Unit and Integration tests

## Requirements

- Java 11+
- (Optional) The s3270 tool installed for real integration

## Author

Marcos Willian de Souza