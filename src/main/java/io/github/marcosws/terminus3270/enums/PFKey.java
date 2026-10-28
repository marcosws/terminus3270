package io.github.marcosws.terminus3270.enums;


/**
 * Enum representing the PF keys (Program Function keys) used in 3270 terminal sessions.
 * Each PF key is associated with a specific integer value that corresponds to its position on the terminal keyboard. The enum provides a method to retrieve the integer value of each PF key, which can be used in the underlying implementation to send the appropriate commands to the terminal when a PF key is pressed.
 * @author Marcos Willian de Souza
 * @version 1.0
 * @since 2026-04
 */
public enum PFKey {
	
    PF1(1), 
    PF2(2), 
    PF3(3), 
    PF4(4),
    PF5(5), 
    PF6(6), 
    PF7(7), 
    PF8(8),
    PF9(9), 
    PF10(10), 
    PF11(11), 
    PF12(12),
    PF13(13), 
    PF14(14), 
    PF15(15), 
    PF16(16),
    PF17(17), 
    PF18(18), 
    PF19(19), 
    PF20(20),
    PF21(21), 
    PF22(22), 
    PF23(23), 
    PF24(24);

    private final int value;

    PFKey(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
