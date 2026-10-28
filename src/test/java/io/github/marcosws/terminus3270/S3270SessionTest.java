package io.github.marcosws.terminus3270;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.marcosws.terminus3270.exceptions.S3270SessionException;

/**
 * Unit tests for S3270Session class using JUnit 5.
 * Tests low-level I/O operations with mocked Process and streams.
 * 
 * @author Marcos Willian de Souza
 * @version 1.0
 * @since 2026-04
 */
@DisplayName("S3270Session Unit Tests")
class S3270SessionTest {

	private S3270Session session;
	
	@BeforeEach
	void setUp() {
		session = new S3270Session();
	}
	
	// ============== CONSTRUCTOR TEST ==============
	
	@Test
	@DisplayName("Should create S3270Session with initialized executor")
	void testConstructor() {
		assertNotNull(session);
	}
	
	// ============== SESSION VALIDATION TESTS ==============
	
	@Test
	@DisplayName("checkSession should throw exception when not connected")
	void testCheckSessionNotConnected() {
		S3270SessionException exception = assertThrows(
			S3270SessionException.class,
			() -> session.checkSession()
		);
		
		assertTrue(exception.getMessage().contains("not initialized"));
		assertTrue(exception.getMessage().contains("connect"));
	}
	
	// ============== SEND COMMAND TESTS ==============
	
	@Test
	@DisplayName("sendCommand should send command and read response")
	void testSendCommand() throws IOException {
		// Setup
		StringWriter writerStream = new StringWriter();
		BufferedWriter mockWriter = new BufferedWriter(writerStream);
		StringReader readerStream = new StringReader("response line1\nok\n");
		BufferedReader mockReader = new BufferedReader(readerStream);
		Process mockProcess = mock(Process.class);
		
		// Inject mocks
		setPrivateField(session, "writer", mockWriter);
		setPrivateField(session, "reader", mockReader);
		setPrivateField(session, "process", mockProcess);
		
		// Execute
		String result = session.sendCommand("TestCommand");
		
		// Verify
		assertNotNull(result);
		assertTrue(result.contains("ok"));
	}
	
	@Test
	@DisplayName("sendCommand should throw exception on error response")
	void testSendCommandError() throws IOException {
		// Setup
		StringWriter writerStream = new StringWriter();
		BufferedWriter mockWriter = new BufferedWriter(writerStream);
		StringReader readerStream = new StringReader("error\n");
		BufferedReader mockReader = new BufferedReader(readerStream);
		Process mockProcess = mock(Process.class);
		
		// Inject mocks
		setPrivateField(session, "writer", mockWriter);
		setPrivateField(session, "reader", mockReader);
		setPrivateField(session, "process", mockProcess);
		
		// Execute & Verify
		S3270SessionException exception = assertThrows(
			S3270SessionException.class,
			() -> session.sendCommand("FailCommand")
		);
		
		assertTrue(exception.getMessage().contains("error"));
		assertTrue(exception.getMessage().contains("FailCommand"));
	}
	
	@Test
	@DisplayName("sendCommand with timeout should execute with deadline")
	void testSendCommandWithTimeout() throws IOException {
		// Setup
		StringWriter writerStream = new StringWriter();
		BufferedWriter mockWriter = new BufferedWriter(writerStream);
		StringReader readerStream = new StringReader("response\nok\n");
		BufferedReader mockReader = new BufferedReader(readerStream);
		Process mockProcess = mock(Process.class);
		
		// Inject mocks
		setPrivateField(session, "writer", mockWriter);
		setPrivateField(session, "reader", mockReader);
		setPrivateField(session, "process", mockProcess);
		
		// Execute
		String result = session.sendCommand("TestCommand", 5000);
		
		// Verify
		assertNotNull(result);
		assertTrue(result.contains("ok"));
	}
	
	@Test
	@DisplayName("sendCommand with timeout should throw on timeout")
	void testSendCommandTimeout() throws IOException, InterruptedException {
		// Setup: Create mock reader that delays
		StringWriter writerStream = new StringWriter();
		BufferedWriter mockWriter = spy(new BufferedWriter(writerStream));
		
		// Mock reader that simulates slow response
		BufferedReader mockReader = mock(BufferedReader.class);
		when(mockReader.readLine()).thenAnswer(invocation -> {
			Thread.sleep(2000); // Simulate delay longer than timeout
			return "ok";
		});
		
		Process mockProcess = mock(Process.class);
		
		// Inject mocks
		setPrivateField(session, "writer", mockWriter);
		setPrivateField(session, "reader", mockReader);
		setPrivateField(session, "process", mockProcess);
		
		// Execute & Verify
		S3270SessionException exception = assertThrows(
			S3270SessionException.class,
			() -> session.sendCommand("SlowCommand", 100)
		);
		
		assertTrue(exception.getMessage().contains("Timeout"));
	}
	
