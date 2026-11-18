package io.github.tiagoadmstz.hl7handler.parser;

import io.github.tiagoadmstz.hl7handler.model.Hl7Delimiters;
import io.github.tiagoadmstz.hl7handler.model.Hl7Version;

/**
 * High-performance detector that identifies the HL7 version directly from MSH-12
 * without parsing the whole message payload first.
 * Resilient to preambles, garbage bytes, and batch headers (FHS, BHS).
 */
public final class Hl7VersionDetector {

    private Hl7VersionDetector() {}

    /**
     * Finds the starting character index of the first valid MSH segment in the text,
     * skipping any file/batch headers (FHS, BHS) or leading preamble garbage.
     *
     * @param rawHl7 raw HL7 text
     * @return index of "MSH", or -1 if not found
     */
    public static int findMshIndex(String rawHl7) {
        if (rawHl7 == null || rawHl7.isEmpty()) {
            return -1;
        }

        int idx = 0;
        int len = rawHl7.length();

        while (idx < len && (idx = rawHl7.indexOf("MSH", idx)) != -1) {
            // Check if MSH is at start of string or preceded by a line break / line start
            boolean validLineStart = (idx == 0);
            if (!validLineStart) {
                char prev = rawHl7.charAt(idx - 1);
                if (prev == '\r' || prev == '\n') {
                    validLineStart = true;
                } else {
                    int lastBreak = Math.max(rawHl7.lastIndexOf('\r', idx - 1), rawHl7.lastIndexOf('\n', idx - 1));
                    int startCheck = (lastBreak >= 0) ? lastBreak + 1 : 0;
                    boolean onlyWs = true;
                    for (int k = startCheck; k < idx; k++) {
                        if (!Character.isWhitespace(rawHl7.charAt(k))) {
                            onlyWs = false;
                            break;
                        }
                    }
                    if (onlyWs) {
                        validLineStart = true;
                    }
                }
            }

            if (validLineStart && idx + 3 < len) {
                char sep = rawHl7.charAt(idx + 3);
                // Valid separator is not alphanumeric, not newline, and not whitespace
                if (sep != '\r' && sep != '\n' && !Character.isLetterOrDigit(sep) && !Character.isWhitespace(sep)) {
                    return idx;
                }
            }

            idx += 3;
        }

        return -1;
    }

    /**
     * Inspects the input HL7 raw content to detect the version.
     * Looks at field 12 of the MSH segment.
     */
    public static Hl7Version detect(String rawHl7) {
        if (rawHl7 == null || rawHl7.trim().isEmpty()) {
            return Hl7Version.UNKNOWN;
        }

        int mshIndex = findMshIndex(rawHl7);
        if (mshIndex == -1) {
            return Hl7Version.UNKNOWN;
        }

        // Delimiters
        Hl7Delimiters delimiters = Hl7Delimiters.fromMshHeader(rawHl7.substring(mshIndex));
        char fieldSep = delimiters.fieldSeparator();

        // Find end of MSH line (\r or \n)
        int lineEnd = rawHl7.indexOf('\r', mshIndex);
        if (lineEnd == -1) {
            lineEnd = rawHl7.indexOf('\n', mshIndex);
        }
        if (lineEnd == -1) {
            lineEnd = rawHl7.length();
        }

        String mshLine = rawHl7.substring(mshIndex, lineEnd);
        // MSH-1 is the separator itself, so MSH-12 starts after the 11th separator.
        int fieldStart = 0;
        for (int i = 0; i < 11; i++) {
            int sep = mshLine.indexOf(fieldSep, fieldStart);
            if (sep == -1) {
                return Hl7Version.UNKNOWN;
            }
            fieldStart = sep + 1;
        }
        int fieldEnd = mshLine.indexOf(fieldSep, fieldStart);
        String versionField = fieldEnd == -1 ? mshLine.substring(fieldStart) : mshLine.substring(fieldStart, fieldEnd);
        // Might have component separator, e.g. "2.5^USA"
        if (versionField.indexOf(delimiters.componentSeparator()) != -1) {
            versionField = versionField.substring(0, versionField.indexOf(delimiters.componentSeparator()));
        }
        return Hl7Version.fromIdentifier(versionField);
    }
}
