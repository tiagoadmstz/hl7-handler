package io.github.tiagoadmstz.hl7handler.v3.writer;

import io.github.tiagoadmstz.hl7handler.binding.Hl7JsonBinder;
import io.github.tiagoadmstz.hl7handler.v3.parser.Hl7V3Parser;
import io.github.tiagoadmstz.hl7handler.v3.parser.Hl7V3Xml;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Immutable and thread-safe HL7 v3 writer, created by {@code Hl7Handler.v3().buildWriter()}. Each call returns a new
 * XML document. When a schema is configured, the produced XML is parsed again with the schema-validating parser:
 * in STRICT mode a violation throws, and in WARN mode the document is returned anyway.
 */
public final class Hl7V3Writer {

    private final Hl7V3MapToXmlTransformer transformer;
    private final Hl7V3Parser schemaValidator;
    private final boolean indent;
    private final Hl7JsonBinder jsonBinder;

    /**
     * Creates a writer from validated components.
     *
     * @param transformer the transformer that builds the DOM from a semantic Map
     * @param schemaValidator the parser used to validate the output, or {@code null} when no schema is configured
     * @param indent whether the output is indented by two spaces per level
     * @param jsonBinder the binder used to convert a Value Object into a Map
     */
    public Hl7V3Writer(Hl7V3MapToXmlTransformer transformer, Hl7V3Parser schemaValidator, boolean indent,
                       Hl7JsonBinder jsonBinder) {
        this.transformer = transformer;
        this.schemaValidator = schemaValidator;
        this.indent = indent;
        this.jsonBinder = jsonBinder;
    }

    /**
     * Writes the semantic Map as HL7 v3 XML.
     *
     * @param data the values, keyed as the mapping template; {@code null} values are skipped
     * @return the XML text, declaring UTF-8
     * @throws IllegalArgumentException if the data is {@code null}
     * @throws io.github.tiagoadmstz.hl7handler.v3.Hl7V3SchemaValidationException in STRICT mode, when the output does not match the schema
     */
    public String write(Map<String, Object> data) {
        if (data == null) {
            throw new IllegalArgumentException("Data to write must not be null");
        }
        String xml = Hl7V3Xml.toXml(transformer.transform(data), indent);
        if (schemaValidator != null) {
            schemaValidator.parse(new InputSource(new StringReader(xml)));
        }
        return xml;
    }

    /**
     * Writes a Value Object, or any bean-like object, as HL7 v3 XML, after converting it into a Map.
     *
     * @param value the object to write
     * @return the XML text, declaring UTF-8
     * @throws IllegalArgumentException if the value is {@code null}
     */
    public String write(Object value) {
        Map<String, Object> data = jsonBinder.toMap(value);
        if (data == null) {
            throw new IllegalArgumentException("Data to write must not be null");
        }
        return write(data);
    }

    /**
     * Writes the semantic Map as UTF-8 bytes.
     *
     * @param data the values, keyed as the mapping template
     * @return the UTF-8 bytes of the XML document
     */
    public byte[] writeBytes(Map<String, Object> data) {
        return write(data).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Writes a Value Object as UTF-8 bytes.
     *
     * @param value the object to write
     * @return the UTF-8 bytes of the XML document
     */
    public byte[] writeBytes(Object value) {
        return write(value).getBytes(StandardCharsets.UTF_8);
    }
}
