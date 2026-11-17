package io.github.tdteixeira.hl7handler;

/**
 * Thrown when the HL7 input is missing or blank, or when the MLLP byte stream cannot be read.
 */
public class Hl7ParseException extends Hl7Exception {

    private static final long serialVersionUID = 1L;

    public Hl7ParseException(String message) {
        super(message);
    }

    public Hl7ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
