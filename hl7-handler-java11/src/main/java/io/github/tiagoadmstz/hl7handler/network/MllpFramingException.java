package io.github.tiagoadmstz.hl7handler.network;

import io.github.tiagoadmstz.hl7handler.Hl7Exception;

/**
 * Exception thrown when an MLLP stream violates framing boundaries
 * (e.g. missing start block, truncated EOF before end block, or exceeded message size).
 */
public class MllpFramingException extends Hl7Exception {

    private static final long serialVersionUID = 1L;

    public MllpFramingException(String message) {
        super(message);
    }

    public MllpFramingException(String message, Throwable cause) {
        super(message, cause);
    }
}
