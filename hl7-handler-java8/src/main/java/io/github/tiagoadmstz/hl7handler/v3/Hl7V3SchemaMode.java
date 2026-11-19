package io.github.tiagoadmstz.hl7handler.v3;

/**
 * How an XSD schema violation is reported when a schema is configured on the builder.
 */
public enum Hl7V3SchemaMode {

    /** A document that violates the schema is rejected with {@link Hl7V3SchemaValidationException}. */
    STRICT,

    /** A schema violation is kept as a warning on the parsed document, and the document is accepted. */
    WARN
}
