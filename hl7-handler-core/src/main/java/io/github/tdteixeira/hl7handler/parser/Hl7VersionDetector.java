package io.github.tdteixeira.hl7handler.parser;

import io.github.tdteixeira.hl7handler.model.Hl7Delimiters;
import io.github.tdteixeira.hl7handler.model.Hl7Version;

/**
 * High-performance detector that identifies the HL7 version directly from MSH-12
 * without parsing the whole message payload first.
 */
public final class Hl7VersionDetector {

    private Hl7VersionDetector() {}

    /**
     * Inspects the input HL7 raw content to detect the version.
     * Looks at field 12 of the MSH segment.
     */
    public static Hl7Version detect(String rawHl7) {
        if (rawHl7 == null || rawHl7.trim().isEmpty()) {
            return Hl7Version.UNKNOWN;
        }

        int mshIndex = rawHl7.indexOf("MSH");
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
        String[] fields = mshLine.split("\\" + fieldSep, -1);

        // MSH-1 is the separator itself.
        // fields[0] = "MSH"
        // fields[1] = encoding characters (MSH-2)
        // fields[2] = MSH-3
        // ...
        // fields[11] = MSH-12 (Version ID)
        if (fields.length > 11) {
            String versionField = fields[11];
            // Might have component separator, e.g. "2.5^USA"
            if (versionField.indexOf(delimiters.componentSeparator()) != -1) {
                versionField = versionField.substring(0, versionField.indexOf(delimiters.componentSeparator()));
            }
            return Hl7Version.fromIdentifier(versionField);
        }

        return Hl7Version.UNKNOWN;
    }
}
