package io.github.tdteixeira.hl7handler;

/**
 * Thrown when a map or message cannot be serialized to JSON, or JSON cannot be deserialized into a Value Object.
 */
public class Hl7BindingException extends Hl7Exception {

    private static final long serialVersionUID = 1L;

    public Hl7BindingException(String message) {
        super(message);
    }

    public Hl7BindingException(String message, Throwable cause) {
        super(message, cause);
    }
}
