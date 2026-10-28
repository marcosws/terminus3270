package io.github.marcosws.terminus3270.exceptions;

public class S3270EmulatorException  extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public S3270EmulatorException(String message) {
		super(message);
	}
	
	public S3270EmulatorException(String message, Throwable cause) {
		super(message, cause);
	}

}
