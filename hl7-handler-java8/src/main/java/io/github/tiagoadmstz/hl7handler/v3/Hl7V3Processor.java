package io.github.tiagoadmstz.hl7handler.v3;

import io.github.tiagoadmstz.hl7handler.Hl7ParseException;
import io.github.tiagoadmstz.hl7handler.binding.Hl7JsonBinder;
import io.github.tiagoadmstz.hl7handler.v3.mapping.Hl7V3ToMapTransformer;
import io.github.tiagoadmstz.hl7handler.v3.parser.Hl7V3Parser;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Map;

/**
 * Immutable and thread-safe HL7 v3 processor, created by {@code Hl7Handler.v3().build()}.
 * It can be shared by several threads; each read returns a new {@link Hl7V3Result}.
 */
public final class Hl7V3Processor {

    private static final String EMPTY_INPUT_MESSAGE = "HL7 v3 input content must not be null or blank";

    private final Hl7V3Parser parser;
    private final Hl7V3ToMapTransformer transformer;
    private final Hl7JsonBinder jsonBinder;
    private final Map<String, Object> mapping;

    /**
     * Creates a processor from loaded and validated components. A {@code null} mapping means no YAML mapping;
     * a non-null mapping is copied into a deep unmodifiable structure.
     */
    public Hl7V3Processor(
        Hl7V3Parser parser,
        Hl7V3ToMapTransformer transformer,
        Hl7JsonBinder jsonBinder,
        Map<String, Object> mapping
    ) {
        this.parser = parser;
        this.transformer = transformer;
        this.jsonBinder = jsonBinder;
        this.mapping = mapping != null ? Hl7V3Result.deepCopy(mapping) : null;
    }

    /**
     * Parses an HL7 v3 XML string. A string is already decoded, so its XML declaration is not used for the encoding.
     *
     * @throws Hl7ParseException if the input is null or blank, malformed, contains a DOCTYPE, or is not HL7 v3
     */
    public Hl7V3Result read(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new Hl7ParseException(EMPTY_INPUT_MESSAGE);
        }
        return newResult(parser.parse(new InputSource(new StringReader(xml))));
    }

    /**
     * Parses HL7 v3 XML from a byte stream. The XML parser detects the encoding from the declaration.
     *
     * @throws Hl7ParseException if the stream is null, malformed, contains a DOCTYPE, or is not HL7 v3
     */
    public Hl7V3Result read(InputStream in) {
        if (in == null) {
            throw new Hl7ParseException(EMPTY_INPUT_MESSAGE);
        }
        return newResult(parser.parse(new InputSource(in)));
    }

    /**
     * Parses HL7 v3 XML from bytes. The XML parser detects the encoding from the declaration.
     *
     * @throws Hl7ParseException if the bytes are null, empty, malformed, contain a DOCTYPE, or are not HL7 v3
     */
    public Hl7V3Result readBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new Hl7ParseException(EMPTY_INPUT_MESSAGE);
        }
        return read(new ByteArrayInputStream(bytes));
    }

    /**
     * Returns {@code true} when a YAML mapping was configured on the builder.
     */
    public boolean hasMapping() {
        return mapping != null;
    }

    private Hl7V3Result newResult(Hl7V3Document document) {
        return new Hl7V3Result(document, transformer, jsonBinder, mapping);
    }
}
