package io.github.tiagoadmstz.hl7handler.fhir;

import io.github.tiagoadmstz.hl7handler.binding.Hl7JsonBinder;
import io.github.tiagoadmstz.hl7handler.fhir.mapping.FhirToMapTransformer;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7MappingException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable result of reading one FHIR resource. The resource is parsed once upon creation.
 * The mapped semantic Map is computed on first use, cached, and returned unmodifiable.
 */
public final class FhirResult {

    private final FhirResource resource;
    private final FhirToMapTransformer transformer;
    private final Hl7JsonBinder jsonBinder;
    private final Map<String, Object> mapping;
    private final Object lock = new Object();
    private volatile Map<String, Object> mappedMap;

    public FhirResult(
        FhirResource resource,
        FhirToMapTransformer transformer,
        Hl7JsonBinder jsonBinder,
        Map<String, Object> mapping
    ) {
        this.resource = resource;
        this.transformer = transformer;
        this.jsonBinder = jsonBinder;
        this.mapping = mapping;
    }

    public FhirResource resource() {
        return resource;
    }

    /**
     * Returns the raw JSON representation of the resource.
     */
    public String toRawJson() {
        return resource.toPrettyJson();
    }

    /**
     * Returns the mapped semantic Map, computed once and cached.
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
                mappedMap = deepCopy(transformer.transform(resource, mapping));
            }
            return mappedMap;
        }
    }

    /**
     * Returns the mapped JSON when a YAML mapping was configured, otherwise the raw formatted JSON.
     */
    public String toJson() {
        if (mapping != null) {
            return jsonBinder.toJson(toMap());
        }
        return toRawJson();
    }

    /**
     * Binds the mapped Map into the target Value Object.
     *
     * @throws Hl7MappingException if no YAML mapping was configured
     */
    public <T> T bindTo(Class<T> targetClass) {
        return jsonBinder.bind(toMap(), targetClass);
    }

    static Map<String, Object> deepCopy(Map<?, ?> source) {
        return copyMap(source);
    }

    private static Map<String, Object> copyMap(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), copyValue(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?>) {
            return copyMap((Map<?, ?>) value);
        }
        if (value instanceof List<?>) {
            List<Object> copy = new ArrayList<>();
            for (Object item : (List<?>) value) {
                copy.add(copyValue(item));
            }
            return Collections.unmodifiableList(copy);
        }
        return value;
    }
}
