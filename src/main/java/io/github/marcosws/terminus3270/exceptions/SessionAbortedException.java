package io.github.marcosws.terminus3270.exceptions;

/**
 * Thrown when the user aborts the session from the Terminal Viewer
 * (Abort Session). Scripts should catch this to stop cleanly.
 */
public class SessionAbortedException extends S3270EmulatorException {

    private static final long serialVersionUID = 1L;

    public SessionAbortedException(String message) {
        super(message);
    }

    public SessionAbortedException(String message, Throwable cause) {
        super(message, cause);
    }
}