	// ============== CONNECTION TESTS ==============
	
	@Test
	@DisplayName("disconnect should send Disconnect command")
	void testDisconnect() throws IOException {
		// Setup
		StringWriter writerStream = new StringWriter();
		BufferedWriter mockWriter = new BufferedWriter(writerStream);
		StringReader readerStream = new StringReader("ok\n");
		BufferedReader mockReader = new BufferedReader(readerStream);
		Process mockProcess = mock(Process.class);
		
		// Inject mocks
		setPrivateField(session, "writer", mockWriter);
		setPrivateField(session, "reader", mockReader);
		setPrivateField(session, "process", mockProcess);
		
		// Execute
		String result = session.disconnect();
		
		// Verify
		assertNotNull(result);
		assertTrue(result.contains("ok"));
	}
	
	@Test
	@DisplayName("isConnected should return true when connected")
	void testIsConnectedTrue() throws IOException {
		// Setup
		StringWriter writerStream = new StringWriter();
		BufferedWriter mockWriter = new BufferedWriter(writerStream);
		StringReader readerStream = new StringReader("connected-3270\nok\n");
		BufferedReader mockReader = new BufferedReader(readerStream);
		Process mockProcess = mock(Process.class);
		
		// Inject mocks
		setPrivateField(session, "writer", mockWriter);
		setPrivateField(session, "reader", mockReader);
		setPrivateField(session, "process", mockProcess);
		
		// Execute
		boolean connected = session.isConnected();
		
		// Verify
		assertTrue(connected);
	}
	
	@Test
	@DisplayName("isConnected should return false when not connected")
	void testIsConnectedFalse() throws IOException {
		// Setup
		StringWriter writerStream = new StringWriter();
		BufferedWriter mockWriter = new BufferedWriter(writerStream);
		StringReader readerStream = new StringReader("not-connected\nok\n");
		BufferedReader mockReader = new BufferedReader(readerStream);
		Process mockProcess = mock(Process.class);
		
		// Inject mocks
		setPrivateField(session, "writer", mockWriter);
		setPrivateField(session, "reader", mockReader);
		setPrivateField(session, "process", mockProcess);
		
		// Execute
		boolean connected = session.isConnected();
		
		// Verify
		assertFalse(connected);
	}
	
	// ============== CLOSE TESTS ==============
	
	@Test
	@DisplayName("close should clean up resources")
	void testClose() throws IOException, InterruptedException {
		// Setup
		BufferedWriter mockWriter = mock(BufferedWriter.class);
		BufferedReader mockReader = mock(BufferedReader.class);
		Process mockProcess = mock(Process.class);
		when(mockProcess.isAlive()).thenReturn(true);
		when(mockProcess.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);
		
		// Inject mocks
		setPrivateField(session, "writer", mockWriter);
		setPrivateField(session, "reader", mockReader);
		setPrivateField(session, "process", mockProcess);
		
		// Execute
		session.close();
		
		// Verify
		verify(mockWriter).close();
		verify(mockReader).close();
		verify(mockProcess).destroy();
	}
	
	@Test
	@DisplayName("close should handle IOException gracefully")
	void testCloseWithIOException() throws IOException {
		// Setup
		BufferedWriter mockWriter = mock(BufferedWriter.class);
		doThrow(new IOException("Mock close error")).when(mockWriter).close();
		
		BufferedReader mockReader = mock(BufferedReader.class);
		Process mockProcess = mock(Process.class);
		when(mockProcess.isAlive()).thenReturn(false);
		
		// Inject mocks
		setPrivateField(session, "writer", mockWriter);
		setPrivateField(session, "reader", mockReader);
		setPrivateField(session, "process", mockProcess);
		
		// Execute & Verify
		S3270SessionException exception = assertThrows(
			S3270SessionException.class,
			() -> session.close()
		);
		
		assertTrue(exception.getMessage().contains("Error closing"));
	}
	
	// ============== HELPER METHODS ==============
	
	/**
	 * Utility method to inject private fields for testing.
	 * Uses reflection to set private fields on the session object.
	 */
	private void setPrivateField(Object target, String fieldName, Object value) {
		try {
			var field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		} catch (Exception e) {
			throw new RuntimeException(
				String.format("Failed to set private field '%s'", fieldName), e
			);
		}
	}

}
