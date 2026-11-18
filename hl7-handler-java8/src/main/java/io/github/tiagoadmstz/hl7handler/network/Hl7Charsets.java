package io.github.tiagoadmstz.hl7handler.network;

import io.github.tiagoadmstz.hl7handler.model.Hl7CharsetNames;

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
        return Hl7CharsetNames.resolve(msh18, defaultCharset);
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
