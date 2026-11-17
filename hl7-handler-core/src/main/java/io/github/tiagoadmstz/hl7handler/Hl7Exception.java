package io.github.tiagoadmstz.hl7handler;

/**
 * Base class of the unchecked exceptions thrown by the HL7 Handler library.
 */
public class Hl7Exception extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public Hl7Exception(String message) {
        super(message);
    }

    public Hl7Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
