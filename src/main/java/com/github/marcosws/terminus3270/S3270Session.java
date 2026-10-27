package com.github.marcosws.terminus3270;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.marcosws.terminus3270.exceptions.S3270SessionException;
import com.github.marcosws.terminus3270.interfaces.IControlListener;


/**
 * S3270Session class manages a session with the s3270 terminal emulator.
 * It provides methods to connect, disconnect, send commands, and check connection status.
 * The class uses a Process to interact with the s3270 command-line tool and handles I/O streams for communication.
 * It also includes error handling and logging for better traceability of operations.
 * 
 * @author Marcos Willian de Souza
 * @version 1.0
 * @since 2026-04
 */
public class S3270Session implements AutoCloseable {
	
	private static final Logger logger = LoggerFactory.getLogger(S3270Session.class);
	
    private final List<String> command;
    private final Map<String, String> environment;
	
	private Process process;
	private BufferedWriter writer;
	private BufferedReader reader;
	private ExecutorService executor;
	
	
    private List<IControlListener> listeners = new ArrayList<>();

    public void addCloseListener(IControlListener listener) {
    	listeners.add(listener);
    }
    
	public S3270Session() {
		this(List.of("s3270"), Map.of());
		logger.info("Initializing s3270 session");
        executor = Executors.newSingleThreadExecutor();
	}
	
	
	public S3270Session(List<String> command, Map<String, String> environment) {
        this.command = List.copyOf(command);
        this.environment = Map.copyOf(environment);
		logger.info("Initializing s3270 session");
        executor = Executors.newSingleThreadExecutor();
	}
	
	/**
	 * Quits the s3270 session by sending the "Quit" command and then closing the session.
	 * @return The response from the s3270 process after sending the "Quit" command.
	 */
	public String quit() {
	    logger.info("Quitting s3270 session");
	    try {
	        return sendCommand("Quit");
	    } catch (S3270SessionException e) {
	        logger.debug("s3270 closed the stream after Quit (expected)", e);
	        return "";
	    } finally {
	        close();
	    }
	}
	/**
	 * Closes the s3270 session by closing the I/O streams and destroying the process
	 * and shutting down the executor service to prevent thread leaks.
	 * Logs the closing process and any errors that occur during closing.
	 */
    @Override
	public void close()  {
    	logger.info("Closing s3270 session");
		try {
			if (writer != null) 
				writer.close();
			if (reader != null) 
				reader.close();
			if (process != null && process.isAlive()) {
				process.destroy();
				try {
					process.waitFor(2, TimeUnit.SECONDS);
				}
				catch (InterruptedException e) {
					process.destroyForcibly();
					logger.warn("Process did not terminate gracefully, forced termination applied", e);
				}
			}
		} 
		catch (IOException e) {
			logger.error("Error closing s3270 session", e);
			throw new S3270SessionException("Error closing session", e);
		}
	    finally {
	    	logger.info("Shutting down executor service");
	        for (IControlListener l : listeners) {
	            l.onClose();
	        }
	        executor.shutdownNow(); // Fecha o executor para evitar vazamento de threads
	    }
	}
	
    /**
	 * Checks if the session is properly initialized by verifying that the writer, reader, and process are not null.
	 * If any of these components are null, it throws an S3270SessionException indicating that the session is not initialized.
	 * This method is called before performing any operations that require an active session to ensure that the session is ready for use.
	 */
    public void checkSession() {
    	logger.info("Checking s3270 session state");
	    if (writer == null || reader == null || process == null) {
	        throw new S3270SessionException(
	            "Session not initialized. Call connect(host, port) first."
	        );
	    }
	}
	
    /**
	 * Connects to an s3270 session at the specified host and port by starting the s3270 process and sending the appropriate connect command.
	 * It initializes the I/O streams for communication with the process and returns the response from the s3270 process after sending the connect command.
	 * @param host The hostname or IP address of the s3270 session to connect to
	 * @param port The port number of the s3270 session to connect to
	 * @return The response from the s3270 process after sending the connect command
	 * @throws S3270SessionException If there is an error starting the s3270
	 * process or if there is an error sending the connect command
	 * @see #checkSession() for ensuring the session is properly initialized before performing operations
	 * @see #sendCommand(String) for sending commands to the s3270 process
	 * @see #close() for properly closing the session and releasing resources
	 */
    public String connect(String host, String port) {
		
		logger.info("Connecting to s3270 session at {}:{}", host, port);
		ProcessBuilder processBuilder = null;
		processBuilder = new ProcessBuilder(command);
		processBuilder.environment().putAll(environment);
		processBuilder.redirectErrorStream(true);
		try {
			process = processBuilder.start();
		} 
		catch (IOException e) {
			logger.error("Error starting s3270 process", e);
			throw new S3270SessionException("Error starting s3270 process", e);
		}
		writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
		reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		
        return sendCommand("connect(" + host + ":" + port + ")");
	}
    
