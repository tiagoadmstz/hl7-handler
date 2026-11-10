package io.github.tdteixeira.hl7handler.model;

/**
 * Delimiters used in an HL7 v2 message, typically defined in MSH-1 and MSH-2.
 */
public record Hl7Delimiters(
        char fieldSeparator,
        char componentSeparator,
        char repetitionSeparator,
        char escapeCharacter,
        char subcomponentSeparator
) {
    public static final Hl7Delimiters DEFAULT = new Hl7Delimiters('|', '^', '~', '\\', '&');

    public static Hl7Delimiters fromMshHeader(String mshHeader) {
        if (mshHeader == null || mshHeader.length() < 8) {
            return DEFAULT;
        }

        // MSH|^~\&|...
        // char 3 is field separator (usually '|')
        char field = mshHeader.charAt(3);
        char comp = mshHeader.length() > 4 ? mshHeader.charAt(4) : '^';
        char rep = mshHeader.length() > 5 ? mshHeader.charAt(5) : '~';
        char esc = mshHeader.length() > 6 ? mshHeader.charAt(6) : '\\';
        char sub = mshHeader.length() > 7 ? mshHeader.charAt(7) : '&';

        return new Hl7Delimiters(field, comp, rep, esc, sub);
    }
}
