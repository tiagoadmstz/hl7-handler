package io.github.tiagoadmstz.hl7handler.model;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Utility for encoding and decoding HL7 v2 escape sequences.
 */
public final class Hl7Escapes {

    private Hl7Escapes() {
    }

    /**
     * Decodes HL7 escape sequences using UTF-8 for hexadecimal (\Xhh\) sequences.
     * Sequences like \H\, \N\ and \Z...\ are preserved as literal text.
     */
    public static String decode(String raw, Hl7Delimiters delimiters) {
        return decode(raw, delimiters, StandardCharsets.UTF_8);
    }

    /**
     * Decodes HL7 escape sequences (\F\, \S\, \T\, \R\, \E\, \.br\, \Xhh\).
     * Hexadecimal sequences are bytes in the given charset (UTF-8 when null).
     * Sequences like \H\, \N\ and \Z...\ are preserved as literal text.
     */
    public static String decode(String raw, Hl7Delimiters delimiters, Charset charset) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }

        Hl7Delimiters delims = delimiters != null ? delimiters : Hl7Delimiters.DEFAULT;
        Charset cs = charset != null ? charset : StandardCharsets.UTF_8;
        char esc = delims.escapeCharacter();
        if (raw.indexOf(esc) == -1) {
            return raw;
        }

        StringBuilder sb = new StringBuilder(raw.length());
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == esc) {
                int end = raw.indexOf(esc, i + 1);
                if (end != -1) {
                    String seq = raw.substring(i + 1, end);
                    String replacement = decodeSequence(seq, delims, cs);
                    if (replacement != null) {
                        sb.append(replacement);
                        i = end + 1;
                        continue;
                    }
                }
            }
            sb.append(c);
            i++;
        }

        return sb.toString();
    }

    private static String decodeSequence(String seq, Hl7Delimiters delims, Charset charset) {
        if ("F".equals(seq)) {
            return String.valueOf(delims.fieldSeparator());
        }
        if ("S".equals(seq)) {
            return String.valueOf(delims.componentSeparator());
        }
        if ("T".equals(seq)) {
            return String.valueOf(delims.subcomponentSeparator());
        }
        if ("R".equals(seq)) {
            return String.valueOf(delims.repetitionSeparator());
        }
        if ("E".equals(seq)) {
            return String.valueOf(delims.escapeCharacter());
        }
        if (".br".equalsIgnoreCase(seq)) {
            return "\n";
        }
        if ((seq.startsWith("X") || seq.startsWith("x")) && seq.length() > 1) {
            String hex = seq.substring(1);
            if (hex.length() % 2 == 0 && hex.matches("(?i)[0-9a-f]+")) {
                byte[] bytes = new byte[hex.length() / 2];
                for (int h = 0; h < hex.length(); h += 2) {
                    bytes[h / 2] = (byte) Integer.parseInt(hex.substring(h, h + 2), 16);
                }
                return new String(bytes, charset);
            }
        }
        return null;
    }

    /**
     * Encodes reserved characters and newlines into standard HL7 escape sequences.
     */
    public static String encode(String text, Hl7Delimiters delimiters) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        Hl7Delimiters delims = delimiters != null ? delimiters : Hl7Delimiters.DEFAULT;
        StringBuilder sb = new StringBuilder(text.length());

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == delims.escapeCharacter()) {
                sb.append(delims.escapeCharacter()).append('E').append(delims.escapeCharacter());
            } else if (c == delims.fieldSeparator()) {
                sb.append(delims.escapeCharacter()).append('F').append(delims.escapeCharacter());
            } else if (c == delims.componentSeparator()) {
                sb.append(delims.escapeCharacter()).append('S').append(delims.escapeCharacter());
            } else if (c == delims.repetitionSeparator()) {
                sb.append(delims.escapeCharacter()).append('R').append(delims.escapeCharacter());
            } else if (c == delims.subcomponentSeparator()) {
                sb.append(delims.escapeCharacter()).append('T').append(delims.escapeCharacter());
            } else if (c == '\r') {
                if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                sb.append(delims.escapeCharacter()).append(".br").append(delims.escapeCharacter());
            } else if (c == '\n') {
                sb.append(delims.escapeCharacter()).append(".br").append(delims.escapeCharacter());
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }
}