    /**
	 * Disconnects from the s3270 session by sending the "Disconnect" command to the
	 * s3270 process and returns the response from the process after sending the command.
	 * @return The response from the s3270 process after sending the "Disconnect" command
	 * if the session is not properly initialized
	 */
	public String disconnect() {
		try {
			logger.info("Disconnecting s3270 session");
			return sendCommand("Disconnect");
		}
		catch(S3270SessionException ignore) {
			logger.info("Disconnected s3270 session");
			return "";
		}
	}
	
	/**
	 * Checks if the s3270 session is currently connected by sending a "Query(ConnectionState)" command to the s3270 process and analyzing the response.
	 * It reads the response from the process until it encounters an "ok" line, and
	 * @return true if the response contains "connected-3270", indicating that the session is connected, or false otherwise.
	 * @throws S3270SessionException If there is an error sending the command or reading
	 * the response, or if the session is not properly initialized
	 * @see #checkSession() for ensuring the session is properly initialized before performing operations
	 * 
	 */
    public boolean isConnected() {
		logger.info("Checking if s3270 session is connected");
		try {
			checkSession();
			return sendCommand("Query(ConnectionState)").contains("connected-3270");
		}
		catch (S3270SessionException ignore) {
			return false;
		}
	}
	
    public String sendCommand(String command) {
		logger.info("Sending command to s3270 session: {}", command);
		checkSession();
		
        try {
            writer.write(command);
            writer.newLine();
            writer.flush();
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
                if ("ok".equals(line)) {
                	return response.toString();
                }
                if ("error".equals(line)) {
                	logger.error("S3270 returned error for command: {}\nResponse:\n{}", command, response.toString());
                    throw new S3270SessionException(
                        "S3270 returned error for command: " + command + "\nResponse:\n" + response
                    );
                }
            }
            logger.error("S3270 stream closed unexpectedly while waiting for response to command: {}", command);
            throw new S3270SessionException("S3270 stream closed unexpectedly.");
        } 
        catch (IOException e) {
        	logger.error("Error sending command: {}", command, e);
            throw new S3270SessionException("Error sending command: " + command, e);
        }
		
	}
	
    /**
     * Sends a command to the s3270 session with a specified timeout. It uses an ExecutorService to run the command sending in a separate thread and waits for the response until the timeout is reached.
     * If the command execution exceeds the timeout, it cancels the task and throws an S3270SessionException indicating that the timeout was reached. If there is an error during command execution, it throws an S3270SessionException with the cause of the error.
     * This method is useful for commands that may take a long time to execute or for ensuring that the application does not hang indefinitely while waiting for a response from the s3270 process.
     * @param command The command to be sent to the s3270 session
	 * @param timeoutMillis The maximum time to wait for the command response in milliseconds
	 * @return The response from the s3270 process after sending the command
	 * @throws S3270SessionException If the timeout is reached, if there is an
	 * error during command execution, or if the session is not properly initialized
     */
    public String sendCommand(String command, int timeoutMillis) {
    	
		logger.info("Sending command with timeout to s3270 session: {} (timeout: {} ms)", command, timeoutMillis);
	    checkSession();

	    Future<String> future = executor.submit(() -> {
	        try {
	            writer.write(command);
	            writer.newLine();
	            writer.flush();
	            StringBuilder response = new StringBuilder();
	            String line;
	            while ((line = reader.readLine()) != null) {
	                response.append(line).append("\n");
	                if ("ok".equals(line)) {
	                	return response.toString();
	                }
	                if ("error".equals(line)) {
	                	logger.error("S3270 returned error for command: {}\nResponse:\n{}", command, response.toString());
	                    throw new S3270SessionException(
	                        "S3270 returned error for command: " + command + "\nResponse:\n" + response
	                    );
	                }
	            }
	            logger.error("S3270 stream closed unexpectedly while waiting for response to command: {}", command);
	            throw new S3270SessionException("S3270 stream closed unexpectedly.");
	        } 
	        catch (IOException e) {
	        	logger.error("Error sending command: {}", command, e);
	            throw new S3270SessionException("Error sending command: " + command, e);
	        }
	    });

	    try {
	        return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
	    } 
	    catch (TimeoutException e) {
	    	logger.error("Timeout reached for command: {} (timeout: {} ms)", command, timeoutMillis, e);
	        future.cancel(true);
	        close();   // derruba o processo; o readLine() preso termina com EOF
	        throw new S3270SessionException("Timeout reached for command: " + command);
	    } 
	    catch (ExecutionException e) {
	    	logger.error("Execution exception for command: {}", command, e.getCause());
	        throw new S3270SessionException("Execution exception for command: " + command, e.getCause());
	    } 
	    catch (InterruptedException e) {
	    	logger.error("Interrupted while sending command: {}", command, e);
	        Thread.currentThread().interrupt();
	        throw new S3270SessionException("Interrupted while sending command: " + command, e);
	    } 
	}

}
