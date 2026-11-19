package io.github.tiagoadmstz.hl7handler.v3;

import io.github.tiagoadmstz.hl7handler.Hl7ParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thrown in {@link Hl7V3SchemaMode#STRICT} when an HL7 v3 document does not match the configured XSD schema. Each
 * entry of {@link #errors()} names the line and column of one violation.
 */
public class Hl7V3SchemaValidationException extends Hl7ParseException {

    private static final long serialVersionUID = 1L;

    private final List<String> errors;

    /**
     * Creates the exception with every schema violation found in the document.
     *
     * @param message the summary message
     * @param errors the violations, in document order; copied
     */
    public Hl7V3SchemaValidationException(String message, List<String> errors) {
        super(message);
        List<String> copy = new ArrayList<>();
        if (errors != null) {
            copy.addAll(errors);
        }
        this.errors = Collections.unmodifiableList(copy);
    }

    /**
     * Returns the schema violations, each formatted as {@code XSD line L, column C: message}.
     *
     * @return an unmodifiable list of violations
     */
    public List<String> errors() {
        return errors;
    }
}
