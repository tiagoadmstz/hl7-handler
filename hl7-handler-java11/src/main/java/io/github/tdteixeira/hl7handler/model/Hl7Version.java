package io.github.tdteixeira.hl7handler.model;

import java.util.Arrays;

/**
 * Represents the recognized HL7 versions.
 */
public enum Hl7Version {

    V2_1("2.1"),
    V2_2("2.2"),
    V2_3("2.3"),
    V2_3_1("2.3.1"),
    V2_4("2.4"),
    V2_5("2.5"),
    V2_5_1("2.5.1"),
    V2_6("2.6"),
    V2_7("2.7"),
    UNKNOWN("UNKNOWN");

    private final String identifier;

    Hl7Version(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public static Hl7Version fromIdentifier(String value) {
        if (value == null || value.trim().isEmpty()) {
            return UNKNOWN;
        }
        String clean = value.trim();
        return Arrays.stream(values())
                .filter(v -> v.identifier.equalsIgnoreCase(clean))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
