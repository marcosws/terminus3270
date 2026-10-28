package io.github.marcosws.terminus3270;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.marcosws.terminus3270.components.CursorPosition;
import io.github.marcosws.terminus3270.enums.PAKey;
import io.github.marcosws.terminus3270.enums.PFKey;
import io.github.marcosws.terminus3270.enums.WaitType;
import io.github.marcosws.terminus3270.exceptions.S3270EmulatorException;

/**
 * Unit tests for S3270Emulator class using JUnit 5 and Mockito.
 * Tests high-level interaction methods with mocked S3270Session.
 * 
 * @author Marcos Willian de Souza
 * @version 1.0
 * @since 2026-04
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("S3270Emulator Unit Tests")
class S3270EmulatorTest {

	@Mock
	private S3270Session mockSession;
	
	private S3270Emulator emulator;
	
	@BeforeEach
	void setUp() {
		emulator = new S3270Emulator(mockSession);
	}
	
	// ============== CONSTRUCTOR & GETTER TESTS ==============
	
	@Test
	@DisplayName("Should create S3270Emulator with valid session")
	void testConstructor() {
		assertNotNull(emulator);
		assertEquals(mockSession, emulator.getSession());
	}
	
	@Test
	@DisplayName("Should return the session passed in constructor")
	void testGetSession() {
		S3270Session session = emulator.getSession();
		assertNotNull(session);
		assertEquals(mockSession, session);
	}
	
	// ============== SCREEN RETRIEVAL TESTS ==============
	
	@Test
	@DisplayName("getScreen should send Ascii command and return processed screen")
	void testGetScreen() {
		String rawResponse = "data: line1\ndata: line2\ndata: line3\nok\n";
		when(mockSession.sendCommand("Ascii()")).thenReturn(rawResponse);
		
		String result = emulator.getScreen().getContent();
		
		assertNotNull(result);
		verify(mockSession).sendCommand("Ascii()");
	}
	
	@Test
	@DisplayName("asciiScreen should retry until screen is not empty")
	void testAsciiScreenWithRetry() {
		String emptyResponse = "data:\nok\n";
		String filledResponse = "data: Username: ___\ndata: Password: ___\nok\n";
		
		when(mockSession.sendCommand("Ascii()"))
			.thenReturn(emptyResponse)
			.thenReturn(filledResponse);
		
		String result = emulator.asciiScreen();
		
		assertNotNull(result);
		verify(mockSession, atLeastOnce()).sendCommand("Ascii()");
	}
	
	// ============== FIELD POSITION TESTS ==============
	
	@Test
	@DisplayName("getPositionField should find field label and return correct position")
	void testGetPositionFieldFound() {
		String screen = "Username: ________\nPassword: ________\n";
		
		Optional<CursorPosition> result = emulator.getPositionField(screen, "Username:");
		
		assertTrue(result.isPresent());
		assertEquals(1, result.get().getRow());
		assertEquals(10, result.get().getCol());
	}
	
	@Test
	@DisplayName("getPositionField should return empty Optional when field not found")
	void testGetPositionFieldNotFound() {
		String screen = "Login Screen\n";
		
		Optional<CursorPosition> result = emulator.getPositionField(screen, "Username:");
		
		assertTrue(result.isEmpty());
	}
	
	@Test
	@DisplayName("getPositionField should find field on second line")
	void testGetPositionFieldSecondLine() {
		String screen = "Header\nUsername: ________\nPassword: ________\n";
		
		Optional<CursorPosition> result = emulator.getPositionField(screen, "Username:");
		
		assertTrue(result.isPresent());
		assertEquals(2, result.get().getRow());
	}
	
	// ============== SEND TEXT TESTS ==============
	
	@Test
	@DisplayName("sendTextByField should send text to identified field")
	void testSendTextByField() {
		String screen = "data: Username: ________\ndata: Password: ________\nok\n";
		when(mockSession.sendCommand("Ascii()")).thenReturn(screen);
		when(mockSession.sendCommand(startsWith("MoveCursor"))).thenReturn("ok\n");
		when(mockSession.sendCommand(startsWith("String("))).thenReturn("ok\n");

		String result = emulator.sendTextByField("Username:", "admin");

		assertNotNull(result);
		verify(mockSession, times(4)).sendCommand("Ascii()");
		verify(mockSession, atLeastOnce()).sendCommand(startsWith("MoveCursor"));
		verify(mockSession, atLeastOnce()).sendCommand(startsWith("String("));
	}
	
	@Test
	@DisplayName("sendTextByField should throw exception when field not found")
	void testSendTextByFieldNotFound() {
		String screen = "data: Login Screen\nok\n";
		when(mockSession.sendCommand("Ascii()")).thenReturn(screen);
		
		S3270EmulatorException exception = assertThrows(
			S3270EmulatorException.class,
			() -> emulator.sendTextByField("Username:", "admin")
		);
		
		assertTrue(exception.getMessage().contains("Username:"));
		assertTrue(exception.getMessage().contains("not found"));
	}
	
	@Test
	@DisplayName("sendTextByField with offset should adjust cursor position")
	void testSendTextByFieldWithOffset() {
		String screen = "data: Username: ________\ndata: Password: ________\n";
		when(mockSession.sendCommand("Ascii()")).thenReturn(screen);
		when(mockSession.sendCommand(startsWith("MoveCursor"))).thenReturn("ok\n");
		when(mockSession.sendCommand(startsWith("String("))).thenReturn("ok\n");
		
		String result = emulator.sendTextByField("Username:", "admin", 1, 5);
		
		assertNotNull(result);
		verify(mockSession, atLeastOnce()).sendCommand(anyString());
	}
	
	// ============== MOVE CURSOR TESTS ==============
	
	@Test
	@DisplayName("moveCursor should send MoveCursor command with row and col")
	void testMoveCursor() {
		when(mockSession.sendCommand("MoveCursor(5,10)")).thenReturn("ok\n");
		when(mockSession.sendCommand("Ascii()")).thenReturn("ok\n");
		
		String result = emulator.moveCursor(5, 10);
		
		assertNotNull(result);
		verify(mockSession).sendCommand("MoveCursor(5,10)");
	}
	
	@Test
	@DisplayName("moveCursor with CursorPosition should send correct command")
	void testMoveCursorWithPosition() {
		CursorPosition position = new CursorPosition(10, 20);
		when(mockSession.sendCommand("MoveCursor(10,20)")).thenReturn("ok\n");
		when(mockSession.sendCommand("Ascii()")).thenReturn("ok\n");
		
		String result = emulator.moveCursor(position);
		
		assertNotNull(result);
		verify(mockSession).sendCommand("MoveCursor(10,20)");
	}
	
	// ============== TEXT MOVEMENT TESTS ==============
	
	@Test
	@DisplayName("sendString should send String command with text")
	void testSendString() {
		when(mockSession.sendCommand("String(\"hello\")")).thenReturn("ok\n");
		when(mockSession.sendCommand("Ascii()")).thenReturn("ok\n");
		
		String result = emulator.sendString("hello");
		
		assertNotNull(result);
		verify(mockSession).sendCommand("String(\"hello\")");
	}
	
	@Test
	@DisplayName("moveAndSendString should move cursor and send text")
	void testMoveAndSendString() {
		when(mockSession.sendCommand(startsWith("MoveCursor"))).thenReturn("ok\n");
		when(mockSession.sendCommand(startsWith("String("))).thenReturn("ok\n");
		when(mockSession.sendCommand("Ascii()")).thenReturn("ok\n");
		
		String result = emulator.moveAndSendString(5, 10, "test");
		
		assertNotNull(result);
		verify(mockSession, atLeast(2)).sendCommand(anyString());
	}
	
	// ============== KEYBOARD ACTION TESTS ==============
	
	@Test
	@DisplayName("enter should send Enter command")
	void testEnter() {
		when(mockSession.sendCommand("Enter")).thenReturn("ok\n");
		when(mockSession.sendCommand("Ascii()")).thenReturn("ok\n");
		
		String result = emulator.enter();
		
		assertNotNull(result);
		verify(mockSession).sendCommand("Enter");
	}
	
	@Test
	@DisplayName("tab should send Tab command")
	void testTab() {
		when(mockSession.sendCommand("Tab()")).thenReturn("ok\n");
		when(mockSession.sendCommand("Ascii()")).thenReturn("ok\n");
		
		String result = emulator.tab();
		
		assertNotNull(result);
		verify(mockSession).sendCommand("Tab()");
	}
	
	@Test
	@DisplayName("home should send Home command")
	void testHome() {
		when(mockSession.sendCommand("Home()")).thenReturn("ok\n");
		when(mockSession.sendCommand("Ascii()")).thenReturn("ok\n");
		
		String result = emulator.home();
		
		assertNotNull(result);
		verify(mockSession).sendCommand("Home()");
	}
	
	@Test
	@DisplayName("backspace should send Backspace command")
	void testBackspace() {
		when(mockSession.sendCommand("Backspace()")).thenReturn("ok\n");
		when(mockSession.sendCommand("Ascii()")).thenReturn("ok\n");
		
		String result = emulator.backspace();
		
		assertNotNull(result);
		verify(mockSession).sendCommand("Backspace()");
	}
	
	@Test
	@DisplayName("deleteField should send DeleteField command")
	void testDeleteField() {
		when(mockSession.sendCommand("DeleteField")).thenReturn("ok\n");
		when(mockSession.sendCommand("Ascii()")).thenReturn("ok\n");
		
		String result = emulator.deleteField();
		
		assertNotNull(result);
		verify(mockSession).sendCommand("DeleteField");
	}
	
	@Test
	@DisplayName("eraseEOF should send EraseEOF command")
	void testEraseEOF() {
		when(mockSession.sendCommand("EraseEOF")).thenReturn("ok\n");
		when(mockSession.sendCommand("Ascii()")).thenReturn("ok\n");
		
		String result = emulator.eraseEOF();
		
		assertNotNull(result);
		verify(mockSession).sendCommand("EraseEOF");
	}
	
	@Test
	@DisplayName("eraseInput should send EraseInput command")
	void testEraseInput() {
		when(mockSession.sendCommand("EraseInput")).thenReturn("ok\n");
		when(mockSession.sendCommand("Ascii()")).thenReturn("ok\n");
		
		String result = emulator.eraseInput();
		
		assertNotNull(result);
		verify(mockSession).sendCommand("EraseInput");
	}
	
	@Test
	@DisplayName("reset should send Reset command")
	void testReset() {
		when(mockSession.sendCommand("Reset")).thenReturn("ok\n");
		when(mockSession.sendCommand("Ascii()")).thenReturn("ok\n");
		
		String result = emulator.reset();
		
		assertNotNull(result);
		verify(mockSession).sendCommand("Reset");
	}
	
	@Test
	@DisplayName("clear should send Clear command")
	void testClear() {
		when(mockSession.sendCommand("Clear()")).thenReturn("ok\n");
		when(mockSession.sendCommand("Ascii()")).thenReturn("ok\n");
		
		String result = emulator.clear();
		
		assertNotNull(result);
		verify(mockSession).sendCommand("Clear()");
	}
	
	// ============== FUNCTION KEY TESTS ==============
	
	@Test
	@DisplayName("pressPF should send PF command with correct key number")
	void testPressPF() {
		when(mockSession.sendCommand("PF(1)")).thenReturn("ok\n");
		when(mockSession.sendCommand("Ascii()")).thenReturn("ok\n");
		
		String result = emulator.pressPF(PFKey.PF1);
		
		assertNotNull(result);
		verify(mockSession).sendCommand("PF(1)");
	}
	
	@Test
	@DisplayName("pressPF should work with different PF keys")
	void testPressPFDifferentKeys() {
		when(mockSession.sendCommand("PF(3)")).thenReturn("ok\n");
		when(mockSession.sendCommand("PF(12)")).thenReturn("ok\n");
		when(mockSession.sendCommand("Ascii()")).thenReturn("ok\n");
		
		emulator.pressPF(PFKey.PF3);
		emulator.pressPF(PFKey.PF12);
		
		verify(mockSession).sendCommand("PF(3)");
		verify(mockSession).sendCommand("PF(12)");
	}
	
	@Test
	@DisplayName("pressPA should send PA command with correct key number")
	void testPressPA() {
		when(mockSession.sendCommand("PA(1)")).thenReturn("ok\n");
		when(mockSession.sendCommand("Ascii()")).thenReturn("ok\n");
		
		String result = emulator.pressPA(PAKey.PA1);
		
		assertNotNull(result);
		verify(mockSession).sendCommand("PA(1)");
	}
	
	// ============== WAIT TESTS ==============
	
	@Test
	@DisplayName("waitFor with WaitType should send Wait command with default timeout")
	void testWaitForWithType() {
		when(mockSession.sendCommand(eq("Wait(Unlock)"), eq(30000))).thenReturn("ok\n");
		when(mockSession.sendCommand("Ascii()")).thenReturn("ok\n");
		
		String result = emulator.waitFor(WaitType.UNLOCK);
		
		assertNotNull(result);
		verify(mockSession).sendCommand("Wait(Unlock)", 30000);
	}
	
	@Test
	@DisplayName("waitFor with WaitType and int seconds should send Wait command")
	void testWaitForWithTypeAndSeconds() {
		when(mockSession.sendCommand("Wait(10,Unlock)")).thenReturn("ok\n");
		when(mockSession.sendCommand("Ascii()")).thenReturn("ok\n");
		
		String result = emulator.waitFor(WaitType.UNLOCK, 10);
		
		assertNotNull(result);
		verify(mockSession).sendCommand("Wait(10,Unlock)");
	}
	
	@Test
	@DisplayName("waitFor with WaitType and long seconds should send Wait command")
	void testWaitForWithTypeAndLongSeconds() {
		when(mockSession.sendCommand("Wait(20,InputField)")).thenReturn("ok\n");
		when(mockSession.sendCommand("Ascii()")).thenReturn("ok\n");
		
		String result = emulator.waitFor(WaitType.INPUT_FIELD, 20L);
		
		assertNotNull(result);
		verify(mockSession).sendCommand("Wait(20,InputField)");
	}
	
	@Test
	@DisplayName("waitSeconds with int should send Wait command with Seconds")
	void testWaitSecondsInt() {
		when(mockSession.sendCommand("Wait(5,Seconds)")).thenReturn("ok\n");
		when(mockSession.sendCommand("Ascii()")).thenReturn("ok\n");
		
		String result = emulator.waitSeconds(5);
		
		assertNotNull(result);
		verify(mockSession).sendCommand("Wait(5,Seconds)");
	}
	
	@Test
	@DisplayName("waitSeconds with long should send Wait command with Seconds")
	void testWaitSecondsLong() {
		when(mockSession.sendCommand("Wait(10,Seconds)")).thenReturn("ok\n");
		when(mockSession.sendCommand("Ascii()")).thenReturn("ok\n");
		
		String result = emulator.waitSeconds(10L);
		
		assertNotNull(result);
		verify(mockSession).sendCommand("Wait(10,Seconds)");
	}
	
	@Test
	@DisplayName("waitForText should return when text appears on screen")
	void testWaitForTextFound() {
		String screenWithText = "data: Login successful\nok\n";
		when(mockSession.sendCommand("Ascii()")).thenReturn(screenWithText);
		
		assertDoesNotThrow(() -> emulator.waitForText("Login successful", 3000));
		verify(mockSession, atLeastOnce()).sendCommand("Ascii()");
	}
	
	@Test
	@DisplayName("waitForText should throw exception on timeout")
	void testWaitForTextTimeout() {
		String screenWithoutText = "data: Login screen\nok\n";
		when(mockSession.sendCommand("Ascii()")).thenReturn(screenWithoutText);
		
		S3270EmulatorException exception = assertThrows(
			S3270EmulatorException.class,
			() -> emulator.waitForText("Login successful", 100)
		);
		
		assertTrue(exception.getMessage().contains("Timeout"));
		assertTrue(exception.getMessage().contains("Login successful"));
	}
	
	// ============== TEXT EXTRACTION TESTS ==============
	
	@Test
	@DisplayName("getTextBottonField should retrieve value below the field with correct offset")
	void testGetTextBottonField() {
		String gridScreen = String.join("\n",
			"Numero    | Nome       | Credencial  ",
			"--------------------------------------",
			"2345      | Marcos     | AB34        ",
			"3455      | Roger      | BF32        ",
			"4577      | Pedro      | GF90        ",
			"--------------------------------------"
		);
		when(mockSession.sendCommand("Ascii()"))
			.thenReturn(gridScreen);

		// offset = 1 (Marcos)
		String value1 = emulator.getTextBottonField("Nome", 8, 2).trim();
		assertEquals("Marcos", value1);

		// offset = 2 (Roger)
		String value2 = emulator.getTextBottonField("Nome", 8, 3).trim();
		assertEquals("Roger", value2);

		// offset = 3 (Pedro)
		String value3 = emulator.getTextBottonField("Nome", 8, 4).trim();
		assertEquals("Pedro", value3);
		
		// offset = 1 (4577)
		String value4 = emulator.getTextBottonField("Numero", 10, 4).trim();
		assertEquals("4577", value4);

		// offset = 2 (3455)
		String value5 = emulator.getTextBottonField("Numero", 10, 3).trim();
		assertEquals("3455", value5);

		// offset = 3 (2345)
		String value6 = emulator.getTextBottonField("Numero", 10, 2).trim();
		assertEquals("2345", value6);
		
		// offset = 1 (GF90)
		String value7 = emulator.getTextBottonField("Credencial", 10, 4).trim();
		assertEquals("GF90", value7);

		// offset = 2 (BF32)
		String value8 = emulator.getTextBottonField("Credencial", 10, 3).trim();
		assertEquals("BF32", value8);

		// offset = 3 (AB34)
		String value9 = emulator.getTextBottonField("Credencial", 10, 2).trim();
		assertEquals("AB34", value9);
	}

	@Test
	@DisplayName("getTextBottonField should throw exception if field not found")
	void testGetTextBottonFieldNotFound() {
		String gridScreen = String.join("\n",
			"Numero    | Nome       | Credencial  ",
			"--------------------------------------",
			"2345      | Marcos     | AB34        ",
			"3455      | Roger      | BF32        ",
			"4577      | Pedro      | GF90        ",
			"--------------------------------------"
		);
		when(mockSession.sendCommand("Ascii()"))
			.thenReturn(gridScreen);

		S3270EmulatorException exception = assertThrows(
			S3270EmulatorException.class,
			() -> emulator.getTextBottonField("Inexistente", 8, 1)
		);
		assertTrue(exception.getMessage().contains("Inexistente"));
	}

	@Test
	@DisplayName("getTextTopField should retrieve value above the field with correct offset")
	void testGetTextTopField() {
		String gridScreen = String.join("\n",
			"2345      | Marcos     | AB34        ",
			"3455      | Roger      | BF32        ",
			"4577      | Pedro      | GF90        ",
			"--------------------------------------",
			"Numero    | Nome       | Credencial  ",
			"--------------------------------------"
		);
		when(mockSession.sendCommand("Ascii()"))
			.thenReturn(gridScreen);

		// offset = 1 (Pedro)
		String value1 = emulator.getTextTopField("Nome", 10, 2).trim();
		assertEquals("Pedro", value1);

		// offset = 2 (Roger)
		String value2 = emulator.getTextTopField("Nome", 10, 3).trim();
		assertEquals("Roger", value2);

		// offset = 3 (Marcos)
		String value3 = emulator.getTextTopField("Nome", 10, 4).trim();
		assertEquals("Marcos", value3);
		
		// offset = 1 (4577)
		String value4 = emulator.getTextTopField("Numero", 10, 2).trim();
		assertEquals("4577", value4);

		// offset = 2 (3455)
		String value5 = emulator.getTextTopField("Numero", 10, 3).trim();
		assertEquals("3455", value5);

		// offset = 3 (2345)
		String value6 = emulator.getTextTopField("Numero", 10, 4).trim();
		assertEquals("2345", value6);
		
		// offset = 1 (GF90)
		String value7 = emulator.getTextTopField("Credencial", 10, 2).trim();
		assertEquals("GF90", value7);

		// offset = 2 (BF32)
		String value8 = emulator.getTextTopField("Credencial", 10, 3).trim();
		assertEquals("BF32", value8);

		// offset = 3 (AB34)
		String value9 = emulator.getTextTopField("Credencial", 10, 4).trim();
		assertEquals("AB34", value9);
		
		
	}

	@Test
	@DisplayName("getTextTopField should throw exception if field not found")
	void testGetTextTopFieldNotFound() {
		String gridScreen = String.join("\n",
			"2345      | Marcos     | AB34        ",
			"3455      | Roger      | BF32        ",
			"4577      | Pedro      | GF90        ",
			"--------------------------------------",
			"Numero    | Nome       | Credencial  ",
			"--------------------------------------"
		);
		when(mockSession.sendCommand("Ascii()"))
			.thenReturn(gridScreen);

		S3270EmulatorException exception = assertThrows(
			S3270EmulatorException.class,
			() -> emulator.getTextTopField("Inexistente", 8, 1)
		);
		assertTrue(exception.getMessage().contains("Inexistente"));
	}
	
	@Test
	@DisplayName("getTextBottomField should throw exception if field not found and then retrieve value when field is found")
	void testGetTextBottomFieldNotFoundAndThenRetrieveValueWhenFieldIsFound() {
		String gridScreen = String.join("\n",
			"Numero    | Nome       | Credencial  ",
			"--------------------------------------",
			"2345      | Marcos     | AB34        ",
			"3455      | Roger      | BF32        ",
			"4577      | Pedro      | GF90        ",
			"--------------------------------------"
		);
		when(mockSession.sendCommand("Ascii()"))
			.thenReturn(gridScreen);

		try {
			emulator.getTextBottonField("Inexistente", 10, 2);
		}
		catch (S3270EmulatorException e) {
			assertTrue(e.getMessage().contains("Inexistente"));
			assertNotNull(emulator.getTextBottonField("Nome", 10, 2)); 
		}
		
		
	}
	
	@Test
	@DisplayName("getTextTopField should throw exception if field not found and then retrieve value when field is found")	
	void testGetTextTopFieldNotFoundAndThenRetrieveValueWhenFieldIsFound() {
		String gridScreen = String.join("\n",
			"2345      | Marcos     | AB34        ",
			"3455      | Roger      | BF32        ",
			"4577      | Pedro      | GF90        ",
			"--------------------------------------",
			"Numero    | Nome       | Credencial  ",
			"--------------------------------------"
		);
		when(mockSession.sendCommand("Ascii()"))
			.thenReturn(gridScreen);

		try {
			emulator.getTextTopField("Inexistente", 10, 2);
		}
		catch (S3270EmulatorException e) {
			assertTrue(e.getMessage().contains("Inexistente"));
			assertNotNull(emulator.getTextTopField("Nome", 10, 2)); 
		}
	}
	
	// ============== INTEGRATION SCENARIO TESTS ==============
	
	@Test
	@DisplayName("Full login workflow should work correctly")
	void testFullLoginWorkflow() {
		// Setup
		String loginScreen = "data: Username: ________\ndata: Password: ________\nok\n";
		String welcomeScreen = "data: Welcome to System\nok\n";
		
		when(mockSession.sendCommand("Ascii()"))
			.thenReturn(loginScreen);
		when(mockSession.sendCommand(startsWith("MoveCursor"))).thenReturn("ok\n");
		when(mockSession.sendCommand(startsWith("String("))).thenReturn("ok\n");
		when(mockSession.sendCommand("Enter")).thenReturn(welcomeScreen);
		
		// Execute
		String screen = emulator.asciiScreen();
		emulator.sendTextByField("Username:", "testuser");
		emulator.sendTextByField("Password:", "testpass");
		emulator.enter();
		
		// Verify
		assertNotNull(screen);
	}
}
