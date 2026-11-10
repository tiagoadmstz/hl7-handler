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

    /**
     * Parses the raw HL7 string into an Hl7Message domain model with a pre-detected version,
     * avoiding redundant detection re-scans.
     *
     * @param rawHl7 the raw message text
     * @param detectedVersion the previously detected HL7 version, or null to detect automatically
     * @return the structured Hl7Message
     */
    default Hl7Message parse(String rawHl7, Hl7Version detectedVersion) {
        return parse(rawHl7);
    }
}
