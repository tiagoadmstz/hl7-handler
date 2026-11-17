package io.github.tdteixeira.hl7handler.mapping;

import io.github.tdteixeira.hl7handler.Hl7Exception;

/**
 * Thrown when an HL7 mapping operation fails due to version mismatch,
 * unsupported schema, invalid path, unknown type conversion or syntax errors.
 */
public class Hl7MappingException extends Hl7Exception {

    private static final long serialVersionUID = 1L;

    public Hl7MappingException(String message) {
        super(message);
    }

    public Hl7MappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
