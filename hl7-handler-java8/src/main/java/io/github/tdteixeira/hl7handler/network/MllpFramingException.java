package io.github.tdteixeira.hl7handler.network;

import java.io.IOException;

/**
 * Exception thrown when an MLLP stream violates framing boundaries
 * (e.g. missing start block, truncated EOF before end block, or exceeded message size).
 */
public class MllpFramingException extends IOException {

    public MllpFramingException(String message) {
        super(message);
    }

    public MllpFramingException(String message, Throwable cause) {
        super(message, cause);
    }
}
