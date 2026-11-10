package io.github.tdteixeira.hl7handler.network;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Utility for resolving and detecting character encodings from HL7 v2 messages and MSH-18.
 */
public final class Hl7Charsets {

    private Hl7Charsets() {
    }

    /**
     * Resolves a Java Charset from an HL7 MSH-18 string value.
     */
    public static Charset fromMsh18(String msh18, Charset defaultCharset) {
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

    /**
     * Inspects raw message bytes to detect the encoding specified in MSH-18.
     * If absent or unreadable, returns the provided default charset.
     */
    public static Charset detectCharset(byte[] bytes, Charset defaultCharset) {
        if (bytes == null || bytes.length < 8) {
            return defaultCharset != null ? defaultCharset : StandardCharsets.UTF_8;
        }

        int mshIndex = -1;
        for (int i = 0; i <= bytes.length - 4; i++) {
            if (bytes[i] == 'M' && bytes[i + 1] == 'S' && bytes[i + 2] == 'H') {
                mshIndex = i;
                break;
            }
        }

        if (mshIndex == -1) {
            return defaultCharset != null ? defaultCharset : StandardCharsets.UTF_8;
        }

        byte sep = bytes[mshIndex + 3];
        int fieldIndex = 1;
        int fieldStart = -1;
        int fieldEnd = -1;

        for (int i = mshIndex + 4; i < bytes.length; i++) {
            byte b = bytes[i];
            if (b == '\r' || b == '\n') {
                if (fieldIndex == 17) {
                    fieldEnd = i;
                }
                break;
            }
            if (b == sep) {
                fieldIndex++;
                if (fieldIndex == 17) {
                    fieldStart = i + 1;
                } else if (fieldIndex == 18) {
                    fieldEnd = i;
                    break;
                }
            }
        }

        if (fieldStart != -1) {
            if (fieldEnd == -1) {
                fieldEnd = bytes.length;
            }
            if (fieldEnd > fieldStart) {
                String msh18 = new String(bytes, fieldStart, fieldEnd - fieldStart, StandardCharsets.US_ASCII);
                return fromMsh18(msh18, defaultCharset);
            }
        }

        return defaultCharset != null ? defaultCharset : StandardCharsets.UTF_8;
    }
}
