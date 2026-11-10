package io.github.tdteixeira.hl7handler.model;

import java.util.Objects;

/**
 * Delimiters used in an HL7 v2 message, typically defined in MSH-1 and MSH-2.
 */
public final class Hl7Delimiters {

    public static final Hl7Delimiters DEFAULT = new Hl7Delimiters('|', '^', '~', '\\', '&', null);

    private final char fieldSeparator;
    private final char componentSeparator;
    private final char repetitionSeparator;
    private final char escapeCharacter;
    private final char subcomponentSeparator;
    private final Character truncationCharacter;

    public Hl7Delimiters(
            char fieldSeparator,
            char componentSeparator,
            char repetitionSeparator,
            char escapeCharacter,
            char subcomponentSeparator
    ) {
        this(fieldSeparator, componentSeparator, repetitionSeparator, escapeCharacter, subcomponentSeparator, null);
    }

    public Hl7Delimiters(
            char fieldSeparator,
            char componentSeparator,
            char repetitionSeparator,
            char escapeCharacter,
            char subcomponentSeparator,
            Character truncationCharacter
    ) {
        this.fieldSeparator = fieldSeparator;
        this.componentSeparator = componentSeparator;
        this.repetitionSeparator = repetitionSeparator;
        this.escapeCharacter = escapeCharacter;
        this.subcomponentSeparator = subcomponentSeparator;
        this.truncationCharacter = truncationCharacter;
    }

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

    public char fieldSeparator() {
        return fieldSeparator;
    }

    public char componentSeparator() {
        return componentSeparator;
    }

    public char repetitionSeparator() {
        return repetitionSeparator;
    }

    public char escapeCharacter() {
        return escapeCharacter;
    }

    public char subcomponentSeparator() {
        return subcomponentSeparator;
    }

    public Character truncationCharacter() {
        return truncationCharacter;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Hl7Delimiters)) {
            return false;
        }
        Hl7Delimiters that = (Hl7Delimiters) other;
        return fieldSeparator == that.fieldSeparator
                && componentSeparator == that.componentSeparator
                && repetitionSeparator == that.repetitionSeparator
                && escapeCharacter == that.escapeCharacter
                && subcomponentSeparator == that.subcomponentSeparator
                && Objects.equals(truncationCharacter, that.truncationCharacter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldSeparator, componentSeparator, repetitionSeparator, escapeCharacter, subcomponentSeparator, truncationCharacter);
    }

    @Override
    public String toString() {
        return "Hl7Delimiters[fieldSeparator=" + fieldSeparator
                + ", componentSeparator=" + componentSeparator
                + ", repetitionSeparator=" + repetitionSeparator
                + ", escapeCharacter=" + escapeCharacter
                + ", subcomponentSeparator=" + subcomponentSeparator
                + ", truncationCharacter=" + truncationCharacter + "]";
    }
}
