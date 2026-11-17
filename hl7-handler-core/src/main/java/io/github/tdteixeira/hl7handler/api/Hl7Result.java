package io.github.tdteixeira.hl7handler.api;

import io.github.tdteixeira.hl7handler.Hl7ParseException;
import io.github.tdteixeira.hl7handler.binding.Hl7JsonBinder;
import io.github.tdteixeira.hl7handler.mapping.Hl7MappingException;
import io.github.tdteixeira.hl7handler.mapping.Hl7ToMapTransformer;
import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.parser.Hl7Parser;
import io.github.tdteixeira.hl7handler.parser.Hl7ParserRegistry;
import io.github.tdteixeira.hl7handler.parser.Hl7VersionDetector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable result of reading one HL7 message. The message is parsed once, when the result is created.
 * The mapped Map is computed on first use, cached, and returned unmodifiable; it is safe to share across threads.
 */
public final class Hl7Result {

    private final Hl7Message message;
    private final Hl7ToMapTransformer transformer;
    private final Hl7JsonBinder jsonBinder;
    private final Map<String, Object> mapping;
    private final Object lock = new Object();
    private volatile Map<String, Object> mappedMap;

    Hl7Result(
        String rawHl7,
        Hl7ParserRegistry registry,
        Hl7ToMapTransformer transformer,
        Hl7JsonBinder jsonBinder,
        Map<String, Object> mapping
    ) {
        if (rawHl7 == null || rawHl7.trim().isEmpty()) {
            throw new Hl7ParseException("HL7 input content must not be null or blank");
        }
        Hl7Version version = Hl7VersionDetector.detect(rawHl7);
        Hl7Parser parser = registry.getParserFor(version);
        this.message = parser.parse(rawHl7, version);
        this.transformer = transformer;
        this.jsonBinder = jsonBinder;
        this.mapping = mapping;
    }

    /**
     * Returns the parsed HL7 message domain model.
     */
    public Hl7Message message() {
        return message;
    }

    /**
     * Returns the structural JSON of the message, independent of any YAML mapping.
     */
    public String toRawJson() {
        return jsonBinder.toRawJson(message);
    }

    /**
     * Returns the mapped semantic Map, computed once and cached. The map and its nested maps and lists are unmodifiable.
     *
     * @throws Hl7MappingException if no YAML mapping was configured
     */
    public Map<String, Object> toMap() {
        Map<String, Object> cached = mappedMap;
        if (cached != null) {
            return cached;
        }
        if (mapping == null) {
            throw new Hl7MappingException("No YAML mapping was configured. Call withMapping() before toMap() or use toRawJson()");
        }
        synchronized (lock) {
            if (mappedMap == null) {
                mappedMap = deepCopy(transformer.transform(message, mapping), true);
            }
            return mappedMap;
        }
    }

    /**
     * Returns the mapped JSON when a YAML mapping was configured, otherwise the raw structural JSON.
     */
    public String toJson() {
        if (mapping != null) {
            return jsonBinder.toJson(toMap());
        }
        return toRawJson();
    }

    /**
     * Binds the mapped Map into the client's Value Object.
     *
     * @throws Hl7MappingException if no YAML mapping was configured
     */
    public <T> T bindTo(Class<T> targetClass) {
        return jsonBinder.bind(toMap(), targetClass);
    }

    /**
     * Copies a mapped structure, nested maps and lists included, with String keys.
     */
    static Map<String, Object> deepCopy(Map<?, ?> source, boolean unmodifiable) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), copyValue(entry.getValue(), unmodifiable));
        }
        return unmodifiable ? Collections.unmodifiableMap(copy) : copy;
    }

    private static Object copyValue(Object value, boolean unmodifiable) {
        if (value instanceof Map<?, ?>) {
            return deepCopy((Map<?, ?>) value, unmodifiable);
        }
        if (value instanceof List<?>) {
            List<Object> copy = new ArrayList<>();
            for (Object item : (List<?>) value) {
                copy.add(copyValue(item, unmodifiable));
            }
            return unmodifiable ? Collections.unmodifiableList(copy) : copy;
        }
        return value;
    }
}
