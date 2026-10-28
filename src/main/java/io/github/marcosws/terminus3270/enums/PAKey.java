package io.github.marcosws.terminus3270.enums;

/**
 * Enum representing the PA keys (Program Attention keys) used in 3270 terminal sessions.
 * Each PA key is associated with a specific integer value that corresponds to its position on the terminal keyboard. The enum provides a method to retrieve the integer value of each PA key, which can be used in the underlying implementation to send the appropriate commands to the terminal when a PA key is pressed.
 * @author Marcos Willian de Souza
 * @version 1.0
 * @since 2026-04
 */
public enum PAKey {
	
    PA1(1),
    PA2(2),
    PA3(3);

    private final int value;

    PAKey(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
