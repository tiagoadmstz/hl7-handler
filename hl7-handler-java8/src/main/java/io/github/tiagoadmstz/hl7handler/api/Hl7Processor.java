package io.github.tiagoadmstz.hl7handler.api;

import io.github.tiagoadmstz.hl7handler.Hl7ParseException;
import io.github.tiagoadmstz.hl7handler.binding.Hl7JsonBinder;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7ToMapTransformer;
import io.github.tiagoadmstz.hl7handler.network.MllpStreamReader;
import io.github.tiagoadmstz.hl7handler.parser.Hl7ParserRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Immutable and thread-safe HL7 processor, created by {@code Hl7Handler.builder().build()}.
 * It can be shared by several threads; each read returns a new {@link Hl7Result}.
 */
public final class Hl7Processor {

    private final Hl7ParserRegistry registry;
    private final Hl7ToMapTransformer transformer;
    private final Hl7JsonBinder jsonBinder;
    private final MllpStreamReader mllpReader;
    private final Map<String, Object> mapping;

    /**
     * Creates a processor from loaded and validated components. A {@code null} mapping means no YAML mapping;
     * a non-null mapping is copied into a deep unmodifiable structure.
     */
    public Hl7Processor(
        Hl7ParserRegistry registry,
        Hl7ToMapTransformer transformer,
        Hl7JsonBinder jsonBinder,
        MllpStreamReader mllpReader,
        Map<String, Object> mapping
    ) {
        this.registry = registry;
        this.transformer = transformer;
        this.jsonBinder = jsonBinder;
        this.mllpReader = mllpReader;
        this.mapping = mapping != null ? Hl7Result.deepCopy(mapping, true) : null;
    }

    /**
     * Parses a raw HL7 string.
     *
     * @throws Hl7ParseException if the input is null or blank
     */
    public Hl7Result read(String rawHl7) {
        return new Hl7Result(rawHl7, registry, transformer, jsonBinder, mapping);
    }

    /**
     * Parses HL7 bytes, with or without MLLP framing.
     *
     * @throws Hl7ParseException if the input is empty once the framing is removed
     */
    public Hl7Result readBytes(byte[] bytes) {
        return read(mllpReader.fromBytes(bytes));
    }

    /**
     * Reads one MLLP-framed message from the stream.
     *
     * @return the result, or {@code null} on a clean end of stream
     * @throws Hl7ParseException if the stream cannot be read
     */
    public Hl7Result readMllp(InputStream in) {
        String rawHl7;
        try {
            rawHl7 = mllpReader.readMessage(in);
        } catch (IOException e) {
            throw new Hl7ParseException("Error reading MLLP byte stream", e);
        }
        if (rawHl7 == null) {
            return null;
        }
        return read(rawHl7);
    }

    /**
     * Returns {@code true} when a YAML mapping was configured on the builder.
     */
    public boolean hasMapping() {
        return mapping != null;
    }
}
