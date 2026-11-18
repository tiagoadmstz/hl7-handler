package io.github.tiagoadmstz.hl7handler.model;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Resolves Java charsets from HL7 MSH-18 values.
 */
public final class Hl7CharsetNames {

    private Hl7CharsetNames() {
    }

    /**
     * Resolves a Java Charset from an HL7 MSH-18 string value.
     * Returns the default charset (UTF-8 when none is given) if the value is blank or unknown.
     */
    public static Charset resolve(String msh18, Charset defaultCharset) {
        if (msh18 == null || msh18.trim().isEmpty()) {
            return defaultCharset != null ? defaultCharset : StandardCharsets.UTF_8;
        }

        String clean = msh18.trim().toUpperCase();
        if ("ASCII".equals(clean) || "US-ASCII".equals(clean)) {
            return StandardCharsets.US_ASCII;
        }
        if ("8859/1".equals(clean) || "ISO-8859-1".equals(clean)) {
            return StandardCharsets.ISO_8859_1;
        }
        if ("8859/2".equals(clean) || "ISO-8859-2".equals(clean)) {
            return Charset.forName("ISO-8859-2");
        }
        if ("8859/15".equals(clean) || "ISO-8859-15".equals(clean)) {
            return Charset.forName("ISO-8859-15");
        }
        if ("UNICODE".equals(clean) || "UNICODE UTF-8".equals(clean) || "UTF-8".equals(clean)) {
            return StandardCharsets.UTF_8;
        }
        if ("UNICODE UTF-16".equals(clean) || "UTF-16".equals(clean)) {
            return StandardCharsets.UTF_16;
        }

        try {
            return Charset.forName(clean);
        } catch (Exception ignored) {
            return defaultCharset != null ? defaultCharset : StandardCharsets.UTF_8;
        }
    }
}
