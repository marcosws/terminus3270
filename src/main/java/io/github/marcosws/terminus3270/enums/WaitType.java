package io.github.marcosws.terminus3270.enums;

/**
 * Enum representing the different wait types that can be used in 3270 terminal sessions.
 * Each wait type corresponds to a specific condition that can be waited for during terminal interactions, such as waiting for the terminal to be in a specific mode, waiting for a disconnect, or waiting for input/output operations to complete.
 * The enum values are associated with string representations that can be used in the underlying implementation to specify
 * the type of wait condition being applied.
 * @author Marcos Willian de Souza
 * @version 1.0
 * @since 2026-04
 */
public enum WaitType {
	
	T3270_MODE("3270Mode"),
	NVT_MODE("NVTMode"),
	DISCONNECT("Disconnect"),
	INPUT_FIELD("InputField"),
	OUTPUT("Output"),
	UNLOCK("Unlock");
	
	private final String value;

	WaitType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
