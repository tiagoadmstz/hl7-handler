package io.github.tdteixeira.hl7handler.parser;

import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.model.Hl7Version;

/**
 * Strategy interface for parsing HL7 messages.
 * Allows version-specific specializations while preserving Open/Closed principle.
 */
public interface Hl7Parser {

    /**
     * Checks if this parser supports the detected HL7 version.
     */
    boolean supports(Hl7Version version);

    /**
     * Parses the raw HL7 string into an Hl7Message domain model.
     *
     * @param rawHl7 the raw message text
     * @return the structured Hl7Message
     */
    Hl7Message parse(String rawHl7);
}
