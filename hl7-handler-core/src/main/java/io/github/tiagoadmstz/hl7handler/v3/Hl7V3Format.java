package io.github.tiagoadmstz.hl7handler.v3;

import java.util.Locale;

/**
 * Document family of an HL7 v3 XML document: a CDA R2 clinical document or an HL7 v3 interaction message.
 */
public enum Hl7V3Format {

    CDA,
    MESSAGE;

    /**
     * Resolves a format name from a mapping, ignoring case and surrounding spaces.
     *
     * @param name the format name, such as {@code CDA} or {@code message}
     * @return the matching format, or {@code null} when the name is null or unknown
     */
    public static Hl7V3Format fromName(String name) {
        if (name == null) {
            return null;
        }
        String clean = name.trim().toUpperCase(Locale.ROOT);
        for (Hl7V3Format format : values()) {
            if (format.name().equals(clean)) {
                return format;
            }
        }
        return null;
    }
}
