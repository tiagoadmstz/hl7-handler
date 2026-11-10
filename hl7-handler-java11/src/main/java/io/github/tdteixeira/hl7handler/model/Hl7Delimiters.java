package io.github.tdteixeira.hl7handler.model;

import java.util.Objects;

/**
 * Delimiters used in an HL7 v2 message, typically defined in MSH-1 and MSH-2.
 */
public final class Hl7Delimiters {

    public static final Hl7Delimiters DEFAULT = new Hl7Delimiters('|', '^', '~', '\\', '&');

    private final char fieldSeparator;
    private final char componentSeparator;
    private final char repetitionSeparator;
    private final char escapeCharacter;
    private final char subcomponentSeparator;

    public Hl7Delimiters(
            char fieldSeparator,
            char componentSeparator,
            char repetitionSeparator,
            char escapeCharacter,
            char subcomponentSeparator
    ) {
        this.fieldSeparator = fieldSeparator;
        this.componentSeparator = componentSeparator;
        this.repetitionSeparator = repetitionSeparator;
        this.escapeCharacter = escapeCharacter;
        this.subcomponentSeparator = subcomponentSeparator;
    }

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
                && subcomponentSeparator == that.subcomponentSeparator;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldSeparator, componentSeparator, repetitionSeparator, escapeCharacter, subcomponentSeparator);
    }

    @Override
    public String toString() {
        return "Hl7Delimiters[fieldSeparator=" + fieldSeparator
                + ", componentSeparator=" + componentSeparator
                + ", repetitionSeparator=" + repetitionSeparator
                + ", escapeCharacter=" + escapeCharacter
                + ", subcomponentSeparator=" + subcomponentSeparator + "]";
    }
}
