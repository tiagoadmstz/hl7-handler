package io.github.tdteixeira.hl7handler.model;

/**
 * Delimiters used in an HL7 v2 message, typically defined in MSH-1 and MSH-2.
 */
public record Hl7Delimiters(
        char fieldSeparator,
        char componentSeparator,
        char repetitionSeparator,
        char escapeCharacter,
        char subcomponentSeparator,
        Character truncationCharacter
) {
    public static final Hl7Delimiters DEFAULT = new Hl7Delimiters('|', '^', '~', '\\', '&', null);

    public Hl7Delimiters(char fieldSeparator, char componentSeparator, char repetitionSeparator, char escapeCharacter, char subcomponentSeparator) {
        this(fieldSeparator, componentSeparator, repetitionSeparator, escapeCharacter, subcomponentSeparator, null);
    }

    /**
     * Extracts delimiters from the MSH header.
     * Accurately parses MSH-2 from 2 up to 5 characters (5th being truncation in v2.7+).
     */
    public static Hl7Delimiters fromMshHeader(String mshHeader) {
        if (mshHeader == null || mshHeader.length() < 4 || !mshHeader.startsWith("MSH")) {
            return DEFAULT;
        }

        char field = mshHeader.charAt(3);
        int end = mshHeader.length();
        int nextField = mshHeader.indexOf(field, 4);
        if (nextField != -1) {
            end = nextField;
        }

        int cr = mshHeader.indexOf('\r', 4);
        int lf = mshHeader.indexOf('\n', 4);
        if (cr != -1 && cr < end) {
            end = cr;
        }
        if (lf != -1 && lf < end) {
            end = lf;
        }

        String msh2 = mshHeader.substring(4, end);
        char comp = msh2.length() > 0 ? msh2.charAt(0) : '^';
        char rep = msh2.length() > 1 ? msh2.charAt(1) : '~';
        char esc = msh2.length() > 2 ? msh2.charAt(2) : '\\';
        char sub = msh2.length() > 3 ? msh2.charAt(3) : '&';
        Character trunc = msh2.length() > 4 ? msh2.charAt(4) : null;

        return new Hl7Delimiters(field, comp, rep, esc, sub, trunc);
    }
}
